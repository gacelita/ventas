(ns ventas.themes.clothing.components.footer
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]
   [ventas.events :as events]))

(defn footer []
  (fn []
    [:div.footer
     [:div.ui.container
      [:div.footer__columns
       [:div.footer__column
        [:p (i18n ::footer-text)]]
       [:div.footer__column
        [:h4 (i18n ::links)]
        [:ul
         [:li
          [:a {:href (routes/path-for :frontend.privacy-policy)}
           (i18n ::privacy-policy)]]]]
       [:div.footer__column
        [:h4 (i18n ::contact)]
        [:br]
        (let [email @(rf/subscribe [::events/db [:configuration :email]])]
          [:p (i18n ::email) ":"]
          [:a {:href (str "mailto:" email)}
           email])]]]]))