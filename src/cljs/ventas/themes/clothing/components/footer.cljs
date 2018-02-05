(ns ventas.themes.clothing.components.footer
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]))

(defn footer []
  (fn []
    [:div.footer
     [:div.ui.container
      [:div.footer__columns

       [:div.footer__column
        [:p (i18n ::footer-text)]
        [:p (i18n ::footer-subtext)]]

       [:div.footer__column
        [:h4 (i18n ::links)]
        [:ul
         [:li
          [:a {:href (routes/path-for :frontend.privacy-policy)}
           (i18n ::privacy-policy)]]]]

       [:div.footer__column
        [:h4 (i18n ::contact)]
        [:p "Phone number: 000 000 000"]
        [:p "Email: my-store@coldmail.com"]]]]]))
