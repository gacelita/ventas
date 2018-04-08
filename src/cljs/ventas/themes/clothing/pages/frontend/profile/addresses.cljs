(ns ventas.themes.clothing.pages.frontend.profile.addresses
  (:require
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.components.form :as form]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.session :as session]
   [ventas.themes.clothing.pages.frontend.profile.skeleton :as profile.skeleton]
   [ventas.utils :as utils :include-macros true]
   [ventas.utils.validation :as validation]))

(def state-key ::state)

(def regular-length-validator [::length-error validation/length-validator {:max 30}])

(def form-config
  {:db-path [state-key]
   :validators {::first-name [regular-length-validator]
                ::last-name [regular-length-validator]
                ::company [regular-length-validator]
                ::address [regular-length-validator]
                ::address-second-line [regular-length-validator]
                ::zip [[::length-error validation/length-validator {:max 10}]]
                ::city [regular-length-validator]
                ::state [regular-length-validator]
                ::email [[::email-error validation/email-validator]]
                ::phone [regular-length-validator]
                ::privacy-policy [[::required-error validation/required-validator]]}})

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} _]
   {:dispatch [::backend/users.addresses.save
               {:params (->> (form/get-data db [state-key])
                             (common.utils/map-keys #(keyword (name %))))
                :success ::save.next}]}))

(rf/reg-event-fx
 ::save.next
 (fn [{:keys [db]} [_ new-addr]]
   (let [data (form/get-data db [state-key])]
     {:dispatch-n [[::notificator/notify-saved]
                   [::set-editing? false]
                   [::events/db.update [state-key :addresses]
                    (if (:id data)
                      #(map (fn [address]
                              (if (= (:id address) (:id new-addr))
                                new-addr
                                address))
                            %)
                      #(conj % new-addr))]]})))

(rf/reg-event-db
 ::set-editing?
 (fn [db [_ editing?]]
   (assoc-in db [state-key :editing?] editing?)))

(defn entity->option [entity]
  {:text (:name entity)
   :value (:id entity)})

(rf/reg-event-db
 ::set-options
 (fn [db [_ path data]]
   (assoc-in db path (map entity->option data))))

(rf/reg-event-fx
 ::fetch-states
 (fn [_ [_ country]]
   {:dispatch [::backend/states.list
               {:params {:country country}
                :success [::set-options [state-key :states]]}]}))

(rf/reg-event-fx
 ::remove
 (fn [_ [_ eid]]
   {:dispatch [::backend/users.addresses.remove
               {:params {:id eid}
                :success [::remove.next eid]}]}))

(rf/reg-event-fx
 ::remove.next
 (fn [_ [_ eid]]
   {:dispatch-n [[::events/db.update [state-key :addresses]
                  (fn [addresses]
                    (->> addresses
                         (remove #(= (:id %) eid))))]
                 [::notificator/add {:message (i18n ::address-removed)
                                     :theme "success"}]]}))

(rf/reg-event-fx
 ::edit
 (fn [_ [_ {:keys [country state] :as address}]]
   {:dispatch-n [[::form/populate form-config (-> (common.utils/map-keys #(utils/ns-kw %) address)
                                                  (update ::state :id)
                                                  (update ::country :id))]
                 (when country
                   [::set-options [state-key :countries] [(:id country)]]
                   [::fetch-states (:id country)])
                 (when state
                   [::set-options [state-key :states] [(:id state)]])
                 [::set-editing? true]
                 [::backend/countries.list
                  {:success [::set-options [state-key :countries]]}]]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n key)})])

(defn- address-form []
  [form/form [state-key]
   (let [data @(rf/subscribe [::form/data [state-key]])]
     [:div.addresses-page__form
      [base/header {:as "h3"
                    :attached "top"}
       (if (::id data)
         (i18n ::editing-address)
         (i18n ::new-address))]
      [base/segment {:attached true}
       [base/form
        [base/form-group
         [field {:key ::first-name
                 :width 8}]

         [field {:key ::last-name
                 :width 8}]]

        [base/form-group
         [field {:key ::company
                 :width 16}]]

        [base/form-group
         [field {:key ::address
                 :width 10}]

         [field {:key ::address-second-line
                 :width 6}]]

        [base/form-group
         [field {:key ::city
                 :width 16}]]

        [base/form-group
         [field {:key ::country
                 :width 6
                 :type :combobox
                 :on-change-fx [::fetch-states]
                 :options @(rf/subscribe [::events/db [state-key :countries]])}]

         [field {:key ::state
                 :type :combobox
                 :width 6
                 :options @(rf/subscribe [::events/db [state-key :states]])}]

         [field {:key ::zip
                 :width 6}]]

        [base/form-group
         [field {:key ::phone
                 :width 16}]]

        [base/button {:type "button"
                      :basic true
                      :color "grey"
                      :icon true
                      :on-click #(rf/dispatch [::save])}
         [base/icon {:name "save"}]
         (i18n ::save)]
        [base/button {:type "button"
                      :basic true
                      :color "red"
                      :icon true
                      :on-click #(rf/dispatch [::set-editing? false])}
         [base/icon {:name "cancel"}]
         (i18n ::cancel)]]]])])

(defn- address-view [address]
  [base/card
   [base/card-content
    [:p (:first-name address) " " (:last-name address)]
    [:p (:address address) " " (:address-second-line address)]
    [:p (:zip address) " " (:city address) " " (:name (:state address))]
    [:p (:name (:country address))]]

   [base/card-content {:extra true}
    [base/button {:icon true
                  :basic true
                  :color "grey"
                  :on-click #(rf/dispatch [::edit address])}
     [base/icon {:name "edit"}]
     (i18n ::edit)]

    [base/button {:icon true
                  :basic true
                  :color "red"
                  :on-click #(rf/dispatch [::remove (:id address)])}
     [base/icon {:name "remove"}]
     (i18n ::remove)]]])

(defn- content []
  [:div
   [base/header {:as "h3"}
    (i18n ::my-addresses)]
   (when-let [addresses @(rf/subscribe [::events/db [state-key :addresses]])]
     [base/grid {:columns 3 :class "smaller-padding"}
      [base/grid-row
       (for [address addresses]
         [base/grid-column
          [address-view address]])]])

   (if @(rf/subscribe [::events/db [state-key :editing?]])
     [address-form]
     (let [identity (session/get-identity)]
       [base/button {:basic true
                     :color "grey"
                     :on-click #(rf/dispatch [::edit (select-keys identity #{:first-name :last-name :company})])}
        (i18n ::new-address)]))])

(defn page []
  [profile.skeleton/skeleton
   [content]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::session/require-identity]
                 [::backend/users.addresses
                  {:success [::events/db [state-key :addresses]]}]]}))

(routes/define-route!
  :frontend.profile.addresses
  {:name ::page
   :url ["addresses"]
   :component page
   :init-fx [::init]})
