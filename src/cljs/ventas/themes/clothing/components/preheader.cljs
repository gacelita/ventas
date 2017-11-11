(ns ventas.themes.clothing.components.preheader)

(defn preheader []
  [:div.preheader
   [:div.ui.container
    [:div.preheader__item
     [:strong "Att. cliente y pedidos: "]
     [:a "667 943 180"]]
    [:div.preheader__separator "-"]
    [:div.preheader__item
     [:strong "Horario:"]
     [:span "De Lunes a Viernes 09:00 - 13:30 / 16:00 - 19:30"]]]])