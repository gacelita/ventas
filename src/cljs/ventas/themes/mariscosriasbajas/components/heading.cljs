(ns ventas.themes.mariscosriasbajas.components.heading)

(defn heading [text]
  [:div.heading
   [:div.heading__line]
   [:div.heading__text
    [:h3 [:strong text]]]])