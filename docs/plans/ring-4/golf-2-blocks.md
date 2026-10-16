# Ring 4 golf 2 — stroom B (blokken): 72, 72b

Scope: `blocks`. Branch `ring4/blocks-g2` (worktree, basis `main` 9e30274; C (79) b9415ce en T (71b) a657a87 vooruit gemerged). Bron: `golf-2.md` §2 B, Q9–Q12, G1–G4, F1–F12, H1–H5, §6b; `contract.md` §3; `waves.md` §4–§5.

## 72 — gameplayblokken

| Blok | Gedrag |
|---|---|
| `swamp_gas` | onzichtbaar (`RenderShape.INVISIBLE`), geen outline of collision, vervangbaar, laat licht door, geen loot, **geen item**; state `lit`. Gele `swamp_gas`-particle in `animateTick`. Ontsteken: vuur, fakkel (ook ziel-/wandfakkel), brandend kampvuur of kaars, lava ernaast (`updateShape`/`onPlace`), vuursteen of vuurbal op een vlak met gas ervoor, en elke ontploffing die het bereikt (`SwampGasIgnition`). Ketting via scheduled ticks: een brandend blok verbrandt op zijn tick en steekt de gasburen 2 ticks later aan (golf, nooit recursief in één tick). `SwampGasBurn` per level: ≤ 64 verbrandingen per tick (rest wacht een tick), elk 8e blok van een brand is een clusterstart → echte ontploffing (kracht 2,5, `ExplosionInteraction.MOB` = blokschade alleen met `mobGriefing`), max. 4 per tick; overige clusterstarts flakkeren op met `block.swamp_gas.ignite`, de rest dooft als vlam-particles. Een nieuwe brand (na 40 ticks stilte) begint altijd met een knal. Een ontploffing laat het gas staan maar steekt het aan (`onExplosionHit`), zodat de ketting doorloopt, ook als `mobGriefing` de blokken spaart. In `poison_gases`. |
| `sucking_mud` | modder-sterkte/-geluid, schep. Geen bodem voor levende wezens (collision leeg), vast voor items, boten en vallende blokken. `entityInside` (beide kanten, zoals poedersneeuw): zakken 0,025/tick (~1 blok per 2 s), met springen omhoog 0,04/tick, horizontaal ~0,35–0,4× loopsnelheid; de zwaartekracht van `travel` wordt exact gecompenseerd. Eén keer per tick (`ColumnContact`). Hoofd onder (`SuckingMudSuffocation`, server, elke tick): adem −9/tick (vanilla geeft +4 terug → leeg in ~3 s, de bellen op de HUD waarschuwen), daarna 2 HP per 20 ticks via `SuckingMudDamage.hurt` (C: eigen damage type, `DifficultyFloor.LETHAL_FROM_NORMAL` voor spelers en mobs). Creatief/toeschouwer niet. Geen wand-verstikking van vanilla (`isSuffocating` false), de modder-texture over het scherm met het hoofd erin (`isViewBlocking`), geen spawns erop. Tell: `BUBBLE_POP` en het slurpgeluid (`AmbientSoundLimiter`). |
| `toxic_pool` | eigen vloeistof: `ModFluidTypes.TOXIC_POOL` (zwemmen als lava, verdrinken, geen boten, geen nieuwe bronnen), `ModFluids.TOXIC_POOL`/`FLOWING_TOXIC_POOL` (`ToxicPoolFluid`: daling 2 per blok, tick 30, hellingzoeken 2 → ~3 blokken), `LiquidBlock` `toxic_pool` (geen item), emmer `toxic_pool_bucket`. Menging (`ToxicPoolInteractions`): gif naast water → `mud`, naast lava → `stone`. `toxic_bubble` uit stille bronnen. Client: `client/fluid/ToxicPoolClientExtensions` (T's textures, tint wit, groene mist tot 4 blokken). In `poison_sources`. |
| `updraft` | zoals de bubble column: geen outline/collision, vervangbaar, geen loot, wel item (plat, T's texture). `entityInside` (beide kanten): +0,05/tick tot 0,4 + de eigen zwaartekracht, voor spelers, mobs en items; valafstand weg; één keer per tick (`ColumnContact`). Model: vier zijden buiten + vier binnenvlakken, cutout; zijden tegen een ander updraft-blok vallen weg. `updraft_bubble` en `block.updraft.flow`. |
| `bubble_spring` | nu `updraft_bubble` (start in het water, knapt aan het oppervlak) en `block.bubble_spring.bubble` (limiter). |
| `clawed_tidestone` | tidestone in alles, breekt in `cobbled_tidestone` (Silk Touch: zichzelf); in `ring_4_stones`. |
| `slime_trail` | platte decal (1 px outline) op een stevige vloer, doorloopbaar, vervangbaar, slijmgeluid; `axis` X/Z (geplaatst in de kijkrichting); alleen Silk Touch neemt hem mee. Geen gif. |
| `creature_nest` | volle kubus, hooi-eigenschappen (zonder zachte landing), schoffel, brandbaar 60/20 en door lava. |
| `moonpetal_leaves` | `MoonpetalLeavesBlock`: `blossom` valt onder de kruin (1 op 15 animate ticks), zoals kersenbladeren (backlog r. 271). |

Texturepaden (bevestigd met T, backlog r. 277): `block/sucking_mud`, `block/toxic_pool_still`/`_flow` (tint wit), `item/toxic_pool_bucket`, `block/updraft` (cutout), `block/clawed_tidestone`, `block/slime_trail` (platte decal), `block/creature_nest` (één texture rondom). Alles bestond al; geen ontbrekende PNG's bij `runData`.

### Client/server-impact
- Gasketting, ontsteking, verstikking en vloeistofmenging alleen op de server. De beweging in modder en opwaartse stroom draait in `entityInside` op beide kanten zonder client-class (de client beweegt zijn eigen speler). Particles/geluid via `animateTick`. Enige client-code: `client/fluid/ToxicPoolClientExtensions` (via `RegisterClientExtensionsEvent` in `CorruptedAdventureClient`). Geen payloads.
- AT: `public net.minecraft.world.entity.LivingEntity jumping` (de modder leest of een wezen springt).

### Tests (licht)
- **B:** `./gradlew build` groen: `SwampGasBurnTest` (knal bij nieuwe brand, 1 per 8 in een trage ketting, cap 64/4 per tick, ontstekers), `SuckingMudBlockTest` (zakken/stijgen, horizontale snelheid, vast voor items, adem/schade-ritme, updraft-topsnelheid), `ModBlocksTest` (volgorde 72 na de kristallen, emmer na de scherven, geen item voor gas en gifpoel, doorloopbaar, vloeistof/emmer gekoppeld), C's `PoisonCouplingsTest` groen; `ClientIsolationTest` groen.
- **D:** `runData` twee keer, `git diff --exit-code src/generated` schoon.

## 72b — tropische flora `verdant_falls` (§6b)

Paden en vormen volgens `golf-2-textures.md` (T, 71b). Creative-volgorde: na `creature_nest`, vóór Nether-4.

| Blok | Gedrag |
|---|---|
| houtset (16) | `verdant_log`/`_wood`/`stripped_*` (bijl strippt), planken, trap, plaat, hek, poort, deur, valluik, drukplaat, knop: eik-eigenschappen en -geluid (`ModWoodTypes.VERDANT_SET`), brandbaar als eik; `verdant_leaves` (vaste kleur, geen tint); `verdant_sapling` groeit alleen `ring_4/verdant_tree_fern` (`ModTreeGrowers.VERDANT`, key in `ModConfiguredFeatures` `// ring 4 — B`; tot 75b verbruikt bonemeal zonder groei) + `potted_verdant_sapling`. Geen borden, boot of recepten (H1 = A). |
| `verdant_giant_leaf` | dubbele plant (`TallFlowerBlock`, zonnebloem-eigenschappen: niet vervangbaar), bonemeal → kopie; onderste helft dropt. |
| `verdant_fern` | `VerdantFernBlock`: varen-eigenschappen (vervangbaar, offset), bonemeal → `verdant_tall_fern` (als er ruimte is), loot als varen. |
| `verdant_tall_fern` | dubbele plant (`TallFlowerBlock`, grote-varen-eigenschappen), loot als grote varen. |
| `verdant_bromeliad` + `verdant_wall_bromeliad` | `VerdantPlantBlock` op `#dirt`; de wandvorm `VerdantWallPlantBlock` (geen item, `StandingAndWallBlockItem` zoals de fakkel, dropt de bromelia). Gloei-overlay (emissive laag), geen bloklicht. |
| `verdant_orchid` | `VerdantWallPlantBlock` (`facing` weg van de wand; stevige wand nodig, anders valt hij), plat vlak op de wand + emissive `_glow`-laag, geen bloklicht. |
| `verdant_hanging_orchid` | `VerdantHangingOrchidBlock`: hangende wortels (onder een stevige onderkant, waterlogbaar). |
| `verdant_blossom_bush` | `VerdantPlantBlock` (kruis). |
| `verdant_vine` + `verdant_vine_plant` | `VerdantVineBlock`/`VerdantVinePlantBlock`: weeping vines (hangt, groeit omlaag op random ticks en met bonemeal), klimbaar (`#climbable`), state `flowering` op beide; een liaan houdt `flowering` bij groei en bij kop ↔ stengel. Stengel zonder item; loot als weeping vines. **Voor 75b:** de kop op leeftijd 25 plaatsen waar een streng niet langer mag worden; de liaan is niet waterlogbaar (het hangmos wel), dus niet in water plaatsen. |
| `verdant_water_bloom` | `VerdantWaterBloomBlock`: lelieblad (op een waterbron, beloopbaar), plat model zonder tint. |
| `verdant_fruit_pod` | `VerdantFruitPodBlock`: cacao (3 stadia, random ticks en bonemeal, `facing` naar de stam) maar alleen aan `#verdant_logs`; geen item; dropt `verdant_fruit` (rijp 3, anders 1); modellen met parent `minecraft:block/cocoa_stageN`. |
| item `verdant_fruit` | placeholder zonder voedingswaarde (H3 = A), na `toxic_pool_bucket`. |

Bonemeal (F11 = A): planten die niet groeien laten een kopie vallen (`PoppingBonemealable`, zoals vanilla's hoge bloemen); varen, liaan, peul en sapling groeien.
Tags: `verdant_logs` (blok + item) → `#logs_that_burn`; planken, `wooden_*`, poort, bladeren (`#leaves`, hoe), `#saplings`, `#flower_pots`, `c:stripped_*`, `c:fence_gates/wooden`; `#parrots_spawnable_on` = bladeren + stam (expliciet, F10); `#climbable` liaan + stengel; `#replaceable` varens; `#replaceable_by_trees` zachte flora; `#sword_efficient` planten. Compost en `ModFlammables` zoals de vanilla-tegenhangers (waterbloem en peul niet brandbaar). `ModCreativeTabs` toont elk item één keer (`distinct`: de wandbromelia deelt het item van de bromelia).

### Client/server-impact 72b
Alles gewone blokken; groei op de server (random ticks, bonemeal). Geen client-code, geen payloads.

## Voortgang
- 2026-10-16: 72 gebouwd; B en D groen.
- 2026-10-16: 72 vooruit op main (3585b20); keuzes 1–4 bevestigd door de gebruiker.
- 2026-10-16: 72b gebouwd; B (incl. `ModBlocksTest`, `ModTreeGrowersTest`) en D groen, geen ontbrekende textures.

## CODEBASE-delta
**Nieuwe classes**
- `block/SwampGasBlock` (gas, `LIT`, `ignite(level, pos, delay)`, `ignites(state)`), `block/SwampGasBurn` (budget per level per tick), `block/SwampGasIgnition` (game bus: `ExplosionEvent.Detonate`, `PlayerInteractEvent.RightClickBlock` met `ItemAbilities.FIRESTARTER_LIGHT`).
- `block/SuckingMudBlock` (`nextVerticalSpeed`), `block/SuckingMudSuffocation` (game bus: `EntityTickEvent.Post`; `nextAir`, `hurtsNow`).
- `block/UpdraftBlock` (`nextVerticalSpeed`), `block/ColumnContact` (één blok per entity per tick), `block/SlimeTrailBlock` (`AXIS`), `block/MoonpetalLeavesBlock`.
- `fluid/ToxicPoolFluid` (`Source`/`Flowing`), `fluid/ToxicPoolInteractions` (common setup).
- `client/fluid/ToxicPoolClientExtensions`.
- `registry/ModFluidTypes` (`TOXIC_POOL`), `registry/ModFluids` (`TOXIC_POOL`, `FLOWING_TOXIC_POOL`); beide in `CorruptedAdventure` geregistreerd (na `ModItems`), net als `ToxicPoolInteractions`, `SwampGasIgnition`, `SuckingMudSuffocation`.

**Gewijzigd**
- `ModBlocks`: sectie "Gameplay blocks (package 72)" na `aquamarine_cluster` (vóór Nether-4): `swamp_gas` (zonder item), `sucking_mud`, `toxic_pool` (zonder item), `updraft`, `clawed_tidestone`, `slime_trail`, `creature_nest`; `moonpetal_leaves` is een `MoonpetalLeavesBlock`.
- `ModItems`: `toxic_pool_bucket` (`BucketItem`) na `aquamarine_shard`.
- `BubbleSpringBlock` (particle + geluid), `ModFlammables` (nest), `accesstransformer.cfg` (`LivingEntity.jumping`).
- Datagen: `ModBlockStateProvider.ring4Gameplay()`/`updraft()`, item-models, loot, `ModBlockTagsProvider.ring4Gameplay()`, `Ring4BlockNames` (+ `fluid_type.corruptedadventure.toxic_pool`).

**Tags:** `poison_sources` = `toxic_pool`; `poison_gases` = `swamp_gas` (direct, geen geneste tags); `ring_4_stones` + `clawed_tidestone`; `#replaceable` + gas, updraft, slijmspoor; `#mineable/shovel` modder, `/pickaxe` klauwkras, `/hoe` nest.

**Registries:** 7 blokken (5 met item), 1 item, 1 fluid type, 2 fluids.

**72b — nieuwe classes:** `block/PoppingBonemealable` (bonemeal laat een kopie vallen), `VerdantPlantBlock`, `VerdantFernBlock`, `VerdantWallPlantBlock`, `VerdantHangingOrchidBlock`, `VerdantVineBlock` (`FLOWERING`), `VerdantVinePlantBlock`, `VerdantWaterBloomBlock`, `VerdantFruitPodBlock`.
**72b — gewijzigd:** `ModBlocks` (sectie verdant na `creature_nest`; helpers `verdant(...)`, `registerDoubleHigh`; item van de bromelia als `StandingAndWallBlockItem` in een static-blok), `ModItems` (`verdant_fruit`), `ModWoodTypes` (`VERDANT_SET`, `VERDANT`), `ModTreeGrowers` (`VERDANT`), `ModConfiguredFeatures` (`VERDANT_TREE_FERN`, blok `// ring 4 — B`), `ModTags` (`Blocks/Items.VERDANT_LOGS`), `ModFlowerPots`, `ModFlammables`, `ModCreativeTabs` (`distinct`), datagen (`verdant()` in blockstate-, item-model-, loot- en tagproviders; `doublePlant`, `glowingWallPlant`, `vine`, `fruitPod`; compost; lang).
**72b — registries:** 29 blokken (25 met item: geen item voor `potted_verdant_sapling`, `verdant_wall_bromeliad`, `verdant_vine_plant`, `verdant_fruit_pod`), 1 item.

## Beslissingen
- 2026-10-16, `blocks` (implementatie B): `swamp_gas` heeft geen item (afwijking van contract §3 "alle blokken een item"): het is onzichtbaar, heeft geen outline en kan niet gericht of gebroken worden, zoals vanilla's bubble column; alleen worldgen en commando's plaatsen het. Omkeerbaar.
- 2026-10-16, `blocks` (implementatie B, met akkoord main dat B hoeveelheid en interval kiest): zuigmodder-schade = de adem raakt in ~3 s op (adem −9/tick, de luchtbellen op de HUD waarschuwen), daarna 2 HP per seconde (zoals verdrinken), via C's `SuckingMudDamage`. Server-side als entity-tick, omdat de server een stilstaande speler niet elke tick beweegt.
- 2026-10-16, `blocks` (implementatie B): zakken 0,025/tick, met springen omhoog 0,04/tick (ook met het hoofd onder), horizontaal ~0,35–0,4× loopsnelheid; de beweging in `entityInside` op beide kanten met een access transformer op `LivingEntity.jumping`.
- 2026-10-16, `blocks` (implementatie B): moerasgas: buren branden 2 ticks later; een brand begint opnieuw te tellen na 40 ticks stilte (eerste blok knalt altijd); ontstekers: vuur, fakkels (geen redstonefakkel), brandend kampvuur/kaars, lava, vuursteen/vuurbal, ontploffingen. Brandende wezens en vuurpijlen steken het niet aan.
- 2026-10-16, `blocks` (implementatie B): opwaartse stroom +0,05/tick tot 0,4 blok/tick (~8 blok/s) bovenop de eigen zwaartekracht; werkt in de lucht (anders dan de bubble column).
- 2026-10-16, `blocks` (implementatie B): weergavenaam van de gifpoel "Toxic Sludge" / "Gifslijk" (vloeistof, blok en emmer "Toxic Sludge Bucket" / "Emmer met gifslijk"); registry-namen blijven `toxic_pool`/`toxic_pool_bucket` (contract).
- 2026-10-16, `blocks` (implementatie B): `slime_trail` heeft een `axis` (X/Z) zodat sporen aansluiten, valt zonder vloer weg en geeft alleen met Silk Touch iets; `creature_nest` is een volle kubus met hooi-eigenschappen, brandbaar; `clawed_tidestone` hoort bij `ring_4_stones` (het is tidestone).

- 2026-10-16, gebruiker: de vier keuzes van 72 (gas zonder item; ~3 s adem dan 2 HP/s; ontstekers zoals gebouwd; "Toxic Sludge"/"Gifslijk") bevestigd.
- 2026-10-16, `blocks` (implementatie B, 72b): planten die niet groeien laten met bonemeal een kopie vallen (zoals vanilla's hoge bloemen); de varen groeit tot de hoge varen. De orchidee is alleen een wandplant (item plaatst hem tegen een wand); de bromelia staat op de grond en het item zet hem tegen een wand zoals een fakkel. Reuzenblad niet vervangbaar (een grote plant), de varens wel. De liaan is niet waterlogbaar. Vruchtpeul alleen aan `#verdant_logs`.
- 2026-10-16, `blocks` (implementatie B, 72b): kaartkleuren: schors `TERRACOTTA_WHITE` (zandbeige), hout/planken `SAND` (honingcrème), kruin `GLOW_LICHEN` (pastel zeegroen).

## Backlog
- `blocks`: moerasgas aansteken door brandende wezens, vuurpijlen en een dispenser met vuursteen (nu niet: vanilla plaatst vuur alleen in lucht).
- `blocks`: `swamp_gas` een item geven als de testers het in creatief willen plaatsen (nu `/setblock`/`/fill`).
- `atmosphere`/`textures`: de groene mist in de gifpoel (kleur `4E8A14`, 4 blokken) in-game beoordelen (5b).
- `worldgen` (75b): de liaankop op leeftijd 25 plaatsen in `ledge_strand` (anders groeien strengen door); de liaan niet in water plaatsen (niet waterlogbaar); `verdant_fruit_pod` alleen tegen een `verdant_log` met `facing` naar de stam.
- `items`: recepten van de `verdant`-houtset en het nut van `verdant_fruit` (fase 2; staat al in de backlog).
- `conditions`: zuigmodder-ritme (3 s adem, 2 HP/s) en de zak-/stijgsnelheid afstemmen in teststop 5b.
