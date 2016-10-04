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
trap 'docker service rm epto-service && docker service rm epto-tracker && \
parallel-ssh -t 180 -h hosts "docker swarm leave" && docker network rm epto-network && \
docker swarm leave --force && exit' TERM INT

docker pull swarm-m:5000/epto:latest
docker pull swarm-m:5000/tracker:latest

docker swarm init
TOKEN=$(docker swarm join-token -q worker)
parallel-ssh -t 0 -h hosts "docker swarm join --token ${TOKEN} ${MANAGER_IP}:2377"

# If networking doesn't work use ingress
docker network create -d overlay --subnet=172.28.0.0/16 epto-network

docker service create --name epto-tracker --network epto-network --replicas 1 --limit-memory 350m swarm-m:5000/tracker
docker service create --name epto-service --network epto-network --replicas ${PEER_NUMBER} --env "PEER_NUMBER=${PEER_NUMBER}" \
--limit-memory 250m --log-driver=journald --mount type=bind,source=/home/debian/data,target=/data swarm-m:5000/epto

echo "Fleshing out the network..."
sleep 20s

#wait for apps to finish
for i in {1..185} :
do
	sleep 20s
    echo "waiting..."
done

docker service rm epto-service
docker service rm epto-tracker
docker network rm epto-network
parallel-ssh -t 0 -h hosts "docker swarm leave"
docker swarm leave --force


#while read ip; do
#    rsync -av ${ip}:~/data/ ../data/
#done <hosts
