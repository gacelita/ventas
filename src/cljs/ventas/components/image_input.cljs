(ns ventas.components.image-input
  (:require
   [ventas.components.base :as base]
   [ventas.events :as events]
   [re-frame.core :as rf]
   [ventas.components.image :as image]
   [ventas.utils.ui :as utils.ui]))

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

(defn image-view [id]
  [base/image {:src (image/get-url id :admin-products-edit)
               :size "small"
               :on-click (utils.ui/with-handler
                          #(rf/dispatch [::image-modal.open (image/get-url id :product-page-main-zoom)]))}])

(rf/reg-event-fx
 ::upload
 (fn [_ [_ on-change file]]
   {:dispatch [::events/upload
               {:success [::upload.next on-change]
                :file file}]}))

(rf/reg-event-fx
 ::upload.next
 (fn [_ [_ on-change {:db/keys [id]}]]
   {:dispatch (conj on-change id)}))

(defn- image-placeholder [{:keys [on-change]}]
  (let [ref (atom nil)]
    (fn []
      [:div.ui.small.image.image-input__placeholder
       {:on-click #(-> @ref (.click))}
       [base/icon {:name "plus"}]
       [:input {:type "file"
                :ref #(reset! ref %)
                :on-change #(rf/dispatch [::upload
                                          on-change
                                          (-> (-> % .-target .-files)
                                              js/Array.from
                                              first)])}]])))

(defn image-input [{:keys [on-change value]}]
  [:div.image-input
   (if value
     ^{:key value} [image-view value]
     [image-placeholder {:on-change on-change}])
   [image-modal]])