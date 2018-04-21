(ns ventas.events-test
  (:require
   [clojure.core.async :as core.async :refer [<! >! go]]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.events :as sut]))

(deftest register-pub-sub
  (let [ch (go
             (let [data {:some :data}]
               (sut/register-event! :test)
               (is (= (keys (sut/event :test)) [:chan :mult]))
               (let [ch1 (sut/sub :test)
                     ch2 (sut/sub :test)]
                 (>! (sut/pub :test) data)
                 (is (= data
                        (<! ch1)))
                 (is (= data
                        (<! ch2))))))]
    (core.async/alts!! [ch (core.async/timeout 1000)])))
