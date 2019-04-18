# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 0.1.0 - 2019-04-18
### Changed

- When changing a child entity, the parent entity will also be indexed.
- The new way of specifying how entities are indexed is via `search/configure-type!`, which allows you to register ES migrations. If `search/configure-type!` is not called, that type won't be indexed.
- The tx-report-queue loop has been removed and a new `ventas.tx-processor` has been created, which allows users to add callbacks, which will be called whenever a transaction is processed. This is used for processing the new `:after-transact` entity type property, and for indexing.
- All side-effectful entity lifecycle functions (before-..., after-...) and filter-query have been removed. `:after-transact` has been added, which should be more than enough to cover the usecases for the removed lifecycle functions, and it is called recursively (the previous lifecycle functions would just be fired for direct calls to `entity/create` or `entity/update`, so child entities were being ignored).
- `ventas.entities.configuration` is being deprecated and the email configuration is done with its own entity type.
- Thumbnailator is used now for image resizing, instead of fivetonine/collage.
- `product/images` is using the new `:file.list` entity type now, and a new form input has been added to edit such kind of entities.

### Removed

- All seeding-related lifecycle functions have been removed, and there is no replacement. The same functionality can be achieved using standard lifecycle functions (plus the new `:after-transact` , described above)

### Fixed

- clojure.tools.nrepl.middleware not found error, caused by wrongly specifying the shadow-cljs middleware

### Added

- This document
- Allow using profiles in the configuration EDN file (and get the profile from the VENTAS_PROFILE environment variable)
- Removing a document from Datomic now removes it from Elasticsearch too.
- `db/etouch` and `db/map-etouch`, with the intention of deprecating `db/touch-eid`, which is wasteful and goes against the Datomic model.
- An storage backend abstraction, which allows you to use something different than the local filesystem (S3, for example)
- Websocket ping/pong requests/responses, to keep the connection alive
- Both `entity/create` and `entity/update` return the tx as meta now.
