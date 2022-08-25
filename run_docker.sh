#!/bin/sh

docker run -d --restart always -p 127.0.0.1:8080:8080 -p 127.0.0.1:8081:8081 -v "$(pwd)/conciliator.log:/opt/conciliator/conciliator.log" conciliator:latest
