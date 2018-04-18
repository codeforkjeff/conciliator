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

JAR_PATH=`find target -type f -name "conciliator*.jar" -print`

java -XX:+HeapDumpOnOutOfMemoryError -Xms256m -Xmx256m -Dlogging.level.com.codefork.refine=DEBUG -jar $JAR_PATH
