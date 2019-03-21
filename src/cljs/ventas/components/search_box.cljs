(ns ventas.components.search-box
  (:require
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [ventas.server.api :as backend]
   [ventas.components.image :as image])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-sub
 ::items
 (fn [db [_ id]]
   (get-in db [state-key id :search])))

(rf/reg-sub
 ::query
 (fn [db [_ id]]
   (get-in db [state-key id :search-query])))

(rf/reg-event-db
 ::query.set
 (fn [db [_ id query]]
   (assoc-in db [state-key id :search-query] query)))

(rf/reg-event-db
 ::items.set
 (fn [db [_ id items]]
   (if (empty? items)
     (update-in db [state-key id] #(dissoc % :search))
     (assoc-in db [state-key id :search] items))))

(rf/reg-event-fx
 ::search
 (fn [_ [_ id search-event query]]
   {:dispatch-n
    (if (empty? query)
      [[::items.set id nil]]
      [[::query.set id query]
       (if search-event
         (conj search-event query)
         [::backend/search {:params {:search query}
                            :success [::items.set id]}])])}))

(defn- search-result-view [on-result-click {:keys [name image type] :as item}]
  [:div.search-result
   (when on-result-click
     {:on-click (partial on-result-click item)})
   (when image
     [:img {:src (image/get-url (:id image) :header-search)}])
   [:div.search-result__right
    [:p.search-result__name name]
    (when type
      [:p.search-result__type (i18n (keyword "schema.type" type))])]])

(defn ->options [on-result-click coll]
  (map (fn [item]
         {:value (:id item)
          :text (:name item)
          :key (:id item)
          :content (r/as-element [search-result-view on-result-click item])})
       coll))

(defn search-box [{:keys [id on-result-click search-event] :as props}]
  [base/dropdown
   (merge
    {:placeholder (i18n ::search)
     :class "search-box"
     :selection true
     :icon "search"
     :search (fn [options _] options)
     :options (->options on-result-click @(rf/subscribe [::items id]))
     :on-search-change #(rf/dispatch [::search id search-event (-> % .-target .-value)])}
    props)])