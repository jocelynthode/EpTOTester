#!/usr/bin/env python3.5
import re
import sys
from pathlib import Path
from collections import namedtuple

Stats = namedtuple('Stats', ['start_at', 'end_at', 'duration', 'epto_sent', 'epto_received',
                             'epto_sent_bytes', 'epto_received_bytes'])


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
    bytes_sent = match_line(r'\d+ - EpTO bytes sent: (\d+)')
    bytes_received = match_line(r'\d+ - EpTO bytes received: (\d+)')

    return Stats(start_at, end_at, end_at - start_at, messages_sent, messages_received, bytes_sent, bytes_received)


def all_stats():
    for fpath in Path().glob(sys.argv[1]):
        with fpath.open() as f:
            yield extract_stats(f)

# TODO add global time between first send and last reception use start_at and end_at and fix problem when using more
# TODO than 1 experiment
stats = list(all_stats())
durations = [stat.duration for stat in stats]
mininum = min(durations)
maximum = max(durations)
average = sum(durations) / len(durations)


peer_number = int(sys.argv[2])
print("EpTO run with %d peers" % peer_number)
print("------------------------")
print("Least time to deliver: %d ms" % mininum)
print("Most time to deliver: %d ms" % maximum)
print("Average time to deliver: %d ms" % average)

messages_sent = [stat.epto_sent for stat in stats]
messages_received = [stat.epto_received for stat in stats]
bytes_sent = [stat.epto_sent_bytes for stat in stats]
bytes_received = [stat.epto_received_bytes for stat in stats]

sent_sum = sum(messages_sent)
received_sum = sum(messages_received)
print("Total balls sent: %d" % sent_sum)
print("Total bytes sent: %d" % sum(bytes_sent))
print("Total balls received: %d" % received_sum)
print("Total bytes received: %d" % sum(bytes_received))
print("Total ratio received/sent: %f" % (received_sum / sent_sum))
