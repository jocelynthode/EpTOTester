#!/usr/bin/env bash
# This scripts runs the benchmarks on a remote cluster

LOG_STORAGE=/home/jocelyn/tmp/data/
PEER_NUMBER=$1
DELTA=$2
TIME_ADD=$3
EVENTS_TO_SEND=$4
RATE=$5
CHURN=$6

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
trap 'docker service rm epto-service; docker service rm epto-tracker;kill ${churn_pid}; exit' TERM INT

docker swarm init && \
docker network create -d overlay --subnet=10.0.93.0/24 epto_network

docker service create --name epto-tracker --network epto_network --replicas 1 --limit-memory 300m \
 --constraint 'node.role == manager' tracker:latest

until docker service ls | grep "1/1"
do
    sleep 2s
done

TIME=$(( $(date +%s%3N) + $TIME_ADD ))
docker service create --name epto-service --network epto_network --replicas 0 \
--env "PEER_NUMBER=${PEER_NUMBER}" --env "DELTA=$DELTA" --env "TIME=$TIME" --env "EVENTS_TO_SEND=${EVENTS_TO_SEND}" \
--env "RATE=$RATE" --limit-memory 300m --restart-condition=none \
--mount type=bind,source=${LOG_STORAGE},target=/data epto:latest

# wait for service to start
while docker service ls | grep " 0/$PEER_NUMBER"
do
    sleep 1s
done
echo "Running EpTO tester..."

if [ -n "$CHURN" ]
then
    echo "Running churn"
    ./cluster/churn.py 5 -v --local --delay $(($TIME + 10000)) \
    --synthetic 0,${PEER_NUMBER} 1,1 1,1 1,1 1,1 1,1 1,1 1,1 1,1 1,1 1,1 &
    export churn_pid=$!

    # wait for service to be bigger than the limit below
    sleep 3m

    # wait for service to end
    until docker service ls | grep -q " 20/$(($PEER_NUMBER + 10))"
    do
        sleep 5s
    done
else
    docker service scale epto-service=${PEER_NUMBER}
    while docker service ls | grep -q " 0/$PEER_NUMBER"
    do
        sleep 5s
    done
    echo "Running without churn"
    # wait for service to end
    until docker service ls | grep -q " 0/$PEER_NUMBER"
    do
        sleep 5s
    done
fi

docker service rm epto-tracker
docker service rm epto-service

echo "Services removed"
