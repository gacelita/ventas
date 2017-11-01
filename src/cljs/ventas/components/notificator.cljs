(ns ventas.components.notificator
  (:require
   [ventas.utils :as util]
   [soda-ash.core :as sa]
   [clojure.string :as s]
   [fqcss.core :refer [wrap-reagent]]
   [cljs.core.async :refer [<! >! put! close! timeout chan]]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(rf/reg-event-db ::add
 (fn [db [_ notification]]
   (let [sym (gensym)
         notification (assoc notification :sym sym)]
     (go
      (<! (timeout 4000))
      (rf/dispatch [::remove sym]))
     (if (seq (:notifications db))
       (update db :notifications #(conj notification))
       (assoc db :notifications [notification])))))

(rf/reg-event-db ::remove
  (fn [db [_ sym]]
    (update db :notifications #(remove (fn [item] (= (:sym item) sym)) %))))

(defn notificator
  "Displays notifications"
  []
  [:div.notificator
   (let [notifications @(rf/subscribe [:ventas/db :notifications])]
     (for [{:keys [theme message icon sym]} notifications]
       [:div.notificator__item {:key (gensym) :class theme}
        [base/icon {:class "bu close"
                    :name icon
                    :on-click #(rf/dispatch [::remove sym])}]
        [:p.bu.message message]]))])