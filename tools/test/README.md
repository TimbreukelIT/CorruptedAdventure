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
`build/<stream>.pids`. Environment overrides: `CA_STREAM`, `CA_PORT_N`, `CA_LOCK_DIR` (default `C:/Temp`; on Linux
`${TMPDIR:-/tmp}/ca-test`).

## Linux (cloud container)

`lib.sh` sets `CA_OS` from `uname -s`: `windows` under Git Bash/MSYS/Cygwin (everything as described here), `posix`
elsewhere. On posix: processes via `kill -0`/`kill`/`pgrep -P` and `/proc` (a JVM is found by
`/proc/<pid>/cmdline` containing `<canonical checkout>/build/moddev/<kind>RunVmArgs` and `comm` = `java`; a failed
start kills the Gradle job's process tree), `cygpath` is a pass-through, PIDs are the shell's own, Gradle runs as
`bash ./gradlew` (the file may lack its executable bit), `jfr` instead of `jfr.exe`, Python is `python` or else
`python3`, and the server keeps the container's `JAVA_TOOL_OPTIONS` (proxy, trust store) and adds `-Xmx` to it.
`client.sh` (and so screenshots, `win.ps1`) refuses with exit 1: there is no screen. Server, RCON, `world.sh`, `det.sh`,
`waterscan.sh` and the offline scans work; `perf.sh`/`jfr-prof.sh` run, but cloud timings are not comparable.

```bash
bash tools/test/setup.sh I 7                                   # once
bash tools/test/det.sh --areas tools/test/areas/smoke.txt --name det-smoke   # -> build/test/det-smoke.txt
```

## The machine and the user

- `C:/Temp/ca-user-test.lock` exists = the user is playing: every script that would start a JVM exits with code 3.
  Start nothing, tell main, wait idle. Only main places/removes that lock.
- Max. `CA_SLOTS` (5, since 2026-10-25; was 3) JVMs: every build/runData/server/client holds a slot; perf.sh and jfr-prof.sh take all of them; machine-wide things (the quick-play start of a client,
  JFR) also hold the lock, taken first. Nothing waits for a slot while holding something (a client with a reserved
  slot waits only for the lock, and nobody waits while holding the lock).
- Low-priority mode while the user plays something else: `touch C:/Temp/ca-low-priority` (or `CA_LOW=1`). Every
  script then runs below normal priority with its children, and servers, clients and Gradle builds started through
  the scripts see `CA_LOW_CPUS` (8) processors. Remove the file to measure on a quiet machine again.
- Gradle always with `--no-daemon` (the tools do this themselves): a daemon would stay behind outside the slot count.
  Never `./gradlew --stop` (it also stops the user's daemons).

```bash
bash tools/test/slots.sh run -- ./gradlew build --rerun-tasks --no-daemon   # slot for the duration, always released
bash tools/test/slots.sh run -- ./gradlew runData --no-daemon
bash tools/test/slots.sh run -n 1 --lock --task ffmpeg -- ffmpeg ...           # lock + slot
bash tools/test/slots.sh status | clean | release | held [--task T]
bash tools/test/selftest-slots.sh [--slow]                                     # self-test in a scratch lock dir, no JVM
```

Owner file: `stream=<s> pid=<windows pid of the holder> time=<iso> task=<label> token=<id> root=<checkout>`. `run`
holds with its own PID and a unique token per run (parallel runs in one checkout never free each other's slots).
A bare `take` registers its caller as holder (`--pid` for another one; server/client hand over to their JVM with
`adopt`), so use it only from a script that lives as long as the hold. `take` waits only with `--wait <s>` (retry
every 20 s, holding nothing). `take --tasks server,client1` takes one slot per label at once (all or nothing, each
with its own task); `take -n 0 --lock` takes the lock only. `clean` removes entries whose holder PID is dead and whose
owner file is older than `CA_STALE_MIN` (20) min - longer than a server (900 s) or client (600 s) start, during which
the starting script is the holder (also before every `take`; until 2026-10-20 it was 60 min); when the PID lookup
itself fails nothing is removed. `status` marks dead holders. Test the protocol with
`CA_LOCK_DIR=<scratch dir>` (`selftest-slots.sh` does).

**Server + client: reserve both at once.** A server holds its slot while it runs; a client started later needs a
second one. On 2026-10-19 three streams each held a server slot and waited for a client slot that never came. So a
test that will need a client starts the server with `--client` (`--client 2`, `--client both`): `server.sh` takes the
server's and the client's slot in one `take --tasks server,client1` (or waits holding nothing), the server JVM holds
the client's reservation, `client.sh start` then only takes the lock and the reserved slot, `client.sh stop` hands the
slot back to the running server (a next client start needs no slot either) and `server.sh stop` frees what the server
still holds. Adopt and release go by holder PID, so a restarted server's reservation and an old client's slot with the
same label never get mixed up. Without `--client` a client start works as before, except when this stream's server
holds a slot: then it tries once (exit 2 when busy, with a hint) instead of waiting 1800 s with the server slot held;
an explicit `--wait SEC` still waits.

## Server, RCON, client

```bash
bash tools/test/server.sh start <level> [--fresh] [--perf] [--worktree DIR]   # slot, runServer, waits for "Done"
bash tools/test/server.sh start <level> --client [1|2|both]                   # + reserve the client slot(s) at once
bash tools/test/server.sh start <level> --studio                              # + the photo studio's dimensions (see Studio)
CA_SERVER_JVM="<opts>" bash tools/test/server.sh start ...                     # extra JVM options for the server (jfr-prof.sh)
python tools/test/py/rcon.py "list" "/ca tpring 3"                          # RCON of the running server
bash tools/test/server.sh stop                                               # RCON stop, PID gone, RCON off, slot free
bash tools/test/client.sh start [1|2]          # lock + slot (or the reserved one), quick play (127.0.0.1), waits until joined, then frees the lock
bash tools/test/client.sh shot build/test/shots/x.png [1|2]   # PrintWindow; blank -> F2 fallback + warning; exit 4 if no F2 file
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
- Blank shots: PrintWindow copies what DWM composed for the window, and DWM composes nothing while the display is off
  or the window is minimized. The power plan turns the display off after 15 min without user input; the monitor then
  goes to standby and drops off the bus (event log Kernel-PnP 1010, `DISPLAY\SAM7053` "surprise removed", 2026-10-19
  20:46:08: every shot after that was white). Reproduced 2026-10-20 15:06 with the user idle for 3.6 h; F2 worked.
  `win.ps1` therefore checks every capture (64 x 36 samples within 6 levels per channel, near white or near black; a
  flat fog or sky colour is not blank) and reports `blank #RRGGBB (WxH) minimized=... user-idle=<min>min`;
  `client.sh shot` then presses F2 (Minecraft's own screenshot of its framebuffer, `run/screenshots` or
  `run/client2/screenshots`), waits until the new file's size is stable (max. 20 s) and copies it to the output. A flat
  F2 image is what the game shows (a black cave with F1): kept, with a warning. F2 puts a "Saved screenshot" line in
  the chat for the next shots: hide the HUD with F1 for photo series. `win.ps1 blank 0 <png>` checks an existing image.
- Shots: generate first (wait until the chunks are loaded), then photograph; no `save-all flush` while shooting.
  `python tools/test/py/sheet.py build/test/shots build/test/sheet.png` makes a contact sheet.
- Photo walks (ring 5 walk 1): `python tools/test/py/walk.py <poses.txt> <out dir> [--only label ...]` - spectator, ring
  weather cleared, HUD off, daylight cycle stopped, time per pose; each shot waits until the picture is stable.
  `--light 16` lights caves and undersides with light blocks that stay: only on a copy of a world.
- Ring tour and portal test (change 100): `python tools/test/py/ringspots.py <poses> r1:ring1 nb12:boundary1:nether ...`
  (then `walk.py` on the poses) and `python tools/test/py/portal.py --rings 1 4 5 [--json out]` (client 1 online; leaves
  the frames standing, exit 1 when a ring fails).

## Studio (structure photos, S0)

`tools/structures/studio.sh` photographs one template or structure from every side into one contact sheet
(`docs/plans/structures/s0-studio.md`). It uses this stream's server and client 1 (`--client 2` for Dev2):

```bash
bash tools/structures/studio.sh prepare                  # once, before client.sh start: FOV 70, no clouds, render distance 16
bash tools/test/server.sh start studio --studio --client # --studio installs tools/structures/studio-datapack (void, day + night)
bash tools/test/client.sh start
bash tools/structures/studio.sh template minecraft:village/plains/houses/plains_small_house_1   # -> build/studio/<name>/sheet.png
bash tools/structures/studio.sh structure minecraft:pillager_outpost [--seed S]
bash tools/structures/studio.sh template <id> --here X surface Z [--dim minecraft:overworld]     # in situ (Y or surface)
bash tools/structures/studio.sh shots [--floors 64,69]   # the current placement again; sheet <dir> rebuilds a sheet
```

The driver puts the player in spectator, runs `weather clear` (the studio shares the overworld's rain), hides the HUD
with F1 (and shows it again at the end), teleports to every pose of `/ca studio shots` and shoots until two shots in a
row match (chunks rendered). In situ it also sets the time (6000 / 18000, the old time back after), clears the ring
weather of rings 2–5 and restores the terrain at the end (`--keep` leaves the structure; the snapshot lives only in the
running server, so clear before a restart). A studio placement first empties the studio area: leftovers of a forgotten
session never show. The studio dimensions only exist in a world started with `--studio`; the datapack never goes
into the jar.

## Worlds and offline scans

Area files (`areas/*.txt`): `name cx0 cz0 cx1 cz1 [ow|nether]` in chunks, inclusive.
`int.txt` (integration: ring 3 biomes/landmarks + Nether 1–3), `fi.txt` (feature index ring 1/2), `perf.txt`,
`nether-walls.txt` (Nether boundaries r 875/1500), `smoke.txt` (a few chunks); ring 4: `ring4.txt` (waterscan), `int2.txt`
(golf 2: `verdant_falls`, tunnel net per band, sky lakes, world trees, arch, lagoon, Nether-4), `fi3.txt` (feature index
rings 1–3), `perf-vf.txt`, `perf-r4.txt` (every ring 4 biome, tunnels, a world tree, `sundered_reach`, Nether-4), `perf-r5.txt` (every ring 5 biome, zone 4→5, castle site #1, the void edge, both Nether-5 biomes; `--cold-only "wastes cathedral"`), `trees.txt` (tree
determinism), `decor.txt` (one `mist_fjords` and one `cascade_vale` area: before/after shots, quick det check), `decor-d.txt` (decor D: a `verdant_falls` gorge with its giant tree, the basins, a `sundered_reach` land bridge with its split rock).
`decor-c.txt` (decor rollout C: sky gardens, two giant moonpetals, a ruin, the underside of a low giant island).
`islands.txt` (islands revision 2026-10-20: `tds`, `skyw1`, `skyw2` as in `int2.txt`, the inner edge and the middle of
ring 4 east; det and waterscan), `perf-islands.txt` (the same places in 16 x 16 chunks for `perf.sh --areas`:
forceload takes at most 256 chunks, a bigger area like `tds` measures nothing).
Ring 5 (golf 0, 81b): `ring5.txt` (zone 4→5 east/north, inner band east/north, across the band line east, outer band
east/north, zone 5→void east, castle site #1; voidscan and det) and `castles.txt` (the four castle sites on seed 20260930
with their belts, from `/ca castles`).
`surface.txt` (opt/surface: every ring 2 and ring 3 biome, the zones 1→2, 2→3 and 3→4, a ring 4 basin, zone 4→5,
ring 5 and Nether rings 1–5, ~2.400 chunks; `world.sh --freeze` on the old and the new code, then `detcmp.py`).
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

### Voidscan (ring 5)

What lies over the void and the dry rule of ring 5 (82, V3-A, V9-B), on a `world.sh` world:

```bash
python tools/test/py/voidscan.py <world> tools/test/areas/ring5.txt [--rings rings.txt] [--wet z45e ...] [--list 5] [--json out.json] [--strict]
```

`--rings` takes the ring of every column (the noisy margin of the zone 4→5 holds ring 4's columns, which keep ring 4's
own sea and count no faults except `fluidvoid`; the `z45*` areas refuse to run without it). Make it with the world's
seed (the export uses the default common config: a world with other ring settings needs its own layout):
`CA_RING_AREAS=<abs path of areas> CA_RING_OUT=<abs path out> CA_WORLD_SEED=<seed> ./gradlew cleanTest test --tests '*RingColumnsExportTest*'`
(paths must be absolute: the test JVM does not run from the checkout root).

Counts per area: `void` (columns without any solid block; information) and the faults `bedrock` (bedrock above Y −60 or
over the void), `loose` (solid groups of 1–2 blocks with nothing solid below them), `fluidvoid` (water or lava beside a
void column or over the void), `water` (any water) and `lavahigh` (lava at or above Y −54). `water` and `lavahigh` only in
dry areas: every area except `--wet` ones (default: names starting with `z45`, the zone 4→5 keeps its abyss sea until
85). Lava leaks (the dam of ring 5's lava layer) come from `lavascan.py` on the same world: `lek` must be 0. Golf 0:
`voidscan.py` on `ring5.txt` gives 0 faults (only the castle belt and the void edge have void).

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

Each round also counts the deoptimizations of the round's recording (`py/deopt.py`, `java/DeoptStorm.java`
with the server's JDK): a **storm** is a site with more than 10.000 of action `none`, where C2 compiled a trap that
never recompiles (2026-10-26: 56 million in `Ring5Field.terrain`, ring 5 692x slower after a long session). The
table lists them; `jfr-prof.sh` prints the same line per area. Full world: `--areas tools/test/areas/perf-all.txt`.
`CA_JVM_EXTRA="<opts>" bash tools/test/jfr-prof.sh ...` adds JVM options (e.g. `-XX:PerMethodRecompilationCutoff=20`:
the poisoning limit drops from 201 to 11 summed decompiles, a stress test for storms).

**JFR from the JVM's start (2026-10-27).** `perf.sh` and `jfr-prof.sh` start the server with `CA_JFR_AT_START`
(`lib.sh`: `-XX:StartFlightRecording:settings=none -Xlog:jfr+startup=off`). That is an empty recording in memory for the
whole session: no events, no file and no startup lines. Gradle's toolchain probe parses the output of every java it
starts and rejects a JDK that prints more. Without it, the first RCON `jfr start` of a JVM deoptimized all compiled code
at once: on 2026-10-27, 14.340 nmethods in one second. That start was perf.py's, after the warm-up.
- Why: Minecraft's `JfrProfiler` registers its event classes and JFR retransforms them. At a JVM's first class
  retransformation, HotSpot invalidates every nmethod.
- Every method with C2 code then had one decompile more. Every large compile root compiled afterwards summed ≥ 201 over
  its inline tree, so it compiled its never-taken branches as traps with action `none`.
- Example: the ring 4 storms of 2026-10-27 in `VerdantGorges.shape`. That root has ~450 inlined parses (3× `rampAt`).
  Under the stress flag its sum was 262. Counted from the make_not_entrant events, ~95 % of it came from that one
  moment. The storm came from the measurement, not from the game.

With the recording from the start, that invalidation falls in the JVM's first second (~1.500 nmethods, only bootstrap
code). `jfr start` then deoptimizes nothing beyond the normal churn: at most ~110 make_not_entrant per second around it,
against 14.340. A perf round with it had 0 deopts of action `none` (405 in all), where rounds had thousands before.
The measurements see the JIT state of a server that is not profiled.

To check it: add `CA_JVM_EXTRA="-XX:+LogCompilation -XX:LogFile=<dir>/hs_%p.log"` and count the `<make_not_entrant`
lines per second of their `stamp`. In the compile log of a poisoned task, the `observe ... mdecompiles=` lines give the
running sum at each trap.

### Hot methods (JFR profile)

```bash
bash tools/test/jfr-prof.sh --areas tools/test/areas/perf-vf.txt --only "steamfens vgorge"   # -> build/test/prof-<stamp>/
python tools/test/py/jfrhot.py <jfr.exe> build/test/prof-<stamp>/steamfens.jfr --callees IslandField.lowestAround
```

Same machine rules as `perf.sh` (lock + all 3 slots, fresh world, warm-up). Per area one recording with the server's own
profiler (RCON `jfr start` / `jfr stop`: Minecraft's settings sample Java stacks every 10 ms), so nothing attaches to
the JVM; the server starts with `CA_JFR_AT_START` (see above), `-XX:FlightRecorderOptions:stackdepth=1024` (density
functions nest deeper than JFR's default 64 frames) and `-XX:+DebugNonSafepoints` (else self time lands on the nearest
safepoint's frame, often another inlined method). `jfrhot.py` streams `jfr print` as text (the JSON form of one area's
samples is gigabytes), puts every sample under its chunk step and prints the top self frames and the inclusive share of
every mod method, for all steps and for `--step` (default `noise`); `--callees` splits a method's samples over its
callees. Shares are of the samples, not of the time `perf.sh` compares: a fresh world also pays the per-world decisions.

## Files

| File | What |
|---|---|
| `lib.sh` | shared settings (checkout, stream, ports, lock dir) and helpers (PIDs, properties, run/ preparation) |
| `setup.sh` | one-time setup of a checkout |
| `slots.sh`, `selftest-slots.sh` | lock + JVM slots, their self-test (scratch lock dir, no JVM) |
| `server.sh`, `client.sh`, `win.ps1` | server, client, window helper (own PID only) |
| `world.sh`, `det.sh`, `perf.sh`, `waterscan.sh`, `jfr-prof.sh` | fresh world + areas, determinism, performance, ring 4 waterscan, hot methods |
| `perfpack/` | datapack with the vanilla dimensions for perf |
| `../structures/studio.sh`, `studio.py`, `studio-datapack/` | photo studio for structures (see Studio) |
| `areas/` | area files |
| `py/rcon.py` | RCON client (script + module) |
| `py/region.py`, `py/areas.py` | Anvil reader, area files and dimensions |
| `py/gen.py` | load areas via RCON forceload (forward / reversed) |
| `py/lavascan.py`, `py/waterscan.py`, `py/poisonscan.py`, `py/detcmp.py`, `py/featidx.py`, `py/inventory.py`, `py/section.py` | offline scans |
| `py/perf.py`, `py/perfsum.py` | one perf round, the table |
| `py/jfrprof.py`, `py/jfrhot.py` | one profile per area, hot methods of a recording |
| `py/sheet.py` | contact sheet of screenshots |
| `py/walk.py` | photo walk: a pose file (`label ow\|nether day\|night x y z yaw pitch`) shot with the running server + client 1 |
| `py/ringspots.py` | (change 100) spots from `/ca tpring` / `/ca tpboundary` with the player (`label:ring<n>\|boundary<n>[:nether][:heading°]`): landing, `/ca ring`, biome, written as `walk.py` poses |
| `py/portal.py` | (change 100) portal test per ring: `/ca tpring <n>`, obsidian frame + fire via RCON, the player (creative) to the Nether and back; `/ca ring` at the arrival must give the same ring, the return must land at its own frame |

`tools/nether-lava-walls.py` (RCON, change 39a) still works; offline `lavascan.py` on `areas/nether-walls.txt` covers
the same boundaries.
