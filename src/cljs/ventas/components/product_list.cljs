(ns ventas.components.product-list
  (:require
   [ventas.utils :as utils]
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.utils.formatting :as formatting]
   [ventas.components.base :as base]))

(defn products-list [products]
  [:div.product-list
   [base/grid
    (for [{:keys [id images price name]} products]
      [base/grid-column {:key id
                         :mobile 8
                         :tablet 4
                         :computer 4}
       [:a.product-list__product {:href (routes/path-for :frontend.product :id id)}
        (when (seq images)
          [:img {:src (str "/images/" (:id (first images)) "/resize/product-listing")}])
        [:div.product-list__content
         [:span.product-list__name
          name]
         [:div.product-list__price
          [:span (str (formatting/format-number (:value price))
                      " "
                      (get-in price [:currency :symbol]))]]]]])]])