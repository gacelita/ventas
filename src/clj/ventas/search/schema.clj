(ns ventas.search.schema
  (:require
   [ventas.database.entity :as entity]
   [ventas.search :as search]
   [ventas.database :as db]
   [ventas.utils :as utils]
   [clojure.string :as str]))

(defn- find-migrated-ids []
  (-> (search/search {:query {:term {:schema/type :es-migration}}})
      (get-in [:body :hits :hits])
      (->> (map (fn [hit]
                  (-> hit :_source :es-migration/keyword (subs 1) keyword))))
      (set)))

(defn pending-migrations []
  (let [migrated-ids (find-migrated-ids)]
    (->> @search/type-config
         (mapcat (fn [[kw config]]
                   (map (fn [[migr-kw migr]]
                          [(keyword (name kw) (name migr-kw)) migr])
                        (:migrations config))))
         (remove (comp migrated-ids first)))))

(defn migrate! []
  (doseq [[kw migr] (pending-migrations)]
    (when (:properties migr)
      (search/update-index
       {:properties (:properties migr)}))
    (search/create-document {:schema/type :es-migration
                             :es-migration/keyword kw})))

(defn autocomplete-type []
  {:type "text"
   :analyzer "ventas/autocomplete"
   :search_analyzer "standard"})

(defn setup! []
  (search/create-index
   {:mappings {:doc {:properties {:es-migration/keyword {:type "keyword"}
                                  :schema/type {:type "keyword"}}}}
    :settings {:analysis {:filter {:autocomplete_filter {:type "edge_ngram"
                                                         :min_gram 1
                                                         :max_gram 20}}
                          :analyzer {:ventas/autocomplete {:type "custom"
                                                           :tokenizer "standard"
                                                           :filter ["lowercase"
                                                                    "autocomplete_filter"]}}}}})
  (migrate!))
