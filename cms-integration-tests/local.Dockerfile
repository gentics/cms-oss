# # Cypress factory configuration
# ARG NODE_VERSION="20.12.1"
# ARG CHROME_VERSION="127.0.6533.99-1"

# FROM cypress/factory:4.0.2

FROM node:20.12

RUN apt-get update
RUN apt-get install -y ca-certificates curl

# Install packages via APT
RUN apt-get -y install \
    # Misc & Utils
    build-essential zip unzip vim less tar

# "fake" dbus address to prevent errors
# https://github.com/SeleniumHQ/docker-selenium/issues/87
ENV DBUS_SESSION_BUS_ADDRESS=/dev/null
# Set the image as a proper "CI" image for cypress
ENV CI=1
# Set the XDG cache to a directory which can be written to (In our CI, .cache isn't mounted/writable)
ENV XDG_CACHE_HOME "/tmp"
# Disable mesa cache entirely
ENV MESA_SHADER_CACHE_DISABLE "true"

# Mount the required modules
RUN mkdir /opt/app
WORKDIR /opt/app

COPY ../cms-ui /opt/app/

# Make the image simply execute bash for jenkins to execute the own stuff later on
CMD /bin/bash
ENTRYPOINT [ "/bin/bash" ]
