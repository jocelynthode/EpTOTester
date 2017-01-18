#!/usr/bin/env python3
import logging
import random
import subprocess


class Churn:
    """
    Author: Jocelyn Thode

    A class in charge of adding/suspending nodes to create churn in a EpTO cluster
    """

    def __init__(self, hosts_filename=None, service_name='', repository=''):
        self.containers = {}
        self.peer_list = []
        self.logger = logging.getLogger('churn')
        self.suspended_containers = []

        self.service_name = service_name
        self.repository = repository
        self.hosts = ['localhost']
        if hosts_filename is not None:
            with open(hosts_filename, 'r') as file:
                self.hosts += list(line.rstrip() for line in file)
        self.cluster_size = 0

    def suspend_processes(self, to_suspend_nb):
        """
        Suspend the requested number of docker containers

        :param to_suspend_nb:
        :return:
        """
        if to_suspend_nb < 0:
            raise ArithmeticError('Suspend number must be greater or equal to 0')
        if to_suspend_nb == 0:
            return

        for i in range(to_suspend_nb):
            command_suspend = ["docker", "kill", '--signal=SIGUSR1']
            # Retry until we find a working choice
            count = 0
            while count < 3:
                try:
                    choice = random.choice(self.hosts)
                    self._refresh_host_containers(choice)
                    container, command_suspend = self._choose_container(command_suspend, choice)
                    while container in self.suspended_containers:
                        command_suspend = ["docker", "kill", '--signal=SIGUSR1']
                        choice = random.choice(self.hosts)
                        self._refresh_host_containers(choice)
                        container, command_suspend = self._choose_container(command_suspend, choice)
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

            command_suspend.append(container)
            count = 0
            while count < 3:
                try:
                    subprocess.check_call(command_suspend, stdout=subprocess.DEVNULL)
                    self.logger.info('Container {} on host {} was suspended'
                                     .format(container, choice))
                    self.suspended_containers.append(container)
                except subprocess.CalledProcessError:
                    count += 1
                    self.logger.error("Container couldn't be removed, retrying...")
                    if count >= 3:
                        self.logger.error("Container couldn't be removed", exc_info=True)
                        raise
                    continue
                break

    def add_processes(self, to_create_nb):
        """
        Create the requested number of docker containers

        :param to_create_nb:
        :return:
        """
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
        """
        Abstraction letting the user create and suspend containers in the same round

        :param to_suspend_nb:
        :param to_create_nb:
        :return:
        """
        self.suspend_processes(to_suspend_nb)
        self.add_processes(to_create_nb)

    def set_logger_level(self, log_level):
        self.logger.setLevel(log_level)

    def _choose_container(self, command_suspend, host):
        if host != 'localhost':
            command_suspend = ["ssh", host] + command_suspend

        container = random.choice(self.containers[host])
        self.containers[host].remove(container)
        return container, command_suspend

    def _refresh_host_containers(self, host):
        command_ps = ["docker", "ps", "-qf",
                      "name={service},status=running,ancestor={repo}{service}".format(
                          service=self.service_name, repo=self.repository)]
        if host != 'localhost':
            command_ps = ["ssh", host] + command_ps

        self.containers[host] = subprocess.check_output(command_ps,
                                                        universal_newlines=True).splitlines()
