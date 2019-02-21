(ns ventas.themes.admin.configuration.image-sizes.edit
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.server.api.admin :as api.admin]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]}]
   {:dispatch [::api.admin/admin.entities.save
               {:params (get-in db [state-key :form])
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/notify-saved]
    :go-to [:admin.configuration.image-sizes]}))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [(let [id (routes/ref-from-param :id)]
                   (if-not (pos? id)
                     [::form/populate [state-key] {:schema/type :schema.type/image-size}]
                     [::api.admin/admin.entities.pull
                      {:params {:id id}
                       :success [::form/populate [state-key]]}]))
                 [::api.admin/admin.image-sizes.entities.list
                  {:success [:db [state-key :entities]]}]
                 [::events/enums.get :image-size.algorithm]]}))

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
                   :title "Image size"}
     [field {:key :image-size/keyword}]
     [field {:key :image-size/width}]
     [field {:key :image-size/height}]
     [field {:key [:image-size/algorithm :db/id]
             :type :combobox
             :options @(rf/subscribe [:db [:enums :image-size.algorithm]])}]
     [field {:key :image-size/entities
             :type :tags
             :forbid-additions true
             :xform {:in #(map :db/id %)
                     :out #(map (fn [v] {:db/id v}) %)}
             :options (map (fn [{:keys [ident id]}]
                             {:text (i18n ident)
                              :value id})
                           @(rf/subscribe [:db [state-key :entities]]))}]]

    [base/divider {:hidden true}]

    [base/form-button {:type "submit"}
     (i18n ::submit)]]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-configuration-image-sizes-edit__page
    [content]]])

(routes/define-route!
  :admin.configuration.image-sizes.edit
  {:name ::page
   :url [:id "/edit"]
   :component page
   :init-fx [::init]})
