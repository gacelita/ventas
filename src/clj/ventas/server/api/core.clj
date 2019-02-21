(ns ventas.server.api.core
  (:require
   [ventas.server.api.admin]
   [ventas.server.api.description]
   [ventas.server.api.user]
   [ventas.server.api :as api]))

(defmacro define-api-events-for-ns! []
  (let [ns-name (str (:name (:ns &env)))
        endpoints (->> (keys @api/available-requests)
                       (filter #(= (namespace %) ns-name)))]
    `(doseq [~'endpoint [~@endpoints]]
       (~'re-frame.core/reg-event-fx
        ~'endpoint
        (~'fn [~'_ [~'_ ~'options]]
         {:ws-request (~'merge {:name ~'endpoint}
                       ~'options)})))))