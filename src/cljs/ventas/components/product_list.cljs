(ns ventas.components.product-list
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.components.image :as image]
   [ventas.events :as events]
   [ventas.routes :as routes]
   [ventas.utils.formatting :as utils.formatting]
   [ventas.utils.ui :as utils.ui]
   [ventas.utils :as utils]))

(defn favorite-button [id]
  [base/icon {:name (if @(rf/subscribe [::events/users.favorites.favorited? id])
                      "heart"
                      "empty heart")
              :on-click (utils.ui/with-handler
                         #(rf/dispatch [::events/users.favorites.toggle id]))}])

(defn add-to-cart-button [id]
  [base/icon {:name "shopping bag"
              :on-click (utils.ui/with-handler
                         #(rf/dispatch [::cart/add id]))}])

(defmulti catalog (fn [kw _] kw) :default :grid)

(defmethod catalog :grid [_ products]
  [base/grid
   (doall
    (for [{:keys [id images price name slug]} products]
      (do (assert id)
          [base/grid-column {:key id
                             :mobile 8
                             :tablet 4
                             :computer 4}
           [:div.product-grid__product
            [:div.product-grid__images-wrapper
             {:class (when (empty? images) "product-grid__images-wrapper--no-image")}
             (when (seq images)
               [:a {:href (routes/path-for :frontend.product :id slug)}
                [image/image (:id (first images)) :product-listing]])
             [:div.product-grid__actions
              [favorite-button id]
              [add-to-cart-button id]]]
            [:a.product-grid__content {:href (routes/path-for :frontend.product :id slug)}
             [:span.product-grid__name
              name]
             [:div.product-grid__price
              [:span (utils.formatting/amount->str price)]]]]])))])

(defmethod catalog :list [_ products]
  [:div
   (doall
    (for [{:keys [id images price name slug description]} products]
      [base/segment
       [:a.product-list__product
        {:href (routes/path-for :frontend.product :id slug)}
        [:div.product-list__images-wrapper
         {:class (when (empty? images) "product-list__images-wrapper--no-image")}
         (when (seq images)
           [image/image (:id (first images)) :cart-page-line])]
        [:div.product-list__content
         [:h3.product-list__name name]
         [:div.product-list__price
          [:span (utils.formatting/amount->str price)]]
         [:p.product-list__description (utils/cut-string-on-space description 340)]
         [:div.product-list__actions
          [favorite-button id]
          [add-to-cart-button id]]]]]))])

(defn product-list [products]
  [:div.product-list
   (let [{:customization/keys [product-listing-mode]} @(rf/subscribe [::events/db [:configuration]])]
     [catalog product-listing-mode products])])
