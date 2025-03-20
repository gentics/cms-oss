#!/bin/bash

if ["${CI}" -e "1"]; then
    npm ci
fi

npx -y playwright@1.50.1 install --with-deps --only-shell chromium
npx playwright run-server --port=3000 --host=0.0.0.0
# npm run nx -- run-many --targets=e2e --configuration=ci --skipInstall --output-style=static
