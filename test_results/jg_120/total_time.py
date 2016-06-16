import os
import re

overall_times = []
files = [f for f in os.listdir('.') if os.path.isfile(f)]
for file in files:
    ff = []
    fileReGex = re.compile("node\d_localhost\d*\.txt")
    if fileReGex.match(file):
        with open(file) as f:
            for line in f:
                ff .append(line.split())
        overall_times.append(int(ff[-1][0]) - int(ff[0][0]))

average_time = 0
for i in overall_times:
    average_time += i
average_time /= len(overall_times)
print(average_time)
