(ns ventas.plugins.blog.core
  (:require
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.utils.ui :as utils.ui]
   [ventas.plugins.blog.api :as blog.api]
   [ventas.events :as events]))

(defn page []
  (let [sub-key :blog.core]
    (rf/dispatch [::blog.api/posts.list
                  {:success #(rf/dispatch [::events/db [sub-key] %])}])
    (fn []
      [:div "My page"
       [:ul
        (for [item @(rf/subscribe [::events/db sub-key])]
          [:li (:name item)])]])))

(routes/define-route!
  :blog
  {:name "Blog"
   :url "blog"
   :component page})
