(ns ventas.logging
  (:require
   [clojure.string :as str]
   [taoensso.timbre :as timbre]
   [clojure.pprint :as pprint]
   [io.aviso.ansi :as clansi]))

(defn- timbre-logger
  ([data]
   (timbre-logger nil data))
  ([opts data]
   (let [{:keys [no-stacktrace?]} opts
         {:keys [level ?err msg_ ?ns-str ?file ?line]} data
         whats (try
                 (read-string (str "[" (force msg_) "]"))
                 (catch Exception e
                   (force msg_)))
         whats (if (every? identity (map #(or (symbol? %)
                                              (boolean? %)
                                              (string? %)
                                              (keyword? %)
                                              (number? %)) whats))
                 (apply str (interpose " " whats))
                 whats)]
     (merge
      {:level (str/upper-case (name level))
       :where (str "[" (or ?ns-str ?file "?") ":" (or ?line "?") "]")
       :whats whats}
      (when-not no-stacktrace?
        (when ?err
          {:stacktrace (timbre/stacktrace ?err opts)}))))))

(defn- timbre-appender-fn [{:keys [output_]}]
  (let [{:keys [level where whats]} (force output_)
        info-line (str level " " where " - ")]
    (print (case level
             "ERROR" (clansi/red info-line)
             (clansi/green info-line)))
    (if (string? whats)
      (println whats)
      (doseq [[idx what] (map-indexed vector whats)]
        (-> (with-out-str (pprint/pprint what))
            (str/replace "\n" (str "\n" (str/join (repeat (count info-line) " "))))
            (str/trimr)
            (println))
        (when (not= (dec (count whats)) idx)
          (print (str/join (repeat (count info-line) " "))))))))

(timbre/merge-config!
 {:level :debug
  :output-fn timbre-logger
  :ns-blacklist ["datomic.*" "io.netty.*" "org.apache.http.*"]
  :appenders {:println {:enabled? false}
              :pprint-appender {:enabled?   true
                                :async?     false
                                :min-level  nil
                                :rate-limit [[1 250] [10 5000]]
                                :output-fn  :inherit
                                :fn timbre-appender-fn}}})