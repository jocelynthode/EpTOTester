#!/usr/bin/env python3.5
import re
from collections import namedtuple
import argparse
import difflib
import logging

Stats = namedtuple('Stats', ['events'])


class OutOfOrderException(Exception):
    """Raised when events aren't in order"""
    pass

parser = argparse.ArgumentParser(description='Process EpTO logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse')
parser.add_argument('--verbose', '-v', action='store_true',
                    help='Switch DEBUG logging on')
args = parser.parse_args()
if args.verbose:
    log_level = logging.DEBUG
else:
    log_level = logging.INFO
logging.basicConfig(format='%(levelname)s: %(message)s', level=log_level)

def all_events():
    for file in args.files:
        with open(file, 'r') as f:
            uuids = extract_events(f)
        yield f.name, uuids


def extract_events(file):
    events = []
    for line in iter(file):
        match = re.match(r'\d+ - Delivered: (\[.+\])', line)
        if match:
            events.append(match.group(1))
    return Stats(events)


def find_holes(blocks, stats):
    old_a = blocks[0].a
    old_b = blocks[0].b
    old_size = blocks[0].size

    # Last element returned by get_matching_blocks() isn't important
    for idx, match in enumerate(blocks[1:-1]):
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
            raise OutOfOrderException

events = {}
for name, stats in all_events():
    events[name] = stats.events

least_holes_file = max(events.items(), key=lambda a_tuple:  len(a_tuple[1]))
complete_list = least_holes_file[1]
logging.debug('least_holes filename: {:s}'.format(least_holes_file[0]))

for name, stats in events.items():
    try:
        if complete_list == stats:
            logging.debug('{:s} and {:s} are ordered'.format(least_holes_file[0], name))
            continue
        sm = difflib.SequenceMatcher(None, complete_list, stats, False)
        blocks = list(sm.get_matching_blocks())
        logging.info(blocks)
        find_holes(blocks, stats)

    except OutOfOrderException:
        logging.error('File {:s} and File {:s} are not ordered'.format(least_holes_file[0], name))
        for diff in difflib.unified_diff(complete_list, stats):
            logging.error(diff)
        exit(1)

logging.info('All files have the same order discarding holes!')
