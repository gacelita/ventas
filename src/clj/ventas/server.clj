(ns ventas.server
  (:require
   [cheshire.generate :as cheshire-gen]
   [chord.format.fressian]
   [chord.http-kit]
   [clojure.string :as str]
   [compojure.core :refer [GET POST defroutes] :as compojure]
   [compojure.route]
   [mount.core :refer [defstate]]
   [org.httpkit.server :as http-kit]
   [prone.middleware :as prone]
   [ring.middleware.defaults :as ring.defaults]
   [ring.middleware.gzip :as ring.gzip]
   [ring.middleware.params :as ring.params]
   [ring.middleware.edn :as ring.edn]
   [ring.middleware.json :as ring.json]
   [ring.middleware.session :as ring.session]
   [ring.util.mime-type :as ring.mime-type]
   [ring.util.response :as ring.response]
   [taoensso.timbre :as timbre]
   [ventas.config :as config]
   [ventas.database.entity :as entity]
   [ventas.entities.file :as entities.file]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.logging]
   [ventas.paths :as paths]
   [ventas.plugin :as plugin]
   [ventas.server.spa :as server.spa]
   [ventas.server.ws :as server.ws]
   [ventas.server.http-ws :as server.http-ws]
   [ventas.site :as site]
   [ventas.stats :as stats]
   [ventas.utils :as utils])
  (:import
   [clojure.lang Keyword])
  (:gen-class))

(cheshire-gen/add-encoder Keyword cheshire.generate/encode-str)

(defn wrap-prone
  "If the debug mode is enabled, wraps a Ring request with the Prone library"
  [handler]
  (if (config/get :debug)
    (prone/wrap-exceptions handler {:app-namespaces ["ventas"]})
    handler))

(defn- add-mime-type [response path]
  (if-let [mime-type (ring.mime-type/ext-mime-type path)]
    (ring.response/content-type response mime-type)
    response))

(defn- file-path-by-eid [eid]
  (when-let [file (entity/find eid)]
    (entities.file/filepath file)))

(defn- file-path-by-keyword [kw]
  (when-let [file (entity/find [:file/keyword kw])]
    (entities.file/filepath file)))

(defn- handle-file [search]
  (let [path (cond
               (utils/->number search) (file-path-by-eid (utils/->number search))
               (not (str/includes? search "/")) (file-path-by-keyword (keyword search))
               :else (str (paths/resolve paths/public) search))]
    (if-let [response (some-> path ring.response/file-response)]
      (add-mime-type response path)
      (compojure.route/not-found ""))))

(defn- handle-websocket [format opts]
  (chord.http-kit/wrap-websocket-handler
   (partial server.ws/handle-messages format opts)
   {:format format}))

(defn- handle-image [eid & {:keys [size]}]
  (if-let [image (entity/find eid)]
    (let [path (if size
                 (let [size (entity/find [:image-size/keyword size])]
                   @(entities.image-size/transform image size))
                 (entities.file/filepath image))]
      (-> path
          (ring.response/file-response)
          (add-mime-type path)))
    (compojure.route/not-found "")))

(defroutes api-routes
  (POST "/http-ws/:name" req
    (server.http-ws/handle req)))

(def api-handler
  (-> api-routes
      (site/wrap-multisite)
      (ring.edn/wrap-edn-params)
      (ring.json/wrap-json-body {:keywords? true})
      (ring.defaults/wrap-defaults ring.defaults/api-defaults)
      (ring.gzip/wrap-gzip)))

;; All routes
(defroutes site-routes
  (GET "/ws/fressian" req
    (handle-websocket :fressian (select-keys req #{:server-name})))
  (GET "/ws/transit-json" req
    (handle-websocket :transit-json (select-keys req #{:server-name})))
  (GET "/files/*" {{path :*} :route-params}
    (handle-file path))
  (GET "/images/:image" [image]
    (handle-image (utils/->number image)))
  (GET "/images/:image/resize/:size" [image size]
    (handle-image (utils/->number image) :size (keyword size)))
  (GET "/plugins/:plugin/*" {{path :* plugin :plugin} :route-params}
    (plugin/handle-request (keyword plugin) path))
  (GET "/devcards" _
    server.spa/handle-devcards)
  (GET "/*" _
    server.spa/handle-spa))

(def site-handler
  (-> site-routes
      (wrap-prone)
      (site/wrap-multisite)
      (stats/wrap-stats)
      (ring.session/wrap-session)
      (ring.params/wrap-params)
      (ring.defaults/wrap-defaults ring.defaults/site-defaults)
      (ring.gzip/wrap-gzip)))

(def http-handler
  (if-not (config/get :debug)
    site-handler
    (compojure/routes api-handler site-handler)))

(defn stop-server! [stop-fn]
  (timbre/info "Stopping server")
  (when (ifn? stop-fn)
    (try
      (stop-fn)
      (catch Throwable e
        ;; Avoids occasional ConcurrentModificationException, which is a bug in httpkit
        (timbre/info ::stop-server! " - Caught exception while stopping the server:" (pr-str e))))))

(defn start-server! []
  (timbre/info "Starting server")
  (let [{:keys [host port]} (config/get :server)]
    (timbre/info "Starting server on" (str host ":" port))
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
