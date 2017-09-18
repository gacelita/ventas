(ns ventas.routes
  (:require [clojure.string :as str]
            [bidi.bidi :as bidi]
            [ventas.utils.logging :refer [trace debug info warn error]]
            [accountant.core :as accountant]
            [reagent.session :as session]))

(comment
  ["/" {"admin/" {"" :admin
                  "login/" :admin.login
                  "register/" :admin.register
                  "users/" {"" :admin.users
                            [:id "edit"] :admin.users.edit}
                  "playground/" :admin.playground}
        "frontend" {"" :frontend
                    "index" :frontend.index}
        true :not-found}])

(defn route-parents [route]
  ":admin.users.something -> [:admin :admin.users :admin.users.something]"
  (into [] (map #(keyword (str/join "." %))
                (reduce (fn [acc i]
                          (conj acc (conj (vec (last acc)) i)))
                        []
                        (drop-last (str/split (name route) #"\."))))))

(defn- index-urls
  "Creates a [route -> url] map"
  [routes]
  (reduce (fn [acc {:keys [route url] :as item}]
            (assoc acc route url))
          {}
          routes))

(defn- prepare-routes [routes]
  (let [indexed-urls (index-urls routes)]
    (map (fn [route]
           (let [parent (last (route-parents (:route route)))
                 parent-url (indexed-urls parent)]
             (update route :url #(cond
                                   (= parent-url "") %1
                                   (string? %1) (str "/" %1)
                                   :else (vec (concat ["/"] %))))))
         routes)))

(defn- reducer [acc {:keys [route url] :as item} indexed-urls]
  (let [parents (route-parents route)]
    (if (seq parents)
      (update-in acc (map #(% indexed-urls) parents) assoc url {"" route})
      (assoc acc url {"" route}))))

(defn compile-routes [routes]
  (let [routes (prepare-routes routes)
        indexed-urls (index-urls routes)]
    ["" (-> (reduce (fn [acc item]
                       (reducer acc item indexed-urls))
                     {}
                     routes)
             (assoc true :not-found))]))

(def route-data
  (atom [{:route :admin
          :name "Administración"
          :url "admin"}

         {:route :admin.users
          :name "Usuarios"
          :url "users"}

         {:route :admin.users.edit
          :name "Editar usuario"
          :url [:id "/edit"]}

         {:route :admin.login
          :name "Iniciar sesión"
          :url "login"}

         {:route :admin.register
          :name "Registro"
          :url "register"}

         {:route :admin.playground
          :name "Playground"
          :url "playground"}

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
      (error "Route not found" args))
    path))

(defn match-route
  "bidi/match-route wrapper"
  [& args]
  (apply bidi/match-route @routes args))

(defn go-to [& args]
  (when-let [path (apply path-for args)]
    (accountant/navigate! path)))

(defn current
  "Returns the current route"
  []
  (session/get :route))