(ns ventas.server.api.admin
  (:require
    [byte-streams :as byte-streams]
    [ventas.server.api :as api]
    [ventas.database.entity :as entity]
    [ventas.server.pagination :as pagination]
    [ventas.database :as db]
    [ventas.paths :as paths]
    [ventas.entities.file :as entities.file]
    [ventas.plugin :as plugin]))

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
  :admin.entities.find
  (fn [{{:keys [id]} :params} _]
    (entity/find (Long. id))))

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
  :admin.products.save
  (fn [message state]
    (entity/upsert :product
                   (-> (:params message)
                       (update-in [:price :value] bigdec)))))

(register-admin-endpoint!
  :admin.datadmin.datoms
  (fn [message state]
    (let [datoms (db/datoms :eavt)]
      {:datoms (map db/datom->map (take 10 datoms))})))

(register-admin-endpoint!
  :admin.upload
  {:binary? true}
  (fn [{:keys [params]} state]
    (let [{:keys [bytes is-first is-last file-id]} params
          file-id (if is-first (gensym "temp-file") file-id)
          path (str (paths/resolve paths/storage) "/" file-id)]
      (with-open [r (byte-streams/to-input-stream bytes)
                  w (-> (clojure.java.io/file path)
                        (clojure.java.io/output-stream :append (not is-first)))]
        (clojure.java.io/copy r w))
      (cond
        is-last (entities.file/create-from-file! path)
        is-first file-id
        :default true))))

(register-admin-endpoint!
  :admin.plugins.list
  {:middlewares [pagination/wrap-sort
                 pagination/wrap-paginate]}
  (fn [_ _]
    (->> (plugin/all)
         (map plugin/plugin)
         (map (fn [plugin]
                (select-keys plugin #{:version :name}))))))