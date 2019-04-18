(ns ventas.i18n
  "Server-side tongue wrapper"
  (:require
   [tongue.core :as tongue]
   [ventas.common.utils :as common.utils]))

(def ^:private base-dicts
  {:en_US
   {:ventas.email/new-pending-order "New pending order"
    :ventas.i18n/test-value "Test value"

    :ventas.database.entity/database-not-migrated "The database needs to be migrated before doing this"
    :ventas.database/database-connection-error "Database connection error"
    :ventas.database/transact-exception "Database exception"
    :ventas.entities.configuration/access-denied (fn [{:keys [key]}]
                                                   (str "The current user is not allowed to read the " key " configuration key"))

    :ventas.email.templates.order-status-changed/heading (fn [status ref]
                                                           (case status
                                                             :order.status/unpaid (str "We've received your order #" ref ". We'll notify you when we receive your payment.")
                                                             :order.status/paid (str "We've received your order #" ref ". We'll begin preparing it soon.")
                                                             :order.status/acknowledged (str "We're preparing your order #" ref ". We'll notify you when it's shipped.")
                                                             :order.status/ready (str "Your order #" ref " is ready, and will be updated when we receive your payment.")
                                                             :order.status/shipped (str "Your order #" ref " has been shipped.")
                                                             :order.status/cancelled (str "You've cancelled your order #" ref ".")
                                                             :order.status/rejected (str "Your order #" ref " has been rejected.")))
    :ventas.email.templates.order-status-changed/subject (fn [status ref]
                                                           (str "Order #" ref " - "
                                                                (case status
                                                                  :order.status/unpaid "Unpaid"
                                                                  :order.status/paid "Paid"
                                                                  :order.status/acknowledged "Acknowledged"
                                                                  :order.status/ready "Ready"
                                                                  :order.status/shipped "Shipped"
                                                                  :order.status/cancelled "Cancelled"
                                                                  :order.status/rejected "Rejected")))
    :ventas.email.templates.order-status-changed/product "Product"
    :ventas.email.templates.order-status-changed/quantity "Quantity"
    :ventas.email.templates.order-status-changed/amount "Amount"
    :ventas.email.templates.order-status-changed/total-amount "Total amount"
    :ventas.email.templates.order-status-changed/shipping-address "Shipping address"
    :ventas.email.templates.order-status-changed/no-shipping-address "This order has no shipping address"
    :ventas.email.templates.order-status-changed/go-to-orders "You can see your orders"
    :ventas.email.templates.order-status-changed/go-to-orders-link "here"

    :ventas.email.templates.user-registered/welcome (fn [title] (str "Welcome to " title "!"))
    :ventas.email.templates.user-registered/add-an-address "Now it's a good moment to add an address to your profile, so that it's available for your orders."
    :ventas.email.templates.user-registered/go-to-profile "Click here to go to your profile"

    :ventas.email.templates.password-forgotten/reset-your-password "Reset your password"

    :ventas.email.elements/hello (fn [name]
                                   (str "Hello, " name "!"))
    :ventas.payment-method/payment-method-not-found (fn [{:keys [method]}]
                                                      (str "Payment method not found: " method))
    :ventas.plugin/plugin-not-found (fn [{:keys [keyword]}]
                                      (str "Plugin not found: " keyword))
    :ventas.search/elasticsearch-error "Elasticsearch error"
    :ventas.server.api.admin/unauthorized "Unauthorized: you need to be an administrator to do this"
    :ventas.server.api.user/authentication-required "Authentication required: your identity is invalid or missing"
    :ventas.server.api.user/discount-not-found (fn [{:keys [code]}]
                                                 (str "Discount not found: " code))
    :ventas.server.api.user/entity-update-unauthorized (fn [{:keys [entity-type]}]
                                                         (str "Unauthorized: you need to be the owner of this " entity-type " entity to update it"))
    :ventas.server.api/category-not-found (fn [{:keys [category]}]
                                            (str "Category not found: " category))
    :ventas.server.api/entity-not-found (fn [{:keys [entity]}]
                                          (str "Entity not found: " entity))
    :ventas.server.api/invalid-credentials "Invalid credentials"
    :ventas.server.api/invalid-ref (fn [{:keys [ref]}]
                                     (str "Invalid entity reference: " ref))
    :ventas.server.api/user-not-found "User not found"
    :ventas.server.ws/api-call-not-found (fn [{:keys [name]}]
                                           (str "The requested API call does not exist (" name ")"))
    :ventas.utils.images/file-not-found (fn [{:keys [path]}]
                                          (str "File not found: " path))
    :ventas.utils/spec-invalid "Validation error"

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

   :es_ES
   {:ventas.email/new-pending-order "Nuevo pedido pendiente"
    :ventas.database.entity/database-not-migrated "La base de datos tiene que ser migrada antes de hacer esto"
    :ventas.database/database-connection-error "Error de conexión con la base de datos"
    :ventas.database/transact-exception "Excepción de la base de datos"
    :ventas.entities.configuration/access-denied (fn [{:keys [key]}]
                                                   (str "El usuario actual no tiene acceso a la clave de configuración " key))

    :ventas.email.templates.order-status-changed/heading (fn [status ref]
                                                           (case status
                                                             :order.status/unpaid (str "Hemos recibido tu pedido #" ref ". Te notificaremos cuando recibamos tu pago.")
                                                             :order.status/paid (str "Hemos recibido tu pedido #" ref ". Lo prepararemos lo antes posible.")
                                                             :order.status/acknowledged (str "Estamos preparando tu pedido #" ref ". Te notificaremos cuando lo hayamos enviado.")
                                                             :order.status/ready (str "Tu pedido #" ref " está listo, y será actualizado cuando recibamos tu pago.")
                                                             :order.status/shipped (str "Tu pedido #" ref " ha sido enviado.")
                                                             :order.status/cancelled (str "Has cancelado tu pedido #" ref ".")
                                                             :order.status/rejected (str "Tu pedido #" ref " ha sido rechazado.")))
    :ventas.email.templates.order-status-changed/subject (fn [status ref]
                                                           (str "Pedido #" ref " - "
                                                                (case status
                                                                  :order.status/unpaid "No pagado"
                                                                  :order.status/paid "Pagado"
                                                                  :order.status/acknowledged "Admitido"
                                                                  :order.status/ready "Listo"
                                                                  :order.status/shipped "Enviado"
                                                                  :order.status/cancelled "Cancelado"
                                                                  :order.status/rejected "Rechazado")))
    :ventas.email.templates.order-status-changed/product "Producto"
    :ventas.email.templates.order-status-changed/quantity "Cantidad"
    :ventas.email.templates.order-status-changed/amount "Importe"
    :ventas.email.templates.order-status-changed/total-amount "Importe total"
    :ventas.email.templates.order-status-changed/shipping-address "Dirección de envío"
    :ventas.email.templates.order-status-changed/no-shipping-address "Este pedido no tiene una dirección de envío"
    :ventas.email.templates.order-status-changed/go-to-orders "Puedes ver tus pedidos"
    :ventas.email.templates.order-status-changed/go-to-orders-link "aquí"

    :ventas.email.templates.user-registered/welcome (fn [title] (str "Bienvenido a " title "!"))
    :ventas.email.templates.user-registered/add-an-address "Ahora es un buen momento para añadir una dirección a tu perfil, para usarla cuando realices un pedido."
    :ventas.email.templates.user-registered/go-to-profile "Haz click aquí para ir a tu perfil"

    :ventas.email.templates.password-forgotten/reset-your-password "Restablece tu contraseña"

    :ventas.email.elements/hello (fn [name]
                                   (str "Hola, " name "!"))
    :ventas.payment-method/payment-method-not-found (fn [{:keys [method]}]
                                                      (str "Método de pago no encontrado: " method))
    :ventas.plugin/plugin-not-found (fn [{:keys [keyword]}]
                                      (str "Plugin no encontrado: " keyword))
    :ventas.search/elasticsearch-error "Error de Elasticsearch"
    :ventas.server.api.admin/unauthorized "No autorizado: necesitas ser un administrador para realizar esta operación"
    :ventas.server.api.user/authentication-required "Autenticación requerida: tu identidad es inválida o no existe"
    :ventas.server.api.user/discount-not-found (fn [{:keys [code]}]
                                                 (str "Descuento no encontrado: " code))
    :ventas.server.api.user/entity-update-unauthorized (fn [{:keys [entity-type]}]
                                                         (str "No autorizado: necesitas ser el dieño de esta entidad " entity-type " para actualizarla"))
    :ventas.server.api/category-not-found (fn [{:keys [category]}]
                                            (str "Categoría no encontrada: " category))
    :ventas.server.api/entity-not-found (fn [{:keys [entity]}]
                                          (str "Entidad no encontrada: " entity))
    :ventas.server.api/invalid-credentials "Credenciales inválidas"
    :ventas.server.api/invalid-ref (fn [{:keys [ref]}]
                                     (str "Referencia a entidad inválida: " ref))
    :ventas.server.api/user-not-found "Usuario no encontrado"
    :ventas.server.ws/api-call-not-found (fn [{:keys [name]}]
                                           (str "La llamada de API solicitada no existe (" name ")"))
    :ventas.utils.images/file-not-found (fn [{:keys [path]}]
                                          (str "Archivo no encontrado: " path))
    :ventas.utils/spec-invalid "Error de validación"


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
    :tax.kind/percentage "Porcentaje"}})

(def dicts (atom base-dicts))

(defn- build-translation-fn* []
  (tongue/build-translate @dicts))

(def ^:private translation-fn (atom (build-translation-fn*)))

(defn- build-translation-fn! []
  (reset! translation-fn (build-translation-fn*)))

(defn register-translations! [m]
  (swap! dicts common.utils/deep-merge m)
  (build-translation-fn!))

(defn i18n [& args]
  (apply @translation-fn args))
