(ns ventas.entities.order
  (:require
   [ventas.database :as db]
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.product :as entities.product]
   [clojure.test.check.generators :as gen]))

(defn get-amount
  "Returns an amount entity representing the total amount to be paid"
  [{:order/keys [lines]}]
  (->> lines
       (map entity/find)
       (map (fn [{:order.line/keys [product-variation quantity]}]
              (let [{:product/keys [price]} (entities.product/normalize-variation product-variation)
                    _ (assert price ::variation-has-no-price)
                    {:amount/keys [value currency]} (entity/find price)]
                {:schema/type :schema.type/amount
                 :amount/currency currency
                 :amount/value (* value quantity)})))
       (reduce +)))

(spec/def :order/user
  (spec/with-gen ::entity/ref #(entity/ref-generator :user)))

(def statuses
  #{;; order has not been made (cart)
    :order.status/draft
    ;; order made but not paid yet (wire transfer uses this)
    :order.status/unpaid
    ;; order has been paid
    :order.status/paid
    ;; the order is being prepared
    :order.status/acknowledged
    ;; the order is ready to be shipped
    :order.status/ready
    ;; the order has been shipped
    :order.status/shipped})

(spec/def :order/status
  (spec/with-gen
   (spec/or :pull-eid ::db/pull-eid
            :kind statuses)
   #(gen/elements statuses)))

(spec/def :order/shipping-address
  (spec/with-gen ::entity/ref #(entity/ref-generator :address)))

(spec/def :order/billing-address
  (spec/with-gen ::entity/ref #(entity/ref-generator :address)))

(spec/def :order/shipping-method
  (spec/with-gen ::entity/ref #(entity/ref-generator :shipping-method)))

(spec/def :order/shipping-comments ::generators/string)

(spec/def :order/payment-method ::generators/keyword)

(spec/def :order/payment-reference ::generators/string)

(spec/def :order/payment-amount
  (spec/with-gen ::entity/ref #(entity/ref-generator :amount)))

(spec/def :order/lines
  (spec/with-gen ::entity/refs #(entity/refs-generator :order.line)))

(spec/def :schema.type/order
  (spec/keys :req [:order/user
                   :order/status]
             :opt [:order/shipping-comments
                   :order/payment-reference
                   :order/shipping-address
                   :order/billing-address
                   :order/shipping-method
                   :order/payment-method
                   :order/lines]))

(entity/register-type!
 :order
 {:attributes
  (concat
   [{:db/ident :order/user
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}

    {:db/ident :order/status
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}

    {:db/ident :order/shipping-address
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}

    {:db/ident :order/billing-address
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}

    {:db/ident :order/shipping-method
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :ventas/refEntityType :shipping-method}

    {:db/ident :order/payment-method
     :db/valueType :db.type/keyword
     :db/cardinality :db.cardinality/one}

    {:db/ident :order/shipping-comments
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one}

    {:db/ident :order/lines
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/many
     :db/isComponent true}

    {:db/ident :order/payment-reference
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one}

    {:db/ident :order/payment-amount
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/isComponent true}]

   (map #(hash-map :db/ident %) statuses))

  :dependencies
  #{:address :user :amount :shipping-method}

  :serialize
  (fn [this params]
    (-> ((entity/default-attr :serialize) this params)
        (assoc :amount (entity/serialize (get-amount this) params))))

  :deserialize
  (fn [this]
    (-> this
        (dissoc :amount)
        ((entity/default-attr :deserialize))))})

(spec/def :order.line/product-variation
  (spec/with-gen ::entity/ref #(entity/ref-generator :product.variation)))

(spec/def :order.line/quantity (spec/and integer? pos?))

(spec/def :schema.type/order.line
  (spec/keys :req [:order.line/product-variation
                   :order.line/quantity]))

(entity/register-type!
 :order.line
 {:attributes
  [{:db/ident :order.line/product-variation
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.line/quantity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.line/discount
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :autoresolve? true

  :dependencies
  #{:order :product :product.variation :discount}})

(defn get-country-group
  "Gets the country group from the shipping address of the given order"
  [order]
  (let [country (get-in order [:order/shipping-address :address/country])]
    (:country/group country)))