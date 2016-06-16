#!/usr/bin/python
#running this script inside the folder of logs will check if the delivered events on all peers are the same. If there's an error, it will print error.

import os
import re

number_1 = []
with open("node1_localhost1.txt") as f:
    for line in f:
        l = line.split()
        if len(l) <= 2:
            continue
        if l[1] == 'Delivered:':
            number_1.append(l[-1])

files = [f for f in os.listdir('.') if os.path.isfile(f)]
for file in files:
    tmp = []
    fileReGex = re.compile("node\d_localhost\d*\.txt")
    if fileReGex.match(file):
        with open(file) as f:
            for line in f:
                l = line.split()
                if len(l) <= 2:
                    continue
                if l[1] == 'Delivered:':
                    tmp.append(l[-1])
        if tmp != number_1:
            print("error")
            break;
