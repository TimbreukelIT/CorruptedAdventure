# Ring 5 — golf 1: vormen, blokken en systemen

Scope per pakket: één categorie (CLAUDE.md §2), één commit per pakket met categorie-prefix. Namen en sleutels:
`contract.md` (vast sinds golf 0; **§11 "Besluiten vragenronde golf 1" gaat vóór elke "voorlopig"-waarde erboven**).
Werkwijze: `waves.md` §2 (golf 1), §3 (afhankelijkheden), §5 (eigenaarschap), §6 (INT), **met de lokale afwijkingen van
§3 hieronder** (golf 1 draait op de Windows-pc van de gebruiker, niet in de Linux-container). Golf 0: `golf-0.md`,
`81-infra.md`, `82-worldgen.md`, `81b-infra.md`.

**Status:** plan (2026-10-21). **Startcommit:** de commit met deze plannen op `main` (direct na `e2d9163c`). Alle
stroombranches en de integratiebranch takken daarvan af.

---

## 1. Stromen

| Stroom | Plan | Branch | Worktree (`P:/Projecten/Java/wt/…`) | Poort n | Pakketten (volgorde) | Categorie per commit |
|---|---|---|---|---|---|---|
| B | `golf-1-blocks.md` | `ring5/blocks-g1` | `r5-blocks-g1` | 0 | **88** corrupted varianten + houtset → **88r** recepten → **89** glitch/void/nulliet → **90** Nether-5 | `blocks`; 88r `items` |
| T | `golf-1-textures.md` | `ring5/textures-g1` | `r5-textures-g1` | 6 (geen server) | **99a** eerste versies ring 5 + Nether-5 + houtset + particle-sprites | `textures` |
| W-terrein | `golf-1-terrain.md` | `ring5/terrain-g1` | `r5-terrain-g1` | 1 | **84** buitenband → **85** breukzone 4→5 → **83** binnenband (zie §2) | `worldgen` |
| W-biomes | `golf-1-biomes.md` | `ring5/biomes-g1` | `r5-biomes-g1` | 2 | **86** biome-skelet; na INT1 **86b** (echte blokken in de surface) en **86c** (gespleten rots) | `worldgen` |
| W-nether | `golf-1-nether.md` | `ring5/nether-g1` | `r5-nether-g1` | 3 | **87** Nether-5-terrein + 2 biomes; na INT1 **87b** (echte blokken in de surface) | `worldgen` |
| C | `golf-1-conditions.md` | `ring5/conditions-g1` | `r5-conditions-g1` | 4 | **91** void-meter | `conditions` |
| A | `golf-1-atmosphere.md` | `ring5/atmosphere-g1` | `r5-atmosphere-g1` | 5 | **92** ringweer ring 5 (glitchstorm, omgekeerde regen, stilstand) | `atmosphere` |

Poorten: server 25570 + n, RCON 25580 + n (I = 7 blijft vrij). Elke stroom: eigen agent in een eigen worktree, alleen de
eigen bestanden (`waves.md` §1/§5), docs alleen in het eigen plan (onderaan "CODEBASE-delta", "Beslissingen",
"Backlog"); nooit `CODEBASE.md`, `docs/design/*`, `docs/backlog.md` (dat doet INT).

**Koppelingen tussen stromen, al vastgelegd in de plannen (geen vraag nodig):**
- W-terrein ↔ W-biomes: de biomedrempels en de pinakel-as (`golf-1-biomes.md` §2, `golf-1-terrain.md` §1.3). W-biomes
  is eigenaar van de drempels; een wijziging gaat via main naar W-terrein.
- W-terrein 85 → W-biomes 86c: de gespleten rots verhuist naar `ZoneFracture.landmarksWithin` (85 levert de accessor en een
  tijdelijke lege `landBridgesNear`; 86c ruimt op).
- B ↔ T: de texturepaden staan in beide plannen in dezelfde lijst (`golf-1-blocks.md` §5 = `golf-1-textures.md` §2).
- B → C: `#corruptedadventure:void_sources` (inhoud B in 89); C test met een eigen testdatapack.
- A → C: de void-factoren van het ringweer staan in `[weather.ring5]` (A); C leest ze vanaf INT1 (tot dan constanten).
- A ↔ C: "welk weer voelt een speler hier" = `RingWeather.stateAt` (A maakt hem band-bewust voor ring 5); C vraagt
  alleen `stateAt`, nooit zelf de band voor het weer.

## 2. Volgorde en spreiding op deze pc

**Machine:** 24 logische cores, 32 GB RAM (bij het opstellen ~12 GB vrij), P: ruim vrij (elke nieuwe worktree vraagt
~2 GB voor `build/moddev`). Elke build/runData/server is een JVM van enkele GB; de gebruiker speelt soms tegelijk.
**Regel voor main: max. 2 zware JVM's tegelijk, 3 alleen als de gebruiker niet speelt.** `slots.sh` staat vast op 3
slots (backlog: `CA_MAX_SLOTS`); main bewaakt de 2 zelf door te spreiden en door `slots.sh status` te volgen.

**Start gespreid** (de eerste `build` in een verse worktree is de zwaarste stap):
1. **t = 0:** W-terrein (lange pool: drie terreinpakketten na elkaar) en B (89/90 hebben de vragen van §6 nodig, 88 niet).
   T tegelijk: het texturescript is één `java Ring5Textures.java`-run zonder Gradle (licht); T's `build`/`runData` pas
   aan het eind.
2. **t ≈ +30 min** (als de eerste builds van W-terrein en B klaar zijn): W-biomes en W-nether.
3. **t ≈ +60 min:** C en A (kleiner; A zodra §6 OV1 beantwoord is).

**Volgorde binnen W-terrein: 84 → 85 → 83** (wijkt af van de nummering; `waves.md` §1 staat dat toe). Reden: teststop
6-pre-1 vraagt een eerste vorm van 84 en 85 (`golf-0.md` §5); de binnenband (83) is het minst gevoelig. Wordt
W-terrein de lange pool, dan kan main de gebruiker voorstellen 85 af te splitsen naar een eigen stroom "W-breuk"
(poort 8, eigen worktree; bestanden van 85 zijn grotendeels gescheiden van 83/84) — alleen na akkoord, want `waves.md`
§1 kiest bewust één eigenaar.

**Merge-volgorde INT1:** B → T → W-terrein → W-biomes → W-nether → C → A. Daarna klein en sequentieel op main:
**86b** (W-biomes) → **87b** (W-nether) → **86c** (W-biomes).

## 3. Lokaal worktree-protocol (Windows, wijkt af van `waves.md` §4)

Basis: `docs/plans/ring-3/waves.md` §4 stappen 1–12 inclusief "Golf 3 — sneller testen", met `tools/test/`
(`tools/test/README.md`, de Windows-gang: Git Bash voor de `.sh`, `python` met numpy voor `py/`). Verschillen:

1. **Worktree:** main maakt hem aan (de agent werkt er alleen in):
   `git worktree add -b ring5/<stroom>-g1 P:/Projecten/Java/wt/r5-<stroom>-g1 <startcommit>`. Nooit in andermans
   worktree of in de hoofdwerkmap. De agent meldt pad, branch en eerste `Voortgang:`-regel aan main.
2. **`run/` klaarzetten:** `bash tools/test/setup.sh <stroom> <n>` in de eigen worktree (n uit §1). De eerste `build`
   maakt `build/moddev` aan (minuten).
3. **Locks en slots:** lock-map `C:/Temp` (standaard op Windows). Elke build/runData/server via `slots.sh run` of
   `server.sh` (die nemen zelf een slot). Altijd `--no-daemon`; nooit `./gradlew --stop`. **`C:/Temp/ca-user-test.lock`
   telt hier wél:** bestaat hij, dan speelt de gebruiker; start niets, meld het aan main, wacht. Alleen main legt/haalt
   hem. Nooit wachten met een slot vast.
4. **Geen client in de stromen.** Er is lokaal wel een client, maar stromen hebben hem niet nodig en starten hem niet
   (geheugen, slots). Visueel kijkt de gebruiker bij 6-pre-1 en 6a. Testen per stroom:
   - **B** `./gradlew build` (unit tests, incl. `ClientIsolationTest`);
   - **D** `./gradlew runData` + `git diff --stat src/generated` (alleen eigen output; gecommit in de eigen branch);
   - **S-smoke** (waar het plan het vraagt) = `server.sh start <level> --fresh`, de log zonder ERROR (behalve de
     bekende Yggdrasil-`UnknownHostException`) en zonder client-class-fout, RCON-commando's via `py/rcon.py`,
     `server.sh stop`;
   - **kaarten** = `/ca terrainmap` via RCON (Nether: `/execute in minecraft:the_nether run ca terrainmap …`); PNG's in
     `<wereld>/corruptedadventure/maps/`, kopiëren naar `build/test/maps/<doel>/` (niet in git) en de paden aan main.
5. **Licht testen per pakket** (voorkeur gebruiker sinds 2026-10-19): B, D, unit tests, en S-smoke/kaarten alleen waar
   het plan het noemt. **Geen** `det.sh`, `perf.sh`, `waterscan.sh`, `voidscan.py`/`lavascan.py`-rondes per pakket:
   die doet INT1 één keer vóór 6a.
6. **Commit en review:** commit per pakket, gestaged per pad (geen `git add -A`, nooit `docs/prompts/`); vóór elke commit
   `/code-review` (args `medium`) op de niet-gecommitte diff; bevindingen oplossen of in het plan vastleggen. Trailers
   volgens de sessie-instructie van de agent.
7. **Pushen doet alleen main.** Stromen pushen nooit. Alles komt via INT1 in `main` (één lijn, geen langlevende
   zijbranches): de integratiebranch wordt na de checks in `main` gemerged; daarna mogen de stroombranches en
   worktrees weg (`git worktree remove`, door main).
8. **Liveness en vragen:** `Voortgang: <tijd> <stap>`-regels in het eigen plan (echte klok via `date`). Een ontwerpkeuze
   buiten contract/plan → dat deel stoppen en via main aan de gebruiker (2–4 opties, aanbevolen eerst).
   Implementatiedetails beslist de stroom zelf en zet ze onder "Beslissingen".
9. **Eindrapport aan main:** branch, laatste hash, pakketten, testresultaten, review-uitkomst, afwijkingen, en de drie
   delta-secties.

## 4. Vóór de start (main)

- `CODEBASE.md` kent golf 0 van ring 5 nog niet (geen `Ring5Band`, `Ring5Field`, `ZoneFracture`, `ModDataMaps`,
  `/ca castles`, `voidscan.py`, hakentabel "Ring 5-haken per stroom (81)"). Stromen lezen `CODEBASE.md` eerst
  (CLAUDE.md §1). **Main voegt vóór de start de CODEBASE-delta's van `81-infra.md`, `82-worldgen.md` en `81b-infra.md`
  toe** (eigen `infra:`-commit), of — als dat de start ophoudt — leest elke stroom die drie plannen als aanvulling (staat
  in elk stroomplan).
- De antwoorden op §6 (OV1, OV2) in `contract.md` §11 zetten; de voorstellen van §7 tonen.

## 5. Integratie 1 (lokaal) en de stappen naar teststop 6a

Protocol `waves.md` §6, lokaal:
1. **Worktree** `P:/Projecten/Java/wt/r5-int-g1` op `ring5/integration-1` vanaf de startcommit; `merge --no-ff` in de
   volgorde van §2. Conflicten: `src/generated/**` regenereren; append-blokken (`// ring 5 — <stroom>`) beide houden;
   al het andere terug naar de stroom. Bekende kleine conflicten: `Ring5JsonTest` (W-terrein, W-biomes en W-nether
   passen elk hun eigen assert aan), `ServerConfig` static-blok (C en A), `ModDensityFunctionTypes`.
2. Na elke merge `./gradlew build` en `runData`; vangnet `git diff --exit-code src/generated`.
3. **Integratiefixes (`infra:` of de categorie van de inhoud):**
   - `ModNetwork.PROTOCOL_VERSION` `"8"` → `"9"` (C: `Condition.VOID` maakt `condition_sync` langer; A: `STANDSTILL`
     is een nieuwe enum-index);
   - C's void-weermodifier leest de factoren uit `[weather.ring5]` van A (constanten en hun test-asserts weg);
   - controleren dat C's band (`Ring5Band.inner/outer`) en A's band-semantiek van `stateAt` dezelfde grens gebruiken
     (`value ≥ 0` = buiten);
   - `MapPalette`: vaste kaartkleuren voor de 6 ring 5- en 2 Nether-5-biomes;
   - `voidscan.py`: de standaard `--wet` (`z45*`) vervalt, want zone 4→5 is na 85 droog;
   - `tools/test/areas/ring5.txt`/`castles.txt` nalopen (de kasteelplekken veranderen niet; nieuwe tegels voor een
     kathedraal in Nether-5 en een brug).
4. **S-smoke** (verse wereld `ring5-wave1`, poort 7): log schoon, RCON `/ca ring`, `/ca castles`, `/ca weather`,
   `/ca condition …`; **W-smoke** = de kaartenset van `golf-0.md` §5 (overzicht oost, de vier kasteelplekken, de radiale
   doorsneden, Nether-5) plus een doorsnede dwars door zone 4→5 en een kaart van een kathedraal.
5. **Zwaar, één keer:** `world.sh` op `ring5.txt` + `castles.txt` → `voidscan.py --strict` (0 fouten), `lavascan.py`
   (`lek` 0); `waterscan.sh` op `ring4.txt` (zone 4→5 zonder water); `det.sh` op `ring5.txt` (omgekeerd geladen);
   feature-index ring 1–4 (`fi3.txt`); bitgelijkheid ring 1–4 buiten zone 4→5 (kaarten vóór/na, zoals 81b);
   **prestatie** lokaal en officieel (`perf.sh`, lock + alle slots, gebruiker speelt niet; budget noise ≤ 1,75×).
6. `/code-review` (medium) op de integratiediff; daarna **merge in `main`** (main pusht op verzoek van de gebruiker).
7. **Na INT1, op main, sequentieel:** 86b, 87b, 86c (elk eigen worktree vanaf de INT1-merge, eigen review, merge in
   main); daarna de kaarten opnieuw en `voidscan`/`det` alleen als 86b/87b de vorm raakten (normaal niet).
8. **Docs** (INT, eigen `infra:`-commit): CODEBASE-delta's van alle stromen, beslissingenlog, backlog, de voorgestelde
   doc-wijzigingen van `waves.md` §9 die nog openstaan, en de checklist `docs/plans/ring-5/teststop-6a.md`
   (coördinaten uit `/ca castles` en `/ca tpboundary`).

### Teststop 6-pre-1 — terreinpreview (niet blokkerend; `golf-0.md` §5 "Vroeg in golf 1")
- **Wanneer:** zodra W-terrein 84 en 85 gecommit heeft (vóór 83).
- **Kaarten** (W-terrein, in de eigen worktree, verse wereld seed `20260930`, naar `build/test/maps/6-pre-1/`): de set
  van `golf-0.md` §5 plus `section` dwars door zone 4→5 (bv. `7800 -600 8200 600`) en een kaart van de buitenband met
  brokken en bruggen rond één kasteel.
- **Vliegtocht (gebruiker, lokaal):** main legt `C:/Temp/ca-user-test.lock` (alle stromen pauzeren hun JVM's), de
  gebruiker start een client vanuit `P:/Projecten/Java/wt/r5-terrain-g1` (of main start daar `server.sh start pre1
  --fresh --client`), vliegt langs de breukzone, de bandlijn en één kasteel (`/ca castles tp 1`). Biomes en surface zijn
  daar nog placeholder/vanilla.
- **Beoordelen:** band 50/50 en grilligheid, gradiënt naar de void, pinakels (grootte, ligging), bruggen, breukzone
  (dicht genoeg, overal over te steken, spleten diep genoeg). Bijsturing gaat als getallen terug naar W-terrein.

### Teststop 6a — terrein-wow (BLOKKEREND; `waves.md` §2)
Klaar moet zijn (INT1 + 86b/87b/86c op main):
- terreinkaarten (PNG) van ring 5, zone 4→5 en Nether-5, vóór de game aan de gebruiker voorgelegd;
- lokaal in-game (gebruiker), verse wereld seed `20260930`: de breukzone 4→5 (oversteekbaar, geen landbruggen), de
  dode binnenband, de overgangsband, het uiteenvallen naar buiten, de vier kasteelpinakels met bruggen, de buitenrand tot
  de border, de biomeverdeling en de echte blokken in de surface, de ringbreedte (`/ca tpring 5`, `/ca tpboundary 4`);
- een eerste blik op Nether-5 (vorm, ruimte, `hollow_cathedral`);
- prestatie: de lokale meting van INT1 (stap 5);
- nog niet nodig: grotten, aankleding, horror-features, sfeer, weer en condities (mogen meedraaien; oordeel in 6b).

## 6. Open vragen voor de gebruiker

**Beantwoord 2026-10-21:** OV1 = A (eigen type per band, gedeelde tijden); OV2 = A (diamant, hardheid vanilla-erts
+1,5, XP nulliet 5–9 / witte kern 6–10, witte kern licht 3).

Alleen echte blokkades; de rest staat als voorstel in §7.

### OV1 — Ringweer ring 5: hebben binnen- en buitenband tegelijk elk hun eigen weer? (blokkeert A, 92)
Contract §11: "één cyclus; het type hangt af van de band van de speler via gewichten per band".
- **A *(aanbevolen)*: ja, gedeelde tijden, eigen type per band.** De cyclus bepaalt wanneer het weer is (helder ↔ weer,
  duur); per band wordt het type gekozen uit de gewichten van die band (binnen vooral glitchstorm en stilstand, buiten
  vooral omgekeerde regen). Binnen kan dus een glitchstorm razen terwijl buiten omgekeerde regen valt; in de
  overgangsband (~200) vloeit het beeld over. *Complexiteit:* middel (het buitentype volgt uit de gesyncte seed; geen
  payload). *Multiplayer:* spelers in verschillende banden zien tegelijk ander weer; de server rekent per speler. *Speelgevoel:*
  elke band houdt zijn eigen karakter, ook tegelijk.
- **B: nee, één type tegelijk voor de hele ring.** De cyclus trekt één type; de band bepaalt alleen of je het voelt
  (zoals ring 4, maar met een kans per band i.p.v. een vaste band): soms beide banden hetzelfde weer, soms één band
  helder. *Complexiteit:* laag. *Speelgevoel:* minder eigen weer per band; vaker een helder stuk.

### OV2 — Mijn-niveau van nulliet en witte kern (blokkeert B in 89 en 90, niet 88)
Ring 4 (V3 = B): `needs_diamond_tool`, hardheid vanilla-erts +1,0, XP 4–7 / 5–9.
- **A *(aanbevolen)*: één trede boven ring 4 zonder netherite.** Beide `needs_diamond_tool`, hardheid vanilla-erts +1,5;
  XP nulliet 5–9, witte kern 6–10; nulliet zwak emissive zonder bloklicht, witte kern zachte gloed (licht 3). Fase 2
  kan de gear-tier nog verfijnen. *Complexiteit:* laag. *Speelgevoel:* duidelijk zwaarder dan ring 4, geen nieuwe
  blokkade.
- **B: netherite-niveau** (`neoforge:needs_netherite_tool`). Echte eindgame-drempel. *Risico:* niet gecontroleerd of
  ancient debris in de Nether-ringen te vinden is; zo niet, dan is nulliet onbereikbaar tot fase 2.
- **C: precies zoals ring 4** (diamant, +1,0, XP 4–7). *Speelgevoel:* geen merkbare stap tussen ring 4 en 5.

## 7. Voorstellen ter bevestiging (niet blokkerend; gelden tenzij de gebruiker anders zegt)

**Bevestigd 2026-10-21:** P1–P5 zoals hieronder, door de gebruiker akkoord.

| # | Stroom | Voorstel | Wanneer bijsturen |
|---|---|---|---|
| P1 | W-biomes/W-terrein | Biomeverdeling binnen: `glitch_fields` ~33 %, `false_meadow` ~13 %, `corrupted_wastes` ~54 %; buiten: `shattered_expanse` waar de breukas ≥ 0,6, de rest half `void_scars` / half `inverted_hills` | 6-pre-1 / 6a |
| P2 | A | Standaardgewichten per band: binnen glitchstorm 55, stilstand 30, omgekeerde regen 15; buiten omgekeerde regen 70, glitchstorm 20, stilstand 10; stilstand 40–120 s | 6b (config) |
| P3 | C | Void-schade zoals gif: 1,0 per 2 s, `DifficultyFloor.LETHAL_FROM_NORMAL` (peaceful niets, easy tot 10 HP, normal/hard dodelijk) | 6b (config) |
| P4 | C | De void-rand (9.700–10.200, ring `VOID`) telt voor de meter als buitenband (zoals het weer: `void` volgt ring 5) | 6b |
| P5 | C/A | Nacht- en weerfactor stapelen niet: de grootste geldt (blijft binnen "×1,5–2" van §11); ook stilstand ×1,5 | 6b (config) |

## Voortgang
- 2026-10-21: plan opgesteld (startcommit = de commit met deze plannen).
