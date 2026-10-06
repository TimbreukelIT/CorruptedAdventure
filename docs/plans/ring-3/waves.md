# Ring 3 — golven, stromen en integratieprotocol

Scope: ring 3 + Nether-ring 3 (bouwplan blok E). Werkwijze besloten in de sparring (2026-10-06, antwoorden 18–22): contract-first in golven, stromen parallel in eigen git-worktrees, één integratie-agent, drie teststops, handmatige orkestratie door de hoofdsessie (geen Workflow-tool). Namen en sleutels: `contract.md`.

## 1. Stromen

| Stroom | Categorie | Werkpakketten | Golf-1-plan | Bezit (gedeelde bestanden, zie §5) |
|---|---|---|---|---|
| **B** — blokken | `blocks` | 38, 38b, 40, 40b, 46, 51, 52 | `golf-1-blocks.md` | `ModBlocks`, `ModItems`, `ModTags` (inhoud), `block/*`, blok-datagen (states, models, loot, blok-/itemtags), `Ring3BlockNames` |
| **W-terrein** | `worldgen` | 39a, 39, 42 | `golf-1-terrain.md` | `density_function/ring_3/overworld/**`, `noise/ring_3_*`, de ring 3-sleutels in `ring_blend/overworld*`, `RingFluidPicker` (ring 3-regel), eigen DF-types |
| **W-biomes** | `worldgen` | 39b, 39c, 41, 41b, 47, 47b, 47c, 48, 49 | `golf-1-biomes.md` | `Ring3Biomes`, `Ring3Features`, `noise_settings/overworld.json` (ring 3-takken), preset: ring 3-overworld-source, `ModBiomeTagsProvider` (ring 3-helper), `Ring3BiomeNames` |
| **W-nether** | `worldgen` | 53, 53b, 54 | `golf-1-nether.md` | `density_function/ring_3/nether/**`, `noise/nether_ring_3_*`, `noise_settings/nether.json` (ring 3-tak), preset: Nether-ring 3-source |
| **C** — condities | `conditions` | 43, 44, 55 | `golf-1-conditions.md` | `condition/*` (hitte, kou, verdamping), `ServerConfig` `[conditions.heat/cold]`, `Ring3ConditionText` |
| **A** — sfeer | `atmosphere` | 45, 50, 56 | `golf-1-atmosphere.md` | `weather/*` (ring 3), `client/weather/*` (ring 3), `RingWeatherType`, `ServerConfig` `[weather.ring3]`, `ModSoundEvents`, `ModParticleTypes` (+ providers), `BiomeAmbience` (ring 3-builders), `tools/sounds/Ring3Sounds.java`, `Ring3WeatherText` |
| **T** — textures | `textures` | 57a, 57 | `golf-1-textures.md` | `tools/textures/Ring3Textures.java`, PNG's en `.mcmeta`'s onder `assets/corruptedadventure/textures/` met ring 3-/Nether-3-namen |
| **I** — infra | `infra` | 37c (golf 0), 37d | `change-37c.md`, `golf-1-infra.md` | `config/*`, `command/*` (debug), `datagen/lang/*` (raamwerk), `DataGenerators` |
| **INT** — integratie | (per commit de categorie van de inhoud; eigen commits `infra:`) | — | dit bestand §6 | `src/generated/**`, `CODEBASE.md`, `docs/design/*`, `docs/backlog.md`, `ModNetwork.PROTOCOL_VERSION` |

## 2. Golven

### Golf 0 — fundament (sequentieel, in de hoofdwerkmap)
1. **Open vragen V1–V8** (`contract.md` §9) laten beantwoorden en in het contract verwerken. Daarna staat het contract vast.
2. **37c** (`infra`, plan `docs/plans/change-37c.md`):
   - ringradii 6.000/7.800 in `CommonConfig`;
   - contract-sleutels als constanten (`ModBiomes`-keys, `ModTags`-TagKeys);
   - lang-raamwerk met lege helpers per stroom en de aanroepen in de providers;
   - `DataGenerators` klaar voor ring 3-bootstraps (lege bootstrap-klassen per stroom).
3. **39a** (`worldgen`, plan `docs/plans/change-39a.md`):
   - regio-assen ring 3 (overworld) + Nether-3 en hun noises;
   - router-sleutels in `ring_blend`;
   - stubs (constante 0) van alle gedeelde DF's uit `contract.md` §4;
   - Nether-3 `fluid_levels` in de preset.
   - De placeholder-biomes blijven tot 39b.
4. Commit per pakket; daarna start golf 1 vanaf deze commit (alle worktrees takken hier af).

### Golf 1 — vormen, blokken en systemen (parallel)

| Stroom | Pakketten | Af als | Afhankelijk van |
|---|---|---|---|
| B | 38, 38b, 40, 40b, 46, 51, 52 (in die volgorde, één commit per pakket) | alle contractblokken geregistreerd met gedrag, loot, tags; models verwijzen naar T's paden | golf 0; V2, V3, V7 |
| W-terrein | 39 | kegels, caldera's (actief/slapend), lavarivieren + vloeistofregel, zwevende eilanden, asvlaktes, zeekom, titanenveldvorm; surface tijdelijk vanilla (basalt, blackstone, magma); prestatie gemeten in de eigen worktree (indicatief) | golf 0; V4, V5, V8 |
| W-biomes | 39b | 7 (+ slapende krater, V8) biomes in de `multi_noise` van ring 3 op de regio-assen; kleuren, temperatuur, spawns (geiten V7); voorlopige surface met vanilla-blokken; tags `is_ring_3`, `heat_peaks`, `heat_refuges`; lang | golf 0 (stubs); V7, V8 |
| W-nether | 53 | Nether-3-terrein (bevroren lavazee, ijspilaren-vormen, warme scheuren als vorm), `ring_3/nether/terrain` in `ring_blend/nether`; surface tijdelijk vanilla (`packed_ice`, `blue_ice`, `basalt`) | golf 0 |
| C | 43, 44 | hitte-meter volgens contract §5 (incl. weer-modifiers asregen/uitbarsting) + water verdampt (alle routes); unit tests; S | golf 0 (tags/keys) |
| A | 45 | asregen, uitbarsting (rode lucht, gerommel, lavabommen), aswolk (nieuw type); hitte-factoren in de config (de modifiers bouwt C); particles `ash_flake`/`ember`; S + 2 clients | golf 0; V6 |
| T | 57a | eerste goede versies van alle ring 3- en Nether-3-textures + particle-sprites; contactblad + `VanillaCompare` | golf 0 (namen) |
| I | 37d | `/ca terrainmap` (PNG: hoogte, biome, doorsnede) | golf 0 |

**Integratie 1.**
- Merge-volgorde: I → B → T → W-terrein → W-biomes → W-nether → C → A.
- Daarna **39c** door W-biomes, op de geïntegreerde main: echte ring 3-blokken in de surface (as, puimsteen, smeulsteen, askorst, zwavelkorst, koudlava, Nether-ijs, rijp-netherrack). Dit is klein en sequentieel.
- Dan B/D/S, prestatie (JFR), determinisme en de docs.

**Teststop 4a — terrein-wow (BLOKKEREND).** Klaar moet zijn:
- terreinkaarten (PNG via 37d) van ring 3 en Nether-3, vóór de game aan de gebruiker voorgelegd;
- in-game, met een verse wereld, seed `20260930`:
  - de horizon-vulkanen vanuit `dry_riverbeds`;
  - de lucht die omslaat op de grens;
  - caldera's (actief en slapend), lavarivieren, zwevende eilanden, de vorm van het titanenveld;
  - de biomeverdeling en de echte blokken in de surface;
  - de ringbreedte (`/ca tpring 3`, `tpboundary 2|3`);
- een eerste blik op Nether-3 (vorm en ruimte);
- prestatie ≤ 1,75×.
- Nog niet nodig: features (bomen, skeletten, flora), sfeer, weer en hitte (die mogen al meedraaien, maar worden in 4b beoordeeld).

### Golf 2 — inrichting, overleving en sfeer (parallel)

| Stroom | Pakketten | Inhoud |
|---|---|---|
| W-biomes | 41, 41b, 47, 47b, 47c, 48, 49 | ertsen (+ ring 3-helft van `dry_riverbeds`, backlog r. 123), vuurkristal-geodes, fumarolen/zwavel/zeekom-koraal, `titan_fields` (skeletten, zielvuur, titanen-rotsen), smeulwoud + slapende krater + lavavallen + geiten, `steam_fens`, horror (afgietsels, voetsporen); `dry_riverbeds`-vlekken naar as/verkoold hout (backlog r. 122) |
| W-terrein | 42 | lavabuizen + magmakamers (floodedness/erosion ring 3, backlog r. 89), geode-holtes als DF (sjabloon dauwgeodes) |
| W-nether | 53b, 54 | Nether-3-biomes en aankleding (oerijs-pilaren, bevroren lavavallen, ijspegels, rijpzwammen, warme scheuren), `glacier_rifts` + ruimte-check |
| B | 46 (rest) | eigen geluiden aan fumarole/geiser/modder koppelen (sound ids van A), afstemming na 4a |
| C | 55 + afstemming | kou Nether-3; aswolk-modifier (×3); hitte afstemmen na 4a (refuge, schaduwregel) |
| A | 50, 56 | sfeer ring 3 en Nether-3 (kleuren, particles, ambient, camera-shake, gerommel), `Ring3Sounds.java`, blokgeluiden |
| T | 57a-rondes | textures bijsturen na 4a; nieuwe blokken uit golf 2 |

**Integratie 2** (zelfde protocol; determinismetest met twee werelden, omgekeerd geladen; feature-indexverschuiving in ring 1/2 meten).

**Teststop 4b — overleving + sfeer** (dedicated server + 2 clients). Klaar moet zijn:
- het hitte-ritme over een dag/nachtcyclus, inclusief bronnen, pieken, slapende krater, grotten en water;
- water verdampt (alle routes);
- de drie weertypes met bommen op beide clients;
- voedsel (geiten, flora);
- de levendigheid per biome (2 min lopen per biome);
- zielvuur en skeletten, gloeiende afgietsels 's nachts;
- lavabuizen als schuilroute;
- kou in Nether-3;
- sfeer en geluid.

### Golf 3 — afronding (parallel waar nodig)
- Fix-ronde op de feedback van 4b, per categorie (nieuwe lettercodes als het nodig is, bijv. 39d, 47d).
- **57**: definitieve texture-ronde.
- Backlog-opruiming ring 3.

**Teststop 4c — eindcheck** ring 3 + Nether-3, alle W-lijsten van de pakketten, standaard-W (elke ring en grens, portal per ring, ook overworld ring 3 → Nether-3), prestatie en determinisme.

## 3. Afhankelijkheden in één oogopslag
- Iedereen ← golf 0 (contract, constanten, stubs, lang-raamwerk).
- W-biomes golf 1 ← stub-DF's (39a). De echte waarden komen pas na integratie 1; tot dan mapt de biome-indeling op stubs (= alles één biome). Test 39b daarom ook met tijdelijke lokale ruis die niet gecommit wordt, of na rebase op W-terrein.
- 39c ← B + W-terrein + W-biomes geïntegreerd.
- C (hitte) ← tags/keys (37c); de inhoud van de tags komt van B/W-biomes. Een lege tag is geen fout: C test met een eigen testdatapack.
- A ← niets inhoudelijks van anderen in golf 1. Golf 2: blokgeluiden ↔ B.
- T ← alleen de namen uit het contract.
- Golf 2 W-biomes ← B (blokken voor features), T (uiterlijk), W-terrein (`height`, maskers).

## 4. Worktree-protocol (per stroom)

1. **Start:** Agent met `isolation: "worktree"` vanaf de golf-startcommit. Eerste actie: branch `ring3/<stroom>-g<golf>` aanmaken (`git switch -c …`), en het pad van de worktree plus de branch aan main melden.
2. **`run/` klaarzetten** (staat in `.gitignore`, wordt niet meegekopieerd):
   - `run/eula.txt` (`eula=true`);
   - een eigen `run/server.properties` met `level-type=corruptedadventure:corrupted_world`, `level-seed=20260930`, `server-port` = 25570 + n, `rcon.port` = 25580 + n (`enable-rcon=true`), eigen `level-name` per test.
   - Poortnummers per stroom: B 0, W-terrein 1, W-biomes 2, W-nether 3, C 4, A 5, T 6 (geen server), I 7. De eerste `./gradlew build` in een nieuwe worktree maakt `build/moddev` opnieuw aan (enkele minuten, ~2 GB schijf).
3. **JVM-slots:** max. 3 Gradle/Minecraft-JVM's tegelijk op de machine. Voor elke `build`/`runData`/`runServer`/`runClient` een slot nemen: atomisch `mkdir C:/Temp/ca-jvm-slot-{1,2,3}` (het eerste dat lukt is van jou), met een `owner`-bestand (stroom, PID, tijd); daarna weer verwijderen. Een slot met een owner die > 60 min oud is en waarvan de PID niet meer leeft, mag je opruimen.
4. **Alleen eigen processen stoppen:**
   - schrijf elke gestarte PID naar `build/<stroom>.pids`;
   - stop een server met RCON `stop`, niet met `tools/server-smoke-test.sh` (dat doodt elke `serverRun`-JVM);
   - nooit breed `taskkill`/`pkill`.
5. **Lock voor gedeelde zaken:** `C:/Temp/ca-test.lock` (atomisch `mkdir`) voor alles wat machinebreed is: ffmpeg-runs, `runClient` met quick-play naar localhost en JFR-metingen. Indicatieve metingen in de eigen worktree zijn toegestaan, maar de officiële meting doet de integratie op een rustige machine.
6. **Bouwen:** alleen binnen de eigen pakketten en de eigen bestanden (§1, §5). Commit per pakket met categorie-prefix, gestaged per pad (geen `git add -A`). Nooit `docs/prompts/` of `DewGeodeShapeTest.java` meenemen. Trailer alleen `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
7. **Gegenereerde output:** `runData` mag lokaal (test D). Commit de output in de eigen branch, zodat de branch op zichzelf groen is. Bij de merge regenereert de integratie alles; conflicten in `src/generated/**` worden nooit met de hand opgelost.
8. **Docs:** niet in `CODEBASE.md`, `docs/design/*` of `docs/backlog.md` schrijven. Onderaan het eigen plan staan drie secties:
   - "CODEBASE-delta" (nieuwe of gewijzigde classes, registries en databestanden, in de stijl van `CODEBASE.md`);
   - "Beslissingen" (regels voor de beslissingenlog);
   - "Backlog" (regels met categorie).
9. **Review vóór commit:** `/code-review` (skill `code-review`, args `medium`) op de niet-gecommitte diff van het pakket. Bevindingen oplossen of vastleggen, daarna pas committen. Geen commit voordat de review binnen is.
10. **Liveness:**
    - na elke afgeronde stap een regel `Voortgang: <tijd> <stap>` in het eigen plan (en committen met het pakket);
    - main controleert elke ~15 min de branch (`git -C <worktree> log`), de mtimes en de agentstatus;
    - een stroom zonder voortgang > 30 min wordt aangesproken; een dode stroom wordt opnieuw gestart vanaf de laatste voortgangsregel.
11. **Vragen:** een nieuwe ontwerpkeuze → stop dat deel en stuur de vraag naar main (2–4 opties, aanbevolen eerst). Implementatiedetails beslist de stroom zelf en noteert ze. Contractwijzigingen alleen via main.
12. **Eindrapport aan main:** branch, laatste hash, pakketten, testresultaten (B/D/S + metingen), review-uitkomst, afwijkingen, W-checklist (kort), en de delta-secties.

## 5. Eigenaarschap van bestanden die meerdere stromen raken

| Bestand | Regel |
|---|---|
| `ModConfiguredFeatures`, `ModPlacedFeatures`, `ModFeatures`, `ModDensityFunctionTypes`, `ModPlacementModifierTypes`, `ModSurfaceConditions` | Alleen **toevoegen**, in een eigen blok met kopcommentaar `// ring 3 — <stroom>` aan het eind van de lijst; geen bestaande regels wijzigen. De integratie houdt beide kanten bij een merge. |
| preset `corrupted_world.json` | W-biomes: overworld ring 3-source en `transitions.ring_3`; W-nether: Nether ring 3-source; 39a: `fluid_levels.ring_3`. Elk alleen de eigen sleutel. |
| `ServerConfig` | C: `[conditions.heat]`, `[conditions.cold]`; A: `[weather.ring3]`. Elk een eigen methode/blok. |
| `ModTags` | 37c zet alle contract-TagKeys; daarna alleen B (blok/item) en de worldgen-stromen (biome-tags) via main. |
| lang | elke stroom alleen de eigen helper (37c). |
| `RingWeatherType` | A. |
| `ModNetwork.PROTOCOL_VERSION` | INT, één verhoging per golf als er een payload bij komt. |
| `src/generated/**`, `CODEBASE.md`, `docs/design/*`, `02-beslissingenlog.md`, `docs/backlog.md` | INT. |

## 6. Integratieprotocol (INT, per golf)

1. Wacht tot alle stromen van de golf "klaar" melden (branch + hash + review gedaan). Een stroom die niet af is, blijft staan voor de volgende golf, tenzij een andere stroom ervan afhangt (dan wachten).
2. In de hoofdwerkmap (schone `git status` op de afgesproken uitzonderingen na): `git merge --no-ff ring3/<stroom>-g<golf>` in de vaste volgorde (§2).
3. Conflicten:
   - `src/generated/**` → neem een van beide kanten en regenereer;
   - append-blokken (§5) → beide houden;
   - al het andere → terug naar de stroom (vraag via main), niet zelf ontwerpen.
4. `./gradlew build` (inclusief unit tests en `ClientIsolationTest`), `runData` en de geregenereerde output committen (`infra: integrate ring 3 wave <n>`).
5. **S:** server + 2 clients (quick-play), verse wereld `ring3-wave<n>`. **W-smoke:** `/ca tpring 3`, `tpring 3 nether`, `tpboundary 2|3`, portal ring 3 ↔ Nether-3.
6. **Determinisme:** twee verse werelden, de tweede omgekeerd geladen; eigen blokken identiek (methode change 35). **Feature-index:** ring 1/2 vergelijken op biomes en terreinvorm (valkuil `CODEBASE.md`).
7. **Prestatie:** JFR `minecraft.ChunkGeneration`, stap `noise`, 16×16 chunks, 5 rondes; meetpunten in elke ring 3-biome en in Nether-3; tegen vanilla en tegen de vorige integratie. Budget ≤ 1,75×.
8. **Docs samenvoegen:**
   - de "CODEBASE-delta" van elke stroom → `CODEBASE.md` (kop, status, classes, registries, databestanden, valkuilen);
   - "Beslissingen" → `02-beslissingenlog.md` en `fase-1-wereld.md`;
   - "Backlog" → `docs/backlog.md`.
9. `/code-review` (medium) op de integratiediff (conflictoplossingen, geregenereerde output en docs; de stroom-diffs zijn al gereviewd). Bevindingen oplossen, dan committen.
10. Teststop-checklist samenstellen uit de W-lijsten van de stromen (gegroepeerd per onderwerp, 1–2 sessies, met afstemvragen) in `docs/plans/ring-3/teststop-4<x>.md`.
11. Worktrees opruimen (`git worktree remove`) als de branch gemerged is. Branches blijven tot de teststop akkoord is.

## 7. Lessen uit 35/36/37, hier verwerkt

| Les | Waar |
|---|---|
| Twee servers in één `run/` breken elkaar ("another process has locked a portion of the file") | eigen worktree + eigen poorten (§4.2) |
| `server-smoke-test.sh` doodt elke `serverRun`-JVM | RCON `stop`, PID-bestand (§4.4) |
| Gemengde gegenereerde bestanden vroegen `git update-index --cacheinfo` | eigen branch + regenereren bij de merge (§4.7, §6.3) |
| Een agent stierf stil (build-35) | voortgangsregels + liveness-check (§4.10) |
| 36/37 committen vóór de review binnen was | review vóór commit, harde regel (§4.9) |
| `C:/Temp/ca-test.lock` voor gedeelde testmiddelen | blijft voor machinebrede zaken (§4.5) |
| Prestatie meten tijdens parallel bouwen geeft ruis (±20 % tussen sessies) | officiële meting alleen in INT (§6.7) |
| Feature-indexverschuiving verandert decoratie in ring 1/2 | één eigenaar per dimensie voor features + meting in INT (§6.6) |
