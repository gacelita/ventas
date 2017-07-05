(ns ventas.themes.mariscosriasbajas.core
  (:require [ventas.themes.mariscosriasbajas.pages.frontend]
            [ventas.themes.mariscosriasbajas.pages.category]
            [ventas.themes.mariscosriasbajas.pages.product]
            [ventas.routes :as routes]))

(routes/define-routes!
 [{:route :frontend
   :name "Inicio"
   :url ""}

  {:route :frontend.index
   :name "Índice"
   :url "/index"}

  {:route :frontend.product
   :name "Producto"
   :url ["product/" :id]}

  {:route :frontend.category
   :name "Categoría"
   :url ["category/" :id]}

  {:route :frontend.legal-notice
   :name "Aviso legal"
   :url ["legal-notice"]}

  {:route :frontend.privacy-policy
   :name "Política de privacidad"
   :url ["privacy-policy"]}

  {:route :frontend.cookie-usage
   :name "Uso de cookies"
   :url ["cookie-usage"]}

  {:route :frontend.faq
   :name "Preguntas frecuentes"
   :url ["faq"]}

  {:route :frontend.shipping-fees
   :name "Precios portes"
   :url ["shipping-fees"]}])