#!/usr/bin/env python3.5
import re
from pathlib import Path
from collections import namedtuple
import statistics
import argparse

Stats = namedtuple('Stats', ['start_at', 'end_at', 'duration', 'msg_sent', 'msg_received',
                             'balls_sent', 'balls_received'])

parser = argparse.ArgumentParser(description='Process EpTO logs')
parser.add_argument('peer_number', metavar='PEER_NUMBER', type=int,
                    help='the number of peer for an experiment')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse')
parser.add_argument('-c', metavar='CONSTANT', type=int, default=2,
                    help='the constant to find the minimum ratio we must have')
args = parser.parse_args()
PEER_NUMBER = args.peer_number
expected_ratio = 1 - (1 / (PEER_NUMBER**args.c))


# We must create our own iter because iter disables the tell function
def textiter(file):
    line = file.readline()
    while line:
        yield line
        line = file.readline()


def extract_stats(file):
    it = textiter(file)  # Force re-use of same iterator

    def match_line(regexp_str):
        result = 0
        for line in it:
            match = re.match(regexp_str, line)
            if match:
                result = int(match.group(1))
                break
        return result

    start_at = match_line(r'(\d+) - Sending:')

    # We want the last occurrence in the file
    def find_end():
        result = None
        pos = None
        for line in it:
            match = re.match(r'(\d+) - Delivered', line)
            if match:
                result = int(match.group(1))
                pos = file.tell()

        file.seek(pos)
        return textiter(file), result

    it, end_at = find_end()
    balls_sent = match_line(r'\d+ - Balls sent: (\d+)')
    balls_received = match_line(r'\d+ - Balls received: (\d+)')
    messages_sent = match_line(r'\d+ - Events sent: (\d+)')
    messages_received = match_line(r'\d+ - Events received: (\d+)')

    return Stats(start_at, end_at, end_at - start_at, messages_sent, messages_received, balls_sent, balls_received)


def all_stats():
    for file in args.files:
        with open(file, 'r') as f:
            file_stats = extract_stats(f)
        yield file_stats


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
average = statistics.mean(durations)
global_average = statistics.mean(global_times)

print("EpTO run with %d peers across %d experiments" % (PEER_NUMBER, experiments_nb))
print("------------------------")
print("Least time to deliver in total : %d ms" % mininum)
print("Most time to deliver in total : %d ms" % maximum)
print("Average time to deliver per peer in total: %d ms" % average)

messages_sent = [stat.msg_sent for stat in stats]
messages_received = [stat.msg_received for stat in stats]
balls_sent = [stat.balls_sent for stat in stats]
balls_received = [stat.balls_received for stat in stats]

sent_sum = sum(messages_sent)
received_sum = sum(messages_received)
ratios = [(msg_received / sent_sum) for msg_received in messages_received]
print("Total events sent: %d" % sent_sum)
print("Total events received on average: %d" % (received_sum / PEER_NUMBER))
print("Best ratio events received/sent: %f" % max(ratios))
print("Worst ratio events received/sent: %f" % min(ratios))
print("Total ratio events received/sent on average per peer : %f" % ((received_sum / PEER_NUMBER) / sent_sum))
if min(ratios) >= expected_ratio:
    print("All ratios satisfy the expected ratio of %f" % expected_ratio)
else:
    not_satisfying = 0
    for ratio in ratios:
        if ratio < expected_ratio:
            not_satisfying += 1
    print("%d peers didn't satisfy the expected ratio of %f" % (not_satisfying, expected_ratio))

balls_sent_sum = sum(balls_sent)
balls_received_sum = sum(balls_received)
print("Total balls sent across all peers: %d" % balls_sent_sum)
print("Total balls received across all peers: %d" % balls_received_sum)
print("Total ratio balls received/sent: %f" % (balls_received_sum / balls_sent_sum))

print("------------")
print("Average global time to deliver on all peers per experiment: %d ms" % global_average)
