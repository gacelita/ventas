(ns ventas.auth
  (:require
   [buddy.sign.jwt :as buddy.jwt]
   [ventas.config :as config]
   [ventas.database.entity :as entity]
   [ventas.utils :as utils]))

(defn user->token [user]
  (buddy.jwt/sign {:user-id (:db/id user)} (config/get :auth-secret)))

(defn- unsign [token secret]
  (utils/swallow
   (buddy.jwt/unsign token secret)))

(defn token->user [token]
  (when-let [{:keys [user-id]} (unsign token (config/get :auth-secret))]
    (entity/find user-id)))
