#!/usr/bin/env python3.5
"""
Author: Jocelyn Thode

Create the csv files from the data extracted by epto-tester.py. These csv files are then used in the LaTeX report to
generate the figures

It also creates quick matplotlib figures to have an early peek in the data

"""
import argparse
import csv
import re
import bitmath
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import progressbar



parser = argparse.ArgumentParser(description='Process Bytes logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse')
parser.add_argument('--local-times', metavar='FILE', nargs='+', type=str,
                    help='the files to parse', required=False)
parser.add_argument('--global-times', metavar='FILE', nargs='+', type=str,
                    help='the files to parse', required=False)
parser.add_argument('--local-delta-times', metavar='FILE', nargs='+', type=str,
                    help='the files to parse', required=False)
# parser.add_argument('--global-delta-times', metavar='FILE', nargs='+', type=str,
#                     help='the files to parse', required=False)
parser.add_argument('--events-sent', metavar='FILE', nargs='+', type=str,
                    help='the files to parse', required=False)
parser.add_argument('-n', '--name', type=str, help='the name of the file to write the result to',
                    default='plot')
args = parser.parse_args()


def open_files():
    dfs = []
    bar = progressbar.ProgressBar()
    for file in bar(args.files):
        match = re.match('(.+)/results.*/(.+)/test-(\d+)/', file)
        if match:
            experiment_nb = int(match.group(3))
            if 'jgroups' in match.group(1).casefold():
                test = 'JGroups-{:s}'.format(match.group(2))
            elif 'epto' in match.group(1).casefold():
                test = 'EpTO-{:s}'.format(match.group(2))
            else:
                raise EnvironmentError("Couldn't find a folder named after either EpTO or JGroups")
        else:
            experiment_nb = -1
            test = 'none'
        df = pd.read_csv(file, skiprows=6)
        df['time'] = range(len(df))
        df['experiment_nb'] = experiment_nb
        df['test'] = test
        dfs.append(df)
    return dfs


print('Creating average bytes stats')
dfs = open_files()
all_df = pd.concat(dfs)
all_df['throughput'] = (all_df['recv'] + all_df['send']) / (10**6)

quantiles = all_df.drop(['recv', 'send', 'experiment_nb'], axis=1).groupby(['test', 'time']).apply(
    lambda x: x.quantile([.0, .25, .50, .75, 1.]))

quantiles = quantiles.drop('time', axis=1)
quantiles.index.names = ['test', 'time', 'quantile']
quantiles = quantiles.unstack(level=[0, 2])['throughput']
columns = []
data = {}
for name, serie in quantiles.iteritems():
    new_name = '{:s}-{:f}'.format(name[0], name[1])
    columns.append(new_name)
    data[new_name] = serie.values

average_df = pd.DataFrame(data=data, columns=columns, index=quantiles.index)
average_df.to_csv('average-bytes-sent-recv.csv', na_rep='nan')

print('Creating total bytes stats')
plt.figure()
sum_df = all_df.groupby(['test', 'experiment_nb']).sum()[['recv', 'send']]
means = sum_df.mean(level=0)
std = sum_df.std(level=0)
ax = means.plot.bar(yerr=std, figsize=(20, 20), rot=30)

for rect in ax.patches:
    height = rect.get_height()
    ax.text(rect.get_x() + rect.get_width() / 2, height + 5,
            bitmath.Byte(float(height)).to_GB().format("{value:.2f} {unit}"), ha='center', va='bottom')

plt.title('Average total number of bytes sent/received for an experiment')
ax.set_ylabel('bytes')
plt.savefig('{:s}-peer-total.png'.format(args.name))
means['recv-error'] = std['recv'].values
means['send-error'] = std['send'].values
means.to_csv('total-bytes-sent-recv.csv', na_rep='nan')

def open_events(files):
    dfs = []
    bar = progressbar.ProgressBar()
    for file in bar(files):
        match = re.match('(.+)/results.*/(.+)/.*\.csv', file)
        if match:
            if 'jgroups' in match.group(1).casefold():
                test = 'JGroups-{:s}'.format(match.group(2))
            elif 'epto' in match.group(1).casefold():
                test = 'EpTO-{:s}'.format(match.group(2))
            else:
                raise EnvironmentError("Couldn't find a folder named after either EpTO or JGroups")
        else:
            test = 'none'
        df = pd.read_csv(file)
        df['test'] = test
        dfs.append(df)
    return dfs


print('Creating total events sent stats')
plt.figure()
event_sent_dfs = pd.concat(open_events(args.events_sent)).groupby('test')
means = event_sent_dfs.mean()
std = event_sent_dfs.std()
ax = means.plot.bar(yerr=std, figsize=(20, 20), rot=30)
means['error'] = std['events-sent'].values
means.to_csv('total-events-sent.csv', na_rep='nan')


def open_times(files):
    dfs = []
    bar = progressbar.ProgressBar()
    for file in bar(files):
        match = re.match('(.+)/results.*/(.+)/.*\.csv', file)
        if match:
            if 'jgroups' in match.group(1).casefold():
                test = 'JGroups-{:s}'.format(match.group(2))
            elif 'epto' in match.group(1).casefold():
                test = 'EpTO-{:s}'.format(match.group(2))
            else:
                raise EnvironmentError("Couldn't find a folder named after either EpTO or JGroups")
        else:
            test = 'none'
        df = pd.read_csv(file)
        df['test'] = test
        dfs.append(df)
    return dfs


def create_cdf_plot(dfs, name, precision=1000):
    headers = []

    for df in dfs:
        headers.append('{:s}-x'.format(df['test'][0]))
        headers.append('{:s}-y'.format(df['test'][0]))
    with open(name + '.csv', 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, headers)
        writer.writeheader()

        for df in dfs:
            x_row = '{:s}-x'.format(df['test'][0])
            y_row = '{:s}-y'.format(df['test'][0])
            data = df.quantile(np.linspace(0, 1, precision+1))
            for x, y in zip(data.values, data.index):
                writer.writerow({x_row: x[0] / 1000, y_row: y})


print('Creating local times stats')
global_time_dfs = open_times(args.global_times)
create_cdf_plot(global_time_dfs, '{:s}-global-time-cdf'.format(args.name), 10)

print('Creating local times stats')
local_time_dfs = open_times(args.local_times)
create_cdf_plot(local_time_dfs, '{:s}-local-time-cdf'.format(args.name))

print('Creating local delta stats')
local_delta_dfs = open_times(args.local_delta_times)
create_cdf_plot(local_delta_dfs, '{:s}-local-deltas-cdf'.format(args.name))

# global_delta_dfs = open_times(args.global_delta_times)
# create_cdf_plot(global_delta_dfs, '{:s}-global-deltas-cdf'.format(args.name), 'Global Deltas')

