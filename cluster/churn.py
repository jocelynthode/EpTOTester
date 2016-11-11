#!/usr/bin/env python3
import glob
import logging
import random
import re
import subprocess


def churn_tuple(s):
    try:
        _to_kill, _to_create = map(int, s.split(','))
        return _to_kill, _to_create
    except:
        raise TypeError("Tuples must be (int, int)")


class Churn:
    """
    Author: Jocelyn Thode

    A class in charge of adding/suspending nodes to create churn in a EpTO cluster
    """
    containers = {}
    coordinator = None
    peer_list = []
    periods = 0
    logger = logging.getLogger('churn')

    def __init__(self, hosts_filename=None, service_name='epto'):

        self.service_name = service_name
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
                command_ps = ["docker", "ps", "-aqf", "name={:s}".format(self.service_name), "-f", "status=running"]
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
                self.logger.error('No container available')
                return

            command_suspend += [container]
            subprocess.call(command_suspend, stdout=subprocess.DEVNULL)
            self.logger.info('Container {} on host {} was suspended'
                             .format(container, choice))

    def add_processes(self, to_create_nb):
        if to_create_nb < 0:
            raise ArithmeticError('Add number must be greater or equal to 0')
        if to_create_nb == 0:
            return
        self.cluster_size += to_create_nb
        subprocess.call(["docker", "service", "scale",
                         "{:s}={:d}".format(self.service_name, self.cluster_size)],
                        stdout=subprocess.DEVNULL)
        self.logger.info('Service scaled up to {:d}'.format(self.cluster_size))

    def add_suspend_processes(self, to_suspend_nb, to_create_nb):
        self.suspend_processes(to_suspend_nb)
        self.add_processes(to_create_nb)
        self.periods += 1

    def set_logger_level(self, log_level):
        self.logger.setLevel(log_level)
