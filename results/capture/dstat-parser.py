#!/usr/bin/env python3.5
import argparse
import csv
from itertools import islice
from itertools import zip_longest

parser = argparse.ArgumentParser(description='Process Bytes logs')
parser.add_argument('files', metavar='FILE', nargs='+', type=argparse.FileType('r'),
                    help='the files to parse')
parser.add_argument('-n', '--name', type=str, help='the name of the file to write the result to',
                    default='total-bytes.csv')
args = parser.parse_args()


def open_files():
    for file in args.files:
            yield csv.DictReader(islice(file, 6, None))


def extract():
    csv_readers = open_files()
    reader = zip_longest(*csv_readers, fillvalue={'recv': '0.0', 'send': '0.0'})
    for rows in reader:
        yield {'recv': sum(float(row['recv']) for row in rows),
               'send': sum(float(row['send']) for row in rows)}


with open(args.name, 'w') as csvfile:
    fieldnames = ['recv', 'send']
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)

    writer.writeheader()
    writer.writerows(extract())
