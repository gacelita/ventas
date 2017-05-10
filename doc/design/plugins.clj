;;
;; WP
;;

;; actions
;; Core llama a Plugin en determinados momentos para que haga cosas

;; filters
;; Core llama a Plugin para que modifique algún valor

;; shortcodes
;; Código de usuario o Plantilla llama a Plugin para mostrar algo


;;
;; Ventas
;;

;; Shortcodes
(comment
 (defmethod ventas.plugin/widgets ::link
   (fn [:a "Enlace"])))

;; Actions
(comment
 (defmethod ventas.plugin/actions :ventas.entities.cart/checkout
   (fn []
     "Something")))

;; Filters
(comment
 (defmethod ventas.plugin/filters :ventas.routes/compile
   (fn [routes context]
     routes)))