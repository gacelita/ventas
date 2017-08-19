(ns ventas.themes.mariscosriasbajas.core
  (:require [ventas.themes.mariscosriasbajas.pages.frontend]
            [ventas.themes.mariscosriasbajas.pages.category]
            [ventas.themes.mariscosriasbajas.pages.product]
            [ventas.themes.mariscosriasbajas.pages.privacy-policy]
            [ventas.themes.mariscosriasbajas.pages.login]
            [ventas.themes.mariscosriasbajas.pages.cart]
            [ventas.routes :as routes]))

(routes/define-routes!
 [{:route :frontend
   :name "Inicio"
   :url ""}

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

  {:route :frontend.cookie-usage.test
   :name "Test"
   :url ["test"]}

  {:route :frontend.faq
   :name "Preguntas frecuentes"
   :url ["faq"]}

  {:route :frontend.shipping-fees
   :name "Precios portes"
   :url ["shipping-fees"]}

  {:route :frontend.login
   :name "Iniciar sesión"
   :url ["login"]}

  {:route :frontend.cart
   :name "Carrito"
   :url ["cart"]}])