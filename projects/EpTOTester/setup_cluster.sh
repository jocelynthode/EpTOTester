#!/usr/bin/env bash
# This scripts  will build the images, push them to the repository run the tests
# Credits : https://github.com/sebyx31/ErasureBench/blob/master/projects/erasure-tester/benchmark_on_cluster.sh

MANAGER_IP=172.16.2.119

trap "kill ${ssh_pid}" TERM INT

./gradlew docker

ssh -N -L 5000:localhost:5000 debian@${MANAGER_IP} &
ssh_pid=$!

sleep 5s

docker tag epto:latest localhost:5000/epto:latest
docker tag epto-tracker:latest localhost:5000/epto-tracker:latest
docker push localhost:5000/epto:latest
docker push localhost:5000/epto-tracker:latest


kill ${ssh_pid}

rsync -av --copy-links cluster/ debian@${MANAGER_IP}:~/epto