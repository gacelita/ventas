(ns ventas.search.entities
  "Generic fulltext entity search"
  (:require
   [ventas.database.entity :as entity]
   [ventas.database :as db]
   [ventas.utils :as utils]
   [ventas.search :as search]))

(defn- prepare-search-attrs [attrs culture-kw]
  (for [attr attrs]
    (let [{:ventas/keys [refEntityType]} (db/touch-eid attr)]
      (cond
        (= refEntityType :i18n)
        (keyword (namespace attr)
                 (str (name attr) "__" (name culture-kw)))

        :else attr))))

(defn search
  "Fulltext search for `search` in the given `attrs`"
  [text attrs culture-id]
  {:pre [(utils/check ::entity/ref culture-id)]}
  (let [culture (entity/find culture-id)]
    (let [shoulds (for [attr (prepare-search-attrs attrs (:i18n.culture/keyword culture))]
                    {:match {attr text}})
          hits (-> (search/search {:query {:bool {:should shoulds}}
                                   :_source false})
                   (get-in [:body :hits :hits]))]
      (->> hits
           (map :_id)
           (map (fn [v] (Long/parseLong v)))
           (map #(entity/find-serialize % {:culture (:db/id culture)
                                           :keep-type? true}))))))
