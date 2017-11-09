(ns ventas.paths)

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
