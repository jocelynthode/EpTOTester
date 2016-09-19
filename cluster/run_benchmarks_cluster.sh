#!/usr/bin/env bash
# This scripts runs the benchmarks on a remote cluster

MANAGER_IP=172.16.2.17
PEER_NUMBER=$1


if [ -z "$PEER_NUMBER" ]
  then
    echo "you have to indicate number of peers"
    exit
fi


echo "START..."

# Clean everything at Ctrl+C
trap 'docker network rm epto-network && parallel-ssh -h hosts "docker swarm leave" \
&& docker swarm leave --force && exit' TERM INT

#docker pull swarm-m:5000/epto:latest
#docker pull swarm-m:5000/tracker:latest
#parallel-ssh -h hosts "docker pull swarm-m:5000/epto:latest && docker pull swarm-m:5000/tracker:latest"

docker swarm init
TOKEN=$(docker swarm join-token -q worker)
#parallel-ssh -h hosts "docker swarm join --token ${TOKEN} ${MANAGER_IP}:2377"

# If networking doesn't work use ingress
docker network create -d overlay --subnet=10.0.93.0/24 epto-network

# TODO use image from repo
docker service create --name epto-tracker --network epto-network --replicas 1 --limit-memory 180m tracker
docker service create --name epto-service --network epto-network --replicas ${PEER_NUMBER} --env "PEER_NUMBER=${PEER_NUMBER}" \
 --limit-memory 200m --mount type=bind,source=/home/jocelyn/epto-data,target=/data epto

#wait for apps to finish
for i in {1..20} :
do
	sleep 20s
    echo "waiting..."
done
#echo "waiting 2 more minutes"
#sleep 2m

docker service rm epto-service
docker service rm epto-tracker
docker network rm epto-network
# collect logs
#parallel-ssh -h hosts "docker swarm leave"
docker swarm leave --force

# TODO take from hosts
#for i (46 47); do
#    rsync -a -v "debian@172.16.2.${i}:~/data/" .
#done
echo "finished"
