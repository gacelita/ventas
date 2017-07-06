(ns ventas.entities.order-line
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(s/def :order-line/order
  (s/with-gen integer? #(gen/elements (map :id (entity/query :order)))))

(s/def :order-line/product
  (s/with-gen integer? #(gen/elements (map :id (entity/query :product)))))

(s/def :order-line/product-variation
  (s/with-gen integer? #(gen/elements (map :id (entity/query :product-variation)))))

(s/def :order-line/quantity (s/and integer? pos?))

(s/def :schema.type/order-line
  (s/keys :req [:order-line/order
                :order-line/product
                :order-line/quantity]
          :opt [:order-line/product-variation]))

(defmethod entity/json :order-line [entity]
  (as-> entity entity
        (dissoc entity :type)
        (if-let [order (:order entity)]
          (assoc entity :order (entity/json (entity/find order)))
          entity)
        (if-let [product (:product entity)]
          (assoc entity :product (entity/json (entity/find product)))
          entity)
        (if-let [product-variation (:product-variation entity)]
          (assoc entity (:product-variation (entity/json (entity/find product-variation)))
          entity))))