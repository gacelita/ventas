(ns ventas.themes.blank.pages.frontend
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.routes :as routes]
   [ventas.events :as events]
   [ventas.events.backend :as backend]))

(def state-key ::state)

(defn- example-pane [items]
  [:div.example-pane
   [:div.example-pane__inner
    (for [item items]
      ^{:key (:id item)}
      [:pre (with-out-str (cljs.pprint/pprint item))])]])

(defn page []
  [:div.blank-theme__home
   [base/container
    [:h1 "Hello!"]

    [:p "As you can see, there's nothing here."]
    [:p "However, you have the full ventas API at your disposition :)"]
    [:p "For example:"]

    [:h3 "Categories"]
    [example-pane @(rf/subscribe [::events/db [state-key :categories]])]

    [:h3 "Products (depends on Elasticsearch)"]
    [example-pane (:items @(rf/subscribe [::events/db [state-key :products]]))]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/products.aggregations
                  {:success [::events/db [state-key :products]]}]
                 [::backend/categories.list
                  {:success [::events/db [state-key :categories]]}]]}))

(routes/define-route!
  :frontend
  {:name "Blank theme - home"
   :url ""
   :component page
   :init-fx [::init]})
