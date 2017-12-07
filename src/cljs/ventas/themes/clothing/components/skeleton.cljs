(ns ventas.themes.clothing.components.skeleton
  (:require
   [reagent.core :refer [atom]]
   [re-frame.core :as rf]
   [ventas.components.notificator :as ventas.notificator]
   [ventas.components.popup :as ventas.popup]
   [ventas.components.cookies :as ventas.cookies]
   [ventas.components.menu :as ventas.menu]
   [ventas.components.breadcrumbs :as ventas.breadcrumbs]
   [ventas.themes.clothing.components.header :refer [header]]
   [ventas.themes.clothing.components.menu :as menu]
   [ventas.themes.clothing.components.footer :refer [footer]]
   [ventas.themes.clothing.components.preheader :refer [preheader]]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]))

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