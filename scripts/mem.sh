#!/bin/bash
#using to log memory status of the machin.

addtime() {
    while IFS= read -r line; do
        echo "$(date +%s%N | cut -b1-13) $line"
    done
}

while true; do
	mapfile -t lines < <(free -m)
	lines=("${lines[@]:1}")
	echo "${lines[0]}" | addtime >> $(hostname).txt 2>&1
	sleep 1
done
