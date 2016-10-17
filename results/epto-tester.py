#!/usr/bin/env python3.5
import re
import sys
from pathlib import Path
from collections import namedtuple

Stats = namedtuple('Stats', ['start_at', 'end_at', 'duration', 'epto_sent', 'epto_received'])
PEER_NUMBER = int(sys.argv[2])


def extract_stats(lines):
    it = iter(lines)  # Force re-use of same iterator

    def match_line(regexp_str):
        result = 0
        for line in it:
            match = re.match(regexp_str, line)
            if match:
                result = int(match.group(1))
                break
        return result

    start_at = match_line(r'(\d+) - Sending:')
    end_at = match_line(r'(\d+) - All events delivered !')
    messages_sent = match_line(r'\d+ - EpTO messages sent: (\d+)')
    messages_received = match_line(r'\d+ - EpTO messages received: (\d+)')

    return Stats(start_at, end_at, end_at - start_at, messages_sent, messages_received)


def all_stats():
    for fpath in Path().glob(sys.argv[1]):
        with fpath.open() as f:
            yield extract_stats(f)
qg

def global_time(experiment_nb, stats):
    for i in range(experiment_nb):
        start_index = i * PEER_NUMBER
        end_index = start_index + PEER_NUMBER
        tmp = stats[start_index:end_index]
        mininum_start = min([stat.start_at for stat in tmp])
        maximum_end = max([stat.end_at for stat in tmp])
        yield(maximum_end - mininum_start)


stats = list(all_stats())
experiments_nb = len(stats) // PEER_NUMBER
global_times = list(global_time(experiments_nb, stats))
durations = [stat.duration for stat in stats]
mininum = min(durations)
maximum = max(durations)
average = sum(durations) / len(durations)
global_average = sum(global_times) / len(global_times)

print("EpTO run with %d peers across %d experiments" % (PEER_NUMBER, experiments_nb))
print("------------------------")
print("Least time to deliver in total : %d ms" % mininum)
print("Most time to deliver in total : %d ms" % maximum)
print("Average time to deliver per peer in total: %d ms" % average)

messages_sent = [stat.epto_sent for stat in stats]
messages_received = [stat.epto_received for stat in stats]

sent_sum = sum(messages_sent)
received_sum = sum(messages_received)
print("Total balls sent: %d" % sent_sum)
print("Total balls received: %d" % received_sum)
print("Total ratio received/sent: %f" % (received_sum / sent_sum))

print("------------")
print("Average global time to deliver per experiment: %d ms" % global_average)
