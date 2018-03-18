(ns ventas.email
  (:require
   [postal.core :as postal]
   [ventas.entities.configuration :as entities.configuration]
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

(defn send!
  "Sends an email.
   `args` will be passed to Postal with the `:from` address specified in the
   configuration.
   Example:
   {:to `test@test.com`
    :cc `another-test@test.com`
    :subject `Hi!`
    :body `Test`}
   See the Postal documentation for more information:
   https://github.com/drewr/postal/blob/master/README.md"
  [args]
  (let [{:keys [from] :as config} (get-config)]
    (postal/send-message config
                         (merge args {:from from}))))

(defn- logo-html []
  (let [{:keys [host port]} (config/get :server)]
    [:img {:src (str "http://" host ":" port "/files/logo")}]))

(defmulti template-body (fn [template _] template))

(defmethod template-body :new-pending-order [_ _]
  (hiccup/html
   [:div
    (logo-html)
    [:p
     (i18n :en_US ::new-pending-order)]]))

(defn send-template!
  [template & [args]]
  (send! (merge args
                {:body [{:type "text/html; charset=utf-8"
                         :content (template-body template args)}]})))