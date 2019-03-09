(ns ventas.server
  (:require
   [cheshire.generate :as cheshire-gen]
   [chord.format.fressian]
   [chord.http-kit]
   [clojure.string :as str]
   [compojure.core :refer [GET POST defroutes] :as compojure]
   [compojure.route]
   [mount.core :as mount :refer [defstate]]
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
   [ventas.config :as config]
   [ventas.database.entity :as entity]
   [ventas.entities.file :as entities.file]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.paths :as paths]
   [ventas.plugin :as plugin]
   [ventas.server.admin-spa :as admin-spa]
   [ventas.server.ws :as server.ws]
   [ventas.server.http-ws :as server.http-ws]
   [ventas.site :as site]
   [ventas.utils :as utils]
   [clojure.tools.logging :as log])
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
  (let [image (entity/find eid)
        size-entity (and size (entity/find [:image-size/keyword size]))]
    (cond
      (not image) (compojure.route/not-found "Image not found")
      (and size (not size-entity)) (compojure.route/not-found "Size not found")
      :else
      (let [path (if size-entity
                   @(entities.image-size/transform image size-entity)
                   (entities.file/filepath image))]
        (-> path
            (ring.response/file-response)
            (add-mime-type path))))))

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
    (plugin/handle-request (keyword plugin) path)))

(def site-handler
  (-> (compojure/routes site-routes admin-spa/handler)
      (wrap-prone)
      (site/wrap-multisite)
      (ring.session/wrap-session)
      (ring.params/wrap-params)
      (ring.defaults/wrap-defaults ring.defaults/site-defaults)
      (ring.gzip/wrap-gzip)))

(def http-handler
  (compojure/routes api-handler site-handler))

(defn stop-server! [stop-fn]
  (log/info "Stopping server")
  (when (ifn? stop-fn)
    (try
      (stop-fn)
      (catch Throwable e
        ;; Avoids occasional ConcurrentModificationException, which is a bug in httpkit
        (log/info ::stop-server! " - Caught exception while stopping the server:" (pr-str e))))))

(defn start-server! [& [args]]
  (log/info "Starting server")
  (let [{:keys [host port]} (config/get :server)]
    (log/info "Starting server on" (str host ":" port))
    (http-kit/run-server (if (::handler args)
                           (compojure/routes (::handler args) http-handler)
                           http-handler)
                         {:ip host
                          :port port
                          :join? false})))

(defstate server
  :start
  (do
    (server.ws/start!)
    (start-server! (mount/args)))
  :stop
  (do
    (server.ws/stop!)
    (stop-server! server)))
