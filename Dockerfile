
####
## build container

FROM alpine:3.16.0 as build

WORKDIR /opt/conciliator

RUN apk --no-cache add alpine-conf git maven openjdk8

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src src

ARG skiptests=0

RUN if [ "$skiptests" -eq "1" ]; then SKIPTESTS_ARG="-Dmaven.test.skip"; fi && \
    mvn package $SKIPTESTS_ARG

####
## application container

FROM alpine:3.16.0

EXPOSE 8080 8081

RUN apk --no-cache add alpine-conf openjdk8-jre

RUN setup-timezone -z America/Los_Angeles

WORKDIR /opt/conciliator

COPY --from=build /opt/conciliator/target/conciliator*.jar .

CMD JARFILE=`find . -type f -name "conciliator*.jar" -print` && \
  /usr/bin/java \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=8081 \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -XX:+HeapDumpOnOutOfMemoryError \
  -Xms256m -Xmx256m \
  -Dlogging.level.com.codefork.refine=DEBUG -jar \
  $JARFILE >> conciliator.log 2>> conciliator.log

