(ns ventas.components.category-list
  (:require
   [re-frame.core :as rf]
   [ventas.routes :as routes]))

(defn category-list [categories]
  [:div.category-list
   (for [{:keys [id keyword name image]} categories]
     [:div.category-list__category {:key id
                                    :on-click #(routes/go-to :frontend.category :keyword keyword)}
      (when image
        [:img.category-list__image {:src (:url image)}])
      [:div.category-list__content
       [:h3.category-list__name name]]])])