;; :user {:name "..." :password "..."}
;; {:user/name "..." :user/password "..."}
;; {:user/name "..." :user/password "..." :schema/type :schema.type/user}
;; {:user/name "..." :user/password "..." :schema/type :schema.type/user :user/status :user.status/active}
;; Where do we spec?


;; A
;; Specs should be declared with a different namespace than the database namespaces
;; That is because application's namespaces should not need to match database namespaces
;; That means "req-un" and "opt-un", and that also means specing at the first line (before qualification and merging of type)

;; B
;; If we require that database namespaces match application's namespaces, we can keep specing like this:
;; ventas/user
;; ventas.user/status
;; ventas.user.status/active ...
;; Same thing in the database
;; Maybe this would make the application easier to reason about, but also slightly verbose:
(db/entity-create :ventas.user [...])


;; A:
;; (db/entity-create :user {:name "..." :password "..."}
;; {:user/name "..." :user/password "..." :schema/type :schema.type/user :user/status :user.status/active}
;; (s/def :ventas.user/name str?)
;; (s/def :ventas/user
;;   (s/keys :req-un [:ventas.user/name :ventas.user/password :ventas.user/email :ventas.user/status]
;;   	     :opt-un [:ventas.user/description]))

;; B:
;; (db/entity-create :ventas.user {:name "..." :password "..."})
;; {:ventas.user/name "..." :ventas.user/password "..." :schema/type :schema.type/ventas.user :ventas.user :ventas.user.status/active}
;; (s/def :ventas.user/name str?)
;; (s/def :ventas/user
;;   (s/keys :req-un [:ventas.user/name :ventas.user/password :ventas.user/email :ventas.user/status]
;;   	     :opt-un [:ventas.user/description]))

;; Option B is too verbose for my taste, we can simply spec in the first line and use :req-un I guess.
;; Anyway I still dislike ":ventas" everywhere in the spec

;; Option C:
;; (db/entity-create :user {:name "..." :password "..."}
;; {:user/name "..." :user/password "..." :schema/type :schema.type/user :user/status :user.status/active}
;; (s/def :user/name str?)
;; (s/def :schema.type/user
;;   (s/keys :req-un [:user/name :user/password :user/email :user/status]
;;   	     :opt-un [:user/description]))

;; This looks clean, after all we are only having this problem because we can't spec :user. Using :schema.type makes sense.
;; We also have the benefit of keeping the spec very close to the database