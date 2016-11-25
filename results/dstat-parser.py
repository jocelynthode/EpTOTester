#!/usr/bin/env python3.5
import argparse
import csv
import re

import bitmath
import matplotlib.pyplot as plt
import matplotlib.style as pltstyle
import pandas as pd
import progressbar

pltstyle.use('ggplot')
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


plt.figure()
dfs = open_files()
all_df = pd.concat(dfs)

average_df = all_df.groupby(['test', 'time']).mean()[['recv', 'send']].unstack(level=0)
data = {}
names = []
for first, second in average_df:
    name = '{:s}-{:s}'.format(second, first)
    names.append(name)
    data.update({name: average_df[first][second].values})

pd.DataFrame(index=average_df.index, columns=names, data=data).to_csv('average-bytes-sent-recv.csv')

axes = average_df.plot(figsize=(20, 20))
plt.title('Average bytes sent/received for a peer during an experiment')
axes.set_xlabel('time (seconds)')
axes.set_ylabel('bytes / seconds')
plt.savefig('{:s}-peer-mean.png'.format(args.name))
plt.close()

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
means.to_csv('total-bytes-sent-recv.csv')


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


plt.figure()
event_sent_dfs = pd.concat(open_events(args.events_sent)).groupby('test')
means = event_sent_dfs.mean()
std = event_sent_dfs.std()
ax = means.plot.bar(yerr=std, figsize=(20, 20), rot=30)
means['recv-error'] = std['recv'].values
means['send-error'] = std['send'].values
means.to_csv('total-events-sent.csv')

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


def create_cdf_plot(dfs, name, title):
    headers = []
    for df in dfs:
        headers.append('{:s}-x'.format(df['test'][0]))
        headers.append('{:s}-y'.format(df['test'][0]))
    plt.figure()
    axes = None
    labels = []
    with open(name + '.csv', 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, headers)
        writer.writeheader()

        for df in dfs:
            labels.append(df['test'][0])
            axes = df.plot.hist(cumulative=True, normed=1, bins=100, histtype='step', ax=axes, legend=False)
            axes.set_xlabel('time (ms)')
            axes.set_ylim(0, 1)
        for idx, patch in enumerate(axes.patches):
            # Remove right border of last bin
            patch.set_xy(patch.get_xy()[:-1])
            x_row = '{:s}-x'.format(dfs[idx]['test'][0])
            y_row = '{:s}-y'.format(dfs[idx]['test'][0])
            bar = progressbar.ProgressBar()
            print('Writing  bin to csv...')
            for x, y in bar(patch.get_xy()):
                writer.writerow({x_row: x, y_row: y})

    lines = axes.get_legend_handles_labels()[0]
    axes.legend(lines, labels, loc='lower right')
    plt.title(title)
    plt.savefig(name + '.png')


global_time_dfs = open_times(args.global_times)
create_cdf_plot(global_time_dfs, '{:s}-global-time-cdf'.format(args.name), 'Global delivery time')

local_time_dfs = open_times(args.local_times)
create_cdf_plot(local_time_dfs, '{:s}-local-time-cdf'.format(args.name), 'Local delivery time')

local_delta_dfs = open_times(args.local_delta_times)
create_cdf_plot(local_delta_dfs, '{:s}-local-deltas-cdf'.format(args.name), 'Local Deltas')

# global_delta_dfs = open_times(args.global_delta_times)
# create_cdf_plot(global_delta_dfs, '{:s}-global-deltas-cdf'.format(args.name), 'Global Deltas')

