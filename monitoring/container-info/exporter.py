#!/usr/bin/env python3
import requests_unixsocket
from http.server import BaseHTTPRequestHandler, HTTPServer

session = requests_unixsocket.Session()
DOCKER_SOCK = "http+unix://%2Fvar%2Frun%2Fdocker.sock"

def get_containers():
    r = session.get(f"{DOCKER_SOCK}/containers/json?all=0")
    r.raise_for_status()
    return r.json()

def generate_metrics():
    containers = get_containers()
    lines = []

    for c in containers:
        cid = c["Id"]
        name = c["Names"][0].lstrip("/")
        image = c["Image"]

        labels = c.get("Labels", {})
        project = labels.get("com.docker.compose.project", "standalone")
        service = labels.get("com.docker.compose.service", "none")

        lines.append(
            f'docker_container_info{{id="{cid}",name="{name}",image="{image}",project="{project}",service="{service}"}} 1'
        )

    return "\n".join(lines) + "\n"

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != "/metrics":
            self.send_response(404)
            self.end_headers()
            return

        metrics = generate_metrics()
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; version=0.0.4")
        self.end_headers()
        self.wfile.write(metrics.encode())

def main():
    server = HTTPServer(("", 9323), Handler)
    print("Exporter running on :9323/metrics")
    server.serve_forever()

if __name__ == "__main__":
    main()
