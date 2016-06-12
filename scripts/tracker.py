'''
__author__ = "Sebastien Vaucher"
'''

import subprocess
from random import shuffle
from http.server import HTTPServer, BaseHTTPRequestHandler


def florida_string():
    # pydevd.settrace('192.168.1.201', port=9292, stdoutToServer=True, stderrToServer=True)
    # TODO take IP from context
    
    #in this way docker-compose won't work in the container so instead of the command you wrote originaly, you should just do:
    #ps_output = subprocess.check_output('docker-compose -H tcp://192.168.1.201:2375 ps epto', shell=True).decode().splitlines()
    ps_output = subprocess.check_output('docker ps', shell=true).decode().splitlines()
    #and then find the containers with eptoneem_epto_x name

    nodes_names = [x.split(' ')[0] for x in ps_output if x.startswith('eptoneem')]
    shuffle(nodes_names)
    # TODO change 10 to k later
    return '|'.join(nodes_names[:10]).encode()


class FloridaHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/REST/v1/admin/get_view':
            self.send_response(200)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            self.wfile.write(florida_string())
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
