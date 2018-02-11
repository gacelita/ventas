(ns ventas.themes.clothing.components.preheader
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.components.base :as base]))

(defn preheader []
  [:div.preheader
   [base/container
    [:div.preheader__item
     [:strong (i18n ::support-and-orders)]
     [:a "666 555 444"]]
    [:div.preheader__separator "-"]
    [:div.preheader__item
     [:strong (i18n ::schedule)]
     [:span (i18n ::schedule-info)]]]])
