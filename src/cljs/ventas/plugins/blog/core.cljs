(ns ventas.plugins.blog.core
  (:require
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.plugins.blog.api :as blog.api]
   [ventas.routes :as routes]))

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
