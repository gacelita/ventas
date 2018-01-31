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
   [ventas.entities.image-size :as entities.image-size]
   [ventas.plugin :as plugin])
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
    (if-let [response (ring.response/file-response path)]
      (add-mime-type response path)
      (compojure.route/not-found ""))))

(defn- handle-spa []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (-> (slurp (io/resource "public/index.html"))
             (str/replace "{{current-theme}}" "ventas.themes.clothing.core"))})

(defn- handle-websocket [format]
  (chord.http-kit/wrap-websocket-handler
   (partial server.ws/handle-messages format)
   {:format format}))

(defn- handle-image [eid & {:keys [size]}]
  (if-let [image (entity/find eid)]
    (let [path (if size
                 (let [size (entity/find [:image-size/keyword size])]
                   (entities.image-size/transform image size))
                 (entities.file/filepath image))]
      (-> path
          (ring.response/file-response)
          (add-mime-type path)))
    (compojure.route/not-found "")))

;; All routes
(defroutes routes
  (GET "/ws/json" []
    (handle-websocket :json))
  (GET "/ws/fressian" []
    (handle-websocket :fressian))
  (GET "/files/*" {{path :*} :route-params}
    (handle-file path))
  (GET "/images/:image" [image]
    (handle-image (utils/->number image)))
  (GET "/images/:image/resize/:size" [image size]
    (handle-image (utils/->number image) :size (keyword size)))
  (GET "/plugins/:plugin/*" {{path :* plugin :plugin} :route-params}
    (plugin/handle-request (keyword plugin) path))
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
        (info ::stop-server! " - Caught exception while stopping the server:" (pr-str e))))))

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
