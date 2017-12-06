(ns ventas.utils.images
  (:require
   [fivetonine.collage.util :as util]
   [fivetonine.collage.core :as collage]
   [clojure.java.io :as io]
   [ventas.utils.files :as utils.files]
   [clojure.set :as set]))

(defn- path-with-metadata [path options]
  (str (utils.files/basename path) "-" (hash options) "." (utils.files/extension path)))

(defn- adapt-dimensions-to-relation* [width height target-relation]
  (if (< 1 target-relation)
    {:width width
     :height (/ width target-relation)}
    {:height height
     :width (* height target-relation)}))

(defn- adapt-dimensions-to-relation [{:keys [width height target-relation]}]
  (let [source-relation (/ width height)]
    (if (or (and (<= target-relation 1) (<= source-relation 1))
            (and (<= 1 target-relation) (<= 1 source-relation)))
      (adapt-dimensions-to-relation* width height target-relation)
      (let [{:keys [width height]} (adapt-dimensions-to-relation
                                    {:width width
                                     :height height
                                     :target-relation 1})]
        (adapt-dimensions-to-relation* width height target-relation)))))

(defn- crop-image [image {:keys [offset size relation] :as options}]
  (if relation
    (let [source-width (.getWidth image)
          source-height (.getHeight image)
          {:keys [width height]} (adapt-dimensions-to-relation
                                  {:width source-width
                                   :height source-height
                                   :target-relation relation})]
      (recur image {:offset [(- (/ source-width 2) (/ width 2))
                             (- (/ source-height 2) (/ height 2))]
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