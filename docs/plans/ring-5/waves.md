# Ring 5 — golven, stromen en integratieprotocol

Scope: ring 5 + Nether-ring 5 + de herziening van zone 4→5 (bouwplan blok G, `fase-1-wereld.md` §3; herziene indeling
in §8 hieronder). Ontwerp: `fase-1-wereld.md` → "Ring 5 — Corrupted ring", blok "Sparring ring 5", en de ruwe uitkomst in
`docs/plans/ring-5-sparring.md` (rondes 1–5, 2026-10-21, gezaghebbend). Werkwijze: zoals ring 4 (`docs/plans/ring-4/waves.md`):
contract eerst, golven, stromen parallel in eigen git-worktrees, één integratie per golf, handmatige orkestratie door de
hoofdsessie (main), plus de lichte afstemlus (`docs/plans/ring-4/tuning-loop.md`) voor aankleding. Namen en sleutels:
`contract.md`. Golf 0: `golf-0.md`.

**Status:** concept (2026-10-21). **Start:** na teststop 5c (ring 4) én nadat de gebruiker de open punten in
`golf-0.md` ("Open punten voor de gebruiker") heeft beantwoord. Tot dan wordt er niets gebouwd (CLAUDE.md §3); de
antwoorden gaan in `contract.md`, en via INT in `fase-1-wereld.md` en het beslissingenlog.

**Omgeving:** Claude Code-cloudcontainer (Linux, 4 cores, 15 GB, geen scherm). Mogelijk: `build`, `runData`, headless
`runServer` met RCON, `/ca terrainmap` (PNG, zonder chunks of client), `world.sh`/`det.sh`/`waterscan.sh` en de offline
scans, de texture- en geluidsscripts. Niet mogelijk: `runClient`, screenshots, joinen, vergelijkbare prestatiecijfers.
**In-game checks doet de gebruiker lokaal**, bij de teststops (§2), vanuit een branch die main klaarzet.

---

## 1. Stromen

| Stroom | Categorie | Pakketten (§8) | Bezit (gedeelde bestanden: §5) |
|---|---|---|---|
| **I** — infra | `infra` | 81, 81b (golf 0) | `config/*`, `command/*` (debug), `datagen/lang/*` (raamwerk), `DataGenerators`, `ModDataMaps` (type-registratie) |
| **W-terrein** | `worldgen` | 82 (golf 0), 83, 84, 85, 93 | `density_function/ring_5/overworld/**`, `density_function/zone_4_5/**`, `noise/ring_5_*`, de ring 5-sleutels in `ring_blend/overworld*`, `final_density` in `noise_settings/overworld.json`, `RingFluidPicker`/`RingAquifer` (ring 5-regel en zone 4→5), `Ring5Band`, `Ring5Field`, `ZoneFracture`, `Ring4Sundered` (ombouw/afbraak, 85), de bijbehorende DF-types |
| **W-biomes** | `worldgen` | 86, 94, 95 | `Ring5Biomes`, `Ring5Features`, `Ring5Carvers`, `noise_settings/overworld.json` (surface rule, ring 5-takken), preset: ring 5-overworld-source, `ModBiomeTagsProvider` (ring 5-helper), `Ring5BiomeNames`, `SplitRockFeature`/`SunderedReachFeatures` (verplaatsen, 85-volger) |
| **W-nether** | `worldgen` | 87, 96 | `density_function/ring_5/nether/**`, `noise/nether_ring_5_*`, `noise_settings/nether.json` (ring 5-tak), preset: Nether-ring 5-source en `fluid_levels.ring_5`, de Nether-5-regel in `RingFluidPicker` |
| **B** — blokken | `blocks` | 88, 89, 90 | `ModBlocks`, `ModItems`, `ModTags` (inhoud), `block/*`, blok-datagen (states, models, loot, tags, de data map-inhoud), `Ring5BlockNames` |
| **C** — condities | `conditions` | 91, 97 | `condition/*` (void, stilte), `ServerConfig` `[conditions.void]`, `[conditions.silence]`, `Ring5ConditionText` |
| **A** — sfeer | `atmosphere` | 92, 98 | `weather/*` (ring 5), `client/weather/*` (ring 5 + Nether-5), `RingWeatherType`, `ServerConfig` `[weather.ring5]`, `ModSoundEvents`, `ModParticleTypes`, `BiomeAmbience` (ring 5-builders), het ring 5-geluidsscript, `Ring5WeatherText` |
| **T** — textures | `textures` | 99a, 99b, 99 | `tools/textures/Ring5Textures.java`, PNG's en `.mcmeta`'s met ring 5-/Nether-5-namen |
| **INT** — integratie | per commit de categorie van de inhoud; eigen commits `infra:` | — | `src/generated/**`, `CODEBASE.md`, `docs/design/*`, `docs/backlog.md`, `ModNetwork.PROTOCOL_VERSION` |

- De herziening van zone 4→5 (85) is van **W-terrein**: één eigenaar voor alles wat de vorm van de zone bepaalt
  (ook de ring 4-kant in `IslandField`/`Ring4Sundered`), zodat de les "zonevorm met twee eigenaren" (§7) niet terugkomt.
- W-terrein bouwt zijn pakketten **na elkaar** (één eigenaar van de worldgen-Java); 85 mag vóór 84 als de gebruiker
  dat bij 6-pre wil.

## 2. Golven

### Golf 0 — fundament (sequentieel, worktree `ring5/golf0`)
Details en tests: `golf-0.md`.
1. **Open punten** (`golf-0.md`, max. 12 vragen) aan de gebruiker; antwoorden in `contract.md`. Daarna staat het contract vast.
2. **81** (`infra`): contract-sleutels als constanten (biomes, tags, data map-type), lang-raamwerk en lege haken per
   stroom; config-standaarden als een antwoord dat vraagt (zonebreedtes, `golf-0.md` V2/V7).
3. **82** (`worldgen`): ring 5-skelet — `ring_blend`-invoer en router-sleutels, stubs van de gedeelde DF's, band-as
   `Ring5Band`, `Ring5Field` met de kasteelplekken, vloeistofregel ring 5, echte `initial_terrain`, het skelet van de
   breukzone (`ZoneFracture`, identiteit) en de biomehaak (router-as of Java, V12).
4. **81b** (`infra`): `/ca castles` (lijst + teleport naar een kasteelplek), leest `Ring5Field` (82).
5. Commit per pakket; golf 1 takt af van de laatste golf 0-commit.

**Teststop 6-pre — terreinpreview (niet blokkerend).** Twee momenten:
- *direct na golf 0* (in de container): `/ca terrainmap`-kaarten van de band-as en de vier kasteelplekken (grove
  vormen, `golf-0.md` §5); main legt de PNG's voor;
- *vroeg in golf 1*, zodra 84 en 85 een eerste vorm hebben: kaarten en doorsneden van de breukzone 4→5 en de
  buitenband, en een korte vliegtocht door de gebruiker lokaal.
Doel: de verhoudingen (band, gradiënt, kastelen, bruggen, breukzone) bijsturen vóór de aankleding.

### Golf 1 — vormen, blokken en systemen (parallel)

| Stroom | Pakketten | Af als | Afhankelijk van |
|---|---|---|---|
| W-terrein | 83, 84, 85 | binnenband (dode grond, "net fout"-vormen, glitchvormen, losse scheuren), buitenband (uiteenvallen, brokken, bruggen, kasteelpinakels met bruggen, `void_scars`-scheuren tot de void, omgekeerde heuvels als vorm, aansluiting op de buitenrand), breukzone 4→5 (landbruggen weg); surface tijdelijk vanilla/stand-in | golf 0 |
| W-biomes | 86 | 6 biomes op band-, klimaat- en breukassen, kleuren, voorlopige surface, geen of vanilla-spawns, tags, lang; `transitions.ring_4` blijft `sundered_reach` | golf 0 (stubs; lokale ruis of rebase op W-terrein, §3) |
| W-nether | 87 | Nether-5-terrein (witte vlaktes, zuilenrasters, spiegelsteen-vloer), `white_halls` + `hollow_cathedral`, vloeistofregel Nether-5, ruimte-check, trede 4\|5 afgedekt | golf 0 |
| B | 88, 89, 90 (één commit per pakket) | alle contractblokken met loot, tags, data map `healthy_block`; models verwijzen naar T's paden | golf 0 |
| C | 91 | void-meter volgens contract §5 (teleports alleen op vaste grond in de binnenband, darkness in de buitenband) | golf 0 (`Ring5Band`, tags) |
| A | 92 | glitchstorm, omgekeerde regen, stilstand (staat server, rendering client), config `[weather.ring5]` | golf 0 (`Ring5Band`) |
| T | 99a | eerste goede versies van alle ring 5- en Nether-5-textures (37b-stijl), geanimeerd glitchblok, sterrenpixels voidsteen, contactblad | golf 0 (namen) |

**Integratie 1** (§6). Merge-volgorde: B → T → W-terrein → W-biomes → W-nether → C → A. Daarna klein en sequentieel:
de echte ring 5-blokken in de surface (W-biomes, lettercode `86b`).

**Teststop 6a — terrein-wow (BLOKKEREND).** Klaar moet zijn:
- terreinkaarten (PNG) van ring 5, zone 4→5 en Nether-5, vóór de game aan de gebruiker voorgelegd (container);
- lokaal in-game (gebruiker), verse wereld seed `20260930`: de breukzone 4→5 (oversteekbaar, geen landbruggen), de dode
  binnenband, de overgangsband, het uiteenvallen naar buiten, de vier kasteelpinakels met bruggen, de buitenrand tot de
  border, de biomeverdeling en de echte blokken in de surface, de ringbreedte (`/ca tpring 5`, `tpboundary 4`);
- een eerste blik op Nether-5 (vorm, ruimte, `hollow_cathedral`);
- prestatie: indicatie in de container (relatief, mod/vanilla in dezelfde server); de officiële meting lokaal als de
  gebruiker dat wil.
- Nog niet nodig: grotten, aankleding, horror-features, sfeer, weer en condities (mogen meedraaien, beoordeling in 6b).

### Golf 2 — inrichting, overleving en sfeer (parallel)

| Stroom | Pakketten | Inhoud |
|---|---|---|
| W-terrein | 93 | grotten: onmogelijke gangen (rasters, lussen), void-grotten, gekopieerde/gespiegelde grotten; gespiegelde dubbelgangers (kopieerveld) |
| W-biomes | 94, 95 | aankleding binnenband + `false_meadow` + horror-features (ontbrekende chunks, Herobrine-tekens, verlaten kampen, herinneringen); aankleding buitenband + kasteelomgeving (verwoeste grond, voidsteen-randen) + ertsen (nulliet, glitchblokken, vanilla afgebouwd) + omgekeerde bomen |
| W-nether | 96 | zuilenrasters, witte kern in de zuilen, stil licht, de reuzenzuil van `hollow_cathedral` |
| C | 97 | stilte (Nether-5) + void-koppelingen met de blokken van B |
| A | 98 | sfeer ring 5 en Nether-5: lucht (binnen grijs-violet, buiten zwart met sterren), fog, glitch-fog, echo-voetstappen, geluid (`.ogg`'s), stilte-client (volume, HUD) |
| T | 99b | bijsturen na 6a; nieuwe blokken/particles uit golf 2 |

Aankleding (94–96) loopt na de eerste versie via de **afstemlus** (`docs/plans/ring-4/tuning-loop.md`, in de container
zonder screenshots: `/ca terrainmap` en regio-scans; de gebruiker kijkt lokaal).

**Integratie 2**, dan **teststop 6b — overleving + sfeer** (lokaal, dedicated server + 2 clients): void-meter
(teleports nooit in blokken of richting void, alleen binnenband), stilte met twee spelers, de drie weertypes, grotten,
horror-features, sfeer en geluid per biome, Nether-5.

### Golf 3 — afronding
- Fix-ronde op 6b, per categorie (lettercodes zoals 74b/75c bij ring 4).
- **99**: definitieve textures.
- Backlog-opruiming ring 5 (o.a. `placeholder_ring_5`/`placeholder_nether_ring_5` weg, backlog r. 266).

**Teststop 6c — eindcheck** ring 5 + Nether-5 + zone 4→5: alle W-lijsten, standaard-W (elke ring en grens, portal per
ring, ook ring 5 → Nether-5), determinisme, prestatie. Daarna blok H (100).

## 2b. Implementatiekeuzes (niet door de gebruiker besloten; zoals ring 3/4)
- **81 is `infra` maar zet ook worldgen-/blocks-artefacten neer** (keys, tag-keys, data map-type, lege bootstrap- en
  helperklassen): het contractraamwerk van golf 0, zoals 37c en 58.
- **De void-modifiers van het ringweer bouwt C, niet A** (zoals hitte en gif): A levert alleen de factoren in
  `[weather.ring5]`.
- **De ombouw van zone 4→5 is `worldgen` van ring 5** en raakt ring 4-bestanden (`Ring4Sundered`, `IslandField`,
  `RingFluidPicker`, ring 4-tests). Dat is besloten in de sparring (ronde 5, "na 5c, bij golf 0/1 van ring 5").

## 3. Afhankelijkheden in één oogopslag
- Iedereen ← golf 0 (contract, constanten, stubs, lang-raamwerk, `Ring5Band`, `Ring5Field`).
- W-biomes 86 ← de assen van 82 (band echt, klimaatassen echt, breukas stub). De breukas wordt pas in 84 echt: test
  `shattered_expanse` met tijdelijke lokale ruis (niet committen) of na een rebase op W-terrein.
- De surface met echte blokken (86b) ← B + W-terrein + W-biomes geïntegreerd.
- C (void) ← `Ring5Band` (82), tags (81); de blokbronnen (glitchblok, voidsteen) koppelt C in golf 2 (97). Een lege tag is
  geen fout: C test met een eigen testdatapack. Veilige teleport ← "vaste grond" = de server-side bloktoets, geen DF.
- A ← `Ring5Band` (lucht per band); geen payload nodig als de band op de gesyncte layout rekent (contract §4).
- T ← alleen de namen uit het contract.
- W-biomes 95 (kasteelomgeving, nulliet in scheurwanden en brokonderkanten) ← `Ring5Field`-accessors (84).
- W-biomes: de gespleten rots van ring 4 (`SplitRockFeature`) hangt aan de landbruggen; verhuizen of weghalen volgt
  85 (V8) en gebeurt in dezelfde golf door W-biomes.
- W-nether 87 ← niets van W-terrein behalve de vloeistof-haak in `RingFluidPicker` (eigen methode, §5).

## 4. Worktree-protocol (Linux-container)
Basis: `docs/plans/ring-3/waves.md` §4 (stappen 1–12) inclusief "Golf 3 — sneller testen", met `tools/test/`
(`tools/test/README.md`, sectie "Linux"). Verschillen voor ring 5 in deze container:
1. **Worktrees** onder `/home/user/wt/r5-<stroom>-g<golf>`, branch `ring5/<stroom>-g<golf>` (lokaal), vanaf de
   golf-startcommit: `git worktree add -b ring5/<stroom>-g<n> /home/user/wt/r5-<stroom>-g<n> <commit>`. Nooit in andermans
   worktree of in de hoofdwerkmap werken. **Pushen doet alleen main**, en alleen op verzoek van de gebruiker.
2. **`run/` klaarzetten:** `bash tools/test/setup.sh <stroom> <n>` (poorten zoals ring 3/4: B 0, W-terrein 1, W-biomes 2,
   W-nether 3, C 4, A 5, T 6 geen server, I 7; server 25570 + n, RCON 25580 + n). De eerste build in een nieuwe
   worktree maakt `build/moddev` aan (minuten, ~2 GB).
3. **Locks en slots:** lock-map `${TMPDIR:-/tmp}/ca-test` (standaard op posix). Max. 3 slots in `slots.sh`, maar met
   4 cores en 15 GB **in de praktijk max. 2 zware JVM's tegelijk** (build/runData/server): stromen starten gespreid,
   main houdt het bij. `ca-user-test.lock` is hier niet nodig (de gebruiker speelt lokaal, niet in de container); main
   mag hem toch gebruiken om alles te pauzeren. Altijd `--no-daemon`; nooit `./gradlew --stop`.
4. **Geen client:** `client.sh` weigert (exit 1). Er zijn geen screenshots en geen join. In de plaats:
   - **S-smoke** = headless `server.sh start <level> --fresh`, de log zonder ERROR en zonder client-class, RCON-commando's
     (`/ca ring`, `/ca castles`, `/ca terrainmap …`), `server.sh stop`; de client-isolatie zelf bewaakt
     `ClientIsolationTest` in `build`;
   - **W-smoke** = kaarten en doorsneden met `/ca terrainmap` via RCON (overworld; Nether met
     `/execute in minecraft:the_nether run ca terrainmap …`) en regio-scans van `world.sh`-werelden. `tpring`, portals en
     het speelgevoel test de gebruiker lokaal bij de teststop.
5. **Kaarten** komen in `<wereld>/corruptedadventure/maps/`; kopieer ze naar `build/test/maps/<teststop>/` (niet in git)
   en meld de paden aan main. Main legt ze voor (beschrijving, en zo nodig een privé-pagina met de PNG's).
6. **Bouwen, review, docs:** zoals ring 4: alleen de eigen pakketten en bestanden (§1, §5); commit per pakket met
   categorie-prefix, gestaged per pad (geen `git add -A`); `/code-review` vóór de commit; docs alleen in het eigen plan
   `docs/plans/ring-5/golf-<n>-<stroom>.md` met onderaan "CODEBASE-delta", "Beslissingen", "Backlog"; niet in
   `CODEBASE.md`, `docs/design/*` of `docs/backlog.md`. Trailers volgens de sessie-instructie.
7. **Liveness en vragen:** zoals ring 3 §4.10–11 (`Voortgang: <tijd> <stap>`-regels, echte klok via `date`); een
   ontwerpkeuze buiten het contract → stoppen en via main naar de gebruiker (2–4 opties, aanbevolen eerst).
8. **Offline scans voor ring 5:**
   - *vloeistofscan*: in ring 5 geen water of lava (bij V9-A), en nergens vloeistof tegen de void of tegen lucht in de
     breukzone (`waterscan.sh` en `lavascan.py` op de ring 5-gebieden; uitbreiding van de scripts in 82, `golf-0.md`);
   - *bodemscan* (nieuw, 82): kolommen van `void_scars` en de buitenband zonder bedrock waar dat moet, en geen
     zwevende losse blokken (≤ 2 blokken groot) boven de void;
   - determinisme (`det.sh`, wereld b omgekeerd geladen) en de feature-index van ring 1–4 zoals altijd.

## 5. Eigenaarschap van bestanden die meerdere stromen raken

| Bestand | Regel |
|---|---|
| `ModConfiguredFeatures`, `ModPlacedFeatures`, `ModFeatures`, `ModDensityFunctionTypes`, `ModPlacementModifierTypes`, `ModSurfaceConditions` | Alleen **toevoegen**, in een eigen blok `// ring 5 — <stroom>` aan het eind; geen bestaande regels wijzigen. Uitzondering: W-terrein mag in 85 de ring 4-blokken van `sundered_reach` aanpassen/verwijderen (V8), met een aparte commit. |
| preset `corrupted_world.json` | W-biomes: overworld `rings.ring_5` (en zo nodig een Java-biomehaak-veld, V12); W-nether: Nether `rings.ring_5` en `fluid_levels.ring_5`. `transitions.ring_4` (`sundered_reach`) blijft staan. Elk alleen de eigen sleutel. |
| `ring_blend/overworld*` | 82 zet de ring 5-sleutels. `switch_at` `["ring_5"]` (grens 5→void schakelt hard) blijft; de zone 4→5 heeft `transitions.ring_4`, dus `ring_4` staat er niet in (regel ring 3). |
| `noise_settings/overworld.json` | `noise_router.final_density` (de `zone_fracture`-wrapper) is van W-terrein (82, 85); de surface rule van W-biomes (ring 5-takken, ook de bedrock-uitzondering als V3 die vraagt). |
| `RingFluidPicker`/`RingAquifer` | W-terrein: ring 5-regel en zone 4→5; W-nether: een eigen methode voor Nether-5 (87). Ring 2/3/4-regels niet raken, behalve de zone 4→5-regels in 85. |
| `IslandField`, `Ring4Sundered`, `Ring4Tunnels` | W-terrein (85). |
| `RingBiomeSource` | 82 (W-terrein) als V12 een Java-haak kiest; daarna alleen via main. |
| `ServerConfig` | C: `[conditions.void]`, `[conditions.silence]`; A: `[weather.ring5]`. Elk een eigen methode/blok. |
| `ModTags`, `ModDataMaps` | 81 zet alle contract-keys; daarna B (blok/item, data map-inhoud) en de worldgen-stromen (biome-tags) via main. |
| lang | elke stroom alleen de eigen helper (81). |
| `RingWeatherType` | A. |
| `command/*` | I (81b); een ander debugcommando via main. |
| `ModNetwork.PROTOCOL_VERSION` | INT, één verhoging per golf als er een payload bij komt. |
| `src/generated/**`, `CODEBASE.md`, `docs/design/*`, `02-beslissingenlog.md`, `docs/backlog.md` | INT. |

## 6. Integratieprotocol (INT, per golf, in de container)
Het snelle protocol van ring 3 golf 3 / ring 4 (`docs/plans/ring-4/waves.md` §6), aangepast:
1. Integratie-worktree `/home/user/wt/r5-int-g<n>` op branch `ring5/integration-<n>` vanaf de golf-startcommit; merge
   `--no-ff` in de vaste volgorde (§2). Conflicten: `src/generated/**` regenereren, append-blokken beide houden, al het
   andere terug naar de stroom.
2. `./gradlew build` en `runData` na elke merge; vangnet `git diff --exit-code src/generated`.
3. **S-smoke headless** (§4.4) op een verse wereld `ring5-wave<n>`; **W-smoke** = kaartenset per golf
   (`golf-0.md` §5 voor de vaste coördinaten) + de scans van §4.8.
4. Determinisme omgekeerd geladen (`det.sh`) op de ring 5-gebieden + feature-index ring 1–4.
5. Prestatie alleen als een pakket noise of surface raakt: in de container **alleen relatief** (mod/vanilla in dezelfde
   server, `perf.sh --warm 1 --cold 0`); het budget (≤ 1,75×) wordt lokaal bevestigd bij 6a/6c.
6. `/code-review` (medium) op de integratiediff, dan klaarzetten voor de gebruiker (branch + korte checklist
   `docs/plans/ring-5/teststop-6<x>.md` met coördinaten uit `/ca castles` en `/ca tpboundary`).
7. Docs (CODEBASE-delta, beslissingen, backlog, de voorgestelde doc-wijzigingen van §9) mogen na de merge, vóór de
   afsluitende commit van de golf.

## 7. Lessen uit ring 3 en ring 4, hier verwerkt

| Les | Waar |
|---|---|
| Een zonevorm met twee eigenaren gaat mis: in zone 4→5 mengt `ring_blend` de afgrond van ring 4 met placeholdergrond van ring 5 (backlog r. 262), en `Column.surface()` is daar niet de echte grond (decor D, r. 396) | breukzone als één gedeeld veld ná de blend (`ZoneFracture`, contract §4, V8); één eigenaar W-terrein (§1) |
| Water tegen lucht kostte in 5a een fixronde (~143.000 waterwanden in zone 4→5) | vloeistofregel ring 5 al in golf 0 (82, V9), vloeistof- en bodemscan vanaf golf 1 (§4.8) |
| Vloeistofnaad tussen ringen (water ↔ lokale lava aan de buitenrand van zone 4→5, r. 259) | 82 lost hem op in de vloeistofregel ring 5 |
| Eilanden van ring 3 botsten met de kolom-caps; ring 4 kreeg een eigen veld | `Ring5Field` met eigen raster en eigen `Column` vanaf 82 |
| Biomes testen op stubs = één biome | lokale ruis of rebase (§3) |
| Overgangsbiome vergeten in `switch_at` | regel in §5; zone 4→5 houdt `transitions.ring_4` |
| Een volledige ronde kostte ~4 uur (det ~27 min, waterscan ~27 min, perf 45–65 min, reviews) | licht per pakket, zwaar één keer per golf; aankleding via de afstemlus (20–40 min per ronde) |
| Drie stromen wachtten op een client-slot (2026-10-19) | in de container geen clients; max. 2 zware JVM's tegelijk (§4.3) |
| Verdeling vóór vorm: 5a beoordeelde de vorm te laat | 6-pre in twee momenten, kaarten direct na golf 0 (§2) |
| Determinisme-keten (75c): plaatsing hing af van de laadvolgorde | beslissingen in `Ring5Field` puur per cel en per wereld, nooit via een `flat_cache`; features lezen alleen accessors |
| Sparrings midden in een golf (flora, `verdant_falls`) schoven de planning | golf 1 begint met een korte vragenronde per stroom (zoals ring 4 "Besluiten sparring golf 1") voor blokken, meters en weer |
| Cloud-tijden zijn niet vergelijkbaar | prestatie in de container alleen relatief (§6.5) |

## 8. Herziene indeling van blok G (voorstel; zie `golf-0.md` V11)

Nummers 81–99 blijven, met nieuwe inhoud; extra's met lettercodes. Het oude pakket 93 (buitenrand afstemmen) gaat op in
84; de oude volgorde (blokken eerst, dan terrein) wordt de golfvolgorde van ring 4.

| # | Categorie | Pakket | Golf |
|---|---|---|---|
| 81 | `infra` | Contract-raamwerk ring 5: `ModBiomes` (6 + 2 Nether), `ModTags`, data map-type `healthy_block`, lang-helpers en lege haken per stroom; config-standaarden als V2/V7 dat vragen | 0 |
| 82 | `worldgen` | Ring 5-skelet: router en stubs, `Ring5Band`, `Ring5Field` + kasteelplekken, vloeistofregel ring 5 (+ naad 4\|5), `initial_terrain`, `ZoneFracture`-skelet, biomehaak (V12), scans | 0 |
| 81b | `infra` | `/ca castles` (lijst, `tp <1–4>`) | 0 |
| 83 | `worldgen` | Binnenband: dode grond, "net fout"-vormen, glitchvormen (`quantize`), losse scheuren | 1 |
| 84 | `worldgen` | Buitenband: uiteenvallen, brokken, bruggen, kasteelpinakels + bruggen, `void_scars`-scheuren tot de void, omgekeerde heuvels (vorm), buitenrand tot 10.200 (was 93) | 1 |
| 85 | `worldgen` | Breukzone 4→5 (`sundered_reach` herzien; landbruggen en afgrondzee weg, V7/V8) | 1 |
| 86 | `worldgen` | Biome-skelet ring 5 (6 biomes), kleuren, voorlopige surface, tags, lang; daarna 86b echte blokken in de surface | 1 |
| 87 | `worldgen` | Nether-5-terrein + `white_halls` + `hollow_cathedral` + vloeistofregel + ruimte-check | 1 |
| 88 | `blocks` | Corrupted varianten + tag `corrupted` + data map `healthy_block`; geen gedrag | 1 |
| 89 | `blocks` | Glitchblok (+ glitchscherf), voidsteen, nulliet-ertsen (+ `raw_nullite`) | 1 |
| 90 | `blocks` | Nether-5: kalkrack, witsteen-set, spiegelsteen, stil licht, witte kern | 1 |
| 91 | `conditions` | Void-meter (teleports alleen binnenband, darkness buitenband) | 1 |
| 92 | `atmosphere` | Ringweer ring 5: glitchstorm, omgekeerde regen, stilstand | 1 |
| 99a | `textures` | `Ring5Textures.java`: eerste versies ring 5 + Nether-5 | 1 |
| 93 | `worldgen` | Grotten (onmogelijke gangen, void-grotten, gekopieerde grotten) + gespiegelde dubbelgangers | 2 |
| 94 | `worldgen` | Aankleding binnenband + `false_meadow` + horror-features (ontbrekende chunks, tekens, kampen, herinneringen) | 2 |
| 95 | `worldgen` | Aankleding buitenband + kasteelomgeving + ertsen (nulliet, glitchblokken; vanilla afgebouwd) | 2 |
| 96 | `worldgen` | Nether-5-aankleding (zuilenrasters, witte kern, stil licht) | 2 |
| 97 | `conditions` | Stilte (Nether-5) + void-koppelingen met blokken | 2 |
| 98 | `atmosphere` | Sfeer ring 5 + Nether-5 (lucht, sterren, fog, glitch-fog, echo-voetstappen, geluid, stilte-client) | 2 |
| 99b | `textures` | Nieuwe blokken golf 2 + bijsturen | 2 |
| 99 | `textures` | Definitieve textures ring 5 + Nether-5 | 3 |

## 9. Voorgestelde doc-wijzigingen (door INT, na de antwoorden op de open punten)

**Stand (INT1 golf 1, 2026-10-23):** alles hieronder is gedaan (✅), in de docs-commit van INT1; per punt waar.

- **`fase-1-wereld.md` §3 blok G:** tabel vervangen door §8 hierboven (of de variant uit V11), met de kop "Herzien
  (sparring ring 5)": golven, teststops 6-pre/6a/6b/6c, verwijzing naar `docs/plans/ring-5/`. — ✅ blok G vervangen
  (kop "Herzien 2026-10-21 (sparring ring 5)", met golf, stand en teststops).
- **`fase-1-wereld.md` §1.3:** bij de zone 4→5: "breukzone (sinds de sparring ring 5), breedte <V7>". — ✅ (256, V7-A)
- **`fase-1-wereld.md` §1.8 Buitenrand:** de vorm komt uit de buitenband van ring 5 (84, was change 93); de oude
  zwevende-eilanden-invoer wordt `<V2>`; "Afstemming in change 93" vervalt. — ✅ (V2-A: vervalt in de overworld)
- **`fase-1-wereld.md` §1.6:** weertype `STANDSTILL` (stilstand) bij ring 5; oude changenummers 85 → 92. — ✅
- **`fase-1-wereld.md` §1.9/§1.11:** changenummers 81 (ertsen) → 95; condities 88 → 91, 97 → 97. — ✅ (§1.11; §1.9
  noemt geen nummers; ook §Geluid 90 → 98)
- **`fase-1-wereld.md` Ring 4, "Grens 4→5":** de landbruggen-alinea en de afgrondzee als "vervallen in ring 5 (85)"
  markeren; de breukzone beschrijven (uit V7); de gespleten rots volgens V8. — ✅
- **`fase-1-wereld.md` Ring 5:** de antwoorden in het blok "Sparring ring 5" (band, buitenband, onderkant, hoogtes,
  kastelen, bruggen, vloeistof, biome-indeling); "Biomes (5)" → 6; Nether-5 "Biome (1)" → 2 met de indeling uit V10. — ✅
  (ook het weer: stilstand en de gewichten per band)
- **`02-beslissingenlog.md`:** één regel per beantwoorde vraag (datum, `worldgen`/`infra`, besluit, reden). — ✅ V1–V12
  (2026-10-21); de vragenronde golf 1 stond er al.
- **`CODEBASE.md`:** sectie "Ring 5-haken per stroom (81)"; `Ring5Band`, `Ring5Field`, `ZoneFracture`, nieuwe DF-types,
  `/ca castles`; de valkuil "zonevorm ná de blend"; `Ring4Sundered` bijwerken of verwijderen na 85. — ✅ (golf 0 in
  `476199f7`; valkuil en `Ring4Sundered` in de docs-commit van INT1)
- **`docs/backlog.md`:** r. 259 (naad 4\|5) en r. 262 (zone 4→5 mengt ring 5-placeholder) afhandelen in 82/85;
  r. 396–397 (decor D, gespleten rots) volgens V8; r. 438 (breukzone) bij 85; r. 266 (placeholders) bij golf 3;
  nieuw: `[infra]` slot-aantal van `slots.sh` instelbaar per machine (`CA_MAX_SLOTS`), zodat de container op 2 kan. — ✅
  (259, 262, 438 afgevinkt; 396–397 naar 86c; 266 deels; `CA_MAX_SLOTS` nieuw)
- **`tools/test/README.md`:** de nieuwe bodemscan en de ring 5-gebiedsbestanden (`areas/ring5.txt`, `castles.txt`). — ✅
  (al in 81b/INT1: sectie "Voidscan (ring 5)")
- **`00-overzicht.md`:** geen wijziging nodig (de Nether-5-biomes staan in `fase-1-wereld.md`). — ✅ (niets gedaan)
- **Ook (INT1):** `contract.md` §4: de regel `ring_5/nether/region` volgt 87 (kathedraalmasker; `nether_ring_5_region`
  is de vlakte-ruis). — ✅
