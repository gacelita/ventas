(ns ventas.subscriptions.products
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :products
 (fn [db [_ id]] (-> db :products (get id))))