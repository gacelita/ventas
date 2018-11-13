(ns ventas.themes.clothing.components.footer
  (:require
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.events :as events]
   [ventas.components.crud-form :as crud-form]
   [ventas.widget :as widget]
   [ventas.components.form :as form]
   [ventas.utils.ui :as utils.ui]
   [re-frame.core :as rf]))

(def state-key ::state)
(def config-path [state-key :config])

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

(rf/reg-event-fx
 ::config.init
 (fn [_ _]
   {:dispatch [::crud-form/init config-path :clothing.footer]}))

(defn config []
  (let [{{:keys [culture]} :identity} @(rf/subscribe [::events/db [:session]])]
    [form/form config-path
     [base/form {:on-submit (utils.ui/with-handler
                             #(println "done"))}
      [base/segment {:color "orange"
                     :title "Tax"}
       (crud-form/field
        config-path
        {:key :tax/name
         :type :i18n
         :culture culture})

       (crud-form/field
        config-path
        {:key :tax/amount
         :type :amount})

       (crud-form/field
        config-path
        {:key [:tax/kind :db/id]
         :type :combobox
         :options @(rf/subscribe [::events/db [:enums :tax.kind]])})]]]))

(widget/register!
 ::footer
 {:name ::footer
  :frontend {:component footer}
  :config {:init ::config.init
           :component config}})