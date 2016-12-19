#!/usr/bin/env bash
# This script needs to run in the container
MY_IP_ADDR=$(/bin/hostname -i)
echo "${MY_IP_ADDR}"
MY_IP_ADDR=($MY_IP_ADDR)
echo "${MY_IP_ADDR[0]}"
exec java -Xms100m -Xmx260m -Djava.net.preferIPv4Stack=true \
-cp ./jgroups-tester-1.0-SNAPSHOT-all.jar org.jgroups.stack.GossipRouter -bind_addr ${MY_IP_ADDR[0]} -port 12001
