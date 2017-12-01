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
         {:keys [level ?err msg_ ?ns-str ?file ?line]} data]
     (merge
      {:level (str/upper-case (name level))
       :where (str "[" (or ?ns-str ?file "?") ":" (or ?line "?") "]")
       :what (read-string (force msg_))}
      (when-not no-stacktrace?
        (when ?err
          {:stacktrace (timbre/stacktrace ?err opts)}))))))

(defn- timbre-appender-fn [{:keys [output_]}]
  (let [{:keys [level where what]} (force output_)
        info-line (str level " " where " - ")]
    (print (clansi/green info-line))
    (-> (with-out-str (pprint/pprint what))
        (str/replace "\n" (str "\n" (str/join (repeat (count info-line) " "))))
        (str/trimr)
        (println))))

(timbre/merge-config!
 {:level :debug
  :output-fn timbre-logger
  :appenders {:println {:enabled? false}
              :pprint-appender {:enabled?   true
                                :async?     false
                                :min-level  nil
                                :rate-limit [[1 250] [10 5000]]
                                :output-fn  :inherit
                                :fn timbre-appender-fn}}})