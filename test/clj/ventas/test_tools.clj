(ns ventas.test-tools
  (:require
   [datomic.api :as d]
   [ventas.database :as db]
   [ventas.database.schema :as schema])
  (:import [ch.qos.logback.classic Logger Level]
           [org.slf4j LoggerFactory]))

(defn create-test-uri [& [id]]
  (str "datomic:mem://" (or id (gensym "test"))))

(defn test-conn [& [id]]
  (let [uri (create-test-uri id)]
    (d/create-database uri)
    (let [c (d/connect uri)]
      (with-redefs [db/conn c]
        (schema/migrate))
      c)))

(defmacro with-test-context [& body]
  `(with-redefs [db/conn (test-conn)]
     (let [~'logger (LoggerFactory/getLogger (Logger/ROOT_LOGGER_NAME))
           ~'log-level (.getLevel ~'logger)]
       (.setLevel ~'logger Level/ERROR)
       ~@body
       (.setLevel ~'logger ~'log-level))))