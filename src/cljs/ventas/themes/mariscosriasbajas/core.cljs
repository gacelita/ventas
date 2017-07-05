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
   :url ["category/" :id]}])