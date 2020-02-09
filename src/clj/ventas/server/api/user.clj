(ns ventas.server.api.user
  (:require
   [buddy.hashers :as hashers]
   [slingshot.slingshot :refer [throw+ try+]]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.entities.order :as entities.order]
   [ventas.entities.shipping-method :as entities.shipping-method]
   [ventas.entities.user :as entities.user]
   [ventas.payment-method :as payment-method]
   [ventas.server.api :as api]
   [ventas.server.pagination :as pagination]
   [ventas.server.ws :as ws]))

(defn- user-check! [session]
  (let [{:db/keys [id]} (api/get-user session)]
    (when-not id
      (ws/bad-request! {:type ::authentication-required}))))

(defn register-user-endpoint!
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
 ::users.addresses
 (fn [_ {:keys [session]}]
   (let [user (api/get-user session)]
     (->> (entity/query :address {:user (:db/id user)})
          (map (partial api/serialize-with-session session))))))

(register-user-endpoint!
 ::users.addresses.save
 (fn [{address :params} {:keys [session]}]
   (let [user (api/get-user session)]
     (->> (entity/upsert :address (merge address
                                         {:user (:db/id user)}))
          (api/serialize-with-session session)))))

(register-user-endpoint!
 ::users.save
 (fn [{user :params} {:keys [session]}]
   (entity/update (merge {:id (:db/id (api/get-user session))}
                         (select-keys user #{:first-name :last-name :company :email :phone})))))

(register-user-endpoint!
 ::users.change-password
 (fn [{{:keys [password]} :params} {:keys [session]}]
   (let [{id :db/id} (api/get-user session)]
     (entity/update* {:db/id id
                      :user/password (hashers/derive password)}))))

(register-user-endpoint!
 ::users.addresses.remove
 {:spec {:id ::api/ref}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [user (api/get-user session)
         address (entity/find id)]
     (when-not (= (:db/id user) (:address/user address))
       (ws/bad-request! {:type ::entity-update-unauthorized
                         :entity-type :address}))
     (entity/delete id)
     true)))

(register-user-endpoint!
 ::users.cart.get
 (fn [_ {:keys [session]}]
   (when-let [user (api/get-user session)]
     (let [cart (entities.user/get-cart user)]
       (api/serialize-with-session session cart)))))

;; @TODO Test and finish this
(register-user-endpoint!
 ::users.cart.shipping-methods
 (fn [_ {:keys [session]}]
   (when-let [user (api/get-user session)]
     (let [cart (entities.user/get-cart user)
           amount (entities.order/get-amount cart)
           group (entities.order/get-country-group cart)
           shipping-methods (entity/query :shipping-method)]
       (->> shipping-methods
            (map (fn [method]
                   (assoc method :amount (entities.shipping-method/get-amount method
                                                                              group
                                                                              (:amount/value amount)))))
            (map (partial api/serialize-with-session session)))))))

(register-user-endpoint!
 ::users.cart.payment-methods
 {:doc "Gets the payment methods that apply to the current cart
        (For now, that means all the payment methods)"}
 (fn [_ {:keys [session]}]
   (let [methods (payment-method/all)]
     (common.utils/map-vals
      (fn [{:keys [name]}]
        {:name (if (string? name)
                 name
                 (api/serialize-with-session session name))})
      methods))))

(defn- find-order-line [order product-variation]
  (when-let [id (db/nice-query-attr
                 {:find '[?id]
                  :in {'?order order
                       '?variation product-variation}
                  :where '[[?order :order/lines ?id]
                           [?id :order.line/product-variation ?variation]]})]
    (entity/find id)))

(register-user-endpoint!
 ::users.cart.add
 {:spec {:id ::api/ref}}
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
       (api/find-serialize-with-session session (:db/id cart))))))

(register-user-endpoint!
 ::users.cart.remove
 {:spec {:id ::api/ref}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (when-let [user (api/get-user session)]
     (let [cart (entities.user/get-cart user)]
       (when-let [line (find-order-line (:db/id cart) id)]
         (entity/delete (:db/id line)))
       (api/find-serialize-with-session session (:db/id cart))))))

(register-user-endpoint!
 ::users.cart.set-quantity
 {:spec {:id ::api/ref
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
       (api/find-serialize-with-session session (:db/id cart))))))

(register-user-endpoint!
 ::users.cart.add-discount
 {:spec {:code string?}}
 (fn [{{:keys [code]} :params} {:keys [session]}]
   (when-let [user (api/get-user session)]
     (if-let [discount (entity/query-one :discount {:code code})]
       (let [cart (entities.user/get-cart user)]
         (entity/update* (assoc cart :discount (:db/id discount)))
         (api/find-serialize-with-session session (:db/id cart)))
       (ws/bad-request! {:type ::discount-not-found
                         :code code})))))

(register-user-endpoint!
 ::users.cart.order
 {:doc "Makes an order from the current cart"}
 (fn [{{:keys [email shipping-address shipping-method payment-method payment-params]} :params} {:keys [session]}]
   (let [user (api/get-user session)
         cart (entities.user/get-cart user)]
     (when-not cart
       (ws/bad-request! {:type ::cart-not-found}))
     (when (= (:user/status user) :user.status/unregistered)
       (entity/update* (assoc user :user/email email)))
     (try+
       (let [shipping-address (if-not (number? shipping-address)
                                (:db/id (entity/upsert :address (merge shipping-address
                                                                       {:user (:db/id user)})))
                                shipping-address)
             cart             (entity/update*
                               (merge cart {:order/shipping-address shipping-address
                                            :order/shipping-method  shipping-method
                                            :order/payment-method   payment-method}))]
         (payment-method/pay! cart payment-params))
       (catch [:type ::payment-method/payment-method-not-found] {:keys [type]}
         (ws/bad-request! {:type type}))))))

(register-user-endpoint!
 ::users.orders.list
 {:doc "Lists the orders of an user (excluding drafts)"}
 (fn [_ {:keys [session]}]
   (let [user (api/get-user-id session)]
     (->> (entity/query :order {:user user})
          (remove (fn [order] (= (:order/status order) :order.status/draft)))
          (map #(let [order (api/serialize-with-session session %)]
                  (merge order (entity/dates (:db/id %)))))))))

(register-user-endpoint!
 ::users.favorites.enumerate
 (fn [_ {:keys [session]}]
   (let [user (api/get-user session)]
     (:user/favorites user))))

(register-user-endpoint!
 ::users.favorites.list
 {:middleware [pagination/wrap-sort
               pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (let [user (api/get-user session)]
     (->> (:user/favorites user)
          (map (partial api/find-serialize-with-session session))))))

(defn- toggle-favorite [session product-id f]
  (when-let [user (api/get-user session)]
    (let [favorites (f (set (:user/favorites user)) product-id)]
      (entity/update* {:db/id (:db/id user)
                       :user/favorites favorites})
      favorites)))

(register-user-endpoint!
 ::users.favorites.add
 {:spec {:id ::api/ref}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (toggle-favorite session id conj)))

(register-user-endpoint!
 ::users.favorites.remove
 {:spec {:id ::api/ref}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (toggle-favorite session id disj)))
