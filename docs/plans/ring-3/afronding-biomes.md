# Golf 2-afronding — stroom W-biomes: 47d

Scope: `worldgen`. Worktree `.claude/worktrees/agent-a26041cd066434c4c`, branch `ring3/biomes-afr` (vanaf main `c6cd90d`). Regels: `build/agent-rules-afronding.md` (B + D per pakket, geen in-game tests; die doet de integratie). Besluiten: `teststop-4b-feedback.md`, "Beslissingen" 1, 2, 4, 5.

## 47d — wat er gebouwd wordt
1. **`dormant_caldera` → overwoekerde krater** (besluit 1): mosheuveltjes (`ground_patch` met verhoogde kern) over de hele bodem, mostapijt op het meeste kale mos, azalea-bosjes (vanilla `azalea_tree`) met kleine azaleastruiken (stam + ronde kroon van (bloeiende) azaleabladeren) eromheen waar de groeiruis `ring_3_caldera_growth` hoog is, losse (bloeiende) azalea's, varens en gras overal, dripleaf in de kommen, af en toe een omgevallen eikenstam, ranken en hangende wortels langs de wand (`wall_drape`), glow lichen en bessen aan de voet. Geen water.
2. **`sulfur_fields` → gekleurde kookpoelen + zwavelkristallen** (besluit 2): `terrace_pool`: een kookpoel gelijk met de grond, een verhoogde calcietrand en een band terrassen van gele/oranje terracotta en calciet. De gekleurde modder en de zwavelkristallen zijn blokken van stroom B (lijst naar main, 2026-10-09); tot ze er zijn kookt de poel met `boiling_mud` (`SulfurFieldsFeatures.poolFill`, één plek) en komen er nog geen kristallen.
3. **Meer zielvuur** rond de ribbenkasten (besluit 5): twee keer per ribvoet/wervel (salt 4 en 5), verschoven tot 5 resp. 9 blokken (van het bot af, waar de grondfilter wel vlakke grond vindt), marge 1 (de vlek wordt op de chunkrand afgesneden), vuur op de helft van de kern i.p.v. een kwart.
4. **Determinisme aslaag op erts** (besluit 4): `ash_drift` telt `c:ores` ook als grond. Een ertsklodder uit een buurchunk die tuff/basalt onder de laag in erts verandert (vóór of na deze chunk), geeft zo dezelfde laag.
5. **`ring_chance` sunstone/turquoise** (besluit 4): nieuwe filter `in_ring` (ring 2), zonder trekking uit de decoratie-random, vóór de biome-filter van `sunstone_ore` en `turquoise_ore`. Zo liggen ze niet meer in de ring 3-helft van `dry_riverbeds`, en verschuift de ring 2-decoratie niet (geen nieuwe placed feature, geen extra trekking).

Client/server: alleen worldgen-data en features (server). Raakt de noise-stap niet.

## Voortgang
- Voortgang: 2026-10-09 19:10 start; blokkenlijst (zwavelkristal, gekleurde modder) naar main voor stroom B.
- Voortgang: 2026-10-09 19:50 47d gebouwd (zonder B's blokken). D groen, B `--rerun-tasks` groen (667 tests, 0 fouten). Gecommit `29e3ebd`.
- Voortgang: 2026-10-09 20:30 B's zwavelblokken in de branch gemerged (`git merge --no-ff 60dcb38`, op aanwijzing van main; INT merget beide branches). Poelen in geel/groen/oranje zwavelmodder (één kleur per poel), zwavelkristallen op de kraag van elke zwavelfumarole (`GroundVentConfiguration.accent`) en in groepjes op de zwavelkorst. D groen, B `--rerun-tasks` groen (669 tests, 0 fouten).

## W-checklist (INT)
- [ ] Overwoekerde krater (`/locate biome corruptedadventure:dormant_caldera`): de bodem echt vol (mos, mostapijt, heuveltjes, varens), azalea-bosjes met struiken, dripleaf in kommen, ranken/hangende wortels langs de wand, een omgevallen stam; geen water; niets zwevend; geen afgesneden struiken/bomen op chunkgrenzen.
- [ ] Zwavelvelden: kookpoelen met calcietrand en terrassen, gelijk met de grond, niet op hellingen; gekleurde modder (geel/groen/oranje) met `sulfur_steam`; zwavelkristallen rechtop op de kraag van de fumarolen en in groepjes op de korst, nooit zwevend.
- [ ] Titanenveld: duidelijk meer zielvuur rond de ribbenkasten (INT2: 2).
- [ ] Determinisme (omgekeerd geladen): geen verschil meer in aslagen boven erts; mostapijt/varens op heuveltjes gelijk.
- [ ] `dry_riverbeds`: geen sunstone/turquoise in de ring 3-helft; ring 2-ertsen en -decoratie elders ongewijzigd t.o.v. main.

## CODEBASE-delta
- `worldgen/feature/InRingFilter` (`corruptedadventure:in_ring`, `ModPlacementModifierTypes.IN_RING`): houdt een positie aan de kant van `ring` (`RingLayout.ringProgress` aanwezig), zonder random-trekking; op `sunstone_ore`/`turquoise_ore` (`Ring2Features`).
- `worldgen/feature/WallDrapeFeature`/`WallDrapeConfiguration` (`corruptedadventure:wall_drape`, `ModFeatures.WALL_DRAPE`): ranken op de wand en hangende wortels onder overhangen, van de voet omhoog (kolom + 4 buren, eigen chunk).
- `worldgen/feature/TerracePoolFeature`/`TerracePoolConfiguration` (`corruptedadventure:terrace_pool`, `ModFeatures.TERRACE_POOL`): kookpoel met verhoogde rand en terrassen (`MAX_RADIUS` 3, `MAX_REACH` 6).
- `worldgen/feature/SulfurFieldsFeatures`: `ring_3/sulfur_terrace_pool`, placed `ring_3/sulfur_fields_terrace_pools` (LOCAL, eerst in de zwavelvelden); `poolFill()` = geel/groen/oranje zwavelmodder (B); `crystals()` = zwavelkristallen en -knoppen; `ring_3/sulfur_crust_crystal`, placed `ring_3/sulfur_fields_crust_crystals` (VEGETAL, op `sulfur_crust`, groepjes via `ring_3_fire_flora`).
- `GroundVentConfiguration.accent` (optioneel, `Accent(state, chance)`): iets bovenop de kraag; `ring_3/fumarole` krijgt zwavelkristallen (kans 0,35). Vents zonder accent houden hun random-volgorde.
- `worldgen/feature/DormantCalderaFeatures` (herschreven): nieuw `ring_3/caldera_moss_hummock|_moss_carpet|_azalea_shrub|_fallen_log_x|_z|s|_wall_drape`; placed `ring_3/dormant_caldera_moss_hummocks` (LOCAL), `_azalea_shrubs`, `_fallen_logs`, `_dripleaf` (vanilla `dripleaf`), `_moss_carpets`, `_wall_drapes` (VEGETAL); noise `worldgen/noise/ring_3_caldera_growth.json`. De begroeiing leest de eigen, live `WORLD_SURFACE` (in de krater schrijft elke feature alleen zijn eigen chunk).
- `TitanFieldsFeatures`: `ring_3/titan_fields_soul_fire_around` (tweede zielvuur), vuur 1 : 1 in de kern.
- `AshDriftFeature`: `c:ores` telt als grond.

## Beslissingen
- 2026-10-09 · worldgen · 47d: de overwoekerde krater vult de bodem met mos (heuveltjes 1–2 hoog, ~1 per chunk; mostapijt op ~60 % van het kale mos), azalea-bosjes waar `ring_3_caldera_growth` ≥ 0,15 (vanilla azaleaboom, kans 0,6 per chunk) met struiken (≥ −0,1), varens/gras op 30 %, dripleaf in kommen, omgevallen eikenstam 0,2 per chunk, ranken/wortels langs de wand. Startwaarden.
- 2026-10-09 · worldgen · 47d: kookpoelen als `terrace_pool` met vanilla calciet en gele/oranje terracotta (geen nieuw randblok); de gekleurde modder (3 losse blokken) en de zwavelkristallen komen van stroom B (merge `60dcb38` in deze branch). Kleuren 3 : 2 : 2 geel/groen/oranje; kristallen op 35 % van de kraagblokken van de zwavelfumarolen en op ~30 % van de korst in de groepjes.
- 2026-10-09 · worldgen · 47d: `in_ring` i.p.v. `ring_chance` voor sunstone/turquoise: `ring_chance` trekt per positie uit de decoratie-random en had zo alle volgende ertsposities van die feature verschoven.

## Backlog
- (geen open punten van 47d; de zwavelblokken van B zijn verwerkt)
