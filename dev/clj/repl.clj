(ns repl
  (:require
   [clojure.tools.namespace.repl :as tn]
   [mount.core :as mount]
   [ventas.system :as system]
   [ventas.server :as server]
   [compojure.core :refer [GET defroutes]]
   [shadow.cljs.devtools.server :as shadow.server]
   [shadow.cljs.devtools.api :as shadow.api]
   [hiccup.core :as hiccup]
   [ventas.html :as html]))

(defn- devcards-html [req]
  (str "<!DOCTYPE html>\n"
       (hiccup/html
        [:html
         [:head
          [:base {:href "/"}]
          [:meta {:charset "UTF-8"}]
          [:title "Devcards"]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:script {:src "files/js/devcards/main.js"}]
          (html/html-resources req)]
         [:body
          [:div#app]
          [:script "devcards.core.start_devcard_ui_BANG__STAR_();"]]])))

(defn handle-devcards [req]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (devcards-html req)})

(defroutes dev-routes
  (GET "/devcards" _
    handle-devcards))

(defn refresh [& {:keys [after]}]
  (let [result (tn/refresh :after after)]
    (when (instance? Throwable result)
      (throw result))))

(defn refresh-all [& {:keys [after]}]
  (let [result (tn/refresh-all :after after)]
    (when (instance? Throwable result)
      (throw result))))

(alter-var-root #'*warn-on-reflection* (constantly true))
(tn/set-refresh-dirs "dev/clj" "src/clj" "src/cljc")

(defn start [& [states]]
  (-> (mount/only (or states system/default-states))
      (mount/with-args {::server/handler #'dev-routes})
      mount/start)
  :done)

(defn r [& subsystems]
  (let [states (system/get-states subsystems)]
    (when (seq states)
      (mount/stop states))
    (refresh :after 'repl/start)))

(defn init-next []
  (start))

(defn init []
  (require 'ventas.core)
  (refresh-all :after 'repl/init-next))

(defn watch-cljs [build-id]
  (shadow.server/start!)
  (shadow.api/watch build-id))

(defn release-cljs [build-id]
  (shadow.server/start!)
  (shadow.api/release build-id))