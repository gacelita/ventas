(ns ventas.utils-test
  (:require
   [clojure.core.async :as core.async :refer [go <! >!]]
   [clojure.test :refer [deftest is testing]]
   [ventas.utils :as sut]
   [clojure.spec.alpha :as spec]))

(deftest chan?
  (is (sut/chan? (core.async/chan))))

(deftest swallow
  (is (= nil (sut/swallow (throw (Exception. "Test"))))))

(deftest spec-exists?
  (spec/def ::example integer?)
  (is (not (sut/spec-exists? ::does-not-exist)))
  (is (sut/spec-exists? ::example)))

(deftest dequalify-keywords
  (is (= {:a :b
          :c :d}
         (sut/dequalify-keywords {:ns/a :b
                                  :ns/c :d}))))

(deftest qualify-keyword
  (is (= :some-other.ns/test
         (sut/qualify-keyword :test :some-other.ns))))

(deftest qualify-map-keywords
  (is (= {::a :b
          ::c :d}
         (sut/qualify-map-keywords {:a :b
                                    :c :d}
                                   (namespace ::kw)))))

(deftest transform
  (is (= {:n 0
          :more :data}
         (sut/transform
          {:n 0}
          [(fn [data]
             (update data :n inc))
           (fn [data]
             (assoc data :more :data))
           (fn [data]
             (update data :n dec))]))))

(deftest check
  (spec/def ::test-spec.a integer?)
  (spec/def ::test-spec
    (spec/keys :req [::test-spec.a]))
  (is (sut/check ::test-spec {::test-spec.a 5}))
  (is (thrown? Exception
               (sut/check ::test-spec {::test-spec.a "test"}))))

(deftest update-if-exists
  (is (= {:a 1}
         (sut/update-if-exists {:a 0} :a inc)))
  (is (= {:a 0}
         (sut/update-if-exists {:a 0} :b inc))))

(deftest has-duplicates?
  (is (sut/has-duplicates? [:a :b :c :b]))
  (is (not (sut/has-duplicates? [:a :b :c]))))

(deftest ns-kw
  (is (thrown? Exception (sut/ns-kw "kw"))))

(deftest bigdec?
  (is (sut/bigdec? 5M)))

(deftest ->number
  (is (= 5
         (sut/->number "5"))))

(deftest batch
  (let [in (core.async/chan (core.async/sliding-buffer 10))
        out (core.async/chan)
        _ (sut/batch in out 50 5)
        ch (go
            (dotimes [_ 7]
              (>! in true))
            (is (= (repeat 5 true) (<! out)))
            (is (= (repeat 2 true) (<! out))))]
    (core.async/alts!! [ch (core.async/timeout 500)])))