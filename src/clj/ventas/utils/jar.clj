(ns ventas.utils.jar
  "See https://stackoverflow.com/questions/22363010/get-list-of-embedded-resources-in-uberjar")

(def ^:private running-jar
  "Resolves the path to the current running jar file."
  (-> :keyword class (.. getProtectionDomain getCodeSource getLocation getPath)))

(defn list-resources [& [jar]]
  (let [jar (or jar (java.util.jar.JarFile. running-jar))
        entries (.entries jar)]
    (loop [result  []]
      (if (.hasMoreElements entries)
        (recur (conj result (.. entries nextElement getName)))
        result))))
