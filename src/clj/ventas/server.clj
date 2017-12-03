(ns ventas.server
  (:require
   [chord.format.fressian]
   [chord.http-kit]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [compojure.core :refer [GET defroutes]]
   [compojure.route]
   [mount.core :as mount :refer [defstate]]
   [org.httpkit.server :as http-kit]
   [prone.middleware :as prone]
   [ring.middleware.defaults :as ring.defaults]
   [ring.middleware.gzip :as ring.gzip]
   [ring.middleware.params :as ring.params]
   [ring.middleware.session :as ring.session]
   [ring.util.mime-type :as ring.mime-type]
   [ring.util.response :as ring.response]
   [taoensso.timbre :as timbre :refer [debug info]]
   [ventas.config :as config]
   [ventas.database.entity :as entity]
   [ventas.utils :as utils]
   [ventas.entities.file :as entities.file]
   [ventas.paths :as paths]
   [ventas.server.ws :as server.ws]
   [ventas.logging]
   [clojure.pprint :as pprint])
  (:gen-class)
  (:import [clojure.lang Keyword]))

(cheshire.generate/add-encoder Keyword cheshire.generate/encode-str)

(defn wrap-prone
  "If the debug mode is enabled, wraps a Ring request with the Prone library"
  [handler]
  (if (config/get :debug)
    (prone/wrap-exceptions handler {:app-namespaces ["ventas"]})
    handler))

(defn- add-mime-type [response path]
  (if-let [mime-type (ring.mime-type/ext-mime-type path (:mime-types {}))]
    (ring.response/content-type response mime-type)
    response))

(defn- handle-file [path]
  (let [resource-path (paths/path->resource (str paths/public path))]
    (if-let [resource-response (ring.response/resource-response resource-path)]
      (add-mime-type resource-response path)
      (compojure.route/not-found nil))))

(defn- handle-resource [resource-kw]
  (if-let [resource (first (entity/query :resource {:keyword (keyword resource-kw)}))]
    (let [filename (entities.file/filename (entity/find (:resource/file resource)))
          resource-path (paths/path->resource (str paths/images "/" filename))]
      (-> (ring.response/resource-response resource-path)
          (add-mime-type filename)))
    (compojure.route/not-found nil)))

(defn- handle-spa []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (-> (slurp (io/resource "public/index.html"))
             (str/replace "{{current-theme}}" "ventas.themes.clothing.core")
             (str/replace "{{base-url}}" (paths/base-url)))})

(defn- handle-websocket [format]
  (chord.http-kit/wrap-websocket-handler
   (partial server.ws/handle-messages format)
   {:format format}))

;; All routes
(defroutes routes
  (GET "/ws/json-kw" []
    (handle-websocket :json-kw))
  (GET "/ws/fressian" []
    (handle-websocket :fressian))
  (GET "/files/*" {{path :*} :route-params}
    (handle-file path))
  (GET "/resources/*" {{resource-kw :*} :route-params}
    (handle-resource resource-kw))
  (GET "/*" _
    (handle-spa)))

(def http-handler
  (-> routes
      (wrap-prone)
      (ring.session/wrap-session)
      (ring.params/wrap-params)
      (ring.defaults/wrap-defaults ring.defaults/site-defaults)
      (ring.gzip/wrap-gzip)))

(defn stop-server! [stop-fn]
  (info "Stopping server")
  (when (ifn? stop-fn)
    (try
      (stop-fn)
      (catch Exception e
        ;; Avoids occasional ConcurrentModificationException, which is a bug in httpkit
        ))))

(defn start-server! []
  (info "Starting server")
  (let [{:keys [host port]} (config/get :server)]
    (info "Starting server on" (str host ":" port))
    (http-kit/run-server http-handler
                         {:ip host
                          :port port
                          :join? false})))

(defstate server
  :start
  (do
    (server.ws/start!)
    (start-server!))
  :stop
  (do
    (server.ws/stop!)
    (stop-server! server)))
