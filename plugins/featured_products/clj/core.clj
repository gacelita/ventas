(ns ventas.plugins.featured-products.core)

(defmethod ventas.plugin/filter :ventas.entities.product/postquery
  (fn [product]
      (-> product
          (assoc :name "Overriden"))))

(defmethod ventas.plugin/action :ventas.entities.file/precreate
  (fn [entity]
      (println "File precreate")))

(defmethod ventas.plugin/migration {:version "0.0.1" :description "Adds basic fields for this plugin"}
  [{:db/ident :plugins.featured-products.product/featured
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}])