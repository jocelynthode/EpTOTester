#!/bin/bash
# for running the test and gathering the results
#

PEER_NUMBER=$1

check_if_finish() {
    if [ $(docker ps -aqf "status=exited" | wc -l) == "$PEER_NUMBER" ]
        then
            return 0
        else
            return 1
    fi

}

if [ -z "$PEER_NUMBER" ]
  then
    echo "you have to indicate number of peers"
    exit
fi


echo "rebuild the image"
docker-compose build
echo "clean up previous containers"
if [ -n "$(docker ps -a -q)" ]
    then
        docker rm -f $(docker ps -a -q)
fi
echo "START..."
COMPOSE_HTTP_TIMEOUT=200
export COMPOSE_HTTP_TIMEOUT
docker-compose up -d
docker-compose scale epto=${PEER_NUMBER}

#wait for apps to finish
for i in {1..40} :
do
	sleep 20s
	if check_if_finish;then
		break
	else
		echo "waiting..."
	fi
done
#echo "waiting 2 more minutes"
#sleep 2m

# collect logs
for i in $(docker ps -aqf "ancestor=eptoneem_epto");do  docker cp ${i}:/opt/epto/localhost.txt ./${i}_log.txt; done


# shutdown containers
docker-compose down

#analyze results
echo "finished"
