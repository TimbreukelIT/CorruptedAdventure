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

## CODEBASE-delta
_(wordt aangevuld bij de bouw)_

## Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-16 | textures | Q-T1 A: binnenhout `verdant` (gestript, planken, deur, valluik) bleek warm honingcrème, een lichtere tint uit de zandfamilie van de stam | Besluit gebruiker; leest als tropisch hardhout en blijft los van het roze-crème van `moonpetal` |
| 2026-10-16 | textures | Q-T2 A: kleur per soort: orchidee en hangorchidee lila, bromelia abrikoos (schutbladen), bloeiende struik perzik, bloeiende liaan perzik met wat lila, waterplant bleek perzik-wit; vruchtpeul jong mintgroen → rijp abrikoos-oranje, `verdant_fruit` abrikoos-oranje | Besluit gebruiker; elke soort in één oogopslag herkenbaar |
| 2026-10-16 | textures | Q-T3 A: gloei-overlay in de eigen bloemkleur, lichter (lila gloed op de orchidee, abrikoos op de bromelia; lip/hart en punten) | Besluit gebruiker; duidelijk anders dan de blauwe gloeiflora van de mythische band |
| 2026-10-16 | textures | Q-T4 A: onderbegroeiing (varens, reuzenblad, bladeren van bromelia en orchidee, liaan, struik) vaste kleuren in de pastel mint/zeegroen-familie, een stap dieper dan de kruin; geen biome-tint | Besluit gebruiker; sluit aan op de kruin, B hoeft geen `tintindex` te zetten |
| 2026-10-16 | textures | `sucking_mud` glanzender (natte glanslichten, natte kuilen, twinkelende glinsteringen) | G1 = A (natte glanzende texture als tell); akkoord main |

## Backlog
_(wordt aangevuld bij de bouw)_
