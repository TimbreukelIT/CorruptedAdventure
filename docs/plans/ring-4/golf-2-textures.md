# Golf 2 — stroom T (textures): 71b

Scope: `textures` — pakket 71b; branch `ring4/textures-g2` (vanaf `c5b7d11`), poort n = 6 (geen server nodig).

**Doel (golf-2.md §2 T, Q16 = A):** wat golf 2 nieuw nodig heeft (gameplayblokken van 72, particle-check, natte glanzende `sucking_mud` als tell, G1 = A) plus alle flora-textures van 72b (§6b, mythisch pastel, H4 = A) en een contactblad met `moonpetal` ernaast.

Bron: `golf-2.md` §2 (T, B 72/72b), §4b (G1), §6 (F1–F12, H1–H5); `fase-1-wereld.md` → `verdant_falls`; `golf-1-textures.md` (71a); `tools/textures/Ring4Textures.java`.

## Werk
1. **Golf 2-gameplayblokken (72):** de textures bestaan al uit 71a (`sucking_mud`, `toxic_pool_still`/`_flow`, `item/toxic_pool_bucket`, `updraft`, `clawed_tidestone`, `slime_trail`, `creature_nest`, `bubble_spring_top`/`_side`); `swamp_gas` is onzichtbaar (geen texture). **`sucking_mud` glanzender** (G1 = A): natte glanslichten op de bulten (licht van linksboven), donkere natte kuilen met een reflectierand, en glinsteringen die per frame twinkelen (periodiek, opaak).
2. **Particle-check:** alle acht sprites van golf 2 bestaan (`will_o_wisp`, `blossom`, `firefly`, `swamp_gas`, `toxic_bubble`, `updraft_bubble`, `glitch_speck`, `bone_dust`, elk `_0..3`, 8×8) plus `waterfall_mist`. Niets te leveren.
3. **Flora `verdant_` (72b):** procedureel in `Ring4Textures.java`, zelfde opzet als 71a (vaste seed per bestand, controles, lint).
4. **Contactblad:** `build/texture-preview/ring4.png` (alles) en `build/texture-preview/ring4-verdant.png` (`moonpetal` naast `verdant`: muren, scènes dag/nacht, planten in een slot).

## Texturepaden voor B (onder `assets/corruptedadventure/textures/`)

**Houtset (16 blokken):**
| Blok | Texture(s) |
|---|---|
| `verdant_log` | side `block/verdant_log`, end `block/verdant_log_top` |
| `verdant_wood` | `block/verdant_log` (alle zijden) |
| `stripped_verdant_log` | side `block/stripped_verdant_log`, end `block/stripped_verdant_log_top` |
| `stripped_verdant_wood` | `block/stripped_verdant_log` |
| `verdant_planks`, `_stairs`, `_slab`, `_fence`, `_fence_gate`, `_pressure_plate`, `_button` | `block/verdant_planks` |
| `verdant_door` | `block/verdant_door_top`, `block/verdant_door_bottom`; item `item/verdant_door` |
| `verdant_trapdoor` | `block/verdant_trapdoor` (cutout, kijkgaten) |
| `verdant_leaves` | `block/verdant_leaves` (cutout_mipped, **vaste kleur, geen tint**) |
| `verdant_sapling`, `potted_verdant_sapling` | `block/verdant_sapling` (kruis; jonge boomvaren) |

**Planten (13 blokken) en item:**
| Blok | Texture(s) | Vorm |
|---|---|---|
| `verdant_giant_leaf` | `block/verdant_giant_leaf_bottom`, `_top`; item = `_top` | dubbele plant (kruis), zoals `large_fern` |
| `verdant_fern` | `block/verdant_fern` | kruis |
| `verdant_tall_fern` | `block/verdant_tall_fern_bottom`, `_top`; item = `_top` | dubbele plant (kruis) |
| `verdant_bromeliad` | `block/verdant_bromeliad` + **`block/verdant_bromeliad_glow`** (emissive laag) | kruis |
| `verdant_wall_bromeliad` | dezelfde twee als `verdant_bromeliad` (zoals `wall_torch` de fakkeltexture deelt) | wandmodel van B |
| `verdant_orchid` | `block/verdant_orchid` + **`block/verdant_orchid_glow`** | vooraanzicht rechtop (bladeren onder, bloemtak boven); past op een plat wandvlak en op een kruis |
| `verdant_hanging_orchid` | `block/verdant_hanging_orchid` | hangt vanaf de bovenrand (kruis) |
| `verdant_blossom_bush` | `block/verdant_blossom_bush` | kruis |
| `verdant_vine` (kop) | `block/verdant_vine`, met `flowering=true` `block/verdant_vine_flowering` | kruis, zoals `weeping_vines` |
| `verdant_vine_plant` (stengel) | `block/verdant_vine_plant`, met `flowering=true` `block/verdant_vine_plant_flowering` | kruis; tegelt verticaal, de kop sluit eronder aan |
| `verdant_water_bloom` | `block/verdant_water_bloom` (bovenaanzicht, noord boven); item = deze texture | plat zoals `lily_pad`, **vaste kleur, geen tint** |
| `verdant_fruit_pod` | `block/verdant_fruit_pod_stage0`, `_stage1`, `_stage2` | **UV-indeling van vanilla's cacao**: model `parent` `minecraft:block/cocoa_stageN` met `textures.cocoa` (+ `particle`) op ons pad |
| item `verdant_fruit` | `item/verdant_fruit` | — |

Glow: zoals ring 3/71a (`<naam>` = hele plant met de gloed erin geschilderd, `<naam>_glow` = cutout-laag met alleen de gloeipixels, puls 8×4 interpolate). B tekent de `_glow`-laag emissive over de plant; **geen bloklicht** (F8 = A).

## Palet (H4 = A, Q-T1..4 = A)
- Stam (schors) warm zandbeige `6A5D4C…DBC8A2`; hout honingcrème `A88A5E…F4E6C8` (Q-T1 A).
- Kruin pastel mint/zeegroen `2F6A5C…BFE8D6` (vast); onderbegroeiing een stap dieper zeegroen `1D4A3E…8EC8AA` (vast, Q-T4 A).
- Bloemen (Q-T2 A): orchidee en hangorchidee lila `6E5498…F0E6F8`, bromelia abrikoos `B0603A…FDE4C0`, bloeiende struik perzik `C06A62…FEE8DE`, bloeiende liaan perzik met wat lila, waterplant bleek perzik-wit; vrucht jong mint → rijp abrikoos-oranje `8A4A26…FDDCA4`.
- Gloed (Q-T3 A): eigen kleur lichter: lila `C8B0FF…F6F0FF` (orchidee), abrikoos `FFC890…FFF2DE` (bromelia).
- `sucking_mud`: glans `737C55`, `98A47A`, `C4CEA6` (koel natgroen) boven de bestaande modder.

## Client/server-impact
Geen (assets en een los script).

## Tests
- Script 2× → md5 identiek; lint 0 (of `FLAT_OK` met reden); `checkGlowLayers` groen.
- `./gradlew build --no-daemon` en `./gradlew runData --no-daemon` (gebruikersregel: geen in-game test); `git diff --exit-code src/generated`.

## Voortgang
- 2026-10-16: branch `ring4/textures-g2` vanaf `c5b7d11`; plan en padenlijst; vragen Q-T1..4 aan main (alle A).
- 2026-10-16: `sucking_mud` glanzender (`25c0276`); flora 31 PNG's + 2 `.mcmeta`'s en contactblad `ring4-verdant.png` (`56f0ee3`). Script 2× → md5 identiek, lint 0, `checkGlowLayers` groen; **B** en **D** groen, `src/generated` ongewijzigd.
- 2026-10-16: vervolg op verzoek van main: main (`a8a18cb`) in de branch gemerged (fast-forward); sprite `particle/deep_silhouette` voor A's silhouet onder water (Q18 = A): een lange, slanke leviathan van opzij (gevorkte staartvin, lange kop met open kaak, grote flippers, lage rugvin), bijna zwart met een zweem blauwgroen, zachte alfarand van ~3 px. Let op (A): `setColor(0.02, 0.04, 0.06)` vermenigvuldigt de spritekleur, dus in-game is hij vrijwel zwart en doet de alfavorm het werk. Script 2× → md5 identiek, lint 0, **B** en **D** groen.

## Afwijkingen bij de bouw
- **Dubbele planten** (`verdant_giant_leaf`, `verdant_tall_fern`): één canvas van 16×32 per plant (`tallHalf`, seed `block/<plant>`), boven = rijen 0–15, onder = 16–31; de helften sluiten dus altijd aan.
- **Liaan:** de stengels delen de seed `block/verdant_vine#strands` en zwaaien periodiek over 16 rijen, dus `_plant` tegelt verticaal en de kop sluit er precies onder aan. Bladeren en bloempjes alleen aan knopen op rij 1–14 (niets loopt over de tegelrand; review 71b), bloempjes niet aan de zijrand. De bloeiende varianten zijn exact dezelfde pixels plus bloempjes (eigen seed `…#flowers`).
- **Vruchtpeul:** vanilla's cacao-UV (`cocoa_stage0..2.json` gelezen, geen pixels): boven/onder `[0,0,4|6|8]`, zijden `[11,4,15,9]` / `[9,4,15,11]` / `[8,4,16,13]`, steeltje `[12,0,16,4]`; de rest transparant. Stadium 1 is een menging (55 %) van mint en abrikoos (halfrijp).
- **Orchidee, hangorchidee, sapling:** handgelegde sprites (`stamp`/`sprite`); de gloeipixels van de orchidee en de schutbladspits van de bromelia via `stamp(..., glowing)`.
- **Contactblad:** gras is in de scène een stand-in (biomekleur `6CC24A` boven donkere aarde, alleen in de preview); de peul in de scène is een zij-aanzicht uit de UV-indeling (`podView`).

## Code-review (medium, `c5b7d11..56f0ee3` + plan)
- Opgelost:
  1. **Liaannaad:** een blad of bloempje aan een knoop op rij 15 liep via de wrap door naar rij 0 van dezelfde tegel; onder een kop of richel ontbrak dan de halve rand. Knopen nu alleen op rij 1–14, alles wordt geknipt in plaats van gewrapt (`vinePixel` weg); ook bloempjes die aan de zijrand half zouden vallen vervallen.
  2. **Peul-UV dubbel:** `POD_TOP`/`POD_SIDE` gedeeld door `fruitPod` en `podView`.
  3. **Bloempjes dubbel:** één `floret`-helper voor struik en liaan; de toonset (`FLORET_TONES`) één keer gebouwd.
  4. **Contactblad:** de verdant-sectie staat alleen nog in `ring4-verdant.png` (niet ook onderaan `ring4.png`); het gras-stand-in wordt één keer gebouwd; `writeVerdantPreview` meldt een te klein blad met een duidelijke fout.
- Niet opgepakt:
  - `CODEBASE.md` bijwerken: volgens `waves.md` §5 en `golf-2.md` §1 is dat INT; de delta staat hieronder.
  - "Stage 2 up/down is `[0,0,7,7]`": nagekeken in `cocoa_stage2.json` uit de resources-jar: `[0, 0, 8, 8]`; de texture klopt.
- Gewijzigde PNG's: `verdant_blossom_bush`, `verdant_vine`, `verdant_vine_plant`, `verdant_vine_flowering`, `verdant_vine_plant_flowering`. Script 2× → md5 identiek, lint 0, **B** en **D** groen, `src/generated` ongewijzigd.

## CODEBASE-delta
- `tools/textures/Ring4Textures.java` (71b): paletten `VERDANT_BARK`, `VERDANT_WOOD`, `VERDANT_LEAF`, `VERDANT_PLANT`, `LILAC`, `APRICOT`, `PEACH`, `FRUIT` (+ `FRUIT_OUTLINE`), `LILAC_GLOW`, `APRICOT_GLOW`, `SUCKING_GLOSS`; schilders `verdantLog`, `verdantLeaves`, `verdantSapling`, `giantLeaf`, `verdantFern`, `tallFern`, `bromeliad` (+ `BROMELIAD_SPIKE`), `orchid`, `hangingOrchid`, `blossomBush` (+ `floretNear`, `FLORET_TONES`), `vine` (+ `VineStrand`, `leaflet`), `waterBloom`, `fruitPod` (+ `POD_TOP`/`POD_SIDE`: de cacao-UV, gedeeld met `podView`), `verdantFruit`; helpers `tallHalf` (+ `TALL_CANVASES`, 16×32-canvas voor dubbele planten), `plot`, `stalk`, `blade` (breed blad, optioneel hartvormig), `frond` (varenblad), `strap` (riemblad), `stamp` (sprite op een positie, met gloeitekens), `floret` (bloempje van vijf pixels, struik en liaan); `suckingMud` met natte glans (rangdrempels, twinkelende glinsteringen). Contactblad: sectie `drawVerdant`, alleen in het losse blad `build/texture-preview/ring4-verdant.png` (`writeVerdantPreview`; `ring4.png` houdt de cellen per texture en de nachtrij): houtset `moonpetal` boven `verdant`, scènes cascade vale / verdant falls dag / nacht (`VERDANT`, `GRASS`/`POD`-stand-ins, `podView`, `darkened`, gras eenmalig gebouwd), peulstadia, waterplant naast lotus, `VERDANT_SLOTS`; `NIGHT` + orchidee en bromelia.
- `Ring4Textures.java` (vervolg 71b, na main `a8a18cb`): `deepSilhouette` (+ `silhouetteDepth`, `spineY`, `bodyHalf`, `taperedSegment`, palet `SILHOUETTE`), `DEEP_SILHOUETTE`/`SILHOUETTE_SIZE` (64×64), `SOFT_ALPHA` is nu een lijst van voorvoegsels (waterval-mist + silhouet); te grote texturen (> 32) krijgen in het contactblad één weergave op 2× over diep water in plaats van een 3×3-tegel.
- `textures/particle/deep_silhouette.png` (64×64, zachte alfa): sprite voor A's ongeregistreerde client-particle `DeepSilhouetteParticle` (`corruptedadventure:deep_silhouette` in de particle-atlas). Geen atlasregel nodig: vanilla's `atlases/particles.json` (bron `directory`, `particle`, prefix leeg) neemt elke PNG onder `textures/particle/` van elke namespace op.
- `textures/` (71b): `block/` verdant_log(_top), stripped_verdant_log(_top), verdant_planks, verdant_door_{top,bottom}, verdant_trapdoor, verdant_leaves, verdant_sapling, verdant_giant_leaf_{bottom,top}, verdant_fern, verdant_tall_fern_{bottom,top}, verdant_bromeliad(+`_glow`, puls), verdant_orchid(+`_glow`, puls), verdant_hanging_orchid, verdant_blossom_bush, verdant_vine, verdant_vine_plant, verdant_vine_flowering, verdant_vine_plant_flowering, verdant_water_bloom, verdant_fruit_pod_stage{0,1,2}; `item/` verdant_door, verdant_fruit. `block/sucking_mud` vernieuwd (natte glans). 31 nieuwe PNG's, 2 nieuwe `.png.mcmeta`'s.

## Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-16 | textures | Q-T1 A: binnenhout `verdant` (gestript, planken, deur, valluik) bleek warm honingcrème, een lichtere tint uit de zandfamilie van de stam | Besluit gebruiker; leest als tropisch hardhout en blijft los van het roze-crème van `moonpetal` |
| 2026-10-16 | textures | Q-T2 A: kleur per soort: orchidee en hangorchidee lila, bromelia abrikoos (schutbladen), bloeiende struik perzik, bloeiende liaan perzik met wat lila, waterplant bleek perzik-wit; vruchtpeul jong mintgroen → rijp abrikoos-oranje, `verdant_fruit` abrikoos-oranje | Besluit gebruiker; elke soort in één oogopslag herkenbaar |
| 2026-10-16 | textures | Q-T3 A: gloei-overlay in de eigen bloemkleur, lichter (lila gloed op de orchidee, abrikoos op de bromelia; lip/hart en punten) | Besluit gebruiker; duidelijk anders dan de blauwe gloeiflora van de mythische band |
| 2026-10-16 | textures | Q-T4 A: onderbegroeiing (varens, reuzenblad, bladeren van bromelia en orchidee, liaan, struik) vaste kleuren in de pastel mint/zeegroen-familie, een stap dieper dan de kruin; geen biome-tint | Besluit gebruiker; sluit aan op de kruin, B hoeft geen `tintindex` te zetten |
| 2026-10-16 | textures | `sucking_mud` glanzender (natte glanslichten, natte kuilen, twinkelende glinsteringen) | G1 = A (natte glanzende texture als tell); akkoord main |

## Backlog
- `[textures]` Flora `verdant_falls` in-game beoordelen bij teststop 5b: de lage varen leest op 16 px wat druk, de kruin in een echte schermkroon (motief), de gloed van orchidee en bromelia 's nachts, het halfrijpe peulstadium (olijfkleurig), het reuzenblad als dubbele plant naast de boomvarens.
- `[textures]` `sucking_mud` in-game naast `mud` en veen beoordelen: is de glans genoeg als tell zonder op een bewegend patroon te lijken (5b).
- `[blocks]` B bevestigt de paden en vormen (dubbele planten `_bottom`/`_top`, `verdant_wall_bromeliad` deelt de bromelia-textures, liaan kop/stengel × `flowering`, peul op de cacao-parents, bladeren en waterplant zonder tint); T past aan als B een andere vorm kiest.
- `[atmosphere]` `bubble_spring` "eigen bel-particle" (golf-2.md §2, B 72): geen eigen particle type in contract §6; A kiest of `updraft_bubble` volstaat of dat er een sprite bij moet (T levert dan).
- `[infra]` (bestaand) ontdubbeling texturescripts: `Ring4Textures.java` groeit verder (nu ~3.900 regels).
