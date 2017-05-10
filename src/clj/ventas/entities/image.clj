(ns ventas.entities.image
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.config :refer [config]]))

(s/def :image.tag/image
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :image)))))
(s/def :image.tag/target
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :user)))))
(s/def :image.tag/x
  (s/with-gen integer? #(gen/choose 100 1000)))
(s/def :image.tag/y
  (s/with-gen integer? #(gen/choose 100 1000)))
(s/def :image.tag/caption string?)
(s/def :schema.type/image.tag
  (s/with-gen (s/keys :req [:image.tag/image :image.tag/target]
                      :opt [:image.tag/x :image.tag/y :image.tag/caption])
              #(s/gen (s/keys :req [:image.tag/image :image.tag/target :image.tag/x :image.tag/y :image.tag/caption]))))


(defmethod db/entity-postquery :image [entity]
  (-> entity
      (assoc :url (str (:base-url config) "img/" (:id entity) "." (name (:extension entity))))
      (assoc :tags (db/entity-query :image.tag {:image (:id entity)}))))
