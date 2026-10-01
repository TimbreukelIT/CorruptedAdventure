# Fase 1 — Wereld

Uitwerking van de sparringsessie(s) voor fase 1. Bron van waarheid voor *wat* we in fase 1 bouwen.
Beslissingen staan ook (kort) in `02-beslissingenlog.md`.

**Status:** sparring afgerond. Sessie 1: fundament, ring 1 en ring 2. Sessie 2: ring 3, 4 en 5 (alle met Nether-tegenpolen). Bouwplan compleet (changes 0–96, §3). Volgende stap: change 0 (startsein van de gebruiker).

---

## 1. Fundament

### 1.1 Bouwvolgorde
1. **Skelet:** world preset (overworld + Nether), `ringAt`, ring-BiomeSource, één placeholder-biome per ring, `ring_blend` density function.
2. **Ring voor ring** (1 → 5), telkens met de Nether-tegenpool: terrein → blokken → ores/grotten → sfeer/weer → condities → definitieve textures.

### 1.2 Terrein-architectuur
- Er is één noise settings per dimensie. Terrein per ring komt uit een eigen density function **`corruptedadventure:ring_blend`** (Java-type + codec).
- `ring_blend` krijgt per ring een eigen density function (JSON, in `worldgen/density_function/<ring>/…`) en kiest op basis van `ringAt` welke geldt. In de overgangszone mengt hij met een **smoothstep** (3t² − 2t³) op het lineaire gewicht tussen de twee aangrenzende ringen: geen knik aan de randen van de zone (zie §1.15).
- Alle overige terreinlogica (vorm, canyons, vulkanen) blijft data-driven in JSON.
- Client/server: puur worldgen, deterministisch per seed; geen sync nodig.

### 1.3 Ringgrenzen en overgangszones
- Grenzen uit config (zie `00-overzicht.md`), vervormd met seeded ruis van **±96 blokken**. Vorm (change 2): twee lagen simplex-ruis, golven van ~600 en rimpels van ~150 blokken (75/25), gesampled in overworld-coördinaten zodat de Nether exact meeloopt.
- Overgangszone van **~128 blokken** rond elke grens (Nether: ÷4, dus ~32 blokken en ±24 ruis).
- In de overgangszone: blend van terrein en een eigen overgangsbiome per grens (uit te werken per ring). Tot een grens zijn overgangsbiome heeft, wisselt de biome **hard op gewicht 0,5** (precies op de verruisde grens); overgangsbiomes komen later via een optioneel veld van de ring-BiomeSource (zie §1.14).
- **Config → snapshot per wereld:** de config geeft de standaardwaarden. Bij het aanmaken van een wereld worden ze in die wereld vastgelegd; daarna gebruikt `ringAt` alleen die snapshot. Een aangepaste config geldt alleen voor nieuwe werelden. De server synct de waarden naar de client (voor HUD en muren).
- Snapshot (gebouwd in change 1): `<wereld>/corruptedadventure/ring_settings.json`, gelezen of aangemaakt bij `ServerAboutToStartEvent` (die op dedicated én integrated server vóór `loadLevel()` komt, dus vóór de spawn-chunks). Geen `SavedData`, want die bestaat pas als de level geladen is. Een bestaand snapshot gaat altijd vóór de config. Ongeldige config of corrupt snapshot → de server start niet (duidelijke foutmelding), nooit stilletjes terugvallen.

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

**Uitwerking (change 8):**
- **52 kopieën** (`corruptedadventure:<vanilla-pad>`): alle biomes uit vanilla's overworld-klimaatlijst behalve mushroom fields, inclusief cherry grove, mangrove swamp, lush/dripstone caves en deep dark. Datagen draait vanilla's eigen `BiomeData.bootstrap` en houdt alleen deze biomes over, onder onze namespace. `placeholder_ring_1` is daarmee weg.
- **Verdeling:** eigen biome-source `corruptedadventure:vanilla_climate` (`preset: minecraft:overworld`) als ring 1-source van `corruptedadventure:ring`. Die zet vanilla's parameterlijst bij het laden om naar onze kopieën; omdat de klimaatruis vanilla is, heeft ring 1 op elke x,y,z de kopie van het biome dat een vanilla-wereld met dezelfde seed daar heeft. Herbruikbaar voor Nether-ring 1 (`preset: minecraft:nether`, change 14).
- **Mushroom fields → `corruptedadventure:plains`** (veld `replace`): een gewoon grasland-eiland, met gewone mob-spawns.
- **Tags:** de kopieën komen in dezelfde vanilla-tags als hun origineel (klasse-tags, gedrag-tags, `has_structure/*`), via datagen naar de vanilla-tagpaden (`replace: false`). Alle vanilla overworld-structures dus, **inclusief trial chambers** (niet in deep dark). **Nooit:** `is_overworld` (vanilla's `has_structure/stronghold` = `#is_overworld`), `has_structure/stronghold`, `stronghold_biased_to`, `has_structure/ancient_city`.
- **Villager-types:** NeoForge-data map `villager_types` voor de kopieën, gelijk aan vanilla (woestijn, jungle, savanne, sneeuw, moeras, taiga).
- **Wolf-varianten:** de vanilla-varianten met een vaste biome (pale, snowy, black, ashen, woods, chestnut) worden overschreven met vanilla-biome + kopie; de tag-varianten (spotted, rusty, striped) werken via de gespiegelde tags.
- **Surface rules:** de vanilla surface rules in `noise_settings/overworld.json` verwijzen naar de kopieën in plaats van de `minecraft:`-biomes; de mycelium-tak is weg.
- **Bekend tot change 9:** het terrein is nog placeholder (vlak), het klimaat al vanilla: oceaanbiomes op land, pieken zonder bergen en cave-biomes op vanilla-diepte zijn verwacht.

### 1.8 Buitenrand
- 9.500–10.000: het terrein van ring 5 brokkelt af tot **zwevende eilanden** en daarna lege **void** (via `ring_blend`: een aparte 'void'-invoer).
- **World border op 10.000** (Nether: 2.500). Server-side, alleen in werelden met de `corrupted_world`-preset (herkend aan de chunk-generator `ring_noise` van de overworld); andere presets blijven vanilla.
- Uitwerking (change 7): de overworld-border (midden 0,0, diameter 2 × `voidOuterRadius`) wordt alleen gezet zolang hij nog op de vanilla-standaard staat (grootte én midden 0,0, geen lerp) — in de praktijk bij het aanmaken; een door een admin gewijzigde border (ook alleen het midden) blijft staan. Bekende, geaccepteerde beperking: wie bewust terugzet naar de vanilla-standaard, krijgt na een herstart weer 10.000. Schade en waarschuwing: vanilla-standaard.
- Vanilla schaalt de Nether-border **niet** (alleen het midden; de grootte is 1-op-1 die van de overworld, en de client krijgt altijd de overworld-border). Daarom een echte Nether-border: bij elke start afgeleid als overworld-grootte ÷ `coordinate_scale` (dus 2.500 bij 10.000), en die volgt elke latere wijziging (`/worldborder set/add/center`) mee. Spelers in de Nether krijgen hun border via vanilla-packets na login, dimensiewissel en respawn en bij elke groottewijziging; geen mixins.
- Vorm (change 6): zwevende eilanden (overworld Y ~50–110, Nether Y ~30–90) die van de grens van ring 5 tot ~9.900 dunner en zeldzamer worden; de laatste ~100 blokken vóór de border zijn leeg. Nether ÷4. Afstemming op het echte ring 5-terrein volgt in change 89.
- **Geen water of lava in de void:** vanaf het begin van de overgangszone ring 5 → void geeft de wereld geen zee-vloeistof meer (eigen chunk-generator, zie §1.15).

### 1.9 Omgevingscondities (systeem)
- Per conditie (hitte, gif, void, …) een **meter 0–100 per speler**, opgeslagen in een player attachment.
- Een server-side tick-handler (elke 20 ticks) bepaalt via `ringAt` + biome (+ ringweer) of de meter oploopt of afneemt.
- **Drempels** geven oplopende effecten (bijv. 50: traagheid, 75: zwakte, 100: schade). Sterktes en snelheden in config.
- **HUD-balk** op de client toont de meter; waarde wordt gesynct (alleen bij verandering). HUD-code in `client/`.
- Tegenmaatregelen (gear/items) komen in fase 2 en remmen de opbouw; in fase 1 alleen de conditie zelf.
- Ring 1 heeft geen conditie; het systeem verschijnt voor het eerst in ring 2.
- Nether-condities alleen in **Nether-3** (kou/bevriezing) en **Nether-5** (stilte); de andere Nether-ringen hebben er geen. **Nether-4 is zuiverend**: daar zakken alle meters extra snel.
- Overzicht condities: ring 2 uitdroging · ring 3 hitte · ring 4 gif · ring 5 void/glitches · Nether-3 kou · Nether-5 stilte.
- Sommige drempels sturen **client-effecten** aan (glitch-visuals, volume en HUD bij stilte). De client leest daarvoor alleen de gesyncte meter en beslist zelf niets.

### 1.10 Biome-varianten
- Ring 1: kopieën van vanilla biomes (zie §1.7).
- Ring 2–5: **4–5 varianten per ring** + **1 overgangsbiome per grens**. Nether-ringen: aantal per ring in de ringsecties.
- Bouwvolgorde per ring: eerst 2–3 kernvarianten, de overige als latere changes binnen dezelfde ring.

### 1.11 Ores (algemeen)
- Ring 1 heeft alle vanilla-ores.
- Naar buiten toe **bouwen vanilla-ores af**: ijzer en kolen schaarser, diamant bijna afwezig. Ringores nemen het over.
- Netherite/ancient debris blijft zoals vanilla. Ring-gear is per ring een tier hoger (fase 2), zodat netherite rond ring 3 al achterloopt.

### 1.12 Nether-tegenpolen (schema)
| Ring | Overworld | Nether-tegenpool | Sleutelmateriaal (tegengif voor) |
|---|---|---|---|
| 1 | Vertrouwd vanilla | Vanilla-Nether (kopieën: wastes, crimson, warped) | Zielkristal (bewaker 1→2) |
| 2 | Droog, canyons, oases | Doorweekte schimmel-Nether | Dauwkristal (hitte, ring 3) |
| 3 | Vulkanisch, water gestolen | Bevroren Nether (ijs, gestolde lava) | Oerijs (gif, ring 4) |
| 4 | Moeras, gif | Droge kristal-/bottenwoestijn (zuiverend) | Hemelglas (void/glitches, ring 5) |
| 5 | Corrupted, void | 'Zuivere' witte Nether, onheilspellend stil | Witte kern (Herobrine, kastelen, final) |

Sleutelmaterialen uit Nether-ring N zijn thematisch een tegengif voor ring N+1.

### 1.13 Debugcommando's (change 3)
- `/ca …` alleen voor OP's (permissieniveau 2), vaste Engelse tekst (geen lang-keys).
- `/ca ring`: ring (bij overgang: beide ringen en het gewicht), afstand tot 0,0 (in de Nether ook het overworld-equivalent) en de afstand tot de dichtstbijzijnde grens **inclusief ruis, met teken** (+ buiten, − binnen). Afstanden in blokken van de huidige dimensie.
- `/ca tpring <1–6> [nether]` (6 = void-rand): naar het radiale midden van de nominale ring (750 / 2.500 / 4.500 / 6.500 / 8.500 / 9.750; Nether ÷4), **in je huidige richting vanaf 0,0** (op 0,0 zelf: oost). Zonder `nether` gaat het naar de overworld.
- `/ca tpboundary <1–5> [nether]`: naar de echte, verruisde grens (gewicht 0,5) tussen ring n en n+1, in dezelfde richting. Bedoeld voor test W.
- `/ca border` (change 7): grootte, straal en midden van de border in de huidige dimensie (plus doel en resttijd als hij beweegt). Nodig omdat vanilla `/worldborder get` ook in de Nether de overworld-border toont. Kleine uitzondering op de categorie: gebouwd in change 7 (`worldgen`).
- Landingshoogte: overworld bovenste blok (heightmap); Nether de hoogste plek met 2 blokken lucht op een stevige vloer, onder het dak. Geen veilige plek (void, lava)? Dan toch teleporteren naar een vaste Y (overworld 100, Nether 64), met een waarschuwing in de chat.

### 1.14 Ring-BiomeSource en placeholder-biomes (change 5)
- `corruptedadventure:ring` kiest per positie de ring (via `ringAt`) en geeft de keuze door aan een **geneste biome-source per ring**: nu `minecraft:fixed` met een placeholder, later bijv. `minecraft:multi_noise` voor de varianten binnen een ring (ring 1: `corruptedadventure:vanilla_climate` sinds change 8, zie §1.7).
- **Placeholders (tijdelijk):** `placeholder_ring_1…5` (overworld) en `placeholder_nether_ring_1…5` (Nether). Alleen een **eigen kleur** (lucht, fog, water, gras, blad; Nether vooral fog) en **eigen weer** (neerslag en temperatuur: ring 1 en 4 met regen, ring 2, 3, 5 en de Nether droog). **Geen features, geen mob-spawns**, geen `has_structure/*`-tags. Ze verdwijnen zodra de echte biomes van een ring er zijn.
- **Void (blijvend):** `void_edge` (overworld) en `nether_void_edge` (Nether), voorbij ring 5; aparte biomes zodat sfeer later per dimensie kan.
- Overgangszone: harde wissel op gewicht 0,5 tot er overgangsbiomes zijn (§1.3).

### 1.15 `ring_blend` en placeholder-terrein (change 6)
- **Chunk-generator `corruptedadventure:ring_noise`** (overworld en Nether): vanilla noise-generatie zonder vloeistof in de void-zone. Vanilla vult alle open ruimte onder zeeniveau met water (overworld) of lava (Nether) en zet daarnaast lokale aquifer-vloeistof (onder Y -10 lava, bijv. onder zwevende eilanden); `ring_noise` doet geen van beide vanaf de zone ring 5 → void (vloeistof-kiezer + aquifer-wrapper). Later ook bruikbaar voor ringen met een eigen vloeistofregel.
- **Mengcurve:** smoothstep op het lineaire gewicht van `ringAt`.
- **Placeholder-terrein (tijdelijk; elke ring vervangt het in zijn eigen terreinchange):**

| Ring | Overworld: oppervlak | Nether: vloer (dak vanaf Y ~110) |
|---|---|---|
| 1 | vanilla (change 9) | Y ~30 (lavazee op 32 zichtbaar) |
| 2 | Y ~95 | Y ~40 |
| 3 | Y ~120 | Y ~55 |
| 4 | Y ~64 (rond zeeniveau) | Y ~45 |
| 5 | Y ~85 | Y ~60 |

  Overworld ±8 blokken ruis; Nether-vloer ±8, dak ±4. Grootste sprong (ring 3 → 4, 56 blokken) over een zone van 128 blokken: helling < 1:2, geen kliffen.
- **Void-invoer:** zie §1.8. Aan de void-kant zakt het terrein van ring 5 in de overgangszone weg tot een afbrokkelende rand; midden in de zone valt het naar de void.

---

## 2. Ringen

### Ring 1 — Vanilla-ring (0 – 1.500)

**Terrein en sfeer**
- Vanilla-terrein (vanilla noise router als ring 1-invoer van `ring_blend`). Oceanen, rivieren, hoge bergen en pieken blijven.
- Rond de spawn (~300 blokken) wordt de continentalness omhoog geschoven: altijd land in het midden.
- **Uitwerking (change 9):**
  - Keten: de continentalness-afhankelijke vanilla-density-functions (`continents`, `offset`, `factor`, `jaggedness`, `depth`, `sloped_cheese`, initiële dichtheid) zijn gekopieerd naar `corruptedadventure:ring_1/overworld/…`; ring 1 = vanilla `final_density` op die keten. Grotten, erosion, ridges en noises blijven vanilla. De router-velden `continents` en `depth` wijzen naar de ring 1-versies, zodat biomes, spawn-finder en terrein dezelfde continentalness zien. `minecraft:overworld/…` wordt nooit overschreven (zou vanilla-werelden raken).
  - Boost: `c' = c + w · max(0, 0,03 − c)`, met `w` = `radial_falloff` (1 tot 300 blokken van 0,0, smoothstep naar 0 bij 450). Land dat al hoger ligt blijft ongemoeid; binnen 300 blokken minstens "mid-inland", daarbuiten glijdend naar vanilla. Straal en ondergrens staan in de JSON, niet in de config (geen ringgrens; wijzigen zou naden in bestaande werelden geven).
  - Rivieren blijven toegestaan in de landzone (weirdness/ridges ongemoeid). Op de ondergrens ligt het land laag (rond zeeniveau): ook ondiepe plassen in bos- en vlaktebiomes komen voor.
  - Spawn: `spawn_target` = één punt met alle klimaatparameters `[-2, 2]`; daardoor wint 0,0 altijd en kiest vanilla's 11×11-chunkspiraal de eerste droge, vaste plek rond 0,0. Geen eigen spawn-code (fallback via `LevelEvent.CreateSpawnPosition` alleen als dat nodig blijkt).
  - Initiële dichtheid: aparte `ring_blend/overworld_initial` (ring 1 = vanilla's eigen `initial_density_without_jaggedness`, overige ringen `0,390625 + terrein`).
  - Performance: zie `CODEBASE.md` (valkuil density functions). Beslisregel: noise-stap ≤ 1,3× vanilla → geen ring gate; gemeten 1,17× (ring 1) en 0,92× (ring 3), dus de ring gate volgt bij de eerste zware ring 2-invoer (change 23).
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

**Terrein en sfeer**
- Zwart-grijs aslandschap met vulkaankegels, kraters, lavameren en gestolde lavastromen. Lucht rood-oranje, fog dicht en asgrijs.
- Eigen ring 3-invoer voor `ring_blend`, met per biome-regio een eigen vorm (kegels, stromen, zeekom).
- Het gestolen water is overal zichtbaar: geen natuurlijk water, een lege zeekom, en geplaatst water verdampt bovengronds.

**Biomes (5)**
| Biome (werktitel) | Karakter |
|---|---|
| `ashen_wastes` | Grijze asvlakte met verkoolde bomen en aslagen — kernbiome |
| `volcanic_peaks` | Vulkaankegels met kraters en lavastromen; heetste plek — kernbiome |
| `dried_seabed` | Lege zeekom: diepe kom met dode koraalbanken (vanilla dead coral), het gestolen water |
| `basalt_flows` | Gestolde lavastromen met gloeiende scheuren (smeulsteen) |
| `sulfur_fields` | Gele zwavelkorst en fumarolen |

- Overgangsbiome 3→4: **stoomvenen** (`steam_fens`): hete modderpoelen, geisers en stoomkolommen. Hier komt het gestolen water terug en kookt het tegen de hitte. Stoom is biome-particles (data).

**Unieke blokken (placeholders eerst)**
- Basis is vanilla basalt / blackstone / magma. Eigen blokken:
- **As**: asblok + aslaag (gedraagt zich als sneeuwlaag).
- **Puimsteen-set**: puimsteen, gepolijst, bricks, slab, stairs, wall.
- **Smeulsteen**: gloeiende scheuren, geeft licht, hitte ↑ in de buurt.
- **Verkoold hout-set**: log, wood, planks + houtset.
- **Zwavelkorst + fumarole**: fumarole geeft stoompuffen (client-particles via `animateTick`), hitte ↑ erboven.
- **Stoomvenen**: kokende modder (vertraagt, hitte ↑) en geiser (duwt periodiek omhoog, server-side).

**Ores**
- **Vulkaniet** (hoofdmateriaal, kandidaat ring 3-gear-tier): diep in basalt nabij lava. Risico is beloning.
- **Zwavel**: veel en ondiep bij fumarolen en in `sulfur_fields`; nut in fase 2 (buskruit, vuur, brandstof).
- **Vuurrobijn**: zeldzame edelsteen in de wanden van magmakamers.
- Vanilla-ores verder afgebouwd dan in ring 2: ijzer en kolen schaars, diamant bijna afwezig.

**Grotten**
- **Lavabuizen**: lange, gladde basalttunnels, soms met een lavarivier; bruikbaar als reisroute.
- **Magmakamers**: grote holtes boven een lavameer, met vulkaniet en vuurrobijn in de wanden.

**Water: verdampt bovengronds**
- Geplaatst water (emmer, dispenser, gebroken/smeltend ijs) verdampt direct **op plekken waar de lucht zichtbaar is**, net als in de Nether. Ondergronds en onder een dak werkt water normaal.
- Server-side check met `ringAt` + lucht zichtbaar bij plaatsing.

**Weer (ringweer)**
- **Asregen**: zicht omlaag, as-particles (client); hitte ×1,5 (server).
- **Uitbarsting**: rode lucht, gerommel (client); de server laat **lavabommen** (vanilla small fireballs) uit de lucht vallen, die vuur zetten (volgt de `mobGriefing`-gamerule) maar niet ontploffen; hitte ×2.

**Conditie: hitte**
- Meter ↑ bovengronds overdag (langzaam); sneller binnen ~4 blokken van lava/magma/smeulsteen/fumarolen/kokende modder, in `volcanic_peaks` en tijdens ringweer (×1,5 / ×2).
- Meter ↓ 's nachts, ondergronds (lucht niet zichtbaar en niet bij lava) en in water. Water werkt alleen ondergronds, dus **grotten zijn de schuilplaats**.
- **Fire resistance halveert** de opbouw (maakt niet immuun).
- Drempels (config): traagheid → zwakte → in brand vliegen.
- Tegenmaatregel in fase 2: hittebescherming op basis van dauwkristal (Nether-2).

**Horror-elementen (fase 1)**
- **As-afgietsels**: Pompeii-achtige figuren van asblokken in menselijke houdingen (kruipend, knielend). Deterministische feature.
- **Voetsporen in de as**: een spoor van indrukken in aslagen dat abrupt stopt of in een krater eindigt. Feature.
- **Gerommel en trillingen**: onregelmatig diep gerommel (biome-ambient) en een lichte camera-shake; client-only, per speler willekeurig en bewust niet gesynct.

**Grensmuur 3→4** (fase 5): zwarte aswolk met vonken.

**Nether-tegenpool — Nether-ring 3 (875 – 1.375): bevroren Nether**
- **Terrein**: de lavazee is gestold tot een enorme vlakte van ijs en koudlava, met scheuren waarin nog gloeiende lava ligt (zeldzame warme plekken). Daarnaast gletsjermassa's met smalle kloven en tunnels.
- **Biomes (3)**: `frozen_lava_sea` (hoofdbiome: open vlakte, ijspilaren van vloer tot plafond), `rime_forest` (verstijfde reuzenzwammen onder rijp), `glacier_rifts` (ijsmassa's met kloven en tunnels). De biome-schaal is kleiner dan in de overworld (biomes wisselen vaker). **Ruimte-check** in de W-test: voelt het krap, dan verbreden we ring 3 via de config (alleen voor nieuwe werelden).
- **Blokken**: koudlava-set (gestolde lava: blok, gepolijst, bricks, slab, stairs, wall), Nether-ijs (smelt niet, glad; plus verpakte variant), rijp-netherrack, rijpzwam-set (steel, hoed, planks + houtset), ijsstalactieten (dripstone-gedrag, vallen), gloeiende scheur (warmtebron). Vanilla packed/blue ice waar het kan.
- **Sleutelmateriaal: oerijs**, ijs dat bevroor vóór de corruptie (zuiver water). Het zit als kern in ijspilaren en is alleen met silk touch te winnen (anders breekt het). Nodig voor bewaker 3→4; in fase 2 smelt je het tot zuiver water, de basis van gifbescherming voor ring 4.
- **Sfeer**: blauwwitte fog, zwevende ijskristallen (particles), gierende wind en krakend ijs (ambient). Alles biome-data; geen ringweer in de Nether.
- **Conditie: kou/bevriezing**
  - Meter ↑ overal in Nether-3, sneller op ijs en in de open vlakte.
  - Meter ↓ bij warmte: lava, vuur, kampvuur, magma, gloeiende scheur, smeulsteen (uit ring 3).
  - **Leren armor remt** de opbouw (vanilla-tag `freeze_immune_wearables`).
  - Drempels: traagheid → mining fatigue → vanilla-bevriezing (`ticksFrozen`: frost-overlay en schade, door vanilla al gesynct).

**Client/server-risico's**
- Water verdampen, geiser-duw, lavabommen en de hitte- en kou-meters: allemaal server-side. De lavabommen zijn gewone server-entities, dus alle clients zien dezelfde.
- Camera-shake, gerommel, as- en stoomparticles, uitbarstingslucht: alleen client (`client/` of biome-data). Fumarole-particles via `animateTick` (draait alleen op de client-level, geen client-class nodig).
- `ticksFrozen` is vanilla entity-data en wordt al gesynct. Onze meter stuurt de waarde server-side aan.
- Water-check: alle plaatsingsroutes afvangen (emmer, dispenser, ijs), anders ontstaan er gaten in de regel.

### Ring 4 — Moerasring (5.500 – 7.500)

**Terrein en sfeer**
- Laag, vlak en nat: het gestolen water is hier bedorven. Groengrijze lucht, mistig, gifgroen water (biome-waterkleur).
- Bovengronds beperken riet en wortels het zicht. Onder water zie je niet wat er zwemt.
- Eigen ring 4-invoer voor `ring_blend`: laag terrein rond zeeniveau, diepe poelen, zinkgaten in het veen.

**Biomes (5)**
| Biome (werktitel) | Karakter |
|---|---|
| `toxic_bog` | Gifgroen water, borrelende poelen, gifzwammen; de lucht zelf is giftig — kernbiome |
| `root_forest` | Reuzenwortels boven het water, hangend moerasmos, moerascipressen — kernbiome |
| `drowned_forest` | Dode bomen in diep, donker water |
| `peat_moors` | Veenvlakte met zinkgaten en dwaallichten |
| `reed_marsh` | Rietvelden hoger dan de speler: je ziet niets, je hoort iets bewegen |

- Overgangsbiome 4→5: **rottend moeras** (`blighted_mire`). Het moeras sterft af: grijs verkleurd, stilstaand zwart water, verdorde planten, en de eerste glitchplekken (vierkante gaten, verkeerde blokken) als kleine feature.

**Unieke blokken (placeholders eerst)**
- **Veen**: brandbaar, bruikbaar als brandstof; oppervlak van `peat_moors`.
- **Zuigmodder**: je zakt erin weg zoals in poedersneeuw (server-side).
- **Riet**: 2–3 blokken hoog, blokkeert het zicht.
- **Moerascipres-set**: log, wood, planks, leaves, sapling + houtset; eigen tree feature.
- **Hangend moerasmos**, **gifzwam** (zwak licht, gif ↑ in de buurt).
- **Moerasgas**: onzichtbaar gasblok (zie Grotten).
- **Bellenbron**: bodemblok dat bellen en geborrel geeft (particles via `animateTick`, zonder stroming).
- Vanilla mud en mangrove roots waar het kan.

**Ores**
- **Veenijzer** (werktitel; hoofdmateriaal, kandidaat ring 4-gear-tier): ondiep in veen en op de bodem van giftige poelen. Je moet dus duiken in gifwater.
- **Gifkristal**: groeit in vochtige grotholtes en wortelgrotten; fase 2 voor gifwapens en -pijlen.
- **Jade**: zeldzame edelsteen, diep in overstroomde grotten.
- Vanilla-ores verder afgebouwd dan in ring 3.

**Grotten**
- **Overstroomde grotten**: grote aquifers vol gifwater met luchtkamers; je moet duiken.
- **Wortelgrotten**: tunnels langs reuzenwortels, met hangende wortels en gifkristal.
- **Moerasgaskamers**: holtes gevuld met het moerasgasblok.
  - Het gas is onzichtbaar en niet-solide (vervangbaar, zoals lucht), met subtiele gele particles. Oplettende spelers kunnen het zien.
  - Vuur, fakkel of lava ernaast geeft een **ketting-ontploffing** door het verbonden gas. Er is een cap op het aantal blokken per tick (geen server-lag), en blokschade volgt `mobGriefing`.
  - Hoofd in gas: gif ↑.
  - Alles via server-side block-updates, zonder zone-check.

**Water**
- Gewoon vanilla water met gifgroene kleur; emmers, farms en waterliften werken. De gif-regel zit in de conditie.

**Weer (ringweer)**
- **Dikke mist**: zicht sterk omlaag (client-fog), geen gif. De horror zit in wat je niet ziet.
- **Gifregen**: groene regen (eigen client-rendering); gif ↑ onder open lucht (server).

**Conditie: gif**
- Meter ↑ snel in water in ring 4; langzaam in de lucht van `toxic_bog`; bij gifzwammen, borrelende poelen en in moerasgas; tijdens gifregen onder open lucht.
- Meter ↓ op droog land buiten `toxic_bog`. Noodmiddelen: melk −40, honingfles −20 (vanilla-items, zoals de waterfles in ring 2).
- Drempels (config): misselijkheid → vanilla-gif (doodt niet) → wither (kan doden).
- Tegenmaatregel in fase 2: gifbescherming op basis van oerijs (Nether-3).

**Horror-elementen (fase 1)**
- **Dwaallichten**: zwevende lichtjes boven veen en water die van je wegdrijven (biome-particles).
- **Geritsel in het riet**: geritsel en stappen rondom je in `reed_marsh`, altijd net buiten zicht. Client-only ambient, per speler willekeurig.
- **Bellen uit de diepte**: grote bellenstromen en geborrel uit diepe poelen (bellenbron-feature + biome-ambient); er ligt iets groots onder.

**Grensmuur 4→5** (fase 5): groene gifdamp die overgaat in glitchruis.

**Nether-tegenpool — Nether-ring 4 (1.375 – 1.875): droge kristal-/bottenwoestijn**
- **Terrein**: de lavazee is diep weggezakt. Erboven liggen brede vlaktes van bottenzand tussen reusachtige fossiele ribbenkasten, kristalzuilen van vloer tot plafond, en gigantische holle geodes.
- **Biomes (3)**: `bone_dunes` (hoofdbiome: bottenzand, ribbenkasten), `crystal_spires` (kristalzuilen), `geode_hollows` (holle kristalgeodes waar je in kunt lopen). **Ruimte-check** in de W-test, zoals bij Nether-3.
- **Blokken**: bottenzand (valt zoals zand), gebleekt netherrack, fossielsteen-set (steen, gepolijst, bricks, slab, stairs, wall), zuiverkristal (blok, scherf/cluster, kristallamp). Ribbenkasten van vanilla bone block.
- **Sleutelmateriaal: hemelglas**, gesmolten uit bottenzand + zuiverkristal. Het maakt glitches zichtbaar (lens). **Fase 1 bouwt alleen de grondstoffen**; hemelglas-blok, recipe en lens-functie komen in fase 2 (`items`). Nodig voor bewaker 4→5; basis van bescherming tegen void en glitches in ring 5.
- **Zuiverend (mechaniek)**: geen eigen conditie, maar in Nether-4 zakken **alle conditie-meters** (gif, hitte, kou) extra snel. Wie vergiftigd uit ring 4 komt, knapt hier op. Regel in de condities-tick, server-side.
- **Sfeer**: bijna wit-beige fog met ver zicht, droge wind, zacht kristalgezoem, dwarrelende stofparticles (biome-data). Daarbovenop **pulserend kristallicht**: de fog-kleur pulseert zacht (client-code: fog-hook in `client/`).

**Client/server-risico's**
- Gif-meter, zuigmodder, gasontploffing (met cap per tick) en de zuiverende regel: server-side.
- Mist, gifregen-rendering, dwaallichten, geritsel en de pulserende fog: alleen client. De fog-hook leest de biome op de camerapositie en de tijd. Pulsen hoeven tussen spelers niet synchroon te lopen, dus er is geen sync nodig.
- Gas- en bellen-particles via `animateTick` (client-level, geen client-class).
- Ketting-ontploffing: nooit recursief in één tick. Plan de ontploffingen gespreid in (scheduled ticks), anders lagt of crasht een server bij grote kamers.

### Ring 5 — Corrupted ring (7.500 – 9.500)

**Terrein en sfeer**
- Grijze, dode wastezone waar de wereld zichtbaar "fout" is: blokkerig terrein, zwevende brokken, scheuren tot in de void, en landschappen die net niet kloppen.
- Lucht donkergrijs met een zweem violet, fog dik en kleurloos. Afwijkend fog-gedrag (glitch-fog) via client-code.
- Eigen ring 5-invoer voor `ring_blend`. Blokkerige glitchvormen vragen mogelijk een eigen density function-type (bijv. kwantiseren); alleen als JSON niet volstaat.
- Voorbij 9.500 brokkelt ring 5 af tot zwevende eilanden en void (§1.8).

**Biomes (5)**
| Biome (werktitel) | Karakter |
|---|---|
| `corrupted_wastes` | Grijze dode vlakte, corrupted gras/aarde, dode bomen — kernbiome |
| `glitch_fields` | Blokkerig terrein, zwevende kubussen, verschoven lagen, glitchblokken — kernbiome |
| `void_scars` | Kraters en scheuren **tot in de void**: geen bedrock, vallen is dood (items weg). Randen van voidsteen met void-particles, zodat ze goed zichtbaar zijn |
| `inverted_hills` | Heuvels die omgekeerd in de lucht hangen, met bomen ondersteboven |
| `false_meadow` | Een perfecte kopie van een vanilla-weide waar alles net fout is: geen geluid, geen mobs, gespiegelde bomen. "Thuis" dat niet klopt |

- Geen overgangsbiome naar buiten: de buitenrand (9.500–10.000) is de void-invoer van `ring_blend`.

**Unieke blokken (placeholders eerst)**
- **Corrupted varianten** van vanilla-natuurblokken: gras, aarde, steen, cobble, zand, log, wood, planks, leaves.
  - Elk heeft een vaste gezonde tegenhanger in een **data map** (`corrupted → vanilla`) en de tag `corruptedadventure:corrupted`.
  - Fase 5 voegt alleen het random-tick-gedrag toe (genezen/vervallen). In fase 1 hebben ze **geen gedrag**.
- **Glitchblok**: missing-texture-look, flikkert (geanimeerde texture). Dropt glitchscherven.
- **Voidsteen**: zwart, met void-particles (`animateTick`); randen van void-scheuren.

**Ores en materialen**
- **Nulliet** (werktitel; hoofdmateriaal, kandidaat ring 5-gear-tier): in de wanden van void-scheuren en diep in de ring. Het risico zit aan de rand van de afgrond.
- **Glitchscherven**: droppen uit glitchblokken (loot table), dus je oogst de glitches zelf.
- Vanilla-ores vrijwel afwezig.

**Grotten**
- **Onmogelijke gangen**: rechte, te perfecte 2×1- en 3×3-tunnels in rasterpatronen, doodlopend of in lussen. Het groot geworden broertje van de ring 1-tunnels.
- **Void-grotten**: grotten met gaten in de bodem naar de void.

**Weer (ringweer)**
- **Glitchstorm**: beeldruis-overlay, HUD flikkert, geluiden haperen (client); void ↑ ×2 (server).
- **Omgekeerde regen**: zwarte druppels die omhoog vallen (client); void ↑ ×1,5 (server).

**Conditie: void/glitches**
- Meter ↑ overal in ring 5 (langzaam); sneller bij void-scheuren, glitchblokken, 's nachts en tijdens ringweer.
- Meter ↓ in fel licht: lichtniveau ≥ 12 van eigen lichtbronnen, want "licht houdt de void weg". Daalt ook buiten ring 5, en snel in Nether-4.
- Drempels (config):
  1. Glitch-visuals: client, gestuurd door de gesyncte meter.
  2. Korte teleports van 1–5 blokken: server, **alleen naar een veilige plek, nooit richting void**.
  3. Darkness + void-schade.
- Tegenmaatregel in fase 2: bescherming op basis van hemelglas (Nether-4).

**Horror-elementen (fase 1)**
- **Ontbrekende chunks**: hele 16×16-kolommen die ontbreken tot in de void, precies op het chunkraster. Deterministische feature.
- **Herobrine-tekens**: bordjes met korte teksten ("ga terug", "hij ziet je"; vertaalbare keys) en redstone-fakkels in patronen. Feature.
- **Echo-voetstappen**: je eigen voetstappen klinken even later nog eens, alsof iemand je volgt. Client-only, per speler.

**Grensmuur**: geen uitgaande grens; de buitenrand is void + world border.

**Nether-tegenpool — Nether-ring 5 (1.875 – 2.375): 'zuivere' witte, stille Nether**
- **Terrein**: na alle chaos is dit de engste plek, juist omdat alles schoon, wit, geordend en doodstil is. Gladde witte vlaktes met perfecte zuilen van vloer tot plafond, in rasters. Waar de lavazee lag, ligt een glad, glanzend en begaanbaar oppervlak van spiegelsteen, eindeloos en leeg.
- **Biome (1)**: `white_halls`. Het kale en eentonige is juist het punt.
- **Blokken**: kalkrack (wit netherrack, anders dan het beige gebleekte netherrack van Nether-4), witsteen-set (glad, gepolijst, bricks, zuil, slab, stairs, wall), spiegelsteen, stil licht (witte lichtbron zonder geluid of particles).
- **Sleutelmateriaal: witte kern**, het "hart van de stilte". Zeldzaam, in de kern van de zuilen. Nodig voor toegang tot Herobrines kastelen en de final battle (fase 3/5); in fase 2 de basis van zuiveringsgear tegen corruptie. Geen gedrag in fase 1.
- **Sfeer**: witte fog, geen ambient-geluid, geen muziek, bijna geen particles (biome-data).
- **Conditie: stilte**
  - Meter ↑ overal in Nether-5, sneller in de open vlaktes.
  - Meter ↓ door **zelf geluid te maken**: bel luiden, goat horn, note block of jukebox in de buurt (server-side via vanilla game events).
  - **Andere spelers binnen ~8 blokken remmen de opbouw**: samen sta je sterker.
  - Drempels: geluid gedempt → HUD weg (behalve hotbar) → alle geluid weg + darkness → schade (hartslag).
  - De server stuurt de meter; de client past alleen volume en HUD aan.

**Client/server-risico's**
- Void-meter, teleports (veilige-plek-check op de server, nooit in blokken of richting void) en de stilte-meter: server-side.
- Glitch-visuals, HUD-flikker, beeldruis, omgekeerde regen, glitch-fog, echo-voetstappen, volume-demping en HUD-verbergen: alleen client (`client/`), aangestuurd door gesyncte meters en ringweer.
- Stilte: geluid-detectie via server-side game events (`GameEvent`-listener), niet via client-geluid.
- Void-scheuren en ontbrekende chunks zonder bedrock: deterministisch per seed. Aandachtspunt: mob-spawning en item-verlies in de void zijn vanilla-gedrag.
- Glitchblok-flikkering: texture-animatie (`.mcmeta`), geen code.

---

## 3. Bouwplan

Elke change: één categorie, afgesloten met `/code-review`, `CODEBASE.md` bijgewerkt. Changes 0–37 uit sessie 1, 38–96 uit sessie 2.

**Standaardtests** (per change aangegeven):
- **B** — `./gradlew build` slaagt.
- **D** — `./gradlew runData` slaagt; gegenereerde output gecommit.
- **S** — `./gradlew runServer` (met `level-type=corruptedadventure:corrupted_world`) start zonder client-class-fouten; een client kan joinen.
- **W** — nieuwe wereld met vaste **testseed `20260930`** (preset `corrupted_world`); teleporteren naar het midden van elke ring en elke grens (overworld én Nether); portal-test vanuit elke ring.

### A. Skelet
| # | Categorie | Change | Test |
|---|---|---|---|
| 0 | `infra` | Package hernoemen `nl.corruptedadventure` → `nl.timtoys.corruptedadventure` (bronmappen, `mod_group_id`, `CODEBASE.md`). | B, S |
| 1 | `infra` | `ModConfig` met ringgrenzen (5 buitenradii, void-rand 9.500/10.000), grensruis ±96, overgangsbreedte 128. **Snapshot per wereld** vastgelegd vóór de eerste chunk (zie §1.3). Sync naar de client volgt in change 18 (de client heeft het pas nodig voor HUD en weer). | B, S; configbestand met standaardwaarden; snapshot-bestand in de wereldmap; config wijzigen → bestaande wereld houdt oude waarden |
| 2 | `worldgen` | `Ring` enum (`RING_1`…`RING_5`, `VOID`) + `RingLayout.ringAt(dimension, x, z)` met seeded grensruis, Nether-factor 4 en een overgangsgewicht (0–1) naar de buurring. | B; unit tests voor grenzen, Nether-schaal en determinisme per seed |
| 3 | `infra` | Debugcommando's: `/ca ring` (ring, afstand, overgangsgewicht op je positie) en `/ca tpring <n> [nether]`. Alleen voor OP's. | B, S; commando's werken op de server |
| 4 | `worldgen` | World preset `corrupted_world` + dimension type `corrupted_nether` (`coordinate_scale` 4, `ultrawarm`), voorlopig met vanilla terrein en vaste biome (overworld `minecraft:plains`, Nether `minecraft:nether_wastes`; de overworld houdt het vanilla dimension type). The End blijft technisch in de preset. Testseed: `20260930`. Menunaam "Corrupted World" / "Corrupte wereld"; de preset is niet de standaard. | B, S, W: preset kiesbaar; portals landen op ÷4. *("End niet bereikbaar" verschoven naar change 5: vaste plains zit in `has_structure/stronghold`, dus tot change 5 bestaan er nog strongholds.)* |
| 5 | `worldgen` | `RingBiomeSource` (+ codec) + één placeholder-biome per ring in overworld en Nether (datagen), plus een void-biome. Geen stronghold-tags. | B, D, S, W: F3 toont de juiste placeholder per ring; `/locate structure stronghold` vindt niets, End dus niet bereikbaar (test uit change 4) |
| 6 | `worldgen` | Density function `ring_blend` (+ codec) met per ring een placeholder-terrein (duidelijk verschillende hoogtes) en een void-invoer (zwevende eilanden → leegte, 9.500–10.000). Voor overworld en Nether. Plus chunk-generator `ring_noise`: geen water/lava in de void (§1.15). | B, S, W: hoogteverschil per ring zichtbaar, overgang zonder kliffen, void voorbij 9.500 |
| 7 | `worldgen` | World border op 10.000 bij wereldcreatie (Nether 2.500, afgeleid met ÷ `coordinate_scale`) + `/ca border`. | B, S: `/worldborder get` en `/ca border` in de overworld (20000), `/ca border` in de Nether (5000); `/worldborder get` toont in de Nether altijd de overworld-waarde |

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

### E. Ring 3 (+ Nether-ring 3)
| # | Categorie | Change | Test |
|---|---|---|---|
| 38 | `blocks` | As (blok + laag), puimsteen-set, smeulsteen (licht), verkoold hout-set. | B, D |
| 39 | `worldgen` | Ring 3-terrein (ring 3-invoer `ring_blend`: kegels, kraters, lavameren, geen natuurlijk water) + kernbiomes `ashen_wastes` en `volcanic_peaks` (surface rules, kleuren) + verkoolde-boom-feature. | B, D, W |
| 40 | `blocks` | Vulkaniet-, zwavel- en vuurrobijn-erts. | B, D |
| 41 | `worldgen` | Ores ring 3: vulkaniet (diep bij lava), zwavel (ondiep), vuurrobijn (magmakamers), vanilla-ores verder afgebouwd. | B, W: verdeling klopt per diepte |
| 42 | `worldgen` | Grotten ring 3: lavabuizen + magmakamers. | B, W |
| 43 | `conditions` | Hitte (op/af-regels, hittebronnen binnen ~4 blokken, fire resistance halveert, drempels). | B, S: dag/nacht/grot/water/lava gedragen zich zoals beschreven |
| 44 | `conditions` | Water verdampt bovengronds in ring 3 (emmer, dispenser, gebroken/smeltend ijs; lucht zichtbaar). | B, S: bovengronds verdampt, onder dak en ondergronds niet; buiten ring 3 normaal |
| 45 | `atmosphere` | Ring 3-weer: asregen (zicht, particles, hitte ×1,5) en uitbarsting (rode lucht, gerommel, lavabommen als vanilla small fireballs, hitte ×2). | B, S: twee clients zien dezelfde bommen; `mobGriefing false` → geen vuur |
| 46 | `blocks` | Zwavelkorst, fumarole (stoompuf), kokende modder (vertraagt), geiser (periodieke duw, server-side). | B, D, S |
| 47 | `worldgen` | Extra biomes: `dried_seabed`, `basalt_flows`, `sulfur_fields` (terreinvormen + surface + fumarole-features). | B, D, W |
| 48 | `worldgen` | Overgangsbiome 3→4 `steam_fens` (modderpoelen, geisers, stoom). | B, D, W |
| 49 | `worldgen` | Horror-features ring 3: as-afgietsels + voetsporen in de as. | B, D, W: `/place feature` + in het wild |
| 50 | `atmosphere` | Ring 3-sfeer: lucht/fog-kleuren, as- en stoomparticles, gerommel (ambient) + camera-shake (client). | runClient; S (geen client-class op server) |
| 51 | `blocks` | Nether-3-blokken: koudlava-set, Nether-ijs (+ verpakt), rijp-netherrack, rijpzwam-set, ijsstalactieten, gloeiende scheur. | B, D, S (vallende stalactieten server-side) |
| 52 | `blocks` | Oerijs (alleen met silk touch; anders breekt het zonder drop). | B, D |
| 53 | `worldgen` | Nether-3-terrein (bevroren lavazee met gloeiende scheuren, ijspilaren met oerijs-kern) + biomes `frozen_lava_sea` en `rime_forest`. | B, D, W (Nether) |
| 54 | `worldgen` | Nether-3-biome `glacier_rifts` (kloven, tunnels) + **ruimte-check** van Nether-3; zo nodig ring 3 verbreden via config en vastleggen. | B, D, W (Nether) |
| 55 | `conditions` | Kou Nether-3 (meter, warmtebronnen, leren armor remt, hoogste drempel = `ticksFrozen`). | B, S: frost-overlay op beide clients, warmte laat meter zakken |
| 56 | `atmosphere` | Nether-3-sfeer: blauwwitte fog, ijskristal-particles, wind en krakend ijs (biome-data). | runClient |
| 57 | `textures` | Definitieve textures ring 3 + Nether-ring 3. | runClient |

### F. Ring 4 (+ Nether-ring 4)
| # | Categorie | Change | Test |
|---|---|---|---|
| 58 | `blocks` | Moerascipres-set (+ sapling), riet, hangend moerasmos, veen (brandstof). | B, D |
| 59 | `worldgen` | Ring 4-terrein (laag, poelen, zinkgaten) + kernbiomes `toxic_bog` en `root_forest` (surface, gifgroene waterkleur) + cipres- en reuzenwortel-features. | B, D, W |
| 60 | `blocks` | Veenijzer-, gifkristal- en jade-erts. | B, D |
| 61 | `worldgen` | Ores ring 4: veenijzer (veen/poelbodem), gifkristal (grotholtes), jade (diep), vanilla-ores verder afgebouwd. | B, W |
| 62 | `worldgen` | Grotten ring 4: overstroomde grotten (gif-aquifers met luchtkamers) + wortelgrotten. | B, W |
| 63 | `blocks` | Moerasgas: onzichtbaar gasblok, gele particles, ketting-ontploffing via scheduled ticks met cap per tick, blokschade volgt `mobGriefing`. | B, D, S: grote gaskamer ontsteken → geen lag-spike/crash |
| 64 | `worldgen` | Moerasgaskamers (holtes gevuld met gas). | B, W |
| 65 | `conditions` | Gif (water, `toxic_bog`-lucht, bronnen, gas, gifregen; melk −40, honing −20; drempels misselijkheid → gif → wither). | B, S |
| 66 | `atmosphere` | Ring 4-weer: dikke mist (zicht) en gifregen (groene regen, gif ↑ onder open lucht). | B, S: effect op twee clients gelijk |
| 67 | `blocks` | Zuigmodder (wegzakken, server-side), gifzwam (licht, gif-bron), bellenbron (particles, geen stroming). | B, D, S |
| 68 | `worldgen` | Extra biomes: `drowned_forest`, `peat_moors` (zinkgaten), `reed_marsh` + bellenbron-feature in diepe poelen. | B, D, W |
| 69 | `worldgen` | Overgangsbiome 4→5 `blighted_mire` + kleine glitchplek-feature. | B, D, W |
| 70 | `atmosphere` | Ring 4-sfeer: lucht/fog-kleuren, dwaallichten (particles), geritsel in het riet (client-only), geborrel (ambient). | runClient; S |
| 71 | `blocks` | Nether-4-blokken: bottenzand, gebleekt netherrack, fossielsteen-set, zuiverkristal (blok, scherf, lamp). | B, D |
| 72 | `worldgen` | Nether-4-terrein (lavazee diep, bottenvlaktes) + biomes `bone_dunes` (ribbenkasten van bone block) en `crystal_spires`. | B, D, W (Nether) |
| 73 | `worldgen` | Nether-4-biome `geode_hollows` + **ruimte-check** van Nether-4. | B, D, W (Nether) |
| 74 | `conditions` | Zuiverend: alle conditie-meters zakken extra snel in Nether-4. | B, S |
| 75 | `atmosphere` | Nether-4-sfeer: biome-data (fog, wind, gezoem, stof) + pulserend kristallicht (fog-hook in `client/`). | runClient; S |
| 76 | `textures` | Definitieve textures ring 4 + Nether-ring 4. | runClient |

### G. Ring 5 (+ Nether-ring 5)
| # | Categorie | Change | Test |
|---|---|---|---|
| 77 | `blocks` | Corrupted varianten (gras, aarde, steen, cobble, zand, log, wood, planks, leaves) + tag `corruptedadventure:corrupted` + data map `corrupted → vanilla`. Geen gedrag. | B, D: elke corrupted variant heeft een mapping |
| 78 | `blocks` | Glitchblok (drop: glitchscherf als minimaal basis-item) + voidsteen (void-particles). | B, D |
| 79 | `worldgen` | Ring 5-terrein (ring 5-invoer `ring_blend`: dode vlakte, blokkerige glitchvormen; eigen density function-type alleen als JSON niet volstaat) + kernbiomes `corrupted_wastes` en `glitch_fields`. | B, D, W |
| 80 | `blocks` | Nulliet-erts. | B, D |
| 81 | `worldgen` | Ores ring 5: nulliet (void-scheurwanden, diep), glitchblok-plaatsing, vanilla-ores vrijwel weg. | B, W |
| 82 | `worldgen` | Biome `void_scars`: scheuren tot in de void (geen bedrock), voidsteen-randen. | B, D, W: vallen = void; randen goed zichtbaar |
| 83 | `worldgen` | Grotten ring 5: onmogelijke gangen (rasters, lussen) + void-grotten. | B, W |
| 84 | `conditions` | Void-meter (licht ≥ 12 remt; drempels glitch-visuals (client) → teleports 1–5 blokken met veilige-plek-check → darkness + schade). | B, S: teleport nooit in blokken of richting void; visuals alleen client |
| 85 | `atmosphere` | Ring 5-weer: glitchstorm (beeldruis, HUD-flikker, void ×2) en omgekeerde regen (void ×1,5). | B, S |
| 86 | `worldgen` | Extra biome `false_meadow` (vanilla-weide-kopie, stil, zonder mobs, gespiegelde bomen). | B, D, W |
| 87 | `worldgen` | Extra biome `inverted_hills` (omgekeerde zwevende heuvels). | B, D, W |
| 88 | `worldgen` | Horror-features ring 5: ontbrekende chunks (op chunkraster, tot de void) + Herobrine-tekens (bordjes met vertaalbare teksten, redstone-fakkelpatronen). | B, D, W |
| 89 | `worldgen` | Buitenrand afstemmen: ring 5-terrein brokkelt af tot zwevende eilanden en void (void-invoer uit change 6 op ring 5 afgestemd). | B, W: vloeiende afbrokkeling 9.500–10.000 |
| 90 | `atmosphere` | Ring 5-sfeer: lucht/fog-kleuren, glitch-fog (client), echo-voetstappen (client-only). | runClient; S |
| 91 | `blocks` | Nether-5-blokken: kalkrack, witsteen-set, spiegelsteen, stil licht, witte kern. | B, D |
| 92 | `worldgen` | Nether-5-terrein + biome `white_halls` (zuilenrasters, spiegelsteen-vloer, witte kern in zuilen). | B, D, W (Nether) |
| 93 | `conditions` | Stilte (meter; ↓ via server-side game events van bel, goat horn, note block, jukebox en spelers binnen ~8 blokken; client dempt volume en verbergt HUD). | B, S: twee spelers samen bouwen langzamer op; HUD-verbergen alleen client |
| 94 | `atmosphere` | Nether-5-sfeer: witte fog, geen ambient, geen muziek (biome-data). | runClient |
| 95 | `textures` | Definitieve textures ring 5 + Nether-ring 5 (incl. geanimeerd glitchblok). | runClient |

### H. Afsluiting fase 1
| # | Categorie | Change | Test |
|---|---|---|---|
| 96 | `worldgen` | **Worldgen-eindtest fase 1**: met de testseed elke ring en elke grens (overworld én Nether) bezoeken, overgangsbiomes, buitenrand en world border controleren, portal-test vanuit elke ring, `/locate structure stronghold` ✗, chunk-generatietijd per ring meten. Gevonden fouten klein oplossen of als losse change in de backlog. | B, D, S, W volledig; runServer met twee clients |
