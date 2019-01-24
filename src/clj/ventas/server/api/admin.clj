(ns ventas.server.api.admin
  (:require
   [clojure.core.async :refer [<! <!! >! chan go go-loop]]
   [slingshot.slingshot :refer [throw+]]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.entities.order :as entities.order]
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.search.entities :as search.entities]
   [ventas.server.pagination :as pagination]
   [ventas.utils :as utils]))

(defn- admin-check! [session]
  (let [{:user/keys [roles]} (api/get-user session)]
    (when-not (contains? roles :user.role/administrator)
      (throw+ {:type ::unauthorized}))))

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
 :admin.entities.find-serialize
 {:spec {:id ::api/ref}
  :doc "Returns a serialized entity. Should be used for read-only access to
        entity data."}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (api/find-serialize-with-session session id)))

(register-admin-endpoint!
 :admin.entities.pull
 {:spec {:id ::api/ref}
  :doc "Returns an entity by using the pull API.
        This is the preferred way of getting entities in the administration."}
 (fn [{{:keys [id expr]} :params} _]
   (db/pull (or expr '[*]) id)))

(register-admin-endpoint!
 :admin.entities.save
 {:spec ::entity/entity
  :doc "Saves an entity, updating it if it already exists.
        This is the preferred way of saving entities in the administration."}
 (fn [{entity :params} _]
   (entity/upsert* entity)))

(register-admin-endpoint!
 :admin.entities.remove
 {:spec {:id ::api/ref}
  :doc "Removes the given entity."}
 (fn [{{:keys [id]} :params} _]
   (entity/delete id)))

(register-admin-endpoint!
 :admin.entities.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]
  :spec {:type keyword?}
  :doc "Returns a coll of serialized entities with the given type."}
 (fn [{{:keys [type filters]} :params} {:keys [session]}]
   (->> (entity/query type filters)
        (map (partial api/serialize-with-session session)))))

(register-admin-endpoint!
 :admin.users.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (->> (entity/query :user)
        (filter (fn [{:user/keys [status]}]
                  (not= status :user.status/unregistered)))
        (map #(merge (api/serialize-with-session session %)
                     (entity/dates (:db/id %)))))))

(register-admin-endpoint!
 :admin.orders.list-pending
 {:doc "Returns a coll of orders with these statuses:
        - acknowledged
        - paid
        - unpaid
        - ready"}
 (fn [_ {:keys [session]}]
   (->> (db/nice-query {:find '[?id]
                        :where '[[?id :schema/type :schema.type/order]
                                 (or [?id :order/status :order.status/unpaid]
                                     [?id :order/status :order.status/paid]
                                     [?id :order/status :order.status/acknowledged]
                                     [?id :order/status :order.status/ready])]})
        (into []
              (comp (map (comp entity/find :id))
                    (map (fn [entity]
                           (->> (merge entity (entity/dates (:db/id entity)))
                                (api/serialize-with-session session)))))))))

(register-admin-endpoint!
 :admin.image-sizes.entities.list
 (fn [_ _]
   (->> entities.image-size/entities
        (map (comp utils/dequalify-keywords db/touch-eid)))))

(register-admin-endpoint!
 :admin.events.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]
  :doc "Returns a coll of Datomic transactions. Meant for the activity log section."}
 (fn [_ _]
   (->> (db/transaction-log)
        (take-last 10)
        (db/explain-txs)
        (filter :entity-id))))

(register-admin-endpoint!
 :admin.orders.get
 {:spec {:id ::api/ref}
  :doc "Like doing admin.entities.pull on an order, but also returns the lines
        of the order, serialized."}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [{:order/keys [lines shipping-method shipping-address]} (entity/find id)]
     {:order (db/pull '[*] id)
      :status-history (entities.order/status-history id)
      :shipping {:method (api/find-serialize-with-session session shipping-method)
                 :address (api/find-serialize-with-session session shipping-address)}
      :lines (map (partial api/find-serialize-with-session session)
                  lines)})))

(register-admin-endpoint!
 :admin.search
 {:spec {:search string?
         :attrs #{keyword?}}
  :doc "Does a fulltext search for `search` in the given `attrs`"}
 (fn [{{:keys [search attrs]} :params} {:keys [session]}]
   (let [culture (api/get-culture session)]
     (search.entities/search search attrs culture))))

(register-admin-endpoint!
 :admin.plugins.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]
  :doc "Returns the list of registered plugins."}
 (fn [_ {:keys [session]}]
   (->> (plugin/all)
        (map (fn [[id plugin]]
               (-> plugin
                   (select-keys #{:name :type})
                   (update :name #(if (string? %)
                                    %
                                    (api/serialize-with-session session %)))
                   (assoc :id id)))))))

(register-admin-endpoint!
 :admin.configuration.set
 {:doc "Sets the given configuration key to the given value."}
 (fn [{config :params} _]
   (doseq [[k v] config]
     (entities.configuration/set! k v))))