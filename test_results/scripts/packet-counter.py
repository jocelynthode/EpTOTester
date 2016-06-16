#!/usr/bin/python
#running this script inside the folder of logs will calculate the number of packets transmitted over the network (based on logs from TCPdump).

import os
import re

count = 0 ()
files = [f for f in os.listdir('.') if os.path.isfile(f)]
for file in files:
    fileReGex = re.compile("node\d_ep\d*-lo\.txt")
    if fileReGex.match(file):
        with open(file) as f:
            for line in f:
                count += 1
    fileReGex = re.compile("node\d_ep\d*-eth0\.txt")
    if fileReGex.match(file):
        with open(file) as f:
            for line in f:
                count += 1

print(count)
