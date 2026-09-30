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
- [mobs] Ring 2: zandwormen in de duinenzee, schorpioenen in canyons, mummies bij begraven skeletten. Nether-2: sporenmonsters, slijmwezens. (bron: sparring fase 1)
- [items] Nether-2: dauwkristal als basis van hittebescherming voor ring 3; druipzwam-hout; zielkristal (Nether-1) gebruik bepalen. (bron: sparring fase 1)

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
- [infra] Change 18 (sync naar client): de client kent de wereldseed niet, maar `RingLayout` heeft hem nodig voor de grensruis. Sync een afgeleide ruis-seed (bijv. hash van de wereldseed), nooit de seed zelf. (bron: change 2)
- [worldgen] Vanilla kiest het spawnpunt op het klimaat uit de noise settings (`spawn_target`), niet op de BiomeSource: het kan tot ~2.500 blokken van 0,0 liggen, dus buiten ring 1. Expliciet oplossen in change 9 (gegarandeerd land rond spawn nabij 0,0). (bron: change 5, review)
- [infra] Access transformer `META-INF/accesstransformer.cfg` is als minimale placeholder in een worldgen-change gekomen (twee regels: `NoiseBasedChunkGenerator.globalFluidPicker`, `NoiseChunk.aquifer`); bij een volgende infra-change nagaan of dit de gewenste plek/vorm is en het in `01-techniek.md` onder de stack vastleggen. (bron: change 6)
