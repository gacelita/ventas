(ns ventas.plugins.slider.config
  (:require
   [re-frame.core :as rf]
   [ventas.components.form :as form]
   [ventas.events.backend :as backend]
   [ventas.utils.ui :as utils.ui]
   [ventas.components.crud-form :as crud-form :include-macros true]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.components.draggable-list :as draggable-list]))

(def state-key ::state)

(rf/reg-event-fx
 ::init
 (fn [_ [_ block]]
   (js/console.log :slider.init block)
   {:dispatch [::backend/admin.entities.pull
               {:params {:id (get-in block [:layout.block/config :db/id])}
                :success [::init.next block]}]}))

(rf/reg-event-fx
 ::init.next
 (fn [_ [_ block data]]
   {:dispatch [::form/populate [state-key (:db/id block)] data]}))

(defn- slide-view [state-path slide]
  [:p "the slide" (:db/id slide)]
  (let [state-path (conj state-path 0)]
    [base/segment
     [form/form state-path
      [base/form
       (crud-form/field
        state-path
        {:key [:slider.slider/slides 0 :slider.slide/name]
         :type :i18n})
       (crud-form/field
        state-path
        {:key [:slider.slider/slides 0 :slider.slide/file]
         :type :text})]]]))

(defn- config [block]
  (let [state-path [state-key (:db/id block)]
        form @(rf/subscribe [::form/data state-path])]
    [form/form state-path
     [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}
      (crud-form/field
       state-path
       {:key :slider.slider/name
        :type :i18n})
      (crud-form/field
       state-path
       {:key :slider.slider/auto
        :type :toggle})
      (crud-form/field
       state-path
       {:key :slider.slider/auto-speed
        :type :number})

      (let [field :slider.slider/slides]
        [base/form-field {:class "admin-menus-edit__items"}
         [:label (i18n ::slides)]
         [draggable-list/main-view
          {:on-reorder (fn [items]
                         (let [items (map second items)]
                           (rf/dispatch [::form/set-field state-path field items])))}
          (for [{:db/keys [id] :as item} (get form field)]
            ^{:key id} [slide-view state-path item])]
         [base/button {:size "small"
                       :class "admin-menus-edit__add-item"
                       :on-click (utils.ui/with-handler
                                  #(rf/dispatch [::edit-item]))}
          (i18n ::add-slide)]])

      [base/form-button {:type "submit"} (i18n ::submit)]]]))