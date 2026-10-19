# tools/test — shared test tooling

One set of scripts for every stream and the integration (golf 3, T0). Everything is relative to the checkout the
scripts live in, so they work from the main checkout and from every worktree. Output (logs, state, scans) goes to
`build/test/` of that checkout (on P:, never C:). Git Bash for the `.sh` scripts, Python 3 with numpy (and Pillow for
images) for `py/` — run Python as `python` (not `python -I`: numpy lives in the user site-packages).

## Once per checkout

```bash
bash tools/test/setup.sh <stream> <n>     # e.g. setup.sh I 7: build/test.conf + run/ (eula, server.properties, options.txt)
```

Port n (waves.md §4): server 25570+n, RCON 25580+n (RCON on 127.0.0.1 and only while a server runs). PIDs go to
`build/<stream>.pids`. Environment overrides: `CA_STREAM`, `CA_PORT_N`, `CA_LOCK_DIR` (default `C:/Temp`).

## The machine and the user

- `C:/Temp/ca-user-test.lock` exists = the user is playing: every script that would start a JVM exits with code 3.
  Start nothing, tell main, wait idle. Only main places/removes that lock.
- Max. 3 JVMs: every build/runData/server/client holds a slot; machine-wide things (the quick-play start of a client,
  JFR) also hold the lock, taken first. Nothing waits while holding something.
- Gradle always with `--no-daemon` (the tools do this themselves): a daemon would stay behind outside the slot count.
  Never `./gradlew --stop` (it also stops the user's daemons).

```bash
bash tools/test/slots.sh run -- ./gradlew build --rerun-tasks --no-daemon   # slot for the duration, always released
bash tools/test/slots.sh run -- ./gradlew runData --no-daemon
bash tools/test/slots.sh run -n 1 --lock --task ffmpeg -- ffmpeg ...           # lock + slot
bash tools/test/slots.sh status | clean | release
```

Owner file: `stream=<s> pid=<windows pid of the holder> time=<iso> task=<label> token=<id> root=<checkout>`. `run`
holds with its own PID and a unique token per run (parallel runs in one checkout never free each other's slots).
A bare `take` registers its caller as holder (`--pid` for another one; server/client hand over to their JVM with
`adopt`), so use it only from a script that lives as long as the hold. `take` waits only with `--wait <s>` (retry
every 20 s, holding nothing). `clean` removes entries older than 60 min whose holder PID is dead (also before every
`take`). Test the protocol with `CA_LOCK_DIR=<scratch dir>`.

## Server, RCON, client

```bash
bash tools/test/server.sh start <level> [--fresh] [--perf] [--worktree DIR]   # slot, runServer, waits for "Done"
CA_SERVER_JVM="<opts>" bash tools/test/server.sh start ...                     # extra JVM options for the server (jfr-prof.sh)
python tools/test/py/rcon.py "list" "/ca tpring 3"                          # RCON of the running server
bash tools/test/server.sh stop                                               # RCON stop, PID gone, RCON off, slot free
bash tools/test/client.sh start [1|2]          # lock + slot, quick play (127.0.0.1), waits until joined, then frees the lock
bash tools/test/client.sh shot build/test/shots/x.png [1|2]
bash tools/test/client.sh key F1 [1|2]         # F1 F2 F3 ESC T ENTER
bash tools/test/client.sh stop [1|2]
```

- Seed 20260930, `level-type=corruptedadventure:corrupted_world`, `online-mode=false`, `max-tick-time=-1`.
- On a failed start the tools stop their own background Gradle job (and the JVM if it was found), free the slot
  and turn RCON off.
- The server/client PID is found by its command line (`<checkout>\build\moddev\<kind>RunVmArgs`, exact checkout) and
  checked again before every stop or screenshot. Screenshots (`win.ps1`, PrintWindow) only ever touch the windows of
  that one PID — never a window found by title (golf 2: a script once photographed the user's client).
- A new `run/options.txt` gets `pauseOnLostFocus:false` (a covered window keeps rendering); an existing one only
  `onboardAccessibility:false` and `skipMultiplayerWarning:true`.
- Shots: generate first (wait until the chunks are loaded), then photograph; no `save-all flush` while shooting.
  `python tools/test/py/sheet.py build/test/shots build/test/sheet.png` makes a contact sheet.

## Worlds and offline scans

Area files (`areas/*.txt`): `name cx0 cz0 cx1 cz1 [ow|nether]` in chunks, inclusive.
`int.txt` (integration: ring 3 biomes/landmarks + Nether 1–3), `fi.txt` (feature index ring 1/2), `perf.txt`,
`nether-walls.txt` (Nether boundaries r 875/1500), `smoke.txt` (a few chunks); ring 4: `ring4.txt` (waterscan), `int2.txt`
(golf 2: `verdant_falls`, tunnel net per band, sky lakes, world trees, arch, lagoon, Nether-4), `fi3.txt` (feature index
rings 1–3), `perf-vf.txt`, `perf-r4.txt` (every ring 4 biome, tunnels, a world tree, `sundered_reach`, Nether-4), `trees.txt` (tree
determinism), `decor.txt` (one `mist_fjords` and one `cascade_vale` area: before/after shots, quick det check).
`python tools/test/py/areas.py around <name> <x> <z> <r> [nether]` prints a line around a block position.

```bash
bash tools/test/world.sh <level> tools/test/areas/int.txt [--reverse] [--settle S] [--freeze [--keep --stay-frozen]] [--worktree DIR]   # fresh world, load, stop
W=run/<level>
python tools/test/py/lavascan.py $W tools/test/areas/int.txt [--json out.json] [--strict]
python tools/test/py/inventory.py $W tools/test/areas/int.txt build/test/inv.json --grep ice
python tools/test/py/section.py cut $W x <z> <x0> <x1> <y0> <y1> out.png  |  section.py probe $W <x> <y> <z>
bash tools/test/det.sh [--areas FILE] [--name det]            # determinism, -> build/test/det.txt
python tools/test/py/featidx.py $W <ref-world> tools/test/areas/fi.txt
```

### Lavascan (trede / val / lek)

Lava is *exposed* when a horizontal neighbour or the block below is air; air above is an open surface and fine. Each
exposed block is classified (heuristic; the original scripts of terrain-g2 were lost, this is a rebuild from the code):

| Class | Rule | Source in the code |
|---|---|---|
| `val` | the 6-connected group of exposed lava holds a vertical run of ≥ `--fall-min` (4) falling lava blocks (level ≥ 8), or is an untouched 47c source (one lava source, solid all round except one air side, air dropping ≥ 4 onto lava). Sub-label `eiland` when the block the fall leaves from has open air below it within 48 blocks, else `wand`. | 39f island falls (`RingAquifer`: falling lava in `[fallBottom, fallTop]`), 47c `LavaFallFeature` (source in a wall above terrain lava, scheduled tick) |
| `trede` | no air below, and every air side has lava directly under it (the neighbour column's lava is one block lower; after a fluid tick the overflow on it counts too) | 42b river steps, `RingAquifer.exposed` / `becomesLava` (V1: the lower neighbour surely fills with lava) |
| `lek` | everything else — lava that flows at the first block update. Listed per group with position, Y range and size. | — |

Neighbours outside the area or in unsaved chunks count as not air. The sub-label of a fall is approximate; the check
is `lek` = 0 (or explained). `--strict` exits 1 on any leak.

`gen.py` (used by `world.sh`) forceloads tiles of 15 x 15 chunks block by block (3 x 3 chunks, the next block only
when the previous one is loaded, time-out 600 s per block); `--reverse` reverses areas, tiles, blocks and the corner
order. `--settle S` keeps each tile forceloaded S more seconds (fluids tick); use the same value for compared worlds.

### Waterscan (ring 4)

Water against air outside the planned falls (contract §4). `waterscan.sh` makes a fresh world of the areas (`--keep`),
asks the server for the planned falls per overworld area box (`/ca falls x0 z0 x1 z1`: `IslandField.waterfallsWithin`,
the same call as the terrain; `<world>/ca_falls/<box>.tsv`), stops the server, and runs `py/waterscan.py` and
`py/poisonscan.py`. Every tile stays loaded 10 s after loading (`--settle 10`, default), so flowing water comes to rest
before the chunks unload and the counts repeat. `--freeze` generates under `/tick freeze` instead (`world.sh --freeze
--keep --stay-frozen`: exactly what the generator placed, nothing flows) - only for small areas: a frozen server never
purges its stale chunk tickets, so nothing unloads (INT2: out of memory at ~19.000 loaded chunks with 5 GB).

```bash
bash tools/test/waterscan.sh <level> tools/test/areas/ring4.txt [--settle S | --freeze]   # -> build/test/waterscan-<level>.txt, poisonscan-<level>.txt (+ .json)
python tools/test/py/waterscan.py <world> <areas> <world>/ca_falls [--strict]   # offline, when the falls are there
python tools/test/py/poisonscan.py <world> <areas> [--strict]                   # offline
```

Exposed water (horizontal or below neighbour is air; air above is a surface) is an overtreding unless exempt: the sea
plane Y 63 and the abyss sea Y 0; the falling column of a fall (Y bottom..top) and the four neighbours of its top (the
spring); the plunge pool of a cliff fall that lands on Y 63 (horizontal radius 6, Y 56..63). Missing fall lists are an
error. Areas: `areas/ring4.txt` (interior 4 at r 7000, boundaries 3|4 at r 6000 and 4|5 at r 8000), `areas/int2.txt`.

**Poison scan (INT2):** toxic sludge with air, cave air, swamp gas or a non-solid block a fluid washes away (plants,
carpets; a block with a `waterlogged` property is closed, only water enters it) beside or below it, or with water (also waterlogged blocks, seagrass, kelp, bubble columns) on any side, is an overtreding; so is swamp gas
with water on any side. Gas against air is only counted (`gas~a`: gas halls open into dry tunnels, sinkhole layers lie
under open air). Run it on a `--freeze` world to see what the generator placed: in a settled world a sludge-water
contact may already have turned into mud.

### Determinism and feature index

- `det.sh` makes `run/<name>-a` (areas forward) and `run/<name>-b` (everything reversed, the Nether first) and runs
  `detcmp.py`: per area identical chunks, `own` differences (a mod block on either side), `other` (terrain, fluids,
  air, vanilla features: explain them) and `noise` (only vanilla ore/stone blobs against their host rock). Exit 1 on
  an `own` difference or a missing chunk, 2 when only `other` differs, 0 clean. `--all` lists every kind.
- Feature index: make a world with the same areas from the reference checkout (`world.sh ref-fi tools/test/areas/fi.txt
  --worktree <main checkout or worktree on main>`; that checkout's `run/`), then `featidx.py` compares biomes (must be
  identical) and terrain shape (top block that is not air/fluid/decoration).

**Same terrain before and after a change (vf-perf2):** make a world of the areas with `world.sh --freeze` on the old
code and one on the new code (`/tick freeze`: no fluid or block ticks after generation), then
`python tools/test/py/detcmp.py run/<old> run/<new> <areas>`. Chunks at the edge of an area (and sometimes one further
in) can still differ in features placed across chunk borders (ores, dripstone, huge fungi, mineshafts): the load order.
Two worlds of the *same* code show the same kind; compare against such a pair before blaming the change.

## Performance

```bash
bash tools/test/perf.sh                      # 3 warm + 1 cold, hard stop 45 min -> build/test/perf-<stamp>/table.md
bash tools/test/perf.sh --warm 1 --cold 0 --only "ring1" --max-min 5      # dry run
bash tools/test/perf.sh --ref build/test/perf-<earlier>                    # adds the noise of an earlier run
```

JFR `minecraft.ChunkGeneration` (via RCON `jfr start/stop`, `jfr.exe` next to the server's java), the mod's
dimension against the vanilla datapack dimension (`perfpack/`: `perftest:vanilla`, `perftest:vanilla_nether`) at the
same chunks in one server, order alternating per area and round, a fresh world per round, warm-up over every ring
type (middle of rings 1-3 and the boundaries, from the ring radii in `run/config`; not in the cold round). Holds the
lock + all 3 slots for the whole run; the hard-stop clock starts once they are held. Table: per area the median of
mod/vanilla over the warm rounds for noise (with min–max), surface and features, plus the cold round. Budget noise
≤ 1,75. Only when the user is not playing; the JFR files and round worlds are deleted after each round.

### Hot methods (JFR profile)

```bash
bash tools/test/jfr-prof.sh --areas tools/test/areas/perf-vf.txt --only "steamfens vgorge"   # -> build/test/prof-<stamp>/
python tools/test/py/jfrhot.py <jfr.exe> build/test/prof-<stamp>/steamfens.jfr --callees IslandField.lowestAround
```

Same machine rules as `perf.sh` (lock + all 3 slots, fresh world, warm-up). Per area one recording with the server's
own profiler (RCON `jfr start` / `jfr stop`: Minecraft's settings sample Java stacks every 10 ms), so nothing attaches
to the JVM; the server starts with `CA_SERVER_JVM=-XX:FlightRecorderOptions:stackdepth=1024` (density functions nest
deeper than JFR's default 64 frames) and `-XX:+DebugNonSafepoints` (else self time lands on the nearest safepoint's
frame, often another inlined method). `jfrhot.py` streams `jfr print` as text (the JSON form of one area's samples is
gigabytes), puts every sample under its chunk step and prints the top self frames and the inclusive share of every mod
method, for all steps and for `--step` (default `noise`); `--callees` splits a method's samples over its callees.
Shares are of the samples, not of the time `perf.sh` compares: a fresh world also pays the per-world decisions.

## Files

| File | What |
|---|---|
| `lib.sh` | shared settings (checkout, stream, ports, lock dir) and helpers (PIDs, properties, run/ preparation) |
| `setup.sh` | one-time setup of a checkout |
| `slots.sh` | lock + JVM slots |
| `server.sh`, `client.sh`, `win.ps1` | server, client, window helper (own PID only) |
| `world.sh`, `det.sh`, `perf.sh`, `waterscan.sh`, `jfr-prof.sh` | fresh world + areas, determinism, performance, ring 4 waterscan, hot methods |
| `perfpack/` | datapack with the vanilla dimensions for perf |
| `areas/` | area files |
| `py/rcon.py` | RCON client (script + module) |
| `py/region.py`, `py/areas.py` | Anvil reader, area files and dimensions |
| `py/gen.py` | load areas via RCON forceload (forward / reversed) |
| `py/lavascan.py`, `py/waterscan.py`, `py/poisonscan.py`, `py/detcmp.py`, `py/featidx.py`, `py/inventory.py`, `py/section.py` | offline scans |
| `py/perf.py`, `py/perfsum.py` | one perf round, the table |
| `py/jfrprof.py`, `py/jfrhot.py` | one profile per area, hot methods of a recording |
| `py/sheet.py` | contact sheet of screenshots |

`tools/nether-lava-walls.py` (RCON, change 39a) still works; offline `lavascan.py` on `areas/nether-walls.txt` covers
the same boundaries.
