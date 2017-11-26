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
   [reagent.core :as reagent]))

(def addresses-key ::addresses)

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

(rf/reg-event-db
 ::save
 (fn [db [_]]
   (let [data (get db (::forms/state-key form-config))]
     (js/console.log data)
     db)))

(defn- address-form [address]
  (rf/dispatch [::forms/populate form-config (utils/map-keys #(utils/ns-kw %) address)])
  (let [data @(rf/subscribe [:ventas/db [(::forms/state-key form-config)]])]
    ^{:key (::forms/population-hash data)}
    [:div
     [base/header {:as "h3"
                   :attached "top"}
      (i18n ::new-address)]
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
                     :on-click #(rf/dispatch [::save])}
        (i18n ::save)]]]]))

(defn- address-view [address]
  (rf/dispatch [:ventas/entities.sync (:country address)])
  (rf/dispatch [:ventas/entities.sync (:state address)])
  (fn [address]
    (let [country @(rf/subscribe [:ventas/db [:entities (:country address)]])
          state @(rf/subscribe [:ventas/db [:entities (:state address)]])]
      (js/console.log "address" address "country" country "state" state)
      (when (and (or (nil? (:country address)) country)
                 (or (nil? (:state address)) state))
        [:div
         [base/segment
          [:p (:first-name address) " " (:last-name address)]
          [:p (:address address) " " (:address-second-line address)]
          [:p (:zip-code address) " " (:city address) " " (:name state)]
          [:p (:name country)]]]))))

(defn- content [identity]
  (rf/dispatch [:api/users.addresses
                {:success-fn #(rf/dispatch [:ventas/db [addresses-key] %])}])
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
     [address-form identity]]))

(defn page []
  [profile.skeleton/skeleton
   [content (utils/get-identity)]])

(routes/define-route!
 :frontend.profile.addresses
 {:name (i18n ::page)
  :url ["addresses"]
  :component page})