(ns ventas.email-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [ventas.email :as sut]
   [postal.core :as postal]))

(def test-configuration
  {:host "smtp.gmail.com"
   :port 443
   :user "no-reply@kazer.es"
   :pass "test"
   :from "no-reply@kazer.es"
   :ssl true})

(use-fixtures :once
              #(with-redefs [sut/get-config (constantly test-configuration)]
                 (%)))

(deftest send!
  (let [received-args (atom nil)
        message {:subject "Hey!"
                 :body "My message"}]
    (with-redefs [postal/send-message (fn [& args] (reset! received-args args))]
      (sut/send! message)
      (is (= [test-configuration
              (merge message
                     {:from (:from test-configuration)})]
             @received-args)))))