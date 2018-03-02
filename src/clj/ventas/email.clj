(ns ventas.email
  (:require
   [postal.core :as postal]
   [ventas.entities.configuration :as entities.configuration]
   [clojure.java.io :as io]
   [hiccup.core :as hiccup]
   [ventas.i18n :refer [i18n]]
   [ventas.config :as config]))

(defn- get-config []
  (let [encryption? (entities.configuration/get :email.encryption.enabled)
        encryption-type (entities.configuration/get :email.encryption.type)]
    {:host (entities.configuration/get :email.smtp.host)
     :port (entities.configuration/get :email.smtp.port)
     :user (entities.configuration/get :email.smtp.user)
     :pass (entities.configuration/get :email.smtp.password)
     :from (entities.configuration/get :email.from)
     :ssl (and encryption? (= "ssl" encryption-type))
     :tls (and encryption? (= "tls" encryption-type))}))

(defn send! [args]
  (let [{:keys [from] :as config} (get-config)]
    (postal/send-message config
                         (merge (dissoc args :template) {:from from}))))

(defn- logo-html []
  (let [{:keys [host port]} (config/get :server)]
    [:img {:src (str "http://" host ":" port "/files/logo")}]))

(defmulti template (fn [{:keys [template]}] template))

(defmethod template :new-pending-order [_]
  (hiccup/html
   [:div
    (logo-html)
    [:p
     (i18n ::new-pending-order)]]))

(defn send-template! [args]
  (send! (merge args
                {:body [{:type "text/html; charset=utf-8"
                         :content (template args)}]})))