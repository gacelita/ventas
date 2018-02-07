(ns ventas.seo
  "Prerendering stuff"
  (:require
   [ventas.ws :as ws]
   [ventas.routes :as routes]
   [ventas.common.utils :as common.utils]))

(defn ^:export ready? []
  (and
   (not (ws/requests-pending?))
   (= js/document.readyState "complete")))

(defn ^:export go-to [args]
  (apply routes/go-to
         (map common.utils/process-input-message args)))