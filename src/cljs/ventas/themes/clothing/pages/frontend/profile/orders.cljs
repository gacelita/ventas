(ns ventas.themes.clothing.pages.frontend.profile.orders
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [re-frame.core :as rf]
   [ventas.events :as events]))

(defn page []
  [skeleton
   [:div.login-page
    (let [session @(rf/subscribe [::events/db [:session]])]
      [:div "nothing"])]])

(routes/define-route!
 :frontend.profile.orders
 {:name (i18n ::page)
  :url ["orders"]
  :component page})