# Ring 4 — golf 3: I3 (`infra`)

Branch `ring4/infra-g3`, basis `aeebe14`. Pakket I3 uit `golf-3.md`.

## Gedaan
- **r. 356:** `Ring4Tunnels.register(IEventBus)` hangt een `ServerStoppedEvent`-listener op die `forgetWorlds()`
  aanroept (zoals `RingLayout`/`IslandFields`); geregistreerd in `CorruptedAdventure` na `IslandFields.register`.
  `forgetWorlds()` blijft package-private (de listener zit in dezelfde class).
- **r. 430a:** `Ring4IslandStatsTest` heeft `@Tag("stats")`; `build.gradle` sluit tag `stats` uit in `test`, behalve
  met `-PincludeStats`:
  `./gradlew test -PincludeStats --tests '*Ring4IslandStatsTest' --no-daemon`.
- **tools/test op Linux:** `lib.sh` zet `CA_OS` (`windows` bij `uname -s` = MINGW/MSYS/CYGWIN, anders `posix`). Op
  posix:
  - `ca_pid_alive`: `kill -0` of `/proc/<pid>`; `ca_stop_pid`: `kill -9`; `ca_stop_job`: de job plus zijn hele
    procesboom (`pgrep -P`, recursief: gradlew-launcher → Gradle-JVM → server-JVM) met `kill -9`.
  - `ca_java_pids`: `grep -lsaF` op `/proc/*/cmdline` naar `<canonieke checkout>/build/moddev/<kind>RunVmArgs`,
    alleen processen met `comm` = `java`.
  - nieuw `ca_pid_exe` (pad van de server-java; Windows: dezelfde PowerShell+cygpath als voorheen in `server.sh`,
    posix: `readlink /proc/<pid>/exe`); `ca_descendants`.
  - `cygpath` is een shell-functie die het laatste argument teruggeeft (alleen als er geen `cygpath` is).
  - `CA_LOCK_DIR` standaard `${TMPDIR:-/tmp}/ca-test`; `CA_PY` = `python`, of `python3` als `python` ontbreekt;
    `CA_EXE` = `""` (`jfr` i.p.v. `jfr.exe` in `perf.sh`/`jfr-prof.sh`); `CA_GRADLEW` = `bash ./gradlew`.
  - `server.sh`: controleert `-f gradlew` i.p.v. `-x`, start via `CA_GRADLEW`, en voegt `-Xmx…`/`CA_SERVER_JVM` toe
    aan de bestaande `JAVA_TOOL_OPTIONS` (de container zet daar proxy en truststore in) i.p.v. ze te vervangen.
  - `slots.sh alive_pids`: per PID `ca_pid_alive`, daarna `ok`.
  - `client.sh` weigert buiten Windows (exit 1, duidelijke melding); `win.ps1` wordt dus nooit aangeroepen.
  - `selftest-slots.sh`: stap 8 (PowerShell faalt → niets opruimen) alleen op Windows; op posix "skip".
  - README: sectie "Linux (cloud container)".
- Windows: elke Windows-tak is tekstueel de oude code (zelfde PowerShell-commando's, `C:/Temp`, `python`, `jfr.exe`,
  `./gradlew`, `-x`-check, `JAVA_TOOL_OPTIONS` vervangen).

## Tests
Machine: cloud-container, 4 cores, 15 GB, gedeeld met 2 andere stromen (tijden indicatief).

| Commando | Resultaat | Tijd |
|---|---|---|
| `bash ./gradlew build --no-daemon` | BUILD SUCCESSFUL; 1110 tests, 0 fouten (incl. `ClientIsolationTest`, `Ring4TunnelsTest`); `Ring4IslandStatsTest` niet gedraaid | 4m41s (Gradle-cache al warm) |
| `bash ./gradlew test -PincludeStats --tests '*Ring4IslandStatsTest' --no-daemon` | BUILD SUCCESSFUL, stats-test gedraaid | 51s |
| `bash tools/test/selftest-slots.sh` / `--slow` | all checks passed (stap 8 skip op posix) | 2s / ~25s |
| `bash tools/test/setup.sh I 7` | `build/test.conf`, `run/` (poort 25577, RCON 25587) | <1s |
| `bash tools/test/world.sh smoke-a tools/test/areas/smoke.txt` | server up (Done 11,2s), 2 tegels in 11,8s, gestopt; slot vrij, RCON uit, geen java over | 1m08s |
| `bash tools/test/det.sh --areas tools/test/areas/smoke.txt --name det-smoke` | `ember` 9/9, `n3e` (Nether) 4/4 identiek; own 0, other 0, noise 0; exit 0 | 1m54s (2 werelden) |
| `bash tools/test/server.sh start failtest --fresh --timeout 25` (faalpad) | time-out → server-JVM en Gradle-boom gestopt, slot vrij, RCON uit, exit 1 | ~30s |
| `bash tools/test/client.sh start` | weigert: "client.sh needs Windows …", exit 1 | — |

Server-log: 0 ERROR behalve `YggdrasilServicesKeyInfo` (geen internet naar Mojang; onschuldig, offline-mode).

## CODEBASE-delta
- `worldgen/densityfunction/Ring4Tunnels`: `register(IEventBus)`; wist `WORLDS` bij `ServerStoppedEvent`
  (geregistreerd in `CorruptedAdventure`).
- `build.gradle`: `test` sluit JUnit-tag `stats` uit; `-PincludeStats` neemt hem mee. `Ring4IslandStatsTest` is
  `@Tag("stats")` (alleen uitvoer).
- `tools/test/`: werkt op Windows (Git Bash) en Linux (`CA_OS` in `lib.sh`); op Linux geen client/screenshots;
  lock-map standaard `${TMPDIR:-/tmp}/ca-test`; nieuwe helpers `ca_pid_exe`, `ca_descendants` in `lib.sh`.

## Beslissingen
- Eigen listener in `Ring4Tunnels` (zoals `RingLayout`) i.p.v. een aanroep vanuit `IslandFields`: `forgetWorlds()`
  hoeft niet public.
- Stats via `-PincludeStats` op de bestaande `test`-taak, geen losse `statsTest`-taak: ModDevGradle's `unitTest`
  configureert alleen `test` (FML-omgeving); een tweede `Test`-taak zou die setup moeten nadoen.
- OS-detectie via `uname -s`; alles wat niet Windows is heet `posix` (macOS zou via `pgrep`/`kill` ook werken, maar
  `/proc` ontbreekt daar: niet getest, niet beloofd).
- Posix: `kill -9` voor `ca_stop_pid` en de procesboom (gelijk aan `Stop-Process -Force` / `taskkill /T /F`).
- Posix: `JAVA_TOOL_OPTIONS` aanvullen i.p.v. vervangen (anders geen proxy/truststore voor Gradle-downloads in de
  container); Windows ongewijzigd (vervangen).
- Posix: `bash ./gradlew` (het bestand is in git niet uitvoerbaar).
- Selftest-stap 8 (onbekende levendheid) is op posix niet te simuleren (`kill -0` faalt niet): overgeslagen.

## Backlog
- [infra] `server.sh` toont bij een mislukte start de eerste regels met "Exception"; in de cloud is dat altijd de
  onschuldige `Yggdrasil`-`UnknownHostException`, niet de echte oorzaak (bijv. een time-out). Bij een time-out
  "time-out" melden en de Yggdrasil-regels overslaan.
- [infra] `det.sh` heeft geen `--only`: een det-check op één gebied van een groot gebiedenbestand vraagt nu een eigen
  areas-bestand.
