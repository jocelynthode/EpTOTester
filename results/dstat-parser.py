#!/usr/bin/env python3.5
import argparse
import matplotlib.pyplot as plt
import pandas as pd
import re
from bitmath import best_prefix, SI

parser = argparse.ArgumentParser(description='Process Bytes logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse')
parser.add_argument('-e', '--experiments', type=int, help='the number of experiments',
                    default=1)
parser.add_argument('-n', '--name', type=str, help='the name of the file to write the result to',
                    default='plot')
args = parser.parse_args()

def open_files():
    dfs = []
    for idx, file in enumerate(args.files):
        match = re.match('.*test-(\d+)/', file)
        if match:
            experiment_nb = int(match.group(1))
        else:
            experiment_nb = -1
        df = pd.read_csv(file, skiprows=6)
        df['time'] = range(len(df))
        df['file'] = idx
        df['experiment_nb'] = experiment_nb
        dfs.append(df)
    return dfs

plt.figure()
dfs = open_files()
all_df = pd.concat(dfs)
mean = all_df.groupby('time').mean()[['recv', 'send']].plot(colormap='coolwarm')
plt.savefig('{:s}-peer-mean.png'.format(args.name))
plt.close()


plt.figure()
sum_df = all_df.groupby('experiment_nb').sum()[['recv', 'send']]
means = sum_df.mean()
std = sum_df.std()
means_transformed = pd.DataFrame(data={'received': means.values[0], 'sent': means.values[1]}, index=['EpTO'])
std_transformed = pd.DataFrame(data={'received': std.values[0], 'sent': std.values[1]}, index=['EpTO'])
ax = means_transformed.plot.bar(yerr=std_transformed, colormap='coolwarm')
for rect, value in zip(ax.patches, sum_df.mean()):
    height = rect.get_height()
    ax.text(rect.get_x() + rect.get_width()/2, height + 5,
            best_prefix(float(value), system=SI).format("{value:.3f} {unit}"), ha='center', va='bottom')

plt.savefig('{:s}-peer-total.png'.format(args.name))


def create_cdf_plot(df, name):
    # http://stackoverflow.com/a/26394108/2826574
    plt.figure()
    axes = df.plot.hist(cumulative=True, normed=1, bins=100, histtype='step', colormap='coolwarm')
    # Remove right border of last bin
    axes.patches[0].set_xy(axes.patches[0].get_xy()[:-1])
    axes.set_xticklabels(axes.get_xticks())
    axes.set_ylim(0, 1)
    plt.savefig(name)

time_df = pd.read_csv('local-time-stats.csv', squeeze=True)
create_cdf_plot(time_df, '{:s}-local-time-cdf.png'.format(args.name))

time_df = pd.read_csv('global-time-stats.csv', squeeze=True)
create_cdf_plot(time_df, '{:s}-global-time-cdf.png'.format(args.name))

delta_df = pd.read_csv('delta-stats.csv', squeeze=True)
create_cdf_plot(delta_df, '{:s}-deltas-cdf.png'.format(args.name))


