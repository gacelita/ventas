(ns ventas.plugins.stripe.admin
  (:require
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.i18n :refer [i18n]]
   [re-frame.core :as rf]
   [ventas.components.form :as form]
   [ventas.components.base :as base]
   [ventas.utils.ui :as utils.ui]
   [ventas.routes :as routes]
   [ventas.components.notificator :as notificator]
   [ventas.events.backend :as backend])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(admin.skeleton/add-menu-item!
 :admin.payment-methods
 {:route :admin.payment-methods.stripe
  :label ::page})

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.configuration.set
               {:params (get-in db [state-key :form])
                :success [::notificator/notify-saved]}]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw key))})])

(defn- content []
  [form/form [state-key]
   [base/segment {:color "orange"
                  :title (i18n ::page)}
    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

     [field {:key :stripe.public-key}]
     [field {:key :stripe.private-key}]

     [base/divider {:hidden true}]

     [base/form-button
      {:type "submit"}
      (i18n ::submit)]]]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-payment-methods-stripe__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/configuration.get
                  {:params #{:stripe.public-key
                             :stripe.private-key}
                   :success [::form/populate [state-key]]}]]}))

(routes/define-route!
 :admin.payment-methods.stripe
 {:name ::page
  :url "stripe"
  :component page
  :init-fx [::init]})