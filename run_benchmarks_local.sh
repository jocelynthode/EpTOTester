#!/usr/bin/env bash
# This scripts runs the benchmarks on a remote cluster

MANAGER_IP=172.16.2.98
PEER_NUMBER=$1
DELTA=$2


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


echo "START..."
./gradlew docker

# Clean everything at Ctrl+C
trap 'docker service rm epto-service && docker service rm epto-tracker && exit' TERM INT

docker swarm init && \
docker network create -d overlay --subnet=10.0.93.0/24 epto-network

docker service create --name epto-tracker --network epto-network --replicas 1 --limit-memory 300m tracker
sleep 10s
docker service create --name epto-service --network epto-network --replicas ${PEER_NUMBER} \
--env "PEER_NUMBER=${PEER_NUMBER}" --env "DELTA=$DELTA" \
--limit-memory 250m --log-driver=journald --restart-condition=none \
--mount type=bind,source=/home/jocelyn/tmp/data,target=/data epto

echo "Running EpTO tester..."
while true
do
    sleep 10s
done



