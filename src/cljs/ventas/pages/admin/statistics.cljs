(ns ventas.pages.admin.statistics
  (:require
   [cljsjs.moment]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.chart :as chart]
   [ventas.components.datepicker :as datepicker]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.form :as form]
   [ventas.common.utils :as common.utils]
   [clojure.set :as set]
   [clojure.string :as str])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(def default-interval (* 5 1000))

(defn update-datasets [datasets topics topics-data existing-data?]
  (let [topic->idx (->> topics (map-indexed vector) (into {}) (set/map-invert))]
    (reduce (fn [acc [topic data]]
              (update acc (topic->idx topic) #(let [old (vec %)
                                                    new (map second data)]
                                                (into (if existing-data?
                                                        (vec (nthrest old (count new)))
                                                        old)
                                                      new))))
            datasets
            topics-data)))

(defn- get-labels [topics-data]
  (->> topics-data
       (first)
       (val)
       (map (comp js/moment first))))

(defn- update-labels [old-labels topics-data existing-data?]
  (let [new-labels (get-labels topics-data)]
    (into (if existing-data?
            (vec (nthrest old-labels (count new-labels)))
            old-labels)
          new-labels)))

(rf/reg-event-fx
 ::stats.fetch.next
 (fn [{:keys [db]} [_ {:keys [min topics]} topics-data]]
   (let [{:keys [labels max-y]} (chart/get-data db ::chart)
         existing-data? (seq labels)
         topics-data (common.utils/map-vals
                      (fn [buckets]
                        (as-> buckets %
                              (if existing-data?
                                %
                                (update % min #(or % 0)))
                              (sort-by first %)))
                      topics-data)
         max-value (->> topics-data
                        (vals)
                        (mapcat identity)
                        (map second)
                        (apply max))]
     {:dispatch [::chart/update {:id ::chart
                                 :labels-fn (fn [old-labels]
                                              (update-labels old-labels topics-data existing-data?))
                                 :max-y (if-not existing-data?
                                          max-value
                                          (max max-y max-value))
                                 :data-fn (fn [datasets]
                                            (update-datasets datasets topics topics-data existing-data?))}]})))

(rf/reg-event-fx
 ::stats.fetch
 (fn [_ [_ {:keys [min max interval]}]]
   (let [params {:topics ["navigation" "http"]
                 :min min
                 :max max
                 :interval (or interval default-interval)}]
     {:dispatch-n [[::chart/empty {:id ::chart}]
                   [::backend/admin.stats.realtime
                    {:params params
                     :channel-key ::admin.stats.realtime
                     :success [::stats.fetch.next params]}]]})))

(def custom-settings-db-path
  [state-key :custom-settings])

(def realtime-settings-db-path
  [state-key :realtime-settings])

(defn- traffic-stats-chart []
  [chart/chart
   {:height 500
    :id ::chart
    :config {:type "line"
             :animation false
             :data {:labels []
                    :datasets [{:data []
                                :label (i18n ::navigation-events)
                                :backgroundColor "rgb(75, 192, 192)"
                                :borderColor "rgb(75, 192, 192)"
                                :fill false}
                               {:data []
                                :label (i18n ::http-events)
                                :backgroundColor "rgb(255, 99, 132)"
                                :borderColor "rgb(255, 99, 132)"
                                :fill false}]}
             :options {:responsive true
                       :animation false
                       :elements {:line {:tension 0}}
                       :bezierCurve false
                       :maintainAspectRatio false
                       :scales {:xAxes [{:type "time"
                                         :time {:format "DD/MM/YYYY HH:mm:ss"
                                                :displayFormats {"millisecond" "MMM D HH:mm:ss"
                                                                 "second" "MMM D HH:mm:ss"
                                                                 "minute" "MMM D HH:mm:ss"
                                                                 "hour" "MMM D HH:mm"
                                                                 "day" "MMM D HH:mm"}
                                                :tooltipFormat "ll HH:mm:ss"}}]
                                :yAxes [{:ticks {:beginAtZero true}}]}}}}])

(def one-minute (* 60 1000))
(def one-hour (* 60 60 1000))
(def ten-minutes (* 10 60 1000))
(def one-day (* 24 60 60 1000))

(defn- view-params [id]
  (let [current-time (.getTime (js/Date.))]
    (case id
      :24h {:min (- current-time one-day)
            :interval ten-minutes}
      :week {:min (-> (js/moment) (.startOf "week") (.isoWeekday 1) (.valueOf))
             :interval one-hour}
      :month {:min (-> (js/moment) (.startOf "month") (.valueOf))
              :interval one-hour})))

(rf/reg-event-fx
 ::view.select
 (fn [{:keys [db]} [_ id]]
   [[:db (assoc-in db [state-key :view] id)]
    (when-not (contains? #{:custom :realtime} id)
      [:dispatch [::stats.fetch (view-params id)]])]))

(rf/reg-sub
 ::view
 (fn [db]
   (get-in db [state-key :view])))

(rf/reg-event-fx
 ::range.select
 (fn [_ [_ {:keys [start end]}]]
   (let [start (.valueOf start)
         end (.valueOf end)]
     (when (and start end (< start end))
       {:dispatch-n [[::form/set-field custom-settings-db-path :range {:min start :max end}]
                     [::stats.apply-custom-view]]}))))

(rf/reg-event-fx
 ::stats.apply-custom-view
 (fn [{:keys [db]} _]
   (let [{:keys [:granularity/type :granularity/amount :range]} (form/get-data db custom-settings-db-path)]
     (when (and type amount range (pos? amount))
       {:dispatch [::stats.fetch {:min (:min range)
                                  :max (:max range)
                                  :interval (str amount (name type))}]}))))

(defn- make-interval [type amount]
  (case type
    :s (* amount 1000)
    :m (* amount 60 1000)))

(rf/reg-event-fx
 ::stats.apply-realtime-view
 (fn [{:keys [db]} _]
   (let [{:keys [:granularity/type :granularity/amount :period]} (form/get-data db realtime-settings-db-path)]
     (when (and type amount period)
       {:dispatch [::stats.fetch {:min (let [current-time (.getTime (js/Date.))]
                                         (case period
                                           :m (- current-time one-minute)
                                           :h (- current-time one-hour)))
                                  :interval (make-interval type amount)}]}))))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::form/populate custom-settings-db-path {:granularity/type :h
                                                           :granularity/amount 1}]
                 [::view.select :24h]]}))

(defn- custom-settings []
  [form/form custom-settings-db-path
   [base/form
    [:span (i18n ::every)]
    [form/field {:key :granularity/amount
                 :db-path custom-settings-db-path
                 :type :number
                 :on-change-fx [::stats.apply-custom-view]
                 :placeholder (i18n ::granularity.amount)
                 :min 1
                 :max 60}]
    [form/field {:key :granularity/type
                 :db-path custom-settings-db-path
                 :type :combobox
                 :on-change-fx [::stats.apply-custom-view]
                 :placeholder (i18n ::granularity.type)
                 :default-value :h
                 :options [{:value :m
                            :text (str/lower-case (i18n ::minutes))}
                           {:value :h
                            :text (str/lower-case (i18n ::hours))}
                           {:value :d
                            :text (str/lower-case (i18n ::days))}
                           {:value :w
                            :text (str/lower-case (i18n ::weeks))}
                           {:value :M
                            :text (str/lower-case (i18n ::months))}]}]]])

(defn- realtime-settings []
  [form/form realtime-settings-db-path
   [base/form
    [:span (i18n ::show-last)]
    [form/field {:key :period
                 :db-path realtime-settings-db-path
                 :type :combobox
                 :on-change-fx [::stats.apply-realtime-view]
                 :placeholder (i18n ::realtime-period)
                 :default-value :h
                 :options [{:value :m
                            :text (i18n ::minute)}
                           {:value :h
                            :text (i18n ::hour)}]}]
    [:span (i18n ::every)]
    [form/field {:key :granularity/amount
                 :db-path realtime-settings-db-path
                 :type :number
                 :on-change-fx [::stats.apply-realtime-view]
                 :placeholder (i18n ::granularity.amount)
                 :min 1
                 :max 60}]
    [form/field {:key :granularity/type
                 :db-path realtime-settings-db-path
                 :type :combobox
                 :on-change-fx [::stats.apply-realtime-view]
                 :placeholder (i18n ::granularity.type)
                 :default-value :m
                 :options [{:value :m
                            :text (i18n ::minutes)}
                           {:value :s
                            :text (i18n ::seconds)}]}]]])

(defn- view-option [id label]
  [base/button
   {:active (= id @(rf/subscribe [::view]))
    :on-click #(rf/dispatch [::view.select id])}
   label])

(defn- view-options [options]
  [base/button-group
   (for [[id name] options]
     [view-option id name])])

(defn- content []
  [:div.admin-statistics__traffic
   [base/segment
    [:div.admin-statistics__traffic-header
     [base/header {:as "h3"}
      (i18n ::traffic-statistics)]
     [view-options [[:24h (i18n ::twenty-four-hours)]
                    [:week (i18n ::week)]
                    [:month (i18n ::month)]
                    [:custom (i18n ::custom)]
                    [:realtime (i18n ::realtime)]]]]
    (let [current-view @(rf/subscribe [::view])]
      [:div
       (when (= current-view :custom)
         [:div.admin-statistics__traffic-custom
          [:span (i18n ::show-data-from)]
          [datepicker/range-input {:placeholder (i18n ::datepicker-placeholder)
                                   :on-change-fx [::range.select]}]
          [custom-settings]])
       (when (= current-view :realtime)
         [:div.admin-statistics__traffic-realtime
          [realtime-settings]])])
    [traffic-stats-chart]]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-statistics__page
    [content]]])

(routes/define-route!
 :admin.statistics
 {:name ::page
  :url "statistics"
  :component page
  :init-fx [::init]})