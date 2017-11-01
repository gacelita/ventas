(ns ventas.actions.products
  (:require [ventas.util :as util :refer [value-handler]]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :products/get
 (fn [cofx [_ id]]
   {:ws-request {:name :products/get
                 :params {:id id}
                 :success-fn #(rf/dispatch [:ventas/db [:products (:id %1)] %1])}}))
