(ns ventas.themes.clothing.components.menu
  (:require
   [ventas.components.menu :as menu]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]))

(defn menu []
  [menu/menu
   [{:text (i18n ::home)
     :href (routes/path-for :frontend)}
    {:text (i18n ::men)
     :href (routes/path-for :frontend.category :id "men")}
    {:text (i18n ::women)
     :href (routes/path-for :frontend.category :id "women")}]])
