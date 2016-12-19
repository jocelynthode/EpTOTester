#!/usr/bin/env bash
# This script needs to run in the container

MY_IP_ADDR=$(/bin/hostname -i)

echo 'Starting epto peer'
MY_IP_ADDR=($MY_IP_ADDR)
echo "${MY_IP_ADDR[0]}"
echo "${PEER_NUMBER}"
echo "$DELTA"
echo "$TIME"
echo "${TIME_TO_RUN}"
echo "$RATE"
echo "$CONSTANT"
echo "$FIXED_RATE"
echo "$CHURN_RATE"
echo "$MESSAGE_LOSS"

dstat_pid=0
java_pid=0

signal_handler() {
    curl "http://epto-tracker:4321/terminate"

    if [ $dstat_pid -ne 0 ]; then
        kill $dstat_pid
    fi

    if [ $java_pid -ne 0 ]; then
        kill $java_pid
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

java -Xms100m -Xmx210m -cp ./epto-1.0-SNAPSHOT-all.jar -Dlogfile.name="${MY_IP_ADDR[0]}" utilities.Main --delta "$DELTA" \
--rate "$RATE" -c "$CONSTANT" --fixed-rate "$FIXED_RATE" --churn-rate "$CHURN_RATE" --message-loss "$MESSAGE_LOSS" \
"${MY_IP_ADDR[0]}" "http://epto-tracker:4321" "${PEER_NUMBER}" "$TIME" "$TIME_TO_RUN" &
java_pid=${!}

wait ${java_pid}
kill ${dstat_pid}