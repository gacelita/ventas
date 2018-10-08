(ns ventas.pages.admin.customization.menus.edit
  (:require
   [cljs.reader :refer [read-string]]
   [clojure.set :as set]
   [re-frame.core :as rf]
   [react-sortable-tree-theme-minimal]
   [react-sortable-tree]
   [reagent.core :as r]
   [ventas.common.utils :refer [mapm find-index remove-index]]
   [ventas.components.base :as base]
   [ventas.components.crud-form :as crud-form]
   [ventas.components.form :as form]
   [ventas.components.i18n-input :as i18n-input]
   [ventas.components.search-box :as search-box :refer [search-box]]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.plugins.menu.api :as menu.api]
   [ventas.routes :as routes]
   [ventas.utils.re-frame :refer [pure-subscribe]]
   [ventas.utils.ui :as utils.ui]))

(def state-key ::state)

(def state-path [state-key :form])

(def sortable-tree (r/adapt-react-class (.-default react-sortable-tree)))

(defn db->menu [menu]
  (->> menu
       (sort-by :menu.item/position)
       (mapv #(-> %
                  (update :menu.item/children db->menu)
                  (update :menu.item/link read-string)))))

(defn flatten-menu-item [menu-item]
  (->> (:menu.item/children menu-item)
       (mapcat flatten-menu-item)
       (into [menu-item])))

(defn index-menu [items]
  (->> items
       (mapcat flatten-menu-item)
       (mapm (fn [item]
               [(get item :db/id) item]))))

(rf/reg-event-fx
  ::init
  (fn [_ _]
    {:dispatch [::crud-form/init state-path :menu [::init.next]]}))

(rf/reg-event-fx
 ::init.next
 (fn [{:keys [db]} [_ menu]]
   {:dispatch [::form/populate state-path (update menu :menu/items db->menu)]
    :db (assoc-in db [state-key :menu-item-ids] (map :db/id (:menu/items menu)))}))

(rf/reg-event-db
 ::item-modal.open
 (fn [db [_ path]]
   (assoc-in db [state-key :item-modal] {:open true
                                         :path path})))

(rf/reg-event-db
 ::item-modal.close
 (fn [db _]
   (assoc-in db [state-key :item-modal :open] false)))

(defn path->idxs [tree path]
  (->> path
       (reduce (fn [{:keys [idxs tree]} id]
                 (let [idx (find-index #(= (:db/id %) id) tree)]
                   {:idxs (conj idxs idx)
                    :tree (get-in tree [idx :menu.item/children])}))
               {:idxs []
                :tree tree})
       :idxs
       (interpose :menu.item/children)))

(rf/reg-event-db
 ::save-item
 (fn [db [_ path]]
   (let [item (form/get-data db [state-key :modal-form])
         tree (get-in db [state-key :form :form :menu/items])
         idxs (path->idxs tree path)]
     (if-not path
       (update-in db [state-key :form :form :menu/items] (comp vec conj) (assoc item :db/id (str (gensym))))
       (assoc-in db (into [state-key :form :form :menu/items] idxs) item)))))

(rf/reg-event-fx
 ::edit-item
 (fn [{:keys [db]} [_ path]]
   (let [tree (get-in db [state-key :form :form :menu/items])
         idxs (path->idxs tree path)
         item (if path
                (get-in tree idxs)
                {:schema/type :schema.type/menu.item})]
     (cond
       (and path (:menu.item/link item) (vector? (:menu.item/link item)))
       {:dispatch [::menu.api/routes.get-name {:params {:route (:menu.item/link item)}
                                               :success [::edit-item.next path item]}]}

       (and path (:menu.item/link item))
       {:dispatch [::edit-item.next path item (:menu.item/link item)]}

       :else {:dispatch [::edit-item.next path item]}))))

(def search-box-id ::search-box)

(rf/reg-event-fx
 ::edit-item.next
 (fn [_ [_ path item name]]
   {:dispatch-n [[::search-box/items.set search-box-id (if (:menu.item/link item)
                                                         [{:id (:menu.item/link item)
                                                           :name name}]
                                                         [])]
                 [::search-box/query.set search-box-id ""]
                 [::form/populate [state-key :modal-form] item]
                 [::item-modal.open path]]}))

(rf/reg-event-db
  ::remove-item
  (fn [db [_ path]]
    (let [tree (get-in db [state-key :form :form :menu/items])
          idxs (path->idxs tree path)]
      (update-in db
                 (into [state-key :form :form :menu/items] (butlast idxs))
                 (partial remove-index (last idxs))))))

(rf/reg-event-fx
 ::autocompletions.select
 (fn [{:keys [db]} [_ {:keys [id name]}]]
   (let [form (form/get-data db [state-key :modal-form])]
     {:dispatch-n [[::search-box/items.set ::search-box [{:id id
                                                          :name (if (vector? id) name id)}]]
                   [::form/populate [state-key :modal-form] (assoc form :menu.item/link id)]]})))

(rf/reg-event-fx
 ::autocompletions.search
 (fn [_ [_ id query]]
   {:dispatch-n
    (if (empty? query)
      [[::search-box/items.set id nil]]
      [[::search-box/query.set id query]
       [::menu.api/autocompletions.get
        {:params {:query query}
         :success [::autocompletions.search.next id]}]])}))

(rf/reg-event-fx
 ::autocompletions.search.next
 (fn [_ [_ id items]]
   {:dispatch [::search-box/items.set id (map #(set/rename-keys % {:route :id})
                                              items)]}))

(defn item-modal [culture]
  (let [{:keys [open path]} @(rf/subscribe [::events/db [state-key :item-modal]])]
    [base/modal {:size "small"
                 :open open
                 :centered false
                 :on-close #(rf/dispatch [::item-modal.close])}
     [base/modal-header (i18n ::menu-item)]
     [base/modal-content
      (let [state-path [state-key :modal-form]
            form @(rf/subscribe [::form/data state-path])]
        [form/form state-path
         [base/form
          [:div
           (crud-form/field
            state-path
            {:key :menu.item/name
             :type :i18n
             :culture culture})
           (let [items @(rf/subscribe [::search-box/items search-box-id])
                 query @(rf/subscribe [::search-box/query search-box-id])]
             [search-box
              {:id search-box-id
               :style {:width "100%"}
               :default-value (str (:menu.item/link form))
               :options (->> (if (and (seq query) (empty? items))
                               [{:id query
                                 :name (str (i18n ::add) " " query)}]
                               items)
                             (search-box/->options #(rf/dispatch [::autocompletions.select %])))
               :on-search-change #(rf/dispatch [::autocompletions.search search-box-id (-> % .-target .-value)])}])]]])]
     [base/modal-actions
      [base/button {:on-click #(do
                                 (rf/dispatch [::save-item path])
                                 (rf/dispatch [::item-modal.close]))}
       (i18n ::save)]]]))

(defn ->sortable-tree [menu culture]
  (->> menu
       (map (fn [menu-item]
              {:title (i18n-input/->string (:menu.item/name menu-item)
                                           culture)
               :id (:db/id menu-item)
               :expanded (:sortable-tree/expanded menu-item)
               :children (let [children (:menu.item/children menu-item)]
                           (when (seq children)
                             (->sortable-tree children culture)))}))))

(defn <-sortable-tree [menu flattened-menu]
  (->> menu
       (mapv (fn [{:keys [id children expanded]}]
               (let [item (-> (get flattened-menu id)
                              (assoc :sortable-tree/expanded expanded))]
                 (if (seq children)
                   (assoc item :menu.item/children
                               (<-sortable-tree children flattened-menu))
                  (dissoc item :menu.item/children)))))))

(defn node-buttons [path]
  [:div
   [base/button {:icon true
                 :size "mini"
                 :on-click (utils.ui/with-handler
                             #(rf/dispatch [::edit-item path]))}
    [base/icon {:name "pencil"}]]
   [base/button {:icon true
                 :size "mini"
                 :on-click (utils.ui/with-handler
                             #(rf/dispatch [::remove-item path]))}
    [base/icon {:name "remove"}]]])

(defn content []
  (let [{{:keys [culture]} :identity} @(rf/subscribe [::events/db [:session]])
        form @(rf/subscribe [::form/data state-path])]
    [:div
     [base/segment {:color "orange"
                    :title (i18n ::menu)}
      (crud-form/field
       state-path
       {:key :menu/name
        :type :i18n
        :culture culture})

      (let [field :menu/items
            field-data (get form field)]
        [base/form-field {:class "admin-menus-edit__items"}
         [:label (i18n ::menu-items)]
         [:div.admin-menus-edit__sortable-tree
          [sortable-tree {:treeData (->sortable-tree field-data culture)
                          :isVirtualized false
                          :rowHeight 45
                          :getNodeKey (fn [row] (-> row .-node .-id))
                          :generateNodeProps (fn [row-info]
                                               (let [path (.-path row-info)]
                                                 (clj->js
                                                  {:buttons [(r/as-element (node-buttons path))]})))
                          :onChange (fn [items]
                                      (let [items (js->clj items :keywordize-keys true)]
                                        (rf/dispatch [::events/db [state-key :form :form :menu/items]
                                                      (<-sortable-tree items (index-menu field-data))])))
                          :theme react-sortable-tree-theme-minimal}]]
         [base/button {:size "small"
                       :class "admin-menus-edit__add-item"
                       :on-click (utils.ui/with-handler
                                  #(rf/dispatch [::edit-item]))}
          (i18n ::add-menu-item)]])]
     [item-modal culture]]))

(declare menu->db)

(defn menu-item->db [idx item]
  (-> item
      (dissoc :db/id)
      (dissoc :sortable-tree/expanded)
      (update :menu.item/name i18n-input/anonymize)
      (update :menu.item/link pr-str)
      (assoc :menu.item/position idx)
      (update :menu.item/children menu->db)))

(defn menu->db [menu]
  (map-indexed menu-item->db menu))

(rf/reg-event-fx
  ::submit
  (fn [{:keys [db]} _]
    (let [data (-> (get-in db [state-key :form :form])
                   (update :menu/items menu->db))
          retract-ids (get-in db [state-key :menu-item-ids])]
      {:dispatch-n (into (for [retract-id retract-ids]
                           [::backend/admin.entities.remove {:params {:id retract-id}}])
                         [[::backend/admin.entities.save
                           {:params data
                            :success [::crud-form/submit.next :admin.customization.menus]}]])})))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-menus-edit__page
    [form/form state-path
     [base/form {:on-submit (utils.ui/with-handler
                              #(rf/dispatch [::submit]))}
      [content]
      [base/form-button {:type "submit"}
       (i18n ::crud-form/submit)]]]]])

(routes/define-route!
 :admin.customization.menus.edit
 {:name ::page
  :url [:id "/edit"]
  :component page
  :init-fx [::init]})
