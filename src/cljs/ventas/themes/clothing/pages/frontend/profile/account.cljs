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
   [ventas.utils.validation :as validation]))

(def state-key ::state-key)

(def regular-length-validator [::length-error validation/length-validator {:max 30}])

(def field-validators
  {::first-name [regular-length-validator]
   ::last-name [regular-length-validator]
   ::company [regular-length-validator]
   ::address [regular-length-validator]
   ::address-second-line [regular-length-validator]
   ::zip-code [[::length-error validation/length-validator {:max 10}]]
   ::city [regular-length-validator]
   ::state [regular-length-validator]
   ::email [[::email-error validation/email-validator]]
   ::phone [regular-length-validator]
   ::privacy-policy [[::required-error validation/required-validator]]})

(rf/reg-event-db
 ::populate
 (fn [db [_ data]]
   (->
    (reduce (fn [acc [field value]]
              (update-in acc [state-key (utils/ns-kw field) :value]
                         #(if (nil? %)
                            value
                            %)))
            db
            data)
    (assoc-in [state-key :population-hash] (hash data)))))

(rf/reg-event-db
 ::set-field
 (fn [db [_ field value]]
   (let [{:keys [valid? infractions]} (validation/validate field-validators field value)]
     (assoc-in db [state-key field] {:value value
                                     :valid? valid?
                                     :infractions infractions}))))

(rf/reg-event-fx
 ::save
 (fn [cofx [_]]
   ))

(defn- report-infraction [{:keys [identifier field value params]}]
  (apply i18n identifier (vals params)))

(defn- text-input [field]
  (let [{:keys [valid? value infractions]} @(rf/subscribe [:ventas/db [state-key field]])]
    [:div
     [base/form-input {:error (and (not (nil? valid?)) (not valid?))
                       :label (i18n field)
                       :default-value value
                       :on-change (utils/value-handler
                                   #(rf/dispatch [::set-field field %]))}]
     (when (seq infractions)
       (for [[identifier params] infractions]
         [base/message
          {:error true
           :content (report-infraction {:identifier identifier
                                        :field field
                                        :value value
                                        :params params})}]))]))

(defn- content [identity]
  (rf/dispatch [::populate identity])
  (let [data @(rf/subscribe [:ventas/db [state-key]])]
    ^{:key (:population-hash data)}
    [base/form {:error (not (every? clojure.core/identity (map :valid? (vals data))))}

     [base/form-group
      [base/form-field {:width 5}
       [text-input ::first-name]]

      [base/form-field {:width 11}
       [text-input ::last-name]]]

     [base/form-group
      [base/form-field {:width 16}
       [text-input ::company]]]

     [base/form-group
      [base/form-field {:width 8}
       [text-input ::address]]

      [base/form-field {:width 8}
       [text-input ::address-second-line]]]

     [base/form-group
      [base/form-field {:width 2}
       [text-input ::zip-code]]

      [base/form-field {:width 7}
       [text-input ::city]]

      [base/form-field {:width 7}
       [text-input ::state]]]

     [base/form-group
      [base/form-field {:width 8}
       [text-input ::email]]

      [base/form-field {:width 8}
       [text-input ::phone]]]

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
           :on-change #(rf/dispatch [::set-field field (get (js->clj %2) "checked")])}]])]



     [base/button {:type "button"
                   :on-click #(rf/dispatch [::save])}
      (i18n ::save)]]))

(defn page []
  [profile.skeleton/skeleton content])

(routes/define-route!
 :frontend.profile.account
 {:name (i18n ::page)
  :url ["account"]
  :component page})