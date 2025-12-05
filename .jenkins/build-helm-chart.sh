#!/bin/bash

set -o nounset
set -o errexit

VERSION=${1}

echo "Packaging Helm Chart gentics/cms with Version $VERSION"

docker run --rm -v $(pwd):/apps -w /apps \
    alpine/helm:3.11.1 package cms-helm-chart --version $VERSION --app-version $VERSION -d target
