(ns ventas.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [ventas.core-test]
   [ventas.common.utils-test]))

(enable-console-print!)

(doo-tests 'ventas.core-test
           'ventas.common.utils-test)
