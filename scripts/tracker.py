'''
__author__ = "Jocelyn Thode"

inspired from a script from Sebastien Vaucher
'''

#import pydevd
import time
import os
import random
from http.server import HTTPServer, BaseHTTPRequestHandler

available_peers = {}
K = 16


def florida_string(ip):
    available_peers[ip] = int(time.time())
    for ip, timestamp in available_peers.items():
        new_timestamp = int(time.time())
        if new_timestamp - timestamp > 180:
            to_keep = os.system("ping -c 1 -W 1 " + ip) == 0

            if not to_keep:
                available_peers.pop(ip)
            else:
                available_peers[ip] = new_timestamp


    # pydevd.settrace('192.168.1.201', port=9292, stdoutToServer=True, stderrToServer=True)

    if len(available_peers) > K:
        to_send = random.sample(available_peers.keys(), K)
    else:
        to_send = list(available_peers.keys())

    return '|'.join(to_send).encode()


# TODO add possiblity to request a view of size k
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
