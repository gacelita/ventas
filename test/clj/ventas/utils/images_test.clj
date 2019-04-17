(ns ventas.utils.images-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ventas.utils.images :as sut]
   [ventas.test-tools :refer [with-test-image]]))

(deftest transform-image
  (let [crop-args (atom nil)
        scale-args (atom nil)
        quality-args (atom nil)]
    (with-redefs [sut/source-region (fn [builder x y w h]
                                      (reset! crop-args [x y w h])
                                      (.sourceRegion builder x y w h))
                  sut/scale-to (fn [builder scale]
                                 (reset! scale-args [scale])
                                 (.scale builder scale))
                  sut/output-quality (fn [builder quality]
                                       (reset! quality-args [quality])
                                       (.outputQuality builder quality))]
      (with-test-image
       (fn [image]
         (sut/transform-image (str image)
                              nil
                              {:quality 0.76
                               :resize {:width 50
                                        :height 50}
                               :crop {:relation 1}})))
      (is (= @crop-args [2.5 0.0 95.0 95.0]))
      (is (= @scale-args [1/2]))
      (is (= @quality-args [0.76])))))

(defn round2
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn- crop-test [source-relation target-relation expectation]
  (let [source-region-args (atom nil)
        truncate-values (fn [numbers] (map (partial round2 1) numbers))]
    (with-redefs [sut/source-region (fn [_ & args] (reset! source-region-args args))]
      (#'sut/crop-image nil {:width 100 :height (/ 100 source-relation)} {:relation target-relation})
      (is (= (truncate-values expectation)
             (truncate-values @source-region-args))))))

(deftest crop
  (testing "portrait to portait - lower"
    (crop-test 0.8 0.6 [12.5 0 75 125]))
  (testing "portrait to portrait - higher"
    (crop-test 0.8 0.9 [0 6.9 100 111.1]))
  (testing "portrait to landscape"
    (crop-test 0.8 1.2 [0 20.8 100 83.3]))
  (testing "portrait to square"
    (crop-test 0.8 1 [0 12.5 100 100]))
  (testing "landscape to portrait"
    (crop-test 1.2 0.8 [16.7 0 66.7 83.3]))
  (testing "landscape to landscape - lower"
    (crop-test 1.2 1.4 [0 6 100 71.4]))
  (testing "landscape to landscape - higher"
    (crop-test 1.2 1.1 [4.2 0 91.7 83.3]))
  (testing "landscape to square"
    (crop-test 1.2 1 [8.3 0 83.3 83.3])))
