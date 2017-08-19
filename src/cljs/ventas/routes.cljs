(ns ventas.routes
  (:require [clojure.string :as str]
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
  (map #(keyword (str/join "." %))
       (reduce (fn [acc i]
                 (conj acc (conj (vec (last acc)) i)))
               []
               (str/split (name route) #"\."))))

(defn- index-urls
  "Creates a [route -> url] map"
  [routes]
  (reduce (fn [acc {:keys [route url] :as item}]
            (assoc acc route url))
          {}
          routes))

(defn- reducer [acc {:keys [route url] :as item} indexed-urls]
  (let [parents (drop-last (route-parents route))]
    (if (seq parents)
      (update-in acc (map #(% indexed-urls) parents) assoc url {"" route})
      (assoc acc url {"" route}))))

(defn compile-routes [routes]
  (let [indexed-urls (index-urls routes)]
    ["/" (-> (reduce (fn [acc item]
                       (reducer acc item indexed-urls))
                     {}
                     routes)
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

(defn find-route
  "Finds a route by its id"
  [id]
  (first (filter #(= (:route %) id) @route-data)))

(defn path-for
  "bidi/path-for wrapper"
  [& args]
  (let [path (apply bidi/path-for @routes args)]
    (when-not path
      (throw (js/Error. "Route not found: " (clj->js args))))
    path))

(defn match-route
  "bidi/match-route wrapper"
  [& args]
  (apply bidi/match-route @routes args))

(defn go-to [& args]
  (when-let [path (apply path-for args)]
    (accountant/navigate! path)))

