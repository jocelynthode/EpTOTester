#!/usr/bin/env bash
# This script needs to run in the container

MY_IP_ADDR=$(/bin/hostname -i)

# wait for all peers
sleep 30s

echo 'Starting epto peer'
echo "${MY_IP_ADDR}"
echo "${PEER_NUMBER}"
MY_IP_ADDR=($MY_IP_ADDR)
echo "${MY_IP_ADDR[0]}"
java -Xms150m -Xmx330m -cp ./epto-1.0-SNAPSHOT-all.jar epto.utilities.Main "${MY_IP_ADDR[0]}" "http://epto-tracker:4321" "${PEER_NUMBER}" > "/data/${MY_IP_ADDR[0]}.txt" 2>&1
