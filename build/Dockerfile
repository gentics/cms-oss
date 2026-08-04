FROM docker.gentics.com/maven:3.9.11-amazoncorretto-17-debian-trixie

LABEL maintainer="Peter Nyari <p.nyari@gentics.com>"

ARG GIT_USER_NAME="Maven Release Manager"
ARG GIT_USER_EMAIL="entwicklung@gentics.com"
ARG USER_NAME="jenkins"
ARG NODE_VERSION=26.4.0

# Version makes it possible to force rebuild the image and bypass caching by changing this variable (eg.: when ca-certificates needs update)
ARG VERSION="20260728.0"
ARG DEBIAN_RELEASE="trixie"

# Setup debian channels
RUN echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE} main contrib non-free" > /etc/apt/sources.list
RUN echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE}-backports main" >> /etc/apt/sources.list
RUN echo "deb http://security.debian.org/debian-security ${DEBIAN_RELEASE}-security main" >> /etc/apt/sources.list
RUN echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE}-updates main" >> /etc/apt/sources.list

# Update the system
RUN apt-get update \
 && apt-get upgrade --assume-yes

# Install basic packages
RUN apt-get install --assume-yes \
	lsb-release wget curl apt-transport-https ca-certificates \
	gnupg2 dirmngr jq eatmydata doxygen mariadb-client git-core \
	gawk sed parallel build-essential sudo less vim tar sed psmisc file pwgen locales zip unzip \
	nmap pkg-config libtool libtool-bin bison debhelper lintian devscripts unp bzip2 rsync
	# software-properties-common

# Docker
RUN install -m 0755 -d /etc/apt/keyrings
RUN curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
RUN chmod a+r /etc/apt/keyrings/docker.asc
RUN tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/debian
Suites: $(. /etc/os-release && echo "$DEBIAN_RELEASE")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF
RUN apt-get update
RUN apt-get install --assume-yes docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Cypress Dependencies
# From https://docs.cypress.io/guides/getting-started/installing-cypress#UbuntuDebian
# And https://github.com/cypress-io/cypress-docker-images/blob/master/factory/factory.Dockerfile
RUN apt-get install --assume-yes \
	libgtk2.0-0 libgtk-3-0 libgbm-dev libnotify-dev libnss3 libxss1 libasound2 libxtst6 xauth xvfb libgbm1 libcups2 libatk-bridge2.0-0 libatk1.0-0
RUN apt-get install --assume-yes --allow-unauthenticated chromium chromium-l10n

 # "fake" dbus address to prevent errors
# https://github.com/SeleniumHQ/docker-selenium/issues/87
ENV DBUS_SESSION_BUS_ADDRESS=/dev/null
ENV CI=1

# Set locale
RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && locale-gen
ENV LANG=en_US.UTF-8 \
	LANGUAGE=en_US:en \
	LC_ALL=en_US.UTF-8

# Install docker-compose v2 and compatibility for v1
# RUN mkdir -p /usr/local/lib/docker/cli-plugins/ \
#  && sudo curl -L "https://github.com/docker/compose/releases/download/v2.6.1/docker-compose-$(uname -s)-$(uname -m)" \
# 		-o /usr/local/lib/docker/cli-plugins/docker-compose \
#  && sudo curl -L "https://github.com/docker/compose-switch/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
#  		-o /usr/local/bin/compose-switch \
#  && sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose \
#  && sudo chmod +x /usr/local/bin/compose-switch \
#  && update-alternatives --install /usr/local/bin/docker-compose docker-compose /usr/local/bin/compose-switch 99 \
#  && sudo chmod +x /usr/local/bin/docker-compose

# Install Ruby 2.3 with RVM
ENV PATH="/usr/local/rvm/bin:$PATH"
ENV RVM_PATH="/usr/local/rvm"
RUN curl -sSL https://rvm.io/mpapis.asc | gpg2 --import - \
 && curl -sSL https://rvm.io/pkuczynski.asc | gpg2 --import - \
 && curl -sSL https://get.rvm.io | bash \
 && rvm pkg install openssl \
 && rvm install --autolibs=disable 2.3 -C --with-openssl-dir=$RVM_PATH/usr

# Install Guides
ADD ./build/guides-wrapper.sh /usr/local/bin/guides
RUN rvm-exec 2.3 gem install guides -- --with-cflags="-O2 -pipe -march=native -w -Wno-error=implicit-function-declaration" \
 &&	chmod +x /usr/local/bin/guides

# Add user
# The ID / GID 10000 is used by the jenkinsci/slave Dockerfile and has to match here, because Jenkins
# does the SCM checkout in the jnlp container for some reasons.
RUN groupadd --system --gid 10000 ${USER_NAME} && useradd --create-home --no-log-init --uid 10000 --gid ${USER_NAME} ${USER_NAME}

# Setup Git
RUN git config --system user.name "${GIT_USER_NAME}" \
 && git config --system user.email "${GIT_USER_EMAIL}"

RUN echo "StrictHostKeyChecking no" > /etc/ssh/ssh_config \
 && echo "UserKnownHostsFile=/dev/null" >> /etc/ssh/ssh_config \
 && echo "BatchMode yes" >> /etc/ssh/ssh_config \
 && mv /usr/share/maven/conf/settings.xml /usr/share/maven/conf/settings-original.xml
ADD build/settings.xml /usr/share/maven/conf/settings.xml

# Set the XDG cache to a directory which can be written to (In our CI, .cache isn't mounted/writable)
ENV XDG_CACHE_HOME="/tmp"
# Disable mesa cache entirely
ENV MESA_SHADER_CACHE_DISABLE="true"

# Switch user
USER ${USER_NAME}

# Install NVM so we can install the correct node version.
# Doing so hardcoded is a hassle and error-prone.
# Create a script file sourced by both interactive and non-interactive bash shells
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.6/install.sh | bash
ENV NODE_VERSION=${NODE_VERSION}
ENV NVM_DIR="/home/${USER_NAME}/.nvm"
RUN bash -c "source ~/.nvm/nvm.sh && nvm install --default $NODE_VERSION"

# Workaround for Maven not outputting colors and silence download progress messages
ENV MAVEN_OPTS="-Djansi.passthrough=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Xmx2g -XX:MaxMetaspaceSize=256m"

RUN mkdir -p ~/workspace \
	&& mkdir -p ~/.m2/repository

# TestDB Manager
ENV TESTMANAGER_HOSTNAME="gcn-testdb-manager.gtx-dev.svc"
ENV TESTMANAGER_PORT="8080"

# path to the docker.sock file to be mounted in the ryuk container (for testcontainers)
ENV TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/run/user/1000/docker.sock"
ENV JAVA_HOME="/usr/lib/jvm/java-17-amazon-corretto/"

EXPOSE 80
