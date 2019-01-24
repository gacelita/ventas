(ns ventas.i18n.cldr
  "CLDR importer. Supports countries and states.
   See http://cldr.unicode.org for more information."
  (:require
   [clojure.string :as str]
   [clojure.xml :as xml]
   [datomic.api :as d]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.utils :as utils]
   [clojure.java.io :as io]
   [ventas.database.schema :as schema]
   [clojure.tools.logging :as log])
  (:import [net.lingala.zip4j.core ZipFile]))

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

(defn- countries->entities [countries]
  (for [[keyword translations] countries]
    {:schema/type :schema.type/country
     :country/keyword keyword
     :country/name {:schema/type :schema.type/i18n
                    :i18n/translations translations}}))

(defn- states->entities [states]
  (for [[keyword translations] states]
    {:schema/type :schema.type/state
     :state/keyword keyword
     :state/name {:schema/type :schema.type/i18n
                  :i18n/translations translations}}))

(defn- find-root [tree kw]
  (let [parent (get tree kw)]
    (if parent
      (find-root tree parent)
      kw)))

(defn- states-hierarchy->entities [hierarchy]
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
         (remove nil?))))

(defn- data->entities [data]
  (reduce into [] [(countries->entities (:countries data))
                   (states->entities (:states data))
                   (states-hierarchy->entities (:states-hierarchy data))]))

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

(defn- copy! [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(def ^:private extraction-target (str (System/getProperty "java.io.tmpdir") "/ventas-cldr"))

(defn download-cldr-file! [& [download-url]]
  (let [download-target (io/file (str (System/getProperty "java.io.tmpdir") "/ventas-cldr.zip"))]
    (copy! (or download-url
               "http://unicode.org/Public/cldr/latest/core.zip") download-target)
    (doto (ZipFile. download-target)
      (.extractAll extraction-target))))

(defn cldr->data
  "Gets countries and states from an extracted CLDR package.
   Only the cultures represented by existing :i18n.culture entities will be
   considered."
  [path]
  (let [cultures (entity/query :i18n.culture)]
    {:countries
     (reduce (partial accumulate-culture get-country-names (str path "/common/main"))
             {}
             cultures)
     :states
     (reduce (partial accumulate-culture get-state-names (str path "/common/subdivisions"))
             {}
             cultures)
     :states-hierarchy
     (get-states-hierarchy (str path "/common/supplemental/subdivisions.xml"))}))

(schema/register-migration!
 ::base
 [{:db/ident :ventas.cldr/cldr-import?
   :db/valueType :db.type/boolean
   :db/cardinality :db.cardinality/one}])

(defn- existing-import-id []
  (ffirst (db/q '{:find [?id]
                 :in [$]
                 :where [[?id :ventas.cldr/cldr-import? true]]})))

(defn import-cldr! [& [download-url]]
  (schema/migrate-one! ::base)
  (let [import-id (existing-import-id)]
    (if import-id
     (log/info "An import already exists. Aborting. Import ID:" import-id)
      (do
        (download-cldr-file! download-url)
        (let [data (cldr->data extraction-target)
              entities (data->entities data)]
          (log/info "Importing" (count entities) "CLDR entities")
          (db/transact (conj entities
                             {:db/id (d/tempid :db.part/tx)
                              :ventas.cldr/cldr-import? true})))
        :done))))
