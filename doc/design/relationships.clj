;; Cardinality, relationships

;; "Cardinality many" avoids relation-only tables, unless you want to associate metadata
;; to that relationship
;; [:user :things], {:cardinality :many} = n-m user<->things

;; Otherwise everything stays the same
;; [:image :source] = 1-n user->image
;; [:user :extra-fields] {:unique true} [:extra-fields ...] = 1-1 user<->extra-fields
