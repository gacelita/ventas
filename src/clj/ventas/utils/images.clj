(ns ventas.utils.images
  (:require
   [fivetonine.collage.util :as util]
   [fivetonine.collage.core :as collage]
   [clojure.java.io :as io]
   [clojure.set :as set])
  (:import (org.apache.commons.io FilenameUtils)))

(defn get-tmp-dir []
  (System/getProperty "java.io.tmpdir"))

(defn extension [s]
  (FilenameUtils/getExtension s))

(defn basename [s]
  (FilenameUtils/getBaseName s))

(defn- path-with-metadata [path options]
  (str (basename path) "-" (:quality options)
       (if-let [scale (:scale options)] (str "-x" scale) "")
       (if-let [width (:width options)] (str "-w" width) "")
       (if-let [height (:height options)] (str "-h" height) "")
       (if-let [crop (:crop options)]
         (if (= crop :square)
           (str "-csquare")
           (str "-c" (-> crop :offset first) "x" (-> crop :offset second) "x" (-> crop :size first) "x" (-> crop :size second))))
       (if (:progressive options) "-p" "-b")
       "." (extension path)))

(defn- crop-image [image options]
  (if (= options :square)
    (let [w (.getWidth image)
          h (.getHeight image)
          min-dimension (min w h)
          cropped-offset (- (/ (max w h) 2) (/ min-dimension 2))]
      (recur image {:offset (if (> w h) [cropped-offset 0] [0 cropped-offset]) :size [min-dimension min-dimension]}))
    (collage/crop image (-> options :offset first)
                  (-> options :offset second)
                  (-> options :size first)
                  (-> options :size second))))

(defn- transform-image* [source-path target-path {:keys [quality progressive width height crop scale] :as options}]
  (-> (util/load-image source-path)
      (cond->
       crop (crop-image crop)
       scale (collage/scale scale)
       (or width height) (collage/resize :width width :height height))
      (util/save target-path
                 :quality quality
                 :progressive progressive)))

(defn transform-image [source-path target-dir & [{:keys [quality] :as options}]]
  (let [options (merge {:quality 1} options)
        target-dir (or target-dir (get-tmp-dir))
        target-path (str target-dir "/" (path-with-metadata source-path options))]
    (when (and (:scale options) (or (:width options) (:height options)))
      (throw (Exception. "Setting both :scale and :width / :height does not compute.")))
    (if (.exists (io/file target-path))
      target-path
      (transform-image* source-path target-path options))))


