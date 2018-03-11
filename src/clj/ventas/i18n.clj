(ns ventas.i18n
  (:require
   [tongue.core :as tongue]))

(def ^:private dicts
  {:en_US
   {:ventas.email/new-pending-order "New pending order"
    :ventas.i18n/test-value "Test value"}})

(def ^:private translation-fn (tongue/build-translate dicts))

(defn i18n [kw]
  (translation-fn :en_US kw))