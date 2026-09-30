# Change 12 — Horror-features ring 1: kale plekken met dode bomen + onnatuurlijke 2×1-tunnels

**Scope: `worldgen` — change 12**

**Doel:** ring 1 krijgt zijn eerste onrust via de wereld zelf (§2 Ring 1, "Horror-elementen"):
1. **Kale plekken met dode bomen**: onregelmatige plekken dood gras / verdorde aarde met 1–3 kale bomen, **vaker richting de rand** van ring 1.
2. **Onnatuurlijke 2×1-tunnels**: zeldzame, kaarsrechte, te perfecte gangen (1 breed, 2 hoog) die vanuit een grot of een berghelling het gesteente in lopen en doodlopen.

Beide deterministisch per seed, puur server-side worldgen, testbaar met `/place feature`.

---

## Afhankelijkheden en aannames

- **Changes 0–7 klaar** (zie `CODEBASE.md`): `RingLayout` (met `effectiveDistance`), `ring_noise`, `RingBiomeSource`.
- **Change 8:** ring 1 bestaat uit eigen kopieën van de vanilla-biomes met keys `corruptedadventure:<vanilla-naam>` in `ModBiomes` (incl. cave biomes lush caves, dripstone caves, deep dark), en er is een biome-tag-provider (voor de `has_structure/*`-tags). Heet die anders: alleen verwijzingen aanpassen.
- **Change 10** (zie `docs/plans/change-10.md`): blokken `dead_grass_block`, `withered_dirt`, `dead_log` (as-state), `dead_wood`; `dead_grass_block` en `withered_dirt` in `#minecraft:dirt` (dode struiken en kort gras mogen erop).
- **Change 11** (zie `docs/plans/change-11.md`): `worldgen/feature/`, `ModConfiguredFeatures`/`ModPlacedFeatures` (alleen keys), configured features `dead_tree` en `dead_tree_forked` (kale bomen van `dead_log`, foliage lucht) en een random-selector `withered_trees`, plus `ModPlacementModifierTypes` + `ring_chance` (`RingChanceFilter`, alleen signaal `transition`). **Deze change hergebruikt die bomen en bouwt ze niet opnieuw.** Bestaan ze onverhoopt niet, dan komen `dead_tree`/`dead_tree_forked` hier (zelfde definitie als in het change 11-plan).
- **Placement modifier:** change 11 maakt `corruptedadventure:ring_chance` (`worldgen/feature/RingChanceFilter.java`) met de velden `signal` (enum `RingChanceFilter.Signal`, in change 11 alleen `transition`), `ring`, `from_chance`/`to_chance`, `start` en `exponent`, en daarnaast `ModPlacementModifierTypes`. Deze change voegt alleen het signaal **`progress`** toe, plus `RingLayout.ringProgress` (stap 1). Er komt dus **geen** eigen `ring_progress`-type en geen nieuw veld.
- Beslissingen die vastliggen en niet heropend worden: tunnels zijn een **worldgen-feature** (§2 Ring 1: "worldgen-feature"), 2×1, zeldzaam, "in grotten en bergen"; kale plekken vaker richting de rand; events (fakkels, silhouetten) horen in fase 5.

---

## Plan

### 1. `RingLayout.ringProgress` + signaal `progress` in `ring_chance`
**Waarom een extra signaal:** het signaal `transition` (change 11) meet het overgangsgewicht `blend` uit `ringAt`. Dat is 0–1 binnen de ~128 blokken van een overgangszone; daarbuiten geeft het geen signaal. "Vaker richting de rand" heeft een signaal nodig dat over de **hele ring** loopt, van de binnengrens (ring 1: 0,0) tot de buitengrens (~1.500 blokken). De kanscurve, codec, registratie en tests van `ring_chance` blijven gedeeld; alleen de bron van `t` verschilt.

- **`RingLayout`**: nieuwe publieke methode `ringProgress(Ring ring, double x, double z)` (in overworld-coördinaten, zoals `effectiveDistance`): `(effectiveDistance − binnenradius) / (buitenradius − binnenradius)`, geklemd op 0–1. De binnenradius van ring 1 is 0. Buiten die ring geeft de methode `OptionalDouble.empty()`. Ringgrenzen blijven alleen in `RingLayout` bekend (uit config/snapshot).
- **`worldgen/feature/RingChanceFilter.java`** (change 11): in de enum `RingChanceFilter.Signal` komt de waarde **`progress`** bij (naast `transition`). Het type blijft `corruptedadventure:ring_chance`; velden en codec veranderen niet.
  - `progress`: `t = RingLayout.current().ringProgress(ring, x·s, z·s)` met `s = RingLayout.scale(dim)`; de dimensie komt uit `context.getLevel().getLevel().dimension()`. `ring` is hier de ring zelf (zie change 11, stap 3). Buiten die ring: geen signaal.
  - Verder de gedeelde kanscurve van change 11: geen signaal of `t < start` → 0; anders `from_chance + (to_chance − from_chance) · ((t − start)/(1 − start))^exponent`; plaatsen als `random.nextFloat() < kans`.
  - Deterministisch: `random` is de geseede decoration-random van vanilla (per chunk, feature-index en stap), en `RingLayout` is geseed met de wereldseed. `RingLayout.current()` alleen in `shouldPlace`, nooit in een codec of constructor.
- De registratie in `registry/ModPlacementModifierTypes.java` (change 11) blijft ongewijzigd.
- **Waarden voor de kale plekken** (antwoord 2a; af te stemmen in test W): `signal` `progress`, `ring` `ring_1`, `start` 0,33 (~500 blokken), `from_chance` 0, `to_chance` 0,12 per chunk (~1 per 8 chunks aan de rand), `exponent` 2 (kwadratisch).

### 2. Feature `bare_patch` (eigen Feature-class)
Vanilla `minecraft:disk` volstaat niet: die kan geen bestaande planten/bomen aanpassen, geen onregelmatige rand en geen bomen plaatsen. Daarom:
- **`worldgen/feature/BarePatchConfiguration.java`** (record + codec):
  - `radius` (`IntProvider`, voorstel `uniform 4–9`), `edge_jitter` (0–1, rafelige rand),
  - `surface` en `subsurface` (`BlockStateProvider`: `dead_grass_block`, `withered_dirt`), `subsurface_depth` (`IntProvider`, 1–2),
  - `replaceable` (`HolderSet<Block>`, inline lijst: `grass_block`, `dirt`, `coarse_dirt`, `podzol`, `rooted_dirt` — geen eigen block-tag nodig, dus niets in categorie `blocks`),
  - `trees` (`Holder<ConfiguredFeature>`: `withered_trees` uit change 11), `tree_count` (`IntProvider`, 1–3),
  - `dead_bush_chance` (0–1, dode struiken op het dode gras).
- **`worldgen/feature/BarePatchFeature.java`** (`Feature<BarePatchConfiguration>`), per kolom binnen de rafelige cirkel (straal + ruis per hoek uit de feature-`random`):
  1. Van `WORLD_SURFACE` omlaag scannen (max. 24 blokken) tot een blok uit `replaceable`; onderweg vegetatie **laten verdorren** (antwoord 1a): bladeren → lucht, stammen `#logs` → `dead_log` met dezelfde as, kleine planten/klimop → lucht. Bladeren tot 3 blokken buiten de straal verdwijnen met afnemende kans (rafelige kroonrand).
  2. Kolom overslaan als er vloeistof boven de grond staat of er geen `replaceable`-grond is (water, zand, steen blijven onaangeroerd → plekken volgen het terrein).
  3. Bovenste blok → `surface`, daaronder `subsurface_depth` blokken (alleen `#dirt`) → `subsurface`. Sneeuwlaag erop blijft staan.
  4. Met `dead_bush_chance` een dode struik op het dode gras.
  5. Daarna `tree_count` bomen op willekeurige plekken binnen 0,7 × straal, alleen op `dead_grass_block` met 2 blokken lucht erboven (`trees.value().place(...)`).
- **Schrijfbereik:** herkomst via `in_square` altijd in de middelste chunk; straal ≤ 9 + ≤ 3 randmarge + kroon kale boom ≤ 3 → ruim binnen de 3×3-chunks die een feature mag beschrijven (vanilla weigert daarbuiten). De maxima worden in de codec gevalideerd.
- **Configured feature** `corruptedadventure:bare_patch`.
- **Placed feature** `corruptedadventure:bare_patch`, stap `VEGETAL_DECORATION` (achteraan, dus ná de vanilla-bomen en -bloemen van die chunk, zodat de plek ze kan laten verdorren):
  `in_square` → `ring_chance` (`signal: progress`, waarden uit stap 1) → `heightmap MOTION_BLOCKING_NO_LEAVES` → `biome`.
  (Geen `count`/`rarity`: de kans per chunk komt volledig uit `ring_chance`.)

### 3. Feature `unnatural_tunnel` (eigen Feature-class, géén carver)
**Waarom een feature en geen carver:** een carver kan tunnels over 8 chunks trekken, maar ziet per doelchunk alleen die chunk. Hij kan dus niet vooraf controleren of een tunnel droog blijft, niet gericht vanuit een grot of helling starten en werkt niet met `/place feature` (de test vraagt dat). Een feature ziet het hele 3×3-gebied en kan het pad eerst valideren. De prijs: maximaal ~45 blokken lengte. Voor de ring 1-"voorbode" is dat genoeg; de grote rasters ("onmogelijke gangen") horen bij ring 5.

- **`worldgen/feature/TunnelPath.java`** — pure, testbare padberekening (geen `Level`): uit `RandomSource`, herkomst en config een lijst celposities (vloercel; de tunnel is 2 hoog). Vorm (antwoord 3a): segment 1 van 12–28 blokken in een windrichting, met 50 % kans één haakse bocht naar links of rechts met segment 2 van 6–14 blokken. Perfect horizontaal, geen traptredes, doodlopend. De herkomst ligt midden op segment 1, dus elke cel blijft binnen ±16 blokken van de herkomst → altijd binnen het 3×3-gebied.
- **`worldgen/feature/UnnaturalTunnelConfiguration.java`** (record + codec): `first_length`, `second_length` (`IntProvider`), `turn_chance`, `replaceable` (`TagKey<Block>`: vanilla `#minecraft:overworld_carver_replaceables`), `min_solid_fraction` (voorstel 0,7), `search_distance` (voorstel 16). Codec valideert dat het pad binnen ±16 blijft.
- **`worldgen/feature/UnnaturalTunnelFeature.java`**:
  1. **Mond zoeken:** langs de gekozen richting vanaf de herkomst (tot `search_distance`) de eerste overgang *2 blokken open lucht → 2 blokken vast* zoeken; daar begint de tunnel. Open lucht is grotlucht of buitenlucht, dus de mond ligt in een grotwand of een berghelling ("in grotten en bergen"). Geen overgang → niets plaatsen.
  2. **Valideren (alleen lezen, hele pad):** geen vloeistof in of direct naast (6 buren) een cel, dak of vloer → droge tunnel, geen lekken; geen `FallingBlock` (zand/grind) in het dak; ≥ `min_solid_fraction` van de cellen vast en `replaceable`; geen `#minecraft:features_cannot_replace` (bijv. spawner/kisten). Faalt iets → hele tunnel niet plaatsen (alles-of-niets, dus nooit half).
  3. **Uithakken:** per cel de 2 blokken → `cave_air`, alleen waar het blok `replaceable` is **en** er een dak (niet-lucht op y+2) is. Waar het pad door een grot loopt, blijft het gewoon lucht; zo ontstaan geen geulen aan het oppervlak.
  4. Geen decoratie in de tunnel (het kale is het punt).
- **Configured feature** `corruptedadventure:unnatural_tunnel`.
- **Placed feature** `corruptedadventure:unnatural_tunnel`, stap `TOP_LAYER_MODIFICATION` (laatste stap: ertsen, bronnen, glow lichen en vegetatie van die chunk staan er al, dus de validatie ziet bronnen en de tunnel blijft kaal):
  `rarity_filter` (voorstel 1 op 24 chunks) → `in_square` → `height_range` uniform Y −48 … 160 → `biome`. Geen `ring_chance`: de tunnels zijn gelijkmatig zeldzaam over heel ring 1, ook bij spawn (antwoord 4a).
  Omdat de mondzoektocht vaak niets vindt (bijv. in massief gesteente of in de lucht boven vlak land), ligt de echte dichtheid lager; afstemmen in test W (doel: in een straal van ~300 blokken een paar tunnels te vinden, niet om de hoek van elke grot).

### 4. Biomes via biome modifiers (niet in de biome-kopieën zelf)
De ring 1-kopieën (change 8) worden met vanilla's `OverworldBiomes`-methodes gebouwd; daar features in injecteren zou change 8 openbreken. NeoForge-biome-modifiers zijn schoner en raken alleen onze eigen biomes:
- **Biome-tags** (datagen, in de biome-tag-provider van change 8):
  - `corruptedadventure:has_feature/bare_patch`: de begroeide land-kopieën — plains, sunflower plains, meadow, forest, flower forest, birch forest, old growth birch forest, dark forest, taiga, old growth pine/spruce taiga, savanna, savanna plateau, windswept savanna, windswept hills, windswept forest, jungle, sparse jungle, bamboo jungle, swamp, cherry grove. **Niet:** oceanen, rivieren, stranden, stony shore, woestijn, badlands, sneeuw/ijs, pieken, mangrove, cave biomes, `withered_expanse` (heeft al eigen dode bomen).
  - `corruptedadventure:has_feature/unnatural_tunnel`: **alle** ring 1-kopieën, **inclusief** cave biomes (de `biome`-filter kijkt naar de 3D-biome op de tunnelhoogte; zonder lush/dripstone/deep dark zouden ondergrondse tunnels wegvallen). Niet in de Nether, niet in `withered_expanse`.
- **Biome modifiers** (datagen, `NeoForgeRegistries.Keys.BIOME_MODIFIERS` in de `DatapackBuiltinEntriesProvider` van `DataGenerators`), type `neoforge:add_features`:
  - `corruptedadventure:add_bare_patch` → tag `has_feature/bare_patch`, `VEGETAL_DECORATION`.
  - `corruptedadventure:add_unnatural_tunnel` → tag `has_feature/unnatural_tunnel`, `TOP_LAYER_MODIFICATION`.
  Beide worden achteraan hun stap toegevoegd, in elke biome in dezelfde volgorde → geen "feature order cycle".
- Keys en bootstraps: keys in `ModConfiguredFeatures`/`ModPlacedFeatures` (change 11); bootstraps in **`worldgen/feature/Ring1HorrorFeatures.java`** (`configured`, `placed`) en **`worldgen/biome/Ring1BiomeModifiers.java`** (`bootstrap` + keys). Aanhaken in `DataGenerators`.

### 5. Registraties
- **`registry/ModFeatures.java`**: `DeferredRegister<Feature<?>>` (`Registries.FEATURE`) met `bare_patch` en `unnatural_tunnel`; registreren in `CorruptedAdventure`.
- Geen nieuw placement-modifier-type: `ring_chance` blijft in `ModPlacementModifierTypes` (change 11) staan en krijgt alleen het signaal `progress` (stap 1).

### 6. Unit tests (`./gradlew build`)
- `ring/RingLayoutTest` (uitbreiden): `ringProgress` = 0 op 0,0, ≈ 1 op de verruisde grens van ring 1, buiten de ring leeg; Nether-schaal via de aanroeper.
- `worldgen/feature/RingChanceFilterTest` (change 11, uitbreiden): signaal `progress` geeft `t` = 0 op 0,0 en ≈ 1 bij de grens van ring 1, en geen signaal in ring 2. De kans is 0 vóór `start`, `to_chance` op de rand en loopt monotoon op. De codec-rondreis werkt met `signal: progress`. De bestaande `transition`-tests blijven ongewijzigd groen (regressie).
- `worldgen/feature/TunnelPathTest`: zelfde seed → zelfde pad; alle cellen binnen ±16 van de herkomst; segmenten recht en horizontaal, bocht precies 90°; lengtes binnen de providers.

### 7. Documentatie
- `CODEBASE.md`: `ModFeatures`, `RingChanceFilter` (signaal `progress` erbij; noot van change 11 bijwerken), `BarePatchFeature`/`Configuration`, `UnnaturalTunnelFeature`/`Configuration`, `TunnelPath`, `Ring1HorrorFeatures`, `Ring1BiomeModifiers`, `RingLayout.ringProgress`; registries-tabel ("Overige": features, placement modifier, biome modifiers, biome-tags); tests; status change 12. Valkuilen: "feature mag alleen in 3×3 chunks schrijven", "`biome`-filter kijkt naar de 3D-biome (cave biomes meenemen)", "`/place feature` slaat placement modifiers over".
- `fase-1-wereld.md` (ring 1, "Horror-elementen") + `02-beslissingenlog.md`: antwoorden op de open vragen, feature i.p.v. carver, vorm/lengte tunnels, verloop kale plekken.
- `docs/backlog.md`: `[atmosphere]` tunnels eventueel als stille zone (change 13 beslist); `[infra]` optioneel `/ca` debug-hulp om tunnels te vinden (alleen als test W te veel zoekwerk blijkt).

---

## Client/server-impact

- Puur **server-side worldgen**; geen netwerk, geen sync, geen client-code. Feature- en placement-types zijn common registraties (mod staat aan beide kanten), de client gebruikt ze niet.
- `RingLayout.current()` alleen tijdens worldgen en `/place feature` (server draait dan altijd). Geen client-class bereikbaar.
- Test S is volgens het bouwplan niet verplicht voor deze change (geen gameplay/netwerk), maar kost weinig: aanbevolen als extra check dat nieuwe registraties op een dedicated server laden.

---

## Tests

- **B** — `./gradlew build` slaagt, unit tests groen.
- **D** — `./gradlew runData` slaagt; nieuw in `src/generated`: `worldgen/configured_feature/{bare_patch,unnatural_tunnel}.json`, `worldgen/placed_feature/…`, `neoforge/biome_modifier/{add_bare_patch,add_unnatural_tunnel}.json`, `tags/worldgen/biome/has_feature/…`; output gecommit.
- **W** — nieuwe wereld, seed `20260930`, preset `corrupted_world`.

**Checklist (concept):**
- [ ] `/place feature corruptedadventure:bare_patch` in een vlakte: rafelige plek dood gras + verdorde aarde, 1–3 kale bomen, dode struiken; water/zand/steen niet aangetast.
- [ ] Idem in een (dark) forest: bestaande bomen zijn verdord (kale stammen van `dead_log` in hun oorspronkelijke vorm), geen losse bladeren binnen de plek, rafelige kroonrand.
- [ ] Idem op een chunkgrens: geen afgekapte randen, geen "far chunk"-waarschuwingen in de log.
- [ ] `/place feature corruptedadventure:unnatural_tunnel` naast een grotwand en tegen een berghelling: rechte 2×1-gang, horizontaal, (eventueel) één haakse bocht, doodlopend, droog, geen geul aan het oppervlak. In massief gesteente zonder open lucht: "Failed to place" is correct.
- [ ] Tunnel die over een chunkgrens loopt: doorlopend, geen gat of stop op de grens.
- [ ] In het wild — kale plekken: in spectator vanaf 0,0 radiaal naar grens 1 (`/ca tpring 1`, `/ca tpboundary 1`); binnen ~500 blokken (bijna) geen, richting de rand duidelijk meer; in 3 richtingen tellen.
- [ ] In het wild — tunnels: in spectator door grotten en langs bergflanken binnen ring 1; minstens één gevonden in een redelijke zoektijd (anders `rarity` bijstellen).
- [ ] Geen kale plekken/tunnels in de Nether, in `withered_expanse` en voorbij grens 1 (behalve een tunnel die net over de rand steekt); geen kale plekken in oceanen, woestijnen, sneeuw.
- [ ] Determinisme: dezelfde seed in een tweede wereld → dezelfde plek en tunnel op een paar vaste coördinaten (screenshots/`execute if block` vergelijken).
- [ ] Performance: F3/log — geen merkbaar trager chunk-laden t.o.v. change 11 (ruw: `/ca tpring 1` + rondvliegen, geen "Can't keep up").

---

## Risico's

- **Chunkgrenzen (features):** een feature mag alleen in de 3×3 chunks rond zijn herkomst schrijven; daarbuiten negeert vanilla de wijziging (met logwaarschuwing). Afgedekt door `in_square` + maximale straal/lengte (in de codec gevalideerd) en de tunnel-herkomst midden op segment 1.
- **Generatievolgorde:** buurchunks kunnen hun features nog niet (of al wel) hebben gedraaid. Gevolgen: een boom uit een buurchunk die later zijn kroon over een kale plek legt; een meertje/bron van een later gegenereerde buurchunk tegen een tunnel. Dit is dezelfde beperking als bij vanilla-features (resultaat per seed is gelijk bij dezelfde generatievolgorde); bewust geaccepteerd. De tunnel in de laatste stap (`TOP_LAYER_MODIFICATION`) beperkt het.
- **Determinisme:** alle toeval uit de geseede decoration-random en `RingLayout` (wereldseed); geen `Math.random`, geen eigen `Random` zonder seed, geen instantiestaat in feature/filter-classes (worden multithreaded aangeroepen).
- **Performance:** `ring_chance` (`signal: progress`) = één `effectiveDistance` (2 simplex-samples) per chunk; kale plek ≤ ~300 kolommen × ≤ 24 blokken, zeldzaam; tunnel ≤ ~45 cellen × ~10 leesacties, zeldzaam. Verwaarloosbaar t.o.v. vanilla-decoratie.
- **Vorm bestaande bomen (verdorren, 1a):** kronen die over de rand van de plek steken, worden rafelig afgesneden (marge +3 met afnemende kans); grote bomen (dark oak, jungle-reuzen) kunnen er vreemd uitzien. Als dat in W lelijk is: de marge vergroten (1b valt af; bij echte problemen opnieuw voorleggen).
- **Uitbreiding van `ring_chance`:** wijzigt een class van change 11, maar alleen met een extra enumwaarde; velden en codec blijven gelijk, dus de gegenereerde JSON van change 11 verandert niet (runData-diff controleren). De bestaande `transition`-tests dekken de regressie af.
- **Vindbaarheid tunnels:** te zeldzaam = niemand vindt ze, te vaak = geen horror meer. Afstemmen in W; `/locate` werkt niet voor features.
- **`#minecraft:dirt`** op het dode gras (change 10) laat vanilla-planten (en saplings) erop groeien; de kale plek draait ná de vegetatie van zijn eigen chunk, maar een buurchunk kan later nog een bloem of grasplukje zetten. Klein, geaccepteerd.
- **Namen uit change 8/10/11** kunnen afwijken; dan alleen de verwijzingen aanpassen.

---

## Open ontwerpvragen

Alle vier beantwoord (2026-09-30, telkens de aanbevolen optie). Bij de bouw vastleggen in `fase-1-wereld.md` en `02-beslissingenlog.md` (stap 7).

**1. Wat gebeurt er met bestaande bomen en planten binnen een kale plek?**
- a) *(aanbevolen)* **Verdorren:** bladeren weg, stammen worden `dead_log`, bloemen en gras weg (soms een dode struik). In een bos ontstaat zo een plek met kale, dode bomen van de oorspronkelijke vorm — sterk horror-beeld ("bomen zonder bladeren"). Complexiteit: middel (scan per kolom + rafelige rand voor kronen); risico op rare afgesneden kronen bij grote bomen.
- b) **Alleen op open plekken:** de plek wordt niet geplaatst als er bomen binnen de straal staan; alleen eigen dode bomen. Eenvoudig en altijd netjes, maar in bossen (groot deel van ring 1) bijna geen kale plekken.
- c) **Bomen blijven staan:** alleen de bodem en kleine planten veranderen, levende bomen op dood gras. Eenvoudigst, maar het beeld klopt niet ("kaal" met groene bomen).

Antwoord: **a — verdorren** (2026-09-30).

**2. Hoe loopt de kans op kale plekken op richting de rand?**
- a) *(aanbevolen)* **Vanaf ~500 blokken, kwadratisch oplopend** tot de rand (~1 per 8 chunks aan de rand). Spawn voelt vertrouwd, de onrust groeit merkbaar naar buiten en sluit aan op `withered_expanse`.
- b) **Overal, lineair oplopend** (ook vlak bij spawn heel af en toe één). Onrust vanaf de eerste minuut, maar het "vertrouwde" midden wordt minder vertrouwd.
- c) **Alleen in de buitenste ~500 blokken** (vanaf ~1.000), daar vrij vaak. Scherp contrast, maar dan meer een randzone dan een geleidelijk verloop.

Antwoord: **a — vanaf ~500 blokken, kwadratisch oplopend tot de rand** (2026-09-30). Uitgevoerd via `ring_chance` met `signal: progress` (stap 1).

**3. Welke vorm krijgen de 2×1-tunnels?**
- a) *(aanbevolen)* **Recht, perfect horizontaal, hooguit één haakse bocht, doodlopend**, totaal ~12–42 blokken. Duidelijk "gegraven, niet ontstaan"; ruimte voor de grotere rasters van ring 5.
- b) **Alleen kaarsrecht** (12–30 blokken, geen bocht). Eenvoudigst, minder verrassend.
- c) **Zoals a, plus soms een traptrede omhoog/omlaag** (alsof iemand er mijnt). Sterker Herobrine-gevoel ("hier is iemand bezig geweest"), iets complexere validatie; kan de ring 5-gangen minder uniek maken.

Antwoord: **a — recht, horizontaal, hooguit één haakse bocht, doodlopend, ~12–42 blokken** (2026-09-30).

**4. Hoe zijn de tunnels over ring 1 verdeeld?**
- a) *(aanbevolen)* **Gelijkmatig zeldzaam** over heel ring 1 (ook bij spawn). Past bij "onrust vanaf het begin" (beslissingenlog) en ondergronds valt een vroege vondst niet te veel op; de rand krijgt al de kale plekken.
- b) **Zelfde verloop als de kale plekken** (vaker richting de rand, via `ring_chance` met `signal: progress`). Consistent, maar rond spawn bijna geen tunnels.
- c) **Licht verloop** (overal mogelijk, aan de rand ~2× zo vaak). Tussenweg; `ring_chance` met `signal: progress` en `from_chance` > 0.

Antwoord: **a — gelijkmatig zeldzaam over heel ring 1** (2026-09-30).
