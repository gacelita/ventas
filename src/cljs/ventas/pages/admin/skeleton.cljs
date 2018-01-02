(ns ventas.pages.admin.skeleton
  (:require
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]))

(def configuration-items
  [{:route :admin.configuration.image-sizes :label ::image-sizes}])

(def menu-items
  [{:route :admin.users :label ::users}
   {:route :admin.products :label ::products}
   {:route :admin.plugins :label ::plugins}
   {:route :admin.taxes :label ::taxes}
   {:route :admin.activity-log :label ::activity-log}
   {:route :admin.configuration.image-sizes
    :label ::configuration
    :children configuration-items}])

(defn- menu-item [{:keys [route label children]}]
  [:li
   [:a {:href (routes/path-for route)}
    (i18n label)]
   (when children
     [:ul
      (for [child children]
        ^{:key (hash child)} [menu-item child])])])

(defn- menu []
  [:ul
   (for [item menu-items]
     ^{:key (hash item)} [menu-item item])])

(defn skeleton [content]
  [:div.admin__skeleton
   [:div.admin__sidebar
    [:a {:href (routes/path-for :admin)}
     [:h3 (i18n ::administration)]]
    [menu]]
   [:div.admin__content
    content]])
