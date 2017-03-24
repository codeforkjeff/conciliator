#!/bin/sh
#
# Some useful options (these need to come before -jar)
#
# Listen on different port:
# -Dserver.port=80
#
# Show queries, responses, and time measurements
# -Dlogging.level.com.codefork.refine=DEBUG
#
# These are helpful for stabilizing the JVM's memory usage, which is
# useful for resource-constrained servers. 128M is a very safe number
# and can probably be lower. Setting it as the minimum also prevents
# the JVM from having to dynamically allocate memory, which takes time.
# -Xms128m -Xmx128m

java -XX:+HeapDumpOnOutOfMemoryError -Xms128m -Xmx128m -Dlogging.level.com.codefork.refine=DEBUG -jar target/conciliator-2.1.2.jar
