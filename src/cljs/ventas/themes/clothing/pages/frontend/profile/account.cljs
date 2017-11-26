(ns ventas.themes.clothing.pages.frontend.profile.account
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.themes.clothing.pages.frontend.profile.skeleton :as profile.sidebar]
   [ventas.utils :as utils]
   [ventas.themes.clothing.pages.frontend.profile.skeleton :as profile.skeleton]
   [reagent.core :as reagent]
   [ventas.utils.validation :as validation]
   [ventas.utils.forms :as forms]))

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
 (fn [cofx [_]]))

(defn- content [identity]
  (rf/dispatch [::forms/populate form-config (utils/map-keys #(utils/ns-kw %) identity)])
  (let [data @(rf/subscribe [:ventas/db [(::forms/state-key form-config)]])]
    ^{:key (::forms/population-hash data)}
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

     [base/form-group
      [base/form-field {:width 8}
       [forms/text-input form-config ::email]]

      [base/form-field {:width 8}
       [forms/text-input form-config ::phone]]]

     [base/form-group
      (let [field ::privacy-policy
            {:keys [valid? value infractions]} (get data field)]
        [base/form-field {:control "checkbox"
                          :error (and (not (nil? valid?)) (not valid?))}
         [base/checkbox
          {:label (reagent/as-element
                   [:label (str (i18n ::privacy-policy-text) " ")
                    [:a {:href #(routes/path-for :frontend.privacy-policy)}
                     (i18n field)]])
           :on-change #(rf/dispatch [::set-field form-config field (get (js->clj %2) "checked")])}]])]



     [base/button {:type "button"
                   :on-click #(rf/dispatch [::save])}
      (i18n ::save)]]))

(defn page []
  [profile.skeleton/skeleton
   [content (utils/get-identity)]])

(routes/define-route!
 :frontend.profile.account
 {:name (i18n ::page)
  :url ["account"]
  :component page})