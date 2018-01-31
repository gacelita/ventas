(ns ventas.database.generators
  "More user-friendly generators"
  (:require
   [clojure.test.check.generators :as gen]
   [clojure.string :as str]
   [clojure.spec.alpha :as spec]
   [ventas.utils :as utils]))

(defn string-generator []
  (gen/fmap str/join
            (gen/vector gen/char-alphanumeric 2 10)))

(defn keyword-generator []
  (gen/fmap #(keyword (str/lower-case %))
            (string-generator)))

(spec/def ::string
  (spec/with-gen string? string-generator))

(spec/def ::keyword
  (spec/with-gen keyword? keyword-generator))

(spec/def ::bigdec
  (spec/with-gen
    (spec/and utils/bigdec? pos?)
    (fn []
      (gen/fmap #(-> % (str) (BigDecimal.))
                (gen/double* {:NaN? false :min 0 :max 999})))))