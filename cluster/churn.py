#!/usr/bin/env python3
import argparse
import logging
import random
import subprocess
import time

from datetime import datetime
from nodes_trace import NodesTrace


LOCAL_DATA_FILES = '/home/jocelyn/tmp/data/*.txt'


def churn_tuple(s):
    try:
        _to_kill, _to_create = map(int, s.split(','))
        return _to_kill, _to_create
    except:
        raise TypeError("Tuples must be (int, int)")


class Churn:
    """
    Author: Jocelyn Thode

    A class in charge of adding/suspending nodes to create churn in an EpTO cluster
    """
    containers = {}
    periods = 0

    def __init__(self, hosts_filename=None):
        self.hosts = ['localhost']
        if hosts_filename is not None:
            with open(hosts_filename, 'r') as file:
                self.hosts += list(line.rstrip() for line in file)
        self.cluster_size = 0

    def suspend_processes(self, to_suspend_nb):
        if to_suspend_nb < 0:
            raise ArithmeticError('Suspend number must be greater or equal to 0')
        if to_suspend_nb == 0:
            return
        for i in range(to_suspend_nb):
            command_suspend = ["docker", "kill", '--signal=SIGTERM']

            choice = random.choice(self.hosts)
            if choice not in self.containers:
                command_ps = ["docker", "ps", "-aqf", "name=epto-service", "-f", "status=running"]
                if choice != 'localhost':
                    command_ps = ["ssh", choice] + command_ps

                self.containers[choice] = subprocess.check_output(command_ps,
                                                                  universal_newlines=True).splitlines()

            if choice != 'localhost':
                command_suspend = ["ssh", choice] + command_suspend

            try:
                container = random.choice(self.containers[choice])
                self.containers[choice].remove(container)
            except ValueError or IndexError:
                logging.error("No container available")
                return

            command_suspend += [container]
            subprocess.call(command_suspend)
            logging.info("Container {} on host {} was suspended"
                         .format(container, choice))

    def add_processes(self, to_create_nb):
        if to_create_nb < 0:
            raise ArithmeticError('Add number must be greater or equal to 0')
        if to_create_nb == 0:
            return
        self.cluster_size += to_create_nb
        subprocess.call(["docker", "service", "scale",
                         "epto-service={:d}".format(self.cluster_size)])

    def add_suspend_processes(self, to_suspend_nb, to_create_nb):
        self.suspend_processes(to_suspend_nb)
        self.add_processes(to_create_nb)
        self.periods += 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Create a churn',
                                     formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('delta', type=int, default=60,
                        help='The interval between killing/adding new containers in s')
    parser.add_argument('--local', '-l', action='store_true',
                        help='Run the synthetic churn only on local node')
    parser.add_argument('--synthetic', '-s', metavar='N', type=churn_tuple, nargs='+',
                        help='Pass the synthetic list (to_kill,to_create)(example: 0,100 0,1 1,0)')
    parser.add_argument('--delay', '-d', type=int,
                        help='At which time should the churn start (UTC)')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Switch DEBUG logging on')
    args = parser.parse_args()

    if args.verbose:
        log_level = logging.DEBUG
    else:
        log_level = logging.INFO

    logging.basicConfig(format='%(levelname)s: %(asctime)s - %(message)s', level=log_level)

    if args.synthetic:
        logging.info(args.synthetic)
        nodes_trace = NodesTrace(synthetic=args.synthetic)
    else:
        nodes_trace = NodesTrace(database='database.db')

    if args.local:
        hosts_fname = None
    else:
        hosts_fname = 'hosts'

    delta = args.delta
    churn = Churn(hosts_filename=hosts_fname)

    # Add initial cluster
    churn.add_processes(nodes_trace.initial_size())
    nodes_trace.next()

    if args.delay:
        delay = (datetime.utcfromtimestamp(args.delay // 1000) - datetime.utcnow()).seconds
        if delay < 0:
            delay = 0
    else:
        delay = 0

    logging.info("Starting churn at {:s} UTC"
                 .format(datetime.utcfromtimestamp(args.delay // 1000).isoformat()))
    time.sleep(delay)
    logging.info("Starting churn")

    for _, to_kill, to_create in nodes_trace:
        logging.debug("curr_size: {:d}, to_kill: {:d}, to_create {:d}"
                      .format(_, len(to_kill), len(to_create)))
        churn.add_suspend_processes(len(to_kill), len(to_create))
        time.sleep(delta)

    logging.info("Churn finished")
