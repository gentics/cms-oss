#!/bin/bash

. /__cacert_entrypoint.sh

function join() {
    local IFS=$1
    shift
    echo "$*"
}

CUSTOM_LIBS=$(join ':' libs/*)

java -Duser.dir=/cms -cp cms-server.jar:"$CUSTOM_LIBS":classes/ \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
 com.gentics.contentnode.server.Runner
