#!/usr/bin/env python3.5
"""
Author: Jocelyn Thode

* Check the order of EpTO delivered events, ignoring holes.
* Check for events that were possibly never sent to the cluster even if they appear in the logs
  due to churn. If such events exist it writes them to a file so that epto-tester.py can ignore them
"""
import argparse
import difflib
import logging
import re
from collections import namedtuple

import progressbar

Stats = namedtuple('Stats', ['events'])
is_out_of_order = False


class OutOfOrderException(Exception):
    """Raised when events aren't in order"""
    pass


parser = argparse.ArgumentParser(description='Process JGroups logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse')
parser.add_argument('--verbose', '-v', action='store_true',
                    help='Switch DEBUG logging on')
parser.add_argument('--name', '-n', type=str, help='Log file name', default='order')
args = parser.parse_args()
if args.verbose:
    log_level = logging.DEBUG
else:
    log_level = logging.INFO
logging.basicConfig(format='%(levelname)s: %(message)s', level=log_level, filename='{:s}.log'.format(args.name))


def all_events():
    logging.info('Importing files...')
    bar = progressbar.ProgressBar()
    for file in bar(args.files):
        with open(file, 'r') as f:
            uuids = extract_events(f)
        yield f.name, uuids


sent_events = []


def extract_events(file):
    events = []
    for line in iter(file):
        match = re.match(r'\d+ - Delivered: ([^\s]+)', line)
        if match:
            events.append(match.group(1))
        else:
            match = re.match(r'\d+ - Sending: (.+)', line)
            if match:
                sent_events.append(match.group(1))
    return Stats(events)


def find_holes(blocks, stats):
    old_a = blocks[0].a
    old_b = blocks[0].b
    old_size = blocks[0].size

    # Last element returned by get_matching_blocks() isn't important
    for match in blocks[1:-1]:
        step_a = (old_a + old_size)
        step_b = (old_b + old_size)
        # If we find a discrepancy check if it's only a hole
        if match.a == step_a:
            verify_if_mismatch(stats, complete_list, step_b, match.b)
        if match.b == step_b:
            verify_if_mismatch(complete_list, stats, step_a, match.a)
        old_a = match.a
        old_b = match.b
        old_size = match.size


def verify_if_mismatch(base_list, mismatched, step, new_value):
    logging.debug(base_list[step:new_value])
    for event in base_list[step:new_value]:
        if event in mismatched:
            logging.error("{:s} is mismatched".format(event))
            raise OutOfOrderException


events = {}
for name, stats in all_events():
    events[name] = stats.events

least_holes_file = max(events.items(), key=lambda a_tuple: len(a_tuple[1]))
complete_list = least_holes_file[1]
logging.debug('least_holes filename: {:s}'.format(least_holes_file[0]))

for name, stats in events.items():
    try:
        if complete_list == stats:
            logging.debug('{:s} and {:s} are ordered'.format(least_holes_file[0], name))
            logging.info('{:s} and {:s} have exactly the same events ({:d})'.format(least_holes_file[0], name,
                                                                                     len(complete_list)))
            continue
        sm = difflib.SequenceMatcher(None, complete_list, stats, False)
        blocks = list(sm.get_matching_blocks())
        logging.debug(blocks)
        find_holes(blocks, stats)

    except OutOfOrderException:
        is_out_of_order = True
        logging.error('File {:s} and File {:s} are not ordered'.format(least_holes_file[0], name))
        for diff in difflib.unified_diff(complete_list, stats):
            logging.error(diff)

if not is_out_of_order:
    logging.info('All files have the same order discarding holes!')

has_duplicate = False
for name, a_list in events.items():
    if len(set(a_list)) != len(a_list):
        has_duplicate = True
        logging.info('File {:s} has duplicates!'.format(name))

if not has_duplicate:
    logging.info('No files has any duplicate!')

# This part checks in case JGroups logs that it has sent an event but was interrupted before actually
# sending it
no_problem = False
a_list = [True for event_list in events.values() if len(event_list) == len(sent_events)]
print("A list: {}".format(a_list))
if a_list and all(a_list):
    no_problem = True

# There might be a problem
churn_problem = False
if not no_problem:
    logging.info('Checking for possible churn problem...')
    bar = progressbar.ProgressBar()
    for event in bar(sent_events):
        if not any(True for event_list in events.values() if event in event_list):
            churn_problem = True
            logging.info('TO IGNORE: {:s}'.format(event))
if no_problem or not churn_problem:
    logging.info('All events claimed to be sent were sent')
