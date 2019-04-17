(ns ventas.utils.images
  (:require
   [clojure.java.io :as io]
   [slingshot.slingshot :refer [throw+]]
   [ventas.utils.files :as utils.files])
  (:import [javax.imageio ImageIO]
           [net.coobird.thumbnailator Thumbnails]
           [java.awt.image BufferedImage]
           [net.coobird.thumbnailator.resizers.configurations ScalingMode]))

(defn path-with-metadata [path options]
  (str (utils.files/basename path)
       "-" (hash options)
       "." (utils.files/extension path)))

(defn- portrait? [relation]
  (< relation 1))

(defn- landscape? [relation]
  (<= 1 relation))

;; these functions make this way easier to test

(defn- source-region [builder x y w h]
  (.sourceRegion builder x y w h))

(defn- scale-to [builder factor]
  (.scale builder factor))

(defn- output-quality [builder quality]
  (.outputQuality builder quality))

(defn- scale-dimensions* [scale {:keys [width height]}]
  {:width (* 1.0 scale width)
   :height (* 1.0 scale height)})

(defn- scale-dimensions [source target]
  "Ensures that the target width and height are not higher than their source counterparts"
  (->> target
       (scale-dimensions* (min 1 (/ (:width source) (:width target))))
       (scale-dimensions* (min 1 (/ (:height source) (:height target))))))

(defn- adapt-dimensions-to-relation* [{:keys [width height] :as source} target-relation]
  (scale-dimensions
   source
   (if (landscape? target-relation)
     {:width (* height target-relation)
      :height height}
     {:width width
      :height (/ width target-relation)})))

(defn- adapt-dimensions-to-relation [{:keys [width height target-relation]}]
  "Returns a new width and height that matches the given target relation, using the maximum
   available space within the given width and height"
  (let [source-relation (/ width height)]
    (if (or (= 1 target-relation)
            (and (landscape? target-relation) (landscape? source-relation))
            (and (portrait? target-relation) (portrait? source-relation)))
      (adapt-dimensions-to-relation* {:width width :height height} target-relation)
      (let [{:keys [width height]} (adapt-dimensions-to-relation
                                    {:width width
                                     :height height
                                     :target-relation 1})]
        (adapt-dimensions-to-relation* {:width width :height height} target-relation)))))

(defn- crop-image [builder metadata {:keys [offset size relation]}]
  (if relation
    (let [relation (* 1.0 relation)
          {:keys [width height]} (adapt-dimensions-to-relation
                                  {:width (:width metadata)
                                   :height (:height metadata)
                                   :target-relation relation})]
      (recur builder metadata {:offset [(- (/ (:width metadata) 2.0) (/ width 2.0))
                                        (- (/ (:height metadata) 2.0) (/ height 2.0))]
                               :size [width height]}))
    (source-region builder
                   (first offset)
                   (second offset)
                   (first size)
                   (second size))))

(defn resize-image [builder metadata {:keys [width height allow-smaller?]}]
  (if (and allow-smaller? (< (:width metadata) width) (< (:height metadata) height))
    builder
    (let [width-scale (/ width (:width metadata))
          height-scale (/ height (:height metadata))]
      (scale-to builder (min width-scale height-scale)))))

(defn transform-image* [source-path target-path {:keys [resize scale crop quality]}]
  (when-not (.exists (io/file source-path))
    (throw+ {:type ::file-not-found
             :path source-path}))
  (let [^BufferedImage buffered-image (ImageIO/read (io/file source-path))
        metadata {:width (.getWidth buffered-image)
                  :height (.getHeight buffered-image)}]
    (-> [buffered-image]
        into-array
        Thumbnails/of
        (.scalingMode ScalingMode/PROGRESSIVE_BILINEAR)
        (cond-> crop (crop-image metadata crop)
                scale (scale-to scale)
                resize (resize-image metadata resize)
                quality (output-quality (double quality))
                (and (not scale) (not resize)) (scale-to 1))
        (.toFile (io/file target-path)))))

(defn transform-image [source-path target-dir & [options]]
  (let [target-dir (or target-dir (utils.files/get-tmp-dir))
        target-filename (path-with-metadata source-path options)
        target-path (str target-dir "/" target-filename)]
    (when (and (:scale options) (or (get-in options [:resize :width])
                                    (get-in options [:resize :height])))
      (throw+ {:type ::inconsistent-parameters
               :message "Setting both :scale and :width/:height is forbidden"}))
    (io/make-parents target-path)
    (transform-image* source-path target-path options)
    target-path))
