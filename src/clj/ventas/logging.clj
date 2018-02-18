(ns ventas.logging
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [io.aviso.ansi :as clansi]
   [taoensso.timbre :as timbre]))

(defn- try-parsing [data]
  (try
    (let [clj (read-string (str "[" data "]"))]
      (if (seq (filter symbol? clj))
        data
        clj))
    (catch Throwable _
      data)))

(defn- timbre-logger
  ([data]
   (timbre-logger nil data))
  ([opts data]
   (let [{:keys [no-stacktrace?]} opts
         {:keys [level ?err msg_ ?ns-str ?file ?line]} data]
     (merge
      {:level (str/upper-case (name level))
       :where (str "[" (or ?ns-str ?file "?") ":" (or ?line "?") "]")
       :value (try-parsing (force msg_))}
      (when-not no-stacktrace?
        (when ?err
          {:stacktrace (timbre/stacktrace ?err opts)}))))))

(defn- pad-lines [multiline-str n]
  (str
   (-> multiline-str
       (str/replace "\n" (str "\n" (str/join (repeat n " "))))
       (str/trimr))))

(defn- timbre-appender-fn [{:keys [output_]}]
  (let [{:keys [level where value]} (force output_)
        info-str (str level " " where " - ")]
    (when (if (string? value)
            (not (str/blank? value))
            value)
      (println
       (str
        (case level
          "ERROR" (clansi/red info-str)
          (clansi/green info-str))
        (let [pad-n (count info-str)]
          (if (string? value)
            (pad-lines value pad-n)
            (reduce (fn [out [idx itm]]
                      (str out
                           (pad-lines (with-out-str (pprint/pprint itm))
                                      pad-n)
                           (when (not= (dec (count value)) idx)
                             (str "\n"
                                  (str/join (repeat pad-n " "))))))
                    ""
                    (map-indexed vector value)))))))))

(timbre/merge-config!
 {:level :debug
  :output-fn timbre-logger
  :ns-blacklist ["datomic.*"
                 "io.netty.*"
                 "org.apache.http.*"
                 "org.eclipse.aether.internal.impl.*"
                 "org.apache.kafka.clients.NetworkClient"
                 "org.apache.kafka.clients.consumer.internals.*"]
  :appenders {:println {:enabled? false}
              :pprint-appender {:enabled?   true
                                :async?     false
                                :min-level  nil
                                :rate-limit [[1 250] [10 5000]]
                                :output-fn  :inherit
                                :fn timbre-appender-fn}}})
