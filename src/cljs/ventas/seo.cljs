(ns ventas.seo
  "Prerendering stuff"
  (:require
   [cljs.reader :as reader]
   [ventas.routes :as routes]
   [ventas.ws :as ws]
   [ventas.events :as events]
   [cljs.pprint :as pprint]))

(defn ^:export go-to [args]
  (apply routes/go-to
         (reader/read-string args)))

(defn rendered? []
  (js/document.querySelector "#app > #main"))

(defn ^:export ready? []
  (and
   (not (ws/requests-pending?))
   (rendered?)
   (= js/document.readyState "complete")))



(defn ^:export dump-db []
  (with-out-str
   (pprint/pprint @(re-frame.core/subscribe [::events/db]))))