#!/bin/sh

touch conciliator.log
docker run \
    -d --restart always \
    -p 127.0.0.1:8080:8080 \
    -p 127.0.0.1:8081:8081 \
    -p 127.0.0.1:8082:8082 \
    -e TZ=`cat /etc/timezone` \
    -v "$(pwd)/conciliator.log:/opt/conciliator/conciliator.log" \
    conciliator:latest
