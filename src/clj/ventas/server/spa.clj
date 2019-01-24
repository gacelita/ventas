(ns ventas.server.spa
  (:require
   [ventas.theme :as theme]
   [hiccup.core :as hiccup]
   [cheshire.core :as cheshire]
   [ventas.paths :as paths]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [ventas.plugin :as plugin]))

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
          [:link {:rel "stylesheet" :type "text/css" :href "https://cdnjs.cloudflare.com/ajax/libs/normalize/8.0.1/normalize.min.css"}]
          [:link {:rel "stylesheet" :type "text/css" :href "/files/css/style.css"}]
          [:link {:rel "stylesheet" :type "text/css" :href (str "/files/css/themes/" theme ".css") }]]
         [:body
          (rendered-db-script uri)
          [:div#app (rendered-file uri "html")]
          [:link {:rel "stylesheet" :type "text/css" :href "https://cdn.jsdelivr.net/npm/semantic-ui@2.2.14/dist/semantic.min.css"}]
          [:script {:type "text/javascript" :src (str "files/js/compiled/" theme "/main.js")}]
          [:script init-script]]])))

(defn handle [uri theme init-script]
  (log/debug "Handling SPA" uri theme)
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (get-html uri (name theme) init-script)})

(defn handle-spa [{:keys [uri]}]
  (let [theme-id (theme/current)]
    (when-not theme-id
      (throw (Exception. "No active theme! Set it in config.edn or with (ventas.entities.configuration/set! :theme ...)")))
    (let [{:keys [init-script]} (plugin/find theme-id)]
      (when-not init-script
        (throw (Exception. "No :init-script defined for the current theme. The theme won't be able to start.")))
      (handle uri (theme/current) init-script))))

(defn handle-devcards [{:keys [uri]}]
  (handle uri :devcards "devcards.core.start_devcard_ui_BANG__STAR_();"))