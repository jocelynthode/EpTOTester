#!/usr/bin/env bash
# This scripts runs the benchmarks on a remote cluster

PEER_NUMBER=$1
DELTA=$2
TIME_ADD=$3
EVENTS_TO_SEND=$4
RATE=$5

if [ -z "$PEER_NUMBER" ]
  then
    echo "You have to indicate number of peers"
    exit
fi

if [ -z "$DELTA" ]
  then
    echo "You have to indicate the EpTO delta"
    exit
fi

if [ -z "$TIME_ADD" ]
  then
    echo "you have to indicate by how much you want to delay EpTO start in ms"
    exit
fi

if [ -z "$EVENTS_TO_SEND" ]
  then
    echo "You have to indicate how many events you want to send in total per peers"
    exit
fi

if [ -z "$RATE" ]
  then
    echo "You have to indicate at which rate you want to send events on each peers in ms"
    exit
fi

echo "START..."
./gradlew docker

# Clean everything at Ctrl+C
trap 'docker service rm epto-service && docker service rm epto-tracker && exit' TERM INT

docker swarm init && \
docker network create -d overlay --subnet=10.0.93.0/24 epto-network

docker service create --name epto-tracker --network epto-network --replicas 1 --limit-memory 300m \
 --constraint 'node.role == manager' tracker:latest

until docker service ls | grep "1/1"
do
    sleep 2s
done
TIME=$(( $(date +%s%3N) + $TIME_ADD ))
docker service create --name epto-service --network epto-network --replicas ${PEER_NUMBER} \
--env "PEER_NUMBER=${PEER_NUMBER}" --env "DELTA=$DELTA" --env "TIME=$TIME" --env "EVENTS_TO_SEND=${EVENTS_TO_SEND}" \
--env "RATE=$RATE" --limit-memory 250m --log-driver=journald --restart-condition=none \
--mount type=bind,source=/home/jocelyn/tmp/data,target=/data epto:latest

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
