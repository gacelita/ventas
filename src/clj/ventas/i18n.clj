(ns ventas.i18n
  (:require
   [tongue.core :as tongue]))

(def ^:private dicts
  {:en_US
   {:ventas.email/new-pending-order "New pending order"}})

(def translation-fn (tongue/build-translate dicts))

(defn i18n [kw]
  (translation-fn :en_US kw))