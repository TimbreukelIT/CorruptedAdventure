"""Minimal RCON client (script and module).

    python -I tools/test/py/rcon.py <command> [<command> ...]     one command per argument
    python -I tools/test/py/rcon.py -                              commands from stdin, one per line

Port and password come from (first that is set): --port/--password, RCON_PORT/RCON_PASSWORD, the running server of
this checkout (build/test/server.state, written by server.sh), or <checkout>/run/server.properties.
As a module: `from rcon import Rcon; r = Rcon(); print(r.cmd("list"))`.
"""
import os
import socket
import struct
import sys

MAX_FRAGMENT = 4096  # characters per response packet (vanilla RconClient)
ROOT =os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))


def _kv(path, sep="="):
    out = {}
    if os.path.exists(path):
        for line in open(path, encoding="utf-8"):
            if sep in line and not line.startswith("#"):
                k, v = line.rstrip("\r\n").split(sep, 1)
                out[k.strip()] = v.strip()
    return out


def settings(port=None, password=None):
    state = _kv(os.path.join(ROOT, "build", "test", "server.state"))
    props = _kv(os.path.join(state.get("WT", ROOT), "run", "server.properties"))
    port = port or os.environ.get("RCON_PORT") or state.get("RCON_PORT") or props.get("rcon.port")
    password = password or os.environ.get("RCON_PASSWORD") or state.get("RCON_PASSWORD") or props.get("rcon.password")
    if not port or not password:
        raise SystemExit("rcon: no port/password (start a server with tools/test/server.sh, or set RCON_PORT/RCON_PASSWORD)")
    return int(port), password


class Rcon:
    def __init__(self, port=None, password=None, timeout=600, host="127.0.0.1"):
        port, password = settings(port, password)
        self.s = socket.create_connection((host, port), timeout=timeout)
        self.rid = 0
        self._send(3, password)
        rid, _, _ = self._recv()
        if rid == -1:
            raise SystemExit("rcon: authentication failed")

    def _send(self, kind, body):
        self.rid += 1
        data = struct.pack("<ii", self.rid, kind) + body.encode("utf-8") + b"\x00\x00"
        self.s.sendall(struct.pack("<i", len(data)) + data)

    def _recvn(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.s.recv(n - len(buf))
            if not chunk:
                raise EOFError("rcon connection closed")
            buf += chunk
        return buf

    def _recv(self):
        (length,) = struct.unpack("<i", self._recvn(4))
        data = self._recvn(length)
        rid, kind = struct.unpack("<ii", data[:8])
        return rid, kind, data[8:-2].decode("utf-8", "replace")

    def cmd(self, command):
        """The full response. The server splits a long one into packets of 4096 characters, sent back to back; a full
        packet means more may follow, read until a short one or a short silence. (The vanilla server reads one packet
        per read and drops the rest of the buffer, so a second "end marker" request cannot be used.)"""
        self._send(2, command)
        want = self.rid
        parts = []
        try:
            rid, _, body = self._recv()
        except (EOFError, ConnectionError):  # e.g. after `stop`
            return ""
        if rid == want:
            parts.append(body)
        timeout = self.s.gettimeout()
        try:
            while len(body) >= MAX_FRAGMENT:
                self.s.settimeout(0.5)
                try:
                    rid, _, body = self._recv()
                except (socket.timeout, EOFError, ConnectionError):
                    break
                if rid == want:
                    parts.append(body)
        finally:
            self.s.settimeout(timeout)
        return "".join(parts)

    def close(self):
        self.s.close()


def main(argv):
    port = password = None
    cmds = []
    i = 0
    while i < len(argv):
        if argv[i] == "--port":
            port = argv[i + 1]
            i += 2
        elif argv[i] == "--password":
            password = argv[i + 1]
            i += 2
        else:
            cmds.append(argv[i])
            i += 1
    if not cmds:
        raise SystemExit(__doc__)
    if cmds == ["-"]:
        cmds = [line.strip() for line in sys.stdin if line.strip()]
    r = Rcon(port, password)
    for c in cmds:
        print(r.cmd(c))
    r.close()


if __name__ == "__main__":
    main(sys.argv[1:])
