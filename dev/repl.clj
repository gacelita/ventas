(ns repl
  "Basically ventas-devtools.repl"
  (:require [clojure.tools.namespace.repl :as tn]))

(defn immigrate
  "Copies all vars from `from-ns` to the current ns.
   See https://stackoverflow.com/a/15617624/5923031"
  [from-ns]
  (require from-ns)
  (doseq [[sym v] (ns-publics (find-ns from-ns))]
    (let [target (if (bound? v)
                   (intern *ns* sym (var-get v))
                   (intern *ns* sym))]
      (->>
       (select-keys (meta target) [:name :ns])
       (merge (meta v))
       (with-meta '~target)))))

(defn init []
  (let [result (tn/refresh-all)]
    (when (instance? Throwable result)
      (throw result))
    (immigrate 'ventas-devtools.repl)
    ((ns-resolve 'ventas-devtools.repl 'init))))

