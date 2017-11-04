(ns user
  "Configuration"
  (:require
   [figwheel-sidecar.repl-api :as figwheel]
   [clojure.tools.namespace.repl :as tn]
   [clojure.stacktrace :as st :refer [print-stack-trace]]
   [clojure.repl :refer :all]
   [clojure.core.async :refer [>! go]]
   [repl]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(clojure.tools.namespace.repl/set-refresh-dirs "src/clj" "src/cljc" "dev" "test/clj" "test/cljc")

;; CLJS REPL
(def cljs-repl figwheel/cljs-repl)

(def init repl/init)