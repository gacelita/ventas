(ns ventas.components.popover
  "Use this when you want a component to appear when the user clicks
   somewhere, and you want it to disappear when the user clicks anywhere
   in the window (but outside of the component)"
  (:require
   [ventas.utils :as utils]
   [reagent.core :as reagent]
   [re-frame.core :as rf]))

(def state-key ::state)

(defn set-state [db target-id new-state]
  (assoc-in db [state-key :active-id] (when new-state target-id)))

(rf/reg-event-db
 ::toggle
 (fn [db [_ id]]
   (let [{:keys [active-id]} (get db state-key)]
     (set-state db id (not (= active-id id))))))

(rf/reg-event-db
 ::show
 (fn [db [_ id]]
   (set-state db id true)))

(rf/reg-event-db
 ::hide
 (fn [db [_ id]]
   (set-state db id false)))

(rf/reg-sub
 ::active?
 (fn [db [_ id]]
   (= (get-in db [state-key :active-id]) id)))

(defn popover [id _]
  {:pre [id]}
  (let [node (atom nil)
        click-listener (fn [e]
                         (when-not (utils/child? (.-target e) @node)
                           (rf/dispatch [::hide id])))]
    (reagent/create-class
     {:component-will-unmount
      (fn [_]
        (.removeEventListener js/window "click" click-listener))
      :component-did-mount
      (fn [this]
        (reset! node (reagent/dom-node this))
        (.addEventListener js/window "click" click-listener))
      :reagent-render
      (fn [_ content]
        (when @(rf/subscribe [::active? id])
          content))})))
