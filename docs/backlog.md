# Backlog

Zaken die buiten de scope van een change vielen. Format: `- [categorie] omschrijving (bron: fase/change)`.

## Open vragen uit het ontwerp
- [items] Bron van shulker shells: welke ring/Nether-ring en hoe (loot, bewaker-drop, recipe)? (bron: ontwerp)
- [progression] Hoe hard is een gesloten grens? Voorstel: zachte muur (bron: ontwerp)
- [mobs] Post-game boss: wie of wat? (bron: ontwerp)

## Uit sparring fase 1 (2026-09-30)
- [items] Gear-tiers per ring: elke ring één tier hoger; netherite ≈ ring 1–2-niveau, zodat het in ring 3 zwak is vergeleken met ringmateriaal. (bron: sparring fase 1)
- [corruption] Ring 1-horror-events: verdwijnende/verplaatste fakkels, Herobrine-silhouetten in de verte, geluiden achter je. (bron: sparring fase 1)
- [structures] Ring 1: kleine 'Herobrine-schrijnen' (goudblokken, netherrack, redstone-fakkels) als zeldzame vondst. (bron: sparring fase 1)
- [structures] Ancient cities in ring 1 (deep dark): wel of niet? Beslissen in fase 3. (bron: sparring fase 1)
- [progression] Grensmuren bouwen (types staan in `fase-1-wereld.md` §1.5): per speler, client-rendering, gekoppeld aan unlocks. (bron: sparring fase 1)
- [corruption] Fata morgana in ring 2: oase/gebouw in de verte dat verdwijnt bij nadering. Server kiest posities (client kent de seed niet), client rendert nep-silhouet; renderer delen met Herobrine-silhouetten. (bron: sparring fase 1)
- [structures] Ring 2: oasedorpen met villagers, verlaten kampen, halfbegraven ruïnes. (bron: sparring fase 1)
- [items] Ring 2: veldfles/waterzak tegen uitdroging; zonnesteen-gear-tier; turkoois als handelswaar. (bron: sparring fase 1)
- [items] Placeholder-items `raw_sunstone` en `raw_turquoise` (ruwe drops van de ring 2-ertsen) zijn minimale basis-items zonder recipes of rarity; het gesmolten product (naam en vorm nog open), het smeltrecept (ruw materiaal en erts → product, oven/hoogoven), zonnesteen-gear-tier en turkoois als handelswaar in fase 2. (bron: change 24)
- [items] Ruwe opslagblokken `raw_sunstone_block` en `raw_turquoise_block` (9× ruw materiaal, zoals `raw_iron_block`) en hun recipes; het blok zelf is blocks-werk in fase 2. (bron: change 24)
- [mobs] Ring 2: zandwormen in de duinenzee, schorpioenen in canyons, mummies bij begraven skeletten. Nether-2: sporenmonsters, slijmwezens. (bron: sparring fase 1)
- [items] Nether-2: dauwkristal als basis van hittebescherming voor ring 3; druipzwam-hout; zielkristal (Nether-1) gebruik bepalen. (bron: sparring fase 1) Zie ook de regel van change 15 (`soul_crystal`).

## Uit sparring fase 1, sessie 2 (2026-09-30)
- [items] Ring 3: vulkaniet-gear-tier; zwavel → buskruit/vuur/brandstof; vuurrobijn als edelsteen/handelswaar; hittebescherming op basis van dauwkristal. (bron: sparring fase 1 sessie 2)
- [items] Nether-3: oerijs smelten tot zuiver water (basis gifbescherming ring 4); rijpzwam-hout; warmte-item tegen kou (bijv. smeulsteen-lantaarn). (bron: sparring fase 1 sessie 2)
- [structures] Ring 3: gestrande scheepswrakken en verlaten havens in de lege zeekom (`dried_seabed`), verdroogde putten, verkoolde dorpen. (bron: sparring fase 1 sessie 2)
- [mobs] Ring 3: aswandelaars (uit de as-afgietsels), magmawezens in lavabuizen. Nether-3: bevroren ghasts, ijs-strays. (bron: sparring fase 1 sessie 2)
- [items] Ring 4: veenijzer-gear-tier; gifkristal voor gifwapens/-pijlen; jade als edelsteen; gifbescherming uit oerijs-water; veen als brandstof. (bron: sparring fase 1 sessie 2)
- [items] Nether-4: hemelglas (blok, smeltrecipe uit bottenzand + zuiverkristal, lens die glitches zichtbaar maakt); fossielen als bron van shulker shells? (open vraag). (bron: sparring fase 1 sessie 2)
- [structures] Ring 4: paalwoningen, verzonken kapel in `drowned_forest`, heksenhutten. Nether-4: fossiele tempels in ribbenkasten. (bron: sparring fase 1 sessie 2)
- [mobs] Ring 4: iets groots onder water (bij de bellen), rietsluipers, moerasheksen, gifkikkers. Nether-4: kristalwachters, bottenvreters. (bron: sparring fase 1 sessie 2)
- [items] Ring 5: nulliet-gear-tier; glitchscherven als craftmateriaal; void-bescherming uit hemelglas. Nether-5: witte kern → zuiveringsgear tegen corruptie. (bron: sparring fase 1 sessie 2)
- [structures] Ring 5: ruïnes van 'normale' gebouwen die half in de void hangen; valse dorpen in `false_meadow` (leeg, deuren leiden nergens heen). Nether-5: witte tempel/altaar rond de witte kern. (bron: sparring fase 1 sessie 2)
- [mobs] Ring 5: glitchwezens, schaduwen die alleen bewegen als je niet kijkt. Nether-5: vrijwel geen mobs (stilte), misschien één stille wachter. (bron: sparring fase 1 sessie 2)
- [corruption] Ring 5-events: gefluister bij hoge stilte/void-meter, Herobrine achter je in `false_meadow`, blokken die veranderen als je wegkijkt. (bron: sparring fase 1 sessie 2)
- [worldgen] Onmogelijke objecten in ring 5 (gras op plafonds, stilstaand water in de lucht) viel af voor fase 1; eventueel later als extra horror-feature. (bron: sparring fase 1 sessie 2)
- [items] Placeholder-items uit fase 1-blockchanges: glitchscherf (drop van glitchblok, change 78) en zuiverkristal-scherf (change 71) worden als minimaal basis-item geregistreerd; nut, recipes en balans in fase 2. (bron: sparring fase 1 sessie 2)

## Uit bouw fase 1
- [infra] Access transformer `META-INF/accesstransformer.cfg` is als minimale placeholder in een worldgen-change gekomen (twee regels: `NoiseBasedChunkGenerator.globalFluidPicker`, `NoiseChunk.aquifer`); bij een volgende infra-change nagaan of dit de gewenste plek/vorm is en het in `01-techniek.md` onder de stack vastleggen. (bron: change 6)
- [progression] *Adventuring Time* (en later *Hot Tourist Destinations*) is onhaalbaar met kopie-biomes; vervangen of aanpassen in fase 5. (bron: change 8)
- [items] Visloot: de bamboe-junk in jungles (`gameplay/fishing/junk`, `location_check` op vanilla-keys) valt weg in de jungle-kopieën. (bron: change 8)
- [worldgen] NeoForge `c:`-biome-tags (`c:is_hot/overworld`, …) voor de ring 1-kopieën, voor mod-compatibiliteit. (bron: change 8)
- [items] Recipes dood-hout-set (log → planks, wood, trap, plaat, hek, poort, deur, valluik, knop, drukplaat); generieke vanilla-recipes (stokken, werkbank, kist, houten tools, houtskool) werken al via de tags. (bron: change 10)
- [blocks] Borden/hangborden en [items] boten voor houtsets: vallen buiten de houtset-definitie (vragen een geregistreerde `WoodType`, block entities en client-rendering); eventueel later. (bron: change 10)
- [textures] Ring 1-textures zijn sinds change 17 definitief uit het script, maar nog niet in-game beoordeeld: na runClient 1–2 iteratierondes op palet/patroon met de gebruiker; met de hand bijgewerkte bestanden in `HAND_EDITED` van `tools/textures/Ring1Textures.java` zetten. Gloed van de erts-overlays vervalt mogelijk met Sodium/Embeddium of shaderpacks (dan een gewone overlay). (bron: change 17)
- [infra] Texturescripts draaien handmatig (`java tools/textures/Ring1Textures.java`, `Ring2Textures.java`), niet via Gradle. Sinds change 21 is er een tweede script (`Ring2Textures.java`) met **gekopieerde** helpers (`valueNoise`, `shade`, `mix`, `rim`, `set`/`setWrapped`, preview); ontdubbelen (gedeeld bestand via een Gradle-taak of een klein JDK-classpath) kan nu. Ring 2 heeft daarbij al de generieke opzet (`Animation`-record, gloeikleur per materiaal, ingebouwde controles); ring 1 zou die bij het ontdubbelen overnemen. (bron: change 10/17/21)
- [corruption] Idee: dood gras dat naar vanilla-gras spreidt ("groeiende dode plekken"). (bron: change 10)
- [blocks] Pad en farmland van verdorde aarde/dood gras worden bij bedekken/uitdrogen vanilla-aarde (vanilla hardcodet `Blocks.DIRT`); een speler kan een dode plek zo "genezen". Eigen pad/farmland als dat ongewenst blijkt. (bron: change 10)
- [mobs] Dood gras en verdorde aarde staan bewust niet in `#minecraft:animals_spawnable_on` (geen dieren op dode grond); spawnregels voor `withered_expanse` en kale plekken bij de mobs-fase nagaan. (bron: change 10)
- [mobs] `dead_grass_block` (en evt. `withered_dirt`) aan `#rabbits_spawnable_on` toevoegen, anders spawnen konijnen in de withered expanse alleen op zand (bron: change 11)
- [worldgen] Kale plekken lopen door in buurchunks; buren die later decoreren zetten er vanilla-bomen/planten op (chunkgrens-snede). Opties: (a) plek binnen de eigen chunk houden (rechte randen op chunkgrenzen = vormwijziging), (b) [blocks] dood gras/verdorde aarde uit `#minecraft:dirt` (raakt change 10: planten, `withered_expanse`-begroeiing), (c) uitgestelde nabewerking zodra alle chunks binnen straal 2 gedecoreerd zijn (infra, complex), (d) zo laten. Eerst in test W beoordelen hoe erg het is. (bron: change 12, review)
- [infra] Optioneel `/ca`-debughulp om tunnels en kale plekken te vinden (`/locate` werkt niet voor features); alleen bouwen als test W te veel zoekwerk blijkt. (bron: change 12)
- [atmosphere] Optioneel client-side verloop (kleur en grotgeluid) over de buitenste ~300 blokken van ring 1 op basis van gesyncte ringdata; kan nu (ringdata staat sinds change 18 in `ClientRingLayout`). Nu is "valer/vreemder richting de rand" alleen binnen/buiten `withered_expanse`. (bron: change 13)
- [structures] Nether-ring 1: per seed gegarandeerd minstens één fortress, en liefst ook één bastion (bijv. eigen structure set of `FixedRingPlacement` voor ring 1). Fase 3. Aanleiding: bij vanilla-spacing ~40 % kans op geen fortress in de ring. (bron: change 14)
- [items] Placeholder-item `soul_crystal` (drop van `soul_crystal_ore` en `crimson_soul_crystal_ore`) is een minimaal basis-item zonder recipes of rarity; nut, smelten van het Silk Touch-erts en balans in fase 2. (bron: change 15)
- [items] Creative tab: sinds change 15 eerst alle blokken (per ring), dan alle losse items. Bij fase 2 (veel items) herzien: eventueel per ring blokken + items samen, of aparte tabs. (bron: change 15)
- [items] Balans zielkristal (fase 2): twee tarieven. Aders `soul_crystal_ore` (2 per chunk, grootte 4; gemeten 2,2–2,4 erts per chunk in Nether-ring 1) en de crimson-vloerfeature (rarity 2 × 20 pogingen, grootte 3; gemeten 0,67 per crimson-chunk). Bijstellen in `NetherRing1Features` zodra het verbruik per item bekend is. (bron: change 16)
- [infra] Debugcommando `/ca count <blok> <straal>` als herbruikbare teltool voor de ertsplaatsing van ring 2–5 (changes 25, 41, 61, 81), ook per Y-band en per gastgesteente; change 25 gebruikte een tijdelijk dumpcommando (alle blockstates van 5 × 5 chunks naar een bestand) + Python, werkwijze in plan 25 en `CODEBASE.md`. (bron: change 16, aangevuld change 25)
- [textures] Definitieve HUD-balk en icoon per conditie; nu een placeholder van vlakken (`ConditionHudLayer`, `fill`, kleur uit `Condition.hudColor`). (bron: change 19)
- [conditions] Eigen damage types met doodsberichten per conditie (uitdroging: klaar in change 27; hitte, …) in de change van die conditie; de `debug`-conditie gebruikt `minecraft:generic`. (bron: change 19)
- [items] Recipes kloofsteen-set (keien → trap/plaat/muur, tinten → bakstenen, steenzaag), scorched sandstone-set (incl. smelten → smooth), woestijnbloem → kleurstof. Stenen tools en oven van kloofkeien werken al via `#stone_tool_materials`/`#stone_crafting_materials`. (bron: change 21)
- [blocks] Idee (afgewezen in change 21, vraag 6 C): tonnetjescactus die langzaam groeit (random tick: kleine cactusjes ernaast of een groter stadium). (bron: change 21)
- [infra] Optioneel `/ca`-debughulp om hangend `loose_sand` te plaatsen (zonder blok-updates), voor tests; nu via een testdatapack (barrier in `#minecraft:fire`, werkwijze in `CODEBASE.md`, valkuilen). Alleen bouwen als change 26 of test W het nodig heeft. (bron: change 21)
- [items] Recipes palm-set (log → planks, wood, trap, plaat, hek, poort, deur, valluik, knop, drukplaat). Generieke vanilla-recipes (stokken, werkbank, kist, houten tools, houtskool) werken al via `#planks` / `#logs_that_burn`. (bron: change 22)
- [items] Idee: palmvrucht (dadels/kokosnoot) als extra bladdrop of voedsel in de droge ring (uitdroging/voedsel in de oase). (bron: change 22)
- [textures] Placeholder `palm_leaves.png` is voor ~50 % doorzichtig; bij de definitieve texture (change 37) dichter maken en in-game controleren bij Graphics: Fast én Fancy (render type `cutout_mipped` uit de model-JSON). (bron: change 22, review)
- [structures] Ring 2-biomes (`canyonlands`, `oasis_flats`) hebben geen `minecraft:has_structure/*`-tags: geen woestijnpiramides, woestijndorpen, mineshafts of ruined portals in ring 2. Keuze per structure in fase 3. (bron: change 23)
- [mobs] Kamelen in `oasis_flats` (change 23, vraag 11 A) hebben een spawn placement nodig die vanilla niet heeft (vanilla spawnt kamelen nooit natuurlijk): minimale registratie in `worldgen/biome/BiomeSpawnPlacements` (op de grond, `Animal::checkAnimalSpawnRules`: alleen op `#animals_spawnable_on`, dus de grasoevers). In fase 4 herzien of naar de mobs-code verhuizen. (bron: change 23)
- [worldgen] Grotingangen in kloofbodems die onder Y 63 komen lopen vol (aquifer): kleine waterplekken aan het oppervlak van canyonlands (gemeten 0,28 % van de bovenste blokken op 384 × 384, 0 van 100 willekeurige hoogtepunten). Change 26 maakt het niet erger (de floodedness-band is 0 boven Y 60; 7–24 kolommen per 25 kloofchunks, vóór 14–24); in teststop 2b beoordelen of het zo mag. (bron: change 23, bijgewerkt change 26)
- [infra] Debugcommando voor terreinkaarten (PNG: hoogte/biome uit de density functions, bovenaanzicht met kaartkleuren, verticale doorsnede); in change 23 tijdelijk gebruikt en niet gecommit, werkwijze in `CODEBASE.md` (valkuilen). Nuttig voor de terreinchanges van ring 3–5 en de Nether. (bron: change 23)
- [worldgen] Noise-stap ring 2 is 1,36× vanilla (budget 1,3×; met ring gate, change 23). Waarschijnlijk het hoge terrein (veel ondergrond met grotten en aquifer per open blok). Opties als het speelbaar merkbaar wordt: minder grotvolume boven ~Y 100 in het plateau, of de per-blok-overhead van `RingBlend`/`RingFluidPicker`/`RingAquifer` meten en verlagen. Opnieuw meten bij ring 3 (change 39). (bron: change 23) — **Bijgewerkt 2026-10-01:** het budget voor de ring 2-noise-stap is verhoogd naar ≤ 1,75× vanilla (vraag 23b-9 C, keuze gebruiker; gedeeld met 23b/26/30/31). De opties blijven relevant als het laden in multiplayer merkbaar traag wordt. **Bijgewerkt 2026-10-02 (change 23b):** oase-meetpunt x 2.500 nu 1,72× vanilla (vóór 23b 1,45× in dezelfde sessie), kloofgebied 0,91×; de fijne vormen kosten bijna niets, de stijging komt van het terrein (tafelbergen, poelen/oever). Weinig ruimte meer voor 26/30/31. **Bijgewerkt 2026-10-02 (change 26):** change 26 kost niets meetbaars in de noise-stap (oase 1,74× / kloof 0,91× vanilla; vóór in dezelfde sessie 1,81× / 1,00×); de spreiding tussen sessies (±20 % absoluut) is groter dan de marge tot 1,75×.

## Uit teststop 2 en plannen 23b/23c (2026-10-01)
- [structures] Klifwoningen in de wandnissen van ring 2 (vlakke vloer, 6–16 breed, onder een richel op een crème band; ligging deterministisch per seed). Ook uitkijkposten op tafelbergen en touwbruggen over spleetkloven. Een structure moet de nissen zelf vinden (via het terrein of door dezelfde `canyon_details`-hash te lezen). Fase 3. (bron: change 23b, teststop 2)
- [mobs] Ring 2-kloofmobs die de nieuwe vormen gebruiken (klimmende schorpioenen op richels, nesten in nissen, iets in de spleten). Zie de ring 2-mobsregel hierboven. Fase 4. (bron: change 23b, teststop 2)
- [atmosphere] Windgeluid/fluiten in spleetkloven: ambient per positie (niet per biome), client-side; kan de synthese-pijplijn van change 32 hergebruiken. (bron: change 23b)
- [atmosphere] Zandstorm-duw (change 28) bij spleetkloven, richels en tafelbergranden: valgevaar. Nagaan of de duw daar beperkt moet worden (bijv. geen duw richting een afgrond, of zwakker op smalle richels); nu duwt hij overal waar de speler de hemel ziet. (bron: change 23b)
- [conditions] Wandbron in de kloof (change 23c, vraag 23c-4 C: bron in de wand, val, stroompje, kolk; ~1 per 8–15 kloofchunks) is echt water: een tweede, zeldzame koelplek naast de oase. Met change 27 vraag 7 A telt het stroompje (1 diep) niet, de kolk alleen waar hij 2 diep is, de val wel (gemeten in change 27 bij 2427 66 530: 1 diepe kolk en stroompje `sun`, val `water`). Beslissen of dat zo mag, of dat kloofwater anders telt dan oasewater. (bron: change 23c)
- [structures] Wandbronnen en oases als ankerpunt voor kampen, putten of trappen naar de grondwaterspiegel (zie de reservoirs van change 26). Fase 3. (bron: change 23c)

## Uit change 25
- [worldgen] Change 30: `hoodoo_fields`, `salt_flats` en `dune_sea` in `#is_ring_2` (dan krijgen ze `add_ring_2_ores`); zonnesteen komt alleen in hoodoos van kloofsteen-tinten of `scorched_sandstone` (de `scorched_sandstone_sunstone_ore`-variant bestaat al). (bron: change 25)
- [worldgen] Change 31: `dry_riverbeds` ligt deels voorbij grens 2→3, waar `ring_chance` (`progress`, ring 2) kans 0 geeft: geschaalde vanilla-ores vallen daar weg; eigen ertsset kiezen (bijv. signaal `transition`). (bron: change 25)

## Uit change 26
- [atmosphere] Zandgrotten: kraak- of ruisgeluid / extra stof als waarschuwing (biome-ambient of client-particles); mogelijk in change 32. (bron: change 26)
- [structures] Reservoirs en lore: oude putten, pompstations of trappen naar de grondwaterspiegel onder oases (fase 3). (bron: change 26)
- [progression] Lore "het water van ring 2 zit onder de grond / is gestolen door ring 3" in quests of teksten (fase 5). (bron: change 26)
- [worldgen] Ring 3–5 houden in `ring_blend/overworld_floodedness` en `ring_blend/overworld_erosion` de vanilla-invoer; hun grotten-changes (42 lavabuizen, 62 overstroomde grotten, 83) kiezen hun eigen floodedness en of de deep-dark-check (router-erosion) daar weg moet. (bron: change 26)

## Uit change 27
- [worldgen] `/place feature corruptedadventure:canyon_spring` op een al geladen chunk faalt ("Failed to place feature" + `Unprimed heightmap: WORLD_SURFACE_WG` in de log): de feature leest de WG-heightmap. Voor tests eventueel een fallback op `WORLD_SURFACE` als de WG-heightmap ontbreekt, of het als bekende beperking laten. (bron: change 27)

## Uit change 28
- [conditions] Ringweer-modifiers (28 hittegolf; later 45 asregen/uitbarsting, 66 gifregen, 85) hangen zich elk via `ConditionDefinitions.addModifier` in de condities-pijplijn, in registratievolgorde (nu vanuit `RingWeather.register`). Bij change 43 (hitte) nagaan of een vaste volgorde nodig is (weer vóór gear, zuiverend als laatste). (bron: change 28)
- [textures] Eigen sprite voor de `sand_gust`-particle (nu vanilla `generic_0..7`, zandkleurig getint), bij de ring 2-textures (change 37). (bron: change 28)
- [atmosphere] Windgeluid van de zandstorm is een verwijzing naar vanilla `item.elytra.flying` (pitch 0,7); de synthese-pijplijn van change 32 (`Ring2Sounds.java`) kan het vervangen door een eigen wind. (bron: change 28)
- [atmosphere] Wind-duw ook voor items, mobs en spelers op een rijdier/in een boot, als dat later gewenst is (nu alleen spelers te voet). (bron: change 28)
- [atmosphere] Post-shader `heat_haze` met shaderpacks (Iris) of performance-mods: het effect valt daar mogelijk weg (geaccepteerd in het plan); bij klachten een particle-alternatief (plan 28 vraag 5 B). (bron: change 28)

## Uit change 29
- [items] Zout (`salt`): doel en recipes in fase 2 — 4 zout ↔ zoutblok (`c:storage_blocks/salt`), zoutblok → gepolijst zout (+ steenzaag: trap/plaat/muur); eventueel voedsel/conservering of een rol bij uitdroging. Gepolijst zout en zijn vormen hebben tot dan geen survival-bron. (bron: change 29)
- [blocks] Later-ideeën uit de zout-sparring (afgewezen voor nu): vallende/brosse zoutkorst, een zoutbakstenen-familie (`salt_bricks` + trap/plaat/muur, bijv. voor zoutstructures in fase 3), trap/plaat/muur van het ruwe zoutblok, zoutkristallen die groeien (zoals amethist). (bron: change 29)
- [blocks] Zoutkristal hergebruikt vanilla `AmethystClusterBlock`: een projectiel-treffer speelt het amethist-belgeluid. Bij change 37 (geluid/uiterlijk) beoordelen of een eigen class met eigen geluid nodig is. (bron: change 29)
