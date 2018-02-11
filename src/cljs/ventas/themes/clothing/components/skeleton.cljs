(ns ventas.themes.clothing.components.skeleton
  (:require
   [ventas.components.base :as base]
   [ventas.components.breadcrumbs :as breadcrumbs]
   [ventas.components.cookies :as cookies]
   [ventas.components.notificator :as notificator]
   [ventas.components.popup :as popup]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.clothing.components.footer :as footer]
   [ventas.themes.clothing.components.header :as header]
   [ventas.themes.clothing.components.menu :as menu]
   [ventas.themes.clothing.components.preheader :as preheader]))

(defn skeleton [contents]
  [:div.root
   [notificator/notificator]
   [popup/popup]
   [cookies/cookies (i18n ::cookies)]
   [:div.root__wrapper
    [preheader/preheader]
    [header/header]
    [menu/menu]
    [base/container {:class "breadcrumbs-wrapper"}
     [breadcrumbs/breadcrumbs]]
    [base/divider]
    [:div.page-wrapper
     contents]
    [footer/footer]]])
