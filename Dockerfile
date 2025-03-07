
ARG TZ="America/Los_Angeles"

####
## build container

FROM maven:3.9-eclipse-temurin-11 AS build

WORKDIR /opt/conciliator

COPY pom.xml .

COPY src src

ARG skiptests=0

RUN --mount=type=cache,target=/root/.m2 \
    if [ "$skiptests" -eq "1" ]; then SKIPTESTS_ARG="-Dmaven.test.skip"; fi && \
    mvn clean package $SKIPTESTS_ARG

####
## application container

FROM eclipse-temurin:11

ARG TZ

RUN rm /etc/localtime && ln -s /usr/share/zoneinfo/$TZ /etc/localtime

ENV TZ=$TZ

WORKDIR /opt/conciliator

COPY --from=build /opt/conciliator/target/conciliator*.jar .

EXPOSE 8080 8081 8082

COPY run.sh .

COPY --chmod=755 <<EOT run_wrapper.sh
#!/usr/bin/env bash
./run.sh >> conciliator.log 2>> conciliator.log
EOT

CMD ["./run_wrapper.sh"]
