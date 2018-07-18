### Deployment with Kubernetes

You will need:

- An StorageClass, possibly [rook](https://rook.io)
- A Cassandra cluster
- An Elasticsearch cluster
- If you want statistics, a Kafka cluster (with its own Zookeeper cluster)
- If you want HTTPS, [cert-manager](https://github.com/jetstack/cert-manager) and a ClusterIssuer
- A configured ingress controller and a LoadBalancer (NodePort if on bare metal)
- A private Docker registry with your ventas and Datomic images uploaded.

#### Deploying the prerenderer

```shell
kubectl apply -f k8s/chrome-deployment.yaml
kubectl apply -f k8s/chrome-service.yaml
```

#### Deploying Datomic

You will need to build and push a Cassandra-backed Datomic image. Then:

```shell
kubectl apply -f k8s/datomic-deployment.yaml # set the image and the imagePullSecret
kubectl apply -f k8s/datomic-service.yaml
```

#### Deploying ventas

You will need to build and push a ventas image. Then:

```shell
kubectl apply -f k8s/ventas-persistent-volume-claim.yaml
kubectl apply -f k8s/ventas-service.yaml
kubectl apply -f k8s/ventas-ingress.yaml # set your domain
# set AUTH_SECRET, your image and the imagePullSecret
kubectl apply -f k8s/ventas-deployment.yaml
```

#### Accessing the REPL

```shell

VENTAS=$(kubectl get pods --namespace default -l "app=ventas,tier=backend" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace default port-forward $VENTAS 4001
```

Now you can `lein repl :connect 4001`