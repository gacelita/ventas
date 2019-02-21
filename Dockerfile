FROM anapsix/alpine-java

ADD target/uberjar/ventas.jar /srv/app.jar
EXPOSE 3450
CMD ["java", "-jar", "/srv/app.jar"]
