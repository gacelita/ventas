(ns ventas.themes.mariscosriasbajas.components.preheader
  (:require [fqcss.core :refer [wrap-reagent]]
            [reagent.core :as reagent]
            [ventas.component :as component :refer-macros [load-scss]]))

(defn preheader []
    (wrap-reagent
      [:div.ventas {:fqcss [::preheader]}
        [:div.ui.container
          [:div.ventas {:fqcss [::preheader-item]}
            [:strong "Att. cliente y pedidos: "]
            [:a "666 555 444"]
            [:div.ventas {:fqcss [::preheader-separator]} "|"]
            [:a "666 555 444"]
            [:div.ventas {:fqcss [::preheader-separator]} "|"]
            [:a "444 333 222"]]
          [:div.ventas {:fqcss [::preheader-separator]} "-"]
          [:div.ventas {:fqcss [::preheader-item]}
            [:strong "Horario:"]
            [:span "De Lunes a Viernes 09:00 - 13:30 / 16:00 - 19:30"]]]]))