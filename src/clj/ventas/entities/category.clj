(ns ventas.entities.category
  (:require [clojure.spec :as s]
            [clojure.core.async :refer [<! go-loop]]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.events :as events]))

(s/def :category/name string?)
(s/def :category/parent
  (s/with-gen integer? #(gen/elements (map :id (entity/query :category)))))

(s/def :category/image
  (s/with-gen integer? #(gen/elements (map :id (entity/query :file)))))

(s/def :schema.type/category
  (s/keys :req [:category/name]
          :opt [:category/image :category/parent]))

(defmethod entity/fixtures :category [_]
  [{:name "Default"}])

(defmethod entity/json :category [entity]
  (as-> entity entity
        (dissoc entity :type)
        (if-let [image (:image entity)]
          (assoc entity :image (entity/json (entity/find image)))
          entity)))