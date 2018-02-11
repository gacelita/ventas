(ns ventas.themes.clothing.components.footer
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.components.base :as base]))

(defn footer []
  [:div.footer
   [base/container
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
      [:p "Email: my-store@coldmail.com"]]]]])
