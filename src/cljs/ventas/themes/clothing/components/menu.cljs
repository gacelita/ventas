(ns ventas.themes.clothing.components.menu
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.components.menu :as ventas.menu]))

(defn menu []
  [ventas.menu/menu
   [{:text (i18n ::home)
     :href (routes/path-for :frontend)}
    {:text (i18n ::man)
     :href (routes/path-for :frontend.category :id :man)}
    {:text (i18n ::woman)
     :href (routes/path-for :frontend.category :id :woman)}]])