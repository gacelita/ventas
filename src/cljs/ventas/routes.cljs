(ns ventas.routes
  (:require [clojure.string :as s]
            [taoensso.timbre :as timbre :refer-macros [tracef debugf infof warnf errorf
                                                       trace debug info warn error]]))

(comment
  ["/" {"admin/" {"" :backend
                  "login/" :backend.login
                  "register/" :backend.register
                  "users/" {"" :backend.users
                            [:id "edit"] :backend.users.edit}
                  "playground/" :backend.playground}
        "frontend" {"" :frontend
                    "index" :frontend.index}
        :not-found true}])

(defn route-parents [route]
  ":backend.users.something -> [:backend :backend.users :backend.users.something]"
  (map (fn [a] (keyword (clojure.string/join "." a))) (reduce (fn [acc i] (conj acc (conj (vec (last acc)) i))) [] (clojure.string/split (name route) #"\."))))

(defn index-urls [routes]
  (reduce (fn [acc {:keys [route url] :as item}] (conj acc [route url])) {} routes))

(defn reducer [acc {:keys [route url] :as item} indexed-urls]
  (let [parents (drop-last (route-parents route))]
    (if (seq parents)
      (update-in acc (map #(% indexed-urls) parents) conj [url {"" route}])
      (conj acc [url {"" route}]))))

(defn compile-routes [routes]
  (let [indexed-urls (index-urls routes)]
    ["/" (reduce (fn [acc item] (reducer acc item indexed-urls)) {} routes)]))

(def raw-routes [
  {:route :backend
   :name "Administración"
   :url "admin/"}

  {:route :backend.users
   :name "Usuarios"
   :url "users/"}

  {:route :backend.users.edit
   :name "Editar usuario"
   :url [:id "/edit"]}
  
  {:route :backend.login
   :name "Iniciar sesión"
   :url "login/"}

  {:route :backend.register
   :name "Registro"
   :url "register/"}

  {:route :backend.playground
   :name "Playground"
   :url "playground/"}

  {:route :frontend
   :name "Inicio"
   :url "frontend"}

  {:route :frontend.index
   :name "Índice"
   :url "/index"}

  {:route :frontend.product
   :name "Producto"
   :url ["/product/" :id]}

  {:route :not-found
   :url true}])

(def routes (compile-routes raw-routes))

(debug routes)

(defn raw-route [route-kw]
  (first (filter #(= (:route %) route-kw) raw-routes)))