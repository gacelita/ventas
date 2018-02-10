(ns ventas.themes.clothing.components.skeleton
  (:require
   [reagent.core :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.breadcrumbs :as ventas.breadcrumbs]
   [ventas.components.cookies :as ventas.cookies]
   [ventas.components.notificator :as ventas.notificator]
   [ventas.components.popup :as ventas.popup]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.clothing.components.footer :refer [footer]]
   [ventas.themes.clothing.components.header :refer [header]]
   [ventas.themes.clothing.components.menu :as menu]
   [ventas.themes.clothing.components.preheader :refer [preheader]]))

(defn skeleton [contents]
  [:div.root
   [ventas.notificator/notificator]
   [ventas.popup/popup]
   [ventas.cookies/cookies (i18n ::cookies)]
   [:div.root__wrapper
    [preheader]
    [header]
    [menu/menu]
    [base/container {:class "breadcrumbs-wrapper"}
     [ventas.breadcrumbs/breadcrumbs]]
    [base/divider]
    [:div.page-wrapper
     contents]
    [footer]]])
