(ns ventas.themes.clothing.pages.frontend.profile.orders
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.themes.clothing.pages.frontend.profile.skeleton :as profile.skeleton]
   [ventas.session :as session]))



(defn content [identity]
  [:div.login-page
   (let [session @(rf/subscribe [::events/db [:session]])]
     [:div "Nothing yet!"])])

(defn page []
  [profile.skeleton/skeleton
   [content (session/get-identity)]])

(routes/define-route!
 :frontend.profile.orders
 {:name ::page
  :url ["orders"]
  :component page})