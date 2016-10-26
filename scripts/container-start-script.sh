#!/usr/bin/env bash
# This script needs to run in the container

MY_IP_ADDR=$(/bin/hostname -i)

echo 'Starting epto peer'
MY_IP_ADDR=($MY_IP_ADDR)
echo "${MY_IP_ADDR[0]}"
echo "${PEER_NUMBER}"
echo "$DELTA"
echo "$TIME"
echo "${EVENTS_TO_SEND}"
echo "$RATE"

dstat -n -N eth0 --output "/data/capture/${MY_IP_ADDR[0]}.csv" &
dstat_pid=$!
java -Xms100m -Xmx210m -cp ./epto-1.0-SNAPSHOT-all.jar -Dlogfile.name="${MY_IP_ADDR[0]}" utilities.Main --delta "$DELTA" \
--events "${EVENTS_TO_SEND}" --rate "$RATE" "${MY_IP_ADDR[0]}" "http://epto-tracker:4321" "${PEER_NUMBER}" "$TIME"
kill ${dstat_pid}