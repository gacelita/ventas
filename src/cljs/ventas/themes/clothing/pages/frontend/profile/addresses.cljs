(ns ventas.themes.clothing.pages.frontend.profile.addresses
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.themes.clothing.pages.frontend.profile.skeleton :as profile.skeleton]
   [ventas.utils.validation :as validation]
   [ventas.utils :as utils]
   [ventas.utils.forms :as forms]
   [ventas.components.base :as base]
   [reagent.core :as reagent]
   [ventas.components.notificator :as notificator]))

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
   {:dispatch [:api/users.addresses.save
               {:params
                (->> (get db (::forms/state-key form-config))
                     (map (fn [[k v]]
                            [(keyword (name k)) (:value v)]))
                     (into {}))}]}))

(rf/reg-event-db
 ::cancel-edition
 (fn [db [_]]
   (dissoc db edition-key)))

(defn- address-form [address]
  (rf/dispatch [::forms/populate form-config (utils/map-keys #(utils/ns-kw %) address)])
  (let [data @(rf/subscribe [:ventas/db [(::forms/state-key form-config)]])]
    ^{:key (::forms/population-hash data)}
    [:div.addresses-page__form
     [base/header {:as "h3"
                   :attached "top"}
      (if (:id address)
        (i18n ::editing-address)
        (i18n ::new-address))]
     [base/segment {:attached true}
      [base/form {:error (forms/valid-form? data)}

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
         [forms/text-input form-config ::state]]]

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
   {:dispatch [:api/entities.remove
               {:params {:id eid}
                :success #(rf/dispatch [::remove.next eid])}]}))

(rf/reg-event-fx
 ::remove.next
 (fn [cofx [_ eid]]
   (let [update-call [:ventas/db.update
                      [addresses-key]
                      (fn [addresses]
                        (->> addresses
                             (remove #(= (:id %) eid))))]
         notify-call [::notificator/add {:message (i18n ::address-removed)
                                         :theme "success"}]]
     {:dispatch-n [update-call notify-call]})))

(rf/reg-event-fx
 ::edit
 (fn [cofx [_ address]]
   {:dispatch [:ventas/db [edition-key] address]}))

(defn- address-view [address]
  [:div
   [base/card
    [base/cardContent
     [:p (:first-name address) " " (:last-name address)]
     [:p (:address address) " " (:address-second-line address)]
     [:p (:zip address) " " (:city address) " " (:name (:state address))]
     [:p (:name (:country address))]]
    [base/cardContent {:extra true}

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
  (rf/dispatch [:api/users.addresses
                {:success #(rf/dispatch [:ventas/db [addresses-key] %])}])
  (fn [identity]
    [:div
     [base/header {:as "h3"}
      (i18n ::my-addresses)]
     (when-let [addresses @(rf/subscribe [:ventas/db [addresses-key]])]
       [base/grid {:columns 3 :class "smaller-padding"}
        [base/gridRow
         (for [address addresses]
           [base/gridColumn
            [address-view address]])]])

     (let [editing @(rf/subscribe [:ventas/db [edition-key]])]
       (if editing
         [address-form @(rf/subscribe [:ventas/db [edition-key]])]
         [base/button {:basic true
                       :color "grey"
                       :on-click #(rf/dispatch [::edit (select-keys identity #{:first-name :last-name :company})])}
          (i18n ::new-address)]))]))

(defn page []
  [profile.skeleton/skeleton
   [content (utils/get-identity)]])

(routes/define-route!
 :frontend.profile.addresses
 {:name (i18n ::page)
  :url ["addresses"]
  :component page})