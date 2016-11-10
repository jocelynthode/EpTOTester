FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list && \
    apt-get -yqq update && \
    apt-get -yqq dist-upgrade && \
    apt-get -yqq install --no-install-recommends openjdk-8-jre-headless dnsutils wget dstat ntp && \
    apt-get -yqq clean

RUN mkdir -p /opt/epto
RUN mkdir -p /data/capture

COPY *-all.jar /opt/epto/
COPY *.sh /opt/epto/
RUN chmod +x /opt/epto/*.sh

WORKDIR /opt/epto

CMD ["/opt/epto/container-start-script.sh"]
