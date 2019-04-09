(ns ventas.i18n
  "Tongue wrapper.
   For the moment this ns contains the translations, future plan is to move them
   somewhere else"
  (:require
   [tongue.core :as tongue]
   [ventas.common.utils :as common.utils]
   [re-frame.core :as rf]))

(def ^:private base-dicts
  {:es_ES
   {:ventas.components.table/no-rows "Ningún elemento que mostrar"
    :ventas.components.error/no-data "No encontrado"
    :ventas.components.form/search "Buscar"
    :ventas.components.crud-table/actions "Acciones"
    :ventas.components.search-box/search "Buscar"
    :ventas.components.search-box/product "Producto"
    :ventas.components.search-box/category "Categoría"
    :ventas.components.search-box/page "Página"

    :ventas.components.product-filters/category "Categoría"

    :ventas.components.notificator/saved "¡Guardado!"
    :ventas.components.notificator/error "Ocurrió un error."

    :ventas.events/session-started "¡Bienvenido!"

    :ventas.page/not-found "404"
    :ventas.page/not-implemented "No implementada"
    :ventas.page/this-page-has-not-been-implemented #(str "Esta página (" % ") no ha sido implementada")

    :ventas.session/unregistered-error "Tienes que ser un usuario registrado para hacer eso"

    :ventas.utils.formatting/percentage "%"
    :ventas.utils.formatting/amount ""

    :ventas.utils.validation/length-error (fn [{:keys [min max]}]
                                            (cond
                                              (and min max) (str "La longitud debería ser mayor que " min " y menor que " max)
                                              min (str "La longitud debería ser mayor que " min)
                                              max (str "La longitud debería ser menor que " max)))
    :ventas.utils.validation/email-error "Dirección de email inválida"
    :ventas.utils.validation/required-error "Este campo es requerido"

    :discount.amount.kind/amount "Importe"
    :discount.amount.kind/percentage "Porcentaje"

    :image-size.algorithm/always-resize "Siempre redimensionar"
    :image-size.algorithm/crop-and-resize "Recortar y redimensionar"
    :image-size.algorithm/resize-only-if-over-maximum "Redimensionar solo si supera el tamaño máximo"

    :user.role/administrator "Administrador"
    :user.role/user "Usuario"

    :user.status/inactive "Inactivo"
    :user.status/pending "Pendiente"
    :user.status/active "Activo"
    :user.status/cancelled "Cancelado"
    :user.status/unregistered "No registrado"

    :order.status/draft "Borrador"
    :order.status/acknowledged "Aceptado"
    :order.status/paid "Pagado"
    :order.status/ready "Listo"
    :order.status/shipped "Enviado"
    :order.status/unpaid "No pagado"
    :order.status/rejected "Rechazado"
    :order.status/cancelled "Cancelado"

    :entity.create "Creación"
    :entity.delete "Eliminación"
    :entity.update "Actualización"

    :schema.type/category "Categoría"
    :schema.type/user "Usuario"
    :schema.type/product "Producto"
    :schema.type/brand "Marca"

    :shipping-method.pricing/price "Precio"
    :shipping-method.pricing/weight "Peso"

    :tax.kind/amount "Importe"
    :tax.kind/percentage "Porcentaje"}
   :en_US
   {:ventas.components.base/loading "Loading"

    :ventas.components.cart/product-added "Product added!"

    :ventas.components.error/no-data "Nothing found!"

    :ventas.components.form/search "Search"

    :ventas.components.table/no-rows "No items to show"

    :ventas.components.crud-table/actions "Actions"

    :ventas.components.search-box/search "Search"
    :ventas.components.search-box/product "Product"
    :ventas.components.search-box/category "Category"
    :ventas.components.search-box/page "Page"

    :ventas.components.product-filters/category "Category"

    :ventas.components.notificator/saved "Saved!"
    :ventas.components.notificator/error "An error occurred."

    :ventas.events/session-started "Welcome!"

    :ventas.page/not-found "404"
    :ventas.page/not-implemented "Not implemented"
    :ventas.page/this-page-has-not-been-implemented #(str "This page (" % ") has not been implemented")

    :ventas.session/unregistered-error "You need to be a registered user to do that"

    :ventas.utils.formatting/percentage "%"
    :ventas.utils.formatting/amount ""

    :ventas.utils.validation/length-error (fn [{:keys [min max]}]
                                            (cond
                                              (and min max) (str "Length should be higher than " min " and lower than " max)
                                              min (str "Length should be higher than " min)
                                              max (str "Length should be lower than " max)))
    :ventas.utils.validation/email-error "Invalid email address"
    :ventas.utils.validation/required-error "This field is required"

    :discount.amount.kind/amount "Amount"
    :discount.amount.kind/percentage "Percentage"

    :image-size.algorithm/always-resize "Always resize"
    :image-size.algorithm/crop-and-resize "Crop and resize"
    :image-size.algorithm/resize-only-if-over-maximum "Resize only if over maximum"

    :user.role/administrator "Administrator"
    :user.role/user "User"

    :user.status/inactive "Inactive"
    :user.status/pending "Pending"
    :user.status/active "Active"
    :user.status/cancelled "Cancelled"
    :user.status/unregistered "Unregistered"

    :order.status/draft "Draft"
    :order.status/acknowledged "Acknowledged"
    :order.status/paid "Paid"
    :order.status/ready "Ready"
    :order.status/shipped "Shipped"
    :order.status/unpaid "Unpaid"
    :order.status/rejected "Rejected"
    :order.status/cancelled "Cancelled"

    :entity.create "Creation"
    :entity.delete "Deletion"
    :entity.update "Update"

    :schema.type/category "Category"
    :schema.type/user "User"
    :schema.type/product "Product"
    :schema.type/brand "Brand"

    :shipping-method.pricing/price "Price"
    :shipping-method.pricing/weight "Weight"

    :tax.kind/amount "Amount"
    :tax.kind/percentage "Percentage"}

   :tongue/fallback :en_US})

(def dicts (atom base-dicts))

(defn- build-translation-fn* []
  (tongue/build-translate @dicts))

(def ^:private translation-fn (atom (build-translation-fn*)))

(defn- build-translation-fn! []
  (reset! translation-fn (build-translation-fn*)))

(defn register-translations! [m]
  (swap! dicts common.utils/deep-merge m)
  (build-translation-fn!))

(defn i18n [kw & args]
  (let [culture-kw @(rf/subscribe [:ventas.events/culture-kw])]
    (apply @translation-fn (or culture-kw :en_US) kw args)))
