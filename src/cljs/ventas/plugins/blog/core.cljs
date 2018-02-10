(ns ventas.plugins.blog.core
  (:require
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.plugins.blog.api :as blog.api]
   [ventas.routes :as routes]))

(def state-key ::state)

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::blog.api/posts.list
               {:success #(rf/dispatch [::events/db state-key %])}]}))

(defn page []
  [:div "My page"
   [:ul
    (for [item @(rf/subscribe [::events/db state-key])]
      [:li (:name item)])]])

(routes/define-route!
  :blog
  {:name "Blog"
   :url "blog"
   :component page
   :init-fx [::init]})
