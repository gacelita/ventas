(ns ventas.core
  (:gen-class)
  (:require
   [clojure.core.async :refer [go >!]]
   [mount.core :as mount]
   [ventas.events :as events]
   [ventas.database]
   [ventas.server]
   [ventas.server.api]
   [ventas.entities.address]
   [ventas.entities.amount]
   [ventas.entities.brand]
   [ventas.entities.category]
   [ventas.entities.configuration]
   [ventas.entities.country]
   [ventas.entities.currency]
   [ventas.entities.discount]
   [ventas.entities.file]
   [ventas.entities.i18n]
   [ventas.entities.image-size]
   [ventas.entities.order]
   [ventas.entities.product]
   [ventas.entities.product-taxonomy]
   [ventas.entities.product-term]
   [ventas.entities.shipping-method]
   [ventas.entities.state]
   [ventas.entities.tax]
   [ventas.entities.user]
   [ventas.plugins.blog.core]
   [ventas.plugins.featured-categories.core]
   [ventas.plugins.featured-products.core]
   [ventas.plugins.slider.core]
   [ventas.themes.clothing.core]))

(defn -main [& args]
  (mount/start)
  (go (>! (events/pub :init) true)))

