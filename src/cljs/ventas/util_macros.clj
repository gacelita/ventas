(ns ventas.util-macros)

(defmacro swap-input-value! [where name e]
  `(~'swap! ~where ~'assoc ~name (~'-> ~e .-target .-value)))

(defmacro require-pages []
  `(do ~@(map (fn [a] `(~'require (~'quote [~(symbol (str a))])))
              (filter #(.contains (str %) "ventas.pages") (all-ns)))))

(defmacro require-plugins []
  `(do ~@(map (fn [a] `(~'require (~'quote [~(symbol (str a))])))
              (filter #(.contains (str %) "ventas.plugins") (all-ns)))))