# Fase 1 — Wereld

Uitwerking van de sparringsessie(s) voor fase 1. Bron van waarheid voor *wat* we in fase 1 bouwen.
Beslissingen staan ook (kort) in `02-beslissingenlog.md`.

**Status:** sessie 1 afgerond — fundament, ring 1 en ring 2 (incl. Nether-tegenpolen) staan vast. Volgende sessie: ring 3, 4 en 5.

---

## 1. Fundament

### 1.1 Bouwvolgorde
1. **Skelet:** world preset (overworld + Nether), `ringAt`, ring-BiomeSource, één placeholder-biome per ring, `ring_blend` density function.
2. **Ring voor ring** (1 → 5), telkens met de Nether-tegenpool: terrein → blokken → ores/grotten → sfeer/weer → condities → definitieve textures.

### 1.2 Terrein-architectuur
- Er is één noise settings per dimensie. Terrein per ring komt uit een eigen density function **`corruptedadventure:ring_blend`** (Java-type + codec).
- `ring_blend` krijgt per ring een eigen density function (JSON, in `worldgen/density_function/<ring>/…`) en kiest op basis van `ringAt` welke geldt. In de overgangszone mengt hij lineair (of smoothstep) tussen de twee aangrenzende ringen.
- Alle overige terreinlogica (vorm, canyons, vulkanen) blijft data-driven in JSON.
- Client/server: puur worldgen, deterministisch per seed; geen sync nodig.

### 1.3 Ringgrenzen en overgangszones
- Grenzen uit config (zie `00-overzicht.md`), vervormd met seeded ruis van **±96 blokken**.
- Overgangszone van **~128 blokken** rond elke grens (Nether: ÷4, dus ~32 blokken en ±24 ruis).
- In de overgangszone: blend van terrein en een eigen overgangsbiome per grens (uit te werken per ring).
- **Config → snapshot per wereld:** de config geeft de standaardwaarden. Bij het aanmaken van een wereld worden ze in die wereld vastgelegd; daarna gebruikt `ringAt` alleen die snapshot. Een aangepaste config geldt alleen voor nieuwe werelden. De server synct de waarden naar de client (voor HUD en muren).
- Aandachtspunt voor de bouw: de snapshot moet beschikbaar zijn **vóór de eerste chunk** gegenereerd wordt (ook de spawn-chunks). Waarschijnlijk betekent dat een eigen bestand in de wereldmap, gelezen bij `ServerAboutToStartEvent`, in plaats van gewone `SavedData` (die pas bestaat als de level geladen is). Uitzoeken in change 1.

### 1.4 Textures
- Eerst **placeholders**: getinte/gerecolorde vanilla-textures, zodat worldgen en sfeer testbaar zijn.
- Per ring een aparte `textures`-change die de textures definitief maakt.

### 1.5 Grensmuur (ontwerp nu, bouw in fase 5)
- Elke grens krijgt een **eigen soort muur** die zicht op de volgende ring vaag houdt tot de speler de bewaker van die grens heeft verslagen. Type per grens: zie de ringsecties.
- De muur is **per speler** (volgt de per-speler unlocks). Rendering client-side in de overgangszone, staat server-side en gesynct.
- In fase 1 wordt de muur alleen ontworpen; de bouw hoort bij het grenssysteem (`progression`, fase 5).

### 1.6 Weermodel
- **Vanilla regen/onweer blijft** en is alleen zichtbaar in biomes met neerslag (ring 1, deels ring 4).
- Daarnaast **ringweer**: per ring een eigen weercyclus (typen per ring: zie ringsecties) in `SavedData`, per dimensie.
- Server bepaalt en synct (`CustomPacketPayload`) de weerstaat per ring; de client rendert alleen (lucht, fog, neerslag, particles) vanuit `client/`.
- Gameplay-effecten van weer (bijv. extra hitte bij asregen) lopen via de condities-tick op de server, nooit op de client.

### 1.7 Ring 1: kopieën van vanilla biomes
- Ring 1 gebruikt **eigen kopieën** van de vanilla overworld-biomes (`corruptedadventure:…`), gebouwd in datagen met vanilla's eigen bouwmethodes (`OverworldBiomes`).
- Kopieën krijgen de `has_structure/*`-tags die we willen (bijv. villages), maar **nooit** `has_structure/stronghold` → geen strongholds, geen End-portal.
- Ring 1 gebruikt de vanilla-klimaatparameters, zodat de biomeverdeling vertrouwd aanvoelt.

### 1.8 Buitenrand
- 9.500–10.000: het terrein van ring 5 brokkelt af tot **zwevende eilanden** en daarna lege **void** (via `ring_blend`: een aparte 'void'-invoer).
- **World border op 10.000** (Nether: 2.500 via `coordinate_scale` 4). Gezet bij het aanmaken van de wereld (server-side).

### 1.9 Omgevingscondities (systeem)
- Per conditie (hitte, gif, void, …) een **meter 0–100 per speler**, opgeslagen in een player attachment.
- Een server-side tick-handler (elke 20 ticks) bepaalt via `ringAt` + biome (+ ringweer) of de meter oploopt of afneemt.
- **Drempels** geven oplopende effecten (bijv. 50: traagheid, 75: zwakte, 100: schade). Sterktes en snelheden in config.
- **HUD-balk** op de client toont de meter; waarde wordt gesynct (alleen bij verandering). HUD-code in `client/`.
- Tegenmaatregelen (gear/items) komen in fase 2 en remmen de opbouw; in fase 1 alleen de conditie zelf.
- Ring 1 heeft geen conditie; het systeem verschijnt voor het eerst in ring 2.
- Nether-condities alleen in **Nether-3** (kou/bevriezing) en **Nether-5** (stilte); de andere Nether-ringen hebben er geen.

### 1.10 Biome-varianten
- Ring 1: kopieën van vanilla biomes (zie §1.7).
- Ring 2–5: **4–5 varianten per ring** + **1 overgangsbiome per grens**. Nether-ringen: aantal per ring in de ringsecties.
- Bouwvolgorde per ring: eerst 2–3 kernvarianten, de overige als latere changes binnen dezelfde ring.

### 1.11 Ores (algemeen)
- Ring 1 heeft alle vanilla-ores.
- Naar buiten toe **bouwen vanilla-ores af**: ijzer en kolen schaarser, diamant bijna afwezig. Ringores nemen het over.
- Netherite/ancient debris blijft zoals vanilla. Ring-gear is per ring een tier hoger (fase 2), zodat netherite rond ring 3 al achterloopt.

### 1.12 Nether-tegenpolen (schema)
| Ring | Overworld | Nether-tegenpool |
|---|---|---|
| 1 | Vertrouwd vanilla | Vanilla-Nether (kopieën: wastes, crimson, warped) |
| 2 | Droog, canyons, oases | Doorweekte schimmel-Nether |
| 3 | Vulkanisch, water gestolen | Bevroren Nether (ijs, gestolde lava) |
| 4 | Moeras, gif | Droge kristal-/bottenwoestijn (zuiverend) |
| 5 | Corrupted, void | 'Zuivere' witte Nether, onheilspellend stil |

Sleutelmaterialen uit Nether-ring N zijn thematisch een tegengif voor ring N+1.

---

## 2. Ringen

### Ring 1 — Vanilla-ring (0 – 1.500)

**Terrein en sfeer**
- Vanilla-terrein (vanilla noise router als ring 1-invoer van `ring_blend`). Oceanen, rivieren, hoge bergen en pieken blijven.
- Rond de spawn (~300 blokken) wordt de continentalness omhoog geschoven: altijd land in het midden.
- Lucht, fog en kleuren vanilla; in de buitenste zone subtiel valer (via het randbiome/overgangsbiome).

**Biomes**
- Eigen kopieën van de vanilla overworld-biomes (zie §1.7), inclusief deep dark. **Geen** mushroom fields.
- Overgangsbiome 1→2: **verdorde vlakte** (`withered_expanse`, werktitel): bladloze bossen, vaal gras, zandsteenranden, een voorbode van ring 2.

**Unieke blokken (fase 1, placeholders)**
- Dood gras / verdorde aarde, dood hout (log + planks) voor kale plekken en het overgangsbiome.

**Ores**
- Alle vanilla-ores, vanilla-verdeling.

**Grotten**
- Vanilla noise caves en carvers, inclusief deep dark (zonder ancient cities; beslissing in fase 3).
- **Stille zones**: plekken waar ambient geluid wegvalt of vreemde cave sounds vaker klinken, vaker richting de rand (client-side ambient).

**Weer en condities**
- Alleen vanilla regen/onweer. Geen ringweer, **geen conditie**.

**Horror-elementen (fase 1)**
- Zeldzame **onnatuurlijke 2×1-tunnels** in grotten en bergen (worldgen-feature).
- **Kale plekken met dode bomen**, vaker richting de rand.
- Stille zones in grotten (zie Grotten).
- Events (fakkels, silhouetten) → fase 5, zie backlog.

**Grensmuur 1→2** (fase 5): dichte witte mist.

**Nether-tegenpool — Nether-ring 1 (0 – 375)**
- Kopieën van nether wastes, crimson forest, warped forest (geen soul sand valley, geen basalt deltas). Vanilla Nether-terrein.
- Vanilla Nether-ores (quartz, goud, ancient debris).
- **Sleutelmateriaal: zielkristal** — nieuw, zeldzaam erts in de crimson-bodem en netherrack, alleen in Nether-ring 1. Nodig voor bewaker 1→2 (fase 5); gebruik in fase 2.
- Geen conditie.

**Client/server-risico's:** geen bijzondere. Stille zones zijn biome-ambient (data) of client-only; tunnels en kale plekken zijn deterministische features.

### Ring 2 — Droge ring (1.500 – 3.500)

**Terrein en sfeer**
- Droog, warm licht; lucht bleek geelblauw, fog zanderig. Canyons diep ingesneden, hoodoos verticaal, duinen golvend, zoutvlakte vlak.
- Eigen ring 2-invoer voor `ring_blend`, met per biome-regio een eigen vorm (canyons en duinen via noise binnen de ring 2-density function).

**Biomes (5)**
| Biome (werktitel) | Karakter |
|---|---|
| `canyonlands` | Diepe kloven, gelaagde kloofsteen (rood/oranje/crème) — kernbiome |
| `oasis_flats` | Zandvlakte met waterpoelen, palmen en groen; het enige veilige water; villagers (fase 3) — kernbiome |
| `salt_flats` | Kaal, wit en vlak met zoutkorst |
| `hoodoo_fields` | Zandsteenpilaren, bogen, balancerende rotsen |
| `dune_sea` | Hoge golvende duinen, makkelijk verdwalen |

- Overgangsbiome 2→3: **drooggevallen rivierbeddingen** (`dry_riverbeds`): brede lege beddingen met gebarsten klei, gestrande botten en resten; richting ring 3 zwart geblakerd. Vertelt het verhaal van het gestolen water.

**Unieke blokken (placeholders eerst)**
- **Kloofsteen-set**: gelaagde canyonsteen in 3 tinten + cobble, bricks, slab, stairs, wall.
- **Zout-set**: zoutkorst (bovenlaag zoutvlakte), zoutblok, gepolijst zout, zoutkristal (decoratie, zwak licht).
- **Palm-set**: log, wood, planks, leaves, sapling + houtset (deur, trapdoor, hek, poort, trap, slab, knop, drukplaat); eigen tree feature.
- **Woestijnflora + scorched sandstone**: scorched sandstone-set, gebarsten klei, dorre struik, tonnetjescactus, woestijnbloem.

**Ores**
- **Zonnesteen** (hoofdmateriaal, kandidaat ring 2-gear-tier): gloeiend erts hoog in canyonwanden en hoodoos.
- **Turkoois**: zeldzaam, diep in kloofsteen.
- Vanilla-ores afgebouwd: ijzer en kolen minder dan in ring 1, diamant zeldzaam.

**Grotten**
- **Ondergrondse waterreservoirs** (aquifers) die de oases voeden; oriëntatie en lore.
- **Instortende zandgrotten**: plafonds van los zand (vanilla-zwaartekracht, server-side).

**Weer (ringweer)**
- **Zandstorm**: zicht sterk beperkt (client fog + particles), lichte wind-duw (server).
- **Hittegolf**: lucht trilt (client), uitdroging loopt 2× sneller (server).

**Conditie: uitdroging**
- Meter ↑ overdag in direct zonlicht (hemel zichtbaar); 2× in een hittegolf.
- Meter ↓ in schaduw, in water, in het oase-biome, 's nachts, en ~30 punten per vanilla-waterfles.
- Drempels (config): sneller honger → traagheid → mining fatigue → lichte schade.
- Tegenmaatregel in fase 2: veldfles/waterzak.

**Horror-elementen (fase 1)**
- **Zingend zand**: laag zoemend geluid in de duinenzee, vooral 's nachts (biome-ambient, data).
- Fata morgana → fase 5 (backlog).

**Grensmuur 2→3** (fase 5): wervelende zandstorm.

**Nether-tegenpool — Nether-ring 2 (375 – 875): doorweekte schimmel-Nether**
- **Terrein: zwamkathedralen** — hoge open holtes, reuzenzwammen als zuilen van vloer tot plafond; de lavazee ligt lager en is deels bedekt met sporenmos-eilanden.
- **Biomes**: druipende reuzenzwamwouden (hoofdbiome), lichtgevende sporengrotten. Dauwbronnen als feature in beide.
- **Blokken**: reuzenzwam-set (druipzwam-steel, -hoed, planks + houtset), doorweekt netherrack, sporenmos (spreidt zoals nylium), slijmstalactieten (dripstone-gedrag), lichtgevende schimmel + zwamlamp, dauwbron.
- **Dauwbron**: eigen natuurblok dat water-achtig oogt en druppelt; **geen echte waterfluid**, de Nether blijft `ultrawarm`.
- **Sleutelmateriaal: dauwkristal**, groeit langzaam aan dauwbronnen (zoals budding amethyst). Nodig voor bewaker 2→3; in fase 2 de basis van hittebescherming voor ring 3.
- **Sfeer**: dichte grijsgroene fog, constante druppelgeluiden, zwevende sporen-particles (alles biome-data).
- Geen conditie.

**Client/server-risico's**
- Zandstorm-duw en hittegolf-effect: server-side (condities-tick/ringweer); fog, particles en luchttrilling alleen client (`client/`).
- Instortend zand, dauwkristalgroei en sporenmos-spreiding: vanilla-mechanismen / random ticks, server-side.
- Uitdroging-meter: attachment + sync; HUD client-only.

### Ring 3 — Vulkanische ring (3.500 – 5.500)
_Volgende sessie._

### Ring 4 — Moerasring (5.500 – 7.500)
_Volgende sessie._

### Ring 5 — Corrupted ring (7.500 – 9.500)
_Volgende sessie._

---

## 3. Bouwplan

Elke change: één categorie, afgesloten met `/code-review`, `CODEBASE.md` bijgewerkt. Ring 3–5 worden na de volgende sparringsessie aan dit plan toegevoegd.

**Standaardtests** (per change aangegeven):
- **B** — `./gradlew build` slaagt.
- **D** — `./gradlew runData` slaagt; gegenereerde output gecommit.
- **S** — `./gradlew runServer` (met `level-type=corruptedadventure:corrupted_world`) start zonder client-class-fouten; een client kan joinen.
- **W** — nieuwe wereld met vaste testseed (vastleggen in change 4); teleporteren naar het midden van elke ring en elke grens (overworld én Nether); portal-test vanuit elke ring.

### A. Skelet
| # | Categorie | Change | Test |
|---|---|---|---|
| 0 | `infra` | Package hernoemen `nl.corruptedadventure` → `nl.timtoys.corruptedadventure` (bronmappen, `mod_group_id`, `CODEBASE.md`). | B, S |
| 1 | `infra` | `ModConfig` met ringgrenzen (5 buitenradii, void-rand 9.500/10.000), grensruis ±96, overgangsbreedte 128. **Snapshot per wereld** vastgelegd vóór de eerste chunk (zie §1.3). Sync naar de client volgt in change 18 (de client heeft het pas nodig voor HUD en weer). | B, S; configbestand met standaardwaarden; snapshot-bestand in de wereldmap; config wijzigen → bestaande wereld houdt oude waarden |
| 2 | `worldgen` | `Ring` enum (`RING_1`…`RING_5`, `VOID`) + `RingLayout.ringAt(dimension, x, z)` met seeded grensruis, Nether-factor 4 en een overgangsgewicht (0–1) naar de buurring. | B; unit tests voor grenzen, Nether-schaal en determinisme per seed |
| 3 | `infra` | Debugcommando's: `/ca ring` (ring, afstand, overgangsgewicht op je positie) en `/ca tpring <n> [nether]`. Alleen voor OP's. | B, S; commando's werken op de server |
| 4 | `worldgen` | World preset `corrupted_world` + dimension type `corrupted_nether` (`coordinate_scale` 4, `ultrawarm`), voorlopig met vanilla terrein en vaste biome. The End blijft technisch in de preset. Testseed vastleggen. | B, S, W: preset kiesbaar; portals landen op ÷4; End niet bereikbaar |
| 5 | `worldgen` | `RingBiomeSource` (+ codec) + één placeholder-biome per ring in overworld en Nether (datagen), plus een void-biome. Geen stronghold-tags. | B, D, S, W: F3 toont de juiste placeholder per ring; `/locate structure stronghold` vindt niets |
| 6 | `worldgen` | Density function `ring_blend` (+ codec) met per ring een placeholder-terrein (duidelijk verschillende hoogtes) en een void-invoer (zwevende eilanden → leegte, 9.500–10.000). Voor overworld en Nether. | B, S, W: hoogteverschil per ring zichtbaar, overgang zonder kliffen, void voorbij 9.500 |
| 7 | `worldgen` | World border op 10.000 bij wereldcreatie (Nether 2.500 via schaal). | B, S: `/worldborder get` in overworld en Nether |

### B. Ring 1
| # | Categorie | Change | Test |
|---|---|---|---|
| 8 | `worldgen` | Kopieën van vanilla overworld-biomes (datagen via `OverworldBiomes`, zonder mushroom fields), met vanilla-klimaatparameters binnen ring 1; tags: villages e.d. wel, stronghold niet. | B, D, W: vertrouwde biomeverdeling; `/locate` village ✓, stronghold ✗ |
| 9 | `worldgen` | Ring 1-terrein: vanilla noise router als ring 1-invoer van `ring_blend` + gegarandeerd land rond spawn (~300 blokken). | B, W: 10 seeds → spawn altijd op land nabij 0,0; oceanen/bergen/deep dark aanwezig |
| 10 | `blocks` | Dood gras/verdorde aarde, dood hout-set (log, wood, planks + houtset). Placeholder-textures (getinte vanilla). | B, D; creative tab, loot, tags (log, planks) kloppen |
| 11 | `worldgen` | Overgangsbiome 1→2 `withered_expanse` (bladloze bossen, vaal gras, zandsteenranden). | B, D, W: zone van ~128 blokken rond grens 1 |
| 12 | `worldgen` | Horror-features ring 1: kale plekken met dode bomen (vaker richting de rand) + onnatuurlijke 2×1-tunnels. | B, D, W: `/place feature` + in het wild gevonden |
| 13 | `atmosphere` | Stille zones in grotten (ambient/mood per biome) + valere kleuren aan de rand van ring 1. | B, runClient: hoorbaar/zichtbaar verschil |
| 14 | `worldgen` | Nether-ring 1: kopieën van nether wastes, crimson forest, warped forest + vanilla Nether-terrein als ring 1-invoer. | B, D, W (Nether) |
| 15 | `blocks` | Zielkristal-erts (+ loot, tags, placeholder-texture). | B, D |
| 16 | `worldgen` | Plaatsing zielkristal in de crimson-bodem en netherrack van Nether-ring 1. | B, W: vindbaar, alleen in Nether-ring 1 |
| 17 | `textures` | Definitieve textures ring 1 (dood gras/aarde, dood hout, zielkristal). | runClient: visuele check |

### C. Systemen (vóór ring 2)
| # | Categorie | Change | Test |
|---|---|---|---|
| 18 | `infra` | Netwerk-basis: payload-registratie (`RegisterPayloadHandlersEvent`), versiebeheer, helper om naar spelers in een ring te sturen; ring-snapshot uit change 1 naar de client bij het joinen. | B, S: client kent na joinen dezelfde grenzen als de server |
| 19 | `conditions` | Meter-systeem: player attachment (meters 0–100), tick-handler elke 20 ticks, drempel-effecten uit config, sync bij verandering, HUD-balk (client). Test-conditie via debugcommando. | B, S: meter loopt op/af, HUD alleen client, twee spelers zien elk hun eigen meter |
| 20 | `atmosphere` | Ringweer-systeem: `SavedData` per dimensie met weerstaat per ring, cyclus uit config, sync, client-hook voor rendering (nog geen weertypes). | B, S: `/ca weather <ring> <type>` synct naar alle clients |

### D. Ring 2 (+ Nether-ring 2)
| # | Categorie | Change | Test |
|---|---|---|---|
| 21 | `blocks` | Kloofsteen-set + scorched sandstone-set + woestijnflora (gebarsten klei, dorre struik, tonnetjescactus, woestijnbloem). | B, D |
| 22 | `blocks` | Palm-set (houtset + sapling, zonder tree feature). | B, D |
| 23 | `worldgen` | Ring 2-terrein (ring 2-invoer `ring_blend`: canyons, vlaktes) + kernbiomes `canyonlands` en `oasis_flats` (surface rules, kleuren) + palm tree feature. | B, D, W |
| 24 | `blocks` | Zonnesteen-erts en turkoois-erts. | B, D |
| 25 | `worldgen` | Ores ring 2: zonnesteen, turkoois, afgebouwde vanilla-ores. | B, W: verdeling klopt per diepte |
| 26 | `worldgen` | Grotten ring 2: ondergrondse waterreservoirs + instortende zandgrotten. | B, W |
| 27 | `conditions` | Uitdroging (op/af-regels, waterfles, drempels). | B, S: zon/schaduw/nacht/oase/waterfles gedragen zich zoals beschreven |
| 28 | `atmosphere` | Ring 2-weer: zandstorm (fog, particles, wind-duw server-side) en hittegolf (trilling client, uitdroging ×2). | B, S: effect op twee clients tegelijk gelijk |
| 29 | `blocks` | Zout-set. | B, D |
| 30 | `worldgen` | Extra biomes: `salt_flats`, `hoodoo_fields`, `dune_sea` (terreinvormen + surface). | B, D, W |
| 31 | `worldgen` | Overgangsbiome 2→3 `dry_riverbeds`. | B, D, W |
| 32 | `atmosphere` | Ring 2-sfeer: lucht/fog-kleuren, zingend zand (ambient). | runClient |
| 33 | `blocks` | Nether-2-blokken: reuzenzwam-set, doorweekt netherrack, sporenmos (spreiding), slijmstalactieten, lichtgevende schimmel, zwamlamp. | B, D, S (random ticks server-side) |
| 34 | `blocks` | Dauwbron + dauwkristal (groei aan dauwbron, zoals amethist). | B, D, S |
| 35 | `worldgen` | Nether-2-terrein (zwamkathedralen) + biomes reuzenzwamwoud en sporengrotten + dauwbron-features. | B, D, W (Nether) |
| 36 | `atmosphere` | Nether-2-sfeer: grijsgroene fog, druppelgeluiden, sporen-particles (biome-data). | runClient |
| 37 | `textures` | Definitieve textures ring 2 + Nether-ring 2. | runClient |
