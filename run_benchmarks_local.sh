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
trap 'docker service rm epto-service && docker service rm epto-tracker && docker network rm epto-network && \
docker swarm leave --force && exit' TERM INT

docker swarm init

# If networking doesn't work use ingress
docker network create -d overlay --subnet=10.0.93.0/24 epto-network

docker service create --name epto-tracker --network epto-network --replicas 1 --limit-memory 370m tracker
docker service create --name epto-service --network epto-network --replicas ${PEER_NUMBER} --env "PEER_NUMBER=${PEER_NUMBER}" \
--limit-memory 370m --log-driver=journald --mount type=bind,source=/home/jocelyn/tmp/data,target=/data epto

echo "Fleshing out the network..."
sleep 10s

#wait for apps to finish
for i in {1..60} :
do
	sleep 20s
    echo "waiting..."
done

docker service rm epto-service
docker service rm epto-tracker
docker network rm epto-network
docker swarm leave --force


#while read ip; do
#    rsync -av ${ip}:~/data/ ../data/
#done <hosts
