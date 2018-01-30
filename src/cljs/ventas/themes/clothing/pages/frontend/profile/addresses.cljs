(ns ventas.themes.clothing.pages.frontend.profile.addresses
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.themes.clothing.pages.frontend.profile.skeleton :as profile.skeleton]
   [ventas.utils.validation :as validation]
   [ventas.utils :as utils :include-macros true]
   [ventas.utils.forms :as forms]
   [ventas.components.base :as base]
   [reagent.core :as reagent]
   [ventas.components.notificator :as notificator]
   [ventas.common.utils :as common.utils]
   [ventas.events.backend :as backend]
   [ventas.session :as session]
   [ventas.events :as events]))

(def addresses-key ::addresses)

(def edition-key ::edition)

(def regular-length-validator [::length-error validation/length-validator {:max 30}])

(def form-config
  {::forms/state-key ::state-key
   ::forms/validators {::first-name [regular-length-validator]
                       ::last-name [regular-length-validator]
                       ::company [regular-length-validator]
                       ::address [regular-length-validator]
                       ::address-second-line [regular-length-validator]
                       ::zip-code [[::length-error validation/length-validator {:max 10}]]
                       ::city [regular-length-validator]
                       ::state [regular-length-validator]
                       ::email [[::email-error validation/email-validator]]
                       ::phone [regular-length-validator]
                       ::privacy-policy [[::required-error validation/required-validator]]}})

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} [_]]
   {:dispatch [::backend/users.addresses.save
               {:params
                (->> (forms/get-values form-config)
                     (common.utils/map-keys #(keyword (name %))))
                :success #(do (rf/dispatch [::notificator/add
                                            {:message (i18n ::address-saved)
                                             :theme "success"}])
                              (rf/dispatch [::cancel-edition]))}]}))

(rf/reg-event-db
 ::cancel-edition
 (fn [db [_]]
   (dissoc db edition-key)))

(defn- address-form [address]
  (rf/dispatch [::backend/states.list
                {:success #(forms/set-field-property! form-config ::state :options %)}])

  (rf/dispatch [::backend/countries.list
                {:success #(forms/set-field-property! form-config ::country :options %)}])

  (fn [address]

    ^{:key (forms/get-key form-config)}
    [:div.addresses-page__form
     [base/header {:as "h3"
                   :attached "top"}
      (if (forms/get-value form-config ::id)
        (i18n ::editing-address)
        (i18n ::new-address))]
     [base/segment {:attached true}
      [base/form

       [base/form-group
        [base/form-field {:width 5}
         [forms/text-input form-config ::first-name]]

        [base/form-field {:width 11}
         [forms/text-input form-config ::last-name]]]

       [base/form-group
        [base/form-field {:width 16}
         [forms/text-input form-config ::company]]]

       [base/form-group
        [base/form-field {:width 8}
         [forms/text-input form-config ::address]]

        [base/form-field {:width 8}
         [forms/text-input form-config ::address-second-line]]]

       [base/form-group
        [base/form-field {:width 2}
         [forms/text-input form-config ::zip-code]]

        [base/form-field {:width 7}
         [forms/text-input form-config ::city]]

        [base/form-field {:width 7}
         [forms/dropdown-input form-config ::state]]]

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
                     :on-click #(rf/dispatch [::cancel-edition])}
        [base/icon {:name "cancel"}]
        (i18n ::cancel)]]]]))

(rf/reg-event-fx
 ::remove
 (fn [cofx [_ eid]]
   {:dispatch [::backend/users.addresses.remove
               {:params {:id eid}
                :success #(rf/dispatch [::remove.next eid])}]}))

(rf/reg-event-fx
 ::remove.next
 (fn [cofx [_ eid]]
   (let [update-call [::events/db.update
                      [addresses-key]
                      (fn [addresses]
                        (->> addresses
                             (remove #(= (:id %) eid))))]
         notify-call [::notificator/add {:message (i18n ::address-removed)
                                         :theme "success"}]]
     {:dispatch-n [update-call notify-call]})))

(defn transform-address-for-edition [address]
  (-> address
      (update ::state :id)
      (update ::country :id)))

(rf/reg-event-fx
 ::edit
 (fn [cofx [_ address]]
   {:dispatch-n [[::events/db [edition-key] true]
                 [::forms/populate form-config (->> address
                                                    (common.utils/map-keys #(utils/ns-kw %))
                                                    (transform-address-for-edition))]]}))

(defn- address-view [address]
  [:div
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
      (i18n ::remove)]]]])

(defn- content [identity]
  (rf/dispatch [::backend/users.addresses
                {:success #(rf/dispatch [::events/db [addresses-key] %])}])
  (fn [identity]
    [:div
     [base/header {:as "h3"}
      (i18n ::my-addresses)]
     (when-let [addresses @(rf/subscribe [::events/db [addresses-key]])]
       [base/grid {:columns 3 :class "smaller-padding"}
        [base/grid-row
         (for [address addresses]
           [base/grid-column
            [address-view address]])]])

     (if @(rf/subscribe [::events/db [edition-key]])
       [address-form]
       [base/button {:basic true
                     :color "grey"
                     :on-click #(rf/dispatch [::edit (select-keys identity #{:first-name :last-name :company})])}
        (i18n ::new-address)])]))

(defn page []
  [profile.skeleton/skeleton
   [content (session/get-identity)]])

(routes/define-route!
 :frontend.profile.addresses
 {:name ::page
  :url ["addresses"]
  :component page
  :init-fx [::session/require-identity]})