#!/usr/bin/env python3.5
"""
Author: Jocelyn Thode

This script extracts all useful data from the logs obtained by running JGroups benchmarks

It will create csv files to be used by dstat-parser.py as well as a console output that can be used to verify results
"""
import argparse
import csv
import logging
import multiprocessing
import re
import statistics
from collections import namedtuple
from enum import Enum

import numpy as np
import tqdm

Stats = namedtuple('Stats', ['state', 'start_at', 'end_at', 'duration', 'msg_sent', 'msg_received'])


class State(Enum):
    perfect = 1
    late = 2
    dead = 3


parser = argparse.ArgumentParser(description='Process JGroups logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse (must be given by experiments)')
parser.add_argument('-i', '--ignore-events', metavar='FILE', type=argparse.FileType('r'), nargs='+',
                    help='File containing unsent events due to churn (Given by check_order.py)')
args = parser.parse_args()
logging.basicConfig(format='%(message)s', level=logging.INFO)
ignored_events = {}

if args.ignore_events:
    for file in args.ignore_events:
        idx = int(re.match("test-(\d+)\.log", file.name).group(1))
        ignored_events[idx] = []
        for line in iter(file):
            match = re.match(r'.+TO IGNORE: (.+)', line)
            if match:
                ignored_events[idx].append(match.group(1))

    logging.info(ignored_events)

experiments_nb = len(list(ignored_events.keys()))
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
    experiment_nb = int(re.match(r'.*test-(\d+).*', file.name).group(1))
    def match_line(regexp_str):
        result = 0
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
        pos = None
        events_sent_count = 0
        state = State.perfect
        for line in it:
            match = re.match(r'(\d+) - Delivered: ([^\s]+)|(\d+) - Sending: (.+)|'
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
            elif match.group(3):
                if match.group(4) not in ignored_events[experiment_nb]:
                    events_sent_count += 1
            else:
                state = State.late

        file.seek(pos)
        return textiter(file), result, events_sent_count, state

    it, end_at, evts_sent, state = find_end()
    messages_sent = match_line(r'\d+ - Events sent: (\d+)')
    if not messages_sent:
        return Stats(State.dead, None, None, None, evts_sent, None), \
               events_delivered, local_deltas, experiment_nb
    messages_received = match_line(r'\d+ - Events received: (\d+)')

    if state == State.late:
        return Stats(state, None, None, None, messages_sent, None), \
               events_delivered, local_deltas, experiment_nb
    else:
        return Stats(state, start_at, end_at, end_at - start_at, messages_sent, messages_received), \
            events_delivered, local_deltas, experiment_nb


def all_stats(files):
    file_stats = {}
    local_deltas = []
    events_delivered = {}
    for file in files:
        with open(file, 'r') as f:
            file_stat, events_delivered_temp, local_deltas_temp, experiment_nb = extract_stats(f)
            if experiment_nb in file_stats:
                file_stats[experiment_nb].append(file_stat)
            else:
                file_stats[experiment_nb] = [file_stat]
            for event, time in events_delivered_temp.items():
                if event in events_delivered:
                    events_delivered[event].append(time)
                else:
                    events_delivered[event] = [time]
            local_deltas += local_deltas_temp
    return file_stats, events_delivered, local_deltas


def global_time(stats):
    for experiment_nb, the_stats in stats.items():
        perfect_stats = [stat for stat in the_stats if stat.state == State.perfect]
        mininum_start = min([stat.start_at for stat in perfect_stats])
        maximum_end = max([stat.end_at for stat in perfect_stats])
        yield (maximum_end - mininum_start)


stats = {}
chunk = list(map(lambda x: x.tolist(), np.array_split(np.array(args.files), 4)))
logging.info("Importing files...")
with multiprocessing.Pool(processes=4) as pool:
    for result, events_delivered_stats, local_deltas_stats in tqdm.tqdm(pool.imap_unordered(all_stats, chunk),
                                                                        total=len(chunk)):
        for key, value in result.items():
            if key in stats:
                stats[key] += value
            else:
                stats[key] = value

        local_deltas += local_deltas_stats
        for event, times in events_delivered_stats.items():
            if event in events_delivered:
                events_delivered[event] += times
            else:
                events_delivered[event] = times

global_times = list(global_time(stats))
messages_sent = {}
messages_received = {}
durations = []
for experiment_nb, the_stats in stats.items():
    messages_sent[experiment_nb] = []
    messages_received[experiment_nb] = []
    for stat in the_stats:
        messages_sent[experiment_nb].append(stat.msg_sent)
        if stat.state == State.perfect:
            messages_received[experiment_nb].append(stat.msg_received)
            durations.append(stat.duration)

mininum = min(durations)
maximum = max(durations)
average = statistics.mean(durations)
global_average = statistics.mean(global_times)

logging.info("JGroups")
logging.info("-------------------------------------------")
logging.info("Least time to deliver in total : {:d} ms".format(mininum))
logging.info("Most time to deliver in total : {:d} ms".format(maximum))
logging.info("Average time to deliver per peer in total: {:d} ms".format(average))
logging.info("Population std fo the time to deliver: {:f} ms" .format(statistics.pstdev(durations, average)))
logging.info("Average global time to deliver on all peers per experiment: {:d} ms".format(global_average))
logging.info("Population std fo the time to deliver: {:f} ms".format(statistics.pstdev(global_times, global_average)))
logging.info("-------------------------------------------")

all_delivered = []
stats_events_sent = []
for experiment_nb, the_stats in stats.items():
    sent_sum = sum(messages_sent[experiment_nb])
    received_sum = sum(messages_received[experiment_nb])
    stats_events_sent.append(sent_sum)
    logging.info("Experiment {:d}:" % experiment_nb)
    logging.info("Total events sent: {:d}" % sent_sum)
    logging.info("Total events received on average: {:f}".format(
        sum(received_sum) / len(messages_received[experiment_nb])))
    logging.info("-----------")
    ratios = [(msg_received / sent_sum) for msg_received in messages_received[experiment_nb]]
    logging.info("Best ratio events received/sent: {:.10g}".format(max(ratios)))
    logging.info("Worst ratio events received/sent: {:.10g}".format(min(ratios)))
    logging.info("Total ratio events received/sent on average per peer : {:.10g}".format((statistics.mean(ratios))))
    logging.info("-------------------------------------------")
    all_delivered.append(sent_sum == received_sum / len(messages_received[experiment_nb]))


def check_list_all_identical(lst):
    return not lst or [lst[0]] * len(lst) == lst


if check_list_all_identical(all_delivered):
    logging.info("All events sent were delivered in every experiments")
else:
    for idx, result in enumerate(all_delivered, 1):
        if not result:
            logging.info("Experiment {:d} didn't deliver every event sent".format(idx))

with open('local-time-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['local_time'])
    writer.writeheader()
    logging.info('Writing local times to csv file...')
    for duration in tqdm.tqdm(durations):
        writer.writerow({'local_time': duration})

with open('global-time-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['global_time'])
    writer.writeheader()
    logging.info('Writing global times to csv file...')
    for duration in tqdm.tqdm(global_times):
        writer.writerow({'global_time': duration})

with open('local-delta-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['delta'])
    writer.writeheader()
    logging.info('Writing local deltas to csv file...')
    for delta in tqdm.tqdm(local_deltas):
        writer.writerow({'delta': delta})

# with open('global-delta-stats.csv', 'w', newline='') as csvfile:
#     writer = csv.DictWriter(csvfile, ['delta'])
#     writer.writeheader()
#     logging.info('Writing global deltas to csv file...')
#     bar = progressbar.ProgressBar()
#     for event, time in bar(events_sent_time.items()):
#         times = events_delivered[event]
#         deltas = [a_time - time for a_time in times]
#         for delta in deltas:
#             writer.writerow({'delta': delta})

with open('event-sent-stats.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, ['events-sent'])
    writer.writeheader()
    logging.info('Writing events sent to csv file...')
    for event_sent in tqdm.tqdm(stats_events_sent):
        writer.writerow({'events-sent': event_sent})
