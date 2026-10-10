# Ring 3 — golf 3: textures (57), backlog-opruiming en teststop 4c

Scope per pakket: één categorie (CLAUDE.md §2). Start: `main` na `8724301` (2026-10-10). Besluiten: `docs/design/fase-1-wereld.md` ("Sparring golf 3") en `02-beslissingenlog.md` (2026-10-10). Testritme: `waves.md` §4 (golf 3) en §6.

## Stromen en pakketten

| Stroom | Branch | Poort (n) | Pakketten | Categorie | Start |
|---|---|---|---|---|---|
| I | `ring3/infra-g3` | 7 | **T0** testtooling `tools/test/` | `infra` | direct |
| T | `ring3/textures-g3` | 6 | **57** definitieve textures ring 3 + Nether-3 (r1, zo nodig r2), randpunten `glowing_fissure` symmetrisch, Sodium/Iris-check (max. ~30 min) | `textures` | direct |
| B | `ring3/blocks-g3` | 0 | **38c** askorst die alleen instort als er een speler onder staat | `blocks` | na T0 |
| W | `ring3/worldgen-g3` | 1 | **42c** lavalek afgrond dichten · **54c** ijsblokjes Nether-3-scheuren weg · **39g** lavaoevers smaller en minder magma · **47e** archipelvallen: geen aslaag in het stroomgebied, vallen niet in water · **42d** nieuwe askorst op dunne daken + een stuk dun dak vóór elke buismond (na 38c) | `worldgen` | na T0 |

- W bouwt zijn pakketten na elkaar (één eigenaar voor de worldgen-bestanden van terrein, biomes en Nether in deze golf). 42d wacht op 38c: main merget B vooruit naar main zodra 38c gereviewd is.
- Prestatie: 42c, 39g en 42d raken surface/noise → één meting na INT3 (3 warm + 1 koud), als de gebruiker niet speelt.

## T0 — testtooling (`tools/test/`, in git)
Samenvoegen uit de bestaande scripts (niet opnieuw uitvinden): `.claude/worktrees/int2/build/int2/` (`slot.sh`, `acquire.sh`, `release.sh`, `srv.sh`, `srvstop.sh`, `cli.sh`, `win.ps1`, `det*.sh`, `perf*.sh`, `py/`), `build/c39d/py`, `tools/nether-lava-walls.py`. De scripts van `terrain-g2/build/afr/py/` zijn niet bewaard.

1. **Lock en slots** in één script: eerst `C:/Temp/ca-test.lock` (alleen voor machinebrede zaken), dan een slot `C:/Temp/ca-jvm-slot-{1,2,3}`; nooit wachten met het lock vast; altijd vrijgeven (ook bij fout, `trap`); `owner`-bestand (stroom, PID, tijd); oude slots opruimen (> 60 min en PID dood). Respecteert `C:/Temp/ca-user-test.lock` (dan weigert het).
2. **Server**: `run/` klaarzetten (eula, `server.properties` met eigen poort, RCON-poort, `level-name`, seed `20260930`, `level-type`), starten via Gradle `runServer` in de achtergrond, PID registreren (`build/<stroom>.pids`), wachten op "Done" in het log met time-out, stoppen via RCON `stop` + controle dat de PID weg is.
3. **RCON-client** (Python, één bestand, ook als module).
4. **Client**: `runClient` met quick-play naar de eigen server, PID registreren; **screenshots alleen van vensters van de eigen client-PID**; stoppen alleen eigen PID.
5. **Offline regio-scans** (Python op `.mca`):
   - lava tegen lucht, met de indeling **trede / val / lek** (riviertreden, eiland- en 47c-vallen apart; de rest = lek) per meetgebied;
   - determinisme: twee werelden (de tweede omgekeerd geladen) vergelijken op eigen blokken (en desgewenst alles), uitvoer per gebied;
   - feature-index ring 1/2: biomes en terreinvorm vergelijken met een referentiewereld.
6. **Prestatie**: JFR `minecraft.ChunkGeneration`, mod vs vanilla als datapack-dimensie in één server, stappen noise/surface/features, mediaan per gebied, 3 warm + 1 koud standaard, **harde stop** (parameter, standaard 45 min), tabel als uitvoer.
7. `tools/test/README.md`: één pagina met de aanroepen. Alles werkt vanuit elke worktree (paden relatief aan de repo-root).

Test: B (build), en één rooktest van de tools zelf (server start/stop + RCON + lock/slots + één scan op een kleine wereld), zonder de gebruiker te storen (respecteert het speel-lock).

## 57 — textures (T)
- Alle ring 3- en Nether-3-blokken, inclusief de aankleding uit golf 2 en de afronding: `sulfur_crystal` + knoppen, `yellow_/green_/orange_sulfur_mud` (nu placeholders), en de 57a-r2-lijst uit `teststop-4b.md` §I.
- `glowing_fissure`: randpunten symmetrisch (midden van elke rand), zodat de 32 rotaties aansluiten.
- Werkwijze 57a: shots per blok in een echte omgeving (dag, en nacht waar gloed telt), contactbladen; de gebruiker geeft per blok "goed / lichter / rustiger / meer gloed"; max. twee rondes.
- Sodium/Iris: losse test-instance met Sodium + Iris voor 1.21.1 (NeoForge), nachtshot van de gloeilagen. Lukt het niet binnen ~30 min: stoppen en terug naar de backlog.

## Integratie 3 (snel protocol, `waves.md` §6)
Merge I (al eerder vooruit) → B → W → T; B/D + vangnet `src/generated`; S-smoke (server + 1 client, verse wereld); offline lavascan (trede/val/lek), determinisme omgekeerd geladen, feature-index ring 1/2; review; **merge naar main** → de gebruiker speelt vanuit de hoofdwerkmap. Daarna 57 r2 als nodig, dan prestatie (als de gebruiker niet speelt), dan **teststop 4c** (`teststop-4c.md`).

## Teststop 4c — eindcheck ring 3 + Nether-3 (blokkerend)
Checklist uit alle W-lijsten (golf 1, 2, afronding, 3), standaard-W (elke ring en grens, portal per ring, ook overworld ring 3 → Nether-3), prestatie en determinisme. Presenteren en wachten op het oordeel van de gebruiker.

## Backlog-uitkomst (sparring 2026-10-10)
- **Gebouwd in golf 3:** lavalek afgrond, ijsblokjes Nether-3, askorst-blok + plaatsing, lavaoevers, determinisme archipelvallen, placeholders zwavelblokken, randpunten `glowing_fissure`, Sodium/Iris-check, `steam_puff`/`ice_glint`-tint (beoordelen in 57).
- **Naar een infra-ronde na ring 3:** noise-overhead ringraamwerk (`mapAll`), surface ring 3, `VolcanicField`-cachehelper, `Ring3Conditions.register`, `WoodSet`, texturescripts ontdubbelen, `/ca terrainmap`-interpolatie.
- **Fase 2 (items):** placeholder-items ring 3, `ember_berry_bush`-item, `sulfur_crystal`-drop. **Fase 3:** `getBaseHeight` en lavameren. **Fase 4:** stray-spawnregel Nether-3, striders ring 3. **Ring 4:** `ring3Seal` in zone 3→4, ertsen in `steam_fens`.
- **Blijft staan (info of alleen bij klachten):** `minecraft:ice`-smelten (geen `packed_/blue_ice`), eilandvallen tegen daklichten, kegel door de wolkenlaag, eigen ring 3-carvers.
- INT3 verwerkt dit in `docs/backlog.md` (afgehandeld/verplaatst markeren).

## Voortgang
- 2026-10-10 11:50: plan en besluiten vastgelegd; I (T0) en T (57 r1) gestart.
