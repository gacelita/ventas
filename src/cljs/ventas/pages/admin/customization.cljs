(ns ventas.pages.admin.customization
  (:require
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.events.backend :as backend]
   [re-frame.core :as rf]
   [ventas.i18n :refer [i18n]]
   [ventas.components.form :as form]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(admin.skeleton/add-menu-item!
 {:route :admin.payment-methods.stripe
  :label ::page
  :parent :admin.payment-methods})

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.configuration.set
               {:params (get-in db [state-key :form])
                :success [::notificator/notify-saved]}]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw key))})])

(defonce ^:private fields
  (atom {:customization/name {}
         :customization/logo {:type :image}
         :customization/header-image {:type :image}
         :customization/background-color {:type :color}
         :customization/foreground-color {:type :color}
         :customization/product-listing-mode {:type :radio}
         :customization/font-family {}}))

(defn add-field! [key config]
  (swap! fields assoc key config))

(defn remove-field! [key]
  (swap! fields dissoc key))

(defn- content []
  [form/form [state-key]
   [base/segment {:color "orange"
                  :title (i18n ::page)}
    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

     (for [[key field-config] @fields]
       [field (assoc field-config :key key)])

     [base/divider {:hidden true}]

     [base/form-button
      {:type "submit"}
      (i18n ::submit)]]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/configuration.get
                  {:params (set (keys @fields))
                   :success [::form/populate [state-key]]}]]}))

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-customization__page
    [content]]])

(admin.skeleton/add-menu-item!
 {:route :admin.customization
  :icon "edit"
  :label ::page})

(routes/define-route!
 :admin.customization
 {:name ::page
  :url "customization"
  :component page
  :init-fx [::init]})