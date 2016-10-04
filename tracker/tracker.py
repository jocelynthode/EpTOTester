#!/usr/bin/env python2.7
'''
__author__ = "Jocelyn Thode"

inspired from a script from Sebastien Vaucher
'''

# import pydevd
import random
import time
from http.server import HTTPServer, BaseHTTPRequestHandler

available_peers = {}
K = 30


def florida_string(ip):
    available_peers[ip] = int(time.time())

    if len(available_peers) > K:
        to_send = random.sample(available_peers.keys(), K)
    else:
        to_send = list(available_peers.keys())

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
