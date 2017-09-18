(ns ventas.entities.resource
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :resource/keyword keyword?)
(spec/def :resource/name string?)
(spec/def :resource/file
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :file)))))

(spec/def :schema.type/resource
  (spec/keys :req [:resource/keyword
                :resource/file]
          :opt [:resource/name]))


(defmethod entity/json :resource [entity]
  (-> entity
      (dissoc :type)
      (#(if-let [t (:file %1)]
          (assoc %1 :file (entity/json (entity/find t)))
          %1))))