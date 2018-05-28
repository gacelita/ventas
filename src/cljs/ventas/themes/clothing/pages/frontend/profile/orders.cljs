(ns ventas.themes.clothing.pages.frontend.profile.orders
  (:require
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.session :as session]
   [ventas.themes.clothing.pages.frontend.profile.skeleton :as profile.skeleton]
   [clojure.string :as str]
   [ventas.utils.formatting :as utils.formatting]
   [ventas.components.base :as base]
   [ventas.components.image :as image]
   [ventas.components.term :as term]
   [cljsjs.moment]))

(def state-key ::state)

(defn content []
  [:div.orders-page
   (doall
    (for [{:keys [amount lines created-at]} @(rf/subscribe [::events/db [state-key :orders]])]
      [base/segment
       [:div.orders-page__order
        [:div.orders-page__order-top
         [:h4.orders-page__order-price
          (str "Total: " (utils.formatting/amount->str amount))]
         [:p.orders-page__order-created-at
          (.format (js/moment created-at) "dddd, MMMM Do YYYY, h:mm:ss a")]]
        (for [{{:keys [name images variation]} :product-variation} lines]
          [:div.orders-page__line
           [:div.orders-page__line-image
            [:img {:src (image/get-url (-> images first :id) :product-listing)}]]
           [:div.orders-page__line-info
            [:h4.orders-page__line-title name]
            [:p.orders-page__line-price
             (utils.formatting/amount->str amount)]
            [:div.orders-page__line-terms
             (for [{:keys [taxonomy selected]} variation]
               (when selected
                 [:div.cart-notification__term
                  [term/term-view (:keyword taxonomy) selected {:active? true}]]))]]])]]))])

(defn page []
  [profile.skeleton/skeleton
   [content]])

(rf/reg-event-db
 ::init.orders.success
 (fn [db [_ data]]
   (assoc-in db [state-key :orders] data)))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::session/require-identity]
                 [::backend/users.orders.list
                  {:success [::init.orders.success]}]]}))

(routes/define-route!
  :frontend.profile.orders
  {:name ::page
   :url ["orders"]
   :component page
   :init-fx [::init]})
