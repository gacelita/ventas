(ns ventas.pages.admin.skeleton
  (:require
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]))

(def menu-items
  [{:route :admin.users :label ::users}
   {:route :admin.products :label ::products}
   {:route :admin.plugins :label ::plugins}
   {:route :admin.taxes :label ::taxes}])

(defn- menu []
  [:ul
   (map-indexed
    (fn [idx {:keys [route label]}]
      [:li {:key idx}
       [:a {:href (routes/path-for route)}
        (i18n label)]])
    menu-items)])

(defn skeleton [content]
  [:div.admin__skeleton
   [:div.admin__sidebar
    [:a {:href (routes/path-for :admin)}
     [:h3 (i18n ::administration)]]
    [menu]]
   [:div.admin__content
    content]])
