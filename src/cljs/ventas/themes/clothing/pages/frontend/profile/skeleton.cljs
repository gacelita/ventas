(ns ventas.themes.clothing.pages.frontend.profile.skeleton
  (:require
   [ventas.components.sidebar :as sidebar]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.utils :as utils]
   [ventas.themes.clothing.components.skeleton :as ventas.skeleton]
   [ventas.events :as events]))

(defn sidebar []
  [sidebar/sidebar

   [sidebar/sidebar-section {:name (i18n ::my-profile)}

    [sidebar/link {:href (routes/path-for :frontend.profile.orders)}
     (i18n ::my-orders)]

    [sidebar/link {:href (routes/path-for :frontend.profile.addresses)}
     (i18n ::my-addresses)]

    [sidebar/link {:href (routes/path-for :frontend.profile.account)}
     (i18n ::my-account)]

    [sidebar/link {:on-click #(rf/dispatch [::events/session.stop])}
     (i18n ::logout)]]])

(defn skeleton [content]
  [ventas.skeleton/skeleton
   [:div.ui.container.profile-skeleton
    [sidebar]
    [:div.profile-skeleton__content
     content]]])