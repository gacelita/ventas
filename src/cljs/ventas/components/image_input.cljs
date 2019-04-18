(ns ventas.components.image-input
  (:require
   [ventas.components.base :as base]
   [ventas.events :as events]
   [re-frame.core :as rf]
   [ventas.components.image :as image]
   [ventas.utils.ui :as utils.ui]
   [ventas.components.form :as form]
   [ventas.components.draggable-list :as draggable-list]))

(def state-key ::state)

(rf/reg-sub
 ::image-modal
 (fn [db]
   (get-in db [state-key :image-modal])))

(rf/reg-event-db
 ::image-modal.open
 (fn [db [_ url]]
   (assoc-in db [state-key :image-modal] {:open true
                                          :url url})))

(rf/reg-event-db
 ::image-modal.close
 (fn [db [_]]
   (assoc-in db [state-key :image-modal :open] false)))

(defn image-modal []
  (let [{:keys [open url]} @(rf/subscribe [::image-modal])]
    [base/modal {:basic true
                 :size "small"
                 :open open
                 :on-close #(rf/dispatch [::image-modal.close])}
     [base/modal-content {:image true}
      [base/image {:wrapped true
                   :size "large"
                   :src url}]]]))

(defn image-view [{:keys [id on-remove]}]
  [:div
   [base/image {:src (image/get-url id :admin-products-edit)
                :size "small"
                :on-click (utils.ui/with-handler
                           #(rf/dispatch [::image-modal.open (image/get-url id :product-page-main-zoom)]))}]
   [base/button {:icon true
                 :size "mini"
                 :on-click (utils.ui/with-handler
                            #(rf/dispatch on-remove))}
    [base/icon {:name "remove"}]]])

(rf/reg-event-fx
 ::upload
 (fn [_ [_ on-change files]]
   {:dispatch-n (for [file files]
                  [::events/upload
                   {:success [::upload.next on-change]
                    :file file}])}))

(rf/reg-event-fx
 ::upload.next
 (fn [_ [_ on-change entity]]
   {:dispatch (conj on-change entity)}))

(rf/reg-event-fx
 ::image.set
 (fn [_ [_ db-path key entity]]
   {:dispatch [::form/set-field db-path key entity]}))

(defn- image-placeholder [{:keys [on-change]}]
  (let [ref (atom nil)]
    (fn []
      [:div.ui.small.image.image-input__placeholder
       {:on-click #(-> @ref (.click))}
       [base/icon {:name "plus"}]
       [:input {:type "file"
                :multiple true
                :ref #(reset! ref %)
                :on-change #(rf/dispatch [::upload
                                          on-change
                                          (-> (-> % .-target .-files)
                                              js/Array.from)])}]])))

(defn image-input [{:keys [on-change value]}]
  [:div.image-input
   (if value
     ^{:key value}
     [image-view {:id value
                  :on-remove on-change}]
     [image-placeholder {:on-change on-change}])
   [image-modal]])

(defmethod form/input ::image [{:keys [value db-path key]}]
  [image-input
   {:on-change [::image.set db-path key]
    :value (:db/id value)}])

;; images input

(defn- assoc-file [list file]
  (let [list (or list {:schema/type :schema.type/file.list
                       :file.list/elements []})]
    (update list :file.list/elements conj {:schema/type :schema.type/file.list.element
                                           :file.list.element/file file
                                           :file.list.element/position (count (:file.list/elements list))})))

(defn- dissoc-file [list file]
  (update list :file.list/elements
          (fn [elements]
            (remove #(= (get-in % [:file.list.element/file :db/id]) (:db/id file))
                    elements))))

(rf/reg-event-fx
 ::list-element.upload
 (fn [_ [_ db-path key file]]
   {:dispatch [::form/update-field db-path key #(assoc-file % file)]}))

(rf/reg-event-fx
 ::list-element.remove
 (fn [_ [_ db-path key file-id]]
   {:dispatch [::form/update-field db-path key #(dissoc-file % {:db/id file-id})]}))

(defn list-image-view [db-path key {:db/keys [id]}]
  [:div.image-input__list-element
   [image-view {:id id
                :on-remove [::list-element.remove db-path key id]}]])

(defmethod form/input ::image-list [{:keys [value db-path key]}]
  [base/form-field {:class "image-input__list"}
   [base/image-group
    [draggable-list/main-view
     {:on-reorder (fn [items]
                    (let [files (map #(nth % 3) items)]
                      (rf/dispatch [::form/set-field db-path key (reduce assoc-file nil files)])))}
     (for [{:file.list.element/keys [file position]} (:file.list/elements value)]
       ^{:key position}
       [list-image-view db-path key file])]
    [image-placeholder
     {:on-change [::list-element.upload db-path key]}]]])