(ns ventas.pages.admin.configuration
  (:require
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

(defn page []
  [admin.skeleton/skeleton
   "Nothing here for the moment!"])

(routes/define-route!
  :admin.configuration
  {:name ::page
   :url "configuration"
   :component page})