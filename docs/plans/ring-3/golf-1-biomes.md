# Golf 1 — stroom W-biomes: 39b (biome-skelet), daarna 39c (echte surface)

Scope: `worldgen` — 39b in branch `ring3/biomes-g1`; 39c pas na integratie 1 (op main, sequentieel, vóór teststop 4a).

**Doel 39b:** de biomes van ring 3 bestaan en staan op de juiste plek, met kleuren, klimaat, spawns en tags. Het gaat om de 7 sub-biomes + de micro-biome `dormant_caldera` (8e biome, V8 = A); `steam_fens` volgt in golf 2. De `placeholder_ring_3` verdwijnt. Surface tijdelijk met vanilla-blokken.

**Doel 39c:** de surface rules wisselen naar de echte ring 3-blokken van B. Pas na integratie 1, dan kan teststop 4a met echte blokken.

Bron: `fase-1-wereld.md` Ring 3 + "Uitwerking sparring ring 3"; `contract.md` §2, §4; sjablonen `Ring2Biomes` (biomes, `generation`, tags), de controlelijst "Plekken die ring 2-biomes opsommen" in `CODEBASE.md`, `BiomeAmbience` (alleen placeholder-kleuren; sfeer is A in golf 2).

## Werk 39b
1. **`Ring3Biomes`** (bootstrap uit 37c): `ashen_wastes`, `volcanic_peaks`, `ember_woods`, `basalt_flows`, `sulfur_fields`, `dried_seabed`, `titan_fields` en `dormant_caldera` (V8 = A: groen, koel, geen natuurlijk water). Allemaal:
   - droog (geen neerslag), temperatuur hoog;
   - voorlopige lucht-/fogkleuren in asrood/grijs, met verschil per biome;
   - de definitieve kleuren komen van A (golf 2).
   - De lucht moet al op de grens omslaan; dat is een wow-voorwaarde voor 4a.
2. **Indeling:** inline `multi_noise` in de ring 3-source van de preset op `temperature` = `region_heat` en `vegetation` = `region_ground` + de derde as `weirdness` = `ring_3/overworld/caldera_dormant` (39a; V8 = A): `dormant_caldera` op weirdness ≈ 1, de andere biomes op 0. Tot W-terrein `caldera_dormant` invult, is die stub 0 en verschijnt `dormant_caldera` niet. Voorstel als startpunt:
   - heet + mineraal → `volcanic_peaks`/`sulfur_fields`;
   - heet + begroeid → `basalt_flows`/`ember_woods`;
   - koel + mineraal → `ashen_wastes`/`dried_seabed`;
   - koel + vreemd → `titan_fields`.
   - Aandelen meten op r 3.600/4.750/5.900. Afstemmen met de vormen van W-terrein (pieken waar de kegels staan): via main.
3. **Tags:** `is_ring_3`, `heat_peaks` (`volcanic_peaks`), `heat_refuges` (`dormant_caldera`), `c:is_hot/overworld`, `c:is_dry/overworld`; geen `minecraft:has_structure/*`.
4. **Spawns:** geen vee, behalve geiten in `volcanic_peaks` (V7 = A: lage weight, groepjes 1–3); monsters vanilla.
5. **Generatie:** carvers, geen water-lakes, geen vanilla-bomen/gras. Ertsen en features volgen in golf 2 (41, 47*).
6. **Surface (`noise_settings/overworld.json`):** per biome een tak, nu met vanilla-blokken:
   - as = `smooth_basalt`/grind;
   - pieken = basalt/blackstone/magma;
   - zwavel = `yellow_terracotta`;
   - zeekom = zand/grind + dood koraal later;
   - titanen = `soul_soil` + basalt.
   - Steile wanden nooit vanilla-gras (de les van de kloofsteenlagen).
7. **Controlelijst** "Plekken die ring 3-biomes opsommen" maken (zoals ring 2) en als CODEBASE-delta aanleveren.
8. **Lang:** `Ring3BiomeNames` (en + nl).

## Werk 39c (na integratie 1)
De surface naar de echte blokken:
- `ash_block`/`ash_layer` en `ash_crust` boven holtes (alleen waar het terrein dat toelaat);
- puimsteen in de pieken, smeulsteen in de scheuren van `basalt_flows`;
- `sulfur_crust` in `sulfur_fields`;
- de slapende krater groen (moss/coarse dirt).
Geblakerde vlekken in `dry_riverbeds` naar as en verkoold hout (backlog r. 122).

## Afhankelijk van open vragen
- Geen meer: V7 en V8 = A (2026-10-06), verwerkt hierboven.

## Client/server-impact
Geen (data). Biome-kleuren zijn data; de client rendert.

## Tests
- **B**, **D**, **S.**
- Biome-aandelen en vlekgrootte (raster over de ring).
- `/locate biome` per biome.
- Ring 1/2 onveranderd.
- Feature-index-effect op ring 1/2 noteren (de officiële meting doet INT).
- **W-checklist** voor 4a: per biome een coördinaat en de grensovergang.

## Bij afronding
`waves.md` §4.
