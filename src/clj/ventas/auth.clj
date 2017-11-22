(ns ventas.auth
  (:require
   [buddy.sign.jwt :as buddy.jwt]
   [ventas.config :as config]
   [ventas.database.entity :as entity]))

(defn user->token [user]
  (buddy.jwt/sign {:user-id (:db/id user)} (config/get :auth-secret)))

(defn token->user [token]
  (-> (buddy.jwt/unsign token (config/get :auth-secret))
      :user-id
      (entity/find)))