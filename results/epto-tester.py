#!/usr/bin/env python3.5
import re
from pathlib import Path

length = 0


def extract_duration(lines):
    it = iter(lines)  # Force re-use of same iterator
    end_at = 0
    start_at = 0
    for line in it:
        match = re.match(r'(\d+) -  sending:', line)
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
    for fpath in Path().glob('172.*.txt'):
        global length
        length += 1
        with fpath.open() as f:
            yield extract_duration(f)


durations = list(all_durations())
mininum = min(durations)
maximum = max(durations)
average = sum(durations) / len(durations)

# print(durations)
print("EpTO run with %d peers" % length)
print("------------------------")
print("Least time to deliver: %d ms" % mininum)
print("Most time to deliver: %d ms" % maximum)
print("Average time to deliver: %d ms" % average)
