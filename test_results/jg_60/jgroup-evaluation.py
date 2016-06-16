import os
import re

overall_delay = []
files = [f for f in os.listdir('.') if os.path.isfile(f)]
for file in files:
    sent = []
    delivered = []
    event_diffs = []
    fileReGex = re.compile("node\d_localhost\d*\.txt")
    if fileReGex.match(file):
        with open(file) as f:
            for line in f:
                l = line.split()
                if len(l) == 1:
                    continue
                if l[1] == 'Sent:':
                    sent.append([l[0], l[-1]])
                if l[1] == 'Delivered:':
                    delivered.append([l[0], l[-1]])

        for j in sent:
            for k in delivered:
                if j[1] == k[1]:
                    event_diffs.append(int(k[0]) - int(j[0]))
        if len(event_diffs) > 1:
            avg_event_diff = sum(event_diffs) / float(len(event_diffs))
            overall_delay.append(avg_event_diff)

total_average = sum(overall_delay) / float(len(overall_delay))
print(total_average)
