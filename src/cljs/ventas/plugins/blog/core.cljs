(ns ventas.plugins.blog.core
  (:require
   [ventas.plugin :as plugin]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.utils.ui :as utils.ui]))

(rf/reg-event-fx
 :plugins.blog/post.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :blog.list} options)}))

(defn page []
  (let [sub-key :blog.core]
    (rf/dispatch [:plugins.blog/post.list
                  {:success-fn #(rf/dispatch [:ventas.api/success [sub-key] %])}])
    (utils.ui/reg-kw-sub sub-key)
    (fn []
      [:div "My page"
       [:ul
        (for [item @(rf/subscribe [sub-key])]
          [:li (:name item)])]])))

(routes/define-route!
 :blog
 {:name "Blog"
  :url "blog"
  :component page})
