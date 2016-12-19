FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list && \
    apt-get -yqq update && \
    apt-get -yqq dist-upgrade && \
    apt-get -yqq install --no-install-recommends openjdk-8-jre-headless dnsutils dstat ntp && \
    apt-get -yqq clean

COPY *-all.jar /opt/
COPY *.sh /opt/
WORKDIR /opt

EXPOSE 12001

CMD ["/opt/start-script.sh"]
