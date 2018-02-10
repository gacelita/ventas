(ns ventas.server
  (:require
   [cheshire.core :as cheshire]
   [chord.format.fressian]
   [chord.http-kit]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [compojure.core :refer [GET defroutes]]
   [compojure.route]
   [mount.core :refer [defstate]]
   [org.httpkit.server :as http-kit]
   [prone.middleware :as prone]
   [ring.middleware.defaults :as ring.defaults]
   [ring.middleware.gzip :as ring.gzip]
   [ring.middleware.params :as ring.params]
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
   [ventas.server.ws :as server.ws]
   [ventas.theme :as theme]
   [ventas.utils :as utils]
   [ventas.site :as site])
  (:import
   [clojure.lang Keyword])
  (:gen-class))

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

(defn rendered-file [path extension]
  (let [path (if (= path "/")
               "/index"
               path)
        file (io/as-file (str (paths/resolve ::paths/rendered) path "." extension))]
    (if (.exists file)
      (slurp file)
      "")))

(defn rendered-db-script [path]
  (let [edn-str (rendered-file path "edn")]
    (if (empty? edn-str)
      ""
      (str "<script>window.__rendered_db="
           (cheshire/encode edn-str)
           "</script>"))))

(defn- handle-spa [{:keys [uri]}]
  (timbre/debug "Handling SPA" uri)
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (let [theme-name (theme/current)
               {:keys [cljs-ns]} (plugin/plugin theme-name)]
           (-> (slurp (io/resource "public/index.html"))
               (str/replace "{{theme}}"
                            (name theme-name))
               (str/replace "{{rendered-html}}"
                            (rendered-file uri "html"))
               (str/replace "{{rendered-db-script}}"
                            (rendered-db-script uri))))})

(defn- handle-devcards [_]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (let [theme-name (theme/current)
               {:keys [cljs-ns]} (plugin/plugin theme-name)]
           (-> (slurp (io/resource "public/devcards.html"))
               (str/replace "{{theme}}"
                            (name theme-name))))})

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
  (GET "/devcards" _
    handle-devcards)
  (GET "/*" _
    handle-spa))

(def http-handler
  (-> routes
      (wrap-prone)
      (site/wrap-multisite)
      (ring.session/wrap-session)
      (ring.params/wrap-params)
      (ring.defaults/wrap-defaults ring.defaults/site-defaults)
      (ring.gzip/wrap-gzip)))

(defn stop-server! [stop-fn]
  (timbre/info "Stopping server")
  (when (ifn? stop-fn)
    (try
      (stop-fn)
      (catch Exception e
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
