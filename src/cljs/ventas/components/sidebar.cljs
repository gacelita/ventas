(ns ventas.components.sidebar
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]))

(def state-key ::state)

(rf/reg-event-db
 ::toggle-filter
 (fn [db [_ id]]
   (update-in db [state-key id :closed] not)))

(defn link [attrs label]
  [:a.sidebar-link attrs label])

(defn sidebar-section [{:keys [name id]} & args]
  (let [id (if-not id (str (gensym)) id)]
    (fn [{:keys [name id]} & args]
      (let [{:keys [closed]} @(rf/subscribe [:db [state-key id]])]
        [:div.sidebar-section {:class (str "sidebar-section--" (if closed "closed" "open"))}
         [:div.sidebar-section__header
          {:on-click #(rf/dispatch [::toggle-filter id])}
          [:h2 name]
          [base/icon {:name (str "chevron " (if closed "down" "up"))}]]
         [:div.sidebar-section__content
          args]]))))

(defn sidebar [& children]
  [:div.sidebar
   (map-indexed
    (fn [idx child]
      (with-meta child {:key idx}))
    children)])
