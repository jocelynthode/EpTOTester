#!/usr/bin/env python3.5
import re
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import networkx as nx

graph = []


def extract_nodes(from_seconds=0):
    def concat_lines():
        for fpath in Path().glob('*.txt'):
            with fpath.open() as f:
                yield from f

    lines = list(concat_lines())

    def extract_starts(lines):
        for line in lines:
            match = re.match(r'(\d+) - Started.*', line)
            if match:
                yield int(match.group(1))

    start_at = min(extract_starts(lines))
    print(start_at)

    min_time = start_at + from_seconds * 1000  # Timestamp are in milliseconds
    views = {}
    for line in lines:
        match = re.match(r'(\d+) - PSS View: ([0-9. ]+)', line)
        if not match:
            continue
        timestamp, nodes_str = match.groups()
        timestamp = int(timestamp)
        source, *nodes = filter(None, nodes_str.split(' '))

        if timestamp > min_time and source not in views:
            views[source] = nodes

    for source, nodes in views.items():
        yield ' '.join([source, *nodes])


def create_graph(from_seconds=30):
    return nx.parse_adjlist(extract_nodes(from_seconds))


G = create_graph(int(sys.argv[1]))
D = nx.DiGraph(G)
nx.draw_circular(G, with_labels=True)
print(nx.average_shortest_path_length(D))
print(nx.average_clustering(G))
indeg = D.in_degree()
# print(indeg)
print("Node nb: " + str(len(indeg)))
print("Average indegree: " + str(sum(indeg.values()) / len(indeg)))

plt.show()
