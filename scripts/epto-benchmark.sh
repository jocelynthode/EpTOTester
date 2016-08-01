#!/bin/bash
# for running the test and gathering the results
#

MANAGER_IP=192.168.1.40
PEER_NUMBER=$1


if [ -z "$PEER_NUMBER" ]
  then
    echo "you have to indicate number of peers"
    exit
fi

# TODO have a repo and pull from it for the image

echo "START..."
docker swarm init
TOKEN=$(docker swarm join-token -q worker)
parallel-ssh -h hosts "docker swarm join --token ${TOKEN} ${MANAGER_IP}:2377"

docker service create --name epto-tracker --network ingress --replicas 1 --limit-memory 180m tracker
docker service create --name epto-service --network ingress --replicas ${PEER_NUMBER} --limit-memory 180m --mount type=bind,source=/data,target=/data epto

#wait for apps to finish
for i in {1..40} :
do
	sleep 20s
    echo "waiting..."
done
#echo "waiting 2 more minutes"
#sleep 2m

docker service rm epto-service
docker service rm epto-tracker
# collect logs
#for i in $(docker ps -aqf "ancestor=epto");do  docker cp ${i}:/opt/epto/localhost.txt ./${i}_log.txt; done
parallel-ssh -h hosts "docker swarm leave"
docker swarm leave --force


#analyze results
echo "finished"
