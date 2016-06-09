#!/bin/bash
# for running the test and gathering the results
# 

containsElement() {
  local e
  for e in "${@:2}"; do [[ "$e" == "$1" ]] && return 0; done
  return 1
}

check_if_finish() {
	mapfile -t lines < <(docker ps -a)
	lines=("${lines[@]:1}")
	number_of_nodes=${#lines[@]}
	number_of_running_nodes=0
	for i in "${lines[@]}"
	do
		read -a line <<<$i
		if containsElement "Up" "${line[@]}"; then
		   ((number_of_running_nodes++))
		fi
	done
	finished_nodes=$(expr $number_of_nodes - $number_of_running_nodes)
	if (( $finished_nodes > 3 )); then 
		return 0
	fi
	return 1
}

if [ -z "$1" ]
  then
    echo "you have to indicate number of peers"
    exit
fi

cd /root/epto-neem
echo "rebuild the image"
docker-compose build
echo "clean up previous containers"
docker rm -f $(docker ps -a -q)
echo "START..."
COMPOSE_HTTP_TIMEOUT=200
export COMPOSE_HTTP_TIMEOUT
docker-compose up -d
docker-compose scale epto=$1

#wait for apps to finish
for i in {1..10} :
do
	sleep 1m
	if check_if_finish;then
		break
	else
		echo "waiting..."
	fi
done
echo "waiting 2 more minutes"
sleep 2m

# collect logs
mapfile -t lines < <(docker ps -a)
lines=("${lines[@]:1}")
for i in "${lines[@]}"
do
	read -a line <<<$i
	LENGTH=${#line[@]}
	LAST_POSITION=$((LENGTH - 1))

	docker cp "${line[0]}":/code/scripts/localhost.txt /root/epto-neem/scripts/"${line[$LAST_POSITION]}"_log.txt
done


# shutdown containers
docker-compose down

#analyze results
echo "finished"
