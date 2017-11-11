(ns ventas.themes.clothing.core
  (:require [ventas.themes.clothing.pages.frontend]
            [ventas.themes.clothing.pages.category]
            [ventas.themes.clothing.pages.product]
            [ventas.themes.clothing.pages.privacy-policy]
            [ventas.themes.clothing.pages.login]
            [ventas.themes.clothing.pages.cart]
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