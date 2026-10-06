"""Lava walls at a Nether ring boundary: lava blocks with a horizontal air neighbour (a wall or fall of lava).

RingFluidPicker switches the lava level hard on the midline of a zone; where carvers or terrain open space across it
between two different levels, a wall appears (change 39a, contract ring 3 §4). Run against a running server with RCON
on (server.properties: enable-rcon=true, rcon.password=...) in a throwaway world; it forceloads chunks.

Usage: python -I tools/nether-lava-walls.py <rcon-password> <boundary-radius> [y-min y-max] [port]
  e.g. python -I tools/nether-lava-walls.py test39a 875 9 32   (Nether radius of boundary 2|3; 3|4 is 1500)
Checks 8 directions x 3 chunks across the midline (radius -16, 0, +16). Exit code 1 if any wall is found.
"""
import math
import socket
import struct
import sys


class Rcon:
    def __init__(self, password, port):
        self.s = socket.create_connection(('127.0.0.1', port), timeout=900)
        self.n = 0
        self.cmd(password, kind=3)

    def cmd(self, body, kind=2):
        self.n += 1
        data = struct.pack('<ii', self.n, kind) + body.encode('utf-8') + b'\x00\x00'
        self.s.sendall(struct.pack('<i', len(data)) + data)
        size = struct.unpack('<i', self._read(4))[0]
        return self._read(size)[8:-2].decode('utf-8', 'replace')

    def _read(self, n):
        buf = b''
        while len(buf) < n:
            chunk = self.s.recv(n - len(buf))
            if not chunk:
                raise IOError('RCON connection closed')
            buf += chunk
        return buf


def main():
    password, radius = sys.argv[1], int(sys.argv[2])
    y_min, y_max = (int(sys.argv[3]), int(sys.argv[4])) if len(sys.argv) > 4 else (9, 32)
    port = int(sys.argv[5]) if len(sys.argv) > 5 else 25575
    r = Rcon(password, port)
    nether = 'execute in minecraft:the_nether '
    lava_total = walls = 0
    for i in range(8):
        angle = i * math.pi / 4 + 0.1
        for dr in (-16, 0, 16):
            x0 = (int((radius + dr) * math.cos(angle)) >> 4) << 4
            z0 = (int((radius + dr) * math.sin(angle)) >> 4) << 4
            r.cmd(f'{nether}run forceload add {x0 - 16} {z0 - 16} {x0 + 31} {z0 + 31}')
            lava = [(x, y, z) for y in range(y_min, y_max + 1) for x in range(x0, x0 + 16) for z in range(z0, z0 + 16)
                    if 'passed' in r.cmd(f'{nether}if block {x} {y} {z} minecraft:lava')]
            found = 0
            for x, y, z in lava:
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if 'passed' in r.cmd(f'{nether}if block {x + dx} {y} {z + dz} #minecraft:air'):
                        found += 1
                        print(f'  wall: lava {x} {y} {z}, air at {x + dx} {y} {z + dz}')
                        break
            print(f'chunk {x0},{z0}: lava {len(lava)}, lava beside air {found}')
            lava_total += len(lava)
            walls += found
            r.cmd(f'{nether}run forceload remove {x0 - 16} {z0 - 16} {x0 + 31} {z0 + 31}')
    print(f'boundary r {radius} (Nether), Y {y_min}..{y_max}: lava {lava_total}, walls {walls}')
    sys.exit(1 if walls else 0)


if __name__ == '__main__':
    main()
