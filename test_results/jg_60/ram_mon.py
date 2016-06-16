import os
import re

node = []

for i in range(1, 5):
    n = []
    with open("node" + str(1) + "_node" + str(1) + ".txt") as f:
        for line in f:
            l = line.split()
            n.append(l[3])
    node.append(n)

avg = []
for i in range(0, len(node[1])):
    avg.append((int(node[0][i]) + int(node[1][i]) + int(node[2][i]) + int(node[3][i]))/4)

for i in avg:
    print(i)
