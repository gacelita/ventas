(ns ventas.plugins.featured-products.core
  (:require [ventas.plugin :as plugin]))

(defmethod plugin/filter :ventas.entities.product/postquery [product]
  (-> product
      (assoc :name "Overriden")))

(defmethod plugin/action :ventas.entities.file/precreate [entity]
  (println "File precreate"))

(comment [{:db/ident :plugins.featured-products.product/featured
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}])