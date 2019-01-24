(ns ventas.plugins.menu.core
  (:require
   [re-frame.core :as rf]
   [ventas.components.menu :as menu]
   [ventas.plugins.menu.api :as api]
   [ventas.routes :as routes]))

(def state-key ::state)

(rf/reg-event-fx
 ::init
 (fn [_ [_ state-id menu-id]]
   {:dispatch [::api/menus.get {:params {:id menu-id}
                                :success [:db [state-key state-id]]}]}))

(defn- ->menu-item [{:keys [name link children]}]
  {:text name
   :href (if (vector? link)
           (apply routes/path-for link)
           link)
   :target (when-not (vector? link)
             "_blank")
   :children (map ->menu-item children)})

(rf/reg-sub
 ::items
 (fn [db [_ state-id]]
   (let [items (get-in db [state-key state-id :items])]
     (->> items
          (map ->menu-item)))))

(defn menu [state-id]
  [menu/menu
   {:items @(rf/subscribe [::items state-id])
    ;; @todo Figure this out using the current route
    :current-fn (constantly nil)}])