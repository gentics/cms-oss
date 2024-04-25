FROM docker.apa-it.at/maven:3.9.2-amazoncorretto-17-debian-bullseye
LABEL maintainer="Peter Nyari <p.nyari@gentics.com>"

ARG GIT_USER_NAME="Maven Release Manager"
ARG GIT_USER_EMAIL="entwicklung@gentics.com"
ARG USER_NAME="jenkins"
ARG NODE_VERSION=20.12.1

# Version makes it possible to force rebuild the image and bypass caching by changing this variable (eg.: when ca-certificates needs update)
ARG VERSION="20220711.0"
ARG DEBIAN_RELEASE="bullseye"

# Add repositories and install
RUN echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE} main contrib non-free" > /etc/apt/sources.list \
 && echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE}-backports main" >> /etc/apt/sources.list \
 && echo "deb http://security.debian.org/debian-security ${DEBIAN_RELEASE}-security main" >> /etc/apt/sources.list \
 && echo "deb http://ftp.at.debian.org/debian ${DEBIAN_RELEASE}-updates main" >> /etc/apt/sources.list \
 && apt-get update \
 && apt-get upgrade --assume-yes \
 && apt-get install --assume-yes lsb-release wget curl apt-transport-https ca-certificates \
		gnupg2 software-properties-common dirmngr \
 && curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - \
 && curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
 && add-apt-repository "deb https://download.docker.com/linux/debian ${DEBIAN_RELEASE} stable" \
 && add-apt-repository "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" \
 && apt-get update \
 && apt-get install --assume-yes eatmydata \
  		doxygen mariadb-client git-core gawk sed parallel build-essential sudo \
  		less vim tar sed psmisc file pwgen locales zip unzip nmap pkg-config libtool libtool-bin bison \
  		debhelper lintian devscripts unp bzip2 rsync docker-ce=5:20* docker-ce-cli=5:20* \
		python2.7 \
 && apt-get install --assume-yes --allow-unauthenticated google-chrome-beta

# Set locale
RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && locale-gen
ENV LANG=en_US.UTF-8 \
	LANGUAGE=en_US:en \
	LC_ALL=en_US.UTF-8

# Set Environment variable for python2
RUN sudo update-alternatives --install /usr/local/bin/python python /usr/bin/python2.7 20 \
 && sudo chmod +x /usr/local/bin/python
ENV PYTHON /usr/local/bin/python

# Install docker-compose v2 and compatibility for v1
RUN mkdir -p /usr/local/lib/docker/cli-plugins/ \
 && sudo curl -L "https://github.com/docker/compose/releases/download/v2.6.1/docker-compose-$(uname -s)-$(uname -m)" \
		-o /usr/local/lib/docker/cli-plugins/docker-compose \
 && sudo curl -L "https://github.com/docker/compose-switch/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
 		-o /usr/local/bin/compose-switch \
 && sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose \
 && sudo chmod +x /usr/local/bin/compose-switch \
 && update-alternatives --install /usr/local/bin/docker-compose docker-compose /usr/local/bin/compose-switch 99 \
 && sudo chmod +x /usr/local/bin/docker-compose

# Update public keys for node installs.
RUN set -ex \
	&& for key in \
		4ED778F539E3634C779C87C6D7062848A1AB005C \
		141F07595B7B3FFE74309A937405533BE57C7D57 \
		74F12602B6F1C4E913FAA37AD3A89613643B6201 \
		DD792F5973C6DE52C432CBDAC77ABFA00DDBF2B7 \
		CC68F5A3106FF448322E48ED27F5E38D5B0A215F \
		8FCCA13FEF1D0C2E91008E09770F7A9A5AE15600 \
		C4F0DFFF4E8C1A8236409D08E73BC641CC11F4C8 \
		890C08DB8579162FEE0DF9DB8BEAB4DFCF555EF4 \
		C82FA3AE1CBEDC6BE46B9360C43CEC45C17AB93C \
		108F52B48DB57BB0CC439B2997B01419BD92F80A \
		A363A499291CBBC940DD62E41F10027AF002F8B0 \
		; do \
			gpg2 --batch --keyserver keys.openpgp.org --recv-keys "$key"; \
		done

# Download NodeJS
RUN mkdir /opt/node \
	&& curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.tar.xz" \
	&& curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt" \
	&& curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt.sig"
# Verify downloaded NodeJS files
RUN gpg --verify SHASUMS256.txt.sig SHASUMS256.txt \
	&& grep " node-v$NODE_VERSION-linux-x64.tar.xz\$" SHASUMS256.txt | sha256sum -c -
# Install NodeJS
RUN tar -xJf "node-v$NODE_VERSION-linux-x64.tar.xz" -C /opt/node --strip-components=1 \
	&& rm "node-v$NODE_VERSION-linux-x64.tar.xz" SHASUMS256.txt.sig SHASUMS256.txt \
	&& ln -s /opt/node/bin/node /usr/local/bin/node \
	&& ln -s /opt/node/bin/npm /usr/local/bin/npm \
	&& ln -s /opt/node/bin/npx /usr/local/bin/npx \
	&& echo "export PATH=\$PATH:/opt/node/bin" > /etc/profile.d/nodejs.sh \
    && chmod +x /etc/profile.d/* \
	&& chown -R 10000:10000 /opt/node

# Install Ruby 2.3 with RVM
ENV PATH "/usr/local/rvm/bin:$PATH"
ENV rvm_path "/usr/local/rvm"
RUN curl -sSL https://rvm.io/mpapis.asc | gpg2 --import - \
 && curl -sSL https://rvm.io/pkuczynski.asc | gpg2 --import - \
 && curl -sSL https://get.rvm.io | bash \
 && rvm pkg install openssl \
 && rvm install --autolibs=disable 2.3 -C --with-openssl-dir=$rvm_path/usr

# Install Guides
ADD ./build/guides-wrapper.sh /usr/local/bin/guides
RUN rvm-exec 2.3 gem install guides -- --with-cflags="-O2 -pipe -march=native -w" \
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

# Allow updating Google Chrome
RUN echo "jenkins ALL=NOPASSWD: /usr/bin/apt-get update" > /etc/sudoers.d/jenkins \
 && echo "jenkins ALL=NOPASSWD: /usr/bin/apt-get install --assume-yes --allow-unauthenticated google-chrome-beta" >> /etc/sudoers.d/jenkins

# Switch user
USER ${USER_NAME}

# Workaround for Maven not outputting colors and silence download progress messages
ENV MAVEN_OPTS "-Djansi.passthrough=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Xmx2g -XX:MaxMetaspaceSize=256m"

RUN mkdir -p ~/workspace \
 && mkdir -p ~/.m2/repository

# TestDB Manager
ENV TESTMANAGER_HOSTNAME "gcn-testdb-manager.gtx-dev.svc"
ENV TESTMANAGER_PORT "8080"

# path to the docker.sock file to be mounted in the ryuk container (for testcontainers)
ENV TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE "/run/user/1000/docker.sock"
ENV JAVA_HOME "/usr/lib/jvm/java-17-amazon-corretto/"

EXPOSE 80
