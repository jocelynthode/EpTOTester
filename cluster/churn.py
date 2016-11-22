#!/usr/bin/env python3
import logging
import random
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

    def __init__(self, hosts_filename=None, service_name='epto', repository=''):
        self.containers = {}
        self.peer_list = []
        self.periods = 0
        self.logger = logging.getLogger('churn')

        self.service_name = service_name
        self.repository = repository
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

        # Retrieve all containers id
        if not self.containers:
            for host in self.hosts:
                command_ps = ["docker", "ps", "-aqf",
                              "name={service},status=running,ancestor={repo}{service}".format(
                                  service=self.service_name, repo=self.repository)]
                if host != 'localhost':
                    command_ps = ["ssh", host] + command_ps

                self.containers[host] = subprocess.check_output(command_ps,
                                                                universal_newlines=True).splitlines()
            self.logger.debug(self.containers)

        for i in range(to_suspend_nb):
            command_suspend = ["docker", "kill", '--signal=SIGSTOP']

            # Retry until we find a working choice
            count = 0
            while count < 3:
                try:
                    choice = random.choice(self.hosts)

                    if choice != 'localhost':
                        command_suspend = ["ssh", choice] + command_suspend

                    container = random.choice(self.containers[choice])
                    self.containers[choice].remove(container)
                except (ValueError, IndexError):
                    count += 1
                    if not self.containers[choice]:
                        self.hosts.remove(choice)
                    self.logger.error('Error when trying to pick a container')
                    if count == 3:
                        self.logger.error('Stopping churn because no container was found')
                        raise
                    continue
                break

            command_suspend += [container]
            subprocess.call(command_suspend, stdout=subprocess.DEVNULL)
            self.logger.info('Container {} on host {} was terminated'
                             .format(container, choice))

    def add_processes(self, to_create_nb):
        if to_create_nb < 0:
            raise ArithmeticError('Add number must be greater or equal to 0')
        if to_create_nb == 0:
            return
        self.cluster_size += to_create_nb
        i = 0
        while i < 5:
            try:
                subprocess.check_call(["docker", "service", "scale",
                                       "{:s}={:d}".format(self.service_name, self.cluster_size)],
                                      stdout=subprocess.DEVNULL)
            except subprocess.CalledProcessError:
                i += 1
                if i >= 5:
                    raise
                self.logger.error("Couldn't scale service")
                continue
            break

        self.logger.info('Service scaled up to {:d}'.format(self.cluster_size))

    def add_suspend_processes(self, to_suspend_nb, to_create_nb):
        self.suspend_processes(to_suspend_nb)
        self.add_processes(to_create_nb)
        self.periods += 1

    def set_logger_level(self, log_level):
        self.logger.setLevel(log_level)
