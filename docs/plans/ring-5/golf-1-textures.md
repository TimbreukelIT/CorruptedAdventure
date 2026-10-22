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
- Voortgang: 2026-10-21 20:46 worktree `P:/Projecten/Java/wt/r5-textures-g1` op `ring5/textures-g1` vanaf `3dfd50aa`.
- Voortgang: 2026-10-21 20:59 `Ring5Textures.java` klaar: 47 PNG's + 3 `.mcmeta`'s, lint 0 waarschuwingen, twee runs
  byte-identiek, contactblad bekeken en bijgestuurd (aders, zand, voidsteen, nulliet, glitchblok).

## CODEBASE-delta
- **Nieuw:** `tools/textures/Ring5Textures.java` — single-file generator voor ring 5 + Nether-5 (37b-stijl, alleen JDK,
  seed = hash van de naam). Draaien: `java tools/textures/Ring5Textures.java [--vanilla <resources-jar>]` vanuit de
  repo-root; zonder argument zoekt hij de jar in `build/moddev/artifacts/` (alleen voor het contactblad; zonder jar een
  kruis i.p.v. vanilla). Contactblad `build/texture-preview/ring5.png` (niet in git): corrupted naast vanilla (6×6),
  overige muren (ook 's nachts), drie scènes (binnenband, void-scheur 's nachts, witte hal + spiegelvloer), houtset,
  items in een slot, particles, en per texture alle frames.
- **Textures** (onder `assets/corruptedadventure/textures/`):
  `block/` corrupted_grass_block_top, corrupted_grass_block_side, corrupted_dirt, corrupted_stone,
  corrupted_cobblestone, corrupted_sand, corrupted_log, corrupted_log_top, stripped_corrupted_log,
  stripped_corrupted_log_top, corrupted_planks, corrupted_leaves, corrupted_door_top, corrupted_door_bottom,
  corrupted_trapdoor, glitch_block (+mcmeta), voidstone, voidstone_glow (+mcmeta), nullite_ore_overlay (+mcmeta),
  chalkrack, white_stone, polished_white_stone, white_stone_bricks, white_stone_pillar, white_stone_pillar_top,
  mirror_stone, still_light, white_core_ore_overlay; `item/` corrupted_door, corrupted_sign, corrupted_hanging_sign,
  glitch_shard, raw_nullite, white_core; `entity/signs/corrupted` en `entity/signs/hanging/corrupted` (64 × 32, eerste
  bord-textures van de mod), `gui/hanging_signs/corrupted`; `particle/` void_mote_0..3, glitch_pixel_0..3,
  reverse_raindrop_0..1.
- Controles in het script: afmeting (16², particles 8², borden 64 × 32), alpha 0/255, alfamasker bij interpolatie,
  opake `cycling`, `checkGlowLayers` (voidsteen ↔ `voidstone_glow`), `SIDE_RULES` (gras-zijkant = `corrupted_dirt` vanaf
  rij 6), `lint` met `FLAT_OK` (`glitch_block`).

## Beslissingen
- **Paletten** (donker → licht): gras `4E534C…8B9184`, aarde `3B3836…77706A`, steen `52515A…97969C` (voeg `2E2C34`),
  zand `A29F99…DAD8D1`, schors `2F2B2D…6B6465`, hout `56524F…9D9791`, blad `2B2F2B…6B7167`; aders `1A0E24 2A1838 3A1F52`,
  violette kern van de kopse kant `2A1838 3A1F52 4E2C6E`, kettingen `1C1C22…5E5E68`. Void `07050A…231A31` + diepte
  `24173A 30204E`, sterren `C8B4F0 E8DCFF FFFFFF` (dim `6A5498`). Nulliet: lijf `120A1E 1C1030 281644`, rand `40246A
  7444BC 8A4FD8`, glans `C6A6FF`, omlijning `1E1428`/`06030A`; ruw `0A0612…42286A` + `8A4FD8`. Kalkrack `9EA3AD…E9EBEE`
  (spleet `878C97`), witsteen `B8BCC4…FFFFFF` (voeg `A9AEB8`), spiegelsteen `AAB3C1…FFFFFF`, stil licht
  `C8CDD7…FFFFFF`, witte kern parel `C4C2D8…FFFFFF` (+ `FFEEF4`, `EAF4FF`), omlijning `9CA0B4`/`80859A`, halo
  `F9FBFF`/`EEF2FA`.
- **Aders:** 1-px lijn die elke stap één pixel vooruit gaat (een van 8 richtingen, ~1/3 van de stappen 45° opzij), één
  gat, donker in het midden; alleen over de eigen materiaaltonen (nooit in een gat of over een lijst). Steen/aarde/gras
  1–2 per tegel, gestript hout en deuren/luik 1, planken op 2–3 van de 4 planken (één per plank las als streepjesvloer),
  keien: in de voegen (stukken voeg violet-zwart), zand: 3 losse vlekjes zonder gedeelde rij/kolom.
- **Glitchblok:** 8 frames, `frametime` 3, niet geïnterpoleerd, opaak; toestandsvolgorde `0 0 0 1 2 2 3 4` (houdt,
  springt, hapert). Vlak 8 × 8-schaakbord zonder bevel (bevel las als knoppen, niet als missing texture) + banden
  verschoven, 4 × 4-blokken verplaatst/omgekeerd, een gedimde rij, soms een cyaan/witte balk. In `FLAT_OK` (twee
  vlakke kleuren is de bedoeling).
- **Voidsteen:** 4–5 sterren, één met kruisje, nooit op dezelfde rij/kolom. `voidstone_glow` **twinkelt per ster**
  (8 frames × 8 ticks, geïnterpoleerd, elke ster eigen fase naar de dimtoon) i.p.v. de gelijke `pulsing` van ring 4.
- **Nulliet-overlay:** 3 hoekige kristallen; lichtzijde-rand `8A4FD8`, schaduwzijde-rand `40246A`, lijf violet-zwart,
  glans; puls `dim 3A2260` → alleen rand en glans bewegen (zacht, `glow D2B8FF`), het lijf blijft donker. Ruw nulliet
  mat (geen puls).
- **Witte kern:** overlay en item **stil** (geen puls): Nether-5 is stilte/orde en het plan vraagt alleen bij nulliet een
  puls. 1–3 parels per tegel (vaak 1–2: zeldzaam materiaal), grijsblauwe omlijning zodat ze op witsteen leesbaar zijn,
  1-px witte halo erbuiten (gloeit mee als B de overlay emissive maakt).
- **Spiegelsteen:** zachte diagonale glans + één onderbroken reflectiestreep, beide periodiek in x en y over 16 px
  (vloer zonder naad). Een tweede streep maakte de vloer te druk.
- **Borden:** UV-indeling van vanilla's `SignRenderer` (bord 24×12×2 op (0,0), paal 2×14×2 op (0,14)) en
  `HangingSignRenderer` (balk 16×2×4 op (0,0), kettingen 3×6 op (0,6) en (6,6), V-kettingen 12×6 op (14,6), bord 14×10×2
  op (0,12)); alleen het alfamasker van vanilla als referentie bekeken, eigen pixels.
- **Particles:** eigen kleuren (niet grijs voor tint), zoals `glitch_speck`; A kan ze in 98 nog tinten.
- **Contactblad:** vanilla-grijsbeelden (steen, gras, blad) worden rechtstreeks uit de raster gelezen; Java's `getRGB`
  maakt grijs-PNG's lichter (lineaire grijsruimte).
- **`/code-review` (medium):** 10 bevindingen, geen die een texture in de game breekt. Opgelost: graanlengte bordvlak
  één keer getrokken, deuradern wrapt niet meer, sterren/zandvlekjes ook los van kruisarmen/tweede pixel, twee
  dode plekken op het blad nooit op dezelfde pixel, dode glitch-takken weg, `main` gebruikt `animationStrip`, onbekende
  argumenten falen. Bewust gelaten: de twinkel-painter rendert voidsteen per frame opnieuw (een scriptrun, verwaarloosbaar)
  en de helperkopieën (staat al in de backlog, `infra`). Na de fixes opnieuw twee runs byte-identiek.
- `./gradlew build`/`runData` niet gedraaid (alleen PNG's/mcmeta's, geen code in de mod; INT1 dekt het).

## Backlog
- `textures`/`infra`: `VanillaCompare.java` leest vanilla met `getRGB`, waardoor grijs-PNG's (steen, gras, blad, …) te
  licht in `ring2/3-vs-vanilla.png` staan; de raster-fix uit `Ring5Textures.Vanilla.rawArgb` overnemen.
