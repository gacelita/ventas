(ns ventas.logging-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [io.aviso.ansi :as clansi]
   [ventas.logging :as sut]))

(deftest appender
  (testing "filters blacklisted namespaces"
    (with-redefs [sut/blacklisted? (fn [_ ns]
                                     (= ns (str *ns*)))]
      (is (= nil
             (#'sut/timbre-logger {:level :debug
                                   :?ns-str (str *ns*)})))))
  (testing "properly logs strings"
    (is (= {:level "DEBUG"
            :value "My message"
            :where "[example.namespace:79]"}
           (#'sut/timbre-logger {:level :debug
                                 :?ns-str "example.namespace"
                                 :?line 79
                                 :msg_ (delay "My message")}))))
  (testing "recognizes clojure structures"
    (is (= {:level "DEBUG"
            :value [{:example :data} [:more :data]]
            :where "[example.namespace:79]"}
           (#'sut/timbre-logger {:level :debug
                                 :?ns-str "example.namespace"
                                 :?line 79
                                 :msg_ (delay (str (pr-str {:example :data}) " " [:more :data]))}))))
  (testing "avoids using read-string when there are unquoted strings present"
    (is (= {:level "DEBUG"
            :value "{:example :data} [:more :data] and a string"
            :where "[example.namespace:79]"}
           (#'sut/timbre-logger {:level :debug
                                 :?ns-str "example.namespace"
                                 :?line 79
                                 :msg_ (delay (str (pr-str {:example :data}) " " [:more :data] " and a string"))})))))

(deftest logger
  (testing "does not log blank strings or nils"
    (let [received-args (atom nil)]
      (with-redefs [println (fn [& args] (reset! received-args args))]
        (#'sut/timbre-appender {:output_ (delay {:level "DEBUG"
                                                 :where "[example.namespace:79]"
                                                 :value ""})})
        (is (= nil
               @received-args)))))

  (testing "properly logs strings"
    (let [received-args (atom nil)]
      (with-redefs [println (fn [& args] (reset! received-args args))]
        (#'sut/timbre-appender {:output_ (delay {:level "DEBUG"
                                                 :where "[example.namespace:79]"
                                                 :value "Test"})})
        (is (= [(str (clansi/green "DEBUG [example.namespace:79] - ") "Test")]
               @received-args)))))

  (testing "properly logs clojure structures"
    (let [received-args (atom nil)]
      (with-redefs [println (fn [& args] (reset! received-args args))]
        (#'sut/timbre-appender {:output_ (delay {:level "DEBUG"
                                                 :where "[example.namespace:79]"
                                                 :value [{:example [:test :data]}]})})
        (is (= [(str (clansi/green "DEBUG [example.namespace:79] - ")
                     "{:example [:test :data]}")]
               @received-args)))))

  (testing "properly logs clojure structures spanning multiple lines"
    (let [received-args (atom nil)]
      (with-redefs [println (fn [& args] (reset! received-args args))]
        (#'sut/timbre-appender {:output_ (delay {:level "DEBUG"
                                                 :where "[example.namespace:79]"
                                                 :value [{:example {:of [:nested :data]
                                                                    :that {:should [:span {:several :lines}]}}}]})})
        (is (= [(str (clansi/green "DEBUG [example.namespace:79] - ")
                     "{:example\n                                {:of [:nested :data], :that {:should [:span {:several :lines}]}}}")]
               @received-args))))))
