(ns ventas.server.api.admin
  (:require
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]
   [ventas.server.pagination :as pagination]
   [ventas.database :as db]
   [ventas.plugin :as plugin]
   [ventas.entities.image-size :as entities.image-size]))

(defn- admin-check! [session]
  (let [{:user/keys [roles]} (api/get-user session)]
    (when-not (contains? roles :user.role/administrator)
      (throw (Exception. "This API request requires administration privileges")))))

(defn- register-admin-endpoint!
  ([kw f]
    (api/register-endpoint! kw {} f))
  ([kw opts f]
   (api/register-endpoint!
     kw
     opts
     (fn [request {:keys [session] :as state}]
       (admin-check! session)
       (f request state)))))

(register-admin-endpoint!
  :admin.brands.list
  {:middlewares [pagination/wrap-sort
                 pagination/wrap-paginate]}
  (fn [_ {:keys [session]}]
    (map #(entity/to-json % {:culture (api/get-culture session)})
         (entity/query :brand))))

(register-admin-endpoint!
  :admin.entities.remove
  (fn [{:keys [params]} state]
    (entity/delete (:id params))))

(register-admin-endpoint!
 :admin.currencies.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :currency))))

(register-admin-endpoint!
  :admin.taxes.list
  {:middlewares [pagination/wrap-sort
                 pagination/wrap-paginate]}
  (fn [_ {:keys [session]}]
    (map #(entity/to-json % {:culture (api/get-culture session)})
         (entity/query :tax))))

(register-admin-endpoint!
  :admin.taxes.save
  (fn [message state]
    (entity/upsert :tax (-> (:params message)
                            (update :amount float)))))

(register-admin-endpoint!
  :admin.users.list
  {:middlewares [pagination/wrap-sort
                 pagination/wrap-paginate]}
  (fn [_ {:keys [session]}]
    (->> (entity/query :user)
         (map #(entity/to-json % {:culture (api/get-culture session)})))))

(register-admin-endpoint!
 :admin.users.addresses.list
 (fn [{:keys [user]} {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :address {:user user}))))

(register-admin-endpoint!
  :admin.users.save
  (fn [{:keys [params]} state]
    (entity/upsert
      :user
      (-> params
          (update :culture (fn [v]
                             (when v
                               [:i18n.culture/keyword v])))))))

(register-admin-endpoint!
  :admin.image-sizes.list
  {:middlewares [pagination/wrap-sort
                 pagination/wrap-paginate]}
  (fn [_ {:keys [session]}]
    (map #(entity/to-json % {:culture (api/get-culture session)})
         (entity/query :image-size))))

(register-admin-endpoint!
 :admin.image-sizes.entities.list
 (fn [_ _]
   entities.image-size/entities))

(register-admin-endpoint!
  :admin.entities.find
  (fn [{{:keys [id]} :params} _]
    {:pre [(number? id)]}
    (entity/find id)))

(register-admin-endpoint!
 :admin.entities.find-json
 (fn [{{:keys [id]} :params} _]
   {:pre [(number? id)]}
   (entity/find-json id)))

(register-admin-endpoint!
  :admin.events.list
  {:middlewares [pagination/wrap-sort
                 pagination/wrap-paginate]}
  (fn [_ _]
    (->> (db/transaction-log)
         (take-last 10)
         (db/explain-txs)
         (filter :entity-id))))

(register-admin-endpoint!
 :admin.orders.get
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [order (entity/find id)]
     {:order order
      :lines (map #(entity/find-json % {:culture (api/get-culture session)})
                  (:order/lines order))})))

(register-admin-endpoint!
 :admin.orders.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :order))))

(register-admin-endpoint!
 :admin.products.save
 (fn [{:keys [params]} _]
   (entity/upsert* (if (get-in params [:product/price :amount/value])
                     (update-in params [:product/price :amount/value] bigdec)
                     params))))

(register-admin-endpoint!
 :admin.product.terms.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :product.term))))

(register-admin-endpoint!
  :admin.datadmin.datoms
  (fn [message state]
    (let [datoms (db/datoms :eavt)]
      {:datoms (map db/datom->map (take 10 datoms))})))

(register-admin-endpoint!
  :admin.plugins.list
  {:middlewares [pagination/wrap-sort
                 pagination/wrap-paginate]}
  (fn [_ _]
    (->> (plugin/all)
         (map plugin/plugin)
         (map (fn [plugin]
                (select-keys plugin #{:version :name}))))))