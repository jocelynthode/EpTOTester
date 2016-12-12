#!/usr/bin/env python3
'''
__author__ = "Jocelyn Thode"

inspired from a script from Sebastien Vaucher
'''

# import pydevd
import random
import logging

import time
from http.server import HTTPServer, BaseHTTPRequestHandler

available_peers = {}
K = 25
logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.INFO)


def florida_string(ip):
    available_peers[ip] = int(time.time())

    to_choose = list(available_peers.keys())
    logging.info("View size: {:d}".format(len(to_choose)))
    to_choose.remove(ip)
    if len(to_choose) > K:
        to_send = random.sample(to_choose, K)
    else:
        to_send = to_choose

    return '|'.join(to_send).encode()


class FloridaHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/REST/v1/admin/get_view':
            self.send_response(200)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            self.wfile.write(florida_string(self.client_address[0]))
        else:
            self.send_response(404)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            self.wfile.write(b"Nothing here, content is at /REST/v1/admin/get_view\n")


class FloridaServer:
    def __init__(self):
        self.server = HTTPServer(('', 4321), FloridaHandler)
        self.server.serve_forever()


FloridaServer()
