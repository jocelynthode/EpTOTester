#!/usr/bin/env python3
"""
Author: Jocelyn Thode

This script is in charge of running the benchmarks either locally or on the cluster.

It creates the network, services and the churn if we need it.

"""
import argparse
import logging
import signal
from logging import config

import yaml
from benchmark import Benchmark
from jgroups_churn import JGroupsChurn

with open('config.yaml', 'r') as f:
    CLUSTER_PARAMETERS = yaml.load(f)


def create_logger():
    with open('logger.yaml') as f:
        conf = yaml.load(f)
        logging.config.dictConfig(conf)


def churn_tuple(s):
    try:
        _to_kill, _to_create = map(int, s.split(','))
        return _to_kill, _to_create
    except:
        raise TypeError("Tuples must be (int, int)")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run benchmarks',
                                     formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('peer_number', type=int, help='With how many peer should it be ran')
    parser.add_argument('time_add', type=int, help='Delay experiments start in seconds')
    parser.add_argument('time_to_run', type=int, help='For how long should the experiment run in seconds')
    parser.add_argument('app_config', type=argparse.FileType('r'), help='Application configuration file')
    parser.add_argument('-l', '--local', action='store_true',
                        help='Run locally')
    parser.add_argument('-t', '--tracker', action='store_true', help='Specify whether the app uses a tracker')
    parser.add_argument('-n', '--runs', type=int, default=1, help='How many experiments should be ran')
    parser.add_argument('--verbose', '-v', action='store_true', help='Switch DEBUG logging on')

    subparsers = parser.add_subparsers(dest='churn', help='Specify churn and its arguments')

    churn_parser = subparsers.add_parser('churn', help='Activate churn')
    churn_parser.add_argument('period', type=int,
                              help='The interval between killing/adding new containers in ms')
    churn_parser.add_argument('--synthetic', '-s', metavar='N', type=churn_tuple, nargs='+',
                              help='Pass the synthetic list (to_kill,to_create)(example: 0,100 0,1 1,0)')
    churn_parser.add_argument('--delay', '-d', type=int, default=0,
                              help='With how much delay compared to the tester should the tester start in ms')

    args = parser.parse_args()
    APP_CONFIG = yaml.load(args.app_config)

    if args.verbose:
        log_level = logging.DEBUG
    else:
        log_level = logging.INFO
    create_logger()
    logger = logging.getLogger('benchmarks')
    logger.setLevel(log_level)

    logger.info('START')
    if args.local:
        hosts_fname = None
        repository = ''
        file_path = CLUSTER_PARAMETERS['cluster_data'] + '/*.txt'
    else:
        hosts_fname = 'hosts'
        repository = APP_CONFIG['repository']['name']
        file_path = CLUSTER_PARAMETERS['local_data'] + '/*.txt'

    if args.churn:
        churn = JGroupsChurn(hosts_filename=hosts_fname, service_name=APP_CONFIG['service']['name'],
                             repository=repository, delay=args.delay, synthetic=args.synthetic,
                             file_path=CLUSTER_PARAMETERS[''],
                             kill_coordinator_round=APP_CONFIG['service']['parameters']['kill_coordinator_round'])
        churn.set_logger_level(log_level)
    else:
        churn = None
    benchmark = Benchmark(APP_CONFIG, CLUSTER_PARAMETERS, args.local, log_level, churn)
    benchmark.set_logger_level(log_level)
    benchmark.run(args.time_add, args.time_to_run, args.runs)


    def signal_handler(signal, frame):
        logger.info('Stopping Benchmarks')
        benchmark.stop()
        exit(0)


    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
