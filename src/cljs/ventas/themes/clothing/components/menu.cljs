(ns ventas.themes.clothing.components.menu
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.components.menu :as ventas.menu]))

(defn menu []
  [ventas.menu/menu
   [{:text (i18n ::home)
     :href (routes/path-for :frontend)}
    {:text (i18n ::men)
     :href (routes/path-for :frontend.category :id :root.men)}
    {:text (i18n ::women)
     :href (routes/path-for :frontend.category :id :root.women)}]])