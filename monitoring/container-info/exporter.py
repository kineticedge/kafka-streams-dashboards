#!/usr/bin/env python3

import os
import requests_unixsocket
from http.server import BaseHTTPRequestHandler, HTTPServer

# import docker
# client = docker.from_env()
# info = client.containers.get(cid).attrs
# pid = info["State"]["Pid"]

session = requests_unixsocket.Session()
DOCKER_SOCK = "http+unix://%2Fvar%2Frun%2Fdocker.sock"

def find_cgroup_path(cid):
    # Docker Desktop (macOS)
    p1 = f"/sys/fs/cgroup/docker/{cid}"
    if os.path.exists(p1):
        return p1
    # Linux Docker Engine (systemd)
    p2 = f"/sys/fs/cgroup/system.slice/docker-{cid}.scope"
    if os.path.exists(p2):
        return p2
    # Linux Docker Engine (cgroupfs)
    p3 = f"/sys/fs/cgroup/{cid}"
    if os.path.exists(p3):
        return p3
    return None

def get_containers():
    r = session.get(f"{DOCKER_SOCK}/containers/json?all=0")
    r.raise_for_status()
    return r.json()

def get_container_details(cid):
    r = session.get(f"{DOCKER_SOCK}/containers/{cid}/json")
    r.raise_for_status()
    return r.json()

pid_cache = {}
def get_pid(cid):

    # git pid from cache, verify process still exists
    if cid in pid_cache:
        pid = pid_cache[cid]
        if os.path.exists(f"/proc/{pid}"):
            return pid
        del pid_cache[cid]

    r = session.get(f"{DOCKER_SOCK}/containers/{cid}/json")
    r.raise_for_status()
    pid = r.json()["State"]["Pid"]
    started_at = r.json()["State"]["StartedAt"]
    pid_cache[cid] = (pid, started_at)
    return pid

device_cache = {}
def get_device_name(major_minor):
    """Resolves '254:0' to a device name. Mapping is global to the kernel."""
    if major_minor in device_cache:
        return device_cache[major_minor]
    try:
        if os.path.exists("/proc/partitions"):
            with open("/proc/partitions", "r") as f:
                for line in f:
                    parts = line.split()
                    if len(parts) < 4 or not parts[0].isdigit():
                        continue

                    # Store mapping: "254:0" -> "/dev/vda"
                    device_cache[f"{parts[0]}:{parts[1]}"] = f"/dev/{parts[3]}"
    except Exception:
        pass
    # Fallback to raw ID if resolution fails
    return device_cache.get(major_minor, major_minor)


def get_container_metrics_from_cgroup(cid, name, project, service):
    base_path = find_cgroup_path(cid)
    lines = []

    # details = get_container_details(cid)
    # pid = details["State"]["Pid"]
    # #print(f"pid={pid}, cid={cid}, name={name}, project={project}, service={service}, pid={pid}, base_path={base_path}")
    # print(f"map={details}")

    pid = get_pid(cid);
    print(f"pid={pid}, cid={cid}, name={name}, project={project}, service={service}, pid={pid}, base_path={base_path}")

    try:
        # 1. Total Memory Usage
        if os.path.exists(f"{base_path}/memory.current"):
            with open(f"{base_path}/memory.current", "r") as f:
                val = f.read().strip()
                lines.append(f'container_memory_bytes{{key="usage", id="{cid}", name="{name}", project="{project}", service="{service}"}} {val}')

        # 2. Memory Limit (if set)
        if os.path.exists(f"{base_path}/memory.max"):
            with open(f"{base_path}/memory.max", "r") as f:
                val = f.read().strip()
                # 'max' in cgroup v2 means no limit
                if val != "max":
                    lines.append(f'container_memory_bytes{{key="limit", id="{cid}", name="{name}", project="{project}", service="{service}"}} {val}')
                else:
                    lines.append(f'container_memory_bytes{{key="limit", id="{cid}", name="{name}", project="{project}", service="{service}"}} 0')

        # 3. Granular breakdown from memory.stat (The "Advanced" Metrics)
        if os.path.exists(f"{base_path}/memory.stat"):
            with open(f"{base_path}/memory.stat", "r") as f:
                for line in f:
                    parts = line.split()
                    if len(parts) != 2: continue
                    key, val = parts[0], parts[1]
                    # 'anon' - rss / heap + native
                    # file = total page cache
                    # inactive_file = page cache that can be reclaimed easily
                    # mapped_file = memory mapped files (rocksdb index/filter blocks)
                    # unevictable = pinned memory (dangerous for OOM)
                    # workingset = cAdvisor-style 'working set' if exported by kernel
                    # pagefaults = major page faults (container is hitting disk for memory)
                    if key in ["anon", "file", "inactive_file", "file_mapped", "unevictable", "pgmajfault", "kernel_stack", "shmem", "slab", "sock"]:
                        lines.append(f'container_memory_bytes{{key="{key}", id="{cid}", name="{name}", project="{project}", service="{service}"}} {val}')

        # 4. CPU Usage (Cumulative nanoseconds)
        if os.path.exists(f"{base_path}/cpu.stat"):
            with open(f"{base_path}/cpu.stat", "r") as f:
                for line in f:
                    parts = line.split()
                    if len(parts) != 2: continue
                    key, val = remove_suffix(parts[0], "_usec"), float(parts[1]) / 1000000
                    if key in ["usage", "system", "user"]:
                        lines.append(f'container_cpu_seconds_total{{key="{key}", id="{cid}", name="{name}", project="{project}", service="{service}"}} {val}')

        if os.path.exists(f"{base_path}/io.stat"):
            with open(f"{base_path}/io.stat", "r") as f:
                for line in f:
                    parts = line.split()
                    if len(parts) < 2: continue

                    # parts[0] is the device major:minor (e.g., "254:0")
                    dev = parts[0]
                    dev_name = get_device_name(dev)
                    # Create a map from the key=value pairs (rbytes=123, wbytes=456, etc.)
                    fields = {}
                    for p in parts[1:]:
                        if '=' in p:
                            k, v = p.split('=')
                            fields[k] = v

                    # Map to Prometheus-style metrics
                    mapping = {
                        "rbytes": "container_fs_reads_bytes_total",
                        "wbytes": "container_fs_writes_bytes_total",
                        "rios":   "container_fs_reads_total",
                        "wios":   "container_fs_writes_total"
                    }

                    for field_key, metric_name in mapping.items():
                        if field_key in fields:
                            lines.append(f'{metric_name}{{id="{cid}", name="{name}", project="{project}", service="{service}", device="{dev_name}"}} {fields[field_key]}')

                # for line in f:
                #     parts = line.split()
                #     if len(parts) != 2: continue
                #     key, val = parts[0], int(parts[1]
                #     if key in ["rbytes", "wbytes"]:
                #         lines.append(f'container_io_bytes{{key="{key}", id="{cid}", name="{name}", project="{project}", service="{service}"}} {val}')

        pid_info = get_pid(cid)
        # get_pid returns (pid, started_at) based on your cache logic
        pid = pid_info[0] if isinstance(pid_info, tuple) else pid_info

        # ... existing memory/cpu/io code ...

        # 5. Network IO
        if pid:
            lines.extend(get_network_metrics(pid, cid, name, project, service))

        #lines.extend(get_host_network_metrics())

    except Exception as e:
        print(f"Error reading cgroups for {name}: {e}")

    return lines

def get_network_metrics(pid, cid, name, project, service):
    lines = []
    path = f"/proc/{pid}/net/dev"
    if not os.path.exists(path):
        return lines
    try:
        with open(path, "r") as f:
            # Skip the two header lines
            next(f)
            next(f)
            for line in f:
                parts = line.split()
                if not parts: continue

                # Interface name is parts[0] (e.g., "eth0:")
                iface = parts[0].strip(":")
                if iface in ["lo", "tunl0", "gre0", "gretap0", "erspan0", "ip_vti0", "ip6tnl0", "ip6gre0", "ip6_vti0", "sit0"]:
                    continue

                # Index 1: Receive Bytes, Index 9: Transmit Bytes
                lines.append(f'container_network_receive_bytes_total{{id="{cid}", name="{name}", project="{project}", service="{service}", interface="{iface}"}} {parts[1]}')
                lines.append(f'container_network_transmit_bytes_total{{id="{cid}", name="{name}", project="{project}", service="{service}", interface="{iface}"}} {parts[9]}')
                # Index 2: Receive Packets, Index 10: Transmit Packets
                lines.append(f'container_network_receive_packets_total{{id="{cid}", name="{name}", project="{project}", service="{service}", interface="{iface}"}} {parts[2]}')
                lines.append(f'container_network_transmit_packets_total{{id="{cid}", name="{name}", project="{project}", service="{service}", interface="{iface}"}} {parts[10]}')
    except Exception as e:
        print(f"Error reading network for pid {pid}: {e}")
    return lines


# def get_host_network_metrics():
#     """Reads network stats for the physical host/docker bridges."""
#     lines = []
#     path = "/proc/1/net/dev" # Since we are pid:host, proc/1 is the host
#
#     if not os.path.exists(path):
#         return lines
#
#     try:
#         with open(path, "r") as f:
#             next(f); next(f) # Skip headers
#             for line in f:
#                 parts = line.split()
#                 iface = parts[0].strip(":")
#
#                 # 'docker0' is the default bridge
#                 # 'br-...' are custom bridges (like your 'ksd' network)
#                 #if iface.startswith("br-") or iface == "docker0":
#                 lines.append(f'host_network_receive_bytes_total{{interface="{iface}"}} {parts[1]}')
#                 lines.append(f'host_network_transmit_bytes_total{{interface="{iface}"}} {parts[9]}')
#     except Exception:
#         pass
#     return lines


def remove_suffix(name, suffix):
    if suffix and name.endswith(suffix):
        return name[:-len(suffix)]
    return name

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
            f'container_info{{id="{cid}",name="{name}",project="{project}",service="{service}",image="{image}"}} 1'
        )

        lines.extend(get_container_metrics_from_cgroup(cid, name, project, service))

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
