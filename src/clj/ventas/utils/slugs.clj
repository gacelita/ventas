(ns ventas.utils.slugs
  (:require
   [cuerdas.core :as cuerdas]
   [ventas.common.utils :as common.utils]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.database :as db]))

(defn slug [s]
  ;; cuerdas is just an implementation detail, that's why we're wrapping it
  (cuerdas/slug s))

(defn- slugify-i18n* [i18n]
  {:pre [(entity/entity? i18n)]}
  (if (:db/id i18n)
    (->> (entity/serialize i18n)
         (common.utils/map-vals slug)
         (entities.i18n/get-i18n-entity))
    (update i18n
            :i18n/translations
            (fn [translations]
              (map #(update % :i18n.translation/value slug)
                   translations)))))

(defn slugify-i18n [i18n]
  (slugify-i18n*
   (if (number? i18n)
     (entity/find i18n)
     i18n)))

(defn add-slug-to-entity [entity source-attr]
  (let [source (get entity source-attr)]
    (if (and (not (:ventas/slug entity)) source)
      (assoc entity :ventas/slug (slugify-i18n source))
      entity)))

(defn resolve-by-slug [slug]
  (db/nice-query-attr
   {:find '[?id]
    :in {'?slug slug}
    :where '[[?translation :i18n.translation/value ?slug]
             [?i18n :i18n/translations ?translation]
             [?id :ventas/slug ?i18n]]}))
