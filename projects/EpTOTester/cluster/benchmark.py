import logging
import re
import subprocess
import threading
import time
from datetime import datetime

import docker
from docker import errors
from docker import types
from docker import utils
from nodes_trace import NodesTrace


class Benchmark:
    """
    Author: Jocelyn Thode

    A class in charge of Setting up and running a benchmark
    """

    def __init__(self, app_config, cluster_config, local, use_tracker,
                 churn=None,
                 cli=docker.Client(base_url='unix://var/run/docker.sock')):
        self.cli = cli
        self.app_config = app_config
        self.cluster_config = cluster_config
        self.local = local
        self.logger = logging.getLogger('benchmarks')
        self.churn = churn
        self.use_tracker = use_tracker

    def run(self, time_add, time_to_run, peer_number, runs=1):
        """
        Run the benchmark

        :param peer_number: How many starting peers
        :param time_add: How much time before starting the benchmark
        :param time_to_run: How much time to run the benchmark for
        :param runs: RUn the benchmark how many time
        """
        time_add *= 1000
        time_to_run *= 1000
        if self.local:
            service_image = self.app_config['service']['name']
            if self.use_tracker:
                tracker_image = self.app_config['tracker']['name']
            with subprocess.Popen(['../gradlew', '-p', '..', 'docker'],
                                  stdin=subprocess.PIPE,
                                  stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE,
                                  universal_newlines=True) as p:
                for line in p.stdout:
                    print(line, end='')
        else:
            service_image = (self.app_config['repository']['name']
                             + self.app_config['service']['name'])
            for line in self.cli.pull(service_image, stream=True, decode=True):
                print(line)
            if self.use_tracker:
                tracker_image = (self.app_config['repository']['name']
                                 + self.app_config['tracker']['name'])
                for line in self.cli.pull(tracker_image, stream=True):
                    print(line)
            try:
                self.cli.init_swarm()
                if not self.local:
                    self.logger.info('Joining Swarm on every hosts:')
                    token = self.cli.inspect_swarm()['JoinTokens']['Worker']
                    subprocess.call(
                        [
                            'parallel-ssh',
                            '-t',
                            '0',
                            '-h',
                            'hosts',
                            'docker',
                            'swarm',
                            'join',
                            '--token',
                            token,
                            '{:s}:2377'.format(
                                self.cluster_config['manager_ip'])])
                ipam_pool = utils.create_ipam_pool(
                    subnet=self.app_config['service']['network']['subnet'])
                ipam_config = utils.create_ipam_config(
                    pool_configs=[ipam_pool])
                self.cli.create_network(
                    self.app_config['service']['network']['name'],
                    'overlay', ipam=ipam_config)
            except errors.APIError:
                self.logger.info('Host is already part of a swarm')
                if not self.cli.networks(
                        names=[self.app_config['service']['network']['name']]):
                    self.logger.error('Network  doesn\'t exist!')
                    exit(1)

        for run_nb, _ in enumerate(range(runs), 1):
            if self.use_tracker:
                self._create_service(
                    self.app_config['tracker']['name'],
                    tracker_image,
                    placement={
                        'Constraints': ['node.role == manager']},
                    mem_limit=self.app_config['service']['mem_limit'])
                self._wait_on_service(self.app_config['tracker']['name'], 1)
            time_to_start = int((time.time() * 1000) + time_add)
            self.logger.debug(
                datetime.utcfromtimestamp(
                    time_to_start /
                    1000).isoformat())

            environment_vars = self.app_config['service']['parameters'].copy()
            environment_vars.update({'PEER_NUMBER': peer_number,
                                     'TIME': time_to_start,
                                     'TIME_TO_RUN': time_to_run})
            environment_vars = [
                '{:s}={}'.format(k, v) for k, v in environment_vars.items()]
            self.logger.debug(environment_vars)

            service_replicas = 0 if self.churn else peer_number
            log_storage = (self.cluster_config['local_data']
                           if self.local
                           else self.cluster_config['cluster_data'])
            self._create_service(
                self.app_config['service']['name'],
                service_image,
                env=environment_vars,
                mounts=[
                    types.Mount(
                        target='/data',
                        source=log_storage,
                        type='bind')],
                replicas=service_replicas,
                mem_limit=self.app_config['service']['mem_limit'])

            self.logger.info(
                'Running Benchmark -> Experiment: {:d}/{:d}'.format(
                    run_nb, runs))
            if self.churn:
                thread = threading.Thread(
                    target=self._run_churn, args=[
                        time_to_start + self.churn.delay], daemon=True)
                thread.start()
                self._wait_on_service(
                    self.app_config['service']['name'], 0, inverse=True)
                self.logger.info('Running with churn')
                if self.churn.synthetic:
                    # Wait for some peers to at least start
                    time.sleep(120)
                    total = [sum(x) for x in zip(*self.churn.synthetic)]
                    # Wait until only stopped containers are still alive
                    self._wait_on_service(
                        self.app_config['service']['name'],
                        containers_nb=total[0],
                        total_nb=total[1])
                else:
                    # TODO not the most elegant solution
                    thread.join()  # Wait for churn to finish
                    time.sleep(300)  # Wait 5 more minutes

            else:
                self._wait_on_service(
                    self.app_config['service']['name'], 0, inverse=True)
                self.logger.info('Running without churn')
                self._wait_on_service(self.app_config['service']['name'], 0)
            if self.use_tracker:
                self.cli.remove_service(self.app_config['tracker']['name'])
            self.cli.remove_service(self.app_config['service']['name'])

            self.logger.info('Services removed')
            time.sleep(30)

            if not self.local:
                subprocess.call(
                    'parallel-ssh -t 0 -h hosts "mkdir -p {path}/test-{nb}/capture &&'
                    ' mv {path}/*.txt {path}/test-{nb}/ &&'
                    ' mv {path}/capture/*.csv {path}/test-{nb}/capture/"'
                        .format(path=self.cluster_config['cluster_data'],
                                nb=run_nb),
                    shell=True)

            subprocess.call(
                'mkdir -p {path}/test-{nb}/capture'.format(path=log_storage,
                                                           nb=run_nb),
                shell=True)
            subprocess.call(
                'mv {path}/*.txt {path}/test-{nb}/'.format(path=log_storage,
                                                           nb=run_nb),
                shell=True)
            subprocess.call(
                'mv {path}/capture/*.csv {path}/test-{nb}/capture/'.format(
                    path=log_storage, nb=run_nb), shell=True)

        self.logger.info('Benchmark done!')

    def stop(self):
        """
        Stop the benchmark and get every logs

        :return:
        """
        try:
            if self.use_tracker:
                self.cli.remove_service(self.app_config['tracker']['name'])
            self.cli.remove_service(self.app_config['service']['name'])
            if not self.local:
                time.sleep(15)
                with open('hosts', 'r') as file:
                    for host in file.read().splitlines():
                        subprocess.call('rsync --remove-source-files '
                                        '-av {:s}:{:s}/*.txt ../data'
                                        .format(host, self.cluster_config['cluster_data']),
                                        shell=True)
                        subprocess.call(
                            'rsync --remove-source-files '
                            '-av {:s}:{:s}/capture/*.csv ../data/capture'
                                .format(host, self.cluster_config['cluster_data']),
                            shell=True)
        except errors.NotFound:
            pass

    def set_logger_level(self, log_level):
        """
        Set the logger level of the object

        :param log_level: The logger level
        :return:
        """
        self.logger.setLevel(log_level)

    def _run_churn(self, time_to_start):
        self.logger.debug('Time to start churn: {:d}'.format(time_to_start))
        if self.churn.synthetic:
            self.logger.info(self.churn.synthetic)
            nodes_trace = NodesTrace(synthetic=self.churn.synthetic)
        else:
            real_churn_params = self.app_config['real_churn']
            nodes_trace = NodesTrace(
                database=real_churn_params['database'],
                min_time=real_churn_params['epoch'] +
                         real_churn_params['start_time'],
                max_time=real_churn_params['epoch'] +
                         real_churn_params['start_time'] +
                         real_churn_params['duration'],
                time_factor=real_churn_params['time_factor'])

        delta = self.churn.period

        # Add initial cluster
        self.logger.debug(
            'Initial size: {}'.format(
                nodes_trace.initial_size()))
        self.churn.add_processes(nodes_trace.initial_size())
        delay = int((time_to_start - (time.time() * 1000)) / 1000)
        self.logger.debug('Delay: {:d}'.format(delay))
        self.logger.info(
            'Starting churn at {:s} UTC' .format(
                datetime.utcfromtimestamp(
                    time_to_start //
                    1000).isoformat()))
        time.sleep(delay)
        self.logger.info('Starting churn')
        nodes_trace.next()
        for size, to_kill, to_create in nodes_trace:
            self.logger.debug('curr_size: {:d}, to_kill: {:d}, to_create {:d}'
                              .format(size, len(to_kill), len(to_create)))
            self.churn.add_suspend_processes(len(to_kill), len(to_create))
            time.sleep(delta / 1000)

        self.logger.info('Churn finished')

    def _create_service(
            self,
            service_name,
            image,
            env=None,
            mounts=None,
            placement=None,
            replicas=1,
            mem_limit=314572800):
        container_spec = types.ContainerSpec(
            image=image, env=env, mounts=mounts)
        self.logger.debug(container_spec)
        task_tmpl = types.TaskTemplate(container_spec,
                                       resources=types.Resources(
                                           mem_limit=mem_limit),
                                       restart_policy=types.RestartPolicy(),
                                       placement=placement)
        self.logger.debug(task_tmpl)
        self.cli.create_service(task_tmpl, name=service_name,
                                mode={'Replicated': {'Replicas': replicas}},
                                networks=[{'Target': self.app_config['service']['network']['name']}])

    def _wait_on_service(self, service_name, containers_nb,
                         total_nb=None, inverse=False):
        def get_nb():
            output = subprocess.check_output(['docker',
                                              'service',
                                              'ls',
                                              '-f',
                                              'name={:s}'.format(service_name)],
                                             universal_newlines=True).splitlines()[1]
            match = re.match(r'.+ (\d+)/(\d+)', output)
            return int(match.group(1)), int(match.group(2))

        if inverse:  # Wait while current nb is equal to containers_nb
            current_nb = containers_nb
            while current_nb == containers_nb:
                self.logger.debug(
                    'current_nb={:d}, containers_nb={:d}'.format(
                        current_nb, containers_nb))
                time.sleep(1)
                current_nb = get_nb()[0]
        else:
            current_nb = -1
            current_total_nb = -1
            while current_nb > containers_nb or current_total_nb != total_nb:
                self.logger.debug(
                    'current_nb={:d}, containers_nb={:d}'.format(
                        current_nb, containers_nb))
                self.logger.debug(
                    'current_total_nb={:d}'.format(current_total_nb))
                time.sleep(5)
                current_nb, current_total_nb = get_nb()
                if not total_nb:
                    total_nb = current_total_nb
                else:
                    self.logger.debug(
                        'current_total_nb={:d}, total_nb={:d}'.format(
                            current_total_nb, total_nb))
