(ns ventas.seo
  "Prerendering"
  (:require
   [cheshire.core :as cheshire]
   [clojure.core.async :as core.async :refer [go <! go-loop]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [etaoin.api :as etaoin]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [ventas.config :as config]
   [ventas.database.entity :as entity]
   [ventas.paths :as paths]
   [ventas.database :as db]
   [ventas.theme :as theme]
   [ventas.plugin :as plugin]))

(defstate driver
  :start
  (let [{:keys [host port]} (config/get :chrome-headless)]
    (timbre/info "Starting prerendering driver")
    (if-not (and host port)
      (etaoin/chrome-headless)
      (-> (etaoin/create-driver :chrome (-> (config/get :chrome-headless)
                                            (select-keys #{:host :port})))
          (etaoin/connect-driver))))
  :stop
  (do
    (timbre/info "Stopping prerendering driver")
    (etaoin/quit driver)))

(defn type->slugs [type]
  (db/nice-query
   {:find '[?slug-value]
    :in {'?type type}
    :where '[[?entity :schema/type ?type]
             [?entity :ventas/slug ?slug]
             [?slug :i18n/translations ?translations]
             [?translations :i18n.translation/value ?slug-value]]}))

(defn- frontend-ready? [driver]
  (go-loop []
   (if (etaoin/js-execute driver "return ventas.seo.ready_QMARK_();")
     true
     (do (<! (core.async/timeout 400))
         (recur)))))

(defn- server-uri []
  (let [{:keys [port host docker-host]} (config/get :server)]
    (str "http://" (or docker-host host) ":" port)))

(defn url->path [url extension]
  (str (paths/resolve ::paths/rendered)
       "/"
       (if (empty? url) "index" url)
       "."
       extension))

(defn prerender [route & {:keys [skip-init?]}]
  (timbre/debug "Prerendering route" route)
  (go
    (when-not skip-init?
      (etaoin/go driver (server-uri)))
    (etaoin/js-execute driver (str "ventas.seo.go_to(" (cheshire/encode route) ");"))
    (let [ready-ch (frontend-ready? driver)
          [message ch] (core.async/alts! [ready-ch
                                          (core.async/timeout 4000)])
          url (subs (etaoin/js-execute driver "return document.location.pathname;") 1)]
      (let [html-path (url->path url "html")]
        (io/make-parents html-path)
        (spit html-path (etaoin/js-execute driver "return document.getElementById('app').innerHTML;")))
      (spit (url->path url "edn") (etaoin/js-execute driver "return ventas.seo.dump_db();")))))

(defn prerender-all []
  (etaoin/go driver (server-uri))
  (go
    (when-let [prerendered-fn (-> (theme/current)
                                  plugin/plugin
                                  :prerendered-routes)]
      (doseq [route (prerendered-fn)]
        (<! (prerender route :skip-init? true))))))