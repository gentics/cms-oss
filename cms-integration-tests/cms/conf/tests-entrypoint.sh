#!/usr/bin/env bash

# Debug output
id
ls -l

# Making sure that the packages and logs are writeable,
# as in jenkins they cause troubles.
chown node -R /cms/packages /cms/logs
chmod 775 -R /cms/packages /cms/logs

# Debug output
id
ls -l

# Use the original entrypoint to start the cms
/cms/entrypoint.sh
