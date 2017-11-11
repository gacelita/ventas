(ns ventas.components.product-list
  (:require
   [ventas.utils :as utils]
   [re-frame.core :as rf]
   [ventas.routes :as routes]))

(def products-key ::products)

(rf/reg-event-fx
 ::products
 (fn [cofx [_]]
   {:dispatch [:api/products.list {:success-fn #(rf/dispatch [:ventas/db [products-key] %])}]}))

(defn products-list []
  (rf/dispatch [::products])
  (fn []
    [:div.product-list
     (let [products @(rf/subscribe [:ventas/db [products-key]])]
       (for [{:keys [id images price]} products]
         [:div.product-list__product {:key id}
          (when (seq images)
            [:img {:src (:url (first images))}])
          [:div.product-list__content
           [:a {:href (routes/path-for :frontend.product :id id)}
            name]
           [:div.product-list__price
            [:span (utils/format-price price)]]]]))]))