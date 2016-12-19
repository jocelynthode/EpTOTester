#!/usr/bin/env python3

import sqlite3
import time


class NodesTrace:
    """
    Author: Sébastien Vaucher
    From: https://github.com/sebyx31/ErasureBench/blob/e1dbac83dd7993302da049db700105b8fa4df69f/projects/erasure-tester/container-scripts/nodes_trace.py

    A generator for the number of nodes in the cluster at each time.
    There are 2 modes:
     * Synthetic: Return the next tuple in a list at each call
     * Database: Follow a real recorded failure trace

    For each call to the generator, returns a tuple:
    (current number of nodes, node IDs to kill, node IDs to create)

    Modifications from Jocelyn Thode:
     * Synthetic: Can now take a list of tuples of the form [(to_kill, to_create), ...]
     * Change static properties to instance properties
    """

    def __init__(self, time_factor=1, database=None, synthetic=None, min_time=None, max_time=None):
        assert database is not None or synthetic is not None

        self.synthetic_sizes = None
        self.sql = None
        self.last_time = -1000
        self.begin_time = None
        self.current_size = 0

        self.time_factor = time_factor

        if database is not None:
            self.sql = sqlite3.connect(database)
            self.cur = self.sql.cursor()
            # HUGE performance boost
            self.cur.execute(r'CREATE INDEX IF NOT EXISTS start_time_index ON event_trace (event_start_time)')
            self.cur.fetchone()

            self.cur.execute(r'SELECT MIN(event_start_time), MAX(event_start_time) FROM event_trace')
            self.min_time, self.max_time = self.cur.fetchone()
            if min_time is not None:
                self.min_time = min_time
            if max_time is not None:
                self.max_time = max_time

            self.cur.execute('''
              SELECT DISTINCT node_id FROM event_trace
                WHERE event_start_time <= ?
                ORDER BY node_id
                ''', (self.max_time,))
            self.nodes_id = [x[0] for x in self.cur.fetchall()]
            self.reverse_nodes_id = dict(zip(self.nodes_id, range(len(self.nodes_id))))
        elif synthetic is not None:
            self.synthetic_sizes = synthetic
            self.synthetic_index = -1

    def next(self):
        if self.synthetic_sizes is not None:
            self.synthetic_index += 1
            if self.synthetic_index >= len(self.synthetic_sizes):
                raise StopIteration()
            else:
                to_kill, to_create = self.synthetic_sizes[self.synthetic_index]
                next_size = self.current_size + (to_create - to_kill)
                if next_size < 0:
                    raise ArithmeticError("Cluster size must at least be equal to 0")
                ret = next_size, list(range(to_kill)), list(range(to_create))
                self.current_size = next_size
                return ret
        else:
            if self.last_time > self.max_time:
                raise StopIteration

            now = time.time()
            if self.begin_time is None:
                self.begin_time = now
            now = (now - self.begin_time) * self.time_factor + self.begin_time

            now_db = now - self.begin_time + self.min_time

            self.cur.execute('''
              SELECT node_id, event_type FROM event_trace
                WHERE event_start_time > ?
                  AND event_start_time <= ?
                ORDER BY node_id, event_start_time''',
                             (self.last_time, now_db))
            events = self.cur.fetchall()
            servers_to_kill = []
            servers_to_create = []

            nodes = {x[0] for x in events}
            for node in nodes:
                up_events = len([x for x in events if x[0] == node and x[1] == 1])
                down_events = len([x for x in events if x[0] == node and x[1] == 0])
                node_position = self.reverse_nodes_id[node]
                if up_events > down_events:
                    self.current_size += 1
                    servers_to_create.append(node_position)
                elif down_events > up_events:
                    self.current_size -= 1
                    servers_to_kill.append(node_position)

            self.last_time = now_db
            return self.current_size, servers_to_kill, servers_to_create

    def __iter__(self):
        return self

    def __next__(self):
        return self.next()

    def initial_size(self):
        if self.synthetic_sizes is not None:
            size = self.synthetic_sizes[0][1] - self.synthetic_sizes[0][0]
            if size < 0:
                raise ArithmeticError('Initial cluster size must be at least 0')
            return size
        else:
            self.cur.execute('''
              SELECT SUM(CASE event_type WHEN 1 THEN 1 ELSE -1 END) FROM event_trace
                WHERE event_start_time <= ?''',
                             (self.min_time,))
            return int(self.cur.fetchone()[0])


if __name__ == '__main__':
    # Test
    sut = NodesTrace(synthetic=[3, 4, 2, 3])
    count = 0
    initial = sut.initial_size()
    for r in sut:
        if count == 0:
            assert r[0] == initial
        print(r)
        assert count + len(r[1]) - len(r[2]) == r[0]
        count = r[0]

    sut = NodesTrace(time_factor=30, database='./databases/dummy.db')
    count = 0
    initial = sut.initial_size()
    print(initial)
    for r in sut:
        if count == 0:
            pass
            assert r[0] == initial
        print(r)
        assert count + len(r[2]) - len(r[1]) == r[0]
        time.sleep(1)
        count = r[0]
