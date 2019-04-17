(ns ventas.email
  (:require
   [hiccup.core :as hiccup]
   [postal.core :as postal]
   [ventas.email.templates :as templates]
   [ventas.database.generators :as generators]
   [ventas.i18n :refer [i18n]]
   [ventas.config :as config]
   [ventas.common.utils :refer [remove-nil-vals]]
   [ventas.database.entity :as entity]
   [clojure.spec.alpha :as spec]
   [clojure.tools.logging :as log]))

(spec/def :email-config/encryption-enabled? boolean?)
(spec/def :email-config/encryption-type (entity/enum-spec #{:email-config.encryption-type/tls
                                                            :email-config.encryption-type/ssl}))
(spec/def :email-config/smtp-host ::generators/string)
(spec/def :email-config/smtp-port number?)
(spec/def :email-config/smtp-user ::generators/string)
(spec/def :email-config/smtp-password ::generators/string)
(spec/def :email-config/from-address ::generators/string)

(spec/def :schema.type/email-config
  (spec/keys :req [:email-config/encryption-enabled?
                   :email-config/encryption-type
                   :email-config/smtp-host
                   :email-config/smtp-port
                   :email-config/smtp-user
                   :email-config/smtp-password
                   :email-config/from-address]))

(entity/register-type!
 :email-config
 {:migrations
  [[:base [{:db/ident :email-config/encryption-enabled?
            :db/valueType :db.type/boolean
            :db/cardinality :db.cardinality/one}
           {:db/ident :email-config/encryption-type
            :db/valueType :db.type/ref
            :ventas/refEntityType :enum
            :db/cardinality :db.cardinality/one}
           {:db/ident :email-config.encryption-type/tls}
           {:db/ident :email-config.encryption-type/ssl}
           {:db/ident :email-config/smtp-host
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}
           {:db/ident :email-config/smtp-port
            :db/valueType :db.type/long
            :db/cardinality :db.cardinality/one}
           {:db/ident :email-config/smtp-user
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}
           {:db/ident :email-config/smtp-password
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}
           {:db/ident :email-config/from-address
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}]]]})

(defn save-config! [config]
  (entity/upsert*
   (ventas.common.utils/remove-nil-vals
    {:db/ident :email-config
     :schema/type :schema.type/email-config
     :email-config/smtp-host (:host config)
     :email-config/smtp-port (:port config)
     :email-config/smtp-user (:user config)
     :email-config/smtp-password (:pass config)
     :email-config/from-address (:from config)
     :email-config/encryption-enabled? (or (:ssl config) (:tls config))
     :email-config/encryption-type (cond (:ssl config) :email-config.encryption-type/ssl
                                         (:tls config) :email-config.encryption-type/tls)})))

(defn- get-config-from-db []
  (let [entity (entity/find :email-config)
        encryption? (:email-config/encryption-enabled? entity)
        encryption-type (:email-config/encryption-type entity)]
    (remove-nil-vals
     {:host (:email-config/smtp-host entity)
      :port (:email-config/smtp-port entity)
      :user (:email-config/smtp-user entity)
      :pass (:email-config/smtp-password entity)
      :from (:email-config/from-address entity)
      :ssl (and encryption? (= :email-config.encryption-type/ssl encryption-type))
      :tls (and encryption? (= :email-config.encryption-type/tls encryption-type))})))

(defn- get-config-from-env []
  (let [encryption? (config/get :email :encryption :enabled)
        encryption-type (config/get :email :encryption :type)]
    (remove-nil-vals
     {:host (config/get :email :smtp :host)
      :port (config/get :email :smtp :port)
      :user (config/get :email :smtp :user)
      :pass (config/get :email :smtp :password)
      :from (config/get :email :from)
      :ssl (and encryption? (= "ssl" encryption-type))
      :tls (and encryption? (= "tls" encryption-type))})))

(defn get-config []
  (merge (get-config-from-env)
         (get-config-from-db)))

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
    (when from
      (log/info "Sending email to" (:to args))
      (postal/send-message config
                           (merge args {:from from})))))

(defn send-template!
  [template {:keys [user] :as template-args}]
  (let [{:keys [subject body reply-to]} (templates/template template template-args)]
    (send! (cond-> {:to (:user/email user)
                    :subject subject
                    :body [{:type "text/html; charset=utf-8"
                            :content (hiccup/html body)}]}
                   reply-to (assoc :reply-to reply-to)))))
