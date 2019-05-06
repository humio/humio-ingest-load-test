#!/bin/bash

set -e
set -x

sbt assembly

for HOST in "test01" "test02" ; do rsync target/scala-2.12/perftest.jar setup.sh $HOST.humio.com: ; done

# java -jar -Dusers=20 -Dtoken=${TOKEN}  perftest.jar -s com.humio.perftest.FilebeatSimulation
