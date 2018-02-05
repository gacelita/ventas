(ns ventas.seo
  "Prerendering"
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [clojure.core.async :as core.async :refer [go <! go-loop]]
   [etaoin.api :as etaoin]
   [ventas.database.entity :as entity]
   [clojure.string :as str]
   [cheshire.core :as cheshire]
   [clojure.java.io :as io]
   [ventas.config :as config]
   [ventas.paths :as paths]))

(defn- get-routes []
  (concat
   [[:frontend]
    [:frontend.privacy-policy]
    [:frontend.login]]
   (->> (entity/query :product)
        (map :db/id)
        (map (fn [id]
               [:frontend.product :id id])))
   (->> (entity/query :category)
        (map :db/id)
        (map (fn [id]
               [:frontend.category :id id])))))

(defn- no-pending-requests-async [driver]
  (go-loop []
   (if-not (etaoin/js-execute driver "return ventas.ws.js_pending_requests();")
     true
     (do (<! (core.async/timeout 400))
         (recur)))))

(defn prerender-all []
  (let [{:keys [port host]} (config/get :server)
        driver (etaoin/chrome-headless)]
    (etaoin/go driver (str "http://" host ":" port))
    (go
      (doseq [route (get-routes)]
        (timbre/debug "Prerendering route" route)
        (etaoin/js-execute driver (str "ventas.routes.js_go_to("
                                       (cheshire/encode route)
                                       ");"))
        (let [pending-ch (no-pending-requests-async driver)
              [message ch] (core.async/alts! [pending-ch
                                              (core.async/timeout 4000)])
              url (subs (etaoin/js-execute driver "return document.location.pathname;") 1)
              path (str (paths/resolve ::paths/rendered)
                        "/"
                        (if (empty? url) "index" url)
                        ".html")]
          (io/make-parents path)
          (spit path
                (etaoin/js-execute driver "return document.getElementById('app').innerHTML;"))))
      (etaoin/quit driver))))