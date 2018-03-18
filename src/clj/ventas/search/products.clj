(ns ventas.search.products
  "Products ES search"
  (:require
   [ventas.database.entity :as entity]
   [ventas.search :as search]
   [ventas.entities.product :as entities.product]))

(defn- get-product-category-filter [categories]
  (mapcat (fn [category]
            [{:term {:product/categories category}}])
          categories))

(defn- term-aggregation->json [{:keys [buckets]} & [json-opts taxonomy-kw]]
  (entities.product/serialize-terms
   (for [{:keys [key doc_count]} buckets]
     (let [{:keys [taxonomy] :as term} (entity/find-serialize key json-opts)]
       (merge (dissoc term :keyword)
              {:count doc_count
               :taxonomy (or taxonomy
                             {:id taxonomy-kw
                              :keyword taxonomy-kw})})))))

(defn aggregate [categories culture]
  (let [json-opts {:culture culture}
        aggs-result (search/search {:size 0
                                    :query {:bool {:must (get-product-category-filter categories)}}
                                    :aggs {:categories {:terms {:field "product/categories"}}
                                           :terms {:terms {:field "product/terms"}}
                                           :variation-terms {:terms {:field "product/variation-terms"}}
                                           :brands {:terms {:field "product/brand"}}}})
        aggs (get-in aggs-result [:body :aggregations])]
    (concat (term-aggregation->json (:categories aggs) json-opts :category)
            (term-aggregation->json (:terms aggs) json-opts)
            (term-aggregation->json (:variation-terms aggs) json-opts))))

(defn sorting-field->es [field]
  (get {:price "product/price"}
       field))

(defn- get-products-query [{:keys [terms categories name price]} culture-kw]
  {:pre [culture-kw]}
  (concat [{:term {:schema/type ":schema.type/product"}}]
          (when terms
            [{:bool {:should (mapcat (fn [term]
                                       [{:bool {:should [{:term {:product/terms term}}
                                                         {:term {:product/variation-terms term}}]}}])
                                     terms)}}])
          (get-product-category-filter categories)
          (when price
            [{:range {:product/price {:gte (:min price)
                                      :lte (:max price)}}}])
          (when name
            [{:match {(search/i18n-field :product/name culture-kw) name}}])))

(defn search [filters {:keys [items-per-page page sorting]} culture]
  (let [culture-kw (-> culture
                       entity/find
                       :i18n.culture/keyword)
        items-per-page (or items-per-page 10)
        page (or page 0)
        results (search/search
                 (merge
                  {:_source false
                   :query {:bool {:must (get-products-query filters culture-kw)}}
                   :size items-per-page
                   :from (* page items-per-page)}
                  (let [{:keys [field direction]} sorting]
                    (when (and field direction)
                      {:sort [{(sorting-field->es field)
                               (name direction)}
                              "_score"]}))))]
    {:can-load-more? (<= items-per-page (get-in results [:body :hits :total]))
     :items (->> (get-in results [:body :hits :hits])
                 (map :_id)
                 (map (fn [v] (Long/parseLong v)))
                 (map #(entity/find-serialize % {:culture culture})))}))