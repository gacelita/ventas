(ns ventas.themes.clothing.pages.frontend.profile
  (:require
   [ventas.themes.clothing.pages.frontend.profile.account]
   [ventas.themes.clothing.pages.frontend.profile.addresses]
   [ventas.themes.clothing.pages.frontend.profile.orders]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.components.sidebar :as sidebar]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.components.base :as base]))

(defn content [user]
  [:div.profile-page__content

   [:h2.profile-page__name (str (i18n ::welcome (:name user)) "!")]

   [base/segment
    [:h4 (i18n ::my-orders)]
    [:p (i18n ::my-orders-explanation)]]

   [base/segment
    [:h4 (i18n ::my-addresses)]
    [:p (i18n ::my-addresses-explanation)]]

   [base/segment
    [:h4 (i18n ::my-account)]
    [:p (i18n ::personal-data-explanation)]]])

(defn page []
  [skeleton
   (let [session @(rf/subscribe [:ventas/db [:session]])]
     (if (get-in session [:identity :id])
       (let [user (:identity session)]
         [:div.ui.container.profile-page
          [sidebar/sidebar

           [sidebar/sidebar-section {:name (i18n ::my-profile)}

            [sidebar/link {:href (routes/path-for :frontend.my-orders)}
             (i18n ::my-orders)]

            [sidebar/link {:href (routes/path-for :frontend.my-addresses)}
             (i18n ::my-addresses)]

            [sidebar/link {:href (routes/path-for :frontend.my-profile)}
             (i18n ::my-account)]

            [sidebar/link {:on-click #(rf/dispatch [:ventas/session.stop])}
             (i18n ::logout)]]]
          [content user]])
       (routes/go-to :frontend.login)))])

(routes/define-route!
 :frontend.profile
 {:name (i18n ::page)
  :url ["profile"]
  :component page})