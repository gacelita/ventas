(ns ventas.themes.clothing.components.menu
  (:require
   [ventas.components.menu :as menu]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.events :as events]
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]
   [clojure.string :as str]))

(def state-key ::state)

(defn- category->menu-item [[{:keys [slug name]} children]]
  {:text name
   :id slug
   :href (routes/path-for :frontend.category :id slug)
   :children (when children (map category->menu-item children))})

(defn get-categories []
  (->> @(rf/subscribe [::events/db :categories])
       (common.utils/tree-by :id :parent)
       (map category->menu-item)
       (into [{:text (i18n ::home)
               :href (routes/path-for :frontend)}])))

(defn menu []
  (let [current (-> @(rf/subscribe [::events/db [state-key :current-category]])
                    (str/split "-"))]
    [menu/menu
     {:current-fn (fn [{:keys [id]}]
                    (when (= :frontend.category (routes/handler))
                      (let [split (str/split id "-")]
                        (= current (try (subvec split 0 (count current))
                                        (catch :default e))))))
      :items (get-categories)}]))
