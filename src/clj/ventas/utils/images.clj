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

(defn- path-with-metadata [path quality options]
  (let [extension (extension path)]
    (str "-" quality
         (if-let [scale (:scale options)] (str "-x" scale) "")
         (if-let [width (:width options)] (str "-w" width) "")
         (if-let [height (:height options)] (str "-h" height) "")
         (if-let [crop (:crop options)]
           (if (= crop :square)
             (str "-csquare")
             (str "-c" (-> crop :offset first) "x" (-> crop :offset second) "x" (-> crop :size first) "x" (-> crop :size second))))
         (case (:progressive options)
           true "-p" ;; progressive
           false "-b" ;; baseline
           nil "")
         "." extension)))

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

(defn- transform-image* [source-path target-path quality options]
  (-> (util/load-image source-path)
      (cond->
       (:crop options) (crop-image (:crop options))
       (:scale options) (collage/scale (:scale options))
       (or (:width options) (:height options)) (collage/resize :width (:width options) :height (:height options)))
      (util/save target-path
                 :quality quality
                 :progressive (:progressive options))))

(defn transform-image [source-path target-dir quality & [options]]
  (when (and (:scale options) (or (:width options) (:height options)))
    (throw (Exception. "Setting both :scale and :width / :height does not compute.")))
  (let [target-dir (or target-dir (get-tmp-dir))
        target-path (str target-dir "/" (path-with-metadata source-path quality options))]
    (if (.exists (io/file target-path))
      target-path
      (transform-image* source-path target-path quality options))))


