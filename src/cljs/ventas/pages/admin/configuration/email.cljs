(ns ventas.pages.admin.configuration.email
  (:require
   [reagent.ratom :refer [atom]]
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.components.base :as base]
   [ventas.utils.ui :as utils.ui]
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.components.form :as form]
   [ventas.events.backend :as backend]
   [ventas.components.notificator :as notificator])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.configuration.set
               {:params (get-in db [state-key :form])
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/add {:message (i18n ::saved)
                                  :theme "success"}]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw key))})])

(defn- form [data]
  [base/segment {:color "orange"
                 :title (i18n ::page)}
   [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

    [field {:key :email.from
            :label (i18n ::email.from)}]

    [field {:key :email.encryption.enabled
            :label (i18n ::email.from)
            :type :toggle}]

    (when (:email.encryption.enabled data)
      [field {:key :email.encryption.type
              :label (i18n ::email.from)
              :type :radio
              :options [{:id "ssl"
                         :name (i18n ::ssl)}
                        {:id "tls"
                         :name (i18n ::tls)}]}])

    [field {:key :email.smtp.enabled
            :label (i18n ::email.from)
            :type :toggle}]

    (when (:email.smtp.enabled data)
      [:div

       [field {:key :email.smtp.host
               :label (i18n ::email.from)}]

       [field {:key :email.smtp.port
               :label (i18n ::email.from)
               :type :number}]

       [field {:key :email.smtp.user
               :label (i18n ::email.from)}]

       [field {:key :email.smtp.password
               :label (i18n ::email.from)
               :type :password}]])

    [base/divider {:hidden true}]

    [base/form-button
     {:type "submit"}
     (i18n ::submit)]]])

(defn- content []
  [form/form [state-key] form])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-email__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::backend/admin.configuration.get
               {:params #{:email.from
                          :email.encryption.enabled
                          :email.encryption.type
                          :email.smtp.enabled
                          :email.smtp.host
                          :email.smtp.port
                          :email.smtp.user
                          :email.smtp.password}
                :success [::form/populate [state-key]]}]}))

(routes/define-route!
 :admin.configuration.email
 {:name ::page
  :url "email"
  :component page
  :init-fx [::init]})
