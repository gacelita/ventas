(ns ventas.themes.clothing.pages.frontend.favorites
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.product-list :as product-list]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :as theme.skeleton]))

(def state-key ::state)

(defn page []
  [theme.skeleton/skeleton
   [base/container
    [:div.favorites-page
     [product-list/product-list
      @(rf/subscribe [::events/db state-key])]]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::backend/users.favorites.list
               {:success [::events/db state-key]}]}))

(routes/define-route!
 :frontend.favorites
 {:name ::page
  :url ["favorites"]
  :component page
  :init-fx [::init]})
