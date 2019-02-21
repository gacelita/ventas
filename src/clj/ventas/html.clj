(ns ventas.html
  (:require [clojure.string :as str]
            [hiccup.core :as hiccup]))

(defn enqueue-resource [type request name file]
  (assoc-in request [::resources type name] file))

(def enqueue-css (partial enqueue-resource :css))
(def enqueue-js (partial enqueue-resource :js))

(defn- ->script [src]
  [:script {:src src}])

(defmulti resource->html (fn [type _] type))

(defmethod resource->html :js [_ resource]
  (hiccup/html [:script {:src resource}]))

(defmethod resource->html :css [_ resource]
  (hiccup/html [:link {:href resource :rel "stylesheet" :type "text/css"}]))

(defn html-resources [request]
  (->> (::resources request)
       (mapcat (fn [[type resource-map]]
                 (map (partial resource->html type) (vals resource-map))))
       (str/join)))