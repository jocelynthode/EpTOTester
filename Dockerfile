FROM debian:jessie

RUN echo 'deb http://mirror.switch.ch/ftp/mirror/debian/ jessie-backports main' >> /etc/apt/sources.list && \
    apt-get -yqq update && \
    apt-get -yqq dist-upgrade && \
    apt-get -yqq install --no-install-recommends openjdk-8-jre-headless python3-pip wget dnsutils && \
    apt-get -yqq clean

RUN pip3 install docker-compose pydevd

RUN mkdir -p /opt/epto/results

COPY build/libs/*-all.jar /opt/epto/
COPY scripts/*.sh scripts/*.py scripts/*.rb docker-compose.yml /opt/epto/
RUN chmod +x /opt/epto/container-start-script.sh

WORKDIR /opt/epto

CMD ["/opt/epto/container-start-script.sh"]
