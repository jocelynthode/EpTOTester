#!/usr/bin/env python3.5
import re
import sys
from pathlib import Path


def extract_duration(lines):
    it = iter(lines)  # Force re-use of same iterator
    end_at = 0
    start_at = 0
    for line in it:
        match = re.match(r'(\d+) - Sending:', line)
        if match:
            start_at = int(match.group(1))
            break
    for line in it:
        match = re.match(r'(\d+) - All events delivered !', line)
        if match:
            end_at = int(match.group(1))
            break
    return end_at - start_at


def all_durations():
    for fpath in Path().glob(sys.argv[1]+'/**/172.*.txt'):
        with fpath.open() as f:
            yield extract_duration(f)


def extract_balls(lines):
    messages_sent = 0
    messages_received = 0
    it = iter(lines)
    for line in it:
        match = re.match(r'\d+ - EpTO messages sent: (\d+)',line)
        if match:
            messages_sent += int(match.group(1))
            break
    for line in it:
        match = re.match(r'\d+ - EpTO messages received: (\d+)',line)
        if match:
            messages_received += int(match.group(1))
            break
    return messages_sent, messages_received


def count_balls():
    for fpath in Path().glob(sys.argv[1]+'/**/172.*.txt'):
        with fpath.open() as f:
            yield extract_balls(f)

durations = list(all_durations())
mininum = min(durations)
maximum = max(durations)
average = sum(durations) / len(durations)

# print(durations)
peer_number = int(sys.argv[2])
print("EpTO run with %d peers" % peer_number)
print("------------------------")
print("Least time to deliver: %d ms" % mininum)
print("Most time to deliver: %d ms" % maximum)
print("Average time to deliver: %d ms" % average)

balls = list(count_balls())
balls_sent, balls_received = zip(*balls)
balls_received = list(balls_received)
balls_sent = list(balls_sent)
sent_sum= sum(balls_sent)
received_sum = sum(balls_received)

print("Total balls sent: %d" % sent_sum)
print("Total balls received: %d" % received_sum)
print("Total ratio received/sent: %f" % (received_sum / sent_sum))
