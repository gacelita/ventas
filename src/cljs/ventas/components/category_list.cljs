(ns ventas.components.category-list
  (:require [ventas.util :as util]
            [re-frame.core :as rf]
            [clojure.string :as s]
            [soda-ash.core :as sa]
            [fqcss.core :refer [wrap-reagent]]
            [cljs.pprint :refer [pprint]]
            [ventas.routes :as routes]))

(rf/reg-sub ::main
  (fn [db _] (-> db :categories)))

(rf/reg-event-fx ::init
  (fn [cofx [_]]
    (js/console.log "initing")
    {:ws-request {:name :categories/list
                  :success-fn #(rf/dispatch [:app/entity-query.next [::main] %])}}))

(rf/reg-event-fx
 ::add
 (fn [{:keys [db local-storage]} [_ item]]
   {:db (assoc-in db [:categories (:id item)] item)}))

(defn category-list []
  "Category list"
  (js/console.log "rendering")
  [:div.category-list
   (for [[id category] @(rf/subscribe [::main])]
     (do
       (js/console.log "category" category)
       ^{:key id}
       [:div.category-list__category {:key (:id category)
                                      :on-click #(routes/go-to :frontend.category :id 1)}
        (when (:image category)
          [:img.category-list__image {:src (:url (:image category))}])
        [:div.category-list__content
         [:h3 (:name category)]
         (when (:description category)
           [:p (:description category)])]]))])