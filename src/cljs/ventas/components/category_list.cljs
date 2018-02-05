(ns ventas.components.category-list
  (:require
   [re-frame.core :as rf]
   [ventas.routes :as routes]))

(defn category-list [categories]
  [:div.category-list
   (for [{:keys [id keyword name image]} categories]
     [:div.category-list__category {:key id
                                    :on-click #(routes/go-to :frontend.category :id keyword)}
      (if image
        [:img.category-list__image {:src (str "/images/" (:id image) "/resize/category-listing")}]
        [:div.category-list__image-placeholder])
      [:div.category-list__content
       [:h3.category-list__name name]]])])
