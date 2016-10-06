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

# Clean everything at Ctrl+C
trap 'docker service rm epto-service && docker service rm epto-tracker && exit' TERM INT

docker pull swarm-m:5000/epto:latest
docker pull swarm-m:5000/tracker:latest

docker swarm init && \
TOKEN=$(docker swarm join-token -q worker) && \
parallel-ssh -t 0 -h hosts "docker swarm join --token ${TOKEN} ${MANAGER_IP}:2377" && \
docker network create -d overlay --subnet=172.28.0.0/16 epto-network

for i in {1..10}
do
    echo "Running EpTO tester $PEER_NUMBER peers - $i"
    docker service create --name epto-tracker --network epto-network --replicas 1 --limit-memory 350m swarm-m:5000/tracker
    docker service create --name epto-service --network epto-network --replicas ${PEER_NUMBER} \
    --env "PEER_NUMBER=${PEER_NUMBER}" \
    --limit-memory 250m --log-driver=journald --restart-condition=none \
    --mount type=bind,source=/home/debian/data,target=/data swarm-m:5000/epto

    sleep 5m
    docker service rm epto-service && docker service rm epto-tracker || exit
    echo "Removed services"
    sleep 1m
    while read ip; do
        rsync --remove-source-files -av "${ip}:~/data/*.txt" "../data/test-$i/"
    done <hosts
    mv ../data/*.txt "../data/test-$i"
done

