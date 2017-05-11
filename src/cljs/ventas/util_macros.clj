(ns ventas.util-macros
  (:require [taoensso.timbre :as timbre :refer-macros [tracef debugf infof warnf errorf
                                                       trace debug info warn error]]))

(defmacro swap-input-value! [where name e]
  `(~'swap! ~where ~'assoc ~name (~'-> ~e .-target .-value)))

(defmacro require-pages []
  `(do ~@(map (fn [a] `(~'require (~'quote [~(symbol (str a))])))
              (filter #(.contains (str %) "ventas.pages") (all-ns)))))

(defmacro require-plugins []
  `(do ~@(map (fn [a] `(~'require (~'quote [~(symbol (str a))])))
              (filter #(.contains (str %) "ventas.plugins") (all-ns)))))