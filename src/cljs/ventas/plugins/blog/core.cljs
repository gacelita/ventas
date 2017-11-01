(ns ventas.plugins.blog.core
  (:require
   [ventas.plugin :as plugin]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.utils.ui :as utils.ui]
   [ventas.plugins.blog.api :as blog.api]))

(defn page []
  (let [sub-key :blog.core]
    (rf/dispatch [::blog.api/posts.list
                  {:success-fn #(rf/dispatch [:ventas/db [sub-key] %])}])
    (fn []
      [:div "My page"
       [:ul
        (for [item @(rf/subscribe [:ventas/db sub-key])]
          [:li (:name item)])]])))

(routes/define-route!
 :blog
 {:name "Blog"
  :url "blog"
  :component page})
