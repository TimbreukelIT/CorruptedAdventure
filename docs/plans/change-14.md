# Change 14 — Nether-ring 1: vanilla-biomekopieën en vanilla Nether-terrein

**Scope: `worldgen` — change 14**

**Doel:** Nether-ring 1 (0 – 375, overworld-equivalent 0 – 1.500) wordt de vertrouwde vanilla-Nether:
- eigen kopieën van **nether wastes, crimson forest en warped forest** (geen soul sand valley, geen basalt deltas), verdeeld met vanilla's Nether-klimaat via change 8's `corruptedadventure:vanilla_climate`;
- het **vanilla Nether-terrein** (lavazee op 32, grotten en dak) als ring 1-invoer van `ring_blend`;
- vanilla Nether-structures (fortress, bastion, ruined portals).

De placeholder `placeholder_nether_ring_1` verdwijnt.

Bron: `fase-1-wereld.md` §2 "Nether-tegenpool — Nether-ring 1", §1.12, §1.14, §1.15, bouwplan rij 14; `01-techniek.md` "Nether (fase 1)"; `docs/plans/change-08.md`.

---

## Afhankelijkheden en aannames

- Changes 0–7 zijn klaar (zie `CODEBASE.md`). Changes 8–13 zijn gebouwd zoals gepland.
- **Change 8** (plan `change-08.md`, vraag 1 = A) levert:
  - `VanillaBiomeCopies` met `copyOf(ResourceKey<Biome>)` (→ `corruptedadventure:<zelfde pad>`), `OVERWORLD_COPIES`, en een bootstrap die vanilla's `BiomeData.bootstrap(...)` aanroept met een doorgeefcontext. Die context registreert alleen keys uit een toegestane set.
  - `VanillaClimateBiomeSource` (`corruptedadventure:vanilla_climate`): codec `preset` (vanilla-preset-id) + optioneel `replace` (vanilla-key → eigen key). Elke waarde uit de vanilla-parameterlijst wordt `replace.get(key)` of `copyOf(key)`.
  - `ModBiomeTagsProvider`: spiegelt vanilla-tags voor de kopieën via `ExistingFileHelper`, schrijft naar de vanilla-tag-paden, en staat ná `createDatapackRegistryObjects`.
  - `VanillaClimateBiomeSourceTest`.
  - Surface rules met `corruptedadventure:`-keys (overworld).
  - Lang voor de kopieën (Engels = vanilla-naam; Nederlands met de hand overgenomen).
  - Geen `ModBiomes`-constanten voor kopieën.
- **Change 9** zet vanilla's overworld-`final_density` in `ring_1/overworld.json`. Hier gebeurt hetzelfde voor de Nether, met één afwijking (stap 4).
- Changes 10–13 raken de Nether niet.
- **Change 16** (zielkristal) gaat uit van deze kopieën en hergebruikt de tag `corruptedadventure:is_nether_ring_1` als die er al is. Die maak ik hier (stap 3).
- `RingBiomeSource`, `RingFluidPicker`, `RingAquifer`, `RingChunkGenerator` en `RingBlend` veranderen niet.

---

## Plan

### 1. Biome-kopieën: `VanillaBiomeCopies` uitbreiden (geen nieuwe class)
- **Nieuw in `VanillaBiomeCopies`:** `NETHER_COPIES` = `minecraft:nether_wastes`, `minecraft:crimson_forest`, `minecraft:warped_forest`. De doorgeefcontext van de bootstrap registreert voortaan `OVERWORLD_COPIES ∪ NETHER_COPIES`.
  - Vanilla's `BiomeData.bootstrap` roept al `NetherBiomes.netherWastes/crimsonForest/warpedForest(...)` aan.
  - Zo komen de drie biomes 1-op-1 uit vanilla: features, `nether_cave`-carver, mob-spawns (piglins, hoglins, ghasts, striders, endermen), sfeer (particles, ambient, muziek) en ores (`addNetherDefaultOres`: quartz, goud, ancient debris, plus magma, grind, blackstone en lavabronnen).
  - Soul sand valley en basalt deltas blijven overgeslagen.
- `DataGenerators` verandert niet: de bootstrap-lambda van change 8 roept `VanillaBiomeCopies::bootstrap` al aan.
- **Weg:** `ModBiomes.PLACEHOLDER_NETHER_RING_1`, de registratie in `PlaceholderBiomes` (commentaar bij de Nether-placeholders bijwerken: ring 1 heeft er geen meer) en de lang-regels in `EnglishLanguageProvider`/`DutchLanguageProvider`.
- **Lang:** zoals change 8 voor de 3 kopieën.
  - Engels: "Nether Wastes", "Crimson Forest", "Warped Forest".
  - Nederlands: de vanilla-`nl_nl`-namen, in-game controleren en met de hand overnemen.

### 2. Biome-source: `corrupted_world.json` → `vanilla_climate` (keuze vastgelegd)
- Nether `rings.ring_1`: `minecraft:fixed` wordt:
  ```json
  { "type": "corruptedadventure:vanilla_climate", "preset": "minecraft:nether",
    "replace": { "minecraft:soul_sand_valley": "corruptedadventure:nether_wastes",
                 "minecraft:basalt_deltas": "corruptedadventure:nether_wastes" } }
  ```
- **Waarom `vanilla_climate` en niet `multi_noise`:**
  - Het is hetzelfde mechanisme als ring 1 in de overworld: één regel en geen uitgeschreven parameters.
  - Het volgt vanilla automatisch, en `level.dat` blijft klein.
  - Door `replace` naar nether wastes liggen crimson en warped **exact waar vanilla ze legt**. Alleen de soul-sand- en basalt-vlakken worden woestenij.
  - Met `multi_noise` zonder die twee punten zouden de grenzen verschuiven. Crimson zou dan deels de oude soul-sand-ruimte innemen.
  - Verwachte verdeling: wastes ~60 %, crimson en warped elk ~20 %.
- `preset: minecraft:nether` staat in `MultiNoiseBiomeSourceParameterList.knownPresets()`. De codec van change 8 hoeft niet te veranderen.
- *Terugval (alleen als change 8 anders is gebouwd dan gepland):* `minecraft:multi_noise` met inline `biomes`. Parameters:
  - wastes: (0, 0, 0, 0, 0, 0, offset 0)
  - crimson: (temperature 0,4, offset 0)
  - warped: (humidity 0,5, offset 0,375)
- `RingBiomeSource` geeft dezelfde sampler door; aan de Java-kant verandert daar niets.

### 3. Biome-tags (`ModBiomeTagsProvider` van change 8)
Vraag 1 = A: vanilla-structures.
- **Spiegelen** (dezelfde spiegelroute als change 8, nu met `NETHER_COPIES` erbij), zodat de kopieën dezelfde directe lidmaatschappen krijgen als de vanilla-biomes:
  - `#minecraft:is_nether` ← de 3 kopieën. Daardoor komen `has_structure/nether_fortress`, `has_structure/ruined_portal_nether` en `spawns_warm_variant_frogs` vanzelf mee (die bevatten `#is_nether`).
    - Change 8 spiegelt `is_nether` niet: dat was een overworld-lijst. Deze tag komt erbij.
    - Vanilla-code gebruikt `is_nether` verder niet (gecontroleerd in de 1.21.1-sources).
  - `#minecraft:has_structure/bastion_remnant` ← de 3 kopieën. Vanilla voegt ze los toe, niet via `is_nether`.
  - `#minecraft:snow_golem_melts` ← de 3 kopieën. Staat al in change 8's lijst; de Nether-kopieën gaan vanzelf mee.
- **Expliciet** (NeoForge-conventietags; zonder bijwerkingen, voor mod-compatibiliteit): `#c:is_nether`, `#c:is_hot/nether` en `#c:is_dry/nether` ← de 3 kopieën; `#c:is_nether_forest` ← crimson + warped. Expliciet, omdat niet zeker is dat de `ExistingFileHelper` NeoForge's `c:`-data bevat.
- **Eigen tag `#corruptedadventure:is_nether_ring_1`** ← de 3 kopieën (voor change 16 en later sfeer/weer per Nether-ring).
- **Niet:** `has_structure/nether_fossil` (alleen soul sand valley).
- Placeholders van Nether-ring 2–5 blijven zonder tags → daar geen fortresses of bastions.
- Frequentie blijft vanilla (`nether_complexes`). Een garantie komt in fase 3 (zie Doorgeschoven).

### 4. Terrein: `density_function/ring_1/nether.json` (handgeschreven, vervangt de placeholder)
- **Inhoud:** vanilla's Nether-`final_density`, **zonder de buitenste `squeeze` en `mul 0.64`**:
  `interpolated(blend_density(2.5 + y_clamped_gradient(-8→24, 0→1) · (−2.5 + 0.9375 + y_clamped_gradient(104→128, 1→0) · (−0.9375 + minecraft:nether/base_3d_noise))))`.
  - Verwijst naar vanilla's geregistreerde density function `minecraft:nether/base_3d_noise` (`old_blended_noise`); die wordt niet gekopieerd.
- **Waarom zonder squeeze:**
  - `squeeze(0,64·x)` behoudt het teken (de functie is oneven en stijgend op [−1, 1]).
  - In de Nether telt alleen het teken: aquifers staan uit, dus `density > 0` = vast, anders beslist de vloeistof-kiezer. Binnen ring 1 blijft het terrein daardoor blok voor blok vanilla.
  - De squeeze maakt de waarden wel ~3× kleiner (max ±0,46) dan de placeholder-invoer van Nether-ring 2 (±1–1,5).
  - In de overgangszone van ~32 blokken zou ring 2 dan al na een paar blokken winnen: in de praktijk een klif.
  - Zonder squeeze zijn de groottes vergelijkbaar en mengt de smoothstep over de hele zone.
- Beardifier (structures die het terrein aanpassen) speelt hier geen rol: fortress en bastion hebben geen `terrain_adaptation`.
- `legacy_random_source: true` staat al in onze `noise_settings/nether.json` (kopie van vanilla). Bij dezelfde seed geeft dat dezelfde ruis als vanilla.
- **Lavazee op 32:** `sea_level` 32 en `default_fluid` lava staan al in onze Nether-noise-settings.
  - `RingFluidPicker` geeft buiten de void-zone de vanilla-kiezer door, dus de lavazee in Nether-ring 1 werkt zonder wijziging.
  - De Nether heeft geen aquifers. `RingAquifer` wrapt dus een uitgeschakelde aquifer; ook hier geen wijziging.
- **Dak:** de vanilla-gradiënt 104–128 in de invoer plus de bedrock-dak-surface-rule (al in de kopie) geven het vanilla-dak. Nether-ring 2 heeft zijn placeholder-dak vanaf Y ~110; de overgang mengt beide.
- `ring_blend/nether.json` verandert niet: het verwijst al naar `ring_1/nether`.

### 5. `noise_settings/nether.json`: surface rules en klimaatschaal
- **Surface rules:** de `biome_is`-lijsten verwijzen naar vanilla-keys. De kopie-keys komen erbij, **naast** de vanilla-keys.
  - De vanilla-keys blijven staan: dat is onschuldig, en zo blijft de kopie met vanilla te vergelijken.
  - Change 8 *vervangt* ze in de overworld. Voor de Nether kies ik toevoegen, omdat de vanilla-Nether-biomes in onze wereld nergens voorkomen. Het effect is gelijk.
  - `minecraft:warped_forest` (r. ~401) + `corruptedadventure:warped_forest` → warped nylium en wart.
  - `minecraft:crimson_forest` (r. ~459) + `corruptedadventure:crimson_forest` → crimson nylium en wart.
  - `minecraft:nether_wastes` (r. ~520) + `corruptedadventure:nether_wastes` → zielzand en grind rond lavahoogte.
  - De regels voor soul sand valley en basalt deltas blijven ongebruikt staan (vanilla-kopie).
- **Klimaatschaal (vraag 2 = A):** in de noise router gaan `temperature.xz_scale` en `vegetation.xz_scale` van 0,25 naar **1,0**. De biomevlakken worden daardoor ~4× kleiner, in alle Nether-ringen.
  - Dit raakt alleen de biome-keuze; in de Nether gebruikt verder niets deze ruis.
  - `shift_x`/`shift_z` blijven vanilla.

### 6. Unit test
- `VanillaClimateBiomeSourceTest` (change 8) uitbreiden:
  - bij `preset: minecraft:nether` met de `replace` uit stap 2 geeft elk vanilla-Nether-parameterpunt de kopie of nether wastes;
  - `possibleBiomes` = precies de 3 kopieën.

### 7. Opruimen en documentatie
- **`./gradlew runData`:** controleren dat `src/generated/.../biome/placeholder_nether_ring_1.json` verdwijnt (vanilla-datagen ruimt oude output op via de cache) en dat de 3 biome-JSONs en de tags verschijnen.
- **`CODEBASE.md`:**
  - biome-tabel: placeholder weg, 3 Nether-kopieën erbij;
  - `VanillaBiomeCopies` (`NETHER_COPIES`) en de tags;
  - preset-regel: Nether ring 1 = `vanilla_climate` met `preset: minecraft:nether`;
  - `ring_1/nether.json` is geen placeholder meer; surface rules met kopie-keys; klimaatschaal 1,0.
  - Valkuil: *"Ring-invoer met `squeeze`: kleine waarden → overgang te kort. Het teken bepaalt vast/leeg, de grootte bepaalt de menging."*
- **`fase-1-wereld.md`:**
  - §1.15-tabel, rij 1 Nether → "vanilla-terrein (change 14)";
  - §2 Nether-ring 1: vanilla-structures (fortress, bastion, ruined portals); verdeling via `vanilla_climate` + `replace` naar wastes; Nether-klimaatruis 4× fijner (geldt voor alle Nether-ringen).
- **`02-beslissingenlog.md`:** één regel per antwoord, plus de keuze `vanilla_climate` boven `multi_noise`.
- **`docs/backlog.md`:** zie "Doorgeschoven".

---

## Client/server-impact

- Puur worldgen en data (biomes, tags, preset, density function, surface rules): server-side en deterministisch per seed.
- De client krijgt biomes en tags via de vanilla-registry-sync. Geen eigen payloads, geen client-code.
- De enige Java-wijzigingen: een set in `VanillaBiomeCopies` (datagen) en tag-regels (datagen). `VanillaClimateBiomeSource` blijft ongewijzigd.
- Bestaande testwerelden met chunks in `placeholder_nether_ring_1` worden ongeldig, omdat die biome niet meer bestaat. Test W gebruikt altijd een nieuwe wereld.

---

## Tests

- **B** — `./gradlew build`, inclusief de uitgebreide `VanillaClimateBiomeSourceTest`.
- **D** — `./gradlew runData`: 3 biome-JSONs, tags (`is_nether`, `has_structure/bastion_remnant`, `snow_golem_melts`, `c:*`, `is_nether_ring_1`) en lang; de placeholder-JSON is weg. Output committen.
- **S (extra, aanbevolen)** — `tools/server-smoke-test.sh` met een nieuwe `level-name`. De preset en de biome-registry veranderen, dus de dedicated start moet gecontroleerd worden.
- **W (Nether)** — nieuwe wereld, seed **`20260930`**, preset `corrupted_world`. Checklist:
  - [ ] `/ca tpring 1 nether`: vanilla-Nether-terrein (grote grotten, pilaren, lavazee op Y 31, bedrock-dak); je landt op een vaste vloer.
  - [ ] Surface rules werken:
    - F3 toont `corruptedadventure:nether_wastes` / `crimson_forest` / `warped_forest`;
    - crimson en warped nylium, wart blocks en huge fungi zijn zichtbaar;
    - in de wastes ligt zielzand of grind rond lavahoogte.
  - [ ] `/locate biome corruptedadventure:crimson_forest`, `…:warped_forest` en `…:nether_wastes` vanaf 0,0: alle drie gevonden binnen ~375 blokken. Noteer de afstanden.
  - [ ] `/locate biome minecraft:soul_sand_valley` en `minecraft:basalt_deltas`: niet gevonden. `corruptedadventure:placeholder_nether_ring_1` bestaat niet meer.
  - [ ] Nether-structures:
    - `/locate structure minecraft:fortress`, `minecraft:bastion_remnant` en `minecraft:ruined_portal_nether` vinden elk iets;
    - `/ca ring` op die plek zegt ring 1;
    - noteer aantal en afstanden (input voor de backlog-regel over frequentie).
  - [ ] Mobs:
    - piglins en zombified piglins in wastes en crimson;
    - hoglins in crimson;
    - endermen in warped;
    - ghasts boven de lavazee;
    - blazes bij een fortress.
  - [ ] Ores: quartz en goud zichtbaar; ancient debris aanwezig (RCON-telling uit de valkuilen in `CODEBASE.md`).
  - [ ] `/ca tpboundary 1 nether`: de overgang van Nether-ring 1 naar 2 loopt over ~32 blokken, zonder klif of muur; de biome wisselt op gewicht 0,5.
  - [ ] `/ca tpring 2…6 nether`: de placeholders zijn ongewijzigd; de void heeft nog steeds geen lava.
  - [ ] Portal-test:
    - een portal in overworld-ring 1 landt in Nether-ring 1 op ÷4, in vanilla-terrein en niet in de lavazee;
    - terug werkt ook;
    - portals uit ring 2–5 werken als voorheen.
  - [ ] Overworld ongewijzigd (steekproef `/ca tpring 1`).
  - [ ] Optioneel: dezelfde seed met de vanilla-preset "Default". De Nether op 0,0 moet dezelfde terreinvorm hebben als in onze wereld (biomes en oppervlak verschillen).
  - [ ] **Prestatie:**
    - in ring 3 van de Nether via RCON `forceload add` over bijv. 32×32 chunks en de tijd meten;
    - vergelijken met dezelfde meting vóór deze change (commit van change 13);
    - verwachting: ongeveer de kosten van de vanilla-Nether.

---

## Risico's

- **Prestatie:**
  - `RingBlend.mapAll` geeft de visitor door aan alle ring-invoeren. `NoiseChunk` vult daardoor in **elke** Nether-chunk (ook ring 2–5 en de void) de `interpolated`-marker van ring 1, met `old_blended_noise`.
  - De kosten zijn ongeveer die van de vanilla-Nether plus de goedkope placeholders. Dat is acceptabel, maar het stapelt als ring 2–5 later ook dure invoer krijgen.
  - Meten (zie W). Bij problemen: backlog `worldgen` (zie Doorgeschoven); dat hoort niet in deze change.
- **Kleinere biomevlakken (bewuste keuze, vraag 2):**
  - Het Nether-beeld wordt drukker dan in vanilla.
  - Blijkt 1,0 in test W te druk of nog te grof, dan is `xz_scale` de knop. Een andere waarde leg ik eerst aan de gebruiker voor.
- **Structure-frequentie (bewuste keuze, vraag 1):**
  - `nether_complexes` (spacing 27 chunks, fortress:bastion 2:3) doet in de hele ring ~2–3 pogingen (π·375² / 432² ≈ 2,4).
  - Verwacht: ~1 fortress en ~1,4 bastion. Er is ruwweg 40 % kans op geen enkele fortress; dat wordt in fase 3 opgelost.
- **Structures en carvers bij de grens:**
  - Een fortress of bastion die net binnen ring 1 start, kan tot ~100 blokken over het placeholder-terrein van Nether-ring 2 uitsteken.
  - `nether_cave`-carvers uit chunks van ring 1 snijden tot ~8 chunks in ring 2.
  - Dit is cosmetisch; ring 2 krijgt in zijn eigen terreinchange nieuw terrein.
- **Overgang:**
  - Het weglaten van `squeeze` (stap 4) is een aanname op basis van de waardebereiken.
  - Blijkt de overgang toch te hard of te zacht, dan is een factor op de ring 1-invoer de knop. Die verandert alleen de grootte, niet het teken, dus ring 1 blijft vanilla.
- **Spiegelroute van change 8:**
  - Leest de `ExistingFileHelper`-route `is_nether` of `bastion_remnant` niet goed, dan schrijf ik die twee tags expliciet uit.
  - Faalt de tagcontrole op `#is_nether`-verwijzingen, dan ligt dat aan de volgorde van de providers (valkuil uit change 8).
- **Oude datagen-output:**
  - Ruimt `runData` `placeholder_nether_ring_1.json` niet op, dan eerst de cache (`src/generated/resources/.cache`) controleren en opnieuw draaien.
  - Met de hand verwijderen mag niet (CLAUDE.md §9).

---

## Doorgeschoven (naar `docs/backlog.md`)

- `[structures]` Nether-ring 1: per seed gegarandeerd minstens één fortress, en liefst ook één bastion, bijv. met een eigen structure set of `FixedRingPlacement` voor ring 1. Fase 3. Aanleiding: bij vanilla-spacing is er ~40 % kans op geen fortress in de ring (beslissing change 14, 2026-09-30).
- `[worldgen]` (alleen als de meting het nodig maakt) de ring-invoeren van `ring_blend` niet vullen in chunks waar die ring niet voorkomt.

---

## Open ontwerpvragen

**1. Vanilla Nether-structures in Nether-ring 1?** Het design noemt alleen biomes en terrein; change 8 houdt de overworld-structures (villages e.d.) wel.
- **A (aanbevolen):**
  - fortress, bastion en Nether-ruined-portals via de vanilla-tags van de kopieën (`is_nether` + `has_structure/bastion_remnant`), met vanilla-spacing;
  - dat voelt vertrouwd, en blaze rods (brouwen) en netherite-upgrade-templates (alleen uit bastions) zijn te vinden;
  - nadeel: door de kleine ring ~40 % kans per seed op geen fortress, dus een backlog-regel `[structures]` voor een garantie in fase 3;
  - geen gevolgen voor multiplayer.
- **B:** zoals A, plus nu al een dichtere eigen structure set voor ring 1.
  - Dat garandeert vondsten, maar het is `structures`-werk (andere categorie) en hoort in fase 3.
  - Vanilla's `nether_complexes` overschrijven zou ook vanilla-werelden raken.
- **C:** geen Nether-structures tot fase 3. Dat past strikt bij "alleen worldgen", maar tot fase 3 zijn er dan geen blaze rods (geen brouwen) en is er geen netherite-upgrade.

Antwoord: **A** — vanilla-structures (fortress, bastion, ruined portals) via de vanilla-tags, met vanilla-spacing. Een gegarandeerde fortress per seed gaat naar de backlog `[structures]`, fase 3 (2026-09-30).

**2. Biomeverdeling binnen Nether-ring 1 (straal 375)?** Vanilla-Nether-biomes zijn honderden blokken groot. Zonder soul sand valley en basalt deltas neemt nether wastes hun ruimte over.
- **A (aanbevolen):** vanilla-parameters (zonder de twee) en de klimaatruis in de Nether-router 4× fijner (`xz_scale` 0,25 → 1,0).
  - Alle drie de biomes komen op vrijwel elke seed meermaals in de ring voor.
  - Ook de andere smalle Nether-ringen (375–500 blokken breed) krijgen meer variatie.
  - De vlakken zijn wel kleiner dan in vanilla (drukker beeld).
- **B:** puur vanilla (parameters én schaal). Per vlak het meest vertrouwd, maar op veel seeds maar 1–2 biomes in de hele ring. Crimson (zielkristal, change 16) of warped kan dan ontbreken.
- **C:** vanilla-schaal, met de parameters herverdeeld naar ~⅓ per biome. Meer bos en minder woestenij, maar door de vlakgrootte blijft het risico op ontbrekende biomes.

Antwoord: **A** — vanilla-parameters (soul sand valley en basalt deltas → nether wastes via `replace` van `vanilla_climate`) en de klimaatruis van de Nether-router 4× fijner (`xz_scale` 1,0) (2026-09-30).
