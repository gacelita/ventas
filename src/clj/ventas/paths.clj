(ns ventas.paths
  (:require [clojure.string :as str]
            [ventas.config :as config]))

(def project-resources
  "A path for project-wide resources, like the configuration"
  "resources")

(def public
  "Files accessible via HTTP"
  (str project-resources "/public"))

(def public-files
  "Non-HTML public files. This is a necessary because of routing limitations."
  (str public "/files"))

(def images
  "Public images"
  (str public-files "/img"))

(def transformed-images
  "Redimensioned or otherwise altered images"
  (str images "/transformed"))

(def seeds
  "Where the files for seeding live"
  (str project-resources "/seeds"))

(defn path->relative-url [path]
  (str/replace path (str public "/") ""))

(defn path->url [path]
  (str (config/get :base-url) (path->relative-url path)))

(defn path->resource [path]
  (str/replace path (str project-resources "/") ""))