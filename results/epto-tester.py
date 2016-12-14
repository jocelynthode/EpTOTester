#!/usr/bin/env python3.5
import argparse
import csv
import multiprocessing
import logging
import re
import statistics
from collections import namedtuple
from enum import Enum

import numpy as np
import progressbar

Stats = namedtuple('Stats', ['state', 'start_at', 'end_at', 'duration', 'msg_sent', 'msg_received',
                             'balls_sent', 'balls_received'])


class State(Enum):
    perfect = 1
    late = 2
    dead = 3


parser = argparse.ArgumentParser(description='Process EpTO logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse (must be given by experiments)')
parser.add_argument('-c', '--constant', metavar='CONSTANT', type=int, default=2,
                    help='the constant to find the minimum ratio we must have')
parser.add_argument('-e', '--experiments-nb', metavar='EXPERIMENT_NB', type=int, default=1,
                    help='How many experiments were run')
parser.add_argument('-i', '--ignore-events', metavar='FILE', type=argparse.FileType('r'), nargs='+',
                    help='File containing unsent events due to churn (Given by check_order.py)')
args = parser.parse_args()
logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.INFO)
experiments_nb = args.experiments_nb
PEER_NUMBER = len(args.files) // experiments_nb
ignored_events = {}

if args.ignore_events:
    for idx, file in enumerate(args.ignore_events, 1):
        ignored_events[idx] = []
        for line in iter(file):
            match = re.match(r'.+TO IGNORE: (\[.+\])', line)
            if match:
                ignored_events[idx].append(match.group(1))

    print(ignored_events)

expected_ratio = 1 - (1 / (PEER_NUMBER ** args.constant))
k = ttl = delta = 0
local_deltas = []
events_delivered = {}


# We must create our own iter because iter disables the tell function
def textiter(file):
    line = file.readline()
    while line:
        yield line
        line = file.readline()


def extract_stats(file):
    it = textiter(file)  # Force re-use of same iterator
    experiment_nb = int(re.match(r'.*test-([0-9]+).*', file.name).group(1))

    def match_line(regexp_str):
        result = None
        for line in it:
            match = re.match(regexp_str, line)
            if match:
                result = int(match.group(1))
                break
        return result

    start_at = match_line(r'(\d+) - Sending:')
    file.seek(0)  # Start again
    events_delivered = {}
    local_deltas = []

    # We want the last occurrence in the file
    def find_end():
        result = None
        pos = file.tell()
        events_sent_count = 0
        state = State.perfect
        bls_sent = None
        bls_received = None
        for line in it:
            match = re.match(r'(\d+) - Delivered: (\[.+\])|(\d+) - Sending: (\[.+\])|'
                             r'.+ - Balls sent: (\d+)|.+ - Balls received: (\d+)|'
                             r'.+ - Time given was smaller than current time', line)
            if not match:
                continue
            if match.group(1):
                time = int(match.group(1))
                event = match.group(2)
                events_delivered[event] = time
                local_match = re.match(r'\d+ - Delivered: .+ -- Local Delta: (\d+)', line)
                if local_match:
                    delta = int(local_match.group(1)) / (10**6)
                    local_deltas.append(delta)
                result = int(match.group(1))
                pos = file.tell()
                continue
            elif match.group(3):
                if match.group(4) not in ignored_events[experiment_nb]:
                    events_sent_count += 1
                else:
                    print('Ignored Event!')
                    print(file)
                    print(match.group(4))
                continue
            elif match.group(5):
                bls_sent = int(match.group(5))
                continue
            elif match.group(6):
                bls_received = int(match.group(6))
                continue
            else:
                state = State.late

        file.seek(pos)
        return textiter(file), result, events_sent_count, bls_sent, bls_received, state

    it, end_at, evts_sent, tmp_balls_sent, tmp_balls_received, state = find_end()

    balls_sent = match_line(r'\d+ - Total Balls sent: (\d+)')
    # Only count complete peers
    if balls_sent is None:
        return Stats(State.dead, None, None, None, evts_sent,
                     None, tmp_balls_sent, tmp_balls_received), events_delivered, local_deltas
    balls_received = match_line(r'\d+ - Total Balls received: (\d+)')
    messages_sent = match_line(r'\d+ - Events sent: (\d+)')
    messages_received = match_line(r'\d+ - Events received: (\d+)')
    if state == State.late:
        return Stats(state, None, None, None, messages_sent,
                     None, balls_sent, balls_received), events_delivered, local_deltas
    else:
        print(state)
        print(messages_received)
        print(len(events_delivered))
        print(file)
        return Stats(state, start_at, end_at, end_at - start_at, messages_sent,
                     messages_received, balls_sent, balls_received), events_delivered, local_deltas


def all_stats(files):
    print('Importing files...')
    bar = progressbar.ProgressBar()
    file_stats = []
    local_deltas = []
    events_delivered = {}
    for file in bar(files):
        with open(file, 'r') as f:
            file_stat, events_delivered_temp, local_deltas_temp = extract_stats(f)
            file_stats.append(file_stat)
            for event, time in events_delivered_temp.items():
                if event in events_delivered:
                    events_delivered[event].append(time)
                else:
                    events_delivered[event] = [time]
            local_deltas += local_deltas_temp
    return file_stats, events_delivered, local_deltas


def global_time(experiment_nb, stats):
    for i in range(experiment_nb):
        start_index = i * perfect_length
        end_index = start_index + perfect_length
        tmp = stats[start_index:end_index]
        mininum_start = min([stat.start_at for stat in tmp])
        maximum_end = max([stat.end_at for stat in tmp])
        yield (maximum_end - mininum_start)


stats = []
chunk = list(map(lambda x: x.tolist(), np.array_split(np.array(args.files), 4)))

with open(args.files[0]) as file:
    it = iter(file)
    for line in it:
        match = re.match(r'\d+ - TTL: (\d+), K: (\d+)', line)
        if match:
            ttl = int(match.group(1))
            k = int(match.group(2))
            break

    for line in it:
        match = re.match(r'\d+ - Delta: (\d+)', line)
        if match:
            delta = int(match.group(1))
            break

with multiprocessing.Pool(processes=4) as pool:
    for result, events_delivered_stats, local_deltas_stats in pool.map(all_stats, chunk):
        stats += result
        local_deltas += local_deltas_stats
        # events_sent.update(events_sent_stats)
        for event, times in events_delivered_stats.items():
            if event in events_delivered:
                events_delivered[event] += times
            else:
                events_delivered[event] = times

perfect_stats = [stat for stat in stats if stat.state == State.perfect]
late_stats = [stat for stat in stats if stat.state == State.late]
dead_stats = [stat for stat in stats if stat.state == State.dead]

print([stat.msg_received for stat in perfect_stats])

stats_length = len(stats) / experiments_nb
perfect_length = len(perfect_stats) / experiments_nb
late_length = len(late_stats) / experiments_nb
dead_length = len(dead_stats) / experiments_nb

print(perfect_length, late_length, dead_length)

if not stats_length.is_integer() or not perfect_length.is_integer() \
        or not late_length.is_integer() or not dead_length.is_integer():
    raise ArithmeticError('Length should be an integer')

stats_length = int(stats_length)
perfect_length = int(perfect_length)
late_length = int(late_length)
dead_length = int(dead_length)

global_times = list(global_time(experiments_nb, perfect_stats))
durations = [stat.duration for stat in perfect_stats]
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
print("Population std fo the time to deliver: %f ms" % statistics.pstdev(durations, average))
print("Average global time to deliver on all peers per experiment: %d ms" % global_average)
print("Population std fo the time to deliver: %f ms" % statistics.pstdev(global_times, global_average))
print("-------------------------------------------")
messages_sent = [stat.msg_sent for stat in stats]
messages_received = [stat.msg_received for stat in perfect_stats]
balls_sent = [stat.balls_sent for stat in stats if stat.balls_sent]
balls_received = [stat.balls_received for stat in stats if stat.balls_received]
print(len(stats))
print(len(balls_sent))
print(len(balls_received))
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
stats_events_sent = []
for i in range(experiments_nb):
    start_index_sent = i * stats_length
    end_index_sent = start_index_sent + stats_length
    start_index_received = i * perfect_length
    end_index_received = start_index_received + perfect_length
    sent_sum = sum(messages_sent[start_index_sent:end_index_sent])
    messages_received_split = messages_received[start_index_received:end_index_received]
    stats_events_sent.append(sent_sum)
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
    print('Writing local times to csv file...')
    bar = progressbar.ProgressBar()
    for duration in bar(durations):
        writer.writerow({'local_time': duration})

with open('global-time-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['global_time'])
    writer.writeheader()
    print('Writing global times to csv file...')
    bar = progressbar.ProgressBar()
    for duration in bar(global_times):
        writer.writerow({'global_time': duration})

with open('local-delta-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['delta'])
    writer.writeheader()
    print('Writing local deltas to csv file...')
    bar = progressbar.ProgressBar()
    for delta in bar(local_deltas):
        writer.writerow({'delta': delta})

# with open('global-delta-stats.csv', 'w', newline='') as csvfile:
#     writer = csv.DictWriter(csvfile, ['delta'])
#     writer.writeheader()
#     print('Writing global deltas to csv file...')
#     bar = progressbar.ProgressBar()
#     for event, time in bar(events_sent.items()):
#         times = events_delivered[event]
#         deltas = [a_time - time for a_time in times]
#         for delta in deltas:
#             writer.writerow({'delta': delta})

with open('event-sent-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['events-sent'])
    writer.writeheader()
    print('Writing events sent to csv file...')
    bar = progressbar.ProgressBar()
    for event_sent in bar(stats_events_sent):
        writer.writerow({'events-sent': event_sent})
