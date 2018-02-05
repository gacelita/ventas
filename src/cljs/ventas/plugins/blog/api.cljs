(ns ventas.plugins.blog.api
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::posts.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :ventas.plugins.blog/posts.list} options)}))
