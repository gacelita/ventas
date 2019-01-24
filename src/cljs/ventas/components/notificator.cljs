(ns ventas.components.notificator
  (:require
   [cljs.core.async :refer [<! timeout go]]
   [cljs.pprint :as pprint]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.seo :as seo]))

(seo/add-prerendering-hook
 ::hook
 #(assoc % :notifications []))

(rf/reg-event-db
 ::add
 (fn [db [_ notification]]
   (let [id (hash notification)
         notification (assoc notification :id id)]
     (go
      (<! (timeout (or (:timeout notification) 4000)))
      (rf/dispatch [::remove id]))
     (if (seq (filter (fn [n] (= (:id n) id))
                      (:notifications db)))
       db
       (if (seq (:notifications db))
         (update db :notifications #(conj % notification))
         (assoc db :notifications [notification]))))))

(rf/reg-event-db
 ::remove
 (fn [db [_ id]]
   (update db :notifications #(remove (fn [item] (= (:id item) id)) %))))

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
   (let [notifications @(rf/subscribe [:db [:notifications]])]
     (for [{:keys [theme message icon id component]} notifications]
       [:div.notificator__item {:key id
                                :class theme
                                :on-click #(rf/dispatch [::remove id])}
        (if component
          component
          [:div
           [base/icon {:class "close"
                       :name icon}]
           [:p.message
            (if (string? message)
              message
              (with-out-str (pprint/pprint message)))]])]))])
