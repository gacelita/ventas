(ns ventas.util-macros)

(defmacro require-pages []
  `(do ~@(map (fn [a] `(~'require (~'quote [~(symbol (str a))])))
              (filter #(.contains (str %) "ventas.pages") (all-ns)))))

(defmacro require-plugins []
  `(do ~@(map (fn [a] `(~'require (~'quote [~(symbol (str a))])))
              (filter #(.contains (str %) "ventas.plugins") (all-ns)))))