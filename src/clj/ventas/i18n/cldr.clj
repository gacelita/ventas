(ns ventas.i18n.cldr
  "CLDR importer. Supports countries and states."
  (:require
   [clojure.string :as str]
   [clojure.xml :as xml]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.utils :as utils]))

(defn- get-country-names [path]
  (let [xml (xml/parse path)
        countries (->> xml
                       :content
                       (common.utils/find-first #(= (:tag %) :localeDisplayNames))
                       :content
                       (common.utils/find-first #(= (:tag %) :territories))
                       :content)]
    (for [{:keys [attrs content]} countries]
      {:keyword (keyword (str/lower-case (:type attrs)))
       :name (first content)})))

(defn- get-state-names [path]
  (let [xml (xml/parse path)
        states (->> xml
                    :content
                    (common.utils/find-first #(= (:tag %) :localeDisplayNames))
                    :content
                    (common.utils/find-first #(= (:tag %) :subdivisions))
                    :content)]
    (for [{:keys [attrs content]} states]
      {:keyword (keyword (str/lower-case (:type attrs)))
       :name (first content)})))

(defn- get-states-hierarchy [path]
  (let [xml (xml/parse path)
        states (->> xml
                    :content
                    (common.utils/find-first #(= (:tag %) :subdivisionContainment))
                    :content
                    (map :attrs))]
    (utils/mapm
     (fn [{:keys [contains type]}]
       [(keyword (str/lower-case type))
        (->> (str/split contains #" ")
             (map keyword))])
     states)))

(defn- transact-countries! [countries]
  (->> (for [[keyword translations] countries]
         {:schema/type :schema.type/country
          :country/keyword keyword
          :country/name {:schema/type :schema.type/i18n
                         :i18n/translations translations}})
       (db/transact)))

(defn- transact-states! [states]
  (->> (for [[keyword translations] states]
         {:schema/type :schema.type/state
          :state/keyword keyword
          :state/name {:schema/type :schema.type/i18n
                       :i18n/translations translations}})
       (db/transact)))

(defn- find-root [tree kw]
  (let [parent (get tree kw)]
    (if parent
      (find-root tree parent)
      kw)))

(defn- transact-states-hierarchy! [hierarchy]
  (let [inverted (reduce (fn [tree [keyword children]]
                           (reduce (fn [acc child]
                                     (assoc acc child keyword))
                                   tree
                                   children))
                         {}
                         hierarchy)]
    (->> inverted
         (map (fn [[keyword parent]]
                (if-let [parent-ref (db/entity [:state/keyword parent])]
                  {:state/keyword keyword
                   :state/parent (:db/id parent-ref)
                   :state/country [:country/keyword (find-root inverted parent)]}
                  (when-let [country-ref (db/entity [:country/keyword parent])]
                    {:state/keyword keyword
                     :state/country (:db/id country-ref)}))))
         (remove nil?)
         (db/transact))))

(defn- accumulate-translation [culture-kw acc {:keys [keyword name]}]
  (update acc
          keyword
          #(conj %
                 {:schema/type :schema.type/i18n.translation
                  :i18n.translation/value name
                  :i18n.translation/culture [:i18n.culture/keyword culture-kw]})))

(defn- accumulate-culture [getter-fn path acc {culture-kw :i18n.culture/keyword}]
  (reduce (partial accumulate-translation culture-kw)
          acc
          (let [language (first (str/split (name culture-kw) #"_")) ]
            (getter-fn (str path "/" language ".xml")))))

(defn import-cldr!
  "Imports countries and states from an extracted CLDR package.
   Only the cultures represented by existing :i18n.culture entities will be
   considered.
   See http://cldr.unicode.org for more information"
  [path]
  (let [cultures (entity/query :i18n.culture)]
    (->> cultures
         (reduce (partial accumulate-culture get-country-names (str path "/common/main"))
                 {})
         (transact-countries!))
    (->> cultures
         (reduce (partial accumulate-culture get-state-names (str path "/common/subdivisions"))
                 {})
         (transact-states!))

    (->> (get-states-hierarchy (str path "/common/supplemental/subdivisions.xml"))
         (transact-states-hierarchy!))))
