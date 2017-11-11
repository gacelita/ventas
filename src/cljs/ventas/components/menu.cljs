(ns ventas.components.menu)

(defn menu-item [data]
  [:li
   [:a {:href (:href data)} (:text data)]])

(defn menu [items]
  [:div.menu
   [:ul
    (map-indexed
     (fn [item idx] ^{:key idx} [menu-item item])
     items)]])