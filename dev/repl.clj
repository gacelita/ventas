(ns repl
  "Basically ventas-devtools.repl"
  (:require
   [clojure.tools.namespace.repl :as tn]
   [ventas.config]))

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

(cond->> ["src/clj" "src/cljc" "test/clj" "test/cljc"]
         (not (ventas.config/get :strict-classloading)) (concat ["dev"])
         true (apply clojure.tools.namespace.repl/set-refresh-dirs))

(defn init []
  (let [result (tn/refresh-all)]
    (when (instance? Throwable result)
      (throw result))
    (immigrate 'ventas-devtools.repl)
    ((ns-resolve 'ventas-devtools.repl 'init))))