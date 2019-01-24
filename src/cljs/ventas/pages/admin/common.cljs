(ns ventas.pages.admin.common
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf]
   [ventas.components.form :as form]
   [ventas.events :as events]
   [ventas.events.backend :as backend]))

(def state-key ::state)

(rf/reg-event-fx
 ::search
 (fn [_ [_ key attrs search]]
   {:dispatch [::backend/admin.search
               {:params {:search search
                         :attrs attrs}
                :success [:db [state-key :search-results key]]}]}))

(defn entity->option [entity]
  (-> entity
      (select-keys #{:id :name})
      (set/rename-keys {:id :value
                        :name :text})))

(defn entity-search-field [{:keys [db-path label key attrs selected-option]}]
  [form/field {:db-path db-path
               :label label
               :key key
               :type :entity
               :on-search-change #(rf/dispatch [::search key attrs (-> % .-target .-value)])
               :options (->> @(rf/subscribe [:db [state-key :search-results key]])
                             (map entity->option)
                             (into (if selected-option
                                     [(entity->option selected-option)]
                                     [])))}])
