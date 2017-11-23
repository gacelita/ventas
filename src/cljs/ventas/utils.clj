(ns ventas.utils)

(defmacro ns-kw
  "Takes a string or a keyword. Returns a keyword where the ns is the caller ns
   and the name is the given string, or the name of the given keyword."
  [input]
  (let [caller-ns (str (:name (:ns &env)))]
    `(~'keyword ~caller-ns ~input)))
