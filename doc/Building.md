### Building

Building a JAR for your store is done like this:

```shell
bower install
lein clean
lein uberjar
```

This will compile your Clojurescript and SASS files too, so don't worry.

To create docker images and push them:

```shell
docker build -t your-registry.com/your-store .
docker build -t your-registry.com/your-store-datomic datomic
docker push your-registry.com/your-store
docker push your-registry.com/your-store-datomic
```

