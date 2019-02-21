(ns ventas.entities.file-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.database.entity :as entity]
   [ventas.entities.file :as sut]
   [ventas.test-tools :as test-tools :refer [with-test-image]]))

(def example-file
  {:file/keyword :example-file
   :file/extension "jpg"
   :schema/type :schema.type/file})

(use-fixtures :once #(test-tools/with-test-context
                       (entity/create* example-file)
                       (%)))

(deftest identifier
  (let [file (entity/find [:file/keyword (:file/keyword example-file)])]
    (is (= "example-file"
           (sut/identifier file)))
    (is (= (:db/id file)
           (sut/identifier (-> file
                               (dissoc :file/keyword)))))))

(deftest filename
  (let [file (entity/find [:file/keyword (:file/keyword example-file)])]
    (is (= "example-file.jpg" (sut/filename file)))))

(deftest filepath
  (let [file (entity/find [:file/keyword (:file/keyword example-file)])]
    (is (= "storage/example-file.jpg" (sut/filepath file)))))

(deftest create-from-file!
  (with-test-image
   (fn [image]
     (is (= {:file/extension "png"
             :schema/type :schema.type/file}
            (-> (sut/create-from-file! (str image) "png")
                (dissoc :db/id)))))))

(deftest normalization
  (let [file (entity/find [:file/keyword (:file/keyword example-file)])]
    (is (= "/files/example-file" (:url (entity/serialize file))))
    (is (not (:file/url (entity/deserialize :file (entity/serialize file)))))))
