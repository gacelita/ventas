(ns ventas.pages.admin.configuration.image-sizes.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.components.base :as base]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.i18n :refer [i18n]]
   [ventas.events.backend :as backend]
   [ventas.common.utils :as common.utils]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]))

(def form-data-key ::form-data)

(def form-hash-key ::form-key)

(def image-size-entities-key ::image-size-entities)

(rf/reg-event-fx
 ::submit
 (fn [cofx [_ data]]
   {:dispatch [::backend/admin.image-sizes.save
               {:params data
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::saved-notification)
                                  :theme "success"}]
    :go-to [:admin.configuration.image-sizes]}))

(rf/reg-event-db
 ::set-field
 (fn [db [_ field value]]
   (let [field (if-not (sequential? field)
                 [field]
                 field)]
     (assoc-in db (concat [form-data-key] field) value))))

(defn form []
  (rf/dispatch [::backend/entities.find (routes/ref-from-param :id)
                {:success (fn [entity-data]
                            (rf/dispatch [::events/db form-data-key entity-data])
                            (rf/dispatch [::events/db form-hash-key (hash entity-data)]))}])
  (rf/dispatch [::backend/admin.image-sizes.entities.list
                {:success [::events/db image-size-entities-key]}])
  (rf/dispatch [::events/enums.get :image-size.algorithm])
  (fn []
    (let [form-data @(rf/subscribe [::events/db [form-data-key]])
          form-hash @(rf/subscribe [::events/db [form-hash-key]])]
      [base/form {:key form-hash
                  :on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

       [base/segment {:color "orange"
                      :title "Image size"}
        [base/form-input
         {:label (i18n ::keyword)
          :default-value (:keyword form-data)
          :on-change #(rf/dispatch [::set-field :keyword (-> % .-target .-value)])}]
        [base/form-input
         {:label (i18n ::width)
          :default-value (:width form-data)
          :on-change #(rf/dispatch [::set-field :width (-> % .-target .-value)])}]
        [base/form-input
         {:label (i18n ::height)
          :default-value (:height form-data)
          :on-change #(rf/dispatch [::set-field :height (-> % .-target .-value)])}]
        [base/form-field
         [:label (i18n ::algorithm)]
         [base/dropdown
          {:fluid true
           :selection true
           :options (map #(update % :value str)
                         @(rf/subscribe [::events/db [:enums :image-size.algorithm]]))
           :default-value (str (:algorithm form-data))
           :on-change #(rf/dispatch [::set-field :algorithm (-> % .-target .-value)])}]]
        [base/form-field
         [:label (i18n ::entities)]
         [base/dropdown
          {:multiple true
           :selection true
           :options (map (fn [entity]
                           {:text (i18n entity)
                            :value (str entity)})
                         @(rf/subscribe [::events/db image-size-entities-key]))
           :default-value (:entities form-data)
           :on-change #(rf/dispatch [::set-field :entities (set (.-value %2))])}]]]

       [base/divider {:hidden true}]

       [base/form-button {:type "submit"} (i18n ::submit)]])))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-configuration-image-sizes-edit__page
    [form]]])

(routes/define-route!
  :admin.configuration.image-sizes.edit
  {:name ::page
   :url [:id "/edit"]
   :component page})