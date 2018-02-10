(ns ventas.pages.admin.configuration.image-sizes.edit
  (:require
   [re-frame.core :as rf]
   [reagent.core :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.components.form :as form]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [_ [_ data]]
   {:dispatch [::backend/admin.image-sizes.save
               {:params data
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/add {:message (i18n ::saved-notification)
                                  :theme "success"}]
    :go-to [:admin.configuration.image-sizes]}))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/entities.find (routes/ref-from-param :id)
                  {:success [::form/populate state-key]}]
                 [::backend/admin.image-sizes.entities.list
                  {:success [::events/db [state-key :image-sizes]]}]
                 [::events/enums.get :image-size.algorithm]]}))

(defn form []
  (let [{:keys [form form-hash]} @(rf/subscribe [::events/db state-key])]
    ^{:key form-hash}
    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

     [base/segment {:color "orange"
                    :title "Image size"}

      (let [field :keyword]
        [base/form-input
         {:label (i18n ::keyword)
          :default-value (get form field)
          :on-change #(rf/dispatch [::form/set-field field (-> % .-target .-value)])}])

      (let [field :width]
        [base/form-input
         {:label (i18n ::width)
          :default-value (get form field)
          :on-change #(rf/dispatch [::form/set-field field (-> % .-target .-value)])}])

      (let [field :height]
        [base/form-input
         {:label (i18n ::height)
          :default-value (get form field)
          :on-change #(rf/dispatch [::form/set-field field (-> % .-target .-value)])}])

      (let [field :algorithm]
        [base/form-field
         [:label (i18n ::algorithm)]
         [base/dropdown
          {:fluid true
           :selection true
           :options (map #(update % :value str)
                         @(rf/subscribe [::events/db [:enums :image-size.algorithm]]))
           :default-value (str (get form field))
           :on-change #(rf/dispatch [::form/set-field field (-> % .-target .-value)])}]])

      (let [field :entities]
        [base/form-field
         [:label (i18n ::entities)]
         [base/dropdown
          {:multiple true
           :selection true
           :options (map (fn [entity]
                           {:text (i18n entity)
                            :value (str entity)})
                         @(rf/subscribe [::events/db :image-sizes]))
           :default-value (get form field)
           :on-change #(rf/dispatch [::form/set-field field (set (.-value %2))])}]])]

     [base/divider {:hidden true}]

     [base/form-button {:type "submit"} (i18n ::submit)]]))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-configuration-image-sizes-edit__page
    [form]]])

(routes/define-route!
  :admin.configuration.image-sizes.edit
  {:name ::page
   :url [:id "/edit"]
   :component page
   :init-fx [::init]})
