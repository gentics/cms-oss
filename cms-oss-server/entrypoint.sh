#!/bin/bash

function join() {
    local IFS=$1
    shift
    echo "$*"
}

CUSTOM_LIBS=$(join ':' libs/*)

java -Duser.dir=/cms -cp cms-oss-server.jar:"$CUSTOM_LIBS" \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  com.gentics.contentnode.server.OSSRunner
