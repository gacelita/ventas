(ns ventas.server-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.seed :as seed]
   [ventas.server :as server]))
