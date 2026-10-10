# Ring 3 — golf 3, stroom I: T0 testtooling (`tools/test/`)

Scope: `infra`. Opdracht: `golf-3.md` §T0. Poort n = 7 (server 25577, RCON 25587).

## Wat
De losse testscripts van integratie 2 (`.claude/worktrees/int2/build/int2/`), `build/c39d/py` en de verloren lavascan van
`terrain-g2` samenvoegen tot één set in git onder `tools/test/`, met paden relatief aan de repo-root (werkt vanuit elke
worktree), parameters in plaats van hardcoded worktree-paden en één README.

1. `slots.sh` — lock + JVM-slots in één script (lock alleen met `--lock`; nooit wachten met iets vast; `run` geeft vrij via `trap`; `owner`-bestand; stale > 60 min + dode PID opruimen; weigert bij het speel-lock).
2. `server.sh` — `run/` klaarzetten, `runServer` starten, PID registreren, wachten op "Done", stoppen via RCON + PID-controle, RCON daarna uit.
3. `py/rcon.py` — RCON-client (script + module), poort/wachtwoord uit `run/server.properties`.
4. `client.sh` + `win.ps1` — `runClient` met quick-play naar de eigen server; screenshots/toetsen alleen via de eigen client-PID.
5. Offline scans: `py/lavascan.py` (trede/val/lek), `det.sh` + `py/detcmp.py` (determinisme), `py/featidx.py` (feature-index ring 1/2), plus `world.sh` (verse wereld + gebieden laden, vooruit of omgekeerd).
6. `perf.sh` + `py/perf.py` + `py/perfsum.py` — JFR, mod vs vanilla-datapackdimensie, 3 warm + 1 koud, harde stop, tabel.
7. `tools/test/README.md`.

## Lavascan-heuristiek (trede / val / lek)
De scripts van `terrain-g2/build/afr/py/` zijn verloren; de indeling is opnieuw gemaakt uit de code:
- **val**: groep (6-buren) blootliggende lava met een verticale reeks ≥ 4 vallende lavablokken (`level` ≥ 8; 39f-eilandvallen
  uit `RingAquifer`, 47c-wandvallen na hun eerste tick), of een onaangeraakte 47c-bron (één bronblok, rondom vast op één
  luchtkant na, daaronder ≥ 4 lucht tot lava; `LavaFallFeature`). Sublabel `eiland`/`wand` naar open lucht onder het blok
  waar de val vertrekt (≤ 48 blokken); bij benadering.
- **trede**: geen lucht eronder en onder elke luchtkant ligt lava (buurkolom één lager; 42b `RingAquifer.exposed`/`becomesLava`, V1).
- **lek**: de rest, per groep met positie, Y-bereik en grootte.
- Ijking op de wereld `afr-det-a` (INT-afronding): A = 1 eilandval (zwevende vulkaan, 82 blokken), B = 75 treden op Y 95–98,
  archipel = 4 eilandvallen + 3 treden, pit = 18 lek (Y 5–9, aquiferlava in een grot), ember/n3rifts 0.

## Voortgang
- Voortgang: 2026-10-10 11:44 worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a26bce4478d9e5084`, branch `ring3/infra-g3` (vanaf `e2a0366`); regels, golf-3 §T0, waves §4/§6 en de bronscripts gelezen.
- Voortgang: 2026-10-10 11:50 B (`./gradlew build --rerun-tasks`) groen via `slots.sh run` (eerste build in de worktree, 1 min 43 s).
- Voortgang: 2026-10-10 11:57 `slots.sh` rooktest in een testmap (`CA_LOCK_DIR`): alles-of-niets, lock eerst, busy zonder iets vast, `run` geeft vrij via `trap` (ook bij exit 7), stale (> 60 min + dode PID) opgeruimd, verse entry met dode PID blijft, nep-speellock → exit 3 voor slots/server/client/perf.
- Voortgang: 2026-10-10 12:09 server gestart (`smoke`, 9 s tot Done); PID-herkenning faalde eerst (args-bestand met dubbele backslashes), opgelost; RCON `list`, `gen.py` op `areas/smoke.txt` (13 chunks, 12 s), lavascan/inventory/detcmp/featidx op de kleine wereld OK.
- Voortgang: 2026-10-10 12:19 `det.sh --areas areas/smoke.txt`: twee keer server start → gen (vooruit / omgekeerd) → stop met de definitieve startcode (Done na 11–12 s); 13/13 chunks identiek, own 0.
- Voortgang: 2026-10-10 12:30–13:00 onderbroken (plan mode; T hield het lock). Daarna verder op aanwijzing van main.
- Voortgang: 2026-10-10 13:11 eerste clienttest faalde: quick play naar `localhost` gaat naar ::1, de server luistert op 127.0.0.1 → `client.sh` gebruikt 127.0.0.1; het foutpad stopte de eigen client en gaf lock + slot vrij.
- Voortgang: 2026-10-10 13:16 perf-droogrun (`--warm 1 --cold 1 --only ring1 --max-min 6`): 5 min, tabel ring1 noise 0,83 (koud 0,98), surface 1,43, features 0,94; lock + 3 slots vrijgegeven; JFR-rapporten worden nu ook opgeruimd.
- Voortgang: 2026-10-10 13:18 client joint (Dev, eigen PID), `client.sh list/shot/key F1/shot/stop` OK (venster van de eigen PID, HUD weg op shot 2); harde stop (`--max-min 1`): ronde 1 "deadline reached, 0 of 1 areas", `aborted`, ronde 2 niet gestart, tabel met "Onvolledig (harde stop)".
- Voortgang: 2026-10-10 13:23 B (`./gradlew build --rerun-tasks`) groen.

## W-checklist (INT3)
- Na de merge naar main: `bash tools/test/setup.sh <stroom> <n>` per worktree; INT gebruikt `world.sh`/`det.sh`/`lavascan.py`/`featidx.py` en (na de go) `perf.sh`.
- Lavascan op `areas/int.txt`: `lek` per gebied nalopen (de indeling is een heuristiek, zie hierboven).

## CODEBASE-delta
Tabel tools (bij `tools/server-smoke-test.sh`), nieuwe regels:
| `tools/test/` | (T0, golf 3) gedeelde testtooling, werkt vanuit elke checkout (paden relatief aan de repo-root, uitvoer in `build/test/`); handleiding `tools/test/README.md`. `setup.sh <stroom> <n>` (eenmalig: `build/test.conf`, `run/` met preset, seed 20260930, poorten 25570+n/25580+n), `slots.sh` (lock `C:/Temp/ca-test.lock` + JVM-slots `ca-jvm-slot-{1,2,3}`: alles-of-niets, lock eerst, nooit wachten met iets vast, `run -- <cmd>` geeft altijd vrij, `owner` met stroom/Windows-PID/tijd/taak/checkout, stale > 60 min + dode PID; weigert met exit 3 bij `C:/Temp/ca-user-test.lock`; `CA_LOCK_DIR` voor tests), `server.sh start|stop|status` (runServer op 127.0.0.1, RCON alleen tijdens de run met een nieuw wachtwoord, PID via de commandline `<checkout>\build\moddev\serverRunVmArgs` naar `build/<stroom>.pids`, stop via RCON + PID-controle; `--fresh`, `--perf`, `--worktree`), `client.sh start|shot|key|stop` (quick play naar de eigen server, lock + slot, screenshots/toetsen via `win.ps1` alleen op de eigen client-PID), `world.sh` (verse wereld + gebieden vooruit/omgekeerd), `det.sh` (twee werelden, tweede omgekeerd, `py/detcmp.py`), `perf.sh` (JFR `minecraft.ChunkGeneration`, mod tegen `perfpack/`-vanilladimensies in één server, 3 warm + 1 koud, harde stop 45 min, tabel `py/perfsum.py`); `py/`: `rcon.py` (script + module), `region.py`, `areas.py`, `gen.py`, `lavascan.py` (trede/val/lek), `detcmp.py` (own/other/noise), `featidx.py` (biomes + terreinvorm), `inventory.py`, `section.py` (doorsnede/probe), `perf.py`, `perfsum.py`, `sheet.py`; `areas/` (`int`, `fi`, `perf`, `nether-walls`, `smoke`) |
Valkuilen (nieuw):
- **JVM-PID's op Windows:** het args-bestand op de commandline van een MDG-run staat met dubbele backslashes (`"@P:\\…\\build\\moddev\\serverRunVmArgs.txt"`); normaliseer vóór het vergelijken. Git Bash-PID's (`$$`) zijn niet zichtbaar voor Windows-tools: `/proc/$$/winpid`.
- **Achtergrondjobs in Git Bash:** een gestarte native JVM houdt de stdout van de aanroeper vast (een pipe blijft open tot de JVM stopt) tenzij de job eerst al zijn descriptors omleidt (`(exec >log 2>&1 </dev/null; …) &`).
- **numpy en `python -I`:** numpy staat in de user-site-packages; `-I` slaat die over. De tools draaien met `python`.

## Beslissingen
- 2026-10-10 · infra · Testtooling in git onder `tools/test/` (T0): één set voor alle stromen en INT; per checkout `setup.sh <stroom> <n>`; de lavascan-indeling trede/val/lek opnieuw opgebouwd uit `RingAquifer`/`LavaFallFeature` (de terrain-g2-scripts waren verloren), als heuristiek gedocumenteerd.
- 2026-10-10 · infra · RCON en de testserver binden aan 127.0.0.1 (`server-ip`), met een nieuw RCON-wachtwoord per start; RCON gaat na `server.sh stop` uit.

## Backlog
- [infra] `tools/nether-lava-walls.py` (RCON, 39a) is overbodig naast `lavascan.py` op `tools/test/areas/nether-walls.txt`; weghalen zodra de docs die ernaar verwijzen zijn bijgewerkt. (bron: T0)
- [infra] `tools/server-smoke-test.sh` stopt elke `serverRun`-JVM (verboden sinds golf 2); vervangen door `tools/test/server.sh` of weghalen. (bron: T0)
