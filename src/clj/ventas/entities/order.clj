(ns ventas.entities.order
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]))

#_"
  Orders have:
  - An user
  - A status
  - Billing and shipping addresses
  - A list of products with their quantities (order lines)
  - A shipping method (maybe with comments)
  - A payment method
"

(spec/def :order/user
  (spec/with-gen ::entity/ref #(entity/ref-generator :user)))

(spec/def :order/status
  #{:order.status/unpaid
    :order.status/paid
    :order.status/acknowledged
    :order.status/ready
    :order.status/shipped
    :order.status/draft})

(spec/def :order/shipping-address
  (spec/with-gen ::entity/ref #(entity/ref-generator :address)))

(spec/def :order/billing-address
  (spec/with-gen ::entity/ref #(entity/ref-generator :address)))

(spec/def :order/shipping-method ::generators/keyword)
(spec/def :order/shipping-comments ::generators/string)

(spec/def :order/payment-method ::generators/keyword)
(spec/def :order/payment-reference ::generators/string)

(spec/def :schema.type/order
  (spec/keys :req [:order/user
                   :order/status]
             :opt [:order/shipping-comments
                   :order/payment-reference
                   :order/shipping-address
                   :order/billing-address
                   :order/shipping-method
                   :order/payment-method]))

(entity/register-type!
 :order
 {:attributes
  [{:db/ident :order/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/status
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.status/draft}
   {:db/ident :order.status/unpaid}
   {:db/ident :order.status/paid}
   {:db/ident :order.status/acknowledged}
   {:db/ident :order.status/ready}
   {:db/ident :order.status/shipped}

   {:db/ident :order/shipping-address
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/billing-address
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/shipping-method
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/payment-method
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/shipping-comments
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/payment-reference
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:address :user}

  :to-json
  (fn [this params]
    (-> ((entity/default-attr :to-json) this params)
        (assoc :lines (->> (entity/query :order.line {:order (:db/id this)})
                           (map #(entity/to-json (dissoc % :order.line/order) params))))))})


(spec/def :order.line/order
  (spec/with-gen ::entity/ref #(entity/ref-generator :order)))

(spec/def :order.line/product-variation
  (spec/with-gen ::entity/ref #(entity/ref-generator :product.variation)))

(spec/def :order.line/quantity (spec/and integer? pos?))

(spec/def :schema.type/order.line
  (spec/keys :req [:order.line/order
                   :order.line/product-variation
                   :order.line/quantity]))

(entity/register-type!
 :order.line
 {:attributes
  [{:db/ident :order.line/order
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.line/product-variation
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.line/quantity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.line/discount
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:order :product :product.variation :discount}})