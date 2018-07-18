### Deployment with docker-compose

There's a `docker-compose.prod.yml` file included. It assumes the existence of `ventas` and `datomic` images. See [Building](./Building.md) to know how to create the images for both.

The file assumes the existence of an `.env` file with at least `LOCAL_PORT` in it. It's advised that this port is different than port 80, and to use an nginx server pointing to that port, for (at least) adding HTTPS to your store.

An example nginx configuration:

```nginx
upstream ventas_backend {
  server ventas.kazer.es:80;
  keepalive 32;
}
server {
  listen [::]:80;
  listen 80;
  server_name ventas.kazer.es;
  return 301 https://ventas.kazer.es$request_uri;
}
server {
  listen [::]:443 ssl http2;
  listen 443 ssl http2;
  server_name ventas.kazer.es;
  
  ssl_certificate     /etc/letsencrypt/live/ventas.kazer.es/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/ventas.kazer.es/privkey.pem;
  ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
  include /etc/letsencrypt/options-ssl-nginx.conf;

  error_log /var/log/nginx/ventas.kazer.es.error.log;
  access_log /var/log/nginx/ventas.kazer.es.access.log;

  location /.well-known/acme-challenge/ {
    root /srv/http/letsencrypt;
  }

  location /ws/ {
    proxy_pass http://ventas_backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $http_host;
  }

  location ^~ {
    proxy_pass  http://ventas_backend;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header Host $http_host;
  }

  charset utf-8;
  include h5bp/basic.conf;
}

```

Please note that this deployment uses the `dev` storage of Datomic, instead of a proper one for production like Cassandra (see [Kubernetes](./Kubernetes.md)).