(ns ventas.search.entities
  "Generic fulltext entity search"
  (:require
   [ventas.database.entity :as entity]
   [ventas.database :as db]
   [ventas.utils :as utils]
   [ventas.search :as search]
   [ventas.search.indexing :refer [subfield-property]]))

(defn- prepare-search-attrs
  "Applies the culture to the idents that refer to i18n entities.
   :product/name -> :product/name__en_US
   :product/reference -> :product/reference"
  [attrs culture-kw]
  (for [attr attrs]
    (let [{:ventas/keys [refEntityType]} (db/etouch attr)]
      (if-not (= refEntityType :i18n)
        attr
        (subfield-property attr culture-kw)))))

(defn search
  "Fulltext search for `search` in the given `attrs`"
  [text attrs culture-id]
  {:pre [(utils/check ::entity/ref culture-id)]}
  (let [culture (entity/find culture-id)
        shoulds (for [attr (prepare-search-attrs attrs (:i18n.culture/keyword culture))]
                  {:match {attr text}})]
    (->> (get-in (search/search {:query {:bool {:should shoulds}}
                                 :_source false})
                 [:body :hits :hits])
         (map :_id)
         (map (fn [v] (Long/parseLong v)))
         (map #(entity/find-serialize % {:culture (:db/id culture)
                                         :keep-type? true})))))
