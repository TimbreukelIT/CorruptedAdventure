# Golf 1 — stroom W-biomes: 64 (biome-skelet ring 4)

Scope: `worldgen` — pakket 64 in branch `ring4/biomes-g1` (basis `main` 7ffbfeb, poort n = 2). Contract: `contract.md` §2, §4, §8; ontwerp: `fase-1-wereld.md` → Ring 4. De echte ring 4-blokken in de surface volgen na INT1 (op main).

**Doel:** de 11 ring 4-biomes bestaan en staan op de juiste plek (8 grond + 2 hoogte + `sundered_reach`), met kleuren, klimaat, spawns, tags en lang; voorlopige surface met vanilla-blokken; de preset en de router kennen ze.

## Werk
1. **`Ring4Biomes`** (bootstrap uit 58): lijsten `MYTHIC` (5), `BLIGHTED` (3), `GROUND` (= de ring 4-`multi_noise`), `HEIGHT` (`sky_gardens`, `withered_gardens`: de hoogte-biomehaak), `ALL`, `TRANSITION` (`sundered_reach`), `WITH_TRANSITION`, `BLIGHTED_TAG` (bedorven band + `withered_gardens`). Per biome een `Look` (kleuren, gras/blad, neerslag, downfall); temperatuur 0,8. Kleuren voorlopig hier; de sfeerhaak (`BiomeAmbience`, ring 4-builders) is van A (80).
2. **Indeling** (preset, `rings.ring_4` = `multi_noise`) op `weirdness` = band (−1 mythisch … 1 bedorven; grens 0 = `Ring4Band.blighted`), `temperature` = `region_water` (hoog = open water), `humidity` = `region_growth` (hoog = begroeid). Continentalness, erosion, depth vol [−2, 2]. Drempels uit de gemeten kwantielen (5 seeds, 1 M punten; beide assen symmetrisch: q20 −0,244, q33 −0,125, q40 −0,075, q50 0,000, q67 0,125):
   - mythisch (band < 0): growth < −0,075 (40 %) → `crystal_lagoon` (water ≥ 0) | `mist_fjords` (water < 0); growth ≥ −0,075 (60 %) → `lotus_shallows` (water ≥ 0,125) | `glowroot_swamp` (−0,125 … 0,125) | `cascade_vale` (water < −0,125). Elk 20 %.
   - bedorven (band ≥ 0): water ≥ 0,125 → `drowned_forest`; daaronder growth ≥ 0 → `reed_marsh`, < 0 → `peat_moors`. Elk 1/3.
   - **mistrand** (V1 = A): de mythische helft van de bandovergang (−1 < band < 0; preset weirdness [−0,999, −0,0001], ~32 blokken) is altijd `mist_fjords`, ongeacht het klimaat; de andere mythische vakken hebben weirdness [−2, −0,999], de bedorven [0, 2]. Zo heeft elke bandgrens een smalle mistrand ("de mist in").
   - Gemeten op seed 20260930 (raster over ring 4 zonder zones): mythisch 63 %; elke grondbiome ≥ 8 % van de ring.
   - Afspraak met W-terrein (`Ring4Relief`, 2026-10-13): zelfde tekenconventie en drempels (W-biomes is eigenaar; een wijziging gaat via een bericht aan W-terrein); het terrein vloeit ±0,045 rond elke drempel over en maakt de mistrand een lage fjordrand.
3. **Preset:** `rings.ring_4` = die `multi_noise`; `transitions.ring_4` = `sundered_reach` (fixed); `island_biomes` → `sky_gardens` / `withered_gardens`. **Router:** `ring_4` uit `switch_at` van `overworld_temperature`, `_vegetation`, `_weirdness` (harde regel, `Ring3JsonTest.climateSwitchesExactlyAtBoundariesWithoutATransitionBiome`).
4. **Spawns** (contract §4): monsters overal vanilla (`commonSpawns`, met vleermuizen); dieren per biome (V4).
5. **Generatie:** de diepe ring 2-carverkopieën (`Ring2Carvers`, zoals ring 3 en `steam_fens`): vanilla-carvers dicht aan het oppervlak laten zand hangen en snijden door meerbodems; lava is in ring 4 al veilig (`WorldCarverLavaMixin`, 59). Geen eigen carvers (`Ring4Carvers` leeg), geen features (75–77), geen biome-modifiers (ertsen 77).
6. **Surface** (`noise_settings/overworld.json`): één ring 4-tak (alle 11 biomes) vóór vanilla's regels, uit het script `tools/worldgen/ring4_surface.py` (idempotent; de rest van het bestand blijft byte-gelijk). Vanilla-blokken per biome; nooit vallend zand/grind tegen een plafond (plafond → zandsteen/steen, zoals vanilla).
7. **Tags** (`Ring4BiomeTags`): `is_ring_4` = `WITH_TRANSITION`; `ring_4_blighted` = `BLIGHTED_TAG`; `c:`-tags (V5). Geen `minecraft:`-tags.
8. **Lang** (`Ring4BiomeNames`, en + nl).

## Client/server-impact
Alleen data (biomes, preset, surface, tags, lang): server-side worldgen; de client krijgt de biomes via de registry-sync en rendert kleuren. Geen client-class, geen payload, geen `PROTOCOL_VERSION`.

## Tests (licht: B, D, unit)
- `Ring4BiomesTest`: bootstrap = precies de lijst; kleuren per biome verschillend; neerslag/temperatuur; spawns per biome; carvers = de diepe kopieën, geen features.
- `Ring4RegionSharesTest`: de preset noemt precies `GROUND`; de vakken dekken het vlak per band zonder overlap (band −1 → mythisch, 1 → bedorven); aandelen op de echte ruis (3 seeds, ±3 pt).
- Surface: één ring 4-tak vóór vanilla, elke biome een onvoorwaardelijk top- en diepteblok, geen vanilla-gras op steile wanden van de fjorden.
- `Ring3JsonTest` (switch_at = grenzen zonder overgang) groen; `Ring4JsonTest` haak → `sky_gardens`/`withered_gardens`.
- Tags: `is_ring_4` incl. `sundered_reach`, `ring_4_blighted`.

## Vragen (naar team-lead, 2026-10-13)
V1 mistrand `mist_fjords`; V2 indeling/aandelen; V3 kleuren + klimaat; V4 spawns; V5 `c:`-tags. Antwoorden: zie "Beslissingen".

## Kleuren, klimaat, spawns, tags (zoals gebouwd)
- Kleuren (sky / fog / water / waterFog / gras / blad): `cascade_vale` 72D2E3/EAF4F2/8FD8F0/2C6E86/6FCB6A/7FD66E; `crystal_lagoon` 6CD6E8/E4F6F4/2FD3C8/0F5A66/7ED67A/6CCB64; `lotus_shallows` 78D4E4/F0F2EE/9ADFF2/356F82/86D47A/7AC96C; `glowroot_swamp` 6FC9D6/DCEBE2/4CC08A/16503A/5FAE5A/4E9E4C; `mist_fjords` 9FB8C4/C8D0D4/2F5A7A/0E2234/4F8F4A/3F7A44; `drowned_forest` 8FAE8A/A9BB98/3E8A3A/0E2A10/7A8A5A/6A7A4A; `peat_moors` 9CB08A/B4BE98/6FC23A/1E3A12/8A8E5A/7A7E4E; `reed_marsh` 96B28E/AEC09C/5DB03A/1A3614/8E9A5C/7C8A50; `sky_gardens` 66D0EE/F2F8F8/8FE0F4/2E7890/7CDA74/88E07A; `withered_gardens` A2B496/BCC4A8/6FA048/22381A/9A9466/8A8458; `sundered_reach` 8C9096/9A9CA0/5A6670/1A1E22/8A8C84/7C7E78. Cave mood: vanilla (legacy).
- Klimaat: temperatuur 0,8 (geen sneeuw, ook niet op Y 250); neerslag aan voor de 10 ring 4-biomes, uit voor `sundered_reach`; downfall 0,9 (water/moeras: lagune, lotus, glowroot, fjorden, bedorven band), 0,6 (land: cascade, de tuinen, `sundered_reach`).
- Spawns: overal `BiomeDefaultFeatures.commonSpawns` (monsters, vleermuizen, glow squids in grotten). Dieren: lagune tropische vis 25/8–8, kogelvis 15/1–3, dolfijn 2/1–2, schildpad 5/2–5; glowroot kikker 10/2–5, axolotl 10/4–6, tropische vis 25/8–8; cascade vos 8/2–4, konijn 4/2–3, zalm 5/1–5; lotus kikker 6/2–4, zalm 5/1–5; fjorden kabeljauw 15/3–6, zalm 15/1–5, inktvis 3/1–4, konijn 2/1–2; `sky_gardens` konijn 4/2–3; bedorven band, `withered_gardens` en `sundered_reach` geen dieren.
- Tags: `is_ring_4` (11), `ring_4_blighted` (bedorven band + `withered_gardens`); `c:is_temperate/overworld` alle 11; `c:is_wet/overworld` 8 grond + `sky_gardens`; `c:is_swamp` glowroot, drowned, peat, reed; `c:is_forest` cascade; `c:is_aquatic` lagune, lotus; `c:is_magical` 5 mythisch + `sky_gardens`; `c:is_lush` cascade, glowroot, `sky_gardens`; `c:is_floral` lotus, `sky_gardens`; `c:is_spooky` 3 bedorven + `withered_gardens` + `sundered_reach`; `c:is_dead` drowned, `withered_gardens`; `c:is_wasteland` `sundered_reach`. Geen `minecraft:`-tags.
- Surface (vanilla, voorlopig; land-top / land-onder / nat-top / nat-onder): cascade gras/aarde/zand/zand; lagune zand ×4; lotus gras/aarde/klei/aarde; glowroot gras/aarde/modder/modder; fjorden mosblok/aarde/grind/grind, steil steen; drowned grove aarde/aarde/modder/modder; peat podzol/modder/modder/modder; reed gras/aarde/modder/modder; `sky_gardens` gras/aarde/klei/aarde; `withered_gardens` grove aarde/aarde/grove aarde/aarde; `sundered_reach` grove aarde/steen/steen/steen. Plafondwacht: zand → zandsteen, grind → steen.

## Tests (uitgevoerd)
- `./gradlew build` groen (770 tests); `./gradlew runData` groen.
- Nieuw: `Ring4BiomesTest` (lijst, kleuren/klimaat, spawns, carvers/geen features, preset-transitie en haak, gegenereerde tags, surface-tak), `Ring4RegionSharesTest` (preset = `GROUND` + mistrand, tegeling per band incl. de mistrand, aandelen per band op de echte ruis: mythisch 0,198–0,201, bedorven 0,332–0,335; raster over de echte ring met de echte band).
- Aangepast: `Ring4JsonTest.presetHasTheHeightHookOnTheHeightBiomes` (haak nu `sky_gardens`/`withered_gardens`). `Ring3JsonTest.climateSwitchesExactlyAtBoundariesWithoutATransitionBiome` groen met `transitions.ring_4`.
- `src/generated`: alleen eigen output (11 biome-JSON's, `is_ring_4`, `ring_4_blighted`, 10 nieuwe `c:`-tags + `c:is_wet/overworld`, en/nl-lang).
- Niet gedaan (licht testen): runServer, determinisme, prestatie, in-game `/locate biome`.

## Voortgang
- 2026-10-13: branch aangemaakt, kwantielen gemeten, vragen verstuurd.
- 2026-10-13 20:03: besluiten V1–V5 verwerkt (mistrand), afspraak met W-terrein; build + runData groen.

## CODEBASE-delta
- `worldgen/biome/Ring4Biomes` (64): lijsten `MYTHIC`, `BLIGHTED`, `GROUND` (= de ring 4-`multi_noise`), `HEIGHT` (`island_biomes`), `ALL`, `TRANSITION` (`sundered_reach`), `WITH_TRANSITION`, `BLIGHTED_TAG`; record `Look` (kleuren, downfall, neerslag) per biome, `look(key)`; `generation` (diepe `Ring2Carvers`-kopieën, geen features); `spawns(key)`.
- `datagen/tags/Ring4BiomeTags`: `is_ring_4`, `ring_4_blighted`, `CONVENTION` (`c:`-tags).
- `datagen/lang/Ring4BiomeNames`: en + nl voor de 11 biomes.
- Preset `corrupted_world.json`: `rings.ring_4` = `multi_noise` (8 grondbiomes + mistrandvak), `transitions.ring_4` = `sundered_reach`, `island_biomes` = `sky_gardens`/`withered_gardens`.
- `ring_blend/overworld_temperature`, `_vegetation`, `_weirdness`: `switch_at` = [`ring_5`].
- `noise_settings/overworld.json`: één ring 4-tak (11 biomes) na de ring 3-tak, uit `tools/worldgen/ring4_surface.py` (nieuw).
- "Plekken die ring 4-biomes opsommen": `Ring4Biomes`, de preset (`rings.ring_4`, `transitions.ring_4`, `island_biomes`), `tools/worldgen/ring4_surface.py` (`RING_4`, `SURFACES`), `Ring4BiomeTags.CONVENTION`, `Ring4BiomeNames`; drempels gespiegeld in `Ring4Relief` (W-terrein).
- `ModBiomes`: commentaar ring 4 bijgewerkt (biomes nu geregistreerd).

## Beslissingen
- 2026-10-13 · worldgen · V1 A: `mist_fjords` is een klimaatregio in de mythische band én altijd de mythische helft van de bandovergang (smalle mistrand aan elke bandgrens).
- 2026-10-13 · worldgen · V2 A: gelijke aandelen per band (mythisch 5×20 %, bedorven 3×33 %), indeling zoals hierboven, drempels uit gemeten kwantielen (test ±3 pt).
- 2026-10-13 · worldgen · V3 A: kleurentabel hierboven (voorlopig, A stelt bij in 80); temperatuur 0,8, neerslag aan voor de 10 ring 4-biomes, `sundered_reach` zonder; downfall 0,9/0,6.
- 2026-10-13 · worldgen · V4 A: dieren per biome zoals hierboven; bedorven band, `withered_gardens` en `sundered_reach` zonder dieren; monsters vanilla.
- 2026-10-13 · worldgen · V5 A (voorstel, team-lead meldt het aan de gebruiker): `c:`-tags zoals hierboven, geen `minecraft:`-tags.
- Implementatiekeuzes (akkoord team-lead): surface via `tools/worldgen/ring4_surface.py`; diepe `Ring2Carvers`-kopieën (lava al veilig via de mixin van 59); geen features/modifiers in 64; `placeholder_ring_4` blijft geregistreerd; kleuren tijdelijk in `Ring4Biomes`.

## Backlog
- [worldgen] `placeholder_ring_4` verwijderen (ongebruikt sinds 64): `PlaceholderBiomes` (ook door W-nether bewerkt), `ModBiomes`, de lang-providers en `MapPalette` → INT na golf 1.
- [infra] `MapPalette` (`/ca terrainmap`): vaste kleuren voor de 11 ring 4-biomes (nu een kleur uit de id).
- [atmosphere] ring 4-kleuren uit `Ring4Biomes` naar een `BiomeAmbience.ring4`-haak (A, 80), zoals ring 3.
- [worldgen] prestatie: ring 4 heeft geen diep snelpad in de surface (zoals `ring_3_deep`); alleen bouwen als de perfronde het vraagt.
- [worldgen] na INT1: de echte ring 4-blokken in `ring4_surface.py` (tidestone, peat, …).
