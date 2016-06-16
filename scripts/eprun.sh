#!/bin/bash
# run this script to log the packets sending to specific address of epto nodes
# and the node loobback (for epto peers running on the same node)
# also it uses ./mem.sh to log the memory status

tcpdump -i eth0 tcp and "(dst host 172.16.0.43 or dst host 172.16.0.48 or dst host 172.16.0.49)" > ep$1-eth0.txt &
tcpdump -i lo tcp > ep$1-lo.txt &
./mem.sh &

