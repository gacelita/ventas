(ns ventas.utils.images
  (:require
   [fivetonine.collage.util :as util]
   [fivetonine.collage.core :as collage]
   [clojure.java.io :as io]
   [ventas.utils.files :as utils.files]
   [clojure.set :as set]))

(defn- path-with-metadata [path options]
  (str (utils.files/basename path) "-" (:quality options)
       (if-let [scale (:scale options)] (str "-x" scale) "")
       (if-let [width (:width options)] (str "-w" width) "")
       (if-let [height (:height options)] (str "-h" height) "")
       (if-let [crop (:crop options)]
         (if (= crop :square)
           (str "-csquare")
           (str "-c" (-> crop :offset first) "x" (-> crop :offset second) "x" (-> crop :size first) "x" (-> crop :size second))))
       (if (:progressive options) "-p" "-b")
       "." (utils.files/extension path)))

(defn- crop-image [image options]
  (if (= options :square)
    (let [w (.getWidth image)
          h (.getHeight image)
          min-dimension (min w h)
          cropped-offset (- (/ (max w h) 2) (/ min-dimension 2))]
      (recur image {:offset (if (> w h) [cropped-offset 0] [0 cropped-offset]) :size [min-dimension min-dimension]}))
    (collage/crop image
                  (-> options :offset first)
                  (-> options :offset second)
                  (-> options :size first)
                  (-> options :size second))))

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

(defn transform-image [source-path target-dir & [{:keys [quality] :as options}]]
  (let [options (merge {:quality 1} options)
        target-dir (or target-dir (utils.files/get-tmp-dir))
        target-path (str target-dir "/" (path-with-metadata source-path options))]
    (when (and (:scale options) (or (get-in options [:resize :width])
                                    (get-in options [:resize :height])))
      (throw (Exception. "Setting both :scale and :width / :height does not compute.")))
    (io/make-parents target-path)
    (if (.exists (io/file target-path))
      target-path
      (transform-image* source-path target-path options))))