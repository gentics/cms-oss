# Cypress factory configuration
ARG NODE_VERSION="20.12.1"
ARG CHROME_VERSION="127.0.6533.99-1"

FROM cypress/factory:4.0.2

# Add Docker's official GPG key:
RUN apt-get update
RUN apt-get install -y ca-certificates curl
RUN install -m 0755 -d /etc/apt/keyrings
RUN curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
RUN chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to APT sources:
RUN echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    tee /etc/apt/sources.list.d/docker.list > /dev/null
RUN apt-get update

# Install packages via APT
RUN apt-get -y install \
    # Docker
    docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin \
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
VOLUME /opt/app

WORKDIR /opt/app

# Make the image simply execute bash for jenkins to execute the own stuff later on
CMD /bin/bash
ENTRYPOINT [ "/bin/bash" ]
