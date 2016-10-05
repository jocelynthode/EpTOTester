#!/usr/bin/env bash
# This scripts runs the benchmarks on a remote cluster

MANAGER_IP=172.16.2.53
PEER_NUMBER=$1


if [ -z "$PEER_NUMBER" ]
  then
    echo "you have to indicate number of peers"
    exit
fi


echo "START..."
./gradlew docker

# Clean everything at Ctrl+C
trap 'docker service rm epto-service && docker service rm epto-tracker && exit' TERM INT

docker swarm init && \
docker network create -d overlay --subnet=10.0.93.0/24 epto-network

docker service create --name epto-tracker --network epto-network --replicas 1 --limit-memory 300m tracker
docker service create --name epto-service --network epto-network --replicas ${PEER_NUMBER} \
--env "PEER_NUMBER=${PEER_NUMBER}" --limit-memory 200m --log-driver=journald \
--restart-condition=none --stop-grace-period=30s --mount type=bind,source=/home/jocelyn/tmp/data,target=/data epto

echo "Running EpTO tester..."
while true
do
    sleep 10s
done



