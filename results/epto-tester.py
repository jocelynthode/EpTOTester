#!/usr/bin/env python3.5
import argparse
import csv
import re
import statistics
from collections import namedtuple
from enum import Enum

Stats = namedtuple('Stats', ['state', 'start_at', 'end_at', 'duration', 'msg_sent', 'msg_received',
                             'balls_sent', 'balls_received'])


class State(Enum):
    perfect = 1
    late = 2
    dead = 3

parser = argparse.ArgumentParser(description='Process EpTO logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse')
parser.add_argument('-c', '--constant', metavar='CONSTANT', type=int, default=2,
                    help='the constant to find the minimum ratio we must have')
parser.add_argument('-e', '--experiments-nb', metavar='EXPERIMENT_NB', type=int, default=1,
                    help='How many experiments were run')
args = parser.parse_args()
experiments_nb = args.experiments_nb
PEER_NUMBER = len(args.files) // experiments_nb
CHURN_NUMBER = 17

expected_ratio = 1 - (1 / (PEER_NUMBER ** args.constant))
k = ttl = delta = 0
events_sent = {}
events_delivered = {}

# We must create our own iter because iter disables the tell function
def textiter(file):
    line = file.readline()
    while line:
        yield line
        line = file.readline()


def extract_stats(file):
    global k, ttl, delta
    it = textiter(file)  # Force re-use of same iterator

    for line in it:
        match = re.match(r'\d+ - TTL: (\d+), K: (\d+)', line)
        if match:
            ttl = int(match.group(1))
            k = int(match.group(2))
            break

    def match_line(regexp_str):
        result = 0
        for line in it:
            match = re.match(regexp_str, line)
            if match:
                result = int(match.group(1))
                break
        return result

    delta = match_line(r'\d+ - Delta: (\d+)')
    start_at = match_line(r'(\d+) - Sending:')
    file.seek(0)  # Start again

    # We want the last occurrence in the file
    def find_end():
        result = None
        pos = None
        events_sent_count = 0
        state = State.perfect
        for line in it:
            match = re.match(r'(\d+) - Delivered: (\[.+\])', line)
            if match:
                time = int(match.group(1))
                event = match.group(2)
                if event in events_delivered:
                    events_delivered[event].append(time)
                else:
                    events_delivered[event] = [time]
                result = int(match.group(1))
                pos = file.tell()
                continue
            match = re.match(r'(\d+) - Sending: (\[.+\])', line)
            if match:
                events_sent[match.group(2)] = int(match.group(1))
                events_sent_count += 1
                continue
            if re.match(r'.+ - Time given was smaller than current time', line):
                state = State.late

        file.seek(pos)
        return textiter(file), result, events_sent_count, state

    it, end_at, evts_sent, state = find_end()
    balls_sent = match_line(r'\d+ - Balls sent: (\d+)')
    # Only count complete peers
    if not balls_sent:
        return Stats(State.dead, start_at, end_at, end_at - start_at, evts_sent,
                     None, None, None)
    balls_received = match_line(r'\d+ - Balls received: (\d+)')
    messages_sent = match_line(r'\d+ - Events sent: (\d+)')
    messages_received = match_line(r'\d+ - Events received: (\d+)')

    return Stats(state, start_at, end_at, end_at - start_at, messages_sent,
                 messages_received, balls_sent, balls_received)


def all_stats():
    for file in args.files:
        with open(file, 'r') as f:
            file_stats = extract_stats(f)
        yield file_stats


def global_time(experiment_nb, stats):
    for i in range(experiment_nb):
        start_index = i * perfect_length
        end_index = start_index + perfect_length
        tmp = stats[start_index:end_index]
        mininum_start = min([stat.start_at for stat in tmp])
        maximum_end = max([stat.end_at for stat in tmp])
        yield (maximum_end - mininum_start)


stats = list(all_stats())
perfect_stats = [stat for stat in stats if stat.state == State.perfect]
late_stats = [stat for stat in stats if stat.state == State.late]
dead_stats = [stat for stat in stats if stat.state == State.dead]

stats_length = len(stats) / experiments_nb
perfect_length = len(perfect_stats) / experiments_nb
late_length = len(late_stats) / experiments_nb
dead_length = len(dead_stats) / experiments_nb

if not stats_length.is_integer() or not perfect_length.is_integer() \
        or not late_length.is_integer() or not dead_length.is_integer():
    raise ArithmeticError('Length should be an integer')

stats_length = int(stats_length)
perfect_length = int(perfect_length)
late_length = int(late_length)
dead_length = int(dead_length)

global_times = list(global_time(experiments_nb, perfect_stats))
durations = [stat.duration for stat in perfect_stats if stat.duration]
mininum = min(durations)
maximum = max(durations)
average = statistics.mean(durations)
global_average = statistics.mean(global_times)

print("EpTO run with initially %d peers across %d experiments" % (perfect_length + dead_length, experiments_nb))
print("K=%d / TTL=%d / Delta=%dms" % (k, ttl, delta))
print("Churn -> Peers created: {:d}, Peers killed {:d} in each experiment".format(late_length, dead_length))
print("-------------------------------------------")
print("Least time to deliver in total : %d ms" % mininum)
print("Most time to deliver in total : %d ms" % maximum)
print("Average time to deliver per peer in total: %d ms" % average)
print("Population std fo the time to deliver: %f ms" % statistics.pstdev(durations))
print("Average global time to deliver on all peers per experiment: %d ms" % global_average)
print("Population std fo the time to deliver: %f ms" % statistics.pstdev(global_times))
print("-------------------------------------------")
messages_sent = [stat.msg_sent for stat in stats if stat.msg_sent]
messages_received = [stat.msg_received for stat in perfect_stats if stat.msg_received]
balls_sent = [stat.balls_sent for stat in stats if stat.balls_sent]
balls_received = [stat.balls_received for stat in stats if stat.balls_received]
print("-----------")
balls_sent_sum = sum(balls_sent)
balls_received_sum = sum(balls_received)
print("Total balls sent across all peers alive at the end: %d" % balls_sent_sum)
print("Total balls received across all peers alive at the end: %d" % balls_received_sum)
print("Total ratio balls received/sent: %f" % (balls_received_sum / balls_sent_sum))
print("-----------")
average_balls_sent = balls_sent_sum / experiments_nb
average_balls_received = balls_received_sum / experiments_nb
print("Average balls sent per experiment (for peers alive at the end): %f" % average_balls_sent)
print("Average balls received per experiment (for peers alive at the end): %f" % average_balls_received)
print("Average ratio balls received/sent per experiment: %f" % (average_balls_received / average_balls_sent))
print("-------------------------------------------")
for i in range(experiments_nb):
    start_index_sent = i * stats_length
    end_index_sent = start_index_sent + stats_length
    start_index_received = i * perfect_length
    end_index_received = start_index_received + perfect_length
    sent_sum = sum(messages_sent[start_index_sent:end_index_sent])
    messages_received_split = messages_received[start_index_received:end_index_received]
    print("Experiment %d:" % (i + 1))
    print("Total events sent: %d" % sent_sum)
    print("Total events received on average: %f"
          % (sum(messages_received_split) / perfect_length))
    print("-----------")
    ratios = [(msg_received / sent_sum) for msg_received in messages_received_split]
    print("Best ratio events received/sent: %.10g" % max(ratios))
    print("Worst ratio events received/sent: %.10g" % min(ratios))
    print("Total ratio events received/sent on average per peer : %.10g" % (statistics.mean(ratios)))
    print("-----------")
    if min(ratios) >= expected_ratio:
        print("All ratios satisfy the expected ratio of %.10g" % expected_ratio)
    else:
        not_satisfying = 0
        for ratio in ratios:
            if ratio < expected_ratio:
                not_satisfying += 1
        print("%d peers didn't satisfy the expected ratio of %.10g"
              % (not_satisfying, expected_ratio))
    start_index_finished = i * (perfect_length + late_length)
    end_index_finished = start_index_finished + (perfect_length + late_length)
    balls_sent_exp = sum(balls_sent[start_index_finished:end_index_finished])
    balls_received_exp = sum(balls_received[start_index_finished:end_index_finished])
    print("Total balls sent: %d" % balls_sent_exp)
    print("Total balls received: %d" % balls_received_exp)
    print("Total ratio balls received/sent: %f" % (balls_received_exp / balls_sent_exp))
    print("-------------------------------------------")

with open('local-time-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['local_time'])
    writer.writeheader()
    for duration in durations:
        writer.writerow({'local_time': duration})

with open('global-time-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['global_time'])
    writer.writeheader()
    for duration in global_times:
        writer.writerow({'global_time': duration})

with open('delta-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['delta'])
    writer.writeheader()
    for event, time in events_sent.items():
        times = events_delivered[event]
        deltas = [a_time - time for a_time in times]
        for delta in deltas:
            writer.writerow({'delta': delta})

