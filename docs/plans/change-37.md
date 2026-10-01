# Change 37 — Definitieve textures ring 2 + Nether-ring 2

Scope: `textures` — change 37

**Doel:** de placeholder-textures van ring 2 en Nether-ring 2 (changes 21, 22, 24, 29, 33, 34) vervangen door definitieve textures van 16×16, in de stijl van ring 1 (`fase-1-wereld.md` §1.4). Het gaat om de kloofsteen-, scorched sandstone-, flora-, palm-, erts-, zout-, druipzwam-, Nether-grond-, stalactiet- en dauwtextures. Het script `tools/textures/Ring2Textures.java` wordt hiervoor verfijnd. Daarnaast komen de animaties (`.mcmeta`) en de emissive gloed waar die gekozen zijn, en de model-aanpassingen die daarvoor nodig zijn. Deze change maakt alleen assets en block-/itemmodels. Er komt geen gedrag bij, geen registries en geen andere vormen of hitboxen.

Bron: `fase-1-wereld.md` §1.4 (stijl, productiemethode, emissive en puls), §2 Ring 2 (kloofsteen "rood/oranje/crème", zoutvlakte "kaal, wit", zonnesteen "gloeiend", Nether-ring 2 "grijsgroene fog", dauwbron "water-achtig") en bouwplan rij 37 (test: runClient). Sjabloon: `docs/plans/change-17.md` (ring 1) en `tools/textures/Ring1Textures.java`.

---

## Afstemming (2026-10-01)

Afgestemd met 21, 22, 24, 28, 29 en 33–36 (basis: de aanbevolen opties).
- **Script-API bestaat al (21):** `Animation`-record, `still`, `pulsing(name, glow, painter)` met gloeikleur per materiaal, en de controles op afmeting, alpha 0/255 en gelijk alpha-masker per frame. §3.1 en §3.2 krimpen daardoor tot: `cycling(...)` toevoegen en de controle "zijkant = grond" (`spore_moss_side`, `salt_crust_side`; de placeholders van 29/33 volgen die regel al).
- **Kristal-helper `crystalCluster(block)` bestaat (29, ook gebruikt door 34):** deze change voegt alleen de `emissive`-vlag toe. Itemmodellen van zoutkristal en dauwstadia gebruiken dezelfde parent (`minecraft:item/amethyst_bud`).
- **Vormen zijn `blocks`:** 21 (cactus), 29 (zoutkristal) en 34 (dauwstadia) leggen hun vorm definitief vast; deze change tekent binnen die vormen.
- **Vraag 3 (zoutkristal-gloed) is de enige plek voor emissive op het zoutkristal** (optie B van 29 vraag 5 is hierheen verplaatst).
- **Namen:** Nether-ring 2-biomes `dripshroom_forest`/`spore_caves` (35 vraag 9 A); PNG-telling van 33 gecorrigeerd naar 26.
- **Particles:** `ModParticleDescriptionProvider` bestaat (28).
- **Kloofsteen-placeholders (21)** hebben al geen harde laaglijn (richting vraag 5 A).
- **Teststop (besloten 2026-10-01, teststop 3 van 3):** na deze change volgt de **blokkerende gebundelde eindcheck ring 2 + Nether-2** (runClient + dedicated server, contactblad vooraf): de vroegere blokkerende Nether-2-check van 35 (die change is nu W-achteraf) plus de W-achteraf-lijsten van 21–37 (behalve wat al in teststop 2b na change 30 akkoord is: terrein 23/23b/23c, ertsen 25, grotten 26, extra biomes 30). Change 38 begint pas na akkoord van de gebruiker. Zie Tests → "Teststop 3".
- **Keuzes van de gebruiker die deze lijst raken (2026-10-01):** 24 vraag 3 B → items `raw_sunstone` (pulseert mee) en `raw_turquoise` (still) i.p.v. `sunstone`/`turquoise`; 26 vraag 5 B → nieuw blok `loose_sand` (change 21) met een eigen texture; 23 vraag 5 B → diepere kloven (banden van 2–8 blokken, periode ~48); 35 vraag 6 B → dauwbronnen in geodes (geen nieuwe textures).
- **`loose_sand`-texture (routinekeuze van de agent, gemarkeerd):** subtiel donkerder en grover gekorreld dan zand, zodat een oplettende speler een zandgrot-plafond kan herkennen zonder dat het opvalt; geen nieuwe ontwerpvraag.

## Afstemming na teststop 2 (2026-10-01)

Afgestemd met de plannen 23b en 23c en de feedback van teststop 2 (`docs/plans/teststop-2-feedback.md`, punt 3).
- **Expliciet aandachtspunt: palet van `canyon_cobblestone` herzien.** Feedback van de gebruiker: *"voelt raar qua palet"*. Het nieuwe palet moet passen bij de drie kloofsteentinten (rood/oranje/crème): kloofkeien lezen als "gebroken kloofsteen" (gemengde brokken in de drie tinten, iets donkerder en met donkere voegen), niet als een vreemde vierde kleur of als vanilla-keien met een rode zweem. Na 23c ligt `canyon_cobblestone` veel in het zicht (keienvelden, puin op puinhellingen, rotsval aan wandvoeten), dus dit is geen detail meer. Controle: in het contactblad een rij `canyon_cobblestone` naast de drie tinten, en in-game een keienveld naast een kloofwand. Neem dit op in de checklist hieronder (Textures) en vraag het de gebruiker expliciet in teststop 3.
- **Meer crème in de wanden (23b-13 B):** rood 22 / oranje 12 / crème 14 per 48, in 4 crème banden. `cream_canyon_stone` is daardoor veel beter zichtbaar: de crème-texture verdient evenveel aandacht als rood, en het contrast van de zonnesteen-overlay op crème (risico "overlay op vier ondergronden") weegt zwaarder.
- **Grind op puinhellingen (23b)** naast de kloofsteentinten in-game beoordelen (vanilla-grind, geen eigen texture; alleen melden als het vloekt).
- **Oase (23c):** `moss_block` en `coarse_dirt` (vanilla) liggen nu naast de palmbladeren en op de bredere grasoever (23b-8). Beoordelen of `palm_leaves` en het oasegras daarmee samengaan.
- **Teststop:** de terrein-W-lijsten van 23, 23b, 23c, 25, 26 en 30 zijn al afgelopen in **teststop 2b** (na change 30); teststop 3 hoeft die niet opnieuw te doen (wel de textures en de overige lijsten).

---

## Afhankelijkheden en aannames

Gecontroleerd in de code op 2026-10-01 (stand na change 17, commit `7714b77`). **Changes 18–36 zijn nog niet gebouwd. Hun plannen zijn de bron, dus alles hieronder dat daaruit komt is een aanname.** Leg bij de start de lijst naast `CODEBASE.md` (tabellen Blocks/Items, textures-regel, tooling-tabel) en volg de echte stand. Afwijkingen noteer je onder "Afwijkingen bij de bouw".

- **Gebouwd (ring 1, change 17):** `tools/textures/Ring1Textures.java`. Dit is een single-file Java-script dat alleen de JDK gebruikt. Het bevat een `TEXTURES`-map (naam → tekenmethode), de hulpfuncties `still`/`pulsing`, een vaste seed per bestandsnaam, de lijst `HAND_EDITED` en het contactblad `build/texture-preview/ring1.png`. Verder heeft het generieke helpers: `valueNoise`, `shade`, `mix`, `topBand`, `crystalClusters`/`shard`/`crystal`, `outline`/`merge`, `window`/`rail`/`frame`, `furrow`, `crack` en `rim`. De puls zit in `pulseStrip` en is **hard vastgelegd** op 8 frames (`PULSE_FRAMES`), `frametime` 4 en één gloeikleur `PULSE_GLOW = 0xD8FFFF` (cyaan). Elke animatie schrijft dezelfde `.mcmeta`. De datagen-helper `ModBlockStateProvider.oreWithOverlay` maakt de overlay emissive (`emissivity(15, 15).ao(false)`) en zet `render_type` op cutout.
- **Aanname change 21:** `tools/textures/Ring2Textures.java` bestaat. Het heeft dezelfde opzet als ring 1, met helpers die uit `Ring1Textures` gekopieerd zijn, en een contactblad `build/texture-preview/ring2.png`. Er geldt één script per ring, en de Nether-tegenpool hoort bij zijn ring (zo staan changes 33/34 erin). **Ontbreekt het script bij de start: stoppen en vragen.**
- **Aanname changes 21/22/24/29/33/34:** alle texturebestandsnamen in de tabel hieronder bestaan als placeholder. Deze change vervangt alleen pixels en voegt `.mcmeta`-bestanden toe. Er worden geen bestanden hernoemd: de models uit datagen verwijzen al naar deze namen. De lijst volgt de **besloten antwoorden** van die plannen (2026-10-01; o.a. `raw_sunstone`/`raw_turquoise` uit 24 vraag 3 B en `loose_sand` uit 21/26 vraag 5 B). Wijkt de bouw daar toch van af, dan volgt deze lijst wat er echt gebouwd is.
- **Aanname change 24:** `oreWithOverlay` heeft een `emissive`-vlag. Zonnesteen is emissive (licht 3), turkoois mat en zonder licht (vraag 5 A van change 24). §1.4 heeft dan een uitzondering voor "edelstenen zonder gloed". Deze change voegt alleen de **puls** toe aan de emissive zonnesteen-overlay, volgens §1.4: alle emissive erts-overlays pulseren en het item krijgt dezelfde puls, maar is niet emissive.
- **Aanname change 29/34:** het zoutkristal en de dauwkristal-stadia zijn vanilla `AmethystClusterBlock` met een kruismodel (`cutout`, `directionalBlock`). Hoogte en offset staan **in de Java-constructor** (`blocks`). Een eventuele gedeelde helper voor kristalmodels uit change 29/34 (bijvoorbeeld `crystalCluster(block)`) breidt deze change uit met een `emissive`-vlag (vraag 1/3).
- **Aanname change 33:** de druipzwam-stam blijft in change 33 `still`. Change 33 laat de animatie (stam, lamp, schimmel) en de emissive gloed uitdrukkelijk aan deze change over. `glow_mold` is een multipart-model naar het voorbeeld van vanilla `glow_lichen`.
- **Aanname change 28/36 (particles):** change 28 gebruikt voor `sand_gust` de vanilla-sprites `minecraft:generic_0…7` en zet "eigen sprite in change 37" in de backlog. Change 36 maakt alleen bij vraag 4 B placeholder-sprites `textures/particle/spore_{0..3}.png` (anders vanilla-particles, geen texture). Zie vraag 7.
- **Aanname change 23/30/31 (testplekken):** biomes `canyonlands`, `oasis_flats`, `salt_flats`, `hoodoo_fields`, `dune_sea`, overgang `dry_riverbeds`. Kloofsteen ligt in banden van 2–8 blokken (periode ~48) in diepe kloofwanden van Y ~70 tot 140, afwisselend rood/oranje/crème (change 23, vraag 5 B). Zandgrot-plafonden zijn `loose_sand` (change 26). Palmen hebben hangende bladwaaiers (change 23). Zoutkorst ligt in grote vlakken (change 30). Gebarsten klei en scorched sandstone liggen in grote vlakken in `dry_riverbeds` (change 31). Nether-ring 2-biomes (change 35 vraag 9 A, afgestemd 2026-10-01): `dripshroom_forest`, `spore_caves`.
- **Vormen liggen vast.** Change 21 ("vorm cactus definitief in change 37"), 29 en 34 ("maten placeholder, vorm definitief in change 37") noemen vormen. Die vormen zitten in Java-code (`BarrelCactusBlock.getShape`, `AmethystClusterBlock(height, offset)`) en horen dus bij `blocks`. Deze change tekent **binnen de bestaande vormen**. Het cactusmodel blijft gelijk aan zijn hitbox. Is er een andere vorm nodig, dan gaat dat naar de backlog (`[blocks]`). Hetzelfde geldt voor `MapColor`-bijstellingen als het definitieve palet sterk afwijkt (afspraak change 22/33).
- Niet opnieuw openen: de stijl van §1.4 (16×16, palet van 4–6 tinten per materiaal, alleen alpha 0/255, eigen pixels, puls 8 frames/`frametime` 4/interpolate, items pulseren mee maar zijn niet emissive), de ring 1-textures (`Ring1Textures.java` en de ring 1-PNG's blijven byte-identiek) en de beslissingen uit de blokplannen.

---

## Plan

### 1. Volledige texturelijst (16×16, tenzij anders vermeld)

Pad: `src/main/resources/assets/corruptedadventure/textures/…`. **Vet** = krijgt een `.png.mcmeta` (animatie), *E* = overlay/model emissive. De kolom "Herkomst" geeft de change die de placeholder maakte.

**Ring 2 (overworld) — 37 PNG's**

| Bestand | Voor | Herkomst | Animatie / gloed |
|---|---|---|---|
| `block/red_canyon_stone.png`, `block/orange_canyon_stone.png`, `block/cream_canyon_stone.png` | drie tinten (lagen in de kloofwanden); ook de basis van de zonnesteen- en turkooisertsen | 21 | still; laagstructuur vraag 5 |
| `block/canyon_cobblestone.png` | keien + trap/plaat/muur; **palet herzien** (teststop 2: "voelt raar qua palet"; zie Afstemming na teststop 2) | 21 | still |
| `block/canyon_stone_bricks.png` | bakstenen + trap/plaat/muur | 21 | still |
| `block/scorched_sandstone_top.png`, `block/scorched_sandstone.png`, `block/scorched_sandstone_bottom.png` | zandsteen + trap/plaat/muur; top ook voor `smooth_` en als boven-/onderkant van `cut_`/`chiseled_`; basis van `scorched_sandstone_sunstone_ore` | 21 | still |
| `block/cut_scorched_sandstone.png`, `block/chiseled_scorched_sandstone.png` | zijkanten | 21 | still |
| `block/loose_sand.png` | los zand (zandgrot-plafonds, change 26); subtiel donkerder/korreliger dan vanilla-zand (routinekeuze, gemarkeerd) | 21 | still |
| `block/cracked_clay.png` | gebarsten klei (willekeurig gedraaid, dus naadloos in alle 4 de rotaties) | 21 | still |
| `block/dry_shrub.png`, `block/desert_flower.png` | kruismodel + plat item (cutout) | 21 | still |
| `block/barrel_cactus_top.png`, `block/barrel_cactus_side.png` | cactus (binnen de bestaande vorm, ook in de pot) | 21 | still |
| `block/palm_log.png`, `block/palm_log_top.png` | stam (ook `palm_wood`) | 22 | still |
| `block/stripped_palm_log.png`, `block/stripped_palm_log_top.png` | gestript (ook `stripped_palm_wood`) | 22 | still |
| `block/palm_planks.png` | planks-familie | 22 | still |
| `block/palm_door_top.png`, `block/palm_door_bottom.png`, `block/palm_trapdoor.png`, `item/palm_door.png` | deur/valluik (cutout) | 22 | still |
| `block/palm_leaves.png` | bladeren (`cutout_mipped`, vaste kleur, tenzij change 22 vraag 2 B → grijswaarden) | 22 | still |
| `block/palm_sapling.png` | zaailing, plat item, pot | 22 | still |
| `block/sunstone_ore_overlay.png` | alle 4 zonnesteen-varianten (transparant) | 24 | **puls**, *E* (model uit change 24) |
| `block/turquoise_ore_overlay.png` | alle 3 turkoois-varianten (transparant) | 24 | still, mat (change 24 vraag 5 A) |
| `item/raw_sunstone.png` | item ruw zonnesteen (24 vraag 3 B) | 24 | **puls**, niet emissive |
| `item/raw_turquoise.png` | item ruw turkoois (24 vraag 3 B) | 24 | still |
| `block/salt_crust_top.png`, `block/salt_crust_side.png` | zoutkorst (onderkant = `salt_block`) | 29 | still |
| `block/salt_block.png`, `block/polished_salt.png` | zoutblok; gepolijst zout + trap/plaat/muur | 29 | still |
| `block/salt_crystal.png` | kruismodel + plat item (cutout) | 29 | vraag 3 |
| `item/salt.png` | item (alleen als change 29 vraag 3 A/C) | 29 | still |

**Nether-ring 2 — 32 PNG's** (26 uit change 33 — telling daar op 2026-10-01 gecorrigeerd van 27 naar 26 — plus 6 uit change 34; zonder fungus/pot uit vraag 4 B van change 33)

| Bestand | Voor | Herkomst | Animatie / gloed |
|---|---|---|---|
| `block/soaked_netherrack.png` | blok + onderkant sporenmos | 33 | still |
| `block/spore_moss.png`, `block/spore_moss_side.png` | top / zijkant (zijkant = exact de pixels van `soaked_netherrack` met een mosrand, regel §1.4) | 33 | still |
| `block/dripshroom_stem.png`, `block/dripshroom_stem_top.png` | stam + hyfen | 33 | stam-zijkant: vraag 4 |
| `block/stripped_dripshroom_stem.png`, `block/stripped_dripshroom_stem_top.png` | gestript | 33 | still |
| `block/dripshroom_planks.png` | planks-familie | 33 | still |
| `block/dripshroom_door_top.png`, `block/dripshroom_door_bottom.png`, `block/dripshroom_trapdoor.png`, `item/dripshroom_door.png` | deur/valluik (cutout) | 33 | still |
| `block/dripshroom_cap.png` | hoed | 33 | still |
| `block/dripshroom_lamp.png` | zwamlamp (licht 15) | 33 | still (vraag 4) |
| `block/glow_mold.png` | gloeischimmel (multiface, cutout) | 33 | vraag 4 |
| `block/slime_stalactite_{down,up}_{tip,tip_merge,frustum,middle,base}.png` (10) | stalactiet/stalagmiet per dikte (cutout) | 33 | still |
| `item/slime_stalactite.png` | item | 33 | still |
| `block/dew_source.png` | dauwbron (opaak, alle zijden) | 34 | vraag 1 |
| `block/small_dew_crystal_bud.png`, `block/medium_dew_crystal_bud.png`, `block/large_dew_crystal_bud.png`, `block/dew_crystal_cluster.png` | stadia (kruismodel + plat item, cutout) | 34 | vraag 1 |
| `item/dew_crystal.png` | item | 34 | vraag 1 (puls als de stadia pulseren, niet emissive) |

**Particles (alleen bij vraag 7 A; 8×8):** `particle/sand_gust_{0..3}.png`, en bij change 36 vraag 4 B ook `particle/spore_{0..3}.png` (die maakt change 36 als placeholder aan).

Kern: **69 PNG's** (37 ring 2 + 32 Nether-ring 2). Met de besloten antwoorden komen er **9 `.mcmeta`'s** bij: zonnesteen-overlay + item `raw_sunstone` (2), dauwbron + 4 stadia + item (6) en druipzwam-stam (1). Bij vraag 7 A komen er 4 particle-sprites bij.

### 2. Paletten per set (donker → licht; vertrekpunt = de werkpaletten uit de blokplannen)

Dit zijn **voorstellen**. Ze worden definitief na het contactblad en runClient en daarna vastgelegd in `fase-1-wereld.md` (zie §6). Elke pixel valt op het palet van zijn materiaal; accenten staan apart.

| Set | Palet | Patroon |
|---|---|---|
| Rode kloofsteen | `5A2418 6E2E1E 8A3B26 A44C30 BC6040 CF7A55` | fijne zandsteenkorrel, horizontaal gerekte ruis, vraag 5 |
| Oranje kloofsteen | `7A3E16 8E4A1C A85E24 C2742E D68B3E E4A55A` | idem, andere seed |
| Crème kloofsteen | `9E876A B49C7A C7B08C D8C39F E6D4B2 F1E3C6` | idem, fijner |
| Kloofkeien | de drie tinten + voeg `3E2218` | ronde stenen in alle drie de tinten, donkere voeg (vanilla-indeling als referentie) |
| Kloofbakstenen | rode tint + voeg `4A2418` | vanilla-baksteenindeling, randschaduw |
| Scorched sandstone | `4A3A2A 6B5236 8A6B45 A88656 C2A06C` + roet `2A221C` | zijkant: horizontale banden zoals zandsteen met roetvlekken die van onder opstijgen; top/bodem korrelig; cut = rand + vlak; chiseled = eenvoudig zonmotief (knipoog naar zonnesteen) |
| Los zand | zandtinten iets donkerder dan vanilla-zand, bijv. `BFAE80 CDBD8F D9CA9C E3D6AB` + korrel `A8976A` | grovere, onregelmatige korrel en een paar donkere spikkels; van dichtbij herkenbaar, op afstand bijna zand (routinekeuze) |
| Gebarsten klei | `7C6A58 92806A A8957C BCA98F` + scheur `4E4236` | polygonen met donkere scheuren, opgekrulde lichte randen, naadloos in alle rotaties |
| Dorre struik | `5A4630 75603F 917A52 AD9468` | dunne verdorde takjes (cutout) |
| Tonnetjescactus | `2F4A2A 3F6136 557A44 6E9455` + stekels `E6D9A8` | ribben (verticaal), stekelpuntjes in rijen; top: ster van ribben + bloemknop |
| Woestijnbloem | blad `A8326E C94C8A E27AAB`, steel `4F6A33 6B8A45` | 5 smalle bloemblaadjes, kort steeltje |
| Palm-schors | `4A3E30 5E5040 74644F 8A7A62 A08F74` | **horizontale** schubben/bladlittekens (palmstam), geen verticale nerf |
| Palmhout | `9C8250 B39A62 C8B077 DAC48E E9D7A6` | planks: 4 × 4 rijen (zelfde indeling als `dead_planks`); kopse kant: vezelige stippen + 1-px schorsrand, geen jaarringen |
| Palmbladeren | `2E5A1E 3F7A28 559A34 72B845 94D060` | lange smalle bladlobben, diagonaal, gaten (cutout_mipped) |
| Zonnesteen | `5A2A08 9A4A0C D9801A F2B23A FFE07A FFF6C8`; gloei `FFF0B0` | 3–5 hoekige brokjes, kristalstijl §1.4 (omlijning, lichte linkerkant, schaduw rechts, highlight); leesbaar op rood, oranje, crème en bruin. Item `raw_sunstone`: ruwe brok zoals vanilla `raw_iron`, met gesteenteresten |
| Turkoois | `0E3B3A 1C6B66 2E9C92 4FC6B5 8FE6D6` + matrix `3A2A1E` | 2–3 ronde, matte knollen met donkere adertjes, geen facetten, geen highlight. Item `raw_turquoise`: ongepolijste knol met matrix-resten |
| Zout | `B8AE9A CFC7B6 E2DCCF EFEBE2 F7F5EF` + rand `FFFFFF`, schaduw `C9C0AE` | korst-top: grove zeshoekige polygonen met opstaande lichte randen; zijkant: korstband 2–3 px op zoutblok-korrel; gepolijst: gladde vlakken met rand |
| Zoutkristal | `BFCFD8 DCE6EB F1F5F7 FFFFFF` + zweem `EBD9D6` | 2–3 puntige kristallen (cutout), vraag 3 |
| Doorweekt netherrack | `2E1A1A 47282A 5E3634 6F4A44` + glans `8A7A70` | netherrack-achtige klonten, natte glansvlekjes |
| Sporenmos | `3E4A3C 556450 6E7F66 8C9A80` + sporen `C8D4B0` | vilt met sporenpuntjes; zijkant = rafelige mosrand over doorweekt netherrack |
| Druipzwam-steel | `3A3440 4E4A58 66667A` + slijm `7FA88A` (vraag 6) | verticale vezels met slijmstrepen; kopse kant: ring + vlezig binnenste |
| Druipzwam-hout | `6E7A6A 87947F A0AC94 B8C2A8` (vraag 6) | planks/gestript/deur/valluik |
| Druipzwam-hoed | `4A3F55 5F516C 75688A` + druppels `9FC3A8` | vlezig, druppelvlekken |
| Zwamlamp | `B8D27A CFE38F E3F0AE F4FAD2` | lichtgevende klonten (shroomlight als referentie) |
| Gloeischimmel | `3E9C86 5FD6B8 8BEACF C2F7E6` | losse plekjes en draadjes (cutout) |
| Slijmstalactiet | `4E8A4A 6AA860 8CC47A B2DE9E` | vanilla-dikte-indeling als referentie, natte highlight aan één kant |
| Dauwbron | `173F52 1F5F78 2E86A6 4FAFC8 86D2E0` + glinster `D6F4FA` | golvende waterlijnen + glinsterpunten, opaak (vraag 1) |
| Dauwkristal | vraag 2 (aanbevolen: `1B4A5C 5E9FB4 A6D3E0 D6EEF4 F4FCFF`, gloei `F0FCFF`) | 1 / 2 / 3 / 4–5 kristallen per stadium, kristalstijl §1.4 |

### 3. Script `tools/textures/Ring2Textures.java` verfijnen

Hetzelfde script dat change 21 aanmaakt. Het wordt **niet** herschreven, alleen verfijnd. `Ring1Textures.java` blijft ongewijzigd.

1. **Animatie generiek maken.** Het record `Texture` krijgt een `Animation` (`frames`, `frametime`, `interpolate`, eventueel `glow`) in plaats van de boolean. De helpers worden:
   - `still(name, painter)`;
   - `pulsing(name, glow, painter)`: de §1.4-puls (8 frames, `frametime` 4, interpolate, alleen kleur). De gloeikleur is een parameter in plaats van `PULSE_GLOW`: zonnesteen warm goud, dauwkristal bijna wit;
   - `cycling(name, frames, frametime, painter)` voor opake bewegende patronen (dauwbron-golf, slijm in de stam). Die tekent per frame met de framefase als invoer, sluit naadloos (periodiek) en gebruikt geen interpolate.

   De `.mcmeta` volgt de animatie.
2. **Ingebouwde controles.** Het script stopt met een fout bij elk van deze gevallen:
   - een afmeting die niet 16×16 of 16×(16·n) is (particles 8×8);
   - een alpha die niet 0 of 255 is;
   - een animatiestrip waarvan het alpha-masker per frame verschilt (valkuil "Geanimeerde textures");
   - een zijkant die niet exact de pixels van zijn grond heeft onder de rand. Dat geldt voor `spore_moss_side` (onder `soaked_netherrack`) en `salt_crust_side` (onder `salt_block`).

   Zo zijn de stijlregels automatisch gecontroleerd en niet alleen visueel.
3. **Tekenmethodes** per texture met de paletten uit §2. Gedeelde patronen worden hergebruikt:
   - `topBand` voor de sporenmos- en zoutkorst-zijkant;
   - `crystalClusters`/`shard` voor zonnesteen, zoutkristal en dauwstadia;
   - `window`/`rail`/`frame` voor deuren en valluiken;
   - de planken-indeling van `dead_planks` voor de palm- en druipzwamplanken;
   - een polygoon-helper voor gebarsten klei en de zoutkorst (één helper, twee paletten).
4. **Contactblad** `build/texture-preview/ring2.png` (niet in git) met twee secties: "Ring 2" en "Nether ring 2". Per texture toont het 8× en een 3×3-tegeling, met alle frames. Er komen extra previewrijen bij, alleen in het contactblad en niet als PNG in de repo:
   - "ores": de overlays over elke basis;
   - "strata": een kolom van 7 blokken met banden rood/oranje/crème zoals change 23, om vraag 5 te beoordelen;
   - "cracked clay": 4×4 met willekeurige rotaties;
   - "salt flat": 5×5 korst-top.
5. **`HAND_EDITED`** blijft leeg, tenzij de gebruiker tijdens het itereren bestanden met de hand bijwerkt.
6. **Volgorde:** eerst het script draaien, dan `runData` (`ExistingFileHelper`). Twee keer draaien moet byte-identieke uitvoer geven, ook voor de `.mcmeta`'s.

Ontdubbeling van de helpers (ring 1 ↔ ring 2) hoort bij `infra`. Daarvoor bestaat al de backlogregel "Texturescripts draaien handmatig…" (change 10/17, aangevuld door change 21). Deze change doet dat niet.

### 4. Animatie en gloed

- **Zonnesteen** (§1.4, vast): de overlay pulseert. Het model is al emissive sinds change 24. Het item `raw_sunstone` pulseert mee en is niet emissive. Alle zonnesteen-varianten delen één overlay en pulseren dus gelijk op.
- **Turkoois:** still en mat. Volgt change 24 vraag 5. Is daar toch emissive gekozen (C), dan geldt de puls ook voor turkoois.
- **Dauw-set:** vraag 1 (aanbevolen):
  - de kristalstadia krijgen een emissive kruismodel en de §1.4-puls;
  - de dauwbron krijgt een trage, opake golf (`cycling`, 16 frames, `frametime` 4, geen interpolate) en is niet emissive;
  - het item `dew_crystal` pulseert mee.
- **Zoutkristal:** vraag 3 (aanbevolen: still, niet emissive).
- **Overige Nether-ring 2:** vraag 4 (aanbevolen):
  - de slijmstrepen in de druipzwam-stam glijden traag omlaag (`cycling`, 16 frames, `frametime` 8). Hyfen gebruiken dezelfde texture;
  - `glow_mold` krijgt een emissive model, statisch;
  - zwamlamp, hoed en stalactieten blijven still.
- Kleuren van een puls: alleen de kleur verandert, het alpha-masker blijft gelijk (§1.4).

### 5. Model-wijzigingen (datagen, `ModBlockStateProvider`)

- **Emissive kruismodel** (vraag 1 A, vraag 3 B/C). De kristalmodel-helper uit change 29/34 krijgt een `emissive`-vlag, naar het voorbeeld van `oreWithOverlay` (change 24). Met de vlag uit blijft het `models().cross(...)` (de gegenereerde JSON ongewijzigd). Met de vlag aan komt er een eigen model met de twee diagonale vlakken van vanilla `block/cross`: elementen met rotatie 45° rond Y en `rescale`, `shade` false, plus `emissivity(15, 15).ao(false)` en `renderType("cutout")`. Dat is nodig omdat `neoforge_data` per element werkt en het vanilla-`cross`-parent geen eigen elementen toelaat. Verifieer de API bij de bouw in NeoForge 21.1.252, en vergelijk met de vanilla-`cross.json` uit de client-extra-jar (valkuil "Vanilla worldgen-JSON"). Blockstate-rotaties blijven gelijk.
- **`glow_mold` emissive** (vraag 4 A): het vlakmodel van change 33 krijgt `emissivity(15, 15).ao(false)` op zijn element. De multipart-blockstate blijft gelijk.
- **Zonnesteen:** geen modelwijziging. Puls via `.mcmeta`.
- **Particles** (vraag 7 A): `ModParticleDescriptionProvider` (change 28) verwijst voor `sand_gust` naar `corruptedadventure:sand_gust_0…3` in plaats van `minecraft:generic_*`. Bij change 36 vraag 4 B verwijzen de spore-sprites al naar `spore_{0..3}`; die worden alleen definitief. `SandGustParticle` blijft ongewijzigd. Hij tint de sprite (aanname: `setColor`; de sprites worden dan lichte grijswaarden) en kiest het frame naar leeftijd (`setSpriteFromAge`). Wijkt de class daarvan af, volg dan de class en noteer dat.
- Overige models: ongewijzigd. Cactusmodel = hitbox (zie aannames).
- Daarna `./gradlew runData`. Alleen de bovenstaande models en `particles/sand_gust.json` mogen veranderen.

### 6. Documentatie

- `fase-1-wereld.md`:
  - §2 Ring 2: **"Textures (change 37, definitief; stijl in §1.4)"** met de definitieve paletten en patronen per set, zoals de ring 1-sectie;
  - Nether-ring 2: hetzelfde voor druipzwam, grond, stalactiet en dauw;
  - §1.4 aanvullen: gloeikleur van de puls per materiaal, opake `cycling`-animaties zonder interpolate (water, slijm), emissive kruis-/vlakmodellen, controles in het script, één contactblad per ring met secties per dimensie.
- `CODEBASE.md`:
  - textures-regel (ring 2 en Nether-ring 2 definitief, lijst + `.mcmeta`'s);
  - tooling-tabel `Ring2Textures.java` ("definitief sinds change 37", `pulsing(glow)`/`cycling`, controles, contactblad-secties);
  - `ModBlockStateProvider` (emissive kruismodel, `glow_mold`);
  - valkuilen: emissive op een kruismodel vraagt eigen elementen; `cycling` alleen op opake textures; particle-sprites via `ParticleDescriptionProvider`;
  - "Laatst bijgewerkt".
- `02-beslissingenlog.md`: de regels onderaan.
- `docs/backlog.md`:
  - `[textures] Ring 2- en Nether-ring 2-textures zijn definitief uit het script, maar nog niet in-game beoordeeld: 1–2 iteratierondes na runClient, handwerk in HAND_EDITED van Ring2Textures.java. (bron: change 37)`;
  - de `[infra]`-regel over de texturescripts bijwerken: er zijn nu twee definitieve scripts met gekopieerde helpers;
  - bij een vorm- of kaartkleurafwijking: `[blocks] …` met het blok en de gewenste vorm/`MapColor`;
  - de change 28-backlogregel over de `sand_gust`-sprite afvinken (vraag 7 A) of laten staan (B).

---

## Client/server-impact

Alleen assets: PNG, `.mcmeta`, block-/itemmodels en de particle-beschrijving. De client laadt die; een dedicated server leest ze niet. Code verandert alleen in de datagen-providers (`ModBlockStateProvider`, eventueel `ModParticleDescriptionProvider`). Er komt geen code in `client/` bij en er verandert niets in common-gedrag. Emissive (`neoforge_data`) is puur een render-effect, los van het `lightLevel` van de blokken. Het texturescript staat buiten `src/` en komt niet in de jar. Geen netwerk, geen staat, geen invloed op worldgen of determinisme. Twee clients zien hetzelfde, behalve dat een puls niet per se in fase loopt (vanilla-animatieklok per client).

---

## Tests

Bouwplan: **runClient** (visuele check). Daarnaast goedkoop en automatisch: script, B, D.

- [ ] **Script:**
  - `java tools/textures/Ring2Textures.java` draait zonder fout en de ingebouwde controles slagen (afmetingen, alpha 0/255, gelijk alpha-masker per frame, grasblok-regel);
  - tweede run → `git status` schoon (byte-identiek, ook de `.mcmeta`'s);
  - `Ring1Textures.java` en de ring 1-PNG's zijn ongewijzigd;
  - het contactblad `build/texture-preview/ring2.png` bestaat met beide secties en de extra rijen (ores, strata, cracked clay, salt flat).
- [ ] **B:** `./gradlew build` slaagt.
- [ ] **D:** `./gradlew runData` slaagt zonder "texture does not exist". De `git diff` van `src/generated` raakt alleen:
  - de emissive kruismodellen (vraag 1/3);
  - `glow_mold` (vraag 4);
  - `particles/sand_gust.json` (vraag 7).

  Alle andere gegenereerde bestanden zijn ongewijzigd, ook `soul_crystal_ore.json` en de zonnesteen-/turkoois-models. De output wordt gecommit.
- **S:** niet nodig, want er verandert geen common- of client-code. Alleen als de bouw toch iets buiten datagen raakt: `tools/server-smoke-test.sh` met `level-name=smoke-change37`.
- [ ] **runClient — visuele checklist** (testwereld seed `20260930`, preset `corrupted_world`; mipmap 0 en 4, smooth lighting aan/uit):
  - [ ] De log bevat geen `Missing textures in model` / `Unable to load model` / `Unable to parse animation metadata` voor `corruptedadventure`. F3+T (resource reload) gaat zonder fouten.
  - [ ] Creative tab: alle ring 2- en Nether-ring 2-blokken en -items hebben hun definitieve texture (geen paars-zwart). Items `raw_sunstone` en `dew_crystal` pulseren (vraag 1); `raw_turquoise` is still.
  - [ ] **Kloofsteen** (`/ca tpring 2`, dan `/locate biome corruptedadventure:canyonlands`): de kloofwand leest als gelaagd gesteente in banden. Er is geen streep-per-blok-patroon (vraag 5) en er zijn geen zichtbare naden in een 5×5-wand per tint. Keien en bakstenen staan herkenbaar naast vanilla-keien en -steenbakstenen. Muur, trap en plaat zijn in orde.
  - [ ] **Kloofkeien-palet** (teststop 2, punt 3; expliciet aandachtspunt): `canyon_cobblestone` in een keienveld en in rotsval aan een wandvoet (23c) past bij de drie tinten en leest als gebroken kloofsteen, niet als een vreemde vierde kleur. Crème banden (23b-13 B) en grind op puinhellingen vloeken niet.
  - [ ] **Scorched sandstone** (`hoodoo_fields`, `dry_riverbeds`): top/zijkant/bodem passen op elkaar, cut/chiseled/smooth zijn opgebouwd zoals vanilla. Het is niet te verwarren met vanilla- of rode zandsteen.
  - [ ] **Los zand:** naast vanilla-zand subtiel donkerder/korreliger, maar op afstand een zandplafond; in een zandgrot (change 26) leest het plafond als zand.
  - [ ] **Gebarsten klei** (`dry_riverbeds`): een groot vlak zonder zichtbare herhaling (willekeurige rotatie).
  - [ ] **Flora:** dorre struik, woestijnbloem en tonnetjescactus (ook in de pot) zonder zwarte randen. De cactustexture past op zijn vorm (geen uitstekende pixels buiten de hitbox).
  - [ ] **Palm** (`oasis_flats`): de stam leest als palm (horizontale schubben), de kroon met bladwaaiers oogt niet als eikenblad, bladeren zijn doorzichtig (ook bij Graphics: Fast). Planken, deur (beide helften, open/dicht, beide scharnieren) en valluik zijn in orde. Zaailing in de hand, op de grond en in de pot. Palmhout is niet te verwarren met oak, birch, jungle of acacia.
  - [ ] **Ertsen** (zonnesteen hoog in canyonwanden, turkoois diep; of `/setblock` van elke variant naast zijn gastgesteente):
    - elke variant valt weg in zijn eigen laag;
    - zonnesteen gloeit in een donkere grot en pulseert vloeiend, alle 4 varianten gelijk op;
    - turkoois is mat en duidelijk anders dan zonnesteen;
    - geen z-fighting of flikkering van de overlay op afstand (mipmap 4).
  - [ ] **Zout** (`salt_flats`): de vlakte is wit en vlak met zichtbare zeshoekige polygonen zonder herhaling. Korst-zijkant sluit aan op het zoutblok eronder. Het zoutkristal is op alle zes zijden juist (vraag 3).
  - [ ] **Nether-ring 2** (`/ca tpring 2 nether`, `/locate biome corruptedadventure:dripshroom_forest` / `spore_caves`):
    - doorweekt netherrack is duidelijk anders dan vanilla-netherrack;
    - sporenmos-zijkant sluit aan op de grond;
    - druipzwam-stam (animatie, vraag 4), hyfen, gestript en planken; de set is niet te verwarren met crimson/warped (vraag 6);
    - hoed, zwamlamp en gloeischimmel (gloeit in het donker, vraag 4);
    - stalactieten in alle diktes, hangend en staand, samengevoegd (`tip_merge`).
  - [ ] **Dauw:**
    - de dauwbron leest als "water in een blok" en golft (vraag 1);
    - de kristalstadia gloeien en pulseren gelijk op;
    - dauwkristal is duidelijk anders dan zielkristal (vraag 2): leg `soul_crystal` en `dew_crystal` naast elkaar in de inventory, en zet een `soul_crystal_ore` naast een `dew_crystal_cluster`.
  - [ ] **Particles** (vraag 7 A): `/ca weather 2 sandstorm` (change 20/28): de zandkorrels zijn herkenbaar en hebben geen zwarte vierkantjes.
  - [ ] **Stijl naast ring 1:** dood hout, palm en druipzwam zijn drie duidelijk verschillende houtsoorten. Zonnesteen, zielkristal en dauwkristal zijn drie duidelijk verschillende kristallen.

**Blokkerende handmatige test van deze change zelf: nee.** Alles waarop latere changes bouwen, controleren het script, B en D automatisch: bestandsnamen, models, `.mcmeta`'s, alpha-regels en de byte-identieke ring 1. Het uiterlijk is alleen visueel te beoordelen, maar geen latere change bouwt erop: ring 3 heeft een eigen texturescript en dezelfde stijlregels uit §1.4. De visuele checklist hierboven is daarom W-achteraf, maar wordt afgelopen **in teststop 3** hieronder. Advies: laat de gebruiker eerst het contactblad `build/texture-preview/ring2.png` bekijken (kan zonder game). Feedback daarop wordt een iteratieronde (backlogregel) en houdt de bouw niet tegen.

### Teststop 3 — gebundelde eindcheck ring 2 + Nether-2 (BLOKKEREND, besloten 2026-10-01)

Na de commit van deze change, vóór change 38. De agent bereidt alles voor (verse wereld seed `20260930`, preset `corrupted_world`, dedicated server + `Dev`/`Dev2` waar twee clients nodig zijn, RCON-hulpcommando's, contactblad); de gebruiker loopt de lijsten af en geeft akkoord. Bevindingen worden backlogregels of een fix-change in de juiste categorie.
1. **Nether-2 (blokkerend, verhuisd uit change 35):** de lijst "Blokkerend in teststop 3" uit `change-35.md` (zwamkathedralen, zuilen, lavazee met eilanden en dam, sporengrotten, dauwgeodes, grenzen zonder lavawand).
2. **Textures (deze change):** de runClient-checklist hierboven.
3. **W-achteraf-lijsten** uit `change-21.md` t/m `change-36.md` (blokken incl. instortend `loose_sand`, palm, uitdroging, zandstorm/hittegolf op twee clients, zout, `dry_riverbeds`, ring 2-sfeer en zingend zand, Nether-2-blokken, dauwgroei, Nether-2-sfeer). Wat al in teststop 1, 2 of **2b** akkoord is, hoeft niet opnieuw: de terreinlijsten van 23/23b/23c, ertsen (25), grotten en zandgrotten (26) en extra biomes/hoodoos (30) zijn in teststop 2b (na change 30) afgelopen.
   - **Expliciet vragen:** het nieuwe palet van `canyon_cobblestone` (teststop 2, punt 3) in een keienveld naast een kloofwand.
4. **Standaard-W:** midden van elke ring en elke grens (overworld en Nether) en een portal vanuit elke ring; geen fouten in client- of serverlog.

---

## Risico's / aandachtspunten

- **Kwaliteit van gegenereerde textures** ("programmer art") bij ~69 bestanden. Mitigatie: het contactblad met extra rijen (strata, ores, vlakken), iteratierondes achteraf en `HAND_EDITED`. Reken op 1–2 rondes per set.
- **Kloofsteenlagen:** een laaglijn in elk blok herhaalt zich per meter en maakt een streepjespatroon dwars door de banden van change 23 (vraag 5).
- **Kristallen lijken op elkaar:** het werkpalet van dauwkristal (change 34) ligt dicht bij zielkristal (cyaan). Zonder bijsturing kun je de sleutelmaterialen van Nether-ring 1 en 2 verwarren (vraag 2).
- **Overlay op vier ondergronden:** een gouden zonnesteen-overlay kan op oranje en crème wegvallen. De 1-px donkere omlijning en de "ores"-rij in het contactblad bewaken dat.
- **Emissive kruismodel:** een eigen elementmodel in plaats van het vanilla-`cross`-parent kan iets anders ogen (shading, rescale). Vergelijk het in runClient met een vanilla-amethistcluster. Sodium/Embeddium en shaderpacks negeren `neoforge_data` mogelijk. Dan blijft een gewone, niet-gloeiende texture over (geen crash).
- **Animaties:**
  - `interpolate` op cutout-textures mengt ook alpha. Het script controleert dat het masker per frame gelijk is;
  - `cycling` zonder interpolate is alleen veilig op opake textures (dauwbron, stam);
  - te snelle of te drukke animatie op grote vlakken (dauwbron, stam in een zwamkathedraal) is onrustig. Daarom trage `frametime`'s.
- **`runData` faalt** als een model naar een nog niet bestaande texture verwijst. Volgorde aanhouden: eerst het script, dan `runData`.
- **Afwijkende antwoorden in changes 21–34** (minder of meer blokken, andere itemnamen, biome-tint op palmbladeren, twee dauwstadia): volg de lijst in `CODEBASE.md`, niet deze tabel.
- **Vorm vs. texture:** de cactus-, zoutkristal- en dauwstadia-vormen liggen vast in `blocks`-code. Zo nodig gaat een vormwijziging naar de backlog, niet naar deze change.
- **Particle-sprites:** als `SandGustParticle` de sprite niet tint of geen sprite-set per leeftijd gebruikt, moeten de sprites anders getekend worden (in kleur, of één frame). Volg de class.
- **Bestaande testwerelden:** geen probleem, er veranderen alleen assets.

---

## Ontwerpvragen

Ronde 1: vraag 1–4 (gloed en kleur van de sleutelmaterialen en de Nether-ring 2). Ronde 2: vraag 5–7 (ring 2-stijl en particles).

### 1. Krijgt de dauw-set gloed en animatie?
Design: dauwbron "oogt water-achtig en druppelt", dauwkristal is het sleutelmateriaal van Nether-ring 2. Volgens §1.4 zijn erts-overlays emissive en pulseren ze. De dauwkristallen zijn geen erts, dus dat staat nog open.
- **A. Kristalstadia emissive + de §1.4-puls (zoals het zielkristal); de dauwbron golft traag, opaak en niet emissive; item `dew_crystal` pulseert mee (aanbevolen).**
  - Het sleutelmateriaal valt op in donkere sporengrotten en voelt even waardevol als het zielkristal.
  - De bron leest als "water" zonder fluid.
  - Kost een emissive kruismodel (datagen-helper, ook bruikbaar voor het zoutkristal) en 6 `.mcmeta`'s.
  - Geen server-impact. Met Sodium/shaders mogelijk geen gloed.
- **B. Kristallen emissive, maar statisch; dauwbron statisch.**
  - Minder werk (geen `.mcmeta`).
  - Dauwkristal oogt rustiger dan zielkristal, en de bron is "een blauw blok".
- **C. Geen emissive; alleen de dauwbron golft.**
  - Geen modelwijziging. Het licht uit change 34 (stadia 1–5) maakt ze al vindbaar.
  - In het donker vallen ze minder op dan zielkristal.
- **D. Alles statisch.**
  - Eenvoudigst. De dauw-set voelt het minst bijzonder van alle sleutelmaterialen.

Antwoord: **A** — kristalstadia emissive + §1.4-puls; dauwbron traag golvend (`cycling`), opaak, niet emissive; item `dew_crystal` pulseert mee.

### 2. Welke kleur krijgt dauwkristal?
Het werkpalet uit change 34 (`2A7F99 … E8FBFF`) ligt dicht bij zielkristal (zielblauw/cyaan `0B3446 … E6FFFF`).
- **A. Helder, bijna kleurloos "waterdruppel"-kristal: wit-transparant ogend met een lichtblauwe kern (`1B4A5C 5E9FB4 A6D3E0 D6EEF4 F4FCFF`), gloei bijna wit (aanbevolen).**
  - Leest als "dauw" en "water".
  - Duidelijk anders dan het verzadigde cyaan van zielkristal, ook in de inventory.
  - Wit zoutkristal (ring 2) is matter en heeft een roze zweem, dus geen verwarring.
- **B. Groenig aqua (`1E5A4E … C8F5E6`), passend bij de grijsgroene Nether-ring 2.**
  - Hoort visueel bij de sporengrotten.
  - Kan tegen de groene gloeischimmel en het slijm wegvallen.
- **C. Werkpalet van change 34 houden (lichtblauw/cyaan).**
  - Geen extra keuze.
  - Wel kans op verwarring met zielkristal, zeker met dezelfde puls.

Antwoord: **A** — helder, bijna kleurloos "waterdruppel"-kristal (`1B4A5C 5E9FB4 A6D3E0 D6EEF4 F4FCFF`), gloei bijna wit.

### 3. Gloeit het zoutkristal?
Design: "zoutkristal (decoratie, zwak licht)". Geldt alleen als change 29 vraag 5 niet al B (emissive) koos.
- **A. Nee: still, niet emissive; het witte kristal en licht 3 volstaan (aanbevolen).**
  - Past bij "decoratie, zwak licht".
  - De gloed blijft voorbehouden aan sleutelmaterialen (zielkristal, zonnesteen, dauwkristal). Geen extra model.
- **B. Emissive, statisch.**
  - Kristallen lichten 's nachts op de zoutvlakte op: sfeervol.
  - Ze concurreren wel met zonnesteen als "gloeiend ding in ring 2". Hergebruikt het emissive kruismodel uit vraag 1.
- **C. Emissive + puls.**
  - Het meest levendig. De hele zoutvlakte knippert zacht, wat bij veel kristallen druk kan ogen.

Antwoord: **A** — zoutkristal still, niet emissive.

### 4. Welke animatie en gloed krijgt de rest van Nether-ring 2?
Vanilla: de crimson/warped-stamzijkant is geanimeerd, glow lichen en shroomlight zijn niet emissive (wel lichtbron). Change 33 liet dit uitdrukkelijk open.
- **A. De slijmstrepen in de druipzwam-stam glijden traag omlaag (opaak, 16 frames, `frametime` 8); gloeischimmel emissive en statisch; zwamlamp, hoed en stalactieten still (aanbevolen).**
  - De "druipende" zwamkathedralen leven, net als vanilla-Nether-stammen.
  - De "lichtgevende sporengrotten" krijgen echt gloeiende plekjes.
  - De lamp is met licht 15 al fel genoeg. Kost 1 `.mcmeta` en één emissive vlakmodel.
- **B. Alleen gloeischimmel emissive; alles verder still.**
  - Minder beweging. Grote stammen ogen dan statisch naast vanilla-Nether-hout.
- **C. Alleen de stam-animatie; geen emissive.**
  - Geen modelwijziging. De schimmel is zichtbaar door zijn licht 7, maar "gloeit" niet in de texture.
- **D. Alles still.**
  - Eenvoudigst. Nether-ring 2 voelt statischer dan Nether-ring 1 (vanilla-stammen bewegen).

Antwoord: **A** — slijmstrepen in de druipzwam-stam glijden traag omlaag (16 frames, `frametime` 8); gloeischimmel emissive en statisch; zwamlamp, hoed en stalactieten still.

### 5. Hoe toont kloofsteen zijn gelaagdheid?
Change 23 bouwt de lagen uit banden van 2–7 blokken per tint. Het werkpalet van change 21 tekent daarnaast 1–2 laaglijnen in elk blok.
- **A. Geen harde laaglijn per blok: horizontaal gerekte korrel en zachte tintvariatie die naadloos verticaal tegelen. De lagen komen uit de blokbanden (aanbevolen).**
  - Een kloofwand leest als echte sedimentlagen met brede banden.
  - Geen streepje elke meter.
  - Elke tint blijft bruikbaar als bouwblok.
- **B. Per blok 1–2 golvende laaglijnen (werkversie change 21).**
  - Meer detail van dichtbij.
  - Op afstand een regelmatig streepjespatroon dat dwars door de banden loopt en de herhaling per blok verraadt.
- **C. Per tint een eigen structuur (rood grof en korrelig, oranje gestreept-fijn, crème glad).**
  - Banden verschillen ook in textuur, niet alleen in kleur.
  - Meer tekenwerk. Een gestreepte tint heeft het risico van B.

Antwoord: **A** — geen harde laaglijn per blok; horizontaal gerekte korrel en zachte tintvariatie; de lagen komen uit de blokbanden van change 23.

### 6. Welke kleurrichting krijgt het druipzwam-hout?
Het design noemt geen kleur. Het werkpalet van change 33 is grijsgroen-paars (stam) met lichter grijsgroen hout.
- **A. Grijsgroen-paars met groene slijmstrepen; planken lichter grijsgroen (werkpalet, aanbevolen).**
  - Past bij de grijsgroene fog van Nether-ring 2.
  - Duidelijk anders dan crimson (rood), warped (turkoois), dood hout (grijsbruin) en palm (zandgeel).
- **B. Bleek blauwgrijs ("doorweekt"), slijm lichtgroen, planken bijna wit-grijs.**
  - Leest sterk als nat en verbleekt.
  - Dicht bij dood hout (verbleekt) en mogelijk bij birch.
- **C. Donker olijfgroen-bruin.**
  - Zwammig en organisch.
  - Donkere planken lijken op dark oak/mangrove in bouwwerken.

Antwoord: **A** — grijsgroen-paars met groene slijmstrepen; planken lichter grijsgroen.

### 7. Krijgen de ring 2-particles eigen sprites?
Change 28 gebruikt voor `sand_gust` de vanilla-sprites `generic_*` en zette "eigen sprite in change 37" in de backlog. Bij change 36 vraag 4 B zijn er al placeholder-sprites voor sporen.
- **A. Ja: 4 eigen sprites `particle/sand_gust_{0..3}` (8×8, lichte grijswaarden, door de particle getint) in `Ring2Textures.java`; `ModParticleDescriptionProvider` verwijst ernaar. Spore-sprites (bij 36 4B) worden definitief (aanbevolen).**
  - Zandstorm oogt als zandkorrels in plaats van rookwolkjes.
  - Puur asset en datagen, geen code. Past bij "textures per ring".
- **B. Nee: vanilla-sprites houden; backlogregel blijft staan.**
  - Niets te doen. De zandstorm blijft "rokerig".
  - Spore-sprites (bij 36 4B) worden dan alleen in pixels afgemaakt.

Antwoord: **A** — ja: 4 eigen sprites `particle/sand_gust_{0..3}` (8×8, grijswaarden, getint); `ModParticleDescriptionProvider` verwijst ernaar.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | textures | Ring 2 en Nether-ring 2 definitief uit `tools/textures/Ring2Textures.java` (één script voor ring + Nether-tegenpool), stijl §1.4; paletten per set in `fase-1-wereld.md` Ring 2 / Nether-ring 2 | Zelfde productiemethode als ring 1; licentie-schoon en reproduceerbaar |
| 2026-10-01 | textures | Puls met gloeikleur per materiaal (zonnesteen warm goud); opake bewegende textures (`cycling`) zonder interpolate; het script controleert afmetingen, alpha 0/255, gelijk alpha-masker per frame en de grasblok-regel | §1.4 generiek voor meerdere materialen; stijlregels automatisch bewaakt |
| 2026-10-01 | textures | Dauw-set: kristalstadia emissive + puls, dauwbron traag golvend (opaak, niet emissive), item pulseert mee | Sleutelmateriaal valt op; bron leest als water |
| 2026-10-01 | textures | Dauwkristal-kleur: helder, bijna kleurloos met lichtblauwe kern, gloei bijna wit | Leest als dauw; duidelijk anders dan zielkristal |
| 2026-10-01 | textures | Zoutkristal: still, niet emissive | Gloed voorbehouden aan sleutelmaterialen |
| 2026-10-01 | textures | Nether-ring 2-animatie en -gloed: stam-slijm glijdt (16 frames, `frametime` 8), gloeischimmel emissive statisch, rest still | Kathedralen leven zoals vanilla-Nether-stammen |
| 2026-10-01 | textures | Kloofsteen-gelaagdheid: geen laaglijn per blok, lagen uit de blokbanden | Geen streepjespatroon per meter |
| 2026-10-01 | textures | Druipzwam-kleurrichting: grijsgroen-paars met groene slijmstrepen, planken lichter grijsgroen | Past bij de fog; anders dan crimson/warped/dood hout/palm |
| 2026-10-01 | textures | Particle-sprites ring 2: eigen `sand_gust_{0..3}` | Zandstorm oogt als zandkorrels |
| 2026-10-01 | textures | `loose_sand`-texture subtiel donkerder/korreliger dan vanilla-zand (routinekeuze agent, gemarkeerd); items `raw_sunstone` (puls) en `raw_turquoise` (still) volgen 24 vraag 3 B | Zandgrot-plafond van dichtbij herkenbaar zonder op te vallen |
| 2026-10-01 | textures | Textures-changes tekenen binnen de bestaande vormen; vorm- en `MapColor`-wijzigingen gaan als `blocks` naar de backlog | Eén categorie per change; vormen zijn Java-code |

Commit (na `/code-review`): `textures: finalize ring 2 and nether ring 2 textures`.
