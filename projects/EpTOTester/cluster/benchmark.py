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
from datetime import datetime
from docker import errors
from docker import types
from docker import utils
from logging import config
from nodes_trace import NodesTrace


class Benchmark:
    def __init__(self, config, local, use_tracker, churn=None):
        self.cli = docker.Client(base_url='unix://var/run/docker.sock')
        self.config = config
        self.local = local
        self.logger = logging.getLogger('benchmarks')
        self.churn = churn
        self.use_tracker = use_tracker

    def run(self):
        if args.local:
            service_image = self.config['service']['name']
            if args.tracker:
                tracker_image = self.config['tracker']['name']
            with subprocess.Popen(['../gradlew', '-p', '..', 'docker'],
                                  stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                  universal_newlines=True) as p:
                for line in p.stdout:
                    print(line, end='')
        else:
            service_image = self.config['repository'] + self.config['service']['name']
            for line in self.cli.pull(service_image, stream=True, decode=True):
                print(line)
            if self.use_tracker:
                tracker_image = self.config['repository'] + self.config['tracker']['name']
                for line in self.cli.pull(tracker_image, stream=True):
                    print(line)
            try:
                self.cli.init_swarm()
                if not self.local:
                    self.logger.info('Joining Swarm on every hosts:')
                    token = self.cli.inspect_swarm()['JoinTokens']['Worker']
                    subprocess.call(['parallel-ssh', '-t', '0', '-h', 'hosts', 'docker', 'swarm',
                                     'join', '--token', token, '{:s}:2377'.format(MANAGER_IP)])
                ipam_pool = utils.create_ipam_pool(subnet=self.config['service']['network']['subnet'])
                ipam_config = utils.create_ipam_config(pool_configs=[ipam_pool])
                self.cli.create_network(self.config['service']['network']['name'], 'overlay', ipam=ipam_config)
            except errors.APIError:
                self.logger.info('Host is already part of a swarm')
                if not self.cli.networks(names=[self.config['service']['network']['name']]):
                    self.logger.error('Network  doesn\'t exist!')
                    exit(1)

    def stop(self):
        try:
            if self.use_tracker:
                self.cli.remove_service(self.config['tracker']['name'])
            self.cli.remove_service(self.config['service']['name'])
            if not self.local:
                time.sleep(15)
                with open('hosts', 'r') as file:
                    for host in file.read().splitlines():
                        subprocess.call('rsync --remove-source-files '
                                        '-av {:s}:{:s}/*.txt ../data'
                                        .format(host, CLUSTER_DATA), shell=True)
                        subprocess.call('rsync --remove-source-files '
                                        '-av {:s}:{:s}/capture/*.csv ../data/capture'
                                        .format(host, CLUSTER_DATA), shell=True)
        except errors.NotFound:
            pass

    def set_logger_level(self, log_level):
        self.logger.setLevel(log_level)

    def _run_churn(self, time_to_start):
        self.logger.debug('Time to start churn: {:d}'.format(time_to_start))
        if self.churn.synthetic:
            self.logger.info(self.churn.synthetic)
            nodes_trace = NodesTrace(synthetic=self.churn.synthetic)
        else:
            real_churn_params = self.config['real_churn']
            # Website02.db epoch starts 1st January 2001. Exact formula obtained from Sebastien Vaucher
            # websites_epoch = 730753 + 1 + 86400. / (16 * 3600 + 11 * 60 + 10)
            nodes_trace = NodesTrace(database=real_churn_params['database'], min_time=real_churn_params['epoch'] +
                                                                                      real_churn_params['start_time'],
                                     max_time=real_churn_params['epoch'] +
                                              real_churn_params['start_time'] +
                                              real_churn_params['duration'],
                                     time_factor=real_churn_params['time_factor'])



        delta = self.churn.period
        #churn = Churn(hosts_filename=hosts_fname, service_name=self.config['service']['name'], repository=repository)


        # Add initial cluster
        self.logger.debug('Initial size: {}'.format(nodes_trace.initial_size()))
        churn.add_processes(nodes_trace.initial_size())
        delay = int((time_to_start - (time.time() * 1000)) / 1000)
        self.logger.debug('Delay: {:d}'.format(delay))
        self.logger.info('Starting churn at {:s} UTC'
                    .format(datetime.utcfromtimestamp(time_to_start // 1000).isoformat()))
        time.sleep(delay)
        self.logger.info('Starting churn')
        nodes_trace.next()
        for size, to_kill, to_create in nodes_trace:
            self.logger.debug('curr_size: {:d}, to_kill: {:d}, to_create {:d}'
                         .format(size, len(to_kill), len(to_create)))
            churn.add_suspend_processes(len(to_kill), len(to_create))
            time.sleep(delta / 1000)

        self.logger.info('Churn finished')

    def _create_service(self, service_name, image, env=None, mounts=None, placement=None,
                        replicas=1, mem_limit=314572800):
        container_spec = types.ContainerSpec(image=image, env=env, mounts=mounts)
        self.logger.debug(container_spec)
        task_tmpl = types.TaskTemplate(container_spec,
                                       resources=types.Resources(mem_limit=mem_limit),
                                       restart_policy=types.RestartPolicy(),
                                       placement=placement)
        self.logger.debug(task_tmpl)
        self.cli.create_service(task_tmpl, name=service_name, mode={'Replicated': {'Replicas': replicas}},
                                networks=[{'Target': self.config['service']['network']['name']}])

    def _wait_on_service(self, service_name, containers_nb, total_nb=None, inverse=False):
        def get_nb():
            output = subprocess.check_output(['docker', 'service', 'ls', '-f', 'name={:s}'.format(service_name)],
                                             universal_newlines=True).splitlines()[1]
            match = re.match(r'.+ (\d+)/(\d+)', output)
            return int(match.group(1)), int(match.group(2))

        if inverse:  # Wait while current nb is equal to containers_nb
            current_nb = containers_nb
            while current_nb == containers_nb:
                self.logger.debug('current_nb={:d}, containers_nb={:d}'.format(current_nb, containers_nb))
                time.sleep(1)
                current_nb = get_nb()[0]
        else:
            current_nb = -1
            current_total_nb = -1
            while current_nb > containers_nb or current_total_nb != total_nb:
                self.logger.debug('current_nb={:d}, containers_nb={:d}'.format(current_nb, containers_nb))
                self.logger.debug('current_total_nb={:d}'.format(current_total_nb))
                time.sleep(5)
                current_nb, current_total_nb = get_nb()
                if not total_nb:
                    total_nb = current_total_nb
                else:
                    self.logger.debug('current_total_nb={:d}, total_nb={:d}'.format(current_total_nb, total_nb))




