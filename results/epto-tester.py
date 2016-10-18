#!/usr/bin/env python3.5
import re
import sys
from pathlib import Path
from collections import namedtuple
import argparse

Stats = namedtuple('Stats', ['start_at', 'end_at', 'duration', 'epto_sent', 'epto_received', 'bytes_tx', 'bytes_rx'])

parser = argparse.ArgumentParser(description='Process EpTO logs')
parser.add_argument('peer_number', metavar='PEER_NUMBER', type=int,
                    help='the number of peer for an experiment')
parser.add_argument('-g', '--glob-string', type=str, default='**/*.txt',
                    help='The glob string to search for log files')
args = parser.parse_args()
PEER_NUMBER = args.peer_number

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
    bytes_sent = match_line(r'(\d+) - Sent Bytes: (\d+)')
    bytes_received = match_line(r'(\d+) - Received Bytes: (\d+)')

    return Stats(start_at, end_at, end_at - start_at, messages_sent, messages_received, bytes_sent, bytes_received)


def all_stats():
    for fpath in Path().glob(args.glob_string):
        with fpath.open() as f:
            yield extract_stats(f)


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
bytes_sent = [stat.bytes_tx for stat in stats]
bytes_received = [stat.bytes_rx for stat in stats]

sent_sum = sum(messages_sent)
received_sum = sum(messages_received)
bytes_sent_sum = sum(bytes_sent)
bytes_received_sum = sum(bytes_received)
print("Total balls sent: %d" % sent_sum)
print("Total balls received: %d" % received_sum)
print("Total ratio balls received/sent: %f" % (received_sum / sent_sum))
print("Total bytes sent: %d" % bytes_sent_sum)
print("Total bytes received: %d" % bytes_received_sum)
try:
    print("Total ratio bytes received/sent %f" % (bytes_received_sum / bytes_sent_sum))
except ZeroDivisionError:
    pass
print("------------")
print("Average global time to deliver on all peers per experiment: %d ms" % global_average)
