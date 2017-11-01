(ns ventas.pages.admin.skeleton
  (:require
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]))

(defn- menu []
  [:ul
   [:li [:a {:href (routes/path-for :admin.users)} (i18n ::users)]]
   [:li [:a {:href (routes/path-for :admin.products)} (i18n ::products)]]
   [:li [:a {:href (routes/path-for :admin.plugins)} (i18n ::plugins)]]])

(defn skeleton [content]
  [:div.admin__skeleton
   [:div.admin__sidebar
    [:a {:href (routes/path-for :admin)}
     [:h3 (i18n ::administration)]]
    [menu]]
   [:div.admin__content
    content]])
