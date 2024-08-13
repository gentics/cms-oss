#!/bin/bash

# Go into the UI Module
cd ../cms-ui

# Install the dependencies
npm ci --no-fund --no-audit

# Now actually run the tests
npm run nx -- run-many --targets=e2e --projects=tag:e2e --configuration=ci --parallel=false --output-style=static
