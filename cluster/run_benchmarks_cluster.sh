#!/usr/bin/env bash
# This scripts runs the benchmarks on a remote cluster

MANAGER_IP=172.16.2.119
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

function getlogs {
    while read ip; do
        rsync --remove-source-files -av "${ip}:~/data/" ../data/
    done <hosts
}

echo "START..."

trap 'getlogs && exit' TERM INT

docker pull swarm-m:5000/epto:latest
docker pull swarm-m:5000/tracker:latest

docker swarm init && \
(TOKEN=$(docker swarm join-token -q worker) && \
parallel-ssh -t 0 -h hosts "docker swarm join --token ${TOKEN} ${MANAGER_IP}:2377" && \
docker network create -d overlay --subnet=172.103.0.0/16 epto_network || exit)


docker service create --name epto-tracker --network epto_network --replicas 1 --limit-memory 300m \
 --constraint 'node.role == manager' swarm-m:5000/tracker:latest

until docker service ls | grep "1/1"
do
    sleep 2s
done
TIME=$(( $(date +%s%3N) + $TIME_ADD ))
docker service create --name epto-service --network epto_network --replicas ${PEER_NUMBER} \
--env "PEER_NUMBER=${PEER_NUMBER}" --env "DELTA=$DELTA" --env "TIME=$TIME" \
--limit-memory 250m --log-driver=journald --restart-condition=none \
--mount type=bind,source=/home/debian/data,target=/data swarm-m:5000/epto:latest

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

echo "Services removed"
getlogs
