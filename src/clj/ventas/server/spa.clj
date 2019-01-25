(ns ventas.server.spa
  (:require
   [ventas.theme :as theme]
   [hiccup.core :as hiccup]
   [cheshire.core :as cheshire]
   [ventas.paths :as paths]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [ventas.plugin :as plugin]
   [ventas.common.utils :as common.utils]))

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

(def request->theme
  (memoize
   (fn [request]
     (let [themes (theme/all)]
       (or (common.utils/find-first
            (fn [[_ v]]
              (and (not (:default? v)) (:should-load? v) ((:should-load? v) request)))
            themes)
           (common.utils/find-first (comp :default? second) themes))))))

(defn handle-spa [{:keys [uri] :as request}]
  (let [[theme-id {:keys [init-script]}] (request->theme request)]
    (when-not theme-id
      (throw (Exception. "None of the themes wanted to load, and there is no default theme. Please check the registered themes")))
    (handle uri theme-id (or init-script "ventas.core.start();"))))