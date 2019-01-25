(ns ventas.themes.admin.customization
  (:require
   [re-frame.core :as rf]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.themes.admin.customization.customize]
   [ventas.themes.admin.customization.menus]
   [ventas.themes.admin.configuration :as admin.configuration]
   [ventas.routes :as routes])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(defn get-items []
  (->> @(rf/subscribe [::admin.skeleton/menu-items])
       (filter #(= (:route %) :admin.customization))
       first
       :children))

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-customization__page
    [admin.configuration/menu (get-items)]]])

(routes/define-route!
 :admin.customization
 {:name ::page
  :url "customization"
  :component page})