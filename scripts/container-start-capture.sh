#!/usr/bin/env bash
# This script needs to run in the container
# inspired from a similar script from Sébastien Vaucher

MY_IP_ADDR=$(/bin/hostname -i)
MY_IP_ADDR=($MY_IP_ADDR)
trap 'kill $(jobs -p)' EXIT

filename="/data/capture/eptocapture_$(date +%s)_${MY_IP_ADDR[0]}.pcapng"
/opt/epto/container-start-script.sh &
epto_pid=$!
dumpcap -gq -f "udp port 10353" -i any -w ${filename} &
echo "Capturing packets..."
wait ${epto_pid}
