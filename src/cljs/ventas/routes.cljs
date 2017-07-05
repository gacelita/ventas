(ns ventas.routes
  (:require [clojure.string :as s]
            [bidi.bidi :as bidi]
            [taoensso.timbre :as timbre :refer-macros [tracef debugf infof warnf errorf
                                                       trace debug info warn error]]
            [accountant.core :as accountant]))

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
    ["/" (-> (reduce (fn [acc item] (reducer acc item indexed-urls)) {} routes)
             (assoc :not-found true))]))

(def route-data
  (atom [{:route :backend
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

         {:route :datadmin
          :name "Datadmin"
          :url "datadmin"}]))

(def routes (atom (compile-routes @route-data)))

(defn define-route! [route]
  (swap! route-data conj route)
  (reset! routes (compile-routes @route-data)))

(defn define-routes! [new-routes]
  (swap! route-data concat new-routes)
  (reset! routes (compile-routes @route-data)))

(defn route->data [kw]
  (first (filter #(= (:route %) kw) @route-data)))

(defn path-for [& args]
  (str "//localhost:3450" (apply bidi/path-for @routes args)))

(defn go-to [& args]
  (let [path (apply bidi/path-for @routes args)]
    (when-not path
      (throw (js/Error. "Route not found: " (clj->js args))))
    (accountant/navigate! path)))

(defn match-route
  "match-route wrapper"
  [& args]
  (apply bidi/match-route @routes args))