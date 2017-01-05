#!/usr/bin/env python3
import socketserver
import sys
import logging
import threading
import urllib.request
import time
import socket

PORT = 15342


class MyUDPHandler(socketserver.BaseRequestHandler):
    """
    This class works similar to the TCP handler class, except that
    self.request consists of a pair of data and client socket, and since
    there is no connection the client address must be given explicitly
    when sending data back via sendto().
    """

    def handle(self):
        data = self.request[0].strip().decode("utf-8")
        logging.info("Message received from {} during loop {}".format(self.client_address[0], data))


class ThreadedUDPServer(socketserver.ThreadingMixIn, socketserver.UDPServer):
    pass


if __name__ == "__main__":
    HOST = sys.argv[1]
    logging.basicConfig(format='%(asctime)s - %(levelname)s: %(message)s', level=logging.INFO,
                        filename='/data/{}.test'.format(HOST))
    server = ThreadedUDPServer((HOST, PORT), MyUDPHandler)
    logging.info("Create server listening on {}:{}".format(HOST, PORT))
    server_thread = threading.Thread(target=server.serve_forever)
    server_thread.daemon = True
    server_thread.start()
    logging.info("Sleeping for 120s")
    time.sleep(120)
    logging.info("Finished sleeping")
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    content = urllib.request.urlopen('http://epto-tracker:4321/REST/v1/admin/get_view').read()
    content = content.decode("utf-8")
    addresses = content.split('|')
    logging.info("View size: {}".format(len(addresses)))
    i = 0
    while True:
        logging.info("Loop {}".format(i))
        for address in addresses:
            try:
                logging.info("Sending to {}".format(address))
                sock.sendto(bytes('{}'.format(i), "utf-8"), (address, PORT))
            except:
                logging.exception("Exception catched for address: {}".format(address))
        time.sleep(5)
        i += 1

