FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list && \
    apt-get -yqq update && \
    apt-get -yqq dist-upgrade && \
    apt-get -yqq install --no-install-recommends openjdk-8-jre-headless dnsutils wget dstat ntp && \
    apt-get -yqq clean

RUN wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.0/dumb-init_1.2.0_amd64
RUN chmod +x /usr/local/bin/dumb-init

RUN mkdir -p /data

RUN mkdir -p /host_etc

COPY *-all.jar /opt/jgroups/
COPY *.sh /opt/jgroups/
RUN chmod +x /opt/jgroups/*.sh

WORKDIR /opt/jgroups

ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/opt/jgroups/container-start-script.sh"]
