#!/usr/bin/env python3.5
import re
import sys
from pathlib import Path

import argparse
import networkx as nx
import statistics as stats

graph = []


def extract_nodes(from_seconds=0):
    def concat_lines():
        for fname in args.files:
            with open(fname, 'r') as f:
                yield from f

    lines = list(concat_lines())

    def extract_starts(lines):
        for line in lines:
            match = re.match(r'(\d+) - Running init PSS-1', line)
            if match:
                yield int(match.group(1))

    start_at = min(extract_starts(lines))

    min_time = start_at + from_seconds * 1000  # Timestamp are in milliseconds
    views = {}
    for line in lines:
        match = re.match(r'(\d+) - PSS View: ([0-9. ]+)', line)
        if not match:
            continue
        timestamp, nodes_str = match.groups()
        timestamp = int(timestamp)
        source, *nodes = filter(None, nodes_str.split(' '))

        if timestamp >= min_time and source not in views:
            views[source] = nodes

    for source, nodes in views.items():
        yield ' '.join([source, *nodes])


def create_graph(from_seconds=30):
    D = nx.DiGraph()
    return nx.parse_adjlist(extract_nodes(from_seconds), create_using=D)


parser = argparse.ArgumentParser(description='Process PSS logs')
parser.add_argument('from_seconds', metavar='FROM_SECONDS', type=int,
                    help='from which point must the snapshot be taken')
parser.add_argument('files', metavar='FILE', nargs='+', type=str,
                    help='the files to parse')
args = parser.parse_args()

G = create_graph(args.from_seconds)
U = G.to_undirected()

print("Average shortest path: %f" % nx.average_shortest_path_length(G))
print("Average clustering: %f" % nx.average_clustering(U))
print('Diameter of the graph: %d' % nx.diameter(G))
print("Is graph strongly connected: %s" % nx.is_strongly_connected(G))
indeg = G.in_degree()
print(indeg)
print("Node nb: %d" % G.number_of_nodes())
print("Average indegree: %f" % (sum(indeg.values()) / len(indeg)))
print("Median indegree %f" % stats.median(indeg.values()))

# nx.draw_circular(G, with_labels=True)
# plt.show()
