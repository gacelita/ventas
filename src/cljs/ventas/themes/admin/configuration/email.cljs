(ns ventas.themes.admin.configuration.email
  (:require
   [re-frame.core :as rf]
   [reagent.ratom :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.server.api :as backend]
   [ventas.server.api.admin :as api.admin]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::api.admin/admin.configuration.set
               {:params (get-in db [state-key :form])
                :success [::notificator/notify-saved]}]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw key))})])

(defn- content []
  [form/form [state-key]
   (let [data @(rf/subscribe [::form/data [state-key]])]
     [base/segment {:color "orange"
                    :title (i18n ::page)}
      [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

       [field {:key :email.from}]

       [field {:key :email.encryption.enabled
               :type :toggle}]

       (when (:email.encryption.enabled data)
         [field {:key :email.encryption.type
                 :type :radio
                 :options [{:value "ssl"
                            :text (i18n ::ssl)}
                           {:value "tls"
                            :text (i18n ::tls)}]}])

       [field {:key :email.smtp.enabled
               :type :toggle}]

       (when (:email.smtp.enabled data)
         [:div

          [field {:key :email.smtp.host}]

          [field {:key :email.smtp.port
                  :type :number}]

          [field {:key :email.smtp.user}]

          [field {:key :email.smtp.password
                  :type :password}]])

       [base/divider {:hidden true}]

       [base/form-button
        {:type "submit"}
        (i18n ::submit)]]])])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-email__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::backend/configuration.get
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
