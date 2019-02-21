(ns ventas.components.crud-form
  "Implementation of ventas.components.form for the most boring case"
  (:require
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]
   [ventas.server.api.admin :as api.admin]
   [ventas.components.notificator :as notificator]
   [ventas.components.base :as base]
   [ventas.utils.ui :as utils.ui]
   [ventas.components.form :as form])
  (:require-macros
   [ventas.utils :refer [ns-kw]]
   [ventas.components.crud-form]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} [_ state-path list-route]]
   {:dispatch [::api.admin/admin.entities.save
               {:params (get-in db (conj state-path :form))
                :success [::submit.next list-route]}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ [_ list-route]]
   {:dispatch [::notificator/notify-saved]
    :go-to [list-route]}))

(rf/reg-event-fx
 ::init
 (fn [_ [_ state-path entity-type next-event]]
   {:dispatch (let [id (routes/ref-from-param :id)]
                (if-not (pos? id)
                  [::form/populate state-path {:schema/type (keyword "schema.type" (name entity-type))}]
                  [::api.admin/admin.entities.pull
                   {:params {:id id}
                    :success [::init.next state-path next-event]}]))}))

(rf/reg-event-fx
 ::init.next
 (fn [_ [_ state-path next-event data]]
   {:dispatch-n [[::form/populate state-path data]
                 (when next-event
                   (conj next-event data))]}))

(defn component [state-path list-route component]
  [form/form state-path
   [base/form {:on-submit (utils.ui/with-handler
                           #(rf/dispatch [::submit state-path list-route]))}
    component
    [base/form-button {:type "submit"}
     (i18n ::submit)]]])
