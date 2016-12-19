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
from churn import churn_tuple
from churn import get_peer_list
from datetime import datetime
from docker import errors
from docker import types
from docker import utils
from logging import config
from nodes_trace import NodesTrace


cli = docker.Client(base_url='unix://var/run/docker.sock')
MANAGER_IP = '172.16.2.119'
LOCAL_DATA = '/home/jocelyn/tmp/data'
CLUSTER_DATA = '/home/debian/data'
LOCAL_DATA_FILES = '{:s}/*.txt'.format(LOCAL_DATA)
DISTANT_REPOSITORY = 'swarm-m:5000/'
SERVICE_NAME = 'jgroups'
TRACKER_NAME = 'jgroups-tracker'
NETWORK_NAME = 'jgroups_network'
SUBNET = '172.113.0.0/16'


def create_service(service_name, image, env=None, mounts=None, placement=None, replicas=1):
    container_spec = types.ContainerSpec(image=image, env=env, mounts=mounts)
    logger.debug(container_spec)
    task_tmpl = types.TaskTemplate(container_spec,
                                   resources=types.Resources(mem_limit=314572800),
                                   restart_policy=types.RestartPolicy(),
                                   placement=placement)
    logger.debug(task_tmpl)
    cli.create_service(task_tmpl, name=service_name, mode={'Replicated': {'Replicas': replicas}},
                       networks=[{'Target': NETWORK_NAME}])


def run_churn(time_to_start):
    logger.debug('Time to start churn: {:d}'.format(time_to_start))
    if args.synthetic:
        logger.info(args.synthetic)
        nodes_trace = NodesTrace(synthetic=args.synthetic)
    else:
        # Website02.db epoch starts 1st January 201. Exact formula obtained from Sebastien Vaucher
        websites_epoch = 730753 + 1 + 86400. / (16 * 3600 + 11 * 60 + 10)
        nodes_trace = NodesTrace(database='websites02.db', min_time=websites_epoch + 22200,
                                 max_time=websites_epoch + 22200 + 7200, time_factor=2)

    if args.local:
        hosts_fname = None
        repository = ''
    else:
        hosts_fname = 'hosts'
        repository = DISTANT_REPOSITORY

    delta = args.delta
    logging.debug("Kill_coordinator = {}".format(args.kill_coordinator))
    churn = Churn(hosts_filename=hosts_fname, kill_coordinator_round=args.kill_coordinator, service_name=SERVICE_NAME,
                  repository=repository)
    churn.set_logger_level(log_level)
    logger.debug("Kill coordinator rounds: {}".format(churn.kill_coordinator_round))

    # Add initial cluster
    logger.debug('Initial size: {}'.format(nodes_trace.initial_size()))
    churn.add_processes(nodes_trace.initial_size())
    delay = int((time_to_start - (time.time() * 1000)) / 1000)
    logger.debug('Delay: {:d}'.format(delay))
    logger.info('Starting churn at {:s} UTC'
                .format(datetime.utcfromtimestamp(time_to_start // 1000).isoformat()))
    time.sleep(delay)
    logger.info('Starting churn')
    try:
        if args.local:
            churn.peer_list = get_peer_list(LOCAL_DATA_FILES)
        else:
            churn.peer_list = get_peer_list()
    except LookupError:
        logger.error('Experiment failed because not all peers started')
        return

    logger.debug(churn.peer_list)
    churn.coordinator = churn.peer_list.pop(0)

    nodes_trace.next()
    for size, to_kill, to_create in nodes_trace:
        logger.debug('curr_size: {:d}, to_kill: {:d}, to_create {:d}'
                     .format(size, len(to_kill), len(to_create)))
        churn.add_suspend_processes(len(to_kill), len(to_create))
        time.sleep(delta / 1000)

    logger.info('Churn finished')


def wait_on_service(service_name, containers_nb, total_nb=None, inverse=False):
    def get_nb():
        output = subprocess.check_output(['docker', 'service', 'ls', '-f', 'name={:s}'.format(service_name)],
                                         universal_newlines=True).splitlines()[1]
        match = re.match(r'.+ (\d+)/(\d+)', output)
        return int(match.group(1)), int(match.group(2))

    if inverse:  # Wait while current nb is equal to containers_nb
        current_nb = containers_nb
        while current_nb == containers_nb:
            logger.debug('current_nb={:d}, containers_nb={:d}'.format(current_nb, containers_nb))
            time.sleep(1)
            current_nb = get_nb()[0]
    else:
        current_nb = -1
        current_total_nb = -1
        while current_nb > containers_nb or current_total_nb != total_nb:
            logger.debug('current_nb={:d}, containers_nb={:d}'.format(current_nb, containers_nb))
            logger.debug('current_total_nb={:d}'.format(current_total_nb))
            time.sleep(5)
            current_nb, current_total_nb = get_nb()
            if not total_nb:
                total_nb = current_total_nb
            else:
                logger.debug('current_total_nb={:d}, total_nb={:d}'.format(current_total_nb, total_nb))


def create_logger():
    with open('logger.yaml') as f:
        conf = yaml.load(f)
        logging.config.dictConfig(conf)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run benchmarks',
                                     formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('peer_number', type=int, help='With how many peer should it be ran')
    parser.add_argument('time_add', type=int, help='Delay experiments start in seconds')
    parser.add_argument('time_to_run', type=int, help='For how long should the experiment run in seconds')
    parser.add_argument('rate', type=int, help='At which frequency should a peer send an event in ms')
    parser.add_argument('-x', '--fixed-rate', type=int, default=None,
                        help='Fix the rate at which a peer will send events')
    parser.add_argument('-l', '--local', action='store_true',
                        help='Run locally')
    parser.add_argument('-n', '--runs', type=int, default=1, help='How many experiments should be ran')
    parser.add_argument('--verbose', '-v', action='store_true', help='Switch DEBUG logging on')

    subparsers = parser.add_subparsers(dest='churn', help='Specify churn and its arguments')

    churn_parser = subparsers.add_parser('churn', help='Activate churn')
    churn_parser.add_argument('delta', type=int,
                              help='The interval between killing/adding new containers in ms')
    churn_parser.add_argument('--kill-coordinator', '-k', type=int, nargs='+', default=[],
                              help='Kill the coordinator at the specified periods')
    churn_parser.add_argument('--synthetic', '-s', metavar='N', type=churn_tuple, nargs='+',
                              help='Pass the synthetic list (to_kill,to_create)(example: 0,100 0,1 1,0)')
    churn_parser.add_argument('--delay', '-d', type=int, default=0,
                              help='With how much delay compared to the tester should the tester start in ms')

    args = parser.parse_args()
    if not args.fixed_rate:
        args.fixed_rate = args.peer_number
    if args.verbose:
        log_level = logging.DEBUG
    else:
        log_level = logging.INFO
    create_logger()
    logger = logging.getLogger('benchmarks')
    logger.setLevel(log_level)

    logger.info('START')
    args.time_add *= 1000
    args.time_to_run *= 1000

    def signal_handler(signal, frame):
        logger.info('Stopping Benchmarks')
        try:
            cli.remove_service(TRACKER_NAME)
            cli.remove_service(SERVICE_NAME)
            if not args.local:
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
        exit(0)
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    if args.local:
        service_image = SERVICE_NAME
        tracker_image = TRACKER_NAME
        with subprocess.Popen(['../gradlew', '-p', '..', 'docker'],
                              stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              universal_newlines=True) as p:
            for line in p.stdout:
                print(line, end='')
    else:
        service_image = DISTANT_REPOSITORY + SERVICE_NAME
        tracker_image = DISTANT_REPOSITORY + TRACKER_NAME
        for line in cli.pull(service_image, stream=True, decode=True):
            print(line)
        for line in cli.pull(tracker_image, stream=True):
            print(line)
    try:
        cli.init_swarm()
        if not args.local:
            logger.info('Joining Swarm on every hosts:')
            token = cli.inspect_swarm()['JoinTokens']['Worker']
            subprocess.call(['parallel-ssh', '-t', '0', '-h', 'hosts', 'docker', 'swarm',
                             'join', '--token', token, '{:s}:2377'.format(MANAGER_IP)])
        ipam_pool = utils.create_ipam_pool(subnet=SUBNET)
        ipam_config = utils.create_ipam_config(pool_configs=[ipam_pool])
        cli.create_network(NETWORK_NAME, 'overlay', ipam=ipam_config)
    except errors.APIError:
        logger.info('Host is already part of a swarm')
        if not cli.networks(names=[NETWORK_NAME]):
            logger.error('Network  doesn\'t exist!')
            exit(1)

    for run_nb, _ in enumerate(range(args.runs), 1):
        create_service(TRACKER_NAME, tracker_image, placement={'Constraints': ['node.role == manager']})
        wait_on_service(TRACKER_NAME, 1)
        time_to_start = int((time.time() * 1000) + args.time_add)
        logger.debug(datetime.utcfromtimestamp(time_to_start / 1000).isoformat())
        environment_vars = {'PEER_NUMBER': args.peer_number, 'TIME': time_to_start,
                            'TIME_TO_RUN': args.time_to_run, 'RATE': args.rate,
                            'FIXED_RATE': args.fixed_rate}
        environment_vars = ['{:s}={:d}'.format(k, v) for k, v in environment_vars.items()]
        logger.debug(environment_vars)

        service_replicas = 0 if args.churn else args.peer_number
        log_storage = LOCAL_DATA if args.local else CLUSTER_DATA
        create_service(SERVICE_NAME, service_image, env=environment_vars,
                       mounts=[types.Mount(target='/data', source=log_storage, type='bind')], replicas=service_replicas)

        logger.info('Running JGroups tester -> Experiment: {:d}/{:d}'.format(run_nb, args.runs))
        if args.churn:
            thread = threading.Thread(target=run_churn, args=[time_to_start + args.delay], daemon=True)
            thread.start()
            wait_on_service(SERVICE_NAME, 0, inverse=True)
            logger.info('Running with churn')
            if args.synthetic:
                # Wait for some peers to at least start
                time.sleep(120)
                total = [sum(x) for x in zip(*args.synthetic)]
                # Wait until only stopped containers are still alive
                wait_on_service(SERVICE_NAME, containers_nb=total[0], total_nb=total[1])
            else:
                thread.join()  # Wait for churn to finish
                time.sleep(600)  # Wait 10 more minutes
        else:
            wait_on_service(SERVICE_NAME, 0, inverse=True)
            logger.info('Running without churn')
            wait_on_service(SERVICE_NAME, 0)

        logger.info('Waiting for stabilisation')
        time.sleep(20)
        cli.remove_service(SERVICE_NAME)
        cli.remove_service(TRACKER_NAME)

        logger.info('Services removed')
        time.sleep(30)

        if not args.local:
            subprocess.call('parallel-ssh -t 0 -h hosts "mkdir -p {path}/test-{nb}/capture &&'
                            ' mv {path}/*.txt {path}/test-{nb}/ &&'
                            ' mv {path}/capture/*.csv {path}/test-{nb}/capture/"'
                            .format(path=CLUSTER_DATA, nb=run_nb), shell=True)

        subprocess.call('mkdir -p {path}/test-{nb}/capture'.format(path=log_storage, nb=run_nb),
                        shell=True)
        subprocess.call('mv {path}/*.txt {path}/test-{nb}/'.format(path=log_storage, nb=run_nb),
                        shell=True)
        subprocess.call('mv {path}/capture/*.csv {path}/test-{nb}/capture/'.format(path=log_storage, nb=run_nb),
                        shell=True)

    logger.info('Benchmark done!')

