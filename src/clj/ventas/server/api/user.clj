(ns ventas.server.api.user
  (:require
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]
   [ventas.entities.user :as entities.user]
   [ventas.database :as db]
   [clojure.string :as str]
   [clojure.spec.alpha :as spec]))

(defn- user-check! [session]
  (let [{:db/keys [id]} (api/get-user session)]
    (when-not id
      (throw (Exception. "This API request requires authentication")))))

(defn- register-user-endpoint!
  ([kw f]
   (register-user-endpoint! kw {} f))
  ([kw opts f]
   (api/register-endpoint!
    kw
    opts
    (fn [request {:keys [session] :as state}]
      (user-check! session)
      (f request state)))))

(register-user-endpoint!
 :users.addresses
 (fn [_ {:keys [session] :as state}]
   (let [user (api/get-user session)]
     (->> (entity/query :address {:user (:db/id user)})
          (map #(entity/to-json % {:culture (api/get-culture session)}))))))

(register-user-endpoint!
 :users.addresses.save
 {:spec {:id ::api/id}}
 (fn [{{:keys [id] :as address} :params} {:keys [session] :as state}]
   (let [user (api/get-user session)
         address (entity/find id)]
     (when-not (= (:db/id user) (:address/user address))
       (throw (Exception. "Unauthorized")))
     (entity/upsert :address address))))

(register-user-endpoint!
 :users.addresses.remove
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [user (api/get-user session)
         address (entity/find id)]
     (when-not (= (:db/id user) (:address/user address))
       (throw (Exception. "Unauthorized")))
     (entity/delete id))))

(register-user-endpoint!
 :users.cart.get
 (fn [_ {:keys [session]}]
   (when-let [user (api/get-user session)]
     (let [cart (entities.user/get-cart user)]
       (entity/to-json cart {:culture (api/get-culture session)})))))

(defn- find-order-line [order product-variation]
  (when-let [id (-> (db/nice-query
                     {:find '[?id]
                      :in {'?order order
                           '?variation product-variation}
                      :where '[[?order :order/lines ?id]
                               [?id :order.line/product-variation ?variation]]})
                    first
                    :id)]
    (entity/find id)))

(register-user-endpoint!
 :users.cart.add
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (when-let [user (api/get-user session)]
     (let [cart (entities.user/get-cart user)]
       (if-let [line (find-order-line (:db/id cart) id)]
         (entity/update* (update line :order.line/quantity inc))
         (entity/update* {:db/id (:db/id cart)
                          :order/lines {:schema/type :schema.type/order.line
                                        :order.line/product-variation id
                                        :order.line/quantity 1}}
                         :append? true))
       (entity/find-json (:db/id cart) {:culture (api/get-culture session)})))))

(register-user-endpoint!
 :users.cart.remove
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (when-let [user (api/get-user session)]
     (let [cart (entities.user/get-cart user)]
       (when-let [line (find-order-line (:db/id cart) id)]
         (entity/delete (:db/id line)))
       (entity/find-json (:db/id cart) {:culture (api/get-culture session)})))))

(register-user-endpoint!
 :users.cart.set-quantity
 {:spec {:id ::api/id
         :quantity number?}}
 (fn [{{:keys [id quantity]} :params} {:keys [session]}]
   (when-let [user (api/get-user session)]
     (let [cart (entities.user/get-cart user)]
       (if-let [line (find-order-line (:db/id cart) id)]
         (entity/update* (assoc line :order.line/quantity quantity))
         (entity/update* {:db/id (:db/id cart)
                          :order/lines {:schema/type :schema.type/order.line
                                        :order.line/product-variation id
                                        :order.line/quantity quantity}}))
       (entity/find-json (:db/id cart) {:culture (api/get-culture session)})))))

(register-user-endpoint!
 :users.cart.add-discount
 {:spec {:code string?}}
 (fn [{{:keys [code]} :params} {:keys [session]}]
   (when-let [user (api/get-user session)]
     (if-let [discount (entity/query-one :discount {:code code})]
       (let [cart (entities.user/get-cart user)]
         (entity/update* (assoc cart :discount (:db/id discount)))
         (entity/find-json (:db/id cart) {:culture (api/get-culture session)}))
       (throw (Exception. "No discount found with the given code"))))))

(register-user-endpoint!
 :users.favorites.list
 (fn [_ {:keys [session]}]
   (let [user (api/get-user session)]
     (:user/favorites user))))

(defn- toggle-favorite [session product-id f]
  (when-let [user (api/get-user session)]
    (let [favorites (f (set (:user/favorites user)) product-id)]
      (entity/update* {:db/id (:db/id user)
                       :user/favorites favorites})
      favorites)))

(register-user-endpoint!
 :users.favorites.add
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (toggle-favorite session id conj)))

(register-user-endpoint!
 :users.favorites.remove
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (toggle-favorite session id disj)))
