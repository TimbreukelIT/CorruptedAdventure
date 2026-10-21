# Pakket 81b — golf 0 ring 5 (infra): `/ca castles`, testgebieden, bodemscan

Scope: `infra` — pakket 81b, worktree `/home/user/wt/ring5-g0` op `ring5/golf0` (`golf-0.md` §3, §4, §5).

## Gedaan
1. **`/ca castles`** (`command/CastlesCommand`, OP via `/ca`): de vier kasteelplekken uit `Ring5Fields.current(overworld)`,
   per regel `#n hoek° x z r=straal top=Y belt=breedte d=afstand` (n = index + 1; afstand tot de bron van het commando).
   `/ca castles tp <1–4>`: de speler naar het midden van die pinakel, Y volgens de landingsregel van `/ca tpring`
   (`RingTeleportCommand.findSafeY`, nu package-private; terugval de top). Geen ringwereld → melding.
2. **Testgebieden:** `tools/test/areas/ring5.txt` (zone 4→5 oost/noord, binnenband oost/noord, dwars over de bandlijn
   oost, buitenband oost/noord, zone 5→void oost, kasteelplek #1) en `castles.txt` (de vier pinakels met gordel en marge,
   coördinaten uit `/ca castles` op seed 20260930).
3. **Bodemscan** `tools/test/py/voidscan.py`: per gebied `void` (info) en de fouten `bedrock` (boven Y −60 of boven de
   void), `loose` (groepjes van 1–2 blokken met niets vasts eronder), `fluidvoid` (vloeistof naast of boven de void),
   `water` en `lavahigh` (lava op/boven Y −54) in droge gebieden (alles behalve `--wet`, standaard de `z45*`-gebieden).
   De regel "ring 5 droog" (V9-B) zit daarmee in de bodemscan; lavalekken van de dam blijven `lavascan.py` (`lek` = 0).
   `tools/test/README.md`: gebieden en sectie "Voidscan (ring 5)".

## Tests
- `./gradlew build` groen: 1.146 tests, 0 fouten (nieuw: `CastlesCommandTest`: regelopmaak, nummer → plek). `runData` groen,
  `src/generated` ongewijzigd.
- **S-smoke** (verse wereld `r5-g0`, seed 20260930, headless): log alleen de Yggdrasil-`UnknownHostException`-ERROR, geen
  client-class-fout; RCON `/ca ring`, `/ca castles` werken. `/ca castles tp` niet uitgevoerd (geen speler in de container).
  Kasteelplekken: #1 327,5° 7794 −4962 r49 top127 gordel 75; #2 60,1° 4653 8078 r50 top124 gordel 62; #3 154,3° −8342 4012
  r51 top129 gordel 58; #4 245,4° −3871 −8440 r47 top139 gordel 77 (onderlinge hoeken 92,6 / 94,2 / 91,1 / 82,1°).
- **`world.sh r5-scan ring5.txt --settle 5`** + `voidscan.py --strict`: 0 fouten (void alleen in `c1`: 41.284 kolommen,
  `z5ve`: 6.909); `lavascan.py`: 0 lava, 0 lek (het skelet heeft geen grotten onder −54, de lavalaag ontstaat pas met grotten
  in 83/93 — dan opnieuw scannen). Eerste versie telde bedrock met lucht eronder in `z45n` (32, carvergrotten tussen de
  bedrocklagen van ring 4, vanilla): de regel telt nu alleen bedrock boven de void.
- **Determinisme** `det.sh --areas ring5.txt`: 991/991 chunks identiek (omgekeerd geladen).
- **Bitgelijk ring 1–4 en Nether** (82): dezelfde kaarten op a61afc1 (vóór golf 0) en na 82, byte voor byte gelijk:
  `terrainmap 3000 0 2400 8` (ring 1–3), `7000 0 600 4` (ring 4 met zone 3→4), `7650 0 100 1` (ring 4 tot vlak voor zone 4→5),
  Nether `1700 0 250 1` (Nether-4) en `2200 0 300 1` (Nether-5), hoogte en biome.

## Teststop 6-pre stap 1 (kaarten, `build/ring5-pre/`)
`top_overworld_8900_0_r1400_s4_*` (overzicht oost), `top_overworld_<x>_<z>_r300_s1_*` per kasteelplek,
`section_overworld_7700_0_10300_0.png` (radiaal oost), `section_overworld_6747_-4298_8687_-5534.png` (radiaal door #1),
`top_the_nether_2200_0_r300_s1_*` (Nether-5, placeholder).

## CODEBASE-delta
`command/CastlesCommand` (`/ca castles`, `tp`; in `CaCommand`), `RingTeleportCommand.findSafeY` package-private;
`tools/test/areas/ring5.txt`, `castles.txt`, `tools/test/py/voidscan.py`, README-secties.

## Beslissingen
Implementatiekeuzes: nummers 1–4 in het commando (index + 1); de droog-regel van ring 5 in `voidscan.py` in plaats van in
`waterscan.py`/`lavascan.py` (die blijven ongewijzigd; `waterscan` heeft vallijsten nodig die ring 5 niet heeft).

## Backlog
- [infra] `slots.sh`: aantal JVM-slots per machine instelbaar (`CA_MAX_SLOTS`) (uit `golf-0.md`).
- [infra] Na 83/93: `ring5.txt` opnieuw door `lavascan.py`/`voidscan.py` (lavalaag en dam echt in de wereld).
