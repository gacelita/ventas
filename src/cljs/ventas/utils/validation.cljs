(ns ventas.utils.validation
  (:require
   [clojure.string :as str]))

(defn length-validator [{:keys [min max] :or {min -1}} value]
  (let [length (count value)
        max (or max (inc (count value)))]
    (< min length max)))

(defn email-validator [_ value]
  (or (empty? value)
      (str/includes? value "@")))

(defn required-validator [_ value]
  (if (or (string? value) (coll? value))
    (not (empty? value))
    value))

(defn validate [field-validators field value]
  (let [validators (get field-validators field)
        results (map (fn [[identifier validation-fn params]]
                       {:identifier identifier
                        :params params
                        :valid? (validation-fn params value)})
                     validators)]
    {:valid? (every? identity (map :valid? results))
     :infractions (->> results
                       (map (fn [{:keys [identifier valid? params]}]
                              (when-not valid?
                                [identifier params])))
                       (remove (fn [[k v]] (nil? v)))
                       (into {}))}))

