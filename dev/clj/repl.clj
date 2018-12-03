(ns repl
  (:require
    [clojure.tools.namespace.repl :as tn]
    [ventas-devtools.repl :as devtools.repl]
    [clojure.repl :refer :all]
    [clojure.core.async :as core.async :refer [<! chan >! go]]
    [clojure.spec.alpha :as spec]
    [taoensso.timbre :as timbre]))

(def cljs-repl        devtools.repl/cljs-repl)
(def init             devtools.repl/init)
(def r                devtools.repl/r)
(def run-tests        devtools.repl/run-tests)
(def set-theme!       devtools.repl/set-theme!)
(def tn-refresh       tn/refresh)
