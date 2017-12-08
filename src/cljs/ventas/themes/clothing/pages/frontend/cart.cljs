(ns ventas.themes.clothing.pages.frontend.cart
  (:require
   [re-frame.core :as rf]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils :as util]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.events :as events]
   [ventas.events.backend :as backend]))

(def state-key ::state)

(defn cart-line-view []
  )

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} [_]]
   {:dispatch [::backend/users.cart
               {:success [::events/db state-key]}]}))

(defn page []
  (rf/dispatch [::init])
  (fn []
    [skeleton
     [base/container
      [:div.cart-page
       [:h2 (i18n ::cart)]
       (let [cart @(rf/subscribe [::events/db state-key])]
         (js/console.log "the cart" cart)
         [cart-line-view])]]]))

(routes/define-route!
 :frontend.cart
 {:name ::page
  :url ["login"]
  :component page})