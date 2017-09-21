(ns ventas.entities.order-line
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :order-line/order
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :order)))))

(spec/def :order-line/product
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :product)))))

(spec/def :order-line/product-variation
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :product-variation)))))

(spec/def :order-line/quantity (spec/and integer? pos?))

(spec/def :schema.type/order-line
  (spec/keys :req [:order-line/order
                :order-line/product
                :order-line/quantity]
          :opt [:order-line/product-variation]))

(entity/register-type! :order-line)