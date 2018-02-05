(ns ventas.server.api.admin
  (:require
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]
   [ventas.server.pagination :as pagination]
   [ventas.database :as db]
   [ventas.plugin :as plugin]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.common.utils :as common.utils]))

(defn- admin-check! [session]
  (let [{:user/keys [roles]} (api/get-user session)]
    (when-not (contains? roles :user.role/administrator)
      (throw (Exception. "This API request requires administration privileges")))))

(defn- register-admin-endpoint!
  ([kw f]
   (register-admin-endpoint! kw {} f))
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
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} state]
   (entity/delete id)))

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
 {:spec ::entity/entity}
 (fn [{tax :params} state]
   (entity/upsert :tax (-> tax
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
 {:spec {:user ::entity/entity}}
 (fn [{:keys [user]} {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :address {:user user}))))

(register-admin-endpoint!
 :admin.users.save
 {:spec ::entity/entity}
 (fn [{user :params} state]
   (entity/upsert :user user)))

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
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} _]
   (entity/find id)))

(register-admin-endpoint!
 :admin.entities.find-json
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} _]
   (entity/find-json id)))

(register-admin-endpoint!
 :admin.entities.pull
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} _]
   (db/pull '[*] id)))

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
 {:spec {:id ::api/id}}
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
 {:spec ::entity/entity}
 (fn [{product :params} _]
   (entity/upsert* (-> product
                       (common.utils/update-in-when-some
                        [:product/price :amount/value]
                        bigdec)))))

(register-admin-endpoint!
 :admin.product.terms.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :product.term))))

(register-admin-endpoint!
 :admin.datadmin.datoms
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ _]
   (->> (db/datoms :eavt)
        (map db/datom->map))))

(register-admin-endpoint!
 :admin.plugins.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ _]
   (->> (plugin/all)
        (map plugin/plugin)
        (map (fn [plugin]
               (select-keys plugin #{:version :name}))))))
