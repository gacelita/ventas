(ns ventas.components.table
  "Meant to replace re-frame-datatable"
  (:require
    [re-frame.core :as rf]
    [ventas.components.base :as base]
    [day8.re-frame.forward-events-fx]
    [ventas.events :as events]))

(rf/reg-event-db
  ::set-state
  (fn [db [_ state-path new-state]]
    (assoc-in db state-path new-state)))

(rf/reg-event-fx
  ::set-state.listener
  (fn [_ [_ {:keys [fetch-fx] :as config}]]
    {:forward-events {:register ::set-page.listener
                      :events #{::set-state}
                      :dispatch-to [fetch-fx config]}}))

(rf/reg-event-fx
  ::set-page
  (fn [{:keys [db]} [_ page {:keys [state-path]}]]
    (let [state (get-in db state-path)]
      {:dispatch [::set-state state-path (assoc state :page page)]})))

(rf/reg-event-fx
  ::sort
  (fn [{:keys [db]} [_ {:keys [state-path]} column]]
    (let [{:keys [sort-direction sort-column] :as state} (get-in db state-path)
          new-direction (if (not= sort-column column)
                          :asc
                          (if (= sort-direction :asc) :desc :asc))]
      {:dispatch [::set-state state-path (merge state {:sort-direction new-direction
                                                       :sort-column column
                                                       :page 0})]})))

(def pagination-width 1)

(def pagination-total-width (+ 5 (* pagination-width 2)))

(defn- right-placeholder [pages current]
  (concat (take (- pagination-total-width 2) pages)
          [::placeholder]
          [(last pages)]))

(defn- left-placeholder [pages current]
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
        (< current 4) (right-placeholder pages current)
        (> current (- total 5)) (left-placeholder pages current)
        :default (both-placeholders pages current)))))

(defn table [{:keys [footer state-path data-path columns init-state] :as config}]
  (rf/dispatch [::set-state.listener config])
  (rf/dispatch [::set-state state-path init-state])
  (fn []
    (let [{:keys [total items-per-page page sort-direction sort-column]} @(rf/subscribe [::events/db state-path])]
      [base/table {:celled true :sortable true}
       [base/table-header
        [base/table-row
         (for [{:keys [id label]} columns]
           [base/table-header-cell
            {:sorted (if (= sort-column id)
                       (if (= sort-direction :asc)
                         "ascending"
                         "descending"))
             :on-click #(rf/dispatch [::sort config id])}
            label])]]
       [base/table-body
        (for [row @(rf/subscribe [::events/db data-path])]
          [base/table-row
           (for [{:keys [id component]} columns]
             [base/table-cell (if component
                                [component row]
                                (id row))])])]
       [base/table-footer
        [base/table-row
         [base/table-header-cell {:col-span (count columns)}
          (when footer
            [footer])
          (let [total-pages (Math/ceil (/ total items-per-page))]
            [base/menu {:floated "right" :pagination true}
             [base/menu-item {:icon true
                              :disabled (= page 0)
                              :on-click #(rf/dispatch [::set-page (dec page) config])}
              [base/icon {:name "left chevron"}]]
             (for [n (get-page-numbers total-pages page)]
               (if (= ::placeholder n)
                 [base/menu-item "..."]
                 [base/menu-item
                  {:active (= n page)
                   :on-click #(rf/dispatch [::set-page n config])}
                  (str (inc n))]))
             [base/menu-item {:icon true
                              :disabled (= page (dec total-pages))
                              :on-click #(rf/dispatch [::set-page (inc page) config])}
              [base/icon {:name "right chevron"}]]])]]]])))