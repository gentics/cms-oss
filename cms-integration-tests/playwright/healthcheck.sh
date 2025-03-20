#!/bin/bash

curl \
    --include \
    --no-buffer \
    --header 'Connection: close' \
    --header 'Upgrade: websocket' \
    --header 'Sec-WebSocket-Version: 13' \
    http://localhost:3000
