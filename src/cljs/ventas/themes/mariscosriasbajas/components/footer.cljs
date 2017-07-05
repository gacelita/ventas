(ns ventas.themes.mariscosriasbajas.components.footer
  (:require [fqcss.core :refer [wrap-reagent]]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [ventas.routes :as routes]
            [ventas.util :as util]))

(defn footer []
  (reagent/with-let [sub-logo (util/sub-resource-url :logo)
                     sub-title (util/sub-configuration :site-title)
                     sub-footer-instagram (util/sub-resource-url :footer-instagram)]
    (wrap-reagent
      [:div.ventas {:fqcss [::footer]}
        [:div.ui.container
         [:div {:fqcss [::columns]}
          [:div {:fqcss [::column]}
           [:p "Compra mariscos y pescados subastados diariamente en las lonjas gallegas."]]
          [:div {:fqcss [::column]}
           [:h4 "Enlaces"]
           [:ul
            [:li
             [:a {:href (routes/path-for :frontend.legal-notice)} "Aviso legal"]
             [:a {:href (routes/path-for :frontend.privacy-policy)} "Política de privacidad"]
             [:a {:href (routes/path-for :frontend.cookie-usage)} "Uso de cookies"]
             [:a {:href (routes/path-for :frontend.faq)} "Preguntas frecuentes"]
             [:a {:href (routes/path-for :frontend.shipping-fees)} "Precios portes"]]]]
          [:div {:fqcss [::column]}
           [:h4 "Contactar"]
           [:p "C/ Isaac Peral, 10, Local 8"]
           [:p "36201 Vigo (Pontevedra)"]
           [:p "NRS: 9913.1961/PO"]
           [:br]
           [:p "986 192 238"]
           [:p "660 006 260"]
           [:p "639 882 149"]
           [:p "Email:"]
           [:a {:href "mailto:clientes@mariscoriasbajas.com"} "clientes@mariscoriasbajas.com"]]]
         [:div {:fqcss [::instagram]}
          [:img {:src sub-footer-instagram}]]]])))