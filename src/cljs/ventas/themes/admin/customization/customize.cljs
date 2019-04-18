(ns ventas.themes.admin.customization.customize
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.colorpicker :as colorpicker]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.components.image-input :as image-input]
   [ventas.components.popover :as popover]
   [ventas.events :as events]
   [ventas.server.api :as backend]
   [ventas.server.api.admin :as api.admin]
   [ventas.i18n :refer [i18n]]
   [ventas.page :as page]
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

(defmulti input (fn [args _] (:type args)) :default :default)

(rf/reg-event-fx
 ::set-field
 (fn [{:keys [db]} [_ db-path key value]]
   {:dispatch [::form/set-field db-path key value]
    :db (assoc db :configuration (assoc
                                  (form/get-data db db-path)
                                   key value))}))

(defmethod input :radio [{:keys [key options]} value]
  [:div
   (for [{:keys [id icon]} options]
     [:div.customize-field__radio
      {:class (when (= value id) "customize-field__radio--active")
       :on-click #(rf/dispatch [::set-field [state-key] key id])}
      [base/icon {:name icon}]])])

(defmethod input :color [{:keys [key]} value]
  [:div.customize-field__color
   {:on-click (utils.ui/with-handler #(rf/dispatch [::popover/toggle key]))
    :style {:background-color value}}
   [popover/popover key
    [:div.customize-field__colorpicker
     {:on-click (utils.ui/with-handler #())}
     [colorpicker/colorpicker
      {:on-change [::set-field [state-key] key]
       :value value}]]]])

(rf/reg-event-fx
 ::upload
 (fn [_ [_ {:keys [file key]}]]
   {:dispatch-n [[::set-field [state-key] key :loading]
                 [::events/upload {:success [::upload.next key]
                                   :file file}]]}))

(rf/reg-event-fx
 ::upload.next
 (fn [_ [_ key {:db/keys [id]}]]
   {:dispatch [::set-field [state-key] key id]}))

(defmethod input :image [_ _]
  (let [ref (atom nil)]
    (fn [{:keys [key]} value]
      [:div
       {:on-click #(-> @ref (.click))}
       (if (= value :loading)
         [base/icon {:name "spinner"
                     :loading true}]
         [base/icon {:name "pencil"}])
       [:input {:type "file"
                :ref #(reset! ref %)
                :on-change #(rf/dispatch [::upload {:file (-> (-> % .-target .-files)
                                                              js/Array.from
                                                              first)
                                                    :key key}])}]])))

(defmethod input :default [{:keys [key]} value]
  [:div
   [:input {:type "text"
            :value value
            :on-change #(let [new-value (-> % .-target .-value)]
                          (rf/dispatch [::set-field [state-key] key new-value]))}]])

(defn- field [{:keys [key type] :as args}]
  [:div.customize-field
   [base/header {:as "h4"}
    (i18n (ns-kw key))]
   [:div.customize-field__input
    {:class (when type (str "customize-field__input--" (name type)))}
    [input args (get @(rf/subscribe [::form/data [state-key]]) key)]]])

(def ^:private fields
  (atom {:customization/name {:type :text}
         :customization/logo {:type ::image-input/image
                              :inline true}
         :customization/header-image {:type ::image-input/image}
         :customization/background-color {:type :color}
         :customization/foreground-color {:type :color}
         :customization/product-listing-mode {:type :radio
                                              :options [{:id :list
                                                         :icon "list layout"}
                                                        {:id :grid
                                                         :icon "grid layout"}]}}))

(defn add-field! [key config]
  (swap! fields assoc key config))

(defn remove-field! [key]
  (swap! fields dissoc key))

(defn- content []
  [form/form [state-key]
   [:div.admin-customize__content
    [:div.admin-customize__back
     [base/button {:icon "chevron left"
                   :content (i18n ::back)
                   :labelPosition "left"
                   :on-click #(routes/go-to :admin)}]]
    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}
     (for [[key field-config] @fields]
       [field (assoc field-config :key key)])
     [base/divider {:hidden true}]
     [base/form-button
      {:type "submit"}
      (i18n ::save)]]]])

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:dispatch-n [[::backend/configuration.get
                  {:params (set (keys @fields))
                   :success [::form/populate [state-key]]}]
                 (:init-fx (routes/find-route :frontend.products))]
    :db (assoc db :customization-route [:frontend])}))

(defn- page []
  [:div.root--customize
   [admin.skeleton/skeleton
    [:div.admin__default-content.admin-customize__page
     [content]
     [page/main :frontend.products]]]])

(admin.skeleton/add-menu-item!
 {:route :admin.customization.customize
  :parent :admin.customization
  :mobile? false
  :label ::page})

(routes/define-route!
 :admin.customization.customize
 {:name ::page
  :url "customization"
  :component page
  :init-fx [::init]})