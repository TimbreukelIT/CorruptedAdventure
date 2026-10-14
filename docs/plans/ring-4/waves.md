# Ring 4 — golven, stromen en integratieprotocol

Scope: ring 4 + Nether-ring 4 (bouwplan blok F, `fase-1-wereld.md` §3). Ontwerp: `fase-1-wereld.md` → Ring 4 ("Sparring ring 4 (2026-10-10)"). Werkwijze: zoals ring 3 (`docs/plans/ring-3/waves.md`): contract-first in golven, stromen parallel in eigen git-worktrees, één integratie, handmatige orkestratie door de hoofdsessie. **Vanaf golf 1 met het snelle testritme en de `tools/test/`-tooling van ring 3 golf 3** (`docs/plans/ring-3/waves.md` §4 "Golf 3 — sneller testen" en §6 "Golf 3 — snel protocol"). Namen en sleutels: `contract.md`.

**Start:** pas na teststop 4c (ring 3) en een sparring-check van het golf 0-plan met de gebruiker. Punten die nog open zijn staan in `contract.md` §9 en worden in het golf 0-plan beslist (vragen aan de gebruiker, niet zelf kiezen).

## 1. Stromen

| Stroom | Categorie | Pakketten | Bezit (gedeelde bestanden, zie §5) |
|---|---|---|---|
| **B** — blokken | `blocks` | 66, 67, 68, 72 | `ModBlocks`, `ModItems`, `ModTags` (inhoud), `block/*`, blok-datagen (states, models, loot, blok-/itemtags), `Ring4BlockNames` |
| **W-terrein** | `worldgen` | 59 (golf 0), 60, 61, 62, 63, 73, 74 | `density_function/ring_4/overworld/**`, `noise/ring_4_*`, de ring 4-sleutels in `ring_blend/overworld*`, `RingFluidPicker`/`RingAquifer` (ring 4-regel), `IslandField` en eigen DF-types, de hoogte-biomehaak in `RingBiomeSource` (59) |
| **W-biomes** | `worldgen` | 64, 75, 76, 77 | `Ring4Biomes`, `Ring4Features`, `noise_settings/overworld.json` (ring 4-takken), preset: ring 4-overworld-source en `transitions.ring_4`, `ModBiomeTagsProvider` (ring 4-helper), `Ring4BiomeNames` |
| **W-nether** | `worldgen` | 65, 78 | `density_function/ring_4/nether/**`, `noise/nether_ring_4_*`, `noise_settings/nether.json` (ring 4-tak), preset: Nether-ring 4-source en `fluid_levels.ring_4` |
| **C** — condities | `conditions` | 69, 79 | `condition/*` (gif, zuiverend), `ServerConfig` `[conditions.poison]` (+ zuiverend), `Ring4ConditionText` |
| **A** — sfeer | `atmosphere` | 70, 80 | `weather/*` (ring 4), `client/weather/*` (ring 4), `RingWeatherType`, `ServerConfig` `[weather.ring4]`, `ModSoundEvents`, `ModParticleTypes` (+ providers), `BiomeAmbience` (ring 4-builders), het ring 4-geluidsscript, `Ring4WeatherText` |
| **T** — textures | `textures` | 71a, 71b, 71 | `tools/textures/Ring4Textures.java`, PNG's en `.mcmeta`'s met ring 4-/Nether-4-namen |
| **I** — infra | `infra` | 58 (golf 0) | `config/*`, `command/*` (debug), `datagen/lang/*` (raamwerk), `DataGenerators` |
| **INT** — integratie | (per commit de categorie van de inhoud; eigen commits `infra:`) | — | `src/generated/**`, `CODEBASE.md`, `docs/design/*`, `docs/backlog.md`, `ModNetwork.PROTOCOL_VERSION` |

## 2. Golven

### Golf 0 — fundament (sequentieel, in de hoofdwerkmap)
1. **Golf 0-plan** met de open punten uit `contract.md` §9 aan de gebruiker voorleggen (sparring-check); antwoorden in het contract verwerken. Daarna staat het contract vast.
2. **58** (`infra`):
   - standaard-ringradii 1.500 / 3.500 / 6.000 / 8.000 / 9.700, void-rand 10.200 in `CommonConfig` (alleen nieuwe werelden; Nether ÷4);
   - zo nodig de bredere zone 4→5 (`contract.md` §1, open);
   - contract-sleutels als constanten (`ModBiomes`-keys, `ModTags`-TagKeys);
   - lang-raamwerk met lege helpers per stroom en de aanroepen in de providers; lege bootstrap-klassen per stroom.
3. **59** (`worldgen`):
   - `ring_blend`-invoer ring 4 + stubs (constante 0) van alle gedeelde DF's uit `contract.md` §4;
   - band-as en klimaatassen (+ router-sleutels);
   - vloeistofregel ring 4 in `RingFluidPicker`/`RingAquifer` (water, geen lava, water-tak voor vallen);
   - echte `initial_terrain` voor ring 4;
   - hoogte-biomehaak in `RingBiomeSource`;
   - `IslandField`-skelet (hergebruikt `Column`/`ColumnTable`/`VolcanicCaches`, plaatsing zonder `flat_cache`).
   - De placeholder-biomes blijven tot 64.
4. Commit per pakket; golf 1 takt af van deze commit.

**Teststop 5-pre — terreinpreview** (ná golf 0, of vroeg in golf 1 zodra 60/61 een eerste vorm hebben): `/ca terrainmap` van ring 4 (eilanden en gaten, band-as, zone 4→5) + een korte vliegtocht in een verse wereld. Doel: de vorm en dichtheid van de eilanden vroeg bijsturen. Niet blokkerend voor B, C, A, T.

### Golf 1 — vormen, blokken en systemen (parallel)

| Stroom | Pakketten | Af als | Afhankelijk van |
|---|---|---|---|
| W-terrein | 60, 61, 62, 63 | basisterrein (laag, terrassen, kliffen, rivieren), eilanden (dichtheid/hoogte/grootte naar buiten, spiegelgaten, kluiten, watervallen, liftposities), klif 3→4, `sundered_reach`; surface tijdelijk vanilla; prestatie indicatief | golf 0 |
| W-biomes | 64 | 8 grondbiomes + 2 hoogte-biomes + `sundered_reach` op band- en klimaatassen; kleuren en waterkleuren; voorlopige surface met vanilla-blokken; spawns (vanilla-dieren per biome); tags; lang | golf 0 (stubs) |
| W-nether | 65 | Nether-4-terrein (lavazee diep, bottenvlaktes, kristalzuilen, geode-holtes), 3 biomes, ruimte-check; trede 3\|4 afgedekt | golf 0 |
| B | 66, 67, 68 (in die volgorde, één commit per pakket) | alle contractblokken geregistreerd met gedrag, loot, tags; models verwijzen naar T's paden | golf 0 |
| C | 69 | gif-meter volgens contract §5 (incl. de gifregen-modifier); unit tests | golf 0 (tags/keys) |
| A | 70 | dikke mist (+ hook fase 4), stormwind (+ duw), gifregen; config `[weather.ring4]` | golf 0 |
| T | 71a | eerste goede versies van alle ring 4- en Nether-4-textures + particle-sprites; contactblad | golf 0 (namen) |

**Integratie 1** (snel protocol, §6). Merge-volgorde: B → T → W-terrein → W-biomes → W-nether → C → A. Daarna, klein en sequentieel op de geïntegreerde main: de echte ring 4-blokken in de surface (W-biomes, zoals 39c bij ring 3; lettercode in het golf 1-plan).

**Teststop 5a — terrein-wow (BLOKKEREND).** Klaar moet zijn:
- terreinkaarten (PNG via `/ca terrainmap`) van ring 4 en Nether-4, vóór de game aan de gebruiker voorgelegd;
- in-game, verse wereld, seed `20260930`:
  - de klif 3→4 met `steam_fens` erbovenop en de watervallen;
  - de eilanden met hun gatmeren, van de binnenrand tot `sundered_reach`;
  - het uiteenvallen bij 4→5 (afgrond, water onderin, landbruggen);
  - de biomeverdeling over de twee banden, de hoogte-biomes en de echte blokken in de surface;
  - de ringbreedte (`/ca tpring 4`, `tpboundary 3|4`);
- een eerste blik op Nether-4 (vorm en ruimte);
- prestatie ≤ 1,75×.
- Nog niet nodig: aankleding, tunnels, wereldboom, sfeer, weer en gif (die mogen al meedraaien, maar worden in 5b beoordeeld).

### Golf 2 — inrichting, overleving en sfeer (parallel)

| Stroom | Pakketten | Inhoud |
|---|---|---|
| B | 72 | moerasgas (ketting-ontploffing, cap per tick), zuigmodder, bellenbron, opwaartse stroom, gifpoel, klauwkras/slijmspoor/nest |
| W-terrein | 73, 74 | tunnelnet (deels overstroomd, luchtkamers, ingangen via gatmeren en zinkgaten) + gaskamers en gifpoelen ondergronds; wereldboom |
| W-biomes | 75, 76, 77 | aankleding mythische band + `sky_gardens` + gloed op de grond onder eilanden (besluit teststop 5a); aankleding bedorven band + `withered_gardens` + gifpoelen; ertsen, sporen van wezens, dieren |
| W-nether | 78 | zeewezen-fossielen, kristalzuilen, geodes |
| C | 79 | zuiverend Nether-4 + gif-koppelingen (blokken van 72) |
| A | 80 | sfeer ring 4, zone 4→5 en Nether-4 (kleuren, particles, geluid, spanning, pulserende fog) |
| T | 71b | bijsturen na 5a; nieuwe blokken uit golf 2 |

**Integratie 2** (snel protocol; determinisme omgekeerd geladen; feature-index ring 1–3).

**Teststop 5b — overleving + sfeer** (dedicated server + 2 clients). Klaar moet zijn:
- gif: de bronnen (gifpoel, moerasgas, gifregen), drempels, melk en honing; water veilig;
- moerasgas ontsteken in een grote kamer (geen lag-spike), zuigmodder, opwaartse stroom (lift) op beide clients;
- de drie weertypes, de stormduw boven Y ~120;
- het tunnelnet (duiken, luchtkamers, sporen), de wereldboom;
- de levendigheid per biome (2 min lopen per biome), dieren, de nachtgloed;
- spanning (dwaallichten, geritsel, bellen, silhouet) en de lege zone 4→5;
- zuiverend in Nether-4, sfeer en geluid.

### Golf 3 — afronding (parallel waar nodig)
- Fix-ronde op de feedback van 5b, per categorie (nieuwe lettercodes waar nodig).
- **71**: definitieve texture-ronde.
- Backlog-opruiming ring 4.

**Teststop 5c — eindcheck** ring 4 + Nether-4: alle W-lijsten van de pakketten, standaard-W (elke ring en grens, portal per ring, ook overworld ring 4 → Nether-4), prestatie en determinisme.

## 2b. Implementatiekeuzes (niet door de gebruiker besloten; zoals ring 3)
- **58 is `infra` maar zet ook worldgen-/blocks-artefacten neer** (keys, tag-keys, lege bootstrap- en helperklassen per stroom; geen inhoud): het contractraamwerk van golf 0, zoals 37c.
- **De gif-modifier van de gifregen bouwt C, niet A** (zoals de hitte-modifiers van ring 3, `docs/plans/ring-3/waves.md` §2b): A levert alleen de factor in `[weather.ring4]`, zodat A en C in golf 1 niet op elkaar wachten.

## 3. Afhankelijkheden in één oogopslag
- Iedereen ← golf 0 (contract, constanten, stubs, lang-raamwerk).
- W-biomes golf 1 ← stub-DF's (59). De echte assen komen pas na integratie 1; test 64 daarom ook met tijdelijke lokale ruis die niet gecommit wordt, of na een rebase op W-terrein.
- De surface met echte blokken ← B + W-terrein + W-biomes geïntegreerd.
- C (gif) ← tags/keys (58); de inhoud van de tags komt van B/W-biomes; de gifpoel- en gasblokken van B (72) koppelt C in golf 2 (79). Een lege tag is geen fout: C test met een eigen testdatapack.
- A ← niets inhoudelijks van anderen in golf 1. Golf 2: blokgeluiden ↔ B.
- T ← alleen de namen uit het contract.
- Golf 2 W-biomes ← B (blokken voor features), T (uiterlijk), W-terrein (`height`, maskers, `IslandField`-accessors).
- W-terrein 73 (gaskamers, gifpoelen ondergronds) ← B 72 (gas- en gifpoelblok); tot de merge met een vanilla-placeholder of na een vooruit-merge van B.

## 4. Worktree-protocol
Zoals `docs/plans/ring-3/waves.md` §4 (stappen 1–12) **inclusief "Golf 3 — sneller testen"** (`tools/test/` voor lock/slots, server, RCON, client-screenshots, offline scans en prestatie; licht per pakket, zwaar één keer; het speel-lock `C:/Temp/ca-user-test.lock`; nooit wachten op een melding; main merget vooruit; echte klok). Verschillen voor ring 4:
- Branches `ring4/<stroom>-g<golf>`.
- Poortnummers per stroom gelijk aan ring 3 (B 0, W-terrein 1, W-biomes 2, W-nether 3, C 4, A 5, T 6 geen server, I 7; server-port 25570 + n, rcon 25580 + n).
- Docs: alleen het eigen plan in `docs/plans/ring-4/` (met de secties "CODEBASE-delta", "Beslissingen", "Backlog" onderaan); niet in `CODEBASE.md`, `docs/design/*` of `docs/backlog.md`.
- De offline scans van ring 3 (lava tegen lucht trede/val/lek) gelden voor ring 4 als "geen lava" (ring 4 heeft geen lava) en, als het golf 0-plan dat vraagt, een water-scan voor water tegen lucht buiten de vastgelegde vallen.

## 5. Eigenaarschap van bestanden die meerdere stromen raken

| Bestand | Regel |
|---|---|
| `ModConfiguredFeatures`, `ModPlacedFeatures`, `ModFeatures`, `ModDensityFunctionTypes`, `ModPlacementModifierTypes`, `ModSurfaceConditions` | Alleen **toevoegen**, in een eigen blok met kopcommentaar `// ring 4 — <stroom>` aan het eind; geen bestaande regels wijzigen. De integratie houdt beide kanten. |
| preset `corrupted_world.json` | W-biomes: overworld ring 4-source en `transitions.ring_4`; W-nether: Nether ring 4-source en `fluid_levels.ring_4`. Elk alleen de eigen sleutel. |
| `ring_blend/overworld_temperature`/`_vegetation`/`_weirdness` | 59 zet de ring 4-sleutels; wie `transitions.ring_4` toevoegt (64) haalt `ring_4` in dezelfde change uit `switch_at` (harde regel ring 3, `docs/plans/ring-3/contract.md` §4). |
| `RingBiomeSource` (hoogte-biomehaak) | 59 (W-terrein); daarna alleen via main. |
| `RingFluidPicker`/`RingAquifer` | W-terrein (ring 4-regel); de ring 3-regels niet raken. |
| `ServerConfig` | C: `[conditions.poison]` (+ zuiverend); A: `[weather.ring4]`. Elk een eigen methode/blok. |
| `ModTags` | 58 zet alle contract-TagKeys; daarna alleen B (blok/item) en de worldgen-stromen (biome-tags) via main. |
| lang | elke stroom alleen de eigen helper (58). |
| `RingWeatherType` | A. |
| `ModNetwork.PROTOCOL_VERSION` | INT, één verhoging per golf als er een payload bij komt. |
| `src/generated/**`, `CODEBASE.md`, `docs/design/*`, `02-beslissingenlog.md`, `docs/backlog.md` | INT. |

## 6. Integratieprotocol (INT, per golf)
Het **snelle protocol** van ring 3 golf 3 (`docs/plans/ring-3/waves.md` §6, stappen 1–11 en "Golf 3 — snel protocol") geldt voor alle ring 4-golven:
- merge in de vaste volgorde (§2), B/D en het vangnet `git diff --exit-code src/generated` na `runData`;
- S-smoke (server + 1 client, verse wereld `ring4-wave<n>`), W-smoke: `/ca tpring 4`, `tpring 4 nether`, `tpboundary 3|4`, portal ring 4 ↔ Nether-4;
- offline checks met `tools/test/`: determinisme omgekeerd geladen, feature-index ring 1–3, de lava-/waterscan van §4;
- review (`/code-review` medium) op de integratiediff, dan **direct naar main**; de gebruiker test vanuit de hoofdwerkmap;
- prestatie daarna en alleen als een pakket noise of surface raakt: 3 warme + 1 koude ronde, meetpunten in elke ring 4-biome, een eilanddicht gebied in de bedorven band, `sundered_reach` en Nether-4, harde stop na ~45 min, als de gebruiker niet speelt. Budget ≤ 1,75×;
- docs (CODEBASE-delta, beslissingen, backlog) en de teststop-checklist (`docs/plans/ring-4/teststop-5<x>.md`) mogen na de merge.

## 7. Lessen uit ring 3, hier verwerkt

| Les | Waar |
|---|---|
| Testen per pakket kost te veel tijd en machine | licht per pakket, zwaar één keer (§4, §6) |
| Eilanden van ring 3 botsten met de kolom-caps | eigen `IslandField` met eigen raster vanaf 59; terreinpreview 5-pre |
| Lava tegen lucht kostte een hele afrondingsronde | ring 4 heeft geen lava; vallen worden vanaf het begin vastgelegd (water-tak) en apart geteld |
| Biomes testen op stubs = één biome | lokale ruis of rebase op W-terrein (§3) |
| Overgangsbiome vergeten in `switch_at` | regel in §5 en de test van ring 3 |
