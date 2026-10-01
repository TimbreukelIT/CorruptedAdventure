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
| 2026-09-30 | textures | Placeholders (getinte vanilla) eerst, per ring een aparte change die ze definitief maakt (methode "getinte vanilla" vervangen op 2026-10-01: eigen pixels via script) | Bouwtempo hoog, sfeer per ring toch vroeg zichtbaar |
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
| 2026-09-30 | worldgen | Ring 3-biomes: `ashen_wastes`, `volcanic_peaks` (kern), `dried_seabed`, `basalt_flows`, `sulfur_fields` | Afwisseling + het gestolen water zichtbaar in de lege zeekom |
| 2026-09-30 | worldgen | Overgang 3→4: stoomvenen (`steam_fens`: hete modder, geisers, stoom) | Het gestolen water komt terug en kookt; opmaat naar moeras |
| 2026-09-30 | conditions | Geplaatst water verdampt in ring 3 alleen bovengronds (lucht zichtbaar); ondergronds werkt water | Gestolen water voelbaar, bouwen onder dak blijft mogelijk |
| 2026-09-30 | worldgen | Ring 3-ores: vulkaniet (hoofdmateriaal, diep bij lava), zwavel (ondiep, fumarolen), vuurrobijn (zeldzaam, magmakamers) | Risico = beloning, utility en schat |
| 2026-09-30 | conditions | Hitte ring 3: ↑ bovengronds overdag en bij hittebronnen/weer; ↓ 's nachts, ondergronds, in water; fire resistance halveert | Grotten als schuilplaats, sluit aan op de water-regel |
| 2026-09-30 | atmosphere | Ring 3-weer: asregen (hitte ×1,5) en uitbarsting met lavabommen (vanilla small fireballs, server-side; hitte ×2) | Actiemoment zonder nieuwe entity |
| 2026-09-30 | worldgen | Ring 3-grotten: lavabuizen en magmakamers | Reisroutes + risicovolle ertsplekken |
| 2026-09-30 | worldgen | Ring 3-horror in fase 1: as-afgietsels, voetsporen in de as, gerommel/trillingen (client-only) | Onrust via wereld zelf |
| 2026-09-30 | blocks | Ring 3-blokken: vanilla basalt/blackstone als basis + as, puimsteen-set, smeulsteen, verkoold hout-set, zwavelkorst, fumarole, kokende modder, geiser | Eigen karakter zonder vanilla te dupliceren |
| 2026-09-30 | worldgen | Nether-3: bevroren lavazee + rijpwoud + gletsjerkloven; ruimte beoordelen in de W-test, zo nodig ring 3 verbreden via config | Maximale variatie; koppeling ÷4 blijft intact |
| 2026-09-30 | blocks | Nether-3-blokken: koudlava-set, Nether-ijs, rijp-netherrack, rijpzwam-set, ijsstalactieten, gloeiende scheur, oerijs | Eigen bouwmateriaal + warmtebronnen voor de kou-conditie |
| 2026-09-30 | worldgen | Sleutelmateriaal Nether-3: oerijs (kern van ijspilaren, silk touch), zuiver water als basis van gifbescherming | Schoon water als tegengif voor het gifmoeras |
| 2026-09-30 | conditions | Kou Nether-3: eigen meter; ↓ bij warmtebronnen; leren armor remt; hoogste drempel = vanilla-bevriezing (`ticksFrozen`) | Consistent met andere condities, vanilla-visuals gratis gesynct |
| 2026-09-30 | atmosphere | Sfeer Nether-3: blauwwitte fog, ijskristal-particles, wind en krakend ijs (biome-data); geen Nether-ringweer | Geen code nodig |
| 2026-09-30 | worldgen | Ring 4-biomes: `toxic_bog`, `root_forest` (kern), `drowned_forest`, `peat_moors`, `reed_marsh` | Sterk voor horror: zicht boven, diepte onder |
| 2026-09-30 | worldgen | Overgang 4→5: rottend moeras (`blighted_mire`) met eerste glitchplekken | Sluit aan op de muur gifdamp → glitchruis |
| 2026-09-30 | conditions | Gifwater = vanilla water met groene kleur + gif-regel in de condities-tick; geen eigen fluid | Laag risico; emmers en farms blijven werken |
| 2026-09-30 | worldgen | Ring 4-ores: veenijzer (hoofdmateriaal, veen/poelbodem), gifkristal (grotholtes), jade (zeldzaam, diep) | Duiken als risico; utility + schat |
| 2026-09-30 | conditions | Gif ring 4: ↑ in water, `toxic_bog`-lucht, bronnen, gas, gifregen; ↓ op droog land; melk −40, honing −20; drempels misselijkheid → gif → wither | Constante druk met vanilla-noodmiddelen |
| 2026-09-30 | atmosphere | Ring 4-weer: dikke mist (zicht) en gifregen (gif ↑ onder open lucht) | Duidelijk verschil: angst vs. gevaar |
| 2026-09-30 | worldgen | Ring 4-grotten: overstroomde grotten, wortelgrotten en moerasgaskamers | Duiken, verkennen en echt gevaar |
| 2026-09-30 | blocks | Moerasgas als onzichtbaar gasblok; vuur/fakkel/lava → ketting-ontploffing (cap per tick, `mobGriefing`), hoofd in gas = gif ↑ | Block-updates i.p.v. zone-check: eenvoudiger en server-veilig |
| 2026-09-30 | worldgen | Ring 4-horror in fase 1: dwaallichten, geritsel in het riet (client-only), bellen uit de diepte | Wat je niet ziet is het engst |
| 2026-09-30 | blocks | Ring 4-blokken: veen, zuigmodder, riet, moerascipres-set, hangend moerasmos, gifzwam, moerasgas, bellenbron | Moerasflora + eigen hout |
| 2026-09-30 | worldgen | Nether-4: `bone_dunes`, `crystal_spires`, `geode_hollows`; ruimte beoordelen in de W-test | Maximale variatie in de droge tegenpool |
| 2026-09-30 | blocks | Nether-4-blokken: bottenzand, gebleekt netherrack, fossielsteen-set, zuiverkristal (blok, scherf, lamp); ribbenkasten van vanilla bone block | Bouwmateriaal + grondstoffen van hemelglas |
| 2026-09-30 | worldgen | Sleutelmateriaal Nether-4: hemelglas (bottenzand + zuiverkristal, maakt glitches zichtbaar); fase 1 alleen grondstoffen, blok/recipe/lens in fase 2 | Crafting-gedreven sleutel; fases blijven zuiver |
| 2026-09-30 | conditions | Nether-4 is zuiverend: alle conditie-meters zakken er extra snel (geen eigen conditie) | Tegenpool van het gifmoeras voelbaar maken |
| 2026-09-30 | atmosphere | Sfeer Nether-4: wit-beige fog, droge wind, kristalgezoem, stof (biome-data) + pulserend kristallicht (fog-hook in `client/`) | Sterk, eigen effect; geen sync nodig |
| 2026-09-30 | worldgen | Ring 5-biomes: `corrupted_wastes`, `glitch_fields` (kern), `void_scars`, `inverted_hills`, `false_meadow` | Van kapot tot 'thuis dat niet klopt' |
| 2026-09-30 | worldgen | Void-scheuren gaan echt tot de void (geen bedrock), met goed zichtbare voidsteen-randen | Sterk en eng; eerlijk door zichtbaarheid |
| 2026-09-30 | blocks | Corrupted varianten van vanilla-natuurblokken met data map `corrupted → vanilla` + tag `corruptedadventure:corrupted`; gedrag pas in fase 5. Plus glitchblok en voidsteen | Fase 5 voegt alleen gedrag toe |
| 2026-09-30 | worldgen | Ring 5-materialen: nulliet (hoofdmateriaal, void-scheuren/diep) en glitchscherven (drop van glitchblokken); vanilla-ores vrijwel weg | Risico aan de afgrond; glitches oogsten |
| 2026-09-30 | worldgen | Ring 5-grotten: onmogelijke gangen (te perfecte rasters) en void-grotten | Echo van de ring 1-tunnels; echt gevaar |
| 2026-09-30 | atmosphere | Ring 5-weer: glitchstorm (void ×2) en omgekeerde regen (void ×1,5) | Twee verschillende 'fouten' |
| 2026-09-30 | conditions | Void-meter ring 5: licht (≥ 12) remt; drempels glitch-visuals → korte teleports (alleen naar veilige plek) → darkness + schade | Unieke 'de wereld klopt niet'-sensatie; bouwen met licht als verdediging |
| 2026-09-30 | worldgen | Ring 5-horror in fase 1: ontbrekende chunks, Herobrine-tekens, echo-voetstappen (client-only) | Direct Herobrine voelbaar |
| 2026-09-30 | worldgen | Nether-5: één biome `white_halls` (witte zuilenrasters, spiegelsteen-vloer waar de lavazee lag) | Het kale en eentonige is het punt |
| 2026-09-30 | blocks | Nether-5-blokken: kalkrack, witsteen-set, spiegelsteen, stil licht, witte kern | Eigen bouwmateriaal, steriel wit |
| 2026-09-30 | worldgen | Sleutelmateriaal Nether-5: witte kern (in de zuilen), toegang tot kastelen/final battle, basis zuiveringsgear | Laatste schakel van de Nether-lus |
| 2026-09-30 | conditions | Stilte Nether-5: ↓ door geluid maken (bel, goat horn, note block, jukebox; server-side game events) en spelers in de buurt; drempels geluid gedempt → HUD weg → alles stil + darkness → schade | Samenspelen beloond; horror via zintuigen |
| 2026-09-30 | infra | Bouwplan fase 1 aangevuld: blok E (ring 3, changes 38–57), F (ring 4, 58–76), G (ring 5, 77–95), H (worldgen-eindtest, 96); volgorde zoals ring 2 | Compleet plan tot het einde van fase 1 |
| 2026-09-30 | infra | Ringgrens-snapshot als JSON-bestand in de wereldmap, gelezen bij `ServerAboutToStartEvent`; ongeldige config of snapshot laat de server niet starten | Moet vóór de eerste chunk bestaan; liever een duidelijke fout dan een wereld met verkeerde grenzen |
| 2026-09-30 | worldgen | Grensruis: golven (~600 blokken) + rimpels (~150 blokken), constante in code | Natuurlijke grens zonder zichtbare cirkel; bij te stellen na de W-test in change 6 |
| 2026-09-30 | infra | `/ca tpring` landt op het radiale midden van de ring, in je huidige richting vanaf 0,0 (op 0,0: oost) | Langs één lijn door de ringen stappen |
| 2026-09-30 | infra | Debugdoelen: `tpring 1–6` (6 = void-rand) plus `/ca tpboundary <1–5> [nether]` op de verruisde grens (gewicht 0,5) | Test W in één commando per ring en per grens |
| 2026-09-30 | infra | Geen veilige landingsplek → toch teleporteren naar Y 100 (overworld) / 64 (Nether), met waarschuwing in de chat | OP's testen meestal in creative; het commando werkt altijd, ook in de void |
| 2026-09-30 | infra | `/ca ring` in vaste Engelse tekst, met afstand tot de dichtstbijzijnde grens inclusief ruis (met teken); `RingLayout.effectiveDistance` read-only erbij | Grensruis controleerbaar; lang-keys pas zinvol met datagen |
| 2026-09-30 | worldgen | Change 4: vaste biome `minecraft:plains` (overworld) en `minecraft:nether_wastes` (Nether) tot de `RingBiomeSource` er is; test "End niet bereikbaar" schuift naar change 5 | Simpelst; testwerelden van change 4 zijn wegwerp, change 5 vervangt de biome-source toch |
| 2026-09-30 | worldgen | Testseed voor test W: `20260930` | Numeriek en makkelijk te typen; gelijk in singleplayer en `server.properties` |
| 2026-09-30 | worldgen | Preset-naam in het menu: "Corrupted World" (en_us) / "Corrupte wereld" (nl_nl); lang met de hand tot er datagen is | Sluit aan op de registry-naam |
| 2026-09-30 | worldgen | Overworld houdt het vanilla dimension type; de preset is niet de standaard in "World Type" | Alleen de Nether heeft een afwijkende schaal nodig; vanilla-presets blijven onaangetast |
| 2026-09-30 | worldgen | Overgangszone kiest tot er overgangsbiomes zijn hard op gewicht 0,5 (inner/outer); overgangsbiomes later via een optioneel veld van de ring-BiomeSource | Niets om weg te gooien; de zone is via `/ca ring` zichtbaar |
| 2026-09-30 | worldgen | Placeholder-biomes: alleen eigen kleur en eigen weer (neerslag/temperatuur) per ring; geen features, geen mob-spawns, geen structure-tags | Minimaal werk, per ring herkenbaar; vijandige mobs hinderen test W niet |
| 2026-09-30 | worldgen | Void-biomes `void_edge` en `nether_void_edge` zijn blijvend; ringplaceholders heten tijdelijk `placeholder_ring_1…5` / `placeholder_nether_ring_1…5` | Void-sfeer later per dimensie; placeholders zijn aan de naam als tijdelijk herkenbaar |
| 2026-09-30 | worldgen | Lang (incl. preset-naam) vanaf change 5 via datagen (`en_us` + `nl_nl`); handgeschreven lang-bestanden verwijderd | Conventie "lang-keys via datagen" (CLAUDE.md §9) |
| 2026-09-30 | worldgen | Placeholder-terrein per ring naar de latere thema's: overworld Y ~70/95/120/64/85 (±8), Nether dak vanaf Y ~110 en vloer Y ~30/40/55/45/60 | Placeholders hinten al naar het echte terrein; elke ring vervangt ze later |
| 2026-09-30 | worldgen | `ring_blend` mengt met smoothstep (3t² − 2t³) op het lineaire gewicht van `ringAt` | Geen knik aan de randen van de overgangszone |
| 2026-09-30 | worldgen | Void-rand: zwevende eilanden die tussen de grens van ring 5 en ~9.900 uitdunnen, laatste ~100 blokken leeg; Nether ÷4 | Afbrokkelend einde van de wereld, met echte leegte vóór de border |
| 2026-09-30 | worldgen | Eigen chunk-generator `ring_noise` met ringbewuste vloeistof-kiezer en aquifer-wrapper (geen zee- of aquifer-vloeistof vanaf de zone ring 5 → void), via twee access-transformer-regels | Vanilla vult de void anders met water/lava; data-driven kan het niet, mixin is breekbaarder |
| 2026-09-30 | worldgen | Echte Nether-border van 2.500: overworld-grootte ÷ `coordinate_scale`, bij elke start afgeleid, volgt latere wijzigingen; alleen naar spelers in de Nether via vanilla-packets, geen mixins | Vanilla schaalt alleen het midden, niet de grootte; zo sluit de Nether-rand aan op de void-rand |
| 2026-09-30 | worldgen | Overworld-border (10.000) alleen zetten zolang hij nog op de vanilla-standaard staat | Werkt ook als de eerste start crasht; een admin-wijziging blijft staan |
| 2026-09-30 | worldgen | World border alleen in werelden met de `corrupted_world`-preset; schade en waarschuwing vanilla-standaard | Vanilla-presets blijven onaangetast; de border is alleen via de void bereikbaar |
| 2026-09-30 | worldgen | Debugcommando `/ca border` (grootte en midden in de huidige dimensie), als kleine categorie-uitzondering in change 7 | `/worldborder get` toont in de Nether altijd de overworld-waarde |
| 2026-10-01 | worldgen | Ring 1 krijgt zijn biomeverdeling via een eigen biome-source `corruptedadventure:vanilla_climate` (vanilla-preset omgezet naar onze kopieën) | `level.dat` en preset blijven klein, volgt vanilla automatisch, herbruikbaar voor Nether-ring 1 |
| 2026-10-01 | worldgen | Waar vanilla mushroom fields plaatst, komt in ring 1 `corruptedadventure:plains` | Eén regel in `replace`, geen terreinwerk; geen "veilige haven" zonder monsters |
| 2026-10-01 | worldgen | Ring 1-kopieën krijgen alle vanilla overworld-structures behalve stronghold en ancient city, inclusief trial chambers | Zo vertrouwd mogelijk vanilla; ring-gear ligt per ring een tier hoger dan trial-loot |
| 2026-10-01 | worldgen | Vanilla-wolf-varianten met een vaste biome-lijst worden in datagen overschreven met vanilla-biome + kopie (uit `VanillaBiomeCopies`) | Anders spawnt in de ring 1-kopieën elke wolf als pale; regressie van change 8 zelf (review) |
| 2026-10-01 | worldgen | Land rond spawn: continentalness `c + w · max(0, 0,03 − c)` met `w` = 1 binnen 300 blokken van 0,0, smoothstep naar 0 bij 450 (ondergrens "mid-inland") | Vertrouwd vanilla-begin, zee binnen loopafstand; land dat al hoger ligt blijft ongemoeid, geen knik aan de zonerand |
| 2026-10-01 | worldgen | Rivieren in de landzone rond spawn toegestaan (alleen continentalness aangepast, weirdness/ridges vanilla) | Vanilla-gevoel, geen extra JSON-kopieën; de spawn-spiraal kiest altijd een droge plek |
| 2026-10-01 | worldgen | Spawn via `spawn_target` (één punt, alle klimaatparameters `[-2, 2]`): 0,0 wint altijd, vanilla's 11×11-chunkspiraal kiest de droge plek; geen `CreateSpawnPosition`-listener | Geen Java nodig; gemeten over 10 seeds altijd ≤ 32 blokken van 0,0, nooit in water |
| 2026-10-01 | worldgen | Straal en ondergrens van de landzone staan in de JSON (`radial_falloff`), niet in de config | Geen ringgrens; een configwijziging zou naden in bestaande werelden geven (de ring-snapshot dekt dit niet) |
| 2026-10-01 | worldgen | Aparte `ring_blend/overworld_initial` voor `initial_density_without_jaggedness` (ring 1 = vanilla's eigen initiële dichtheid) | `0,390625 + final_density` legt het preliminary surface onder grotplafonds en is duur (niet-geïnterpoleerd per kolom) |
| 2026-10-01 | worldgen | Performance-beslisregel: noise-stap ≤ 1,3× vanilla → geen ring gate; erboven → gate. Uitkomst change 9: ring 1 1,17×, ring 3 0,92× → geen gate nu, wel bij de eerste zware ring 2-invoer (change 23) | Houdt change 9 klein; gate pas bouwen als hij nodig is |
| 2026-10-01 | blocks | Dood gras gedraagt zich als gras binnen zijn eigen familie: bedekt → verdorde aarde, spreidt in het licht alleen naar verdorde aarde, `snowy`-state, schop → pad, schoffel → farmland | Voelt vertrouwd; dode plekken blijven dood maar groeien niet (groeien hoort bij `corruption`) |
| 2026-10-01 | blocks | Elke houtset krijgt gestripte log en gestript wood (herbruikbare `StrippableLogBlock`) | Bijl werkt zoals spelers verwachten; sjabloon voor alle latere houtsets |
| 2026-10-01 | textures | Textures (ook placeholders) zijn eigen pixels uit een deterministisch generatorscript (`tools/textures/Ring1Textures.java`), geen vanilla-pixels in de repo; vervangt "getinte vanilla-placeholders" | Licentie-schoon en reproduceerbaar; change 17 verfijnt hetzelfde script |
| 2026-10-01 | blocks | Eén eigen creative tab "The Corrupted Adventure", per ring gesorteerd (registratievolgorde); mod-blokken niet in de vanilla-tabs | Alles op één plek bij het testen; geen tab-keuze per blok |
| 2026-10-01 | worldgen | Overgangsbiome 1→2 `withered_expanse` heeft een verloop binnen de zone via een eigen placement modifier (ring 1-kant meer kale bomen, ring 2-kant meer zand en zandsteen) | Voelt als een echte voorbode; deterministisch, server-side, herbruikbaar voor latere overgangen |
| 2026-10-01 | worldgen | Bodem van `withered_expanse`: dood gras als hoofdbodem, vlekken grove en verdorde aarde, zandsteen op hellingen; kort gras en dode struiken vaal getint | Gebruikt de blokken van change 10 waarvoor ze bedoeld zijn; duidelijk ander gebied |
| 2026-10-01 | worldgen | Een overgangsbiome vult de hele overgangszone, ook boven water (troebel water); geen voorrang voor ring 1-oceanen/-rivieren | Simpel en overal herkenbaar; geen tag-logica in `RingBiomeSource` |
| 2026-10-01 | worldgen | `withered_expanse` is droog als savanne (temperatuur 2,0, geen neerslag), vanilla-monsters, alleen konijnen als dier | De regen stopt al in de zone (voorbode van de droge ring); geen veilige strook; echte mobs in fase 4 |
| 2026-10-01 | worldgen | Eén placement modifier `corruptedadventure:ring_chance` met een `signal`-veld (change 11: `transition`); change 12 breidt uit met `progress` in plaats van een eigen type | Eén kanscurve, codec en test voor alle ringafhankelijke dichtheden |
| 2026-10-01 | worldgen | Kale plekken laten bestaande bomen en planten verdorren (bladeren weg, stammen → `dead_log`, kleine planten weg) | Sterk horror-beeld (kale bomen in hun oorspronkelijke vorm), ook in bossen |
| 2026-10-01 | worldgen | Kale plekken pas vanaf ~een derde van ring 1 (~500 blokken), kwadratisch oplopend tot 0,12 per chunk aan de rand (`ring_chance`, signaal `progress`) | Spawn voelt vertrouwd, de onrust groeit naar buiten en sluit aan op `withered_expanse` |
| 2026-10-01 | worldgen | Onnatuurlijke tunnels zijn een feature, geen carver; recht, horizontaal, 2×1, hooguit één haakse bocht, doodlopend, ~12–42 blokken | Een feature kan het hele pad vooraf valideren (droog, alles-of-niets) en werkt met `/place feature`; grotere rasters blijven voor ring 5 |
| 2026-10-01 | worldgen | Tunnels gelijkmatig zeldzaam over heel ring 1, ook bij spawn | Onrust vanaf het begin; de rand heeft al de kale plekken |
| 2026-10-01 | worldgen | Horror-features komen via NeoForge-biome-modifiers op eigen biome-tags (`has_feature/*`) in de ring 1-kopieën, niet in de kopieën zelf | Kopieën blijven 1-op-1 vanilla (change 8); modifiers raken alleen onze biomes |
| 2026-10-01 | atmosphere | Grotgeluid per ring 1-biome: `withered_expanse` sterk vreemd (eigen geluid, ~1,5 min in het donker); dark forest, old growth pine/spruce taiga en dripstone caves stil; overige kopieën (ook deep dark en lush caves) vanilla | Puur data (`mood_sound`), duidelijk contrast, de onrust zit aan de rand; geen client-code of netwerk |
| 2026-10-01 | atmosphere | Muziek blijft vanilla, ook in de stille biomes | Biome-muziek geldt voor de hele kolom; stilte aan het oppervlak is niet de bedoeling |
| 2026-10-01 | atmosphere | Vreemde grotgeluiden = eigen sound event dat vanilla's grotgeluiden lager en zachter afspeelt (`sounds.json`, type `event`) | Geen eigen audio nodig; later te vervangen door eigen `.ogg`'s zonder de biomes te wijzigen |
| 2026-10-01 | atmosphere | Onnatuurlijke tunnels krijgen geen eigen stille zone; ze erven `mood_sound` van hun biome | Stilte kan zonder client-code alleen per biome; geen netwerk of client-code nodig |
| 2026-10-01 | worldgen | Nether-ring 1 krijgt vanilla-structures (fortress, bastion, Nether-ruined-portals) via de vanilla-tags van de kopieën, met vanilla-spacing; een gegarandeerde fortress per seed volgt in fase 3 | Vertrouwd, en blaze rods en netherite-upgrades zijn te vinden; garantie is `structures`-werk |
| 2026-10-01 | worldgen | Nether-ring 1: vanilla-Nether-parameters met soul sand valley en basalt deltas → nether wastes (`replace`), en de klimaatruis van de Nether-router 4× fijner (`xz_scale` 1,0, geldt voor alle Nether-ringen) | Alle drie de biomes komen in de kleine ring voor; crimson en warped blijven op hun vanilla-plek |
| 2026-10-01 | worldgen | Nether-ring 1 gebruikt `corruptedadventure:vanilla_climate` (`preset: minecraft:nether`), niet `minecraft:multi_noise` met uitgeschreven parameters | Zelfde mechanisme als overworld-ring 1, volgt vanilla automatisch, `level.dat` blijft klein, grenzen van crimson/warped verschuiven niet |
| 2026-10-01 | blocks | Zielkristal-erts in twee varianten: `soul_crystal_ore` (netherrack) en `crimson_soul_crystal_ore` (crimson nylium met kristal, eigen textures, loot en tags) | Erts ook zichtbaar in de crimson-bodem; change 16 vervangt crimson nylium door de crimson-variant |
| 2026-10-01 | blocks | Zielkristal-erts dropt 1 `soul_crystal` (Fortune 1–4), Silk Touch → het erts, XP 3–7 | Vertrouwd ertsgedrag; `soul_crystal` als placeholder-item tot fase 2 |
| 2026-10-01 | blocks | Zielkristal-erts vraagt een ijzeren pickaxe (`needs_iron_tool`) | Sleutelmateriaal; past bij het niveau waarop spelers de Nether in gaan |
| 2026-10-01 | blocks | Zielkristal-erts geeft licht 3, geen particles | Valt op in het donker zonder client-code; licht 3 heeft geen effect op Nether-spawns |
| 2026-10-01 | blocks | `crimson_soul_crystal_ore` laat zonder Silk Touch alleen `soul_crystal` achter; het blok wordt lucht (geen netherrack) | Eenvoudig, één loot-helper, consistent met het netherrack-erts |
| 2026-10-01 | blocks | `crimson_soul_crystal_ore` zit niet in `minecraft:nylium` | Geen fungi/roots op de ertsplekken (kale plek = vindplaats-hint), geen bijwerkingen van bonemeal |
