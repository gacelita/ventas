(ns ventas.routes
  (:require [clojure.string :as s]))

(defn pre-compile-routes [routes]
  (vec (map vec (map #(mapcat identity %) (partition 2 (partition-by keyword? routes))))))

(defn compile-route
  ([route] (compile-route route []))
  ([route acc] 
    (let [
      kw (conj acc (get route 0))
      handler (keyword (s/join "." (map name kw)))
      url (get route 1)
      subroutes (get route 2)]
      (if (empty? subroutes)
        [url handler]
        [url (vec (conj (map #(compile-route % kw) (pre-compile-routes subroutes)) ["" handler]))]))))

(defn compile-routes [routes]
  (first (map compile-route (pre-compile-routes routes))))

(def app-routes (compile-routes
   [:backend "/admin/" [
      :users "users" [
        :edit ["/" :id "/edit"]
      ]
      :login "login"
      :register "register"
      :playground "playground"]
    :not-found true
    :frontend "/" [
      :index "index"]]))