# Ring 4 — golf 2: inrichting, overleving en sfeer

**Status:** gespard 2026-10-16: Q1–Q21 (§4), G1–G5 (§4b) en de flora-sparring F1–F12 (§6) beantwoord. H1–H5 (§6c) beantwoord: geen open punten.

Scope per pakket: één categorie (CLAUDE.md §2). Basis: `main` na teststop 5a2 (89e8831); W-terrein en W-biomes pas na de prestatiefix van de `steam_fens`-zone (Q1). Namen en sleutels: `contract.md` (vast; wijzigingen via main, zie §5). Werkwijze: `waves.md` §2 (golf 2), §4 (worktrees, licht testen), §5 (eigenaarschap), §6 (INT2). Ontwerp: `fase-1-wereld.md` → Ring 4 ("Sparring golf 2 (2026-10-16)"), bouwplan F (72–80, 71b).

## 0. Wat sinds golf 1 veranderde (geldt voor alle stromen)
- **Zone 3→4 is 256 breed** (`transitionWidthOverrides` [0, 0, 256, 256, 0]); `steam_fens` is alleen nog de bovenste ~60 (ring 3-helft), de rest is het ring 4-overgangsbiome **`verdant_falls`** (`transition_splits.ring_3`, in `is_ring_4`, `heat_refuges` en `evaporation_refuges`). De klif 3→4 en `Ring4Cliff` bestaan niet meer; de DF `ring_4/overworld/cliff` betekent nu "exacte verdant-wand".
- `verdant_falls` heeft eigen terrein (`VerdantDescent`, `VerdantBasins`, `VerdantGorges`, `RingArc`), een eigen valcap (6/chunk, los van de 3 eilandvallen), een grotband die alleen gericht opent (nissen, tunnels, richelpad; `Column.openFrom/openTo`), een voetrivier op Y 63 en `before_split` op de `steam_fens`-features.
- `verdant_falls` is nu kaal (alleen `VerdantFallsFeatures`: mostapijt, `ledge_strand`). **De eigen tropische flora komt in golf 2** (Q4 = B): 72b (blokken), 71b (textures), 75b (aankleding).
- Ring 4-wortels en -hangmos zijn klimbaar (`minecraft:climbable`, heel ring 4).
- `Ring4Atmosphere` bestaat (achter `BiomeAmbience.ring4`) met de `verdant_falls`-look, `waterfall_mist`, `Ring4Sounds.java` en twee ambient-events; de overige ring 4-biomes hebben alleen een cave mood.
- `Ring4Features.ORDER` is de globale featurevolgorde (één lijst, zoals ring 3); nu alleen `verdant_falls`.
- Eilanden liggen alleen buiten de zones (`IslandPlacement`).
- Prestatie: de `steam_fens`-zone zat bij INT verdant_falls op **1,90×** (budget 1,75×); fix op `ring4/vf-perf2`. W-terrein en W-biomes takken af ná die merge (Q1).

## 1. Stromen en pakketten

| Stroom | Branch | Poort (n) | Pakketten (volgorde) | Categorie | Start / afhankelijk van |
|---|---|---|---|---|---|
| A-0 | `ring4/atmosphere-g2-0` | 5 | **80-0** sound events + particle types (registratie, vanilla-placeholderaudio, sprites uit 71a), direct op main | `atmosphere` | nu |
| B | `ring4/blocks-g2` | 0 | **72** gameplayblokken → (na F-sparring) **72b** tropische flora | `blocks` | na 80-0; 72 merget vooruit naar main |
| W-terrein | `ring4/terrain-g2` | 1 | **73** tunnelnet | `worldgen` | na `vf-perf2`; 72 (tot dan placeholders) |
| W-terrein-b | `ring4/terrain-g2b` | 8 (server 25578, RCON 25588) | **74** wereldboom, hemelmeren, rotsbogen, eilandparen | `worldgen` | na `vf-perf2` |
| W-biomes | `ring4/biomes-g2` | 2 | **75** → **76** → **77** → **75b** flora `verdant_falls` | `worldgen` | na `vf-perf2`; 72 (vooruit), 73 (voor 77), 74 (voor 75), 72b (voor 75b) |
| W-nether | `ring4/nether-g2` | 3 | **78** Nether-4: fossielen als terrein + aankleding | `worldgen` | nu |
| C | `ring4/conditions-g2` | 4 | **79** zuiverend + gifkoppelingen (+ zuigmodderschade, zie G2) | `conditions` | nu (testdatapack tot 72) |
| A | `ring4/atmosphere-g2` | 5 | **80** sfeer ring 4, zone 4→5, Nether-4 | `atmosphere` | na 80-0 |
| T | `ring4/textures-g2` | 6 (geen server) | **71b** nieuwe golf 2-behoeften + flora-textures | `textures` | nu; flora na F-sparring |

- Geen aparte I-stroom (Q21 = B): de tools (gifscan, `MapPalette`-kleuren, waterscan-settle) gaan naar INT2.
- Elke stroom: eigen agent, eigen worktree, eigen plan `docs/plans/ring-4/golf-2-<stroom>.md` met "CODEBASE-delta", "Beslissingen", "Backlog" onderaan; niet in `CODEBASE.md`, `docs/design/*`, `docs/backlog.md` (INT).
- Licht testen per pakket (B, D + `git diff --exit-code src/generated` voor andermans output, unit tests); geen runServer/determinisme/perf per pakket; `C:/Temp/ca-user-test.lock` respecteren. `/code-review` per stroom-pakket.
- Gedeelde bestanden: `waves.md` §5. W-terrein en W-terrein-b raken beide `IslandField`/`RingAquifer`: 73 alleen de ring 4-floodedness en een tunnel-accessor, 74 alleen nieuwe klassen + een haak in `Column`/`IslandField` (`worldTrees`, meer, boog, paren); INT houdt beide kanten.

## 2. Per stroom

### A-0 — 80-0 (`atmosphere`)
Alle sound events (contract §6, ambient + blok) en particle types (`will_o_wisp`, `blossom`, `firefly`, `swamp_gas`, `toxic_bubble`, `updraft_bubble`, `glitch_speck`, `bone_dust`) registreren met providers in `client/particle/`, vanilla-placeholders in `sounds.json` en de 71a-sprites; direct op main (zoals 50-0 bij ring 3). Voegt 72b een geluid/particle toe, dan via main.

### B — 72 gameplayblokken, 72b tropische flora (`blocks`)
**72 — af als:**
- `swamp_gas`: onzichtbaar, niet-solide, vervangbaar; gele particle `swamp_gas` in `animateTick`; ontsteken door vuur, fakkel, lava, vuursteen of een ontploffing ernaast. **Ketting (Q9 = A):** de ontsteking verspreidt via scheduled ticks (max. ~64 blokken per tick, nooit recursief in één tick); per cluster van ~8 blokken één echte ontploffing (kracht ~2,5), max. 4 per tick; de rest dooft als vlam-particles. Blokschade volgt `mobGriefing`; geluid `block.swamp_gas.ignite`. In `poison_gases`.
- `sucking_mud` (**Q12 = B, dodelijk drijfzand**): je zakt er helemaal in weg, ~1 blok per 2 s (**G2 = A**); met het hoofd boven kun je er traag uitspringen; met het hoofd onder krijg je schade, maar blijven springen brengt je er nog uit. Altijd herkenbaar (**G1 = A**): natte, glanzende texture (T) en af en toe een bel en een slurpgeluid (`animateTick`; event via 80-0, contract-delta). Schade (**G3 = A**): eigen damage type `corruptedadventure:sucking_mud` met doodsbericht, `DifficultyFloor.LETHAL_FROM_NORMAL` (peaceful niets, easy tot 10 HP, normal/hard dodelijk); mobs zakken ook weg, items zinken niet. Implementatiekeuze: de damage-type-sleutel en bootstrap in `Ring4DamageTypes` door C (contract §8), B gebruikt hem.
- `updraft` (**Q11 = A**): gedeelde code in `entityInside` zoals de vanilla bubble column (draait logisch op beide kanten, geen client-class); duwt spelers en items omhoog; `updraft_bubble`; `block.updraft.flow`.
- `toxic_pool` als eigen vloeistof volgens contract §3; `toxic_bubble`; in `poison_sources`.
- `bubble_spring`: geluid `block.bubble_spring.bubble` en eigen bel-particle.
- Sporen: `clawed_tidestone`, `slime_trail`, `creature_nest`.
- `moonpetal_leaves`: `blossom` in `animateTick` (backlog r. 271).
- Paden met T bevestigen (backlog r. 277); `ring_4_stones` aanvullen.
**72b — af als:** de 29 flora-blokken van §6b (+ item `verdant_fruit`) (houtset `verdant_` + planten), met gedrag, loot, tags (`#logs`, `#logs_that_burn`, `#planks`, `#leaves`, `#saplings`, houtset-tags, `minecraft:climbable` voor de liaan, `parrots_spawnable_on`), lang en `ModFlammables`; de sapling groeit `ring_4/verdant_tree_fern` (key van W-biomes, 75b). Geen borden, boot of potten; recipes naar de `items`-backlog (H1 = A). Fruit als peul aan de stam (H3 = A).

### W-terrein — 73 tunnelnet (`worldgen`)
**Af als:**
- DF `ring_4/overworld/tunnels` (per blok), tunnels 5–12 breed.
- **Watermodel (Q5 = A):** per band. Mythisch: overstroomd tot zeeniveau met luchtkamers als koepels op hoge punten (vlak waterniveau, gesloten). Bedorven: droog, met moerasgaskamers (gesloten holtes vol `swamp_gas`) en gifpoelen (alleen bronnen in gesloten kommen, nooit tegen water). In de bandovergang gemengd, met gesloten overgangen.
- **Ingangen en zones (Q6 = A):** ingangen via gatmeren op zeeniveau (water Y 62) en via zinkgaten (droog). Geen ingangen onder gatmeren boven zeeniveau. Geen tunnels in zone 3→4 en 4→5: ze stoppen ≥ 32 blokken vóór de zone.
- Eigen ring 4-floodedness en lokale aquiferniveaus in `RingAquifer`: geen vrij water tegen lucht buiten de vastgelegde vallen.
- Tot de merge van 72: vanilla-placeholders, daarna de echte blokken.
- Accessor voor W-biomes (77): tunnels en kamers binnen een vak.
- Tests: determinisme, geen water tegen lucht in een tunnelvak (offline), geen lava, prestatie indicatief (tunnels per cel, lui, alleen onder het oppervlak).
**verdant_falls:** niets in de zone (Q6); de afdaling, kloven, voetrivier, randmeren, grotband, `verdantWater`, `ring3Seal` en de dammen blijven ongemoeid.
**Backlog die hierbij hoort:** r. 89, 290 (a) (b), 319, 320; `golf-1-terrain.md` r. 150. r. 290 (c) vervalt met de klif.

### W-terrein-b — 74 wereldboom, hemelmeren, rotsbogen (`worldgen`)
**Af als:**
- **Wereldboom (Q7 = A, volledig terrein):** ~1 per 2.500 boog (~15), alleen in de mythische band, nooit in een zone, deterministisch. Stam 8–12, 80–140 hoog, wortels die een half losgerukt eiland vasthouden, gatmeer eronder, vallen langs de wortels (water-tak, eigen cap). Stam, wortels en kruin uit het veld; de surface rule kiest `moonpetal_log`/`_wood`/`moonpetal_leaves` (bladeren `persistent`). `worldTrees` gevuld.
- **Hemelmeren (Q8 = A):** meren (en korte beken) op grote hoge eilanden, waar de vallen van 61 uit kunnen ontspringen; in `withered_gardens` opgedroogde kommen zonder water. Het meer is per blok ingesloten (waterscan 0).
- **Rotsbogen tussen eilanden (Q8 = A):** af en toe een natuurlijke boog tussen twee nabije eilanden (terrein).
- **Accessor eilandparen** voor de ranken van 75.
- Tests: determinisme, waterscan op eilandmeren, noise-prestatie bij de boom.

### W-biomes — 75, 76, 77, 75b (`worldgen`)
**75 — af als:** aankleding van de vijf mythische biomes en `sky_gardens`: `moonpetal`-bomen (incl. `ring_4/moonpetal_tree` voor de sapling), moerasbomen, vanilla spar/donkere eik met mos, `giant_lily_pad`, `lotus`, gloeiflora, koraal/zeegras/sea pickles, hangende (gloei)wortels en ranken onder de eilanden (lage eilanden tot vlak boven de grond), ranken tussen de eilandparen van 74 (per chunk een deel), planten rond de hemelmeren van 74, `bubble_spring` in diepe poelen. **Gloed onder de eilanden** (besluit teststop 5a): licht ≥ 1 op droog land in de eilandschaduw; mythisch `aquamarine`-knoppen + `glow_bloom`/`glowcap`.
**76 — af als:** de drie bedorven biomes en `withered_gardens`: `withered_moonpetal`-bomen, `reeds`, veen, `sucking_mud` (**G1 = A**: kleine plekken ≤ 4×4, 2–3 diep, op oevers in `peat_moors` en `drowned_forest`; niet midden in het riet van `reed_marsh`), gifpoelen, **moerasgas aan het oppervlak (Q10 = A)**: gesloten gasbellen net onder de veenkorst en 1–2 lagen gas in zinkgatbodems; `blighted_glowcap`; dichter richting ring 5. **Gloed onder de eilanden (Q13 = B):** `blighted_glowcap`, `glowcap` **en gifkristal aan het oppervlak** (**G4 = B**: ook clusters, licht 5, die scherven droppen; gevolg: `toxic_crystal_shard` is vroeg en makkelijk te vinden, fase 2 houdt daar rekening mee; budding-blokken alleen ondergronds, **H5 = A**).
**77 — af als:** **ertsen (Q14 = A)**: aquamarijn-geodes half open in de bodem onder diep water (lagune-troggen, fjorden, overstroomde tunnels); gifkristal (budding) in de wanden van de droge bedorven tunnels en gaskamers (de gifgrotten); `tidesilver` in de tunnelwanden; jade diep in overstroomde tunnels; vanilla-ertsen ×0,2 / ×0,15 / ×0,1. Sporen van wezens in de tunnels (klauwkras, slijmspoor, nesten, botresten met `bone_block`). Controle van de dierspawns.
**75b — af als:** de flora in `verdant_falls` volgens §6b (plaatsing per deel), `ledge_strand` binnen `verdant_falls` met `verdant_vine` i.p.v. `hanging_swamp_moss` (klimroutes blijven 100 %), papegaaien in de spawns, de configured feature `ring_4/verdant_tree_fern` voor de sapling.
**Opruimen:** `placeholder_ring_4`/`placeholder_nether_ring_4` weg (backlog r. 265).
**verdant_falls:** behalve 75b geen 75–77-feature in `verdant_falls`; grote features van de grondbiomes niet over de voetrivier of de buitenoever laten hangen; ertsen mogen de exacte wanden niet openen. Backlog r. 226 vervalt (INT sluit hem).

### W-nether — 78 (`worldgen`)
**Af als:**
- **Zeewezen-fossielen als terrein (Q15 = B):** een eigen veld (patroon `GiantGeodes`/`RockPillars`) in de Nether-4-DF, ribbenkasten, ruggengraat en schedel tot 80+ lang, `bone_block` via een surface-conditie op het veld. Plaatsing (**G5 = A**): alleen in `bone_dunes`, ~1 per 250 × 250 Nether-blokken, half begraven; gangen tussen de ribben ≥ 4 breed, binnenkant hol en vlak (ruimte voor de fossiele tempels van fase 3); nooit in de zones of boven de lavazee. Budget ≤ 1,75× (alleen kolommen met een fossiel rekenen per blok); de ruimte-check van 65 (beloopbaarheid duinen, plafond) opnieuw; dammen van trede 3|4 en 4|5 heel.
- Bekleding van de kristalzuilen, geodebekleding via `GiantGeodeShape` + `find(root)` (backlog r. 284), `bone_dust`-aankleding.

### C — 79 (`conditions`)
**Af als:** `RateModifier.Stage.PURIFYING` in Nether-4 (×3 op de daling, config); de koppelingen met de echte blokken van 72 getest; geen gif uit `sucking_mud`, `slime_trail`, gifzwammen of gifkristal. Damage type `corruptedadventure:sucking_mud` (G3 = A): sleutel en bootstrap in `Ring4DamageTypes`, doodsbericht in `Ring4ConditionText`, `DifficultyFloor.LETHAL_FROM_NORMAL` (implementatiekeuze: C bezit de damage types van ring 4; B roept hem aan).

### A — 80 (`atmosphere`)
**Af als:**
- Kleuren naar `Ring4Atmosphere` + haak `BiomeAmbience.netherRing4` (backlog r. 268).
- Biome-particles en nachtgloed; **nachtkoor en nachtgloed alleen 's nachts (Q19 = A)** via een client-controller in `client/` (zoals het gerommel van ring 3).
- Eigen audio uit `Ring4Sounds.java` (incl. de drie weerloops, backlog r. 269; gekraak in de storm, niet positioneel).
- Spanning: geritsel in het riet, bellen, **silhouet onder water (Q18 = A)**: een donkere, enorme billboard-particle ver in diep water (≥ 20 diep), een paar seconden, zeldzaam (≤ 1 per ~20 min per speler).
- **Zone 4→5 (Q17 = A):** een client-hook die lucht, fog en water geleidelijk ontkleurt over de zone (gesyncte layout); drone, gekraak, `glitch_speck`. **Geen flikkerende lucht in fase 1 (Q20 = A)**: naar ring 5 (89/94).
- Nether-4: pulserende fog (fog-hook in `client/`), droge wind, kristalgezoem.
- Zuigmodder: het slurpgeluid (`block.sucking_mud.slurp`, contract-delta) in 80-0 registreren, audio in 80.
**verdant_falls:** de bestaande look, `waterfall_mist`, loop en additions blijven; de grijsfade alleen in zone 4→5. Vogels in de nieuwe bomen (F11) via main.

### T — 71b (`textures`)
**Af als (Q16 = A):** wat golf 2 nieuw nodig heeft (sporen, de natte glanzende `sucking_mud` als tell, particle-check, backlog r. 273) **plus alle flora-textures van 72b** (§6b, mythisch pastel, H4); contactblad. Afstemmen van bestaande textures in 71 met de feedback van 5b.

## 3. Afhankelijkheden en volgorde
1. Nu: A-0 (80-0, op main), C, W-nether, T (niet-flora), A (na 80-0), B 72 (na 80-0). Flora-sparring met de gebruiker.
2. Na `ring4/vf-perf2` op main: W-terrein (73), W-terrein-b (74), W-biomes.
3. B merget 72 vooruit naar main zodra het af is; daarna 72b. W-terrein 73 levert de tunnel-DF vóór 77; 74 levert eilandparen en meren vóór 75.

**Integratie 2** (snel protocol, `waves.md` §6). Merge-volgorde: A-0 (al op main) → B → T → W-terrein → W-terrein-b → W-biomes → W-nether → C → A. Daarna:
- `runData` + `git diff --exit-code src/generated`; S-smoke (dedicated server + 1 client, verse wereld `ring4-wave2`); W-smoke (`tpring 4`, `tpring 4 nether`, `tpboundary 3|4`, `tpboundary 4|5`, portal ring 4 ↔ Nether-4 en vanuit `verdant_falls`);
- **tools (Q21 = B, INT-taak):** gifscan (gif tegen lucht en water), vaste `MapPalette`-kleuren (backlog r. 267), waterscan-settle (backlog r. 291);
- offline: determinisme omgekeerd, feature-index ring 1–3, waterscan (b34/vf/vg: 0 nieuw; eilandmeren, tunnelvakken), gifscan, Nether-lavawanden r 1.500 en 2.000, Nether-ruimte-check met de fossielen;
- `ModNetwork.PROTOCOL_VERSION` +1 alleen bij een nieuwe payload (niet gepland);
- prestatie (73, 74, 78 raken de noise): meetpunten elke ring 4-biome, tunnelgebied, wereldboom, `sundered_reach`, `verdant_falls` met flora, Nether-4 met fossielen; ≤ 1,75×;
- `/code-review` medium; docs en `teststop-5b.md`.

**Teststop 5b** zoals `waves.md` §2 (dedicated server + 2 clients), plus de flora van `verdant_falls` en het drijfzand.

## 4. Besluiten (sparring golf 2, 2026-10-16)
| Q | Antwoord |
|---|---|
| Q1 | A — W-terrein en W-biomes wachten op de prestatiefix (`ring4/vf-perf2`); B, C, A, T, W-nether starten nu |
| Q2 | A — 73 ∥ 74, 74 in een eigen worktree (poort 8) |
| Q3 | A — voorpakket 80-0 op main; 72 merget vooruit |
| Q4 | **B** — tropische flora van `verdant_falls` in golf 2 (72b, 71b, 75b) |
| Q5 | A — tunnels per band: mythisch overstroomd met koepelluchtkamers, bedorven droog met gaskamers en gifpoelen |
| Q6 | A — ingangen via gatmeren op zeeniveau en zinkgaten; geen tunnels in de zones (≥ 32 ervoor) |
| Q7 | A — wereldboom volledig terrein (surface rule, bladeren persistent) |
| Q8 | A — hemelmeren, rotsbogen en eilandparen door W-terrein in 74 |
| Q9 | A — gasketting gespreid, één ontploffing per ~8 blokken, max. 4 per tick |
| Q10 | A — oppervlaktegas: bellen onder de veenkorst + lagen in zinkgatbodems |
| Q11 | A — opwaartse stroom als gedeelde `entityInside` (bubble column) |
| Q12 | **B** — dodelijk drijfzand: helemaal wegzakken, stikschade na een paar seconden |
| Q13 | **B** — gifkristal ook aan het oppervlak van de bedorven band (gloed onder eilanden) |
| Q14 | A — aquamarijn-geodes half open onder diep water; gifkristal in de droge bedorven tunnels en gaskamers |
| Q15 | **B** — Nether-4-fossielen als terrein-DF tot 80+ (binnen het budget) |
| Q16 | A — 71b alleen nieuwe behoeften + particle-check; nu inclusief de flora-textures |
| Q17 | A — client-hook: geleidelijk naar grijs over zone 4→5 |
| Q18 | A — silhouet als donkere billboard-particle, zeldzaam |
| Q19 | A — nachtkoor en nachtgloed via een client-controller, alleen 's nachts |
| Q20 | A — alleen `glitch_speck`; flikkerende lucht naar ring 5 |
| Q21 | **B** — geen I-stroom; tools in INT2 |

## 4b. Vervolgvragen (beantwoord 2026-10-16)
| G | Antwoord |
|---|---|
| G1 | A — drijfzand alleen in kleine plekken (≤ 4×4, 2–3 diep) op oevers in `peat_moors` en `drowned_forest`, niet midden in het riet; altijd herkenbaar (natte glanzende texture, bel, slurpgeluid) |
| G2 | A — zakken ~1 blok per 2 s; hoofd boven: traag eruit springen; hoofd onder: schade, blijven springen brengt je eruit |
| G3 | A — eigen damage type met doodsbericht, schade zoals gif (`DifficultyFloor`); mobs zakken ook, items niet |
| G4 | **B** — gifkristal aan het oppervlak ook als clusters (licht 5, scherven); materiaal vroeg beschikbaar (fase 2-balans) |
| G5 | A — fossielen alleen in `bone_dunes`, ~1 per 250², half begraven, hol en beloopbaar, niet in de zones of boven lava |

## 5. Contract-delta (main/INT)
- §3: 72b-blokken (§6b); `sucking_mud` dodelijk (G1–G3) met damage type `corruptedadventure:sucking_mud` (§8: `Ring4DamageTypes`).
- §6: sound event `block.sucking_mud.slurp`.
- §3/§4: hemelmeren, rotsbogen en eilandparen bij W-terrein (74), niet bij 75.
- §4: Nether-4-fossielen zijn terrein (W-nether, een veld in de Nether-4-DF) in plaats van een feature.
- §6: geen flikkerende lucht in fase 1; nachtgeluid via een client-controller.

## 6. Flora-sparring `verdant_falls` (2026-10-16)
**Wat al vastlag:** volledig eigen flora, geen vanilla-jungle of -vines; klimmen langs wortels en hangmos (`ledge_strand` onder elke klimknop); look lucht `86D6E0`, mist `D2F0E4`, water `3FD0C0`, gras `6CC24A`, blad `55B03A`; calciet op drempels en vallippen, mos op de treden.

### 6a. Besluiten
| F | Antwoord |
|---|---|
| F1 | **B** — volledige houtset (definitie "houtset", `fase-1-wereld.md` ring 2) |
| F2 | A — twee vormen van één soort: emergente boom 14–22 met plankwortels en schermkroon (meeroevers, kloofranden) en een boomvaren 4–7 |
| F3 | **B** — kleine bomen (boomvarens) ook op de stootborden (40 %); paden, klimroutes en vallen vrij (F4) |
| F4 | A — weelderig maar open; vallen, lippen, paden, klimroutes, brugdekken en de bevaarbare voetrivier vrij |
| F5 | **C** — ~10 soorten: de zes van A + waterplant, hangorchidee, fruit |
| F6 | **C** — eigen klimbare liaan + bloeiende variant; vervangt het hangmos in `ledge_strand` binnen `verdant_falls` |
| F7 | A — geen planten in beken, vallen en de voetrivier; stille meren een paar lotussen; oevers reuzenbladen en riet |
| F8 | A — gloeiende overlay op orchidee en bromelia zonder bloklicht |
| F9 | **B** — mythisch pastel, aansluitend op `moonpetal`; moet onderscheidbaar blijven van `cascade_vale` (H4 = A) |
| F10 | A — papegaaien op de nieuwe bomen |
| F11 | A — de sapling groeit alleen de boomvaren; de emergente boom alleen via worldgen; planten werken met bonemeal |
| F12 | A — voorvoegsel `verdant_` |

### 6b. Scope 72b / 71b / 75b
**Houtset (16 blokken, F1 = B, volgens de vaste definitie "houtset"):** `verdant_log`, `verdant_wood`, `stripped_verdant_log`, `stripped_verdant_wood`, `verdant_planks`, `verdant_stairs`, `verdant_slab`, `verdant_fence`, `verdant_fence_gate`, `verdant_door`, `verdant_trapdoor`, `verdant_pressure_plate`, `verdant_button`, `verdant_leaves`, `verdant_sapling`, `potted_verdant_sapling` (geen item). `BlockSetType`/geluid als eik, bijl strippt, brandbaar als vanilla-hout. Geen borden, hangborden, boot of potten; recipes in de `items`-backlog (**H1 = A**).

**Planten (13 blokken, 9 soorten + de bloeiende liaan, F5 = C):**

| Blok | Soort / gedrag | Waar (75b) |
|---|---|---|
| `verdant_giant_leaf` | reuzenblad, 2 hoog (dubbele plant) | treden, oevers |
| `verdant_fern` | lage varen | grond overal |
| `verdant_tall_fern` | dubbele varen | treden, onder bomen |
| `verdant_bromeliad` | bromelia op de grond, gloeiende overlay | treden, oevers |
| `verdant_wall_bromeliad` | bromelia aan een wand (geen item, zoals wall torch) | kloofwanden, stammen |
| `verdant_orchid` | orchidee aan stammen en wanden (facing), gloeiende overlay | emergente stammen, kloofwanden |
| `verdant_hanging_orchid` | hangorchidee onder richels en takken | onder richels, kronen |
| `verdant_blossom_bush` | bloeiende struik | bassinranden (niet op calcietlippen) |
| `verdant_vine` + `verdant_vine_plant` | liaan (kop + stengel, groeit omlaag, klimbaar); state `flowering` voor de bloeiende variant (implementatiekeuze: één blokpaar met state i.p.v. vier blokken) | `ledge_strand`, kloofwanden, kronen |
| `verdant_water_bloom` | drijvende waterplant (zoals een lelieblad, op een waterbron) | alleen stille meren (bassin- en kloofmeren), naast de lotussen (**H2 = A**) |
| `verdant_fruit_pod` | vruchtpeul aan de emergente stam, groeistadia zoals cacao (geen item) | stammen van de emergente bomen (**H3 = A**) |
| item `verdant_fruit` | placeholder-drop van de peul, geen voedingswaarde; eten en recipes in fase 2 (`items`) | — |

Samen 16 + 13 = **29 blokken**. Gloed alleen als emissive overlay (geen licht, F8). Namen `verdant_water_bloom`, `verdant_fruit_pod`, `verdant_fruit`: voorstel plan-agent, B mag via main bijstellen.

**Palet (F9 = B, H4 = A):** mythisch pastel; kruin pastel mint/zeegroen (vaste kleur, geen biome-tint), bloemen perzik, abrikoos en lila, stam warm zandbeige. Gras, lucht, mist en water blijven zoals goedgekeurd. Zo blijft `verdant_falls` onderscheidbaar van `cascade_vale` (zilverwitte stam, roze-lila kruin).

**71b (flora-deel):** textures voor alle 29 blokken (stam/top, gestript, planken, deur boven/onder/item, valluik, bladeren in pastel, planten, liaan kop/stengel × bloeiend/niet), gloei-overlays orchidee en bromelia, vruchtpeul (stadia) en `verdant_fruit`, contactblad met een vergelijking naast `moonpetal`.

**75b (plaatsing, F2–F4, F7, F10):**
- **Treden:** dichte onderbegroeiing (varens, reuzenbladen, bromelia's) en verspreide boomvarens; emergente bomen aan de meeroevers.
- **Stootborden (F3 = B):** boomvarens en varens, nooit op de looplijnen of binnen 2 van een val.
- **Kloven:** emergente bomen aan de kloofranden (niet boven de vallen); wanden orchideeën, wandbromelia's, lianen (ook bloeiend); hangorchideeën onder het richelpad en de klimrichels.
- **Bassinranden:** bloeiende struiken (niet op de calcietlippen en drempels).
- **Water (F7, H2 = A):** beken, vallen, plonspoelen en de voetrivier vrij; stille meren een paar lotussen (ring 4-blok) en `verdant_water_bloom`.
- **Fruit (H3 = A):** peulen aan een deel van de emergente stammen.
- **Altijd vrij (F4):** valkolommen en lippen, richelpad, klimroutes (knoppen en strengen), brugdekken en -opstappen, nissen en tunnels, de bevaarbare voetrivier en de rotsbogen.
- **`ledge_strand`:** in `verdant_falls` `verdant_vine` (deels bloeiend) i.p.v. `hanging_swamp_moss`; 100 % van de routes blijft beklimbaar (test).
- **Papegaaien** (F10) in de spawns; `verdant_leaves`/`verdant_log` in `parrots_spawnable_on` (B).
- Kruinen binnen het 3×3-chunkvenster; determinisme via `Ring4Features.ORDER`.

### 6c. Vervolgvragen (beantwoord 2026-10-16)
| H | Antwoord |
|---|---|
| H1 | A — geen borden, hangborden, boot of potten (vaste definitie "houtset"); recipes in de `items`-backlog (fase 2) |
| H2 | A — de waterplant (`verdant_water_bloom`) alleen in stille meren, naast de lotussen; beken, vallen en de voetrivier vrij |
| H3 | A — vruchtpeul aan de emergente stam (stadia zoals cacao), drop placeholder-item `verdant_fruit` zonder voedingswaarde; eten en recipes in fase 2 |
| H4 | A — kruin pastel mint/zeegroen, bloemen perzik, abrikoos en lila, stam warm zandbeige; gras, lucht, mist en water ongewijzigd |
| H5 | A — budding-gifkristal alleen ondergronds; aan het oppervlak alleen clusters en knoppen |

## Voortgang
- 2026-10-16: concept opgesteld (plan-agent); antwoorden Q1–Q21, G1–G5, F1–F12 en H1–H5 verwerkt. Plan definitief, geen open punten.
