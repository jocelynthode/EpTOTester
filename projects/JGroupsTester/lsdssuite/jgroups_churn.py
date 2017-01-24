import glob
import random
import re
import subprocess

from lsdssuite import Churn


class JGroupsChurn(Churn):
    """
    Author: Jocelyn Thode

    A class in charge of adding/suspending nodes to create churn in a JGroups
    SEQUENCER cluster
    """

    def __init__(self, hosts_filename=None, service_name='', repository='',
                 period=5000, delay=0, synthetic=None,
                 file_path='', kill_coordinator_rounds=None):
        super().__init__(hosts_filename, service_name, repository, period,
                         delay, synthetic)
        self.coordinator = None
        self.peer_list = None
        self.periods = 0
        self._file_path = file_path

        if kill_coordinator_rounds is None:
            self.kill_coordinator_rounds = []
        else:
            self.kill_coordinator_rounds = kill_coordinator_rounds
        self.kill_index = 0

    def suspend_processes(self, to_suspend_nb):
        """
        Suspend the requested number of docker containers

        :param to_suspend_nb:
        :return:
        """
        if to_suspend_nb < 0:
            raise ArithmeticError(
                'Suspend number must be greater or equal to 0')
        if to_suspend_nb == 0:
            return

        for i in range(to_suspend_nb):
            command_suspend = ["docker", "kill", '--signal=SIGUSR1']
            self.logger.debug(
                "Variables: {} {}".format(
                    self.periods,
                    self.kill_coordinator_rounds))
            # If we missed kill period kill the coord at the next chance
            if (self.periods in self.kill_coordinator_rounds
                or (self.kill_index < len(self.kill_coordinator_rounds)
                    and self.periods > self.kill_coordinator_rounds[self.kill_index])):
                self.logger.info("Killing coordinator")
                command_suspend += [self.coordinator]
                for host in self.hosts:
                    self._refresh_host_containers(host)
                for host, containers in self.containers.items():
                    if self.coordinator in containers:
                        if host != 'localhost':
                            command_suspend = ["ssh", host] + command_suspend

                        count = 0
                        while count < 3:
                            try:
                                subprocess.check_call(
                                    command_suspend, stdout=subprocess.DEVNULL)
                                self.logger.info(
                                    'Coordinator {:s} on host {:s} was suspended'
                                    .format(self.coordinator, host))
                                self.suspended_containers.append(
                                    self.coordinator)
                            except subprocess.CalledProcessError:
                                count += 1
                                self.logger.error(
                                    "Container couldn't be removed, retrying...")
                                if count >= 3:
                                    self.logger.error(
                                        "Container couldn't be removed",
                                        exc_info=True)
                                    raise
                                continue
                            break

                self.coordinator = self.peer_list.pop(0)
                self.kill_index += 1
                continue

            # Retry until we find a working choice
            count = 0
            while count < 5:
                try:
                    choice = random.choice(self.hosts)
                    self._refresh_host_containers(choice)
                    container, command_suspend = self._choose_container(
                        command_suspend, choice)
                    self.logger.debug("container: {:s}, coordinator: {:s}"
                                      .format(container, self.coordinator))
                    while (container in self.suspended_containers
                           or container == self.coordinator):
                        command_suspend = [
                            "docker", "kill", '--signal=SIGUSR1']
                        choice = random.choice(self.hosts)
                        self._refresh_host_containers(choice)
                        container, command_suspend = self._choose_container(
                            command_suspend, choice)
                except (ValueError, IndexError):
                    count += 1
                    if not self.containers[choice]:
                        self.hosts.remove(choice)
                    self.logger.error('Error when trying to pick a container')
                    if count == 5:
                        self.logger.error(
                            'Stopping churn because no container was found')
                        raise
                    continue
                break

            command_suspend.append(container)
            count = 0
            while count < 3:
                try:
                    subprocess.check_call(
                        command_suspend, stdout=subprocess.DEVNULL)
                    self.logger.info('Container {} on host {} was suspended'
                                     .format(container, choice))
                    self.peer_list.remove(container)
                    self.suspended_containers.append(container)
                except subprocess.CalledProcessError:
                    count += 1
                    self.logger.error(
                        "Container couldn't be removed, retrying...")
                    if count >= 3:
                        self.logger.error(
                            "Container couldn't be removed", exc_info=True)
                        raise
                    continue
                except ValueError:
                    pass
                break

    def add_suspend_processes(self, to_suspend_nb, to_create_nb):
        if not self.peer_list:
            self._get_peer_list(self._file_path)

        super().add_suspend_processes(to_suspend_nb, to_create_nb)
        self.periods += 1

    def _get_peer_list(self, path):
        self.logger.debug("Path: {}".format(path))
        with open(glob.glob(path)[0], 'r') as f:
            a_list = []
            for line in f.readlines():
                match = re.match(r'\d+ - View: (.+)', line)
                if match:
                    a_list = match.group(1).split(',')
                    break
            if not a_list:
                raise LookupError('No view found in file {}'.format(f.name))
            self.peer_list = a_list
            self.coordinator = a_list[0]
