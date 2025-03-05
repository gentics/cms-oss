FROM node:20-bookworm

ENV PLAYWRIGHT_BROWSERS_PATH=0

RUN npx -y playwright@1.50.1 install --with-deps --only-shell chromium

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

# Make the image simply execute bash for jenkins to execute the own stuff later on
CMD /bin/bash
ENTRYPOINT [ "/bin/bash" ]
