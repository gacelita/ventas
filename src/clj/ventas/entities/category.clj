(ns ventas.entities.category
  (:require [clojure.spec.alpha :as spec]
            [clojure.core.async :refer [<! go-loop]]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.events :as events]))

(spec/def :category/name string?)
(spec/def :category/parent
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :category)))))

(spec/def :category/image
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :file)))))

(spec/def :schema.type/category
  (spec/keys :req [:category/name]
          :opt [:category/image :category/parent]))

(defmethod entity/fixtures :category [_]
  [{:name "Default"}])

(defmethod entity/json :category [entity]
  (as-> entity entity
        (dissoc entity :type)
        (if-let [image (:image entity)]
          (assoc entity :image (entity/json (entity/find image)))
          entity)))