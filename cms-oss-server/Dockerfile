FROM eclipse-temurin:17-jdk-alpine AS eclipse-temurin-upgraded

RUN apk upgrade --no-cache

FROM eclipse-temurin-upgraded

ENV CONF_FILES="" \
    KEYS_PATH="keys" \
    NODE_DB=node_utf8 \
    NODE_DB_USER=node \
    NODE_DB_PASSWORD=node \
    NODE_DB_HOST=localhost \
    NODE_DB_PORT=3306 \
    NODE_USER_PASSWORD="" \
    DBFILES_PATH="data/dbfiles" \
    CACHE_PATH="cache" \
    LOGS_PATH="logs" \
    PUBLISH_PATH="publish" \
    GIS_PATH="publish/gis" \
    PACKAGES_PATH="packages" \
    CONTENT_PACKAGES_PATH="content-packages" \
    SCHEDULER_COMMANDS_PATH="scheduler-commands" \
    JAVA_TOOL_OPTIONS="-Xmx1g"

EXPOSE 8080

USER root
RUN adduser -D -u 1000 -G root -h /cms node && \
    chown 1000:0 /cms -R && chmod 770 /cms && \
    mkdir /cms/conf   && chown 1000:0 /cms/conf  -R && chmod 770 /cms/conf && \
    mkdir /cms/keys   && chown 1000:0 /cms/keys  -R && chmod 770 /cms/keys && \
    mkdir /cms/data   && chown 1000:0 /cms/data  -R && chmod 770 /cms/data && \
    mkdir /cms/cache   && chown 1000:0 /cms/cache  -R && chmod 770 /cms/cache && \
    mkdir /cms/logs   && chown 1000:0 /cms/logs  -R && chmod 770 /cms/logs && \
    mkdir /cms/publish   && chown 1000:0 /cms/publish  -R && chmod 770 /cms/publish && \
    mkdir /cms/packages   && chown 1000:0 /cms/packages  -R && chmod 770 /cms/packages && \
    mkdir /cms/libs   && chown 1000:0 /cms/libs  -R && chmod 770 /cms/libs && \
    mkdir /cms/content-packages   && chown 1000:0 /cms/content-packages  -R && chmod 770 /cms/content-packages && \
    mkdir /cms/scheduler-commands   && chown 1000:0 /cms/scheduler-commands  -R && chmod 770 /cms/scheduler-commands && \
    apk add --no-cache bash rsync curl iputils busybox-extras tar nano mariadb-client

ADD --chown=1000:0 ./target/cms-oss-server*jar /cms/cms-oss-server.jar
ADD --chown=1000:0 entrypoint.sh /cms/

RUN chmod u+x,g+x /cms/entrypoint.sh

USER node
WORKDIR /cms
VOLUME /cms/keys
VOLUME /cms/data
VOLUME /cms/cache
VOLUME /cms/logs
VOLUME /cms/publish
VOLUME /cms/packages
VOLUME /cms/content-packages
VOLUME /cms/scheduler-commands
VOLUME /cms/libs

ENTRYPOINT ["/cms/entrypoint.sh"]
