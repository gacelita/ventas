(ns ventas.server.spa
  (:require
   [ventas.theme :as theme]
   [hiccup.core :as hiccup]
   [taoensso.timbre :as timbre]
   [cheshire.core :as cheshire]
   [ventas.paths :as paths]
   [clojure.java.io :as io]))

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

(defn get-html [uri theme init-script]
  (str "<!DOCTYPE html>\n"
       (hiccup/html
        [:html
         [:head
          [:base {:href "/"}]
          [:meta {:charset "UTF-8"}]
          [:title "ventas"]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:link {:href "files/css/style.css" :rel "stylesheet" :type "text/css"}]
          [:link {:href (str "files/css/themes/" theme ".css") :rel "stylesheet" :type "text/css"}]]
         [:body
          (rendered-db-script uri)
          [:div#app (rendered-file uri "html")]
          [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/semantic-ui@2.2.14/dist/semantic.min.css"}]
          [:script {:src (str "files/js/compiled/" theme ".js") :type "text/javascript"}]
          [:script init-script]]])))

(defn- handle [uri init-script]
  (timbre/debug "Handling SPA" uri)
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (get-html uri (name (theme/current)) init-script)})

(defn handle-spa [{:keys [uri]}]
  (handle uri "ventas.core.start();"))

(defn handle-devcards [{:keys [uri]}]
  (handle uri "devcards.core.start_devcard_ui_BANG__STAR_();"))