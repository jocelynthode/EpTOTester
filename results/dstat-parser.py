#!/usr/bin/env python3.5
import argparse
import bitmath
import matplotlib.style as pltstyle
import matplotlib.pyplot as plt
import pandas as pd
import progressbar
import re

pltstyle.use('ggplot')
parser = argparse.ArgumentParser(description='Process Bytes logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse')
parser.add_argument('--local-times', metavar='FILE', nargs='+', type=str,
                    help='the files to parse', required=True)
parser.add_argument('--global-times', metavar='FILE', nargs='+', type=str,
                    help='the files to parse', required=True)
parser.add_argument('--delta-times', metavar='FILE', nargs='+', type=str,
                    help='the files to parse', required=False)
parser.add_argument('-n', '--name', type=str, help='the name of the file to write the result to',
                    default='plot')
args = parser.parse_args()


def open_files():
    dfs = []
    bar = progressbar.ProgressBar()
    for idx, file in enumerate(bar(args.files)):
        match = re.match('(.+)/test-(\d+)/', file)
        if match:
            experiment_nb = int(match.group(2))
            test = match.group(1)
        else:
            experiment_nb = -1
            test = 'none'
        df = pd.read_csv(file, skiprows=6)
        df['time'] = range(len(df))
        df['file'] = idx
        df['experiment_nb'] = experiment_nb
        df['test'] = test
        dfs.append(df)
    return dfs

plt.figure()
dfs = open_files()
all_df = pd.concat(dfs)
mean = all_df.groupby(['test', 'time']).mean()[['recv', 'send']].unstack(level=0).plot(figsize=(20, 20))
plt.title('Average bytes sent/received for a peer during an experiment')
mean.set_xlabel('time (seconds)')
mean.set_ylabel('bytes / seconds')

plt.savefig('{:s}-peer-mean.png'.format(args.name))
plt.close()

plt.figure()
sum_df = all_df.groupby(['test', 'experiment_nb']).sum()[['recv', 'send']]
means = sum_df.mean(level=0)
std = sum_df.std(level=0)
ax = means.plot.bar(yerr=std, figsize=(20, 20), rot=30)

for rect in ax.patches:
    height = rect.get_height()
    ax.text(rect.get_x() + rect.get_width()/2, height + 5,
            bitmath.Byte(float(height)).to_GB().format("{value:.2f} {unit}"), ha='center', va='bottom')

plt.title('Average total number of bytes sent/received for an experiment')
ax.set_ylabel('bytes')
plt.savefig('{:s}-peer-total.png'.format(args.name))


def open_times(files):
    dfs = []
    bar = progressbar.ProgressBar()
    for file in bar(files):
        match = re.match('(.+)/.*\.csv', file)
        if match:
            test = match.group(1)
        else:
            test = 'none'
        df = pd.read_csv(file)
        df['test'] = test
        dfs.append(df)
    return dfs


def create_cdf_plot(dfs, name, title):
    axes = None
    labels = []
    plt.figure()
    for df in dfs:
        labels.append(df['test'][0])
        axes = df.plot.hist(cumulative=True, normed=1, bins=100, histtype='step', ax=axes, legend=False)
        axes.set_xlabel('time (ms)')
        axes.set_ylim(0, 1)
    # Remove right border of last bin
    for patch in axes.patches:
        patch.set_xy(patch.get_xy()[:-1])
    lines = axes.get_legend_handles_labels()[0]

    axes.legend(lines, labels, loc='lower right')
    plt.title(title)
    plt.savefig(name)

local_time_dfs = open_times(args.local_times)
create_cdf_plot(local_time_dfs, '{:s}-local-time-cdf.png'.format(args.name), 'Local delivery time')

global_time_dfs = open_times(args.global_times)
create_cdf_plot(global_time_dfs, '{:s}-global-time-cdf.png'.format(args.name), 'Global delivery time')

#delta_dfs = open_times(args.delta_times)
#create_cdf_plot(delta_dfs, '{:s}-deltas-cdf.png'.format(args.name))


