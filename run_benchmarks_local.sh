#!/usr/bin/env bash
# This scripts runs the benchmarks on a remote cluster

PEER_NUMBER=$1
DELTA=$2
TIME_ADD=$3

if [ -z "$PEER_NUMBER" ]
  then
    echo "you have to indicate number of peers"
    exit
fi

if [ -z "$DELTA" ]
  then
    echo "you have to indicate the EpTO delta"
    exit
fi

if [ -z "$TIME_ADD" ]
  then
    echo "you have to indicate by how much you want to delay EpTO start"
    exit
fi

echo "START..."
./gradlew docker

# Clean everything at Ctrl+C
trap 'docker service rm epto-service && docker service rm epto-tracker && exit' TERM INT

docker swarm init && \
docker network create -d overlay --subnet=10.0.93.0/24 epto-network

docker service create --name epto-tracker --network epto-network --replicas 1 --limit-memory 300m tracker
until docker service ls | grep "1/1"
do
    sleep 1s
done
TIME=$(( $(date +%s%3N) + $TIME_ADD ))
docker service create --name epto-service --network epto-network --replicas ${PEER_NUMBER} \
--env "PEER_NUMBER=${PEER_NUMBER}" --env "DELTA=$DELTA" --env "TIME=$TIME" \
--limit-memory 250m --log-driver=journald --restart-condition=none \
--mount type=bind,source=/home/jocelyn/tmp/data,target=/data epto

# wait for service to start
while docker service ls | grep " 0/$PEER_NUMBER"
do
    sleep 1s
done
echo "Running EpTO tester..."
# wait for service to end
until docker service ls | grep -q " 0/$PEER_NUMBER"
do
    sleep 5s
done

docker service rm epto-tracker
docker service rm epto-service
