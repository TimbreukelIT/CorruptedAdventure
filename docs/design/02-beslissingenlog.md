# The Corrupted Adventure — Beslissingenlog

Eén regel per beslissing: datum · categorie · beslissing · reden. Nieuwste onderaan.

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-09-30 | infra | Minecraft 1.21.1, NeoForge 21.1.x, ModDevGradle | Stabiele versie |
| 2026-09-30 | infra | Mod-ID `corruptedadventure`, package `nl.corruptedadventure`, naam The Corrupted Adventure | — |
| 2026-09-30 | worldgen | Eigen world preset `corruptedadventure:corrupted_world` | Vanilla blijft ongemoeid; één regel in `server.properties` |
| 2026-09-30 | infra | Multiplayer en dedicated servers zijn een harde eis | Vanaf dag 1, achteraf is te duur |
| 2026-09-30 | worldgen | 5 ringen: 0–1.500 / 1.500–3.500 / 3.500–5.500 / 5.500–7.500 / 7.500–9.500, daarbuiten void + world border | Startwaarden, in config te balanceren |
| 2026-09-30 | worldgen | Nether als tussenwereld met tegenpool-ringen, `coordinate_scale` 4 | Ruimte voor content per Nether-ring, portals landen in dezelfde ring |
| 2026-09-30 | worldgen | The End uit (geen strongholds); shulker boxes blijven via alternatieve shell-bron | End-items niet nodig, shulkers wel handig |
| 2026-09-30 | structures | 4 bewakersstructures per grens, 4 kastelen in ring 5, op vaste berekende posities | Nooit ver teruglopen |
| 2026-09-30 | progression | Progressie deels per speler, deels per server | Samenspelen zonder dat iedereen alles apart moet grinden |
| 2026-09-30 | corruption | Final battle één keer per server, één groep; daarna corruptie weg en kastelen ruïnes | Post-game als duidelijk eindpunt |
| 2026-09-30 | infra | Vijf fases: wereld → items/tools → structures → mobs/bosses → progressie/corruptie | Gecontroleerd bouwen |
| 2026-09-30 | infra | Eén categorie per change; sparren bij elke fase en onduidelijke keuze; `/code-review` na elke change; `CODEBASE.md` altijd eerst lezen | Kwaliteit en token-zuinig werken |
| 2026-09-30 | worldgen | Terrein per ring via één eigen density function `ring_blend` die per ring een JSON-terrein kiest en in de overgangszone mengt | Echte andere landvormen per ring; één noise settings per dimensie blijft |
| 2026-09-30 | worldgen | Overgangszone ~128 blokken, grensruis ±96 (Nether: /4) | Merkbare overgang zonder de ringen te verwateren |
| 2026-09-30 | textures | Placeholders (getinte vanilla) eerst, per ring een aparte change die ze definitief maakt | Bouwtempo hoog, sfeer per ring toch vroeg zichtbaar |
| 2026-09-30 | infra | Fase 1-volgorde: skelet (preset, Nether, `ringAt`, BiomeSource, placeholder-biome per ring, `ring_blend`), daarna ring voor ring | Na elke ring speelbaar en bij te sturen |
| 2026-09-30 | progression | Elke grens krijgt een eigen soort muur (mist, as, …) die de ring erachter vaag houdt tot de bewaker verslagen is; ontwerp in fase 1, bouwen in fase 5 | Muur hangt af van per-speler unlocks |
| 2026-09-30 | atmosphere | Weer: vanilla regen/onweer blijft (alleen zichtbaar in biomes met neerslag) + eigen weercyclus per ring in SavedData, gesynct; client rendert alleen | Afwisseling per ring zonder regio-complexiteit |
| 2026-09-30 | worldgen | Ring 1 = eigen kopieën van vanilla biomes via datagen (zonder stronghold-tag) | Vanilla-gevoel zonder vanilla-werelden te raken; later per biome bij te sturen |
| 2026-09-30 | worldgen | Voorbij ring 5: terrein brokkelt af tot zwevende eilanden en void (9.500–10.000); world border op 10.000 (Nether ÷4), gezet bij het aanmaken van de wereld | Sfeervol 'einde van de wereld' |
| 2026-09-30 | conditions | Condities als meter 0–100 per speler (attachment), server-side opbouw/afbouw, drempels geven effecten; HUD-balk op de client (gesynct); gear uit fase 2 remt opbouw | Spanning en reactietijd; duidelijk voor spelers |
| 2026-09-30 | worldgen | 4–5 biome-varianten per ring (ring 2–5) + 1 overgangsbiome per grens | Rijke ringen die als eigen wereld voelen |
| 2026-09-30 | worldgen | Ring 1 bevat oceanen en rivieren, hoge bergen en deep dark (zonder ancient cities); geen mushroom fields | Vertrouwd vanilla-gevoel |
| 2026-09-30 | worldgen | Ring 1 krijgt al vroege horror-hints; in fase 1 alleen wat worldgen/blocks/atmosphere kan, events in fase 5 | Onrust vanaf het begin |
| 2026-09-30 | worldgen | Horror-hints ring 1 in fase 1: onnatuurlijke 2×1-tunnels, kale plekken met dode bomen (vaker richting de rand), stille zones in grotten, overgangsbiome 1→2 als verdorde vlakte | Onrust via wereld zelf; events pas in fase 5 |
| 2026-09-30 | worldgen | Vanilla-ores: alles in ring 1, daarna naar buiten afbouwen (ijzer/kolen schaarser, diamant bijna weg); ringores nemen over | Basisspullen blijven vindbaar, progressie via ringmaterialen |
| 2026-09-30 | worldgen | Nether-tegenpolen thematisch: 1 vanilla-Nether · 2 doorweekte schimmel-Nether · 3 bevroren Nether · 4 droge kristal-/bottenwoestijn · 5 'zuivere' witte, stille Nether | Sterk contrast; sleutelmateriaal is tegengif voor de volgende ring |
| 2026-09-30 | conditions | Ring 1 heeft geen omgevingsconditie; het meter-systeem verschijnt in ring 2 | Ring 1 is de veilige leerzone |
| 2026-09-30 | progression | Grensmuren: 1→2 dichte witte mist · 2→3 zandstorm · 3→4 zwarte aswolk met vonken · 4→5 groene gifdamp die overgaat in glitchruis | Elke muur is een voorproefje van de ring erachter |
| 2026-09-30 | worldgen | Nether-ring 1: kopieën van nether wastes, crimson forest en warped forest | Minder versnipperd in de kleine ring (straal ~375) |
| 2026-09-30 | worldgen | Ancient debris/netherite blijft zoals vanilla; ring-gear is per ring een tier hoger, zodat netherite rond ring 3 al achterloopt (balans in fase 2) | Vanilla-doel blijft, ringores worden belangrijker |
| 2026-09-30 | worldgen | Gegarandeerd land rond spawn (continentalness omhoog binnen ~300 blokken van 0,0) | Spawn in het midden van de ringen, nooit in oceaan |
| 2026-09-30 | worldgen | Ring 2-biomes: canyonlanden, oase-vlakte, zoutvlakte, hoodoo-rotsvelden, duinenzee | Vijf duidelijk verschillende droge landschappen |
| 2026-09-30 | conditions | Ring 2-conditie: uitdroging (zon overdag ↑; schaduw, water, oase, nacht ↓) | Makkelijk te begrijpen; opmaat naar hitte in ring 3 |
| 2026-09-30 | atmosphere | Ring 2-weer: zandstorm (zicht, lichte wind-duw) en hittegolf (uitdroging 2× sneller) | Iconisch en voelbaar |
| 2026-09-30 | worldgen | Nether-ring 2: druipende reuzenzwamwouden, lichtgevende sporengrotten, dauwbronnen (water dat hier niet verdampt) | Natte tegenpool van de droge ring; dauw als tegengif voor ring 3 |
| 2026-09-30 | blocks | Ring 2-bloksets: kloofsteen-set, zout-set, palm-set, woestijnflora + scorched sandstone-set | Bouwmateriaal per biome |
| 2026-09-30 | worldgen | Ring 2-ores: zonnesteen (hoofdmateriaal, hoog in canyonwanden/hoodoos) en turkoois (zeldzaam, diep) | Zichtbaar klimdoel + zeldzame edelsteen |
| 2026-09-30 | worldgen | Ring 2-grotten: ondergrondse waterreservoirs (voeden de oases) en instortende zandgrotten | Oriëntatie/lore en echt gevaar |
| 2026-09-30 | atmosphere | Ring 2-horror in fase 1: zingend zand (biome-ambient); fata morgana verhuist naar fase 5 | Renderer delen met andere illusies |
| 2026-09-30 | worldgen | Overgang 2→3: drooggevallen rivierbeddingen, richting ring 3 zwartgeblakerd | Vertelt het verhaal van het gestolen water |
| 2026-09-30 | blocks | Dauwbronnen in Nether-ring 2 als eigen blok (geen echte waterfluid; Nether blijft ultrawarm) | Laag risico, geen aanpassing van verdamp-regel |
| 2026-09-30 | conditions | Uitdroging daalt door schaduw, water, oase-biome, nacht en een vanilla-waterfles (~30 punten) | Directe tegenmaatregel vóór fase 2-gear |
| 2026-09-30 | conditions | Nether-condities alleen in Nether-3 (kou/bevriezing) en Nether-5 (stilte) | Alleen waar het thema het sterkst vraagt |
| 2026-09-30 | blocks | Nether-ring 2-blokken: reuzenzwam-set (druipzwam), doorweekt netherrack + sporenmos, slijmstalactieten, lichtgevende schimmel + zwamlamp, dauwbron | Eigen Nether-bouwmateriaal en licht voor sporengrotten |
| 2026-09-30 | worldgen | Sleutelmateriaal Nether-2: dauwkristal (groeit aan dauwbronnen, zoals amethist); Nether-1: nieuw erts zielkristal (crimson-bodem/netherrack) | De Nether-lus direct herkenbaar als ringmechaniek |
| 2026-09-30 | worldgen | Terrein Nether-2: zwamkathedralen (hoge holtes, zwammen als zuilen, lagere lavazee met sporenmos-eilanden) | Duidelijk anders dan de vanilla-Nether |
| 2026-09-30 | atmosphere | Sfeer Nether-2: grijsgroene fog, druppelgeluiden, zwevende sporen (alles biome-data) | Nat contrast, geen code nodig |
| 2026-09-30 | infra | Ringgrenzen: config geeft standaardwaarden; bij wereldcreatie vastgelegd in de wereld en daarna alleen die snapshot gebruikt; server synct naar client | Geen breuklijnen bij latere config-wijziging |
| 2026-09-30 | infra | Package wordt `nl.timtoys.corruptedadventure` (vervangt `nl.corruptedadventure`); hernoemen als change 0 vóór het skelet | Eén naam, gelijk aan `CLAUDE.md` |
