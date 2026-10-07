# Golf 1 — stroom T (textures): 57a eerste goede versies

Scope: `textures` — werkpakket 57a; branch `ring3/textures-g1`. (57 = de eindronde na teststop 4b.)

**Doel:** direct goede eerste versies (antwoord 20) van alle ring 3- en Nether-3-textures uit `contract.md` §3 en §6 (particles), in de 37b-stijl. Teststop 4a beoordeelt het wow ook op kleur en gloed. Er is geen placeholder-ronde.

Bron: `fase-1-wereld.md` §1.4 (stijl, inclusief de aanvullingen van 37 en 37b), Ring 3/Nether-3-secties; `contract.md` §7; sjabloon `tools/textures/Ring2Textures.java` (`Animation`, `still`/`pulsing`/`cycling`, `sprite(rows, legend)`, `SIDE_RULES`, `lint`, contactblad) en `tools/textures/VanillaCompare.java`.

## Werk
1. **`tools/textures/Ring3Textures.java`:** single-file Java, alleen JDK, vaste seed per bestand. Helpers kopiëren uit `Ring2Textures.java`. De ontdubbeling staat in de backlog (r. 48/151/166) en is een aparte `infra`-keuze; hier niet doen tenzij main dat vraagt.
2. **Paletten** (5–7 tonen, hue-shift):
   - as (warm grijs) en puimsteen (licht, poreus);
   - smeulsteen en `smoldering_ash` (zwart met oranjerode emissive scheuren, puls);
   - verkoold hout (zwart, schors met gloeiende kerven op de log-zijkant?, als accent);
   - `ember_leaves` (donkere kronen met gloeiende vlekjes, emissive);
   - vuurflora; vulkaniet (donker metaal met paarsrode glans), zwavel (geel), vuurrobijn (diep rood, emissive puls);
   - vuurkristal (oranje-goud, emissive);
   - zwavelkorst, fumarole, kokende modder (`cycling`), geiser;
   - Nether-3: koudlava (grijsblauw gestold), Nether-ijs, rijp-netherrack (netherrack-toonladder met rijp, les 37b: vanilla-ertsen moeten erin passen), rijpzwam, ijsstalactiet, gloeiende scheur (emissive), oerijs (helder, bijna wit-blauw, licht pulserend?).
   - Gloed en puls volgens §1.4.
3. **Particles:** `ash_flake_0..n`, `ember_0..n`, `steam_puff_0..n`, `ice_glint_0..n` (grijswaarden als ze getint worden; afstemmen met A via main).
4. **Controles:** afmeting, alpha, `cycling` opaak, `SIDE_RULES`, `lint` (vlakheid), contactblad `build/texture-preview/ring3.png` met secties RING 3 / NETHER RING 3, plus `VanillaCompare` voor rijp-netherrack met vanilla-ertsen.
5. Alleen PNG's en `.mcmeta`'s onder de contractnamen; **geen** models of blockstates (die zijn van B).

## Client/server-impact
Geen (assets).

## Tests
- Script 2× → md5 identiek; lint 0.
- **B** (assets kapot = geen B-fout, maar draai hem toch).
- Na integratie met B: `runClient` zonder `Missing textures`.
- Contactblad aan main (main legt het aan de gebruiker voor vóór 4a).

## Bij afronding
`waves.md` §4.

## Voortgang
- Voortgang: 2026-10-06 ~20:30 branch `ring3/textures-g1` (gereset naar 8e0c637), namen + V-T1..3 aan main; antwoorden: V-T1 A (twee lagen), V-T2 A (Nether-ijs opaak), V-T3 particles in lichte grijswaarden (ash_flake, ember; steam_puff/ice_glint golf 2).
- Voortgang: 2026-10-06 ~20:55 `Ring3Textures.java` + `VanillaCompare.java ring3`: alle 90 PNG's + 13 `.mcmeta`'s eerste versie, lint 0; tussenstand batch 1 aan main (`build/texture-preview/57a-batch1.png`, `ring3-vs-vanilla.png`).
- Voortgang: 2026-10-06 21:10 feedback main verwerkt: smeulsteen zonder herhaald motief (gloei-aandeel vast via rangorde, verspreid, `bestOf` met `stripeScore` + `motifScore`), gloeiende scheur als onregelmatig scheurnetwerk dat op vaste randpunten aansluit, zwavelkorst een toon rustiger; daarnaast smeulblad als blaadjes, vuurkristal-item als één kristal. Script 2× → md5 identiek.
- Voortgang: 2026-10-06 21:40 `/code-review` medium binnen (10 bevindingen): 8 opgelost, 2 vastgelegd (zie Review); script 2× → md5 identiek, lint 0.

## Afwijkingen bij de bouw
- **Gloeilagen (V-T1 A):** `smoldering_stone`, `smoldering_ash`, `ember_leaves`, `glowing_fissure` hebben elk `<naam>` (volledig, still) + `<naam>_glow` (cutout, alleen de gloeipixels, puls 8×4 interpolate). De schilder markeert gloeipixels expliciet (`glow(image, x, y, rgb)` → masker per image); de gloeilaag neemt precies die pixels. `checkGlowLayers` faalt als een gemarkeerde pixel geen gloeitoon heeft, als een niet-gemarkeerde pixel wél een gloeitoon heeft, of als de laag leeg is.
- **`glowing_fissure` sluit aan op buren:** de horizontale scheur raakt de linker- en rechterrand op rij 9,5, de verticale de boven- en onderrand op kolom 6,5 (`FISSURE_ROW`/`FISSURE_COLUMN`), als haarlijn (`FISSURE_EDGE_WIDTH` 0,3), zodat aangrenzende scheurblokken in elke richting één scheur vormen en een los blok naast koudlava geen afgesneden lavastomp toont; zijtakken blijven binnen de tegel. Op een volle 6×6-muur geeft dat een kronkelend netwerk; bedoeld voor lijnen/eilandjes in de vloer (contactblad "Nether ring 3 floor", dag en nacht).
- **Ertsen:** één overlay per erts, gedeeld door alle gastgesteenten (volcanite: stone/deepslate/basalt; zwavel: stone/basalt; vuurrobijn: stone/deepslate; geen blackstone-variant). Vulkaniet en zwavel mat (still), vuurrobijn puls (+ emissive in B's model).
- **Vuurkristal:** blok, vier stadia en item pulseren (geen aparte gloeilaag: het hele kristal is emissive, B's keuze in het model). Item = één schuin kristal (een stuk van een cluster), niet de cluster zelf.
- **`ember_berry_bush`:** stadia 0–3 als vanilla's bessenstruik; stadium 2 en 3 delen de struik van stadium 1 en dezelfde besplekken (eigen vaste seed `block/ember_berry_bush_berries`). Item = `block/ember_berry_bush_stage3` (B).
- **`steam_puff_0..3` en `ice_glint_0..3`** zijn getekend (8×8; steam grijswaarden, ice_glint eigen wit-blauw) maar worden pas in golf 2 door A geregistreerd.
- **`VanillaCompare.java`:** argument `ring2` (standaard, uitvoer als voorheen) of `ring3`; nieuwe referentievorm `overlay@base` (erts-overlay over zijn vanilla-gastgesteente, zoals het tweelaagsmodel). Alleen de wandkop-tekst van ring 2 is anders (noemt nu de ertsnamen uit de lijst).
- **`.mcmeta`'s van ring 2:** `Ring2Textures.java` opnieuw draaien geeft alleen een LF/CRLF-verschil (`core.autocrlf`), geen inhoud; teruggezet, niet meegecommit.
- **Lint `FLAT_OK`:** leeg; eindstand 0 waarschuwingen. Gloeilagen en particles vallen buiten de lint (alleen scheurpixels / 8×8).
- **Rijp-netherrack zonder witte glinsters:** één witte pixel per tegel las op de muur als een raster van stippen; nu alleen mauve rijp (`RIME`) op de belichte kant van ~3/5 van de klonten. Gemiddelde helderheid 0,211 (vanilla-netherrack 0,200).
- **Gestripte kopse kanten:** `strippedLogTop(…, wood, bark)` maakt alle schorspixels hout (de houtskoolring van `charred_log_top` is twee pixels breed en dieper in de hoeken), dan een houtrand.
- **`VanillaCompare` toont van een animatiestrip het frame op een kwart van de cyclus** (frame 0 van een puls leunt 12 % naar de dimtint en oogt doffer); geldt ook voor het ring 2-blad.

## Review (`/code-review` medium, 10 bevindingen)
- Opgelost: 1 `stripped_charred_log_top` hield een houtskoolring (nu alle schorstonen → hout); 2 lavastomp aan de rand van een los scheurblok (randbreedte haarlijn, takken binnen de tegel); 3+4 `checkGlowLayers` was een tautologie en de gloeilaag hing af van kleurlidmaatschap (nu expliciete gloeimarkering + controle in beide richtingen); 7 javadocs rijp-netherrack/`bestOf`; 8 `VanillaCompare` liet pulserende textures in het donkere frame 0 zien (nu kwart cyclus); 9 de ertsmuur van ring 3 bevatte eigen blokken (nu alleen vanilla: kwarts, goud, debris, magma, blackstone, packed ice); 10 `Math.floorMod` in de spetters van kokende modder.
- Niet gedaan, met reden: 5 `CODEBASE.md`/beslissingenlog/backlog niet bijgewerkt — volgens `waves.md` §4.8 schrijft een stroom alleen delta-secties in het eigen plan; de integratie voegt ze samen. 6 gekopieerde helpers (~600 regels) — bestaande `[infra]`-backlogregel (ontdubbeling texturescripts), hier bewust niet (plan §1).

## CODEBASE-delta
- `tools/textures/Ring3Textures.java` (nieuw, 57a): texturescript ring 3 + Nether-ring 3, zelfde opzet als `Ring2Textures.java` (helpers gekopieerd: `Animation`, `still`/`pulsing`/`cycling`, `check`, `lint`, `Cells`, `outlineSelective`, `sprite`, `merge`, `stripeScore`, `sharesLine`, `insidePolygon`/`edgeDistance`, hout-/deurhulpen, `pulseStrip`/`cyclingStrip`). Nieuw: `glowLayer(block, tones, glow, dim)` (registreert `<block>_glow`: de pixels die de schilder met `glow(image, x, y, rgb)` markeerde, pulserend; masker per image in `GLOW_MASKS`, `copy` neemt het mee) + `checkGlowLayers` (gemarkeerd ⇔ gloeitoon, niet leeg); `strippedLogTop(…, wood, bark)` (alle schorstonen → hout); `bestOf(image, random, layout)` (8 lay-outs uit de seed, houdt de laagste `stripeScore + motifScore`; `motifScore` = grootste helderheidsverschil tussen de zestien 4×4-blokken van een tegel); `ranked(field)` (rangorde, zodat een vast aandeel pixels boven een drempel ligt: gloei-aandeel smeulsteen `SMOLDER_HOT_SHARE`); `blend`; `pore` (vesikels puimsteen); `bevel` (gepolijste rand); `stoneBricks(palette, mortar)` (vanilla-`stone_bricks`-indeling, puimsteen en koudlava); `chunks` (hoekige ertsbrokjes, sjabloon zonnesteen 37b), `gem` (zeshoekige edelsteen), `bipyramid` (zwavelkristal), `nugget` (hoekige klomp, ruw vulkaniet), `flameSpike` (kristal met vlamgradiënt), `fissureSegments`/`crackPath`/`nearestSegment` (scheurnetwerk met wrap, randpunten `FISSURE_ROW`/`FISSURE_COLUMN`), `LEAFLETS` (smeulblad), `particle` + `ASH_FLAKES`/`EMBERS`/`STEAM_PUFFS`/`ICE_GLINTS`. Contactblad `build/texture-preview/ring3.png`: 6×6-muren (2×), nachtweergave (blok 20 % licht + gloeilaag vol), items in een slot, smeulwoud-scène, Nether-3-vloer dag/nacht, daarna per texture een cel (6×, 3×3 op 2×, frames) in secties RING 3 / NETHER RING 3.
- `tools/textures/VanillaCompare.java`: argument `ring2` (standaard) | `ring3` → `build/texture-preview/ring3-vs-vanilla.png` (muur rijp-netherrack met vanilla-Nether-ertsen, magma, blackstone en packed ice naast vanilla-netherrack; ertsen als `overlay@base` over stone/deepslate/basalt; paren eigen ↔ vanilla-buur; items en flora in een slot). Record `Sheet(output, wallHosts, wallOres, rows)`; van een animatiestrip het frame op een kwart van de cyclus.
- `textures/` (57a, uit `Ring3Textures.java`): `block/` ash_block (ook ash_layer), ash_crust, pumice, polished_pumice, pumice_bricks, smoldering_stone + `_glow` (puls), charred_log(_top), stripped_charred_log(_top), charred_planks, charred_door_{top,bottom}, charred_trapdoor, ember_leaves + `_glow` (puls; cutout), fire_bloom, ash_sprouts, ember_berry_bush_stage0..3 (cutout), smoldering_ash + `_glow` (puls), volcanite_ore_overlay, sulfur_ore_overlay, fire_ruby_ore_overlay (puls), fire_crystal_block (puls), small_/medium_/large_fire_crystal_bud, fire_crystal_cluster (puls, cutout), sulfur_crust, fumarole_top, fumarole_side, boiling_mud (`cycling` 16×3), geyser_top, geyser_side; Nether-3: cold_lava, polished_cold_lava, cold_lava_bricks, nether_ice (opaak), packed_nether_ice, rime_netherrack, rimeshroom_stem(_top), stripped_rimeshroom_stem(_top), rimeshroom_planks, rimeshroom_door_{top,bottom}, rimeshroom_trapdoor, rimeshroom_cap, ice_stalactite_{down,up}_{tip,tip_merge,frustum,middle,base} (`up` = `down` gespiegeld), glowing_fissure + `_glow` (puls), primordial_ice; `item/` charred_door, rimeshroom_door, raw_volcanite, sulfur, fire_ruby (puls), fire_crystal (puls), ice_stalactite; `particle/` ash_flake_0..3, ember_0..3 (lichte grijswaarden, getint door A), steam_puff_0..3, ice_glint_0..3 (golf 2). 90 PNG's, 13 `.png.mcmeta`'s. Stairs/slab/wall gebruiken de texture van hun basisblok.
- Valkuil: `git core.autocrlf=true` — een texturescript schrijft `.mcmeta` met LF; opnieuw draaien geeft voor git een "gewijzigd" bestand zonder inhoudsverschil. Niet meecommitten (`git checkout --`).

## Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-06 | textures | V-T1 A: gloei-blokken (smeulsteen, smeulas, smeulblad, gloeiende scheur) als twee lagen: `<naam>` + `<naam>_glow` (cutout, emissive, puls) | Alleen de scheuren/vlekjes gloeien 's nachts; het gesteente wordt gewoon donker |
| 2026-10-06 | textures | V-T2 A: `nether_ice` opaak, eigen rookgrijsblauw (donkerder en minder verzadigd dan vanilla-ijs) | Geen translucent-sorting over grote ijsvlaktes; §1.4 alpha 0/255 |
| 2026-10-06 | textures | V-T3: particles `ash_flake_0..3` en `ember_0..3` 8×8 in lichte grijswaarden (getint door de particle-klasse van A); `steam_puff`/`ice_glint` golf 2 | Afgestemd met A |
| 2026-10-06 | textures | Paletten ring 3 (implementatie, 57a): as warm grijs `3A3538…968C84`; puimsteen `77716A…DCD7CC`; smeulsteen-gesteente `151214…50423D` met gloed `701808 A82C0C DC5414 FA8828 FFC25A FFF0BC` (gloei `FFE4A0`); verkoold hout schors `110F11…4A423F` + zilverglans `56525C/726E7A`, hout `29201D…765D4C`; vulkaniet donker metaal met paarsrode glans; zwavel geel `4A3A08…FFFBD0`; vuurrobijn diep rood (gloei `FFA090`); vuurkristal vlam `581606…FFFBE0` (gloei `FFF2C0`); Nether-3: koudlava grijsblauw `22252D…78808D`, Nether-ijs `435C7A…C6DAEC`, rijp-netherrack = toonladder vanilla-netherrack + rijp `7C5664 A28C9C` (geen witte glinsters), rijpzwam koel blauw-lila, lava van de scheur `6E1608…FFF4C4`, oerijs `88B6DA…F6FBFF` | Eerste goede versies in 37b-stijl (antwoord 20); beoordeling in teststop 4a/4b |
| 2026-10-06 | textures | Smeulsteen: vast gloei-aandeel (`SMOLDER_HOT_SHARE` 0,4 van het scheurnetwerk), verspreid; lay-out gekozen op `stripeScore + motifScore` | Feedback main: geen herhaald scheurmotief op een muur |
| 2026-10-06 | textures | Gloeiende scheur: scheurnetwerk dat op vaste randpunten aansluit (rij 9,5 / kolom 6,5), zodat aangrenzende scheurblokken één scheur vormen | Feedback main: geen behang van zigzag-kolommen; scheuren lopen door in lijnen/eilandjes |

## Backlog
- `[textures]` Ring 3-textures na teststop 4a/4b bijsturen op het in-game beeld (57a-rondes, golf 2): vooral smeulsteen/gloeiende scheur in een echte vloer en smeulblad in een woud; de vier gloeilagen in Sodium/shaderpacks controleren (`neoforge_data` mogelijk genegeerd).
- `[infra]` (bestaand, backlog r. 48/151/166) ontdubbeling texturescripts: `Ring3Textures.java` kopieert weer ~600 regels helpers uit `Ring2Textures.java`.
- `[textures]` Golf 2: `steam_puff`/`ice_glint` afstemmen met A zodra die particles geregistreerd worden (tint of eigen kleur).
