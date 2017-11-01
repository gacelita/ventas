(ns ventas.themes.mariscosriasbajas.components.skeleton
  (:require
   [reagent.core :refer [atom]]
   [re-frame.core :as rf]
   [ventas.components.notificator :as ventas.notificator]
   [ventas.components.popup :as ventas.popup]
   [ventas.components.cookies :as ventas.cookies]
   [ventas.components.menu :as ventas.menu]
   [ventas.components.breadcrumbs :as ventas.breadcrumbs]
   [ventas.themes.mariscosriasbajas.components.header :refer [header]]
   [ventas.themes.mariscosriasbajas.components.footer :refer [footer]]
   [ventas.themes.mariscosriasbajas.components.preheader :refer [preheader]]
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
    [ventas.menu/menu [{:text (i18n ::home) :href (routes/path-for :frontend)}
                       {:text (i18n ::seafood) :href (routes/path-for :frontend.category :id 1)}]]
    [base/container
     [ventas.breadcrumbs/breadcrumbs]]
    [base/divider]
    contents
    [footer]]])