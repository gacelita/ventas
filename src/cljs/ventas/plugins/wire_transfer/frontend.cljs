(ns ventas.plugins.wire-transfer.frontend
  (:require
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [ventas.components.payment :as payment]))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::events/configuration.get #{:stripe.public-key}]}))

(defn wire-transfer
  "@TODO Remove form-2 dispatch antipattern"
  []
  (rf/dispatch [::init])
  (fn []
    [:div.wire-transfer
     [:h2 (i18n ::wire-transfer)]]))

(payment/add-method
 :stripe
 {:component wire-transfer})
