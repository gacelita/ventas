(ns ventas.server.admin-spa
  (:require
   [ventas.html :as html]
   [compojure.core :refer [GET POST defroutes] :as compojure]
   [hiccup.core :as hiccup]
   [ventas.database.entity :as entity]))

(entity/register-type!
 :admin-spa
 {:fixtures
  (fn []
    [{:schema/type :schema.type/image-size
      :image-size/keyword :admin-products-edit
      :image-size/width 150
      :image-size/height 150
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}

     {:schema/type :schema.type/image-size
      :image-size/keyword :admin-orders-edit-line
      :image-size/width 80
      :image-size/height 80
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}])})


(defn- get-html [req]
  (str "<!DOCTYPE html>\n"
       (hiccup/html
        [:html
         [:head
          [:base {:href "/"}]
          [:meta {:charset "UTF-8"}]
          [:title "ventas administration"]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:link {:href "https://cdn.jsdelivr.net/npm/semantic-ui@2.2.14/dist/semantic.min.css"
                  :rel "stylesheet"
                  :type "text/css"}]
          (html/html-resources req)]
         [:body
          [:div#app]
          [:script "ventas.core.start();"]]])))

(defn handle [req]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (get-html req)})

(defn css-middleware [handler]
  (fn [req]
    (handler
     (-> req
         (html/enqueue-css ::normalize "https://cdnjs.cloudflare.com/ajax/libs/normalize/8.0.1/normalize.min.css")
         (html/enqueue-css ::base "/files/css/main.css")))))

(defn js-middleware [handler]
  (fn [req]
    (handler
     (-> req
         (html/enqueue-js ::base "/files/js/admin/main.js")))))

(def handler
  (-> (compojure/routes
       (GET "/admin*" _ handle))
      (css-middleware)
      (js-middleware)))

