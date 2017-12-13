(ns ventas.components.product-list
  (:require
   [ventas.utils :as utils]
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.utils.formatting :as formatting]))

(defn products-list [products]
  [:div.product-list
   (for [{:keys [id images price name]} products]
     [:a.product-list__product {:key id
                                :href (routes/path-for :frontend.product :id id)}
      (when (seq images)
        [:img {:src (str "/images/" (:id (first images)) "/resize/product-listing")}])
      [:div.product-list__content
       [:span.product-list__name
        name]
       [:div.product-list__price
        [:span (str (formatting/format-number (:value price))
                    " "
                    (get-in price [:currency :symbol]))]]]])])