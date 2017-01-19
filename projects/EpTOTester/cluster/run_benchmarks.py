#!/usr/bin/env python3
"""
Author: Jocelyn Thode

This script is in charge of running the benchmarks either locally or on the cluster.

It creates the network, services and the churn if we need it.

"""
import argparse
import docker
import logging
import re
import signal
import subprocess
import threading
import time
import yaml

from churn import Churn
from benchmark import Benchmark
from datetime import datetime
from docker import errors
from docker import types
from docker import utils
from logging import config
from nodes_trace import NodesTrace


with open('config.yaml', 'r') as f:
    config = yaml.load(f)
    MANAGER_IP = config['manager_ip']
    LOCAL_MANAGER_IP = config['local_manager_ip']
    LOCAL_DATA = config['local_data']
    CLUSTER_DATA = config['cluster_data']


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
    parser.add_argument('config', type=argparse.FileType('r'), help='Configuration file')
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
    APP_CONFIG = yaml.load(args.config)

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
    else:
        hosts_fname = 'hosts'
        repository = APP_CONFIG['repository']['name']

    churn = Churn(hosts_filename=hosts_fname, service_name=APP_CONFIG['service']['name'], repository=repository)
    churn.set_logger_level(log_level)
    benchmark = Benchmark(APP_CONFIG, args.local, log_level, churn)
    benchmark.set_logger_level(log_level)
    benchmark.run()

    def signal_handler(signal, frame):
        logger.info('Stopping Benchmarks')
        benchmark.stop()
        exit(0)
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)




