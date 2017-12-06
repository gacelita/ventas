(ns ventas.utils.images
  (:require
   [fivetonine.collage.util :as util]
   [fivetonine.collage.core :as collage]
   [clojure.java.io :as io]
   [ventas.utils.files :as utils.files]
   [clojure.set :as set]))

(defn- path-with-metadata [path options]
  (str (utils.files/basename path) "-" (hash options) "." (utils.files/extension path)))

(defn- portrait? [relation]
  (< relation 1))

(defn- landscape? [relation]
  (<= 1 relation))

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
    (if (or (and (landscape? target-relation) (landscape? source-relation))
            (and (portrait? target-relation) (portrait? source-relation)))
      (adapt-dimensions-to-relation* {:width width :height height} target-relation)
      (let [{:keys [width height]} (adapt-dimensions-to-relation
                                    {:width width
                                     :height height
                                     :target-relation 1})]
        (adapt-dimensions-to-relation* {:width width :height height} target-relation)))))

(defn- crop-image [image {:keys [offset size relation] :as options}]
  (if relation
    (let [relation (* 1.0 relation)
          source-width (.getWidth image)
          source-height (.getHeight image)
          {:keys [width height]} (adapt-dimensions-to-relation
                                  {:width source-width
                                   :height source-height
                                   :target-relation relation})]
      (recur image {:offset [(- (/ source-width 2.0) (/ width 2.0))
                             (- (/ source-height 2.0) (/ height 2.0))]
                    :size [width height]}))
    (collage/crop image
                  (first offset)
                  (second offset)
                  (first size)
                  (second size))))

(defn- resize-image* [image width height]
  (let [width-scale (/ width (.getWidth image))
        height-scale (/ height (.getHeight image))]
    (collage/scale image (min width-scale height-scale))))

(defn- resize-image [image {:keys [width height allow-smaller?]}]
  "Limits the size of `image` to the specified `width` and `height`, without altering
   image ratio.
   Extra options:
     allow-smaller?: if the image is within the defined bounds, do nothing to it"
  (if allow-smaller?
    (let [w (.getWidth image)
          h (.getHeight image)]
      (if (and (< w width) (< h height))
        image
        (resize-image* image width height)))
    (resize-image* image width height)))

(defn- transform-image* [source-path target-path {:keys [quality progressive crop scale resize] :as options}]
  (-> (util/load-image source-path)
      (cond->
       crop (crop-image crop)
       scale (collage/scale scale)
       resize (resize-image resize))
      (util/save target-path
                 :quality quality
                 :progressive progressive)))

(defn transform-image [source-path target-dir & [options]]
  (let [options (merge-with #(if (nil? %1) %2 %1) {:quality 1} options)
        target-dir (or target-dir (utils.files/get-tmp-dir))
        target-path (str target-dir "/" (path-with-metadata source-path options))]
    (when (and (:scale options) (or (get-in options [:resize :width])
                                    (get-in options [:resize :height])))
      (throw (Exception. "Setting both :scale and :width / :height does not compute.")))
    (io/make-parents target-path)
    (if (.exists (io/file target-path))
      target-path
      (transform-image* source-path target-path options))))