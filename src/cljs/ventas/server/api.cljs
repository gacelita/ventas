(ns ventas.server.api
  (:require
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils])
  (:require-macros
   [ventas.server.api.core :refer [define-api-events-for-ns!]]))

(define-api-events-for-ns!)

(rf/reg-event-fx
 ::categories.list
 (fn [_ [_ options]]
   {:ws-request (common.utils/deep-merge
                 {:name ::categories.list
                  :params {:pagination {:page 0 :items-per-page 5}}}
                 options)}))

(rf/reg-event-fx
 ::entities.find
 (fn [_ [_ id options]]
   {:ws-request (merge {:name ::entities.find
                        :params {:id id}}
                       options)}))

(rf/reg-event-fx
 ::products.list
 (fn [_ [_ options]]
   {:ws-request (common.utils/deep-merge
                 {:name ::products.list
                  :params {:pagination {:page 0 :items-per-page 5}}}
                 options)}))
