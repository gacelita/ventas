(ns ventas.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [ventas.core-test]
   [ventas.common.util-test]))

(enable-console-print!)

(doo-tests 'ventas.core-test
           'ventas.common.util-test)
