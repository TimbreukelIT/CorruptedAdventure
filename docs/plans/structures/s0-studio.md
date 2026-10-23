# S0 — fotostudio voor structures (infra)

Scope: `infra`. Besluiten: `engine-case-study.md` §7 (S0-1 plek, S0-2 camera, S0-3 inhoud); hier alleen de uitwerking.
Doel: Claude ziet een structure van alle kanten in één beeld (contactsheet) zonder live 3D-blik.

## Wat er is

| Deel | Waar | Wat |
|---|---|---|
| Datapack (dev only) | `tools/structures/studio-datapack/` | dimensies `corruptedadventure:studio` (`fixed_time` 6000) en `studio_night` (18000): `minecraft:flat`, biome `minecraft:the_void` (geen neerslag, geen spawns), `features`/`lakes` uit, `structure_overrides: []`, lagen 63 × lucht + 1 × `barrier` op Y 63; dimension type met overworld-lucht, `ambient_light` 0, Y 0–320. Staat buiten `src/`: nooit in de jar |
| Installatie | `tools/test/server.sh start <level> --studio` | kopieert het pack naar `run/<level>/datapacks/castudio` (zoals `--perf` met `perfpack/`) |
| Commando | `command/studio/` | `/ca studio …` (OP, server-side, vaste Engelse tekst), hieronder |
| Driver | `tools/structures/studio.sh` + `studio.py` | plaatsen, poses fotograferen via RCON + `client.sh shot`, contactsheet |

### Commando's
- `/ca studio template <id> [studio|here|<pos>] [rotation] [mirror]` — één `.nbt` via de `StructureTemplateManager`
  (elke namespace). Met `JigsawReplacementProcessor` (jigsawblokken worden hun `final_state`) en zonder
  structure-blokken. In de studio staat de doos midden op x = z = 0 op Y 64.
- `/ca studio structure <id> [studio|here|<pos>] [seed]` — geregistreerde structure zoals `/place structure`
  (`Structure.generate` + `placeInChunk` per chunk), maar met een vaste seed (standaard 20260930) en een random per chunk
  uit die seed: een rerun geeft dezelfde stukken en blokken. In de studio: chunk 0,0.
- Zonder plek (of `studio`): in **beide** studiodimensies (dag en nacht, identiek). `here`/`<pos>`: in-situ in de
  dimensie van de bron (via RCON: `execute in <dim> positioned x y z run ca studio … here`).
- `/ca studio shots [floors]` — de poses (`pose NN_label dim day|night x y z yaw pitch cut|-`), plus `view N`. Hoogt de
  view distance van de server zo nodig op (runtime, tot 32, tot een herstart).
- `/ca studio cut <y>` / `restore` — alles boven Y weg in de doos (stil: geen buurupdates, geen drops), en terug.
- `/ca studio clear` — de doos terug zoals vóór het plaatsen (in-situ dus ook het terrein) en de geplaatste entities
  weg. Een nieuwe plaatsing doet eerst `clear`. Zonder sessie (na een herstart; de sessie leeft alleen in het geheugen)
  maakt `clear` het studiogebied leeg. `/ca studio info`.

### Keuzes (binnen de besluiten)
- **Onzichtbare vloer:** een flat-wereld zonder vaste laag geeft hoogte = min Y, dan landt een jigsaw met
  `project_start_to_heightmap` op de bodem van de wereld. Een barrièrelaag op Y 63 geeft grond op 64 en blijft
  onzichtbaar (en laat licht door): het blijft een lege void op de foto.
- **Dag en nacht** als twee dimensies met `fixed_time` (datapack-dimensies delen de tijd van de overworld). Regen wordt
  wel gedeeld (de lucht wordt donkerder): de driver zet `weather clear` vóór de foto's. In-situ: `time set 6000` /
  `18000` en daarna de oude tijd terug; ringweer ring 2–5 op `clear`.
- **Camera:** de speler (spectator, HUD uit met F1) op een pose. FOV 70 (verticaal), venster 854 × 480 (de
  standaard); `studio.sh prepare` zet `fov`, `fovEffectScale` 0, wolken uit en de render distance. Kader: het
  silhouet van de structure (per kolom een prisma van het laagste tot het hoogste blok dat de plaatsing veranderde; een
  doos was te ruim, een piramidedak liet de bovenhoeken leeg) vult 90 % van het beeld in de krapste richting en staat
  in het midden (`StudioCamera.frame`, richtpunt in 4 rondes bijgeschoven). Iso: pitch 30 vanaf NE/SE/SW/NW; gevels:
  pitch 10 vanaf N/O/Z/W; boven: pitch 90, noord boven; doorsnede: pitch 50 vanaf ZO (bij 60 kwam de maan, die bij
  `fixed_time` 6000 recht onder staat, in beeld); nacht: de 4 iso-hoeken.
- **In-situ vrij zicht:** staat er terrein of een boom tussen camera en structure (straal naar het midden en de top,
  `ClipContext` COLLIDER, alles buiten de structure-doos telt), dan gaat die pose in stappen van 10° omhoog (max. 80).
- **Leeg beginnen:** een studioplaatsing maakt eerst ±128 rond 0,0 (en de nieuwe doos) leeg in de hele hoogte, zodat
  resten van een vergeten sessie niet op de foto komen. In-situ zet de driver het terrein na de foto's terug
  (`--keep` laat het staan); Y `surface` = eerste vrije blok boven de grond (`positioned over
  motion_blocking_no_leaves`, de chunk even geforceload).
- **Verdiepingen** (`StudioFloors`): per laag tellen alleen de kolommen die erboven doorgaan; een laag is vloer als
  ≥ 80 % daarvan er vast is en ze ≥ 10 % van de footprint beslaan (lage zijstukken zoals tenten verdunnen de vloeren van
  een toren dan niet, dakranden tellen niet, muren van torens vanaf 4 × 4 niet); opeenvolgende vloerlagen zijn één vloer
  (looppunt = bovenste laag); minder dan 3 lagen erboven = dak. Geen vloer → de laagste laag van de structure. Een doorsnede houdt de vloer + 2 lagen erboven (meubels,
  onderkant van de muren; zoals een plattegrondsnede) en haalt de rest van de doos weg. Override: `--floors 64,69`.
- **Geplaatste mobs** krijgen `NoAI` (dezelfde foto bij een rerun).
- **Wachten op renderen:** na elke teleport eerst 6 s (nieuwe dimensie) of 2 s, dan foto's tot twee op rij gelijk zijn
  (gemiddeld verschil < 1,5 op een grijze 160 × 90-kopie), max. 25 s.
- **Ringsystemen in de studio:** geen code nodig. Ringweer alleen in overworld/Nether (`RingWeatherType.hasWeather`),
  wind/vulkaan/lavabommen/`HeatZone` alleen in de overworld, client-sfeer alleen in overworld/Nether. De
  `ConditionTicker` draait (`RingWorld.isActive` = de overworld), maar bij x ≈ 0 is dat ring 1 en een spectator stijgt
  nooit. Border: schaal 1, dus de gedeelde overworld-border (midden 0,0). Geen strongholds, geen structure sets.

## Gebruik

```bash
bash tools/structures/studio.sh prepare                       # eenmalig per checkout, vóór client.sh start
bash tools/test/server.sh start studio --studio --client
bash tools/test/client.sh start
bash tools/structures/studio.sh template minecraft:village/plains/houses/plains_small_house_1
bash tools/structures/studio.sh structure minecraft:pillager_outpost
bash tools/structures/studio.sh template <id> --here 120 surface -40     # in-situ (overworld; --dim voor een andere)
bash tools/structures/studio.sh shots --floors 64,69                     # opnieuw, met eigen verdiepingen
```
Uitvoer: `build/studio/<naam>/NN_<label>.png` en `sheet.png` (rijen: iso dag, gevels, boven + doorsneden, iso nacht).

## Client/server-impact
Alleen een server-side commando in `command/studio/` (geen client-class, geen payload, geen `PROTOCOL_VERSION`). De
dimensies bestaan alleen in een wereld met het pack; zonder pack meldt `/ca studio` hoe je het installeert. De mod-jar
bevat niets van de studio behalve het commando.

## Tests (2026-10-23, stroom S, n = 8)
- `./gradlew build` groen (incl. `StudioCameraTest`, `StudioFloorsTest`); `runData` zonder diff; de jar bevat geen
  studio-datapack (alleen `corrupted_nether` onder `dimension_type/`).
- Dedicated server (`server.sh start studio --studio --client`) + client: pack automatisch geladen, drie sheets in
  `build/studio/`: `village_plains_houses_plains_small_house_1` (template, studio: 14 tegels, doorsnede toont bed en
  vloer), `pillager_outpost` (jigsaw, seed 20260930, studio: 15 tegels, vloeren 63 en 76; een rerun gaf dezelfde
  stukken), `village_plains_houses_plains_small_house_1_insitu` (`--here 420 surface -260`, savanne in ring 1, terrein
  daarna terug). Een in-situ-plaatsing tussen bomen (143/152) tilde gevels N/Z/W op tot 20–30°.
- Het scherm stond uit (gebruiker > 15 min weg): elke foto via de F2-terugval van `client.sh shot`; werkt, de
  waarschuwing komt één keer per run.
