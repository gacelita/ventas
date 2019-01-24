(ns ventas.pages.admin.users.edit
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.entities.save
               {:params (get-in db [state-key :form])
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/notify-saved]
    :go-to [:admin.users]}))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [(let [id (routes/ref-from-param :id)]
                   (if-not (pos? id)
                     [::form/populate [state-key] {:schema/type :schema.type/user}]
                     [::backend/admin.entities.pull
                      {:params {:id id}
                       :success [::form/populate [state-key]]}]))
                 [::events/enums.get :user.role]
                 [::events/enums.get :user.status]
                 [::events/i18n.cultures.list]]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw (if (sequential? key)
                                            (first key)
                                            key)))})])

(defn content []
  [form/form [state-key]
   [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

    [base/segment {:color "orange"
                   :title "User"}
     [field {:key :user/first-name}]
     [field {:key :user/last-name}]
     [field {:key :user/email}]
     [field {:key [:user/status :db/id]
             :type :combobox
             :options @(rf/subscribe [:db [:enums :user.status]])}]]

    [base/divider {:hidden true}]

    [base/segment {:color "orange"
                   :title "Contact information"}
     [field {:key :user/phone}]
     [field {:key :user/company}]]

    [base/divider {:hidden true}]

    [base/segment {:color "orange"
                   :title "Configuration"}
     [field {:key :user/culture
             :type :combobox
             :options @(rf/subscribe [:db [:cultures]])}]
     [field {:key :user/roles
             :type :tags
             :xform {:in #(map :db/id %)
                     :out #(map (fn [v] {:db/id v}) %)}
             :forbid-additions true
             :options @(rf/subscribe [:db [:enums :user.role]])}]]

    [base/divider {:hidden true}]

    [base/form-button {:type "submit"}
     (i18n ::submit)]]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-users-edit__page
    [content]]])

(routes/define-route!
  :admin.users.edit
  {:name ::page
   :url [:id "/edit"]
   :component page
   :init-fx [::init]})
