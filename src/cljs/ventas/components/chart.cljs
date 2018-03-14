(ns ventas.components.chart
  "ChartJS wrapper"
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ventas.events :as events]
   [cljsjs.chartjs]))

(def state-key ::state)

(defn- set-labels! [chart labels]
  {:pre [chart]}
  (aset chart "data" "labels" (clj->js labels)))

(defn- set-data! [chart n data]
  {:pre [chart]}
  (aset chart "data" "datasets" (str n) "data" (clj->js data)))

(rf/reg-event-db
 ::update
 (fn [db [_ {:keys [labels-fn data-fn id]}]]
   (if-let [chart (get-in db [state-key id :chart])]
     (let [{:keys [labels data]} (get-in db [state-key id])
           data (data-fn data)
           labels (labels-fn labels)]
       (set-labels! chart labels)
       (set-data! chart 0 data)
       (.update chart)
       (-> db
           (assoc-in [state-key id :labels] labels)
           (assoc-in [state-key id :data] data)))
     db)))

(defn- make-config [config labels data]
  (-> config
      (assoc-in [:data :labels] labels)
      (assoc-in [:data :datasets 0 :data] data)))

(defn- render-chart [config id this]
  (let [context (-> (reagent/dom-node this)
                    (.querySelector "canvas")
                    (.getContext "2d"))
        {:keys [labels data]} @(rf/subscribe [::events/db [state-key id]])
        chart (js/Chart. context (clj->js (make-config config labels data)))]
    (rf/dispatch [::events/db [state-key id] {:chart chart
                                              :labels (or labels [])
                                              :data (or data [])}])))

(defn chart [{:keys [height config id]}]
  (reagent/create-class
   {:component-did-mount (partial render-chart config id)
    :display-name "chart"
    :reagent-render (fn []
                      [:div {:style {:width "100%"
                                     :height height}}
                       [:canvas]])}))