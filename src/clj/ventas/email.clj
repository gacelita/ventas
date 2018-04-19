(ns ventas.email
  (:require
   [postal.core :as postal]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.email.templates.order-done]
   [ventas.email.templates :as templates]
   [hiccup.core :as hiccup]
   [ventas.i18n :refer [i18n]]))

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

(defn send-template!
  [template email-opts extra-args]
  (send! (merge email-opts
                {:body [{:type "text/html; charset=utf-8"
                         :content (hiccup/html (templates/template-body template extra-args))}]})))