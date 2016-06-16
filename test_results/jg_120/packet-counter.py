import os
import re

count = 0
files = [f for f in os.listdir('.') if os.path.isfile(f)]
for file in files:
    fileReGex = re.compile("node\d_jg\d*-lo\.txt")
    if fileReGex.match(file):
        with open(file) as f:
            for line in f:
                count += 1
    fileReGex = re.compile("node\d_jg\d*-eth0\.txt")
    if fileReGex.match(file):
        with open(file) as f:
            for line in f:
                count += 1

print(count)
