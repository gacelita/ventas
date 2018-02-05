(ns ventas.components.menu)

(defn menu-item [data]
  [:li
   [:a {:href (:href data)} (:text data)]])

(defn menu [items]
  [:div.menu
   [:div.ui.container
    [:ul
     (map-indexed
      (fn [idx item] ^{:key idx} [menu-item item])
      items)]]])
