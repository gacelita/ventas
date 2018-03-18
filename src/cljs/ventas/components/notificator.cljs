(ns ventas.components.notificator
  (:require
   [cljs.core.async :refer [<! timeout]]
   [ventas.i18n :refer [i18n]]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.seo :as seo]
   [cljs.pprint :as pprint])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(seo/add-prerendering-hook
 ::hook
 #(assoc % :notifications []))

(rf/reg-event-db
 ::add
 (fn [db [_ notification]]
   (let [sym (gensym)
         notification (assoc notification :sym sym)]
     (go
      (<! (timeout (or (:timeout notification) 4000)))
      (rf/dispatch [::remove sym]))
     (if (seq (:notifications db))
       (update db :notifications #(conj % notification))
       (assoc db :notifications [notification])))))

(rf/reg-event-db
 ::remove
 (fn [db [_ sym]]
   (update db :notifications #(remove (fn [item] (= (:sym item) sym)) %))))

(rf/reg-event-fx
 ::notify-saved
 (fn [_ [_ message]]
   {:dispatch [::add {:message (str (i18n ::saved)
                                    (when message (str "\n" message)))
                      :theme "success"}]}))

(rf/reg-event-fx
 ::notify-error
 (fn [_ [_ message]]
   {:dispatch [::add {:message (str (i18n ::error)
                                    (when message (str "\n" message)))
                      :theme "error"}]}))

(defn notificator
  "Displays notifications"
  []
  [:div.notificator
   (let [notifications @(rf/subscribe [::events/db [:notifications]])]
     (for [{:keys [theme message icon sym component]} notifications]
       [:div.notificator__item {:key (gensym) :class theme}
        (if component
          component
          [:div
           [base/icon {:class "bu close"
                       :name icon
                       :on-click #(rf/dispatch [::remove sym])}]
           [:p.message
            (if (string? message)
              message
              (with-out-str (pprint/pprint message)))]])]))])
