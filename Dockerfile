FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list && \
    apt-get -yqq update && \
    apt-get -yqq dist-upgrade && \
    apt-get -yqq install --no-install-recommends openjdk-8-jre-headless dnsutils && \
    apt-get -yqq clean

RUN mkdir -p /data

COPY build/libs/*-all.jar /opt/epto/
COPY scripts/*.sh scripts/*.rb /opt/epto/
RUN chmod +x /opt/epto/container-start-script.sh

WORKDIR /opt/epto

CMD ["/opt/epto/container-start-script.sh"]
