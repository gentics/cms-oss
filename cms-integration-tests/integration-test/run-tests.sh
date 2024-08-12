#!/bin/bash
npm run nx -- run-many --targets=e2e --projects=tag:e2e --configuration=ci --parallel=false --output-style=static
