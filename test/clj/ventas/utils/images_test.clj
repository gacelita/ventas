(ns ventas.utils.images-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ventas.utils.images :as sut]
   [fivetonine.collage.core :as collage])
  (:import [java.awt.image BufferedImage]))

(defn buffered-image
  "Taken from https://github.com/karls/collage/blob/master/test/fivetonine/collage/helpers.clj"
  ([w] (BufferedImage. w w BufferedImage/TYPE_INT_ARGB))
  ([w h] (BufferedImage. w h BufferedImage/TYPE_INT_ARGB))
  ([w h t] (BufferedImage. w h t)))

(deftest transform-image
  (let [crop-args (atom nil)
        scale-args (atom nil)]
    (with-redefs [collage/crop (fn [& args] (reset! crop-args args) (first args))
                  collage/scale (fn [& args] (reset! scale-args args) (first args))]
      (sut/transform-image "storage/logo.png"
                           nil
                           (cond-> {:quality (rand)
                                    :progressive true
                                    :resize {:width 50
                                             :height 50}
                                    :crop {:relation 1}}))
      (is (= (rest @crop-args) [2.5 0.0 95.0 95.0]))
      (is (= (rest @scale-args) [1/2])))))

(defn- crop-test [source-relation target-relation expectation]
  (let [image
        (#'sut/crop-image (buffered-image 100 (/ 100 source-relation))
         {:relation target-relation})]
    (is (= expectation [(.getWidth image) (.getHeight image)]))))

(deftest crop
  (testing "portrait to portait - lower"
    (crop-test 0.8 0.6 [74 125]))
  (testing "portrait to portrait - higher"
    (crop-test 0.8 0.9 [100 111]))
  (testing "portrait to landscape"
    (crop-test 0.8 1.2 [100 83]))
  (testing "portrait to square"
    (crop-test 0.8 1 [100 100]))
  (testing "landscape to portrait"
    (crop-test 1.2 0.8 [66 83]))
  (testing "landscape to landscape - lower"
    (crop-test 1.2 1.4 [100 71]))
  (testing "landscape to landscape - higher"
    (crop-test 1.2 1.1 [91 83]))
  (testing "landscape to square"
    (crop-test 1.2 1 [83 83])))
