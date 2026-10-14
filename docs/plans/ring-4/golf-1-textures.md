# Golf 1 — stroom T (textures): 71a eerste goede versies

Scope: `textures` — pakket 71a; branch `ring4/textures-g1` (vanaf `7ffbfeb`), poort n = 6 (geen server nodig).

**Doel:** direct goede eerste versies van alle ring 4- en Nether-4-blok- en itemtextures uit `contract.md` §3 (66, 67, 68 én de 72-blokken met een texture) en de particle-sprites uit §6, in de 37b-stijl (`fase-1-wereld.md` §1.4), plus een contactblad voor de gebruiker. Teststop 5a/5b beoordeelt ze in-game; 71b/71 sturen bij.

Bron: `fase-1-wereld.md` §1.4 en "Ring 4"; `contract.md` §3, §6, §7; sjabloon `tools/textures/Ring3Textures.java` (`Animation`, `still`/`pulsing`/`cycling`, `glowLayer`/`glow` + `checkGlowLayers`, `bestOf`, `Cells`, `outlineSelective`, `sprite`, hout-/deurhulpen, `chunks`/`nugget`/`flameSpike`, `lint`, contactblad) en `Ring2Textures.java` (`SIDE_RULES`).

## Werk
1. **`tools/textures/Ring4Textures.java`:** single-file Java, alleen JDK, vaste seed per bestand (hash van de naam). Helpers gekopieerd uit `Ring3Textures.java`, want een single-file script kan geen tweede bronbestand laden. De ontdubbeling is een bestaande `[infra]`-backlogregel en hoort hier niet.
2. **Alleen PNG's en `.mcmeta`'s** onder `src/main/resources/assets/corruptedadventure/textures/{block,item,particle}/`; **geen** models of blockstates (B), geen particle-registratie (A).
3. **Controles** (vóór het wegschrijven): afmeting (16×16, flow 32×32, particles 8×8), alpha 0/255, gelijk alfamasker bij geïnterpoleerde animaties, opake `cycling`, `checkGlowLayers` (gemarkeerd ⇔ gloeitoon, niet leeg), `SIDE_RULES` (zijkant = grond onder de korst). Daarna `lint` (vlakheid) en het contactblad `build/texture-preview/ring4.png`.

## Textures (paden onder `textures/`)

**66 — natuurblokken (ring 4)**
| Pad | Aanpak |
|---|---|
| `block/tidestone` | Blauwgrijs gelaagd: horizontale lagen (golvend, tegelbaar, 2 per blok) met uitgerekte korrel, donkere laagnaden, lichte slibkorrels; `bestOf` |
| `block/cobbled_tidestone` | Keien (cellen) met donkere voeg, licht linksboven, binnen elke kei een vage gelaagdheid |
| `block/polished_tidestone` | Glad, gevlekt, twee vage laaglijnen, vanilla-bevel |
| `block/tidestone_bricks` | Vanilla-indeling `stoneBricks` met donkere blauwgrijze voeg |
| `block/moonpetal_log`, `_log_top` | Wit/zilveren schors met lila-grijze schaduw, horizontale lenticellen (zoals berk, maar zilver), zilverglans; kopse kant: roze-crème jaarringen + zilveren rand |
| `block/stripped_moonpetal_log`, `_log_top` | Bleek roze-crème hout (`strippedLog`/`strippedLogTop`) |
| `block/moonpetal_planks`, `_door_top`, `_door_bottom`, `_trapdoor`, `item/moonpetal_door` | Houtset-hulpen (ring 2/3) in roze-crème hout met zilveren schors als lijst |
| `block/moonpetal_leaves` | Bloesemkruin roze-lila (vaste kleur, geen tint): bloesemtrosjes met lichte hartjes, dieper lila in de schaduw, gaten (cutout_mipped) |
| `block/moonpetal_sapling` | Zilveren stammetje met roze-lila bloesemkruintje (kruis; ook de potted variant) |
| `block/withered_moonpetal_log`, `_log_top`, `block/stripped_withered_moonpetal_log`, `_log_top` | Grijs, dood: verticaal gescheurde, afbladderende schors; grijsbeige hout met radiale droogscheuren |
| `block/reeds`, `block/reeds_top` | Riet (kruis): stengeldeel dat verticaal aansluit + bovendeel met pluimen (stro-groen, pluimen bruin-beige); groeit als suikerriet tot 3 hoog, item = `reeds_top` (besluit gebruiker via B) |
| `block/peat` | Donkerbruin vezelig veen, horizontaal gelaagd, lichte worteldraadjes, natte donkere plekken |
| `block/hanging_swamp_moss` | Grijsgroene hangende slierten (cutout) |
| `block/giant_lily_pad_north_west`, `_north_east`, `_south_west`, `_south_east`, `item/giant_lily_pad` | Eén rond blad van 32×32 met inkeping en nerven vanuit het midden, opstaande lichte rand; per deel één kwadrant (bovenaanzicht, noord = boven) |
| `block/lotus` | Drijft plat op het water zoals een lelieblad (besluit gebruiker via B): bovenaanzicht van een rond blad met inkeping en een open bloem (acht roze bloemblaadpunten, witte binnenkrans, goudgeel hart); item = deze texture |
| `block/glowcap` + `_glow` | Drie gloeizwammen: paarse hoed met blauwe gloeivlekken en gloeiende rand; gloed in twee lagen |
| `block/glow_bloom` + `_glow` | Violette bloem met een blauwwit gloeiend hart en gloeiende bloemblaadpunten |
| `block/glowing_hanging_roots` + `_glow` | Bruingrijze hangwortels met gloeiende knopen en puntjes |
| `block/blighted_glowcap` + `_glow` | Zelfde vorm als `glowcap`, ziekelijk groen/grijs, weinig en zwakke gloeipixels |
| `block/bubble_spring_top`, `block/bubble_spring_side` | Top: minerale korst met bronopeningen en bellenringen; zijkant: korst bovenaan, daaronder exact `tidestone` (`SIDE_RULES`) |

**67 — ertsen en kristallen**
| Pad | Aanpak |
|---|---|
| `block/tidesilver_ore_overlay`, `item/raw_tidesilver` | Hoekige metaalbrokjes (`chunks`) zilver met blauwgroene glans; de overlay pulseert zwak (B: zwakke gloed, licht 3, emissive zoals vuurrobijn); ruwe brokken (`nugget`) mat |
| `block/jade_ore_overlay`, `item/jade` | Melkachtige afgeronde knollen (zacht verloop, glans), mat; item een gepolijste cabochon |
| `block/toxic_crystal_block`, `block/budding_toxic_crystal`, `block/small_/medium_/large_toxic_crystal_bud`, `block/toxic_crystal_cluster`, `item/toxic_crystal_shard` | Amethist-patroon: kristalvlakken (cellen), budding met knopjes, 1/2/3/5 punten, scherf = één schuin kristal; fel zuurgroen |
| `block/aquamarine_block`, `block/budding_aquamarine`, `block/small_/medium_/large_aquamarine_bud`, `block/aquamarine_cluster`, `item/aquamarine_shard` | Idem in licht zee-cyaan |

**68 — Nether-4**
| Pad | Aanpak |
|---|---|
| `block/bone_sand` | Beige-wit korrelig zand met witte botsplinters en donkere korrels |
| `block/bleached_netherrack` | Netherrack-klonten (cellen), gebleekt bleekroze-botwit |
| `block/fossil_stone`, `block/polished_fossil_stone`, `block/fossil_stone_bricks` | Beige-grijs gesteente met kleine fossielfragmenten (ribbogen, schelpspiraal) in botwit; gepolijst met bevel; bricks |
| `block/purity_crystal_block`, `block/purity_crystal_cluster`, `item/purity_crystal_shard` | Melkwit kristal met lichtgouden glans |
| `block/purity_crystal_lamp` | Kristalruiten in een lijst van fossielsteen |

**72 — gameplayblokken (B registreert ze in golf 2)**
| Pad | Aanpak |
|---|---|
| `block/sucking_mud` | Groenzwarte natte modder met glans, langzaam zuigende kuiltjes (`cycling`, opaak) |
| `block/toxic_pool_still` (16×16-frames), `block/toxic_pool_flow` (32×32-frames, zoals vanilla), `item/toxic_pool_bucket` | Fel gifgroene stroperige vloeistof met bellen (`cycling`); emmer met gifgroene inhoud |
| `block/updraft` | Cutout: opstijgende waterstrepen en belletjes (niet-geïnterpoleerde animatie, het alfamasker mag per frame verschillen) |
| `block/clawed_tidestone` | `tidestone` met drie parallelle klauwkrassen |
| `block/slime_trail` | Platte cutout-decal: glanzend grijsgroen slijmspoor van de linker- naar de rechterrand (midden), zodat sporen aansluiten |
| `block/creature_nest` | Gevlochten takjes/wortels met riet, mos en een botsplinter |
| — `swamp_gas` | Geen texture (onzichtbaar) |

**Particles (§6, sprites voor A), 8×8, `particle/<naam>_0..3`:** `will_o_wisp`, `blossom`, `firefly`, `swamp_gas`, `toxic_bubble`, `updraft_bubble`, `glitch_speck`, `bone_dust`.

## Paletten (5–7 tonen, donker → licht, koele schaduwen, warme lichten)
Zie de constanten in het script; de hoofdlijnen:
- tidestone blauwgrijs `2E3644…87919E`; voeg `232A35`.
- moonpetal-schors zilverwit met lila schaduw `8A8496…F2F1F7`; hout roze-crème `9C7F86…F5E7E1`; bloesem roze-lila `6E4472…F6D8EE` (hartje `FFF2DC`).
- withered-schors grijs `3A3A3C…93918F`; hout grijsbeige `5E5852…A59B90`.
- gloeiflora: hoed paars `241C46…6E5CB2`, bloemblad violet `3A2266…A886DC`, wortels `2E241E…806652`, gloed blauw `2A4FB0…E6F6FF` (gloeikleur `CFEFFF`); bedorven: grijsgroen `30352A…777F62`, zwakke gloed `4E6424…BACD7A` (gloeikleur `C8DC8A`).
- veen `1E1612…654D3A`; riet stro-groen `3E4A22…B4BE6E`, pluim `5A4434…D8C2A4`; hangmos `3C4634…B3BC9C`; lelieblad `1A421C…78BA5E`; lotus `B0507A…FFFFFF` (hart `F2D04A`); bellenbron-korst `5E7480…DCE8EC`.
- (V-T1 A) tidesilver zilver met blauwgroene glans `16222C…F2FFFF` (gloei `D8FFF8`), jade koel melkachtig blauwgroen `0E2E26…C4E6D2`, gifkristal zuurgroen → geelgroen `14300A…EEFFB4` (gloei `E8FFB8`), aquamarijn zee-cyaan `0E2E3C…E4FBFC`, zuiverkristal melkwit met lichtgouden glans `7A6A48…FFFFFC` (gloei `FFF4D0`).
- gameplay: zuigmodder groenzwart `1A1C14…575E3E`, gifpoel `2E5A0C…A2E040` (glans `D4F870`), slijm `4E6A48…D6EAC4`, nest `1E1712…8A7256`, updraft `8CC8DC…E6F8FC`.
- Nether-4: bottenzand `9C9078…ECE6D2`, gebleekt netherrack `8A6E68…DFCFC8`, fossielsteen `6A6250…CAC1AC`.

## Vragen (2026-10-13, beantwoord; zie Beslissingen)
- V-T1 kleuren jade/aquamarijn/tidesilver/gifkristal/zuiverkristal; V-T2 welke materialen gloeien/pulseren; V-T3 gifpoel opaak of doorschijnend; V-T4 particles getint (grijs) of eigen kleur.
- Aan B (r4-blocks): modelafhankelijke paden (riet, lotus, lelie-delen, hangmos/-wortels, bellenbron, nest, slijmspoor, updraft, aquamarine-namen).

## Client/server-impact
Geen (assets).

## Tests
- Script 2× → md5 identiek; lint 0 (of `FLAT_OK` met reden).
- **B** (`./gradlew build`) groen; `runData` niet nodig (geen datagen geraakt).
- Na integratie met B: `runClient` zonder `Missing textures` (INT).

## Voortgang
- 2026-10-13: branch `ring4/textures-g1` vanaf `7ffbfeb`; plan, vragen V-T1..4 aan team-lead, padenlijst aan B.
- 2026-10-13: B bevestigt de paden (`part` = north_west/north_east/south_west/south_east; hangmos/-wortels één texture; bellenbron top/side; aquamarine-namen); besluiten V-T1..4 (alle A) en de tidesilver-afwijking van B binnen; riet en lotus besloten (via B).
- 2026-10-13: `Ring4Textures.java` eerste versie: 106 PNG's + 20 `.mcmeta`'s, lint 0, `checkGlowLayers`/`SIDE_RULES` groen; contactblad bekeken en een ronde bijgestuurd (zie Afwijkingen). Script 2× → md5 identiek; **B** groen.

## Afwijkingen bij de bouw
- **Gloeiflora (contract §7):** `glowcap`, `glow_bloom`, `glowing_hanging_roots`, `blighted_glowcap` elk `<naam>` + `<naam>_glow` (cutout, alleen de gloeipixels, puls 8×4 interpolate), zoals ring 3. De schilder markeert gloeipixels met `glow(...)`; `set(...)` wist de markering als een latere streek de pixel overschildert (anders hield de gloeilaag een bedekte pixel). `checkGlowLayers` controleert ook dat een gemarkeerde pixel niet transparant is.
- **Pulserend (V-T2 A + B):** gifkristal (blok, budding, 3 knoppen, cluster, scherf), zuiverkristal (blok, cluster, scherf, lamp) en de `tidesilver_ore_overlay` (zwak, B's keuze) pulseren 8×4; aquamarijn, jade en `raw_tidesilver` stil/mat. Aquamarijn groeit als zeshoekige prisma's met een korte punt (`crystalSpike(..., prism)`), gif- en zuiverkristal als spitse punten.
- **Gifpoel (V-T3 A):** opaak; `toxic_pool_still` 16×16 × 32 frames (frametime 3), `toxic_pool_flow` 32×32 × 32 frames (frametime 2, zoals vanilla's flowing-textures). B zet de tint in de client-extensie op wit (vaste kleur in de texture).
- **Updraft:** cutout-animatie zonder interpolatie (16 × 2); `check` eist een gelijk alfamasker nu alleen bij geïnterpoleerde animaties (`Animation.opaque` onderscheidt opake `cycling` van een bewegende cutout).
- **Reuzenlelie:** één 32×32-blad (`paintLilyPad`, seed `block/giant_lily_pad`), elk deel = één kwadrant, dus de delen sluiten altijd aan; geen aparte controle nodig. Item en lotus gebruiken dezelfde blad-schilder op 16×16.
- **Riet:** stengelposities gedeeld via de seed `block/reeds#stalks`, zodat `reeds_top` precies op `reeds` aansluit; de zwaai is periodiek over de hoogte (gestapelde stengels sluiten aan).
- **Bellenbron-zijkant:** rij 4 en lager = exact `tidestone` (`SIDE_RULES`; korst 1–3 rijen + druppel tot rij 3).
- **Bijgestuurd na het eerste contactblad (71a-r1):** gloeizwammen hogere hoeden (lazen als "T"-vormen); fossielsteen één kortere ribboog + klein schelpje in lichter bot met zachte schaduw en meer korrelspreiding (twee witte bogen herhaalden als motief op een muur; lint "one colour on 50 %"); nest als vlechtwerk in drie richtingen met schaduw (las als spikkels); bellenbron-korst een stap donkerder.
- **Particles (V-T4 A):** 8×8, `_0..3`; lichte grijswaarden (getint door A) behalve `blossom` (wit-roze → dieproze) en `glitch_speck` (magenta/cyaan/zwart).
- **Lint `FLAT_OK`:** `updraft` (doorzichtige kolom, drie tonen) en `slime_trail` (decal); 32×32 (`toxic_pool_flow`) valt buiten de lint.

## Code-review (3283f6a, via main)
- Opgelost:
  1. `purityCrystalLamp`: de bevel ligt nu op de eigen randen van elke 2-px-balk (`LAMP_BARS` 0/1, 7/8, 14/15: eerste lijn licht, tweede donker, kruispunten licht/donker/midden). Er staan geen losse lichte of donkere pixels meer op paneelcoördinaten, en de ruis in het frame is weg.
  2. Lamp-puls: `pulseStrip(frame, animation, still)` laat de tonen in `STILL_TONES` staan (de lamp: alle `FOSSIL_STONE`-tonen), zodat het lichte framesteen niet meepulseert.
  3. `clawedTidestone`: geen wrap meer (`set` in plaats van `setWrapped`; `start` 7–8, zodat x binnen 0–15 blijft).
  4. `slime_trail`: pad en breedte spiegelsymmetrisch om de middenkolom, berekend op de gespiegelde kolom (bit-identiek). Kolom 0 = kolom 15, gecontroleerd door `checkSlimeTrailEdges` vóór het wegschrijven.
  5. `crystalShard`: kristal 14 rijen (rij 1–14), zodat de outline ook boven de punt sluit (gif, zuiver en aquamarijn).
  6. `hangingStrands` geeft `Strand`-records terug (`x(y)` = exact de geschilderde kolom); de gloeiknopen en -punten van `glowing_hanging_roots` gebruiken die. De PNG bleef md5-gelijk.
  7. De kopcommentaarregel van de gekopieerde helpers noemt weer `Ring3Textures.java` (de sed-hernoeming had hem meegenomen).
- Niet opgepakt (besluit main): render-memo/`GLOW_MASKS` (speelt alleen tijdens het draaien van het script) en het ontdubbelen van de helpers → backlog (INT); `CODEBASE.md`, beslissingenlog en backlog → INT.
- Gewijzigde PNG's: `clawed_tidestone`, `purity_crystal_lamp`, `slime_trail`, `item/aquamarine_shard`, `item/purity_crystal_shard`, `item/toxic_crystal_shard`. Script 2× → md5 identiek, lint 0, **B** groen.

## CODEBASE-delta
- `tools/textures/Ring4Textures.java` (nieuw, 71a): texturescript ring 4 + Nether-ring 4, zelfde opzet als `Ring3Textures.java` (helpers gekopieerd: `bevel`, `stoneBricks`, `bestOf`/`motifScore`, hout-/deurhulpen, `chunks`, `nugget`, `particle`, `pulseStrip`, `Cells`, `rim`, `outlineSelective`, `sprite`, `merge`, `isOpaque`, `stripeScore`, `atNight`/`drawOn`/`draw`, `seeded`, `shade`/`mix`/`luminance`, `copy`, `smoothstep`/`lerp`). Nieuw: `Animation.opaque` + `rising(...)` (bewegende cutout zonder interpolatie), `LARGE` (32×32-frames) + `valueNoiseSized`/`sample` (drijvende `cycling`-patronen), `set` wist gloeimarkeringen bij overschilderen, `crystalSet(...)` + `Crystal`-record (amethist-patroon: `crystalBlock`, `buddingCrystal`, `crystalStage`, `crystalSpike` met `prism`, `crystalShard`), `Fungus`-record + `fungi`/`mushroom` (gloeizwam en bedorven variant), `glowBloom`, `hangingStrands` (hangmos, gloeiende wortels), `paintLilyPad`/`lilyPadPart` (2×2-blad uit één 32×32-beeld), `lotus`, `reeds(top)`, `logTop(wood, bark, cracked)`, `witheredLog`, `moonpetalLog`, `moonpetalLeaves`, `pebble` (jade), `tidestone` (golvende lagen), `clawedTidestone`, `bubbleSpringTop/Side`, `suckingMud`, `toxicPoolStill/Flow`, `toxicPoolBucket`, `updraft`, `slimeTrail`, `creatureNest`, `boneSand`, `bleachedNetherrack`, `fossilStone`, `purityCrystalLamp` (+ `LAMP_BARS`); `SIDE_RULES` (bellenbron); `checkSlimeTrailEdges`; `STILL_TONES` (tonen die in een puls stil blijven, `pulseStrip` met een `still`-parameter); `Strand`-record (hangende strengen). Contactblad `build/texture-preview/ring4.png`: 6×6-muren, nacht (gloeilagen), ertsen over tidestone en grijs, items/planten in een slot, scènes (cascade vale, lotus shallows met 2×2-lelies, glowroot swamp 's nachts, Nether-4-vloer, verdorde tuin), daarna per texture een cel in secties RING 4 / NETHER RING 4 / PARTICLES.
- `textures/` (71a, uit `Ring4Textures.java`): `block/` tidestone, cobbled_tidestone, polished_tidestone, tidestone_bricks, moonpetal_log(_top), stripped_moonpetal_log(_top), moonpetal_planks, moonpetal_door_{top,bottom}, moonpetal_trapdoor, moonpetal_leaves, moonpetal_sapling, withered_moonpetal_log(_top), stripped_withered_moonpetal_log(_top), reeds, reeds_top, peat, hanging_swamp_moss, giant_lily_pad_{north_west,north_east,south_west,south_east}, lotus, glowcap(+`_glow`), glow_bloom(+`_glow`), glowing_hanging_roots(+`_glow`), blighted_glowcap(+`_glow`), bubble_spring_{top,side}, tidesilver_ore_overlay (puls), jade_ore_overlay, toxic_crystal_block, budding_toxic_crystal, small_/medium_/large_toxic_crystal_bud, toxic_crystal_cluster (alle puls), aquamarine_block, budding_aquamarine, small_/medium_/large_aquamarine_bud, aquamarine_cluster, sucking_mud (`cycling` 16×5), toxic_pool_still (`cycling` 32×3), toxic_pool_flow (32×32, `cycling` 32×2), updraft (cutout 16×2), clawed_tidestone, slime_trail, creature_nest; Nether-4: bone_sand, bleached_netherrack, fossil_stone, polished_fossil_stone, fossil_stone_bricks, purity_crystal_block, purity_crystal_cluster, purity_crystal_lamp (puls); `item/` moonpetal_door, giant_lily_pad, raw_tidesilver, jade, toxic_crystal_shard (puls), aquamarine_shard, purity_crystal_shard (puls), toxic_pool_bucket; `particle/` will_o_wisp, blossom, firefly, swamp_gas, toxic_bubble, updraft_bubble, glitch_speck, bone_dust (elk `_0..3`, 8×8). 106 PNG's, 20 `.png.mcmeta`'s. Stairs/slab/wall/fence/wood gebruiken de texture van hun basisblok; kristalstadia, lotus, riet (`reeds_top`) en hangplanten gebruiken als item de bloktexture (B).

## Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-13 | textures | V-T1 A: jade koel melkachtig blauwgroen, aquamarijn licht zee-cyaan, tidesilver zilver met blauwgroene parelmoerglans, gifkristal fel zuurgroen → geelgroen, zuiverkristal melkwit met lichtgouden glans | Besluit gebruiker: elk materiaal in één oogopslag te onderscheiden; gif blijft de enige "giftige" kleur; zuiverkristal past bij de wit-beige Nether-4-fog en hemelglas |
| 2026-10-13 | textures | V-T2 A: gif- en zuiverkristal (blok, stadia/cluster, scherf, lamp) pulseren en gloeien; aquamarijn mat-glanzend en stil; jade mat. Afwijking B: `tidesilver_ore` zwakke gloed (licht 3), dus een zwak pulserende emissive overlay; `raw_tidesilver` mat | Besluit gebruiker; gloed blijft zeldzaam, de nacht donker |
| 2026-10-13 | textures | V-T3 A: gifpoel opaak zoals lava, still 16×16 × 32 frames, flow 32×32-frames zoals vanilla | Besluit gebruiker: duidelijk anders dan het veilige gifgroene water, alfaregel 0/255, geen translucent sorting |
| 2026-10-13 | textures | V-T4 A: particles lichte grijswaarden getint door A; vaste kleuren alleen `blossom` en `glitch_speck` | Besluit gebruiker; A kiest per band de kleur (zoals ring 3) |
| 2026-10-13 | textures | Riet: `reeds` (stengel) + `reeds_top` (pluim), groeit als suikerriet tot 3 hoog; lotus drijft plat op het water (bovenaanzicht, vaste kleur, cutout) | Besluit gebruiker (via B) |
| 2026-10-13 | textures | Paden met B afgestemd: `giant_lily_pad_<part>` (north_west/north_east/south_west/south_east), hangmos/-wortels één texture, `bubble_spring_top`/`_side`, ertsen één overlay per erts gedeeld door stone/tidestone/deepslate, aquamarine-namen naar het patroon van toxic_crystal | Bestandsnaam = blok-/itemnaam (contract §7); akkoord team-lead |

## Backlog
- `[textures]` Ring 4-textures na teststop 5a/5b bijsturen op het in-game beeld (71b/71): vooral tidestone in kliffen (gelaagdheid sterk genoeg?), de bloesemkruin in een echt bos (motief), gloeiflora 's nachts, de reuzenlelie en lotus op echt water (kleur tegen de biome-waterkleur), de gifpoel naast gifgroen water, fossielsteen op een muur.
- `[textures/atmosphere]` Particle-sprites in-game beoordelen zodra A ze registreert (tint, grootte); `swamp_gas` en `bone_dust` mogelijk zachter/kleiner.
- `[infra]` (bestaand) ontdubbeling texturescripts: `Ring4Textures.java` kopieert opnieuw ~600 regels helpers uit `Ring3Textures.java` (ook review 71a).
- `[infra]` Review 71a: `render` heeft geen memo en `GLOW_MASKS` groeit tijdens een run (alleen scripttijd, geen gevolg voor de PNG's).
- `[blocks]` Bij 72 (golf 2): B bevestigt de paden van de gameplayblokken (`toxic_pool_still/_flow` + tint wit, `updraft` cutout, `slime_trail` als platte decal, `creature_nest` één texture voor alle zijden); T past aan als B een andere vorm kiest.
