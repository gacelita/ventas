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

(deftest send-template!
  (let [received-args (atom nil)
        subject "Hey!"
        template "Test body"]
    (defmethod sut/template-body :test-template [_ _]
      template)
    (with-redefs [postal/send-message (fn [& args] (reset! received-args args))]
      (sut/send-template! :test-template {:subject subject})
      (is (= [test-configuration
              {:body [{:content template :type "text/html; charset=utf-8"}]
               :from (:from test-configuration)
               :subject subject}]
             @received-args)))))