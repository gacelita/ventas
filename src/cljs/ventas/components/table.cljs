(ns ventas.components.table
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.utils.formatting :as utils.formatting]
   [ventas.routes :as routes]))

(defn amount-column [key data]
  (let [amount (get data key)]
    [:p (utils.formatting/amount->str amount)]))

(defn link-column [route id-key label-key data]
  [:a {:href (routes/path-for route :id (get data id-key))}
   (get data label-key)])

(defn get-state [db state-path]
  (get-in db state-path))

(rf/reg-sub
 ::state
 (fn [db [_ state-path]]
   (get-state db state-path)))

(rf/reg-event-db
 ::set-state
 (fn [db [_ state-path state]]
   (assoc-in db state-path state)))

(rf/reg-event-db
 ::update-state
 (fn [db [_ state-path f]]
   (update-in db state-path f)))

(rf/reg-event-fx
 ::init
 (fn [_ [_ state-path state]]
   (let [{:keys [fetch-fx] :as state} (merge {:page 0
                                              :items-per-page 10
                                              :sort-direction :asc
                                              :sort-column :id}
                                             state)]
     {:dispatch-n [[::set-state state-path state]
                   (when fetch-fx
                     (conj fetch-fx state-path))]})))

(rf/reg-event-fx
 ::set-page
 (fn [{:keys [db]} [_ state-path page]]
   {:dispatch-n [[::update-state state-path #(assoc % :page page)]
                 (when-let [fetch-fx (:fetch-fx (get-state db state-path))]
                   (conj fetch-fx state-path))]}))

(rf/reg-event-db
 ::set-rows
 (fn [db [_ state-path {:keys [total rows]}]]
   (-> db
       (assoc-in (conj state-path :rows) rows)
       (assoc-in (conj state-path :total) total))))

(rf/reg-event-fx
 ::sort
 (fn [{:keys [db]} [_ state-path column]]
   (let [{:keys [sort-direction sort-column fetch-fx] :as state} (get-state db state-path)
         new-direction (if (not= sort-column column)
                         :asc
                         (if (= sort-direction :asc) :desc :asc))]
     {:dispatch-n [[::set-state state-path (merge state
                                                  {:sort-direction new-direction
                                                   :sort-column column
                                                   :page 0})]
                   (when fetch-fx
                     (conj fetch-fx state-path))]})))

(def pagination-width 1)

(def pagination-total-width (+ 5 (* pagination-width 2)))

(defn- right-placeholder [pages]
  (concat (take (- pagination-total-width 2) pages)
          [::placeholder]
          [(last pages)]))

(defn- left-placeholder [pages]
  (concat [(first pages)]
          [::placeholder]
          (take-last (- pagination-total-width 2) pages)))

(defn- both-placeholders [pages current]
  (concat [(first pages)]
          [::placeholder]
          (subvec pages (- current pagination-width) (inc (+ current pagination-width)))
          [::placeholder]
          [(last pages)]))

(defn- get-page-numbers [total current]
  (let [pages (vec (range total))]
    (if (<= total pagination-total-width)
      pages
      (cond
        (< current 4) (right-placeholder pages)
        (> current (- total 5)) (left-placeholder pages)
        :default (both-placeholders pages current)))))

(defn table [state-path]
  (let [{:keys [total items-per-page page sort-direction sort-column
                footer columns rows]} @(rf/subscribe [::state state-path])]
    [base/table {:celled true :sortable true :unstackable true}
     [base/table-header
      [base/table-row
       (for [{:keys [id label]} columns]
         [base/table-header-cell
          {:key id
           :sorted (if (= sort-column id)
                     (if (= sort-direction :asc)
                       "ascending"
                       "descending"))
           :on-click #(rf/dispatch [::sort state-path id])}
          label])]]
     [base/table-body
      (if (empty? rows)
        [base/table-row
         [base/table-cell {:col-span (count columns)}
          [:p.table-component__no-rows (i18n ::no-rows)]]]
        (for [row rows]
          [base/table-row
           {:key (hash row)}
           (for [{:keys [id component width]} columns]
             [base/table-cell {:key id
                               :style (when width
                                        {:width width})}
              (if component
                [component row]
                (id row))])]))]
     [base/table-footer
      [base/table-row
       [base/table-header-cell {:col-span (count columns)}
        (when footer
          [footer])
        (let [total-pages (Math/ceil (/ total items-per-page))]
          [base/menu {:floated "right" :pagination true}
           [base/menu-item {:icon true
                            :disabled (= page 0)
                            :on-click #(rf/dispatch [::set-page state-path (dec page)])}
            [base/icon {:name "left chevron"}]]
           (map-indexed
            (fn [idx n]
              (if (= ::placeholder n)
                [base/menu-item {:key idx} "..."]
                [base/menu-item
                 {:active (= n page)
                  :key idx
                  :on-click #(rf/dispatch [::set-page state-path n])}
                 (str (inc n))]))
            (get-page-numbers total-pages page))
           [base/menu-item {:icon true
                            :disabled (= page (dec total-pages))
                            :on-click #(rf/dispatch [::set-page state-path (inc page)])}
            [base/icon {:name "right chevron"}]]])]]]]))
