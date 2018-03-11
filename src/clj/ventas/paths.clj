(ns ventas.paths
  (:refer-clojure :exclude [resolve])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def project-resources
  "A path for project-wide resources, like the configuration"
  ::project-resources)

(def sites
  "Directory where the site configurations live"
  ::sites)

(def rendered
  "Rendered HTML fragments"
  ::rendered)

(def public
  "Files accessible via HTTP"
  ::public)

(def public-files
  "Non-HTML public files. This is a necessary because of routing limitations."
  ::public-files)

(def storage
  "Regular file storage. This is where :file entities live."
  ::storage)

(def resized-images
  "Resized images"
  ::resized)

(def seeds
  "Where the files for seeding live"
  (str project-resources "/seeds"))

(def ^:private paths
  {project-resources "resources"
   public [project-resources "/public"]
   public-files [public "/files"]
   storage "storage"
   sites [storage "/sites"]
   resized-images [storage "/resized-images"]
   rendered [storage "/rendered"]})

(defn- resolve-path [v]
  (if (string? v)
    v
    (let [path (get paths v)]
      (if (string? path)
        path
        (apply str (map resolve-path path))))))

(defn resolve
  "Resolves a path, makes sure it exists and returns it"
  [kw]
  (let [path (resolve-path kw)]
    (io/make-parents (str path "/."))
    path))

(defn path->resource [path]
  (str/replace path (str (resolve-path project-resources) "/") ""))
