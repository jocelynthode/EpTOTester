FROM ubuntu:14.04


# Add a repo where OpenJDK can be found.
RUN apt-get update && apt dist-upgrade -yqq
RUN apt-get install -yqq software-properties-common
RUN apt-get install -yqq dnsutils
RUN add-apt-repository -y ppa:openjdk-r/ppa
RUN apt-get update

# installing java8
RUN apt-get install -yqq openjdk-8-jdk

# installing gradle
# RUN add-apt-repository -y ppa:cwchien/gradle
# RUN apt-get update && apt-get install -yqq gradle

#installing ruby
RUN apt-get install -yqq ruby

ADD . /code
WORKDIR /code

# setup gradle
RUN ./gradlew --daemon clean shadowJar

RUN chmod +x /code/scripts/container-start-script.sh

RUN echo 'start App'
CMD ["/code/scripts/container-start-script.sh"]
