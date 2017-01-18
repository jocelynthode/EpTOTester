#!/usr/bin/env bash
# This script needs to run in the container

MY_IP_ADDR=$(/bin/hostname -i)

echo 'Starting Peer'
MY_IP_ADDR=($MY_IP_ADDR)
echo "${MY_IP_ADDR[0]}"

dstat_pid=0
app_pid=0

mkdir -p /data/capture

signal_handler() {
    curl "http://epto-tracker:4321/terminate"

    if [ $dstat_pid -ne 0 ]; then
        kill $dstat_pid
    fi

    if [ $app_pid -ne 0 ]; then
        kill $app_pid
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

java -Xms100m -Xmx210m -cp ./epto-1.0-SNAPSHOT-all.jar -Dlogfile.name="${MY_IP_ADDR[0]}" -Djava.net.preferIPv4Stack=true utilities.Main --delta "$DELTA" \
--rate "$RATE" -c "$CONSTANT" --fixed-rate "$FIXED_RATE" --churn-rate "$CHURN_RATE" --message-loss "$MESSAGE_LOSS" \
"${MY_IP_ADDR[0]}" "http://epto-tracker:4321" "${PEER_NUMBER}" "$TIME" "$TIME_TO_RUN" &
app_pid=${!}

wait ${app_pid}
kill ${dstat_pid}