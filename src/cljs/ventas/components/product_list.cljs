(ns ventas.components.product-list
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.image :as image]
   [ventas.events :as events]
   [ventas.routes :as routes]
   [ventas.utils :as utils]
   [ventas.utils.ui :as utils.ui]
   [ventas.utils.formatting :as formatting]))

(defn products-list [products]
  [:div.product-list
   [base/grid
    (doall
     (for [{:keys [id images price name]} products]
       [base/grid-column {:key id
                          :mobile 8
                          :tablet 4
                          :computer 4}
        [:div.product-list__product
         [:div.product-list__images-wrapper
          (when (seq images)
            [image/image (:id (first images)) :product-listing])
          [:div.product-list__actions
           [base/icon {:name (if @(rf/subscribe [::events/users.favorites.favorited? id])
                               "heart"
                               "empty heart")
                       :on-click (utils.ui/with-handler
                                  #(rf/dispatch [::events/users.favorites.toggle id]))}]
           [base/icon {:name "shopping bag"}]]]
         [:a.product-list__content {:href (routes/path-for :frontend.product :id id)}
          [:span.product-list__name
           name]
          [:div.product-list__price
           [:span (str (formatting/format-number (:value price))
                       " "
                       (get-in price [:currency :symbol]))]]]]]))]])