(ns ventas.themes.admin.configuration
  "Mobile-only page listing configuration sections"
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.components.base :as base]))

(defn menu-item [{:keys [route label icon]}]
  [base/list-item {:href (routes/path-for route)}
   (when icon
     [base/list-icon {:name icon :size "large" :vertical-align "middle"}])
   [base/list-content
    [base/list-header
     (i18n label)]]])

(defn get-items []
  (->> @(rf/subscribe [::admin.skeleton/menu-items])
       (filter :configuration?)))

(defn menu [items]
  [:div.admin-configuration__menu
   [base/list {:divided true :relaxed true}
    (for [item items]
      [menu-item item])]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-configuration__page
    [menu (get-items)]]])

(routes/define-route!
  :admin.configuration
  {:name ::page
   :url "configuration"
   :component page})
