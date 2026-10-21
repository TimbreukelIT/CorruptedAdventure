# Ring 5 golf 1 — stroom T (textures): 99a eerste goede versies

Scope: `textures` — pakket 99a, één commit (`textures:`), branch `ring5/textures-g1`, worktree
`P:/Projecten/Java/wt/r5-textures-g1`, poort n = 6 (geen server nodig), basis = de startcommit van `golf-1.md`.

**Doel:** direct goede eerste versies van alle ring 5- en Nether-5-blok- en itemtextures uit `contract.md` §3/**§11**
(88 met de volledige houtset en de bordjes, 89, 90) en de particle-sprites uit §6, in de 37b-stijl (`fase-1-wereld.md`
§1.4), plus een contactblad voor de gebruiker. Teststop 6a/6b beoordeelt ze in-game; 99b/99 sturen bij.

Bronnen: `fase-1-wereld.md` §1.4 en "Ring 5" (blokstijl), sparring ronde 3 ("grijs uitgeloogd met dunne violet-zwarte
aders; glitchblok missing-texture paars-zwart, geanimeerd; voidsteen zwart met sterrenpixels; Nether-5 wit/koel,
spiegelsteen glanzend; witte kern zachte witte gloed"), **§11: nulliet = donkere violet-zwarte kristallen met een
violette rand, zwak emissive zodat ze in grotten net opvallen**; sjabloon `tools/textures/Ring4Textures.java` (en
`Ring3Textures.java`: `Animation`, `still`/`pulsing`/`cycling`, `glowLayer` + `checkGlowLayers`, `bestOf`, `Cells`,
`outlineSelective`, `sprite`, hout-/deurhulpen, `chunks`/`nugget`, `lint`, contactblad), `Ring2Textures.java`
(`SIDE_RULES`), `VanillaCompare.java` (vanilla alleen als referentie uit de moddev-jar, nooit in git).

**Bezit:** `tools/textures/Ring5Textures.java` (nieuw), PNG's en `.mcmeta`'s met ring 5-/Nether-5-namen (lijst §2).
**Niet:** models, blockstates (B), particle-registratie en `particles/*.json` (A, 98).

---

## 1. Werk
1. **`tools/textures/Ring5Textures.java`:** single-file Java, alleen JDK, vaste seed per bestand (hash van de naam). Helpers
   gekopieerd uit `Ring4Textures.java` (een single-file script kan geen tweede bronbestand laden; ontdubbeling staat al in
   de backlog). Draaien met `java tools/textures/Ring5Textures.java` (geen Gradle-slot nodig).
2. **Alleen PNG's en `.mcmeta`'s** onder `src/main/resources/assets/corruptedadventure/textures/{block,item,entity,gui,
   particle}/`.
3. **Controles** vóór het wegschrijven: afmeting (16 × 16; bordjes 64 × 32; `gui/hanging_signs` 16 × 16; particles 8 × 8),
   alpha 0/255, gelijk alfamasker bij geïnterpoleerde animaties, opake `cycling`, `checkGlowLayers` (gemarkeerd ⇔
   gloeitoon, niet leeg), `SIDE_RULES` (zijkant gras = `corrupted_dirt` onder de korst). Daarna `lint` (vlakheid) en het
   contactblad `build/texture-preview/ring5.png`.

**Paletrichting** (voorstel, donker → licht; T verfijnt en noteert de definitieve paletten):
- *Corrupted* = de vanilla-indeling van de tegenhanger, **grijs uitgeloogd** (verzadiging ~10–15 % van vanilla, iets koel),
  met **dunne violet-zwarte aders** (1 px, `1A0E24`–`3A1F52`, 1–3 per tegel, onderbroken, nooit een raster).
- *Glitch* = missing-texture magenta `F800F8` / zwart, maar verschoven en kapot (blokken die verspringen, scanlines).
- *Void* = bijna zwart `08060C`–`1A1622` met violette diepte en losse sterpixels (wit/lichtviolet, emissive).
- *Nether-5* = wit en koel (`E8E8EC`–`FFFFFF` met grijsblauwe schaduw `B8BCC4`), **bijna textuurloos** (orde, stilte):
  minder ruis dan vanilla, wel 5+ kleuren (vlakheidscontrole; uitzonderingen met reden in `FLAT_OK`).

## 2. Textures (paden onder `textures/`; gelijk aan `golf-1-blocks.md` §5)

**88 — corrupted varianten en houtset**
| Pad | Aanpak |
|---|---|
| `block/corrupted_grass_block_top` | vanilla-gras-indeling, grijs-groen uitgeloogd (vaste kleur, **geen tint**), enkele violet-zwarte adertjes |
| `block/corrupted_grass_block_side` | grijze grasrand boven op exact `corrupted_dirt` (`SIDE_RULES`), geen overlay |
| `block/corrupted_dirt` | vanilla-aarde-indeling in grijsbruin, dunne aders |
| `block/corrupted_stone` | vanilla-steen-indeling, koud grijs, aders als haarscheuren; `bestOf` |
| `block/corrupted_cobblestone` | keien (cellen) met donkere voeg; aders in de voegen |
| `block/corrupted_sand` | zandkorrel grijsbeige, vlekjes violet-zwart |
| `block/corrupted_log`, `_log_top` | eikenschors-indeling grijs, aders in de groeven; kopse kant grijze jaarringen met een violette kern |
| `block/stripped_corrupted_log`, `_log_top` | grijs hout (`strippedLog`/`strippedLogTop`) met een enkele ader |
| `block/corrupted_planks` | eikenplanken-indeling grijs, één ader per plank op wisselende plek |
| `block/corrupted_leaves` | eikenblad-indeling, grijs en dunner (meer gaten, cutout_mipped), **geen tint** |
| `block/corrupted_door_top`, `_door_bottom`, `item/corrupted_door`, `block/corrupted_trapdoor` | houtset-hulpen (ring 2–4) in het grijze hout |
| `item/corrupted_sign`, `item/corrupted_hanging_sign` | zoals vanilla-bordjes, grijs hout; hangend bord met kettinkjes |
| `entity/signs/corrupted` (64 × 32), `entity/signs/hanging/corrupted` (64 × 32), `gui/hanging_signs/corrupted` (16 × 16) | **nieuw soort texture in de mod**: de vanilla-UV-indeling (bordvlak, paal, kettingen); vanilla alleen als referentie via `VanillaCompare` (layout), eigen pixels |

**89 — glitch, void, nulliet**
| Pad | Aanpak |
|---|---|
| `block/glitch_block` + `.mcmeta` | missing-texture-schaakbord magenta/zwart dat **kapot** is: vakken die per frame verspringen, horizontale scanline-verschuivingen, af en toe een vak in een vreemde kleur; `cycling`, opaak, 8 frames, `frametime` 2–4 (onregelmatig gevoel door frame-herhaling) |
| `item/glitch_shard` | hoekige scherf in magenta/zwart met een verschoven pixelrij; `outlineSelective` |
| `block/voidstone` | bijna zwart met violette diepte (zachte vlekken), geen ruis-grijs |
| `block/voidstone_glow` (+ `.mcmeta` als ze twinkelen) | **sterrenpixels** (4–7 per tegel, 1 px, een paar met een kruisje van 1 px), emissive (37b-regels); mag traag twinkelen (`pulsing`) |
| `block/nullite_ore_overlay` (+ `.mcmeta`) | **donkere violet-zwarte kristallen met een violette rand** (`chunks` met facetten, rand `8A4FD8`-achtig), **zwak emissive** (alleen rand en facetlicht, zachte puls zoals tidesilver), over `corrupted_stone` en `voidstone`; leesbaar op beide |
| `item/raw_nullite` | ruwe brok (`nugget`) violet-zwart met een violette rand, mat |

**90 — Nether-5**
| Pad | Aanpak |
|---|---|
| `block/chalkrack` | netherrack-indeling in krijtwit (kalkrack), koude schaduw; duidelijk anders dan het beige `bleached_netherrack` |
| `block/white_stone` | glad wit, bijna zonder textuur, zachte grijsblauwe vlekken |
| `block/polished_white_stone` | als `white_stone`, vlakker, vanilla-bevel |
| `block/white_stone_bricks` | vanilla `stoneBricks`-indeling, lichtgrijze voeg |
| `block/white_stone_pillar`, `_pillar_top` | `quartz_pillar`-indeling: verticale groeven; top met ringen |
| `block/mirror_stone` | **glanzend**: diagonale lichtstreep (spiegeling), heel licht grijswit met een koel blauwe zweem; tegelbaar zonder naad |
| `block/still_light` | wit, zacht gloeiend vlak (geen flikkering, geen animatie: "stil") |
| `block/white_core_ore_overlay` | wit-parelmoer kern met **zachte witte gloed** (emissive halo, 1 px rand), over `white_stone` |
| `item/white_core` | gepolijste witte kern met zachte gloed; `outlineSelective` |

**Particle-sprites** (voor A in 98; nu al leveren, geen registratie)
| Pad | Aanpak |
|---|---|
| `particle/void_mote_0..3` | kleine zwart-violette stofjes met een lichter hart, groeiend/krimpend |
| `particle/glitch_pixel_0..3` | 1–3 px magenta/zwart/cyaan blokjes, per frame anders |
| `particle/reverse_raindrop_0..1` | zwarte druppel met violette glans, punt naar boven |

## 3. Contactblad
`build/texture-preview/ring5.png` (niet in git): 6 × 6 muren van elk blok; de corrupted varianten **naast hun vanilla-
tegenhanger** (vanilla uit de moddev-jar via `VanillaCompare`-logica, alleen in het blad); een binnenband-scène (gras,
aarde, steen, dode boom van stam en blad), de houtset (deur, luik, hek, bord), voidsteen en nulliet in een donkere scène
(nacht, de emissive pixels zichtbaar), Nether-5 (vloer spiegelsteen, witsteenwand, zuil, stil licht); items in een slot;
de particles vergroot. Main legt het blad aan de gebruiker voor (pad melden).

## 4. Client/server-impact
Alleen assets (client-resources); geen code in de mod, geen payload. Het script draait buiten de mod.

## 5. Tests en "af als"
- Het script draait foutloos (controles van §1.3 en `lint` zonder onverklaarde waarschuwingen).
- **B** `./gradlew build` en **D** `runData` één keer aan het eind (geen eigen datagen; `src/generated` ongewijzigd) —
  alleen als er een Gradle-slot vrij is (licht; mag ook door INT1 gedekt worden).
- **Af als:** alle paden van §2 bestaan (lijst in het eindrapport, met het aantal PNG's en `.mcmeta`'s), contactblad
  gemaakt, `/code-review` verwerkt.

## Voortgang

## CODEBASE-delta

## Beslissingen

## Backlog
