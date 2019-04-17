(ns ventas.themes.admin.configuration.email
  (:require
   [re-frame.core :as rf]
   [reagent.ratom :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.server.api.admin :as api.admin]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(defn- ->form [data]
  (assoc data :encryption-enabled? (or (:ssl data) (:tls data))
              :smtp-enabled? (boolean (:host data))
              :encryption-type (cond
                                 (:ssl data) "ssl"
                                 (:tls data) "tls")))

(defn- ->entity [form]
  (let [encryption-type (keyword (get form :encryption-type))]
    (-> form
        (dissoc :encryption-enabled? :encryption-type :smtp-enabled? :ssl :tls)
        (cond-> (and encryption-type (:encryption-enabled? form)) (assoc encryption-type true)))))

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::api.admin/admin.email-configuration.set
               {:params (->entity (get-in db [state-key :form]))
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

       [field {:key :from}]

       [field {:key :encryption-enabled?
               :type :toggle}]

       (when (:encryption-enabled? data)
         [field {:key :encryption-type
                 :type :radio
                 :options [{:value "ssl"
                            :text (i18n ::ssl)}
                           {:value "tls"
                            :text (i18n ::tls)}]}])

       [field {:key :smtp-enabled?
               :type :toggle}]

       (when (:smtp-enabled? data)
         [:div
          [field {:key :host}]

          [field {:key :port
                  :type :number}]

          [field {:key :user}]

          [field {:key :pass
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
 ::init.next
 (fn [_ [_ data]]
   {:dispatch [::form/populate [state-key] (->form data)]}))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::api.admin/admin.email-configuration.get
               {:success [::init.next]}]}))

(routes/define-route!
 :admin.configuration.email
 {:name ::page
  :url "email"
  :component page
  :init-fx [::init]})
