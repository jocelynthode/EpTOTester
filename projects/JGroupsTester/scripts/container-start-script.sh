#!/usr/bin/env bash
# This script needs to run in the container
MY_IP_ADDR=$(/bin/hostname -i)
TRACKER_IP=$(dig +short jgroups-tracker)
echo 'Starting jgroup peer'
echo "${MY_IP_ADDR}"
echo "${PEER_NUMBER}"
echo "$TIME"
MY_IP_ADDR=($MY_IP_ADDR)
echo "${MY_IP_ADDR[0]}"
echo "${TRACKER_IP}[12001]"
echo "${TIME_TO_RUN}"
echo "${RATE}"
echo "${FIXED_RATE}"

dstat_pid=0
java_pid=0

signal_handler() {
    if [ $dstat_pid -ne 0 ]; then
        kill $dstat_pid
    fi
    if [ $java_pid -ne 0 ]; then
        kill -SIGSTOP $java_pid
    fi
    echo "KILLED PROCESSES"

    while true
    do
      tail -f /dev/null & wait ${!}
    done
}

trap 'signal_handler' SIGUSR1

dstat -n -N eth0 --output "/data/capture/${MY_IP_ADDR[0]}.csv" &
dstat_pid=${!}

java -Xms100m -Xmx260m -Djgroups.bind_addr="${MY_IP_ADDR[0]}" \
-Djgroups.tunnel.gossip_router_hosts="${TRACKER_IP}[12001]" -Djava.net.preferIPv4Stack=true \
-cp ./jgroups-tester-1.0-SNAPSHOT-all.jar -Dlogfile.name="${MY_IP_ADDR[0]}" EventTesterKt \
--rate "$RATE" --fixed-rate "$FIXED_RATE" "$PEER_NUMBER" "$TIME" "$TIME_TO_RUN" &
java_pid=${!}

wait ${java_pid}
kill ${dstat_pid}

