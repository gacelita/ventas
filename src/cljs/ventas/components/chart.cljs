(ns ventas.components.chart
  "ChartJS wrapper"
  (:require
   [chart.js]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom]
   [ventas.events :as events]
   [ventas.common.utils :as common.utils]))

(def state-key ::state)

(defn- set-labels [chart labels]
  {:pre [chart]}
  [:aset [chart "data" "labels" (clj->js labels)]])

(defn- set-data [chart n data]
  {:pre [chart]}
  [:aset [chart "data" "datasets" (str n) "data" (clj->js data)]])

(defn- set-y-axis-max [chart max]
  [:aset [chart "options" "scales" "yAxes" 0 "ticks" "suggestedMax" max]])

(rf/reg-fx
 :chart/update
 (fn [chart]
   (.update chart)))

(defn- get-data [db id]
  (get-in db [state-key id]))

(rf/reg-sub
 ::data
 (fn [db [_ id]]
   (get-data db id)))

(rf/reg-event-fx
 ::update
 (fn [{:keys [db]} [_ {:keys [labels-fn data-fn id max-y]}]]
   (if-let [chart (get-in db [state-key id :chart])]
     (let [{:keys [labels data]} (get-in db [state-key id])
           data (data-fn data)
           labels (labels-fn labels)]
       (common.utils/into-n
        [(set-labels chart labels)
         (set-y-axis-max chart max-y)]
        (for [n (range (count data))]
          (set-data chart n (get data n)))
        [[:chart/update chart]
         [:db (-> db
                  (assoc-in [state-key id :labels] labels)
                  (assoc-in [state-key id :data] data)
                  (assoc-in [state-key id :max-y] max-y))]]))
     {:db db})))

(rf/reg-event-fx
 ::empty
 (fn [{:keys [db]} [_ {:keys [id]}]]
   (if-let [chart (get-in db [state-key id :chart])]
     (do
       [(set-labels chart [])
        (set-y-axis-max chart 10)]
       (for [n (range (count (aget chart "data" "datasets")))]
         (set-data chart n []))
       [[:chart/update chart]
        [:db (-> db
                 (assoc-in [state-key id :labels] [])
                 (assoc-in [state-key id :data] []))]])
     {:db db})))

(defn- make-config [config {:keys [labels data max-y]}]
  (let [config (-> config
                   (assoc-in [:data :labels] labels)
                   (assoc-in [:data :options :scales :yAxes 0 :ticks :suggestedMax] max-y))]
    (reduce-kv (fn [acc idx itm]
                 (assoc-in acc [:data :datasets idx :data] itm))
               config
               data)))

(defn- render-chart [config id this]
  (let [context (-> (reagent/dom-node this)
                    (.querySelector "canvas")
                    (.getContext "2d"))
        {:keys [labels data] :as state} @(rf/subscribe [::events/db [state-key id]])
        chart (chart.js/Chart. context (clj->js (make-config config state)))]
    (rf/dispatch [::events/db [state-key id] {:chart chart
                                              :labels (or labels [])
                                              :data (or data [])}])))

(defn chart* [{:keys [config id]}]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (render-chart config id this))
    :reagent-render (fn [{:keys [height]}]
                      [:div {:style {:width "100%"
                                     :height height}}
                       [:canvas]])}))

(defn chart [_]
  (let [key (ratom/atom nil)
        resize-listener #(reset! key (str (gensym)))]
    (reagent/create-class
     {:component-did-mount
      (fn [_]
        (.addEventListener js/window "resize" resize-listener))
      :component-will-unmount
      (fn [_]
        (.removeEventListener js/window "resize" resize-listener))
      :reagent-render
      (fn [config]
        ^{:key @key}
        [chart* config])})))