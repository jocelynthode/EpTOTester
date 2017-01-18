FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list && \
    apt-get -yqq update && \
    apt-get -yqq dist-upgrade && \
    apt-get -yqq install --no-install-recommends openjdk-8-jre-headless dnsutils wget curl dstat ntp && \
    apt-get -yqq clean

RUN wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.0/dumb-init_1.2.0_amd64 && \
echo '81231da1cd074fdc81af62789fead8641ef3f24b6b07366a1c34e5b059faf363 /usr/local/bin/dumb-init' > /tmp/dumb-init.sha256 && \
sha256sum -c /tmp/dumb-init.sha256
RUN chmod +x /usr/local/bin/dumb-init


RUN mkdir -p /opt/app
RUN mkdir -p /data/capture

COPY *-all.jar /opt/app/
COPY *.sh /opt/app/
RUN chmod +x /opt/app/*.sh

WORKDIR /opt/app

ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/opt/app/container-start-script.sh"]
