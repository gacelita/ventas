(ns ventas.themes.mariscosriasbajas.pages.cart
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [fqcss.core :refer [wrap-reagent]]
            [re-frame-datatable.core :as dt]
            [ventas.utils.logging :refer [trace debug info warn error]]
            [ventas.actions.products :as actions.products]
            [ventas.page :refer [pages]]
            [ventas.routes :refer [route-parents routes]]
            [ventas.components.notificator :as ventas.notificator]
            [ventas.components.popup :as ventas.popup]
            [ventas.components.category-list :refer [category-list]]
            [ventas.components.product-list :refer [products-list]]
            [ventas.components.cart :as ventas.cart]
            [ventas.themes.mariscosriasbajas.components.header :refer [header]]
            [ventas.themes.mariscosriasbajas.components.skeleton :refer [skeleton]]
            [ventas.themes.mariscosriasbajas.components.preheader :refer [preheader]]
            [ventas.themes.mariscosriasbajas.components.heading :as theme.heading]
            [ventas.util :as util]
            [ventas.plugin :as plugin]
            [soda-ash.core :as sa]
            [ventas.routes :as routes]))

(def products [{:id 17592186046432
            :quantity 2}
           {:id 17592186046428
            :quantity 3}
           {:id 17592186046424
            :quantity 1}
           {:id 17592186046160
            :quantity 5}])

(rf/reg-sub
 :app/products
 (fn [db [_ id]]
   (-> db :app/products (get id))))

;; Cada componente usa qualified keywords, y de ese modo guarda
;; su estado en la db.
;; Las actions de cada componente siguen el mismo mecanismo.

;; Las actions que no están ligadas a ningún componente, por ejemplo
;; una acción para la API :products/get, deberían tener el nombre correspondiente
;; al ns en el que están. Por ejemplo, se podría hacer un ns ventas.actions.products, y entonces
;; llamarlas :ventas.actions.products/get, etc

;; Las suscripciones que no estén ligadas a ningún componente seguirán un mecanismo similar

(defn line [data]
  (let [product @(rf/subscribe [:products (:id data)])]
    (if-not product
      (rf/dispatch [:products/get (:id data)])
      [sa/TableRow
       [sa/TableCell
        [:a {:href (routes/path-for :frontend.product :id (:id product))} (:name product)]]
       [sa/TableCell (:description product)]
       [sa/TableCell (:price product)]
       [sa/TableCell (:quantity data)]
       [sa/TableCell (* (:quantity data) (:price product))]])))

(defmethod pages :frontend.cart []
  [skeleton
   (wrap-reagent
    [sa/Container
     [:div {:fqcss [::page]}
      [:h2 "Carrito"]
      [sa/Table {:celled true :striped true}
       [sa/TableHeader
        [sa/TableRow
         [sa/TableHeaderCell "Producto"]
         [sa/TableHeaderCell "Descripción"]
         [sa/TableHeaderCell "Precio"]
         [sa/TableHeaderCell "Cantidad"]
         [sa/TableHeaderCell "Total"]]]
       [sa/TableBody
        (map-indexed (fn [idx data]
                       [line data])
                     products)]]]])])