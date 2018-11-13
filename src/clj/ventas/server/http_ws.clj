(ns ventas.server.http-ws
  "Exposes the WS api through an HTTP endpoint."
  (:require
   [ventas.server.ws :as server.ws]
   [ventas.server.api :as server.api]
   [ventas.server.api.user :as server.api.user]
   [ventas.utils :as utils]
   [cheshire.core :as cheshire]
   [slingshot.slingshot :refer [throw+]]
   [ventas.auth :as auth]))

(defn- response [content-type res]
  (merge res
         {:headers {"Content-Type" content-type}
          :body (case content-type
                  "application/json" (cheshire/encode (:body res))
                  "application/edn" (pr-str (:body res))
                  (throw+ {:type ::unsupported-content-type
                           :message (str "Only application/json or application/edn are allowed")}))}))

(defn handle [{:keys [server-name edn-params query-params route-params body content-type]}]
  (let [session (atom {})
        body {:name (keyword (:name route-params))
              :params (or edn-params body)}]
    (when-let [token (get query-params "token")]
      (when-let [user (auth/token->user token)]
        (server.api/set-user session user)))
    (try
      (response
       content-type
       {:status 200
        :body (server.ws/handle-request body
                                        {:session session})})
      (catch Throwable e
        (let [type (:type (ex-data e))
              content-type (if (= type ::unsupported-content-type)
                             "application/edn"
                             content-type)]
          (response
           content-type
           {:status (case type
                      ::server.ws/api-call-not-found 404
                      ::server.api.user/authentication-required 401
                      ::utils/spec-invalid 400
                      ::unsupported-content-type 400
                      500)
            :body (server.ws/exception->message e)}))))))