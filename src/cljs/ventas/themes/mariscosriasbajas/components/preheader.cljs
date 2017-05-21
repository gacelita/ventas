(ns ventas.themes.mariscosriasbajas.components.preheader)

(defn preheader []
  [:div.ventas.preheader
    [:div.ui.container
      [:div.ventas.preheader__item
        [:strong "Att. cliente y pedidos: "]
        [:a "666 555 444"]
        [:div.ventas.preheader__separator "|"]
        [:a "666 555 444"]
        [:div.ventas.preheader__separator "|"]
        [:a "444 333 222"]]
      [:div.ventas.preheader__separator "-"]
      [:div.ventas.preheader__item
        [:strong "Horario:"]
        [:span "De Lunes a Viernes 09:00 - 13:30 / 16:00 - 19:30"]]]])