# Change 25 — Ores ring 2: zonnesteen, turkoois, afgebouwde vanilla-ores

Scope: `worldgen` — change 25

**Doel:** ring 2 krijgt zijn eigen ertsverdeling, deterministisch per seed:
- **zonnesteen** (hoofdmateriaal): gloeiend erts **hoog in de canyonwanden**, zichtbaar vanaf de kloofbodem;
- **turkoois**: **zeldzaam, diep in de kloofsteen**;
- **vanilla-ores afgebouwd**: ijzer en kolen minder dan in ring 1, diamant zeldzaam. Inclusief de grote ertsaders (ore veins) uit de noise router.

Puur data (datagen + handgeschreven worldgen-JSON) plus één kleine gedeelde datagen-helper; geen nieuw feature- of placement-type. Het afbouwmechanisme is herbruikbaar voor ring 3–5 (changes 41, 61, 81).

Bron: `fase-1-wereld.md` §1.11 (ores algemeen: naar buiten afbouwen, ringores nemen over), §2 Ring 2 "Ores", bouwplan rij 25 ("B, W: verdeling klopt per diepte"); beslissingenlog 2026-09-30 (vanilla-ores afbouwen; ring 2-ores).

---

## Afstemming (2026-10-01)

Afgestemd met 21, 23, 24, 26, 30 en 31 (basis: de aanbevolen opties). De aannames A2–A4, de doelen in stap 3, de Y-bereiken in vraag 4 en 6 en de tellingen zijn inline bijgewerkt.
- **Ertsblokken per gastgesteente (24):** zonnesteen in de drie tinten + `scorched_sandstone`, turkoois in de drie tinten; per gastgesteente een `BlockMatchTest` in één `minecraft:ore`. Zonnesteen komt zo ook in de hoodoo-kaprotsen (30) en de `scorched_sandstone`-hellingen van `dry_riverbeds` (31).
- **Hoogtes uit 23 (23 vraag 5 = B, keuze gebruiker: diep):** plateau Y 125–140, kloofbodem ~70 (kloven 55–70 diep), vlaktes Y 66–74, kloofsteen Y 8 → oppervlak. Startwaarden: zonnesteen `trapezoid` Y 95–150 (vol 110–140; bovenste helft van de wand Y ~70–140, het trapezium schuift mee: vol vanaf ~57 % van de wandhoogte tot de plateaurand, aanloop 15 blokken eronder, 10 boven de hoogste plateaurand zoals voorheen), turkoois `uniform` Y 10–40 (blijft: onderste ~30 blokken kloofsteen). **Bijgewerkt 2026-10-01 (23b, tafelbergen tot Y ~178):** zonnesteen `trapezoid` Y 95–180, `plateau` 60 (vol ~108–168), 14 aders × 8; zie "Afstemming na teststop 2".
- **Bestaande bouwstenen:** `is_ring_2` en `Ring2Biomes.ALL` (23) hergebruiken; `Ring2Features` (23) **uitbreiden** (niet opnieuw aanmaken); `Ring2BiomeModifiers` maakt deze change. Het ertsenslot van `Ring2Biomes.generation` (23) wordt hier vervangen.
- **Ring 1:** binnen ring 1 blijft alles blok voor blok gelijk (baseline-check). `vein_toggle` via `ring_blend` (vraag 3 A) laat de ore veins in de overgangszone 1→2 geleidelijk uitlopen, ook in de ring 1-helft van `withered_expanse`. **Besloten 2026-10-01 (gebruiker, punt B2):** geaccepteerd, samen met de floodedness van change 26.
- Teststop: geen eigen stop (alles via RCON). **Bijgewerkt 2026-10-01:** niet meer wachten op een akkoord van teststop 2; start na de commit van change 23c. De in-game check (W-achteraf hieronder) gaat mee in **teststop 2b na change 30** (gebundelde terreincheck ring 2, blokkerend vóór 31; keuze gebruiker 23b-17).
- **Drops (24 vraag 3 = B):** de ertsen droppen `raw_sunstone`/`raw_turquoise`; deze change telt alleen ertsblokken (`#c:ores/*`), dus de meting verandert niet.

## Afstemming na teststop 2 (2026-10-01)

Afgestemd met de plannen 23b en 23c (antwoorden 2026-10-01). Waar de tekst hieronder iets anders zegt, geldt dit blok; de belangrijkste plekken zijn inline gecorrigeerd.
- **Terrein na 23b:** tafelbergen bovenop het plateau (toppen Y ~145–178, wanden Y ~132–178), spleetkloven (wanden Y ~90–140), richels en nissen in de kloofwanden, rotsvormen (naalden, keien, vinnen) en treden/kolken in de kloofbodem. Meer kloofsteenvolume per chunk.
- **Zonnesteen opgerekt tot ~Y 180 (besloten 2026-10-01),** zodat ook de tafelbergwanden zonnesteen hebben. Doorgerekend:
  - `trapezoid` **Y 95–180, `plateau` 60** → vol ~Y 108–168, aanloop ~12–13 blokken aan beide kanten. Let op: vanilla's `trapezoid` legt het vlakke deel **in het midden** van [min, max]; de oude omschrijving "vol 110–140, aanloop 15 onder en 10 boven" kon vanilla niet exact (Y 95–150 met `plateau` 30 gaf vol ~108–138). De ondergrens 95 en de aanloop blijven dus gelijk; alleen de bovenkant en het vlakke deel groeien.
  - **Aantal (startwaarde):** het verticale bereik groeit van 42,5 naar 72,5 "volle" blokken (×1,7), en aders boven de plateaurand (Y ~140–180) vallen buiten de tafelbergen in de lucht (geen erts). Om de dichtheid in de kloofwanden ongeveer gelijk te houden: **14 aders per chunk** (was 8), grootte 8. Het doel blijft "≈ ring 1-ijzer per `canyonlands`-chunk"; afstemmen op de meting (meer kloofsteenvolume = meer treffers).
  - Hoodoos (30, vloer Y ~88, toppen tot ~135) en spleetwanden blijven volledig binnen het bereik.
- **Meer crème (23b-13 B):** rood 22 / oranje 12 / crème 14 per 48. Meer `cream_canyon_*_ore` in de telling; de normalisatie per gastgesteente (`#canyon_stones` totaal) vangt het op. Per tint apart rapporteren.
- **Keien en puin van 23c** zijn dezelfde kloofsteentinten boven het oppervlak (`LOCAL_MODIFICATIONS`, vóór `UNDERGROUND_ORES`): een `minecraft:ore` kan er zonnesteen van maken ("glinsterend puin", onschuldig). In de telling per gastgesteente meenemen en apart melden hoeveel ertsblokken boven `WORLD_SURFACE_WG` liggen.
- **Testplekken "hoog in de wanden"** kunnen nu ook richels, nissen en tafelbergwanden zijn.
- **Teststop:** zie hierboven (teststop 2b na 30).

---

## Afhankelijkheden en aannames

**Gecontroleerd in de code (stand na change 17, commit `7714b77`):**

- **Ring 1 bouwt vanilla-ores niet af.** De 52 kopieën hebben vanilla's eigen ore-features 1-op-1 (`VanillaBiomeCopies` draait `BiomeData.bootstrap`); `withered_expanse` (zone 1→2) roept zelf `BiomeDefaultFeatures.addDefaultOres` aan (`TransitionBiomes` r. 44), dus ook volle vanilla-ores. Er bestaat dus **nog geen** afbouwmechanisme; deze change maakt het eerste, en wel op basis van wat ring 1 al deelt:
  - **`ring_chance`** (`RingChanceFilter`, `worldgen/feature/`): placement filter met signaal `progress` (0 op de binnengrens van `ring` → 1 op de buitengrens, **geen signaal = kans 0 buiten die ring**) of `transition`. Trekking `random.nextFloat() < kans` met vanilla's geseede decoration-random, per positie. Na een `count` geplaatst dunt hij dus elke poging los uit: verwachte hoeveelheid × kans. `from_chance == to_chance` geeft een vaste kans (codec staat dat toe; `start` 0, `exponent` 1). `RingLayout.current()` alleen in `shouldPlace`. Factory `RingChanceFilter.progress(ring, from, to, start, exponent)`.
  - **Biome modifiers** (`Ring1BiomeModifiers`, NeoForge `add_features` op een eigen biome-tag, achteraan de stap) — het patroon dat change 16 expliciet voor de ringores van ring 2–5 (changes 25, 41, 61, 81) bedoelde.
  - **Ore-feature zoals `NetherRing1Features`**: één `minecraft:ore` met per gastgesteente een doel (`BlockMatchTest`/`TagMatchTest` → erts-variant), absolute Y-ankers, `discard_chance_on_air_exposure` 0, placement `count`/`rarity` → `in_square` → `height_range` → `biome`. Volgorde-onafhankelijk (valkuil "Generatievolgorde" in `CODEBASE.md`: `minecraft:ore` met alleen vervangbare doelblokken = unie van de aders).
  - **`ring_blend`** (`RingBlend`): density function per ring met smoothstep in de overgangszone; `rings` gebruikt `DensityFunction.HOLDER_HELPER_CODEC` (verwijzing, inline functie of getal).
- **Ore veins** (grote ijzer-/koperaders, `OreVeinifier`) komen niet uit biome-features maar uit de noise router: `noise_settings/overworld.json` heeft `ore_veins_enabled: true` en `vein_toggle` inline (r. 120–135: `interpolated(range_choice(y, −60…51, noise ore_veininess ×1,5))`). Vanilla maakt geen ader waar `|vein_toggle| + randafslag < 0,4`; `vein_toggle` = 0 → geen aders. Zonder ingreep heeft ring 2 dus vanilla-aders.
- Vanilla-ore-doelen zijn `#minecraft:stone_ore_replaceables` (steen, graniet, dioriet, andesiet) en `#minecraft:deepslate_ore_replaceables`. Kloofsteen zit in geen van beide.
- `DataGenerators`: alle bootstraps van één registry in **één** lambda (configured/placed features vóór biomes, biome modifiers erna).

**Uit de plannen van changes 21–24 (nog niet gebouwd; afgestemd op 2026-10-01). Bij de start nalezen in `CODEBASE.md` en de code, en de namen hieronder daarop aanpassen:**

- **A1 — Kloofsteen (change 21):** blok-tag `corruptedadventure:canyon_stones` (`ModTags.Blocks.CANYON_STONES`) met `red_/orange_/cream_canyon_stone`; **niet** in `#base_stone_overworld` (change 21, vraag 3 A). Bevestigd in het plan van change 21, nog niet in code.
- **A2 — Ring 2-biomes (change 23):** `corruptedadventure:canyonlands` en `corruptedadventure:oasis_flats`, gebouwd in `worldgen/biome/Ring2Biomes` met de gedeelde helper `Ring2Biomes.generation(...)` (vanilla `globalOverworldGeneration` + `addDefaultOres` als tijdelijk ertsenslot + soft disks, eigen features achteraan). De biome-tag `corruptedadventure:is_ring_2` (gevuld uit `Ring2Biomes.ALL`) **bestaat al** (change 23): hergebruiken, geen nieuwe tag. `Ring2BiomeModifiers` bestaat nog niet: deze change maakt hem.
- **A3 — Terreinhoogtes ring 2 (change 23; vraag 5 = B, keuze gebruiker):** plateau Y 125–140, kloofbodem Y ~70 (geklemd ≥ 66; kloven 55–70 diep), vlakte/oase Y 66–74 (poelen tot Y ~59); **kloofsteen** in `canyonlands` (en later `hoodoo_fields`, `dry_riverbeds`) van **Y 8 tot het oppervlak** (change 23 vraag 8 A), daaronder deepslate. Hoodoovloer (change 30) Y 85–91, pilaren tot ~Y 135 (tot ~plateauhoogte). `oasis_flats`: zand/zandsteen op steen, geen kloofsteen. Alle Y-bereiken hieronder zijn **startwaarden relatief aan deze hoogtes**: bij de bouw de echte hoogtes uit de `ring_2/overworld`-JSON halen (of via RCON meten) en de bereiken met dezelfde relatieve ligging verschuiven ("bovenste helft van de wand", "onderste lagen kloofsteen"). Kiest change 23 vraag 8 B (kloofsteen alleen boven Y ~60), dan eerst sparren over turkoois (vraag 6).
- **A4 — Ertsblokken (change 24, afgestemd):** **per gastgesteente een eigen blok** (geen enkel `sunstone_ore`/`turquoise_ore`-blok): zonnesteen `red_canyon_sunstone_ore`, `orange_canyon_sunstone_ore`, `cream_canyon_sunstone_ore`, `scorched_sandstone_sunstone_ore` (24 vraag 1 A); turkoois `red_canyon_turquoise_ore`, `orange_canyon_turquoise_ore`, `cream_canyon_turquoise_ore` (24 vraag 2 A; bij 24-2 B ook `deepslate_turquoise_ore`). De ore-feature krijgt per gastgesteente een eigen doel (`BlockMatchTest(<gesteente>)` → variant), precies zoals `soul_crystal_ore` met netherrack/crimson nylium. Tellen via `#c:ores/sunstone` / `#c:ores/turquoise`.
- **A5 — Ring 2-grotten** komen pas in change 26; het ring 2-terrein kan in deze change dus (vrijwel) massief zijn. De W-test normaliseert daarom per gastgesteente (zie Tests), niet per chunk.

---

## Plan

### 1. Afbouw van vanilla-ores: gedeelde helper `worldgen/feature/RingVanillaOres` (nieuw, datagen)

Eén mechanisme voor ring 2–5: **vanilla's eigen ore-placed-features, met één extra `ring_chance`-filter**.

- Per ring en per vanilla-ore een eigen placed feature `corruptedadventure:ring_<n>/<vanilla-pad>` (bijv. `ring_2/ore_iron_upper`):
  - **configured feature = vanilla's** (`OreFeatures.ORE_IRON`, `ORE_COAL_BURIED`, …; verwijzing via `context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(key)`), dus zelfde adergrootte, doelen en air-exposure als vanilla;
  - **modifiers = die van vanilla's placed feature**, met `RingChanceFilter.progress(ring, from, to, 0, 1)` ingevoegd **vóór** de `biome`-filter (ná `height_range`, zodat hij op de definitieve positie werkt).
  - Kans `from`/`to` per erts uit een factortabel (vraag 1 en 2). Vaste factor = `from == to`.
  - Neveneffect dat we bewust gebruiken: `progress` geeft **buiten ring `n` kans 0**. De geschaalde ores verschijnen dus nooit buiten hun ring, ook niet als een ring 2-biome ooit elders opduikt.
- **Bron van vanilla's modifiers:** bij voorkeur gelezen uit `VanillaRegistries.createLookup()` (`OrePlacements.ORE_*` → `value().placement()`, configured-key via `feature().unwrapKey()`): geen gekopieerde getallen, volgt een MC-upgrade vanzelf. API bij de bouw verifiëren; werkt dat in de datagen-run niet, dan een tabel in de helper met de waarden uit `OrePlacements` (private helpers `commonOrePlacement`/`rareOrePlacement` nadoen) en een unit test die hem tegen vanilla legt.
- **Lijst (vanilla-volgorde van `BiomeDefaultFeatures.addDefaultOres(…, false)`, bij de bouw nalezen):** `ore_coal_upper`, `ore_coal_lower`, `ore_iron_upper`, `ore_iron_middle`, `ore_iron_small`, `ore_gold`, `ore_gold_lower`, `ore_redstone`, `ore_redstone_lower`, `ore_diamond`, `ore_diamond_medium`, `ore_diamond_large`, `ore_diamond_buried`, `ore_lapis`, `ore_lapis_buried`, `ore_copper`. Niet-ertsen die `addDefaultOres` ook toevoegt (o.a. `underwater_magma`) worden **niet** geschaald: die blijven 1-op-1 in de biome-bootstrap op hun vanilla-plek.
- Geen emerald (alleen vanilla-bergbiomes, ring 2 heeft er geen), geen badlands-extra-goud, geen `ore_copper_large` (alleen dripstone caves).
- API: `RingVanillaOres.placed(context, ring, factors)` registreert de placed features; `RingVanillaOres.keys(ring)` geeft de keys in vanilla-volgorde voor de biome modifier. `ModPlacedFeatures` krijgt geen 16 constanten per ring: de keys komen uit de helper.

### 2. Ring 2-biomes zonder vanilla's eigen ores

In de ring 2-biome-bootstrap van change 23 (A2): `BiomeDefaultFeatures.addDefaultOres` vervangen door alleen de niet-ertsen daaruit (zie §1), in dezelfde volgorde. `globalOverworldGeneration` (incl. `addDefaultUndergroundVariety`: graniet/dioriet/andesiet/tuf/aarde/grind-klodders) en soft disks blijven. De geschaalde ores komen via de biome modifier (§4).
- Liever niet via NeoForge `remove_features`: indirect, en modifiers lopen alfabetisch, dus de volgorde met de `add_`-modifier wordt subtiel.
- **Feature-volgorde:** de `ring_2/*`-features en de eigen ertsen bestaan alleen in ring 2-biomes en staan daar overal in dezelfde volgorde achteraan `UNDERGROUND_ORES` → geen "Feature order cycle" (valkuil `CODEBASE.md`). Vanilla's `ore_*` komen in ring 2 niet meer voor, dus geen tegenstrijdige volgorde met ring 1.

### 3. Zonnesteen en turkoois: `worldgen/feature/Ring2Features` (bestaat sinds change 23 met palm en vegetatie — hier **uitbreiden**, patroon `NetherRing1Features`)

**Configured features** (`ModConfiguredFeatures`):
- `corruptedadventure:sunstone_ore` (feature-naam): `minecraft:ore`, doelen per gastgesteente (A4): `BlockMatchTest(red_canyon_stone)` → `red_canyon_sunstone_ore`, idem oranje en crème, `BlockMatchTest(scorched_sandstone)` → `scorched_sandstone_sunstone_ore`, adergrootte vraag 5, `discard_chance_on_air_exposure` **0** (zichtbaar in de wanden: "gloeiend erts hoog in canyonwanden").
- `corruptedadventure:turquoise_ore` (feature-naam): `minecraft:ore`, doelen volgens vraag 6 (A: per tint `BlockMatchTest(<tint>_canyon_stone)` → `<tint>_canyon_turquoise_ore`), adergrootte vraag 6, discard vraag 7.

**Placed features** (`ModPlacedFeatures`), alleen deterministische, chunk-lokale modifiers:
- `sunstone_ore`: `count` (vraag 5; na 23b: 14) → `in_square` → `height_range` **absoluut** (vraag 4; na 23b: `trapezoid` Y 95–180, `plateau` 60, vol ~108–168) → `biome`.
- `turquoise_ore`: `rarity_filter`/`count` (vraag 6) → `in_square` → `height_range` absoluut (vraag 6; start A: `uniform` Y 10–40) → `biome`.
- **Bewust niet:** `heightmap`, `surface_relative_threshold_filter`, `environment_scan`, `count_on_every_layer`, `random_offset` naar een oppervlak — die lezen blokken die buurchunks (bomen, rotsen, meren) al dan niet al gezet hebben en hangen dus af van de generatievolgorde. "Hoog in de wand" wordt daarom een absoluut Y-bereik + het gastgesteente (kloofsteen bestaat alleen in de wanden), niet "x blokken onder het oppervlak".
- Geen `ring_chance` op de eigen ertsen: de `biome`-filter + het gastgesteente begrenzen ze al tot ring 2 (kloofsteen bestaat alleen in ring 2-biomes).

### 4. Aan ring 2 hangen: `worldgen/biome/Ring2BiomeModifiers` (nieuw, of uitbreiden als change 23 hem al maakte)

- Biome-tag **`corruptedadventure:is_ring_2`** (`ModTags.Biomes.IS_RING_2`, `ModBiomeTagsProvider`): `canyonlands`, `oasis_flats`. **Bestaat sinds change 23** (gevuld uit `Ring2Biomes.ALL`): hergebruiken. Later: change 30 voegt `salt_flats`, `hoodoo_fields`, `dune_sea` toe; change 31 beslist over `dry_riverbeds` (zie Voor latere changes).
- Eén biome modifier **`corruptedadventure:add_ring_2_ores`** (`add_features`, `#is_ring_2`, stap `UNDERGROUND_ORES`) met de features in één vaste lijst: eerst `RingVanillaOres.keys(RING_2)` (vanilla-volgorde), dan `sunstone_ore`, `turquoise_ore`. Eén modifier i.p.v. drie: de volgorde staat expliciet vast (NeoForge past meerdere modifiers alfabetisch toe).
- `withered_expanse` (zone 1→2) houdt volle vanilla-ores: hij hoort bij ring 1 en de zone is ~128 blokken (vraag 2 B zou dat veranderen).

### 5. Ore veins (vraag 3; aanbevolen A = alleen in ring 1)

Handgeschreven worldgen-JSON, hergebruik van `ring_blend`:
- **Nieuw** `density_function/ring_1/overworld/vein_toggle.json` = de huidige inline `vein_toggle` uit `overworld.json` (letterlijk verplaatst, past in de bestaande `ring_1/overworld/*`-keten).
- **Nieuw** `density_function/ring_blend/overworld_vein_toggle.json`: `ring_blend`, `dimension` overworld, `ring_1` → `corruptedadventure:ring_1/overworld/vein_toggle`, `ring_2` … `void` → `0` (A) of `{"type": "minecraft:mul", "argument1": <f>, "argument2": "corruptedadventure:ring_1/overworld/vein_toggle"}` voor ring 2 (B).
- `noise_settings/overworld.json`: router-`vein_toggle` → `"corruptedadventure:ring_blend/overworld_vein_toggle"`. `vein_ridged`, `vein_gap` en `ore_veins_enabled` blijven.
- Gevolg: in ring 1 (buiten de zone) geeft `ring_blend` exact de ring 1-invoer terug → ring 1 blok voor blok gelijk (W-check). In de zone 1→2 smoothstept de toggle naar 0: de aders lopen uit. Kosten: één ringopzoeking per kolom via `RingColumnCache`, alleen de 1–2 nodige invoeren — verwaarloosbaar.
- De Nether heeft geen ore veins (`ore_veins_enabled` false in `nether.json`): niets te doen.

### 6. `DataGenerators` en datagen-output

- Configured: `Ring2Features::configured` in de bestaande `CONFIGURED_FEATURE`-lambda; placed: `Ring2Features::placed` (roept ook `RingVanillaOres.placed(context, Ring.RING_2, RING_2_FACTORS)`) in de `PLACED_FEATURE`-lambda; `Ring2BiomeModifiers::bootstrap` in de `BIOME_MODIFIERS`-lambda; tag in `ModBiomeTagsProvider`.
- Factortabel `RING_2_FACTORS` als constante in `Ring2Features` (balansgetallen in datagen, niet in config: een wijziging geeft anders naden in bestaande werelden, zoals `radial_falloff`).
- `./gradlew runData`, output committen:
  - `worldgen/configured_feature/{sunstone_ore,turquoise_ore}.json`
  - `worldgen/placed_feature/{sunstone_ore,turquoise_ore}.json`, `worldgen/placed_feature/ring_2/ore_*.json` (16)
  - `neoforge/biome_modifier/add_ring_2_ores.json`
  - `tags/worldgen/biome/is_ring_2.json` (als nieuw)
  - de ring 2-biome-JSON's van change 23 (zonder vanilla-`ore_*`)

### 7. Unit test `worldgen/feature/RingVanillaOresTest` (nieuw)

- Elke geschaalde feature = vanilla's configured feature + vanilla's modifiers in dezelfde volgorde + precies één `ring_chance` (`progress`, juiste ring, juiste factor) vóór de `biome`-filter.
- Alle ertsen uit de lijst aanwezig, in vanilla-volgorde; factoren in (0, 1].
- Codec-rondreis van een geschaalde placed feature (JSON-`RegistryOps`, zoals `StandaloneHolderGetter.ops`).

### 8. Documentatie

- `CODEBASE.md`: `RingVanillaOres`, `Ring2Features`, `Ring2BiomeModifiers` (mappenstructuur + tabel Belangrijke classes); registries (features, modifier, tag `is_ring_2`, density functions `ring_1/overworld/vein_toggle`, `ring_blend/overworld_vein_toggle`); databestanden (`overworld.json`: `vein_toggle` via `ring_blend`); gemeten tarieven; valkuilen: "ore veins komen uit de noise router, niet uit biome-features", "afbouw vanilla-ores = vanilla-placement + `ring_chance`; `progress` begrenst tot de ring", RCON-telwerkwijze per diepte met normalisatie per gastgesteente. "Laatst bijgewerkt".
- `fase-1-wereld.md`: §1.11 **Uitwerking (change 25)** — het afbouwmechanisme (voor 41/61/81); §2 Ring 2 "Ores" — **Uitwerking (change 25)** met doelen, Y-bereiken, aantallen en de gemeten waarden per diepte.
- `02-beslissingenlog.md`: regels hieronder.
- `docs/backlog.md`:
  - `[worldgen]` change 30: `hoodoo_fields` (en de andere nieuwe biomes) in `#is_ring_2`; zonnesteen in hoodoos vraagt een doel in hun gesteente (zandsteen/scorched sandstone) en mogelijk een zonnesteen-variant (change 24/29-achtig `blocks`). (bron: change 25)
  - `[worldgen]` change 31: `dry_riverbeds` ligt deels voorbij de grens 2→3 waar `progress` van ring 2 geen signaal geeft; eigen ertsset (bijv. `transition`-signaal) kiezen. (bron: change 25)
  - bestaande `[infra]`-regel `/ca count` aanvullen: ook per Y-band en per gastgesteente (change 25 gebruikte een RCON-script).
  - vraag 8 B indien gekozen: `[blocks]` kloofsteen-varianten van vanilla-ertsen.

---

## Client/server-impact

- Alleen server-side worldgen: placed/configured features, biome modifier (server-side toegepast bij het laden van de registries), noise router. De client krijgt de blokken via de gewone chunk-packets. Geen netwerk, geen client-code, geen SavedData/attachments.
- `ring_chance` en `ring_blend` vragen `RingLayout.current()` alleen tijdens worldgen (server draait altijd). Geen codec- of constructorcode die de layout raakt.
- **Determinisme:** alleen `count`/`rarity_filter`/`in_square`/absolute `height_range`/`ring_chance`/`biome` — allemaal uit vanilla's geseede decoration-random en de seeded `RingLayout`, chunk-lokaal. De eigen ertsen gebruiken `minecraft:ore` met discard 0 en alleen natuurlijk gesteente als doel: volgorde-onafhankelijk. De geschaalde vanilla-ores erven vanilla's air-exposure-discard (diamant, begraven kolen/goud/lapis): even (on)afhankelijk van de generatievolgorde als in vanilla zelf. `vein_toggle` is een pure functie van positie en seed.
- Bestaande testwerelden: nieuwe ring 2-chunks krijgen een andere indeling (featureset en feature-index veranderen); ring 1 verandert niet. Altijd verse werelden voor W.

---

## Tests

Bouwplan: **B, W** ("verdeling klopt per diepte"). Daarbij **D** (datagen gewijzigd) en S vanzelf (W draait op de dedicated server).

**B** — `./gradlew build` slaagt, incl. `RingVanillaOresTest` en de bestaande tests (`RingChanceFilterTest`, `RingBlendTest`).

**D** — `./gradlew runData` slaagt; bestanden uit §6 bestaan en zijn gecommit. Inhoudscheck: elke `ring_2/ore_*.json` = vanilla-modifiers + `corruptedadventure:ring_chance` (`progress`, `ring_2`) vóór `minecraft:biome`; `add_ring_2_ores` stap `underground_ores`, lijst in vanilla-volgorde + eigen ertsen; de ring 2-biome-JSON's bevatten geen `minecraft:ore_*` meer, wel `underwater_magma` (e.d.) en soft disks; `sunstone_ore`/`turquoise_ore` met absolute ankers.

**S** — `tools/server-smoke-test.sh`, nieuwe `level-name` (`smoke-change25`): start zonder fouten; geen "Feature order cycle", geen codecfout in `ring_blend/overworld_vein_toggle`.

**W** — dedicated server, seed `20260930`, preset `corrupted_world`, RCON aan (daarna weer uit). Ad-hoc RCON-script (niet gecommit), werkwijze uit `CODEBASE.md` (valkuilen "Ertsen tellen via RCON", `forceload` ≤ 256 chunks, `execute if loaded` pollen). Destructief: alleen in wegwerpwerelden.

1. **Baseline vóór de change:** zelfde seed en script op de commit vóór change 25 → tellingen ring 1-proefvlakken bewaren.
2. **Proefvlakken** (elk 5×5 chunks):
   - ring 1: 4 vlakken op ~1.000 blokken (N, O, Z, W) = 100 chunks;
   - ring 2: 4 vlakken in het ringmidden (`/ca tpring 2`-richtingen) = 100 chunks, plus 2 vlakken midden in `canyonlands` en 1 in `oasis_flats` (biome via `execute if biome` op de vlakmiddens);
   - controle: 1 vlak in ring 3 (placeholder), 1 in de zone 1→2 (`/ca tpboundary 1`), 1 in Nether-ring 1 en 2.
3. **Per chunk en per Y-band** (−64…−1, 0…63, 64…127, 128…191, 192…319; elke band ≤ 16×128×16) tellen met `fill <band> minecraft:glass replace <filter>` (glas i.p.v. lucht: geen vallend zand/grind en geen stromend water/lava in de gaten; het aantal staat in de feedback, "No blocks were filled" = 0). Filters:
   - ertsen: `#minecraft:coal_ores`, `#minecraft:iron_ores`, `#minecraft:copper_ores`, `#minecraft:gold_ores`, `#minecraft:redstone_ores`, `#minecraft:lapis_ores`, `#minecraft:diamond_ores`, `#minecraft:emerald_ores`, `minecraft:raw_iron_block`, `minecraft:raw_copper_block`, `#c:ores/sunstone`, `#c:ores/turquoise` (alle varianten, A4);
   - gastgesteente (ná de ertsen): `#minecraft:stone_ore_replaceables`, `#minecraft:deepslate_ore_replaceables`, `#corruptedadventure:canyon_stones`.
   - Kengetal per erts en band: **erts per 10.000 blokken gastgesteente** (vanilla-ores: steen + deepslate; zonnesteen/turkoois: kloofsteen). Dat corrigeert voor verschil in terreinhoogte, grotten (ring 2 heeft er nog geen, A5) en kloofsteen.
4. **Verwachtingen:**
   - **Ring 1 ongewijzigd:** ring 1-proefvlakken per chunk identiek aan de baseline (ores én `raw_iron_block`/`raw_copper_block`). Bewijst dat `ring_blend` op `vein_toggle` ring 1 niet raakt.
   - **Afbouw per diepte:** per erts (alle banden samen, en per band waar vanilla plaatst) verhouding ring 2 ÷ ring 1 van het kengetal ≈ de factor uit vraag 1 (tolerantie ±25 %, diamant ±50 % wegens kleine aantallen). Tabel per band vastleggen in `fase-1-wereld.md`.
   - **Ore veins** (vraag 3 A): `raw_iron_block` + `raw_copper_block` in ring 2 en ring 3 = **0**, in ring 1 > 0; zone 1→2 ertussenin.
   - **Zonnesteen:** > 0 in de `canyonlands`-vlakken, ≥ 80 % in het gekozen Y-bereik (vraag 4; na 23b Y 95–180), > 0 boven Y 150 in vlakken met tafelbergen (tafelbergwanden), per tint gerapporteerd (meer crème na 23b-13 B), het tarief per chunk binnen ±30 % van het doel (vraag 5; het ring 1-ijzer uit dezelfde run is de maatstaf); **0** in ring 1, ring 3, Nether en (bij aanname A3) `oasis_flats`. Ertsblokken in keien boven het oppervlak (23c) apart melden.
   - **Turkoois:** alleen in zijn band (vraag 6), tarief binnen ±50 % van het doel (ring 1-diamant als maatstaf); 0 buiten ring 2.
   - Smaragd: 0 in ring 2.
5. **Determinisme:** twee verse werelden met dezelfde seed (andere `level-name`), de tweede met de vlakken in omgekeerde volgorde geladen → zonnesteen en turkoois per chunk en per band **identiek**; geschaalde vanilla-ores identiek op hooguit vanilla's eigen ruis na (change 16: 1 blok in 1 van 24 chunks).
6. `latest.log` zonder fouten (feature order cycle, onbekende biome in tag, codecfouten).

**Blokkerende handmatige test: nee.** Alles waar latere changes op bouwen (welke ertsen waar, hoeveel per diepte, alleen in ring 2, ring 1 ongewijzigd, determinisme) is automatisch te meten via B, D en RCON op de dedicated server. Zichtbaarheid en speelgevoel zijn afstemming (getallen in datagen, zonder code bij te stellen). → **W-achteraf, afgelopen in teststop 2b na change 30** (bijgewerkt 2026-10-01; de bouwer van 30 neemt deze lijst op in de gebundelde checklist) (runClient, verse wereld met seed `20260930`):
- [ ] `/ca tpring 2` → `/locate biome corruptedadventure:canyonlands`: zonnesteen zichtbaar hoog in de wanden vanaf de kloofbodem, ook in tafelbergwanden, op richels en in nissen; gloed in de schemering (blok uit change 24).
- [ ] Zonnesteen en turkoois droppen `raw_sunstone`/`raw_turquoise` (24 vraag 3 B), Silk Touch het erts.
- [ ] Turkoois vinden door in de onderste kloofsteenlagen te graven: voelt zeldzaam maar haalbaar (bijv. binnen 10–15 min).
- [ ] Mijnen in ring 2 voelt "armer" dan ring 1, maar ijzer en kolen blijven bruikbaar; geen grote ijzeraders.
- [ ] Standaard-W (CLAUDE.md §8): teleport naar elke ring (overworld en Nether), portal vanuit ring 1 en ring 2 werkt.

**Checklist (concept)**
- [ ] Scope `worldgen` benoemd
- [ ] B, D groen; gegenereerde JSON en handgeschreven JSON gecommit
- [ ] W: tabel per erts en band (ring 1 vs. ring 2) in `fase-1-wereld.md`; ring 1 = baseline; veins 0 in ring 2; tweede verse wereld identiek
- [ ] `CODEBASE.md`, design-doc, beslissingenlog, backlog bijgewerkt
- [ ] `/code-review` gedaan, bevindingen afgehandeld
- [ ] Commit `worldgen: add ring 2 ores and taper vanilla ores`

---

## Risico's / aandachtspunten

- **Afhankelijk van ongebouwde changes 21/23/24** (namen, blokvarianten, terreinhoogtes, biome-bootstrap): bij de start alle aannames A1–A5 controleren. Wijkt de kloofsteen-diepte sterk af (bijv. alleen een dunne korst), dan klopt "diep in kloofsteen" niet meer: dan eerst sparren (vraag 6), niet zelf kiezen.
- **Kloofsteen is geen vanilla-ore-doel:** in de kloofsteen komen helemaal geen vanilla-ertsen (vraag 8). Vooral kolen (vanilla Y 0–192) verdwijnen daardoor in de canyonwanden; de meting per gastgesteente laat dat zien, de factor uit vraag 1 geldt alleen in steen/deepslate.
- **`VanillaRegistries.createLookup()` in datagen** kan zwaar of onbruikbaar blijken (NeoForge-registries bevroren); terugval: tabel + unit test.
- **`ring_chance` vóór `biome`:** de trekking verbruikt random; dat verschuift niets buiten de eigen feature (elke feature heeft zijn eigen decoration-seed), dus geen effect op andere features.
- **Router-wijziging (`vein_toggle`)** raakt elke overworld-chunk: ring 1 moet exact gelijk blijven (baseline-check). Een fout in `ring_blend/overworld_vein_toggle` breekt het laden van de preset (codec bij registry-load) → S vangt dat.
- **Ring 2 nog zonder grotten** (A5): vanilla-ores met air-exposure-discard verliezen minder blokken dan in ring 1; de normalisatie per gastgesteente dekt het meeste, de rest valt binnen de tolerantie. Na change 26 even opnieuw meten (backlog-regel bij change 26 niet nodig als het in diens W zit; anders toevoegen).
- **Feature-index:** extra features in `UNDERGROUND_ORES` verschuiven de decoration-seeds van latere features in die stap in ring 2-biomes; deterministisch, maar oude testwerelden krijgen andere ring 2-chunks.

---

## Ontwerpvragen

### 1. Hoeveel minder vanilla-ores in ring 2?
Kans per vanilla-poging (`ring_chance`), dus verwachte hoeveelheid × factor in steen/deepslate. Ring 3–5 bouwen hierop verder af (changes 41, 61, 81).
- **A. Gematigd (aanbevolen):** kolen, ijzer, koper ×0,6; goud, redstone, lapis ×0,5; diamant ×0,25. Letterlijk "minder, diamant zeldzaam"; ijzer blijft genoeg voor emmers/rails, de ringores (zonnesteen) worden het doel. Ruimte voor ring 3 (×0,3 / ×0,1) en ring 5 (vrijwel 0).
- **B. Mild:** kolen, ijzer, koper ×0,8; goud, redstone, lapis ×0,7; diamant ×0,5. Weinig voelbaar verschil met ring 1; de stap naar "schaars" in ring 3 wordt groot.
- **C. Streng:** kolen, ijzer, koper ×0,4; goud, redstone, lapis ×0,3; diamant ×0,1. Duidelijk armer, sterke druk richting ringores; risico dat basisspullen (fakkels, emmers) in multiplayer schaars voelen.

Consequentie voor alle opties: puur data, zonder code bij te stellen; geldt alleen in steen/deepslate (zie vraag 8).

Antwoord: **A** — gematigd: kolen, ijzer, koper ×0,6; goud, redstone, lapis ×0,5; diamant ×0,25.

### 2. Vaste factor per ring of glijdend verloop?
- **A. Vast per ring (aanbevolen):** één factor in heel ring 2 (`from == to`); de stap ligt op de ringgrens (ondergronds onzichtbaar). Eenvoudig te meten en te balanceren; past bij "per ring een tier" (fase 2). `withered_expanse` blijft vanilla.
- **B. Glijdend binnen de ring:** `ring_chance` `progress` van ring 1-niveau (×1,0) aan de binnenrand naar het ring 3-niveau aan de buitenrand; geen stap, maar het verschil tussen twee plekken in dezelfde ring is groot, en de meting moet per afstandsband. Zelfde code, alleen andere `from`/`to`.
- **C. Vast, plus een verloop in `withered_expanse`** (signaal `transition`: ×1,0 → de ring 2-factor over de zone). Zachte overgang onder het overgangsbiome; vraagt `withered_expanse` óók zonder `addDefaultOres` en een geschaalde set voor de zone (meer features, meer testwerk).

Antwoord: **A** — vaste factor per ring (`from == to`); `withered_expanse` blijft vanilla.

### 3. Grote ertsaders (ore veins) in ring 2?
De ijzeraders (Y −60…−8, met ruw-ijzerblokken) en koperaders (Y 0…50) komen uit de noise router, niet uit biome-features; zonder ingreep blijven ze in ring 2–5 vanilla.
- **A. Alleen in ring 1 (aanbevolen):** `vein_toggle` via `ring_blend`: ring 1 vanilla, ring 2–5 en void 0; in de zone 1→2 lopen ze uit. Eén JSON-verwijzing, ring 1 blijft blok voor blok gelijk; diepe ijzer-/koperbron verdwijnt buiten ring 1, wat "ijzer minder" ondersteunt.
- **B. Kleiner in ring 2:** ring 2 = `vein_toggle` × 0,8 (minder en kleinere aders; het effect is niet lineair, dus meten en bijstellen), ring 3–5 later. Meer afstemwerk.
- **C. Vanilla laten:** geen router-wijziging; ring 2 houdt even veel aders als ring 1, wat de afbouw van ijzer op diepte grotendeels tenietdoet.

Antwoord: **A** — ore veins alleen in ring 1 (`vein_toggle` via `ring_blend`, ring 2–5 en void 0). Het geleidelijk uitlopen in de ring 1-helft van zone 1→2 (ook in `withered_expanse`) is door de gebruiker geaccepteerd (2026-10-01, samen met de floodedness van change 26).

### 4. Zonnesteen: waar precies?
Alle opties: doel = kloofsteen (`#canyon_stones`), discard 0 (zichtbaar in de wand), alleen in `canyonlands` (later ook `hoodoo_fields`, change 30). Y-getallen zijn startwaarden bij aanname A3 (diepe hoogtes van 23 vraag 5 B; wand Y ~70–140) en schuiven mee met het echte terrein van change 23.
- **A. Bovenste helft van de wanden (aanbevolen):** `trapezoid` Y 95–150, vol tussen 110 en 140 (afgestemd op de hoogtes van change 23 vraag 5 B; dekt ook het grootste deel van de hoodoo-pilaren van change 30, vloer Y ~88, toppen tot ~135). Klimdoel zoals het design zegt; vanaf de kloofbodem zichtbaar, bereikbaar door te klimmen of vanaf de rand te graven.
- **B. De hele wand:** `uniform` Y 66–150. Makkelijker te vinden (ook op loopniveau), minder "hoog".
- **C. Alleen de rand:** `uniform` Y 125–150. Sterk klimdoel/randmateriaal, maar in ondiepe canyons komt er weinig voor.

Antwoord: **A** — bovenste helft van de wanden: `trapezoid` Y 95–150, vol tussen 110 en 140 (doorgerekend op de diepe hoogtes van change 23 vraag 5 B: wand Y ~70–140). **Bijgewerkt 2026-10-01 (23b, tafelbergen):** opgerekt tot `trapezoid` Y 95–180, `plateau` 60 (vol ~108–168), zodat ook de tafelbergwanden zonnesteen hebben.

### 5. Zonnesteen: hoeveel?
Zonnesteen is het hoofdmateriaal (kandidaat gear-tier ring 2). Maatstaf: ring 1-ijzer per chunk uit dezelfde W-meting.
- **A. Als ijzer, maar alleen in de canyonwanden (aanbevolen):** start 8 aders per chunk, grootte 8; doel ≈ ring 1-ijzer per `canyonlands`-chunk. Genoeg voor een volledige set in één verkenning; buiten canyons niets (de reden om canyons in te gaan).
- **B. Als goud:** start 4 aders, grootte 6; doel ≈ ¼ van ijzer. Kostbaarder materiaal, langere grind voor een set.
- **C. Rijk:** start 12 aders, grootte 9; doel ≈ 1,5× ijzer. Snel uitgerust; zonnesteen voelt dan niet als iets bijzonders.

Antwoord: **A** — als ijzer, alleen in de canyonwanden: start 8 aders per chunk, grootte 8; doel ≈ ring 1-ijzer per `canyonlands`-chunk. **Bijgewerkt 2026-10-01:** start 14 aders (het groter bereik van vraag 4 verdunt 8 aders ×1,7); het doel blijft gelijk.

Antwoord (vervolg, keuze gebruiker 2026-10-02): **C** — 14 aders × 8 blijven. Gemeten (seed 20260930, 99 `canyonlands`-chunks): **19,7 zonnesteen per chunk** = ~¼ van het ring 1-ijzer uit dezelfde run (75,8 per chunk); de opties waren A 50 aders (~70, ≈ ijzer), B 28 (~40), C 14 (~20). Afstemwaarde voor teststop 2b.

### 6. Turkoois: waar en hoeveel?
- **A. Onderste kloofsteenlagen, zeldzaam als diamant (aanbevolen):** doel `#canyon_stones`, `uniform` Y 10–40 (startwaarde, = onderste ~30 blokken kloofsteen bij change 23 vraag 8 A), 1 ader per chunk, grootte 4; doel ≈ ring 1-diamant per `canyonlands`-chunk. Letterlijk "zeldzaam, diep in kloofsteen"; één blok uit change 24 volstaat.
- **B. Diep in steen/deepslate, zoals diamant:** Y −64…0 in `#stone_ore_replaceables`/`#deepslate_ore_replaceables`, in alle ring 2-biomes. Kan alleen bij change 24 vraag 2 B (`deepslate_turquoise_ore`; een steen-variant bestaat in geen enkele optie); concurreert met diamant op dezelfde laag en heeft weinig met kloofsteen te maken.
- **C. Beide:** A + een klein aandeel in deepslate onder de canyons. Meer vindplaatsen, meer blokvarianten en meer afstemwerk.

Antwoord: **A** — onderste kloofsteenlagen, zeldzaam als diamant: `#canyon_stones` per tint, `uniform` Y 10–40, 1 ader per chunk, grootte 4.

Antwoord (vervolg, keuze gebruiker 2026-10-02): **B** — 4 aders × 4. Met 1 ader kwam er 1,8 turkoois per `canyonlands`-chunk, terwijl ring 1-diamant 23,8 per chunk is (vanilla doet 13 diamantpogingen per chunk, niet ~1); opties A 13 aders (~24, ≈ diamant), B 4 (~7, ¼ diamant), C 1 (1,8). Gemeten met 4: **7,1 per chunk**. Afstemwaarde voor teststop 2b.

### 7. Turkoois: zichtbaar of verstopt (`discard_chance_on_air_exposure`)?
- **A. Zichtbaar, 0,0 (aanbevolen):** volledig volgorde-onafhankelijk (de W-determinismetest is exact); diep onder de canyons ziet toch bijna niemand het zonder te graven.
- **B. Half verstopt, 0,5 (zoals vanilla's kleine diamantaders):** meer graafwerk; de air-check leest blokken die buurchunk-features (meren, geodes, tunnels) al dan niet al zetten, dus hetzelfde kleine volgorde-effect als vanilla-diamant (determinismetest met tolerantie).

Antwoord: **A** — zichtbaar, `discard_chance_on_air_exposure` 0,0.

### 8. Komen vanilla-ertsen ook in kloofsteen?
Vanilla-ertsen vervangen alleen steen/deepslate; kloofsteen (change 21) zit daar niet in.
- **A. Nee (aanbevolen):** in de kloofsteen alleen zonnesteen en turkoois; vanilla-ertsen alleen in de steen eronder. Geen nieuwe blokken; de canyons voelen "anders" en armer aan vanilla-erts (extra afbouw boven op vraag 1, vooral kolen).
- **B. Ja, met eigen kloofsteen-varianten** (bijv. `canyon_coal_ore`, `canyon_iron_ore`): consistent uiterlijk; vraagt nieuwe blokken (`blocks`, change 24 of later) → niet in deze change, naar de backlog. Tot dan gedrag A.
- **C. Ja, met de gewone steen-varianten** (grijze ertsklodders in rode kloofsteen): geen nieuwe blokken, wel lelijk en tegen de stijl.

Antwoord: **A** — nee: in kloofsteen alleen zonnesteen en turkoois; vanilla-ertsen alleen in steen/deepslate eronder.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-02 | worldgen | Vanilla-ores afbouwen per ring = vanilla's eigen placed features + `ring_chance` (`progress`) vóór de biome-filter, in een gedeelde helper `RingVanillaOres`; ring 2-biomes zonder vanilla's eigen ore-features | Eén mechanisme voor ring 2–5, vanilla-vormen en -dieptes blijven, deterministisch, begrensd tot de ring |
| 2026-10-02 | worldgen | Factoren vanilla-ores ring 2: kolen/ijzer/koper ×0,6; goud/redstone/lapis ×0,5; diamant ×0,25 | Merkbaar armer, ijzer blijft bruikbaar; ruimte voor verdere afbouw in ring 3–5 |
| 2026-10-02 | worldgen | Verloop afbouw: vaste factor per ring | Eenvoudig meten en balanceren; past bij een tier per ring |
| 2026-10-02 | worldgen | Ore veins alleen in ring 1 (`vein_toggle` via `ring_blend`); uitlopen in de ring 1-helft van zone 1→2 geaccepteerd | Ondersteunt "ijzer minder"; ring 1 buiten de zone blok voor blok gelijk |
| 2026-10-02 | worldgen | Zonnesteen: `trapezoid` Y 95–180 (`plateau` 60, vol ~108–168: bovenste helft van de diepe wanden én de tafelbergwanden van 23b), 14 aders × 8 per chunk (gemeten ~20 per `canyonlands`-chunk, ~¼ ring 1-ijzer; keuze gebruiker na meting); discard 0, alleen in kloofsteen en geschroeide zandsteen | Klimdoel zichtbaar vanaf de kloofbodem en in de tafelbergen; kostbaarder dan ijzer |
| 2026-10-02 | worldgen | Turkoois: onderste kloofsteenlagen Y 10–40, 4 aders × 4 per chunk (gemeten ~7 per `canyonlands`-chunk, ~¼ ring 1-diamant; keuze gebruiker na meting), discard 0 | Zeldzaam en diep in kloofsteen; volgorde-onafhankelijk |
| 2026-10-02 | worldgen | Vanilla-ertsen in kloofsteen: nee | Canyons voelen anders; geen nieuwe blokken |
| 2026-10-02 | worldgen | Ringores via één biome modifier per ring (`add_ring_2_ores` op `#is_ring_2`, vaste volgorde), alleen chunk-lokale placements met absolute Y | Volgt change 16; geen feature-order-cycle; volgorde-onafhankelijk |

Commit (na `/code-review`): `worldgen: add ring 2 ores and taper vanilla ores`.

---

## Voor latere changes

- **Change 26 (grotten ring 2):** na de grotten de ertsmeting per gastgesteente herhalen (air-exposure verandert de vanilla-tarieven iets).
- **Change 30 (`hoodoo_fields` e.a.):** biomes in `#is_ring_2`; zonnesteen in hoodoos vraagt een doel in hun gesteente.
- **Change 31 (`dry_riverbeds`):** ligt deels voorbij grens 2→3 (geen `progress`-signaal van ring 2) → eigen keuze voor ertsen in de zone.
- **Changes 41, 61, 81:** `RingVanillaOres.placed(context, RING_N, factors)` + `add_ring_N_ores`; `vein_toggle` blijft 0 (bij vraag 3 A).

---

## Bouwnotities (change 25, 2026-10-02)

Aannames gecontroleerd tegen de code na 23c (`fb963e1`): A1–A5 klopten (namen `Ring2Biomes.generation`, `Ring2Features`, `#is_ring_2`, de zeven ertsblokken, kloofsteen Y 8 → oppervlak, `ring_blend` met `HOLDER_HELPER_CODEC`). `addDefaultOres` heeft in 1.21.1 precies de 16 ertsen uit §1 plus `underwater_magma`. Tijdelijke hulpmiddelen (dev-commando `/cadev dump`, scripts in `build/c25/`, dumps in `run/devmap/`) zijn niet gecommit.

- **Vanilla's modifiers lezen:** niet via `VanillaRegistries.createLookup()` (holders van een andere lookup kunnen niet in onze datagen-registries geserialiseerd worden), maar door `OrePlacements.bootstrap` te draaien op een `BootstrapContext` die alleen opneemt en de lookups doorgeeft (zelfde patroon als `VanillaBiomeCopies`). Zo verwijzen de geschaalde features naar `minecraft:ore_iron` enz. van de echte lookup, en worden er geen getallen gekopieerd.
- **Factortabel:** `RingVanillaOres.factors(kolen/ijzer/koper, goud/redstone/lapis, diamant)` (soort uit het pad, bereik (0, 1] gecontroleerd); `Ring2Features.RING_2_FACTORS = factors(0.6, 0.5, 0.25)`.
- **Geen `ring_chance` op de eigen ertsen** (zoals gepland). Zonnesteen/turkoois hangen via `add_ring_2_ores` ook in `oasis_flats`; daar is (vrijwel) geen kloofsteen: gemeten 0 zonnesteen in 101 oase-chunks.
- **Afstemming na meting (keuze gebruiker, zie vraag 5 en 6):** zonnesteen blijft 14 × 8, turkoois 1 → 4 aders × 4.
- **Meetmethode:** in plaats van `fill … replace` per band een tijdelijk dumpcommando (alle blockstates + biomes per quart van 5 × 5 chunks naar een bestand) en een Python-analyse: zelfde tellingen, niet-destructief, en blok-voor-blok-vergelijking mogelijk. Gebieden (5 × 5 chunks): ring 1 N/O/Z/W op ~1.000; ring 2 N/O/Z/W op ~2.500 (N, O, Z = oase, W = kloof); kloofgebied 2.352/512, tafelbergen 2.544/624, plateau met spleten 2.400/−608, oase 224/2.016; ring 3 4.464/−32; zone 1→2 1.456/−32; Nether-ring 1 en 2. Werelden: `smoke-change25-base` (commit vóór de change), `-a2` en `-b2` (eindcode; b2 omgekeerd en chunk voor chunk geladen), `-nv` (datapack met `ore_veins_enabled` false: ring 1-referentie zonder aders). Gemeten mét vanilla-carvers (zoals ring 1).

### Testresultaten

- **B:** `./gradlew build` groen, 53 testklassen / 251 tests; nieuw `RingVanillaOresTest` (6), `VeinToggleJsonTest` (2), `Ring2BiomesTest` +1.
- **D:** `./gradlew runData`: nieuw `configured_feature/{sunstone_ore,turquoise_ore}`, `placed_feature/{sunstone_ore,turquoise_ore}`, `placed_feature/ring_2/ore_*` (16), `neoforge/biome_modifier/add_ring_2_ores`; gewijzigd `canyonlands.json`/`oasis_flats.json` (16 vanilla-`ore_*` weg, `underwater_magma` en disks blijven). Elke `ring_2/ore_*` = vanilla-modifiers + `ring_chance` (`progress`, `ring_2`, factor) direct vóór `minecraft:biome`.
- **S:** `tools/server-smoke-test.sh`, `level-name=smoke-change25`, preset `corrupted_world`, seed 20260930: `Done (8.128s)`, geen ERROR, geen "Feature order cycle", geen codecfout, geen "far chunk".
- **Ring 1 ongewijzigd** (base ↔ a2, zelfde laadvolgorde): ore veins blok voor blok gelijk (`raw_copper_block`/`raw_iron_block` 7/7, 3+5/3+5, 1/1 op dezelfde plekken); ertsen in 13–25 van 25 chunks per gebied identiek, de rest kleine vanilla-volgorde-ruis (bijv. kolen 3.980 → 3.972, waarvan 3.971 op dezelfde plek), dezelfde soort ruis als tussen a2 en b2. Nether 50/50 chunks identiek. Ring 3: de enige koperader (5 erts) is weg.
- **Ore veins:** ring 2 0 ruwe blokken (vóór: 55 `raw_iron_block` + 4 `raw_copper_block` in de kloofgebieden, midden in de kloofsteen), ring 3 0, ring 1 > 0. In het zonevlak 1→2 zaten ook vóór de change geen aders (geen tussenwaarde meetbaar).
- **Afbouw per band** (erts per 10.000 steen + deepslate; ring 1 = 100 chunks zonder aders (`-nv`), ring 2 = 200 chunks van `-a2`; "na ÷ vóór" = ring 2 tegen dezelfde plekken in `-base`, zelfde terrein):

  | erts | band | ring 1 | ring 2 | ring 2 ÷ ring 1 | na ÷ vóór | factor |
  |---|---|---|---|---|---|---|
  | kolen | 0…63 | 57,5 | 31,9 | 0,55 | 0,59 | 0,6 |
  | ijzer | −64…−1 | 13,4 | 8,7 | 0,64 | 0,36¹ | 0,6 |
  | ijzer | 0…63 | 43,2 | 25,9 | 0,60 | 0,61 | 0,6 |
  | koper | −64…−1 | 2,6 | 2,2 | 0,83² | 0,59 | 0,6 |
  | koper | 0…63 | 56,2 | 30,0 | 0,53 | 0,56¹ | 0,6 |
  | goud | −64…−1 | 14,8 | 7,3 | 0,50 | 0,51 | 0,5 |
  | goud | 0…63 | 4,2 | 2,0 | 0,47 | 0,42 | 0,5 |
  | redstone | −64…−1 | 24,3 | 11,3 | 0,47 | 0,49 | 0,5 |
  | redstone | 0…63 | 2,9 | 1,7 | 0,60 | 0,52 | 0,5 |
  | lapis | −64…−1 | 9,3 | 4,4 | 0,48 | 0,50 | 0,5 |
  | lapis | 0…63 | 8,5 | 5,1 | 0,60 | 0,53 | 0,5 |
  | diamant | −64…−1 | 18,1 | 4,7 | 0,26 | 0,28 | 0,25 |
  | diamant | 0…63 | 0,42 | 0,15 | 0,36 | 0,21 | 0,25 (n = 24) |

  ¹ "vóór" bevatte ijzer-/koperaders. ² De ring 1-referentie (oceanen, jungles) wijkt op deze diepte af; op hetzelfde terrein 0,59. Banden ≥ 64: in ring 2 vrijwel geen steen (daar is het kloofsteen), n ≤ 56. Smaragd: 0 in ring 2. Het totaal over alle banden is geen goede maat (andere verdeling van het steenvolume over de diepte).
- **Zonnesteen** (99 `canyonlands`-chunks): **19,7 per chunk** (tafelbergen 47, plateau met spleten 21, kloof 9,4, laag kloofgebied met wanden tot Y ~100: 0,3); 12,9 per 10.000 kloofsteen in Y 64–127, 42,9 in Y 128–191; **99,5 % in Y 95–180** (Y 93–178, mediaan 122); **161 blokken boven Y 150** (tafelbergwanden); per tint rood 936 / oranje 449 / crème 632 / geschroeid 0 (nog geen geschroeid zandsteen in ring 2-terrein); 5,3 % ligt bloot (~1 zichtbaar blok per kloofchunk); **0 in keien of puin**; 0 in ring 1, ring 3, Nether en `oasis_flats`.
- **Turkoois** (4 × 4): **7,1 per `canyonlands`-chunk** (ring 1-diamant 23,8: ~¼); Y 8–40 (94 % in 10–40, de rest aderuitloop op Y 8–9), 5,3 per 10.000 kloofsteen in Y 0–63; tinten rood 427 / oranje 146 / crème 161; 0 buiten ring 2.
- **Determinisme** (a2 ↔ b2; ook a ↔ b, toen met 1 turkoois-ader):
  - **Eigen ertsen:** zonnesteen en turkoois **in alle 400 chunks identiek**.
  - **Geschaalde vanilla-ertsen:** per gebied 15–24 van 25 chunks identiek; verschillen alleen vanilla-ruis (vooral kolen; ring 1 laat hetzelfde zien).
  - **23c-features (her-meting na de review-fix van 23c):** kloofgebieden (100 chunks: keien, puin, rotsval, wandbronnen, kloofsteentinten): **0 verschillen**. Oases (100 chunks: palmen, bladeren (zonder `distance`), suikerriet, oeverplanten): identiek, behalve **één chunk per wereldpaar** waar 9–12 blokken kort gras/varen/woestijnbloem (`oasis_vegetation`) op andere plekken staan (oase-chunk 16, 130 in beide paren; chunk −2, −159 in a2/b2), steeds een randchunk van een meetgebied; ondergrond en verder alles gelijk. Oorzaak niet gevonden (geen ander verschil in of rond die chunk dan vanilla-klodders onder de grond); backlog.
  - **Vanilla:** graniet/dioriet/andesiet/tufsteen- en aarde/grindklodders, kolen, ondergronds water (bronnen/aquifer), `disk_gravel`, in ring 1 bamboe/jungle/lush caves en kelp; hetzelfde als in 23b/23c.
- **`latest.log`:** geen fouten.

### Code review (`/code-review medium`)

Geen bevindingen. Nagelopen: geen feature-order-cycle met `withered_expanse` (geen gedeelde ertsfeatures, gedeelde features in dezelfde volgorde; `#is_ring_2` bevat alleen de twee ring 2-biomes), `ORES` = `addDefaultOres` zonder `underwater_magma`, `ring_chance` direct vóór `biome`, `vein_toggle` via `ring_blend` (de `interpolated`-marker blijft via `mapAll` behandeld), de opnemende context (`register` → null wordt door `OrePlacements` genegeerd). Alleen twee Javadoc-regels herschikt.

## In-game checklist (W) — voor teststop 2b (na change 30; overnemen in plan 30)

runClient, nieuwe wereld **Corrupted World**, seed `20260930`, creatief/spectator om te kijken, survival voor het mijnen. Coördinaten x y z.
- [ ] **Zonnesteen in tafelbergwanden:** bloot erts bij 2.613 177 649 (zichtbaar vanaf het zuiden), 2.619 170 673 en 2.623 173 690; in de plateaurand bij 2.596 139 696 en 2.579 135 630. Gloed in de schemering (`/time set 13000`)? Zichtbaar genoeg als klimdoel?
- [ ] **Zonnesteen in kloof- en spleetwanden:** 2.475 138 −534 en 2.437 130 −531 (plateau met spleten), −2.514 132 29 (kloof in het westen). Kijk vanaf de bodem omhoog; ~1 zichtbaar blok per kloofchunk: genoeg, of meer (14 → 28/50 aders)?
- [ ] **Hoeveelheid zonnesteen** (keuze C, ~20 per kloofchunk ≈ ¼ ijzer): 10 min mijnen in de wanden rond 2.590 140 660 (tafelbergen): kostbaar maar haalbaar voor een set?
- [ ] **Turkoois:** graaf in de onderste kloofsteenlagen (Y 10–40) onder 2.400 85 560: bloot erts bij 2.400 30 576 en 2.358 35 556, verder 2.434 10 −579 en 2.552 14 635. Strip-mijnen op Y ~25: een paar binnen 10–15 min (~7 per chunk, keuze B)?
- [ ] **Drops:** ijzeren pickaxe → `raw_sunstone` / `raw_turquoise` + XP, Silk Touch → het erts (zie checklist change 24).
- [ ] **Vanilla-ertsen in ring 2:** mijnen onder Y 8 (steen/deepslate) onder 2.400 −20 560: ijzer en kolen bruikbaar maar armer dan in ring 1, diamant duidelijk zeldzamer; **geen grote ijzer-/koperaders** (ruwe-ertsblokken) en geen grijze ertsen in de kloofsteen.
- [ ] **Ring 1:** ore veins nog aanwezig (koperaders rond 0 30 −1.000, Y 0–50).
- [ ] Standaard-W (CLAUDE.md §8): teleport naar elke ring (overworld en Nether), portal vanuit ring 1 en ring 2 werkt.
