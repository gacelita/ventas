(ns ventas.components.category-list
  (:require
   [re-frame.core :as rf]
   [ventas.routes :as routes]))

(def categories-key ::categories)

(rf/reg-event-fx
 ::init
  (fn [cofx [_]]
    {:dispatch [:api/categories.list
                {:success-fn #(rf/dispatch [:ventas/db [categories-key] %])}]}))

(defn category-list []
  (rf/dispatch [::init])
  (fn []
    [:div.category-list
     (let [categories @(rf/subscribe [:ventas/db [categories-key]])]
       (for [{:keys [id name description image]} categories]
         [:div.category-list__category {:key id
                                        :on-click #(routes/go-to :frontend.category :id id)}
          (when image
            [:img.category-list__image {:src (:url image)}])
          [:div.category-list__content
           [:h3 name]
           (when description
             [:p description])]]))]))