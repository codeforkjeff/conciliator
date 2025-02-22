
ARG TZ="America/Los_Angeles"

####
## build container

FROM eclipse-temurin:11 AS build

# maven needs git
RUN apt-get update && apt-get install -y git

ADD https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz /

RUN cd /opt && tar xzf /apache-maven-3.8.8-bin.tar.gz && mv apache-maven* maven

ENV PATH="$PATH:/opt/maven/bin"

WORKDIR /opt/conciliator

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src src

ARG skiptests=0

RUN if [ "$skiptests" -eq "1" ]; then SKIPTESTS_ARG="-Dmaven.test.skip"; fi && \
    mvn package $SKIPTESTS_ARG

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
