(ns ventas.seo
  "Prerendering stuff"
  (:require
   [cljs.pprint :as pprint]
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]
   [ventas.events :as events]
   [ventas.routes :as routes]
   [ventas.ws :as ws]))

(defn rendered? []
  (js/document.querySelector "#app > #main"))

(defn ^:export ready? []
  (and
   (not (ws/requests-pending?))
   (rendered?)
   (= js/document.readyState "complete")))

(defn ^:export go-to [args]
  (apply routes/go-to
         (map common.utils/process-input-message args)))

(defn ^:export dump-db []
  (with-out-str
   (pprint/pprint @(re-frame.core/subscribe [::events/db]))))