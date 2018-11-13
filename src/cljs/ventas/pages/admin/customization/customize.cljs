(ns ventas.pages.admin.customization.customize
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.colorpicker :as colorpicker]
   [goog.events]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.components.popover :as popover]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.page :as page]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.widget :as widget]
   [reagent.core :as reagent])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.configuration.set
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

(defn- field [state-path {:keys [key type] :as args}]
  [:div.customize-field
   [base/header {:as "h4"}
    (i18n (ns-kw key))]
   [:div.customize-field__input
    {:class (when type (str "customize-field__input--" (name type)))}
    [input args (get @(rf/subscribe [::form/data state-path]) key)]]])

(def ^:private fields
  (atom {:customization/name {:type :text}
         :customization/logo {:type :image
                              :inline true}
         :customization/header-image {:type :image}
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

(defn theme-section []
  [base/transition-group {:animation "fade left"
                          :duration 400}
   (let [state-path [state-key :settings]]
     [form/form state-path
      [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}
       (for [[key field-config] @fields]
         [field state-path (assoc field-config :key key)])]])])

(defn block-section []
  (let [{:keys [block component]} @(rf/subscribe [::events/db [state-key :panel]])]
    [component block]))

(defn panel [title content]
  [:div
   [base/header {:as "h6"}
    [base/icon {:name "chevron left"
                :size "mini"
                :link true
                :on-click #(rf/dispatch [::events/db [state-key :panel] nil])}]
    [base/header-content [:h4 title]]]
   content])

(rf/reg-event-fx
 ::block.edit
 (fn [{:keys [db]} [_ widget block]]
   (let [event (get-in widget [:config :init])]
     {:dispatch [event block]
      :db (assoc-in db [state-key :panel] {:type :block
                                           :name (i18n (:name widget))
                                           :block block
                                           :component (get-in widget [:config :component])})})))

(defn- page-section []
  [:div.admin-customize__page-section
   (for [block (sort-by :layout.block/position
                        @(rf/subscribe [:db [state-key :layout :layout/blocks]]))]
     (let [widget (widget/find (:layout.block/widget block))]
       [base/segment {:on-click #(rf/dispatch [::block.edit widget block])}
        [:div.admin-customize__block
         [:span.admin-customize__block-name
          (i18n (:name widget))]
         [base/icon {:name "eye" :link true}]
         [base/icon {:name "sidebar" :link true :class "admin-customize__grab"}]]]))
   [base/button {:fluid true
                 :content "Add section"
                 :on-click #(rf/dispatch [::block.add])}]
   [:br]
   [base/button {:fluid true
                 :on-click #(rf/dispatch [::events/db [state-key :panel] :theme-settings])}
    "Theme settings"]])

(defn- sidebar []
  [:div.admin-customize__sidebar
   (let [{:keys [type name]} @(rf/subscribe [::events/db [state-key :panel]])]
     (case type
       :theme-settings [panel "Theme settings" [theme-section]]
       :block [panel name [block-section]]
       [page-section]))])

(rf/reg-event-db
 ::page.set
 (fn [db [_ v]]
   db))

{:schema/type :schema.type/layout.block
 :layout.block/config :ref-to-the-entity
 :layout.block/position 0
 :layout.block/section :clothing/home}

(rf/reg-event-db
 ::preview-size.set
 (fn [db [_ preview-size]]
   (assoc-in db [state-key :preview-size] preview-size)))

(defn- top-bar []
  [form/form [state-key :top-bar]
   [:div.admin-customize__top-bar
    [:div.admin-customize__page-combobox
     [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}
      (js/console.log :data (->> @widget/pages
                                 (map (fn [[k {:keys [name]}]]
                                        {:value k
                                         :text (i18n name)}))))
      [form/field {:key :page
                   :db-path [state-key :top-bar]
                   :type :combobox
                   :on-change-fx [::page.set]
                   :default-value @(rf/subscribe [::events/db [state-key :page]])
                   :options (->> (widget/get-pages)
                                 (map (fn [[k {:keys [name]}]]
                                        {:value k
                                         :text (i18n name)})))}]]]
    [:div.admin-customize__preview-size
     [:div.admin-customize__preview-size-icons
      [base/icon {:name "mobile alternate"
                  :link true
                  :on-click #(rf/dispatch [::preview-size.set :mobile])}]
      [base/icon {:name "computer"
                  :link true
                  :on-click #(rf/dispatch [::preview-size.set :computer])}]
      [base/icon {:name "expand arrows alternate"
                  :link true
                  :on-click #(rf/dispatch [::preview-size.set :fullwidth])}]]]
    [:div.admin-customize__save
     [base/form-button
      {:type "submit"
       :size "small"}
      (i18n ::save)]
     ]]])

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:dispatch-n [[::backend/configuration.get
                  {:params (set (keys @fields))
                   :success [::form/populate [state-key]]}]
                 [::form/populate [state-key :top-bar] {:page (->> (widget/get-pages) (first) (key))}]
                 [::backend/layout.get
                  {:success [::events/db [state-key :layout]]}]
                 (:init-fx (routes/find-route :frontend.products))]}))

(rf/reg-sub
 ::preview-size
 (fn [db]
   (get-in db [state-key :preview-size])))

(rf/reg-sub
 ::page
 (fn [db]
   (get (widget/get-pages)
        (:page (form/get-data db [state-key :top-bar])))))

(defn- preview []
  (let [ref (reagent/atom nil)]
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (goog.events/listen
         @ref
         "click"
         (fn [e]
           (println "click")
           (.stopPropagation e)
           (.preventDefault e))))
      :reagent-render
      (fn []
        [:div.admin-customize__preview {:ref #(reset! ref %)}
         (when (= :mobile @(rf/subscribe [::preview-size]))
           {:style {:width "375px"
                    :max-height "667px"}})
         [page/main (:route @(rf/subscribe [::page]))]])})))

(defn- page []
  [:div.root--customize
   [admin.skeleton/skeleton
    [:div.admin__default-content.admin-customize__page
     (when (not= :fullwidth @(rf/subscribe [::preview-size]))
       [sidebar])
     [:div.admin-customize__content
      [top-bar]
      [:div.admin-customize__preview-wrapper
       [preview]]]]]])

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