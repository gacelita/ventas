(ns ventas.entities.category-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.test-tools :as test-tools]
   [ventas.database :as db]
   [ventas.entities.category :as sut]
   [ventas.database.entity :as entity]
   [taoensso.timbre :as timbre]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.database.seed :as seed]))

(def example-category
  {:category/name (entities.i18n/get-i18n-entity {:en_US "Example category"})
   :category/keyword :example-category
   :category/image {:schema/type :schema.type/file
                    :file/extension "jpg"}
   :category/parent {:category/name (entities.i18n/get-i18n-entity {:en_US "Example parent category"})
                     :category/keyword :example-category-parent
                     :category/image {:schema/type :schema.type/file
                                      :file/extension "jpg"}
                     :category/parent {:category/name (entities.i18n/get-i18n-entity {:en_US "Example root category"})
                                       :category/keyword :example-category-root
                                       :schema/type :schema.type/category}
                     :schema/type :schema.type/category}
   :schema/type :schema.type/category})

(declare category)

(use-fixtures :once #(with-redefs [db/db (test-tools/test-conn)]
                       (timbre/with-level
                        :report
                        (seed/seed :minimal? true)
                        (with-redefs [category (entity/create* example-category)]
                          (%)))))

(deftest serialization
  (is (= "jpg" (get-in (entity/serialize category) [:image :extension])))
  (is (not (:category/image (entity/deserialize :category (entity/serialize category))))))

(deftest get-image
  (is (= "jpg" (:extension (sut/get-image category)))))

(deftest get-parents
  (let [pulled-category (db/pull '[{:category/parent [*]} *] (:db/id category))]
    (is (= #{(get-in pulled-category [:db/id])
             (get-in pulled-category [:category/parent :db/id])
             (get-in pulled-category [:category/parent :category/parent :db/id])}
           (sut/get-parents category)))))

(deftest get-parent-slug
  (is (= "example-parent-category-example-category"
         (-> (sut/get-parent-slug (:db/id category))
             :i18n/translations
             first
             :i18n.translation/value))))

(deftest add-slug-to-category
  (let [slug (:ventas/slug (#'sut/add-slug-to-category category))]
    (is slug)
    (is (= "example-parent-category-example-category"
           (entity/find-json slug {:culture [:i18n.culture/keyword :en_US]})))))