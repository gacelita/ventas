(ns ventas.components.notificator
  (:require
   [cljs.core.async :refer [<! timeout]]
   [re-frame.core :as rf]
   [ventas.components.base :as base])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

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