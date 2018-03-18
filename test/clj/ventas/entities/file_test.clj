(ns ventas.entities.file-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.test-tools :as test-tools]
   [ventas.database :as db]
   [ventas.entities.file :as sut]
   [ventas.database.entity :as entity]
   [taoensso.timbre :as timbre]))

(def example-file
  {:file/keyword :example-file
   :file/extension "jpg"
   :schema/type :schema.type/file})

(use-fixtures :once #(with-redefs [db/db (test-tools/test-conn)]
                       (timbre/with-level :report
                         (entity/create* example-file)
                         (%))))

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
  (is (= {:file/extension "png"
          :schema/type :schema.type/file}
         (-> (sut/create-from-file! "storage/logo.png")
             (dissoc :db/id)))))

(deftest get-seed-files
  (is (= ["resources/seeds/placeholder.png"]
         (map str (#'sut/get-seed-files "png")))))

(deftest normalization
  (let [file (entity/find [:file/keyword (:file/keyword example-file)])]
    (is (= "/files/example-file" (:url (entity/serialize file))))
    (is (not (:file/url (entity/deserialize :file (entity/serialize file)))))))