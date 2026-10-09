# Fase 1 — Wereld

Uitwerking van de sparringsessie(s) voor fase 1. Bron van waarheid voor *wat* we in fase 1 bouwen.
Beslissingen staan ook (kort) in `02-beslissingenlog.md`.

**Status:** sparring afgerond. Sessie 1: fundament, ring 1 en ring 2. Sessie 2: ring 3, 4 en 5 (alle met Nether-tegenpolen). Bouwplan compleet (changes 0–96, §3). Changes 0–37 (+ 23b, 23c, 30b, 30c, 37b) gebouwd; teststop 3 akkoord (2026-10-06). **Sparring ring 3 (2026-10-06)** herziet ring 3 en de ringbreedtes (zie Ring 3 → "Uitwerking sparring ring 3"); bouw van ring 3 in parallelle golven volgens `docs/plans/ring-3/waves.md`.

---

## 1. Fundament

### 1.1 Bouwvolgorde
1. **Skelet:** world preset (overworld + Nether), `ringAt`, ring-BiomeSource, één placeholder-biome per ring, `ring_blend` density function.
2. **Ring voor ring** (1 → 5), telkens met de Nether-tegenpool: terrein → blokken → ores/grotten → sfeer/weer → condities → definitieve textures.

### 1.2 Terrein-architectuur
- Er is één noise settings per dimensie. Terrein per ring komt uit een eigen density function **`corruptedadventure:ring_blend`** (Java-type + codec).
- `ring_blend` krijgt per ring een eigen density function (JSON, in `worldgen/density_function/<ring>/…`) en kiest op basis van `ringAt` welke geldt. In de overgangszone mengt hij met een **smoothstep** (3t² − 2t³) op het lineaire gewicht tussen de twee aangrenzende ringen: geen knik aan de randen van de zone (zie §1.15).
- Alle overige terreinlogica (vorm, canyons, vulkanen) blijft data-driven in JSON.
- **Klimaat per ring via de router (change 23):** de router-velden `temperature` en `vegetation` zijn ook een `ring_blend` (`ring_blend/overworld_temperature`/`_vegetation`): ring 1 = de vanilla-klimaatruis (ongewijzigd, dus ring 1 blijft blok voor blok gelijk), ring 2 = de eigen regio-assen `relief`/`moisture`, ring 3–5 en void = 0 tot hun eigen terreinchange. De biome-source van een ring is een inline `minecraft:multi_noise` op die velden; een biome-source krijgt alleen de klimaatsampler, niet de `RandomState`, dus terrein en biome delen hun regio's via de router. Terrein en biome lezen dezelfde named density function: grenzen vallen exact samen, zonder Java. Ring 3–5 en de Nether-ringen (change 35) volgen hetzelfde patroon.
- **Ring gate (change 23):** `RingBlend.mapAll` zet in elke `interpolated`-marker van een ringinvoer een `corruptedadventure:ring_gate`: buiten de ring, zijn overgangszones en 16 blokken marge geeft hij 0. Zo rekent elke chunk alleen het terrein van de 1–2 ringen die er gewicht hebben. Meting en naadcontrole in `CODEBASE.md` (valkuilen).
- Client/server: puur worldgen, deterministisch per seed; geen sync nodig.

### 1.3 Ringgrenzen en overgangszones
- Grenzen uit config (zie `00-overzicht.md`), vervormd met seeded ruis van **±96 blokken**. Vorm (change 2): twee lagen simplex-ruis, golven van ~600 en rimpels van ~150 blokken (75/25), gesampled in overworld-coördinaten zodat de Nether exact meeloopt.
- Overgangszone van **~128 blokken** rond elke grens (Nether: ÷4, dus ~32 blokken en ±24 ruis).
- In de overgangszone: blend van terrein en een eigen overgangsbiome per grens (uit te werken per ring). De zone mag een eigen terreinvorm krijgen via het ring-gewicht als density function (JSON, `ring_weight/overworld/ring_N`: `ring_blend` met constante invoer), naast de smoothstep-blend (change 31: de beddingen van `dry_riverbeds`). Een overgangsbiome vult **de hele zone**, ook boven water (change 11). Tot een grens zijn overgangsbiome heeft, wisselt de biome **hard op gewicht 0,5** (precies op de verruisde grens); overgangsbiomes staan in het optionele veld `transitions` van de ring-BiomeSource (zie §1.14).
- **Config → snapshot per wereld:** de config geeft de standaardwaarden. Bij het aanmaken van een wereld worden ze in die wereld vastgelegd; daarna gebruikt `ringAt` alleen die snapshot. Een aangepaste config geldt alleen voor nieuwe werelden. De server synct de waarden naar de client (voor HUD en muren).
- **Sync naar de client (gebouwd in change 18):** bij elke login in een ringwereld stuurt de server payload `corruptedadventure:ring_layout` (de snapshot + een ruis-seed). De grensruis gebruikt sindsdien als seed de eerste 8 bytes van SHA-256(wereldseed ‖ `corruptedadventure:ring_boundary`), op server én client; de wereldseed zelf gaat nooit naar de client. Daardoor liggen de verruisde grenzen (binnen ±96 blokken) anders dan in werelden van vóór change 18; vorm en karakter zijn gelijk. De client rekent `ringAt` zelf (`ClientRingLayout`, F3-regel `CA ring (client)`), alleen voor weergave; gameplay rekent op de server.
- Snapshot (gebouwd in change 1): `<wereld>/corruptedadventure/ring_settings.json`, gelezen of aangemaakt bij `ServerAboutToStartEvent` (die op dedicated én integrated server vóór `loadLevel()` komt, dus vóór de spawn-chunks). Geen `SavedData`, want die bestaat pas als de level geladen is. Een bestaand snapshot gaat altijd vóór de config. Ongeldige config of corrupt snapshot → de server start niet (duidelijke foutmelding), nooit stilletjes terugvallen.

### 1.4 Textures
- **Productiemethode: eigen pixels uit een deterministisch generatorscript, geen vanilla-pixels in de repo** — ook niet voor placeholders (vervangt "getinte/gerecolorde vanilla-textures", besloten 2026-09-30). Eén script per ring: ring 1 `tools/textures/Ring1Textures.java`, ring 2 (+ Nether-ring 2) `tools/textures/Ring2Textures.java` (sinds change 21; helpers gekopieerd, generieke opzet met `Animation`-record, gloeikleur per materiaal en ingebouwde controles op afmeting/alpha). Single-file Java, alleen JDK, vaste seed per texture. Vanilla dient hooguit als visuele referentie voor de indeling; models mogen wel naar vanilla-textures **verwijzen** (bijv. netherrack als onderlaag van een erts).
- Eerst **placeholders** (eenvoudige eerste scriptversie, zie change 10), zodat worldgen en sfeer testbaar zijn.
- Per ring een aparte `textures`-change die het script verfijnt tot de definitieve textures.
- **Stijl (change 17; ring 1 definitief, sjabloon voor de textures-changes van ring 2–5):**
  - 16×16; per materiaal een vast palet van 4–6 tinten (donker → licht, hex in de ringsectie). Elke pixel valt op het palet (kwantisering); korrel via seeded, tegelbare waarderuis. Alleen alpha 0 of 255 (cutout- en mipmap-veilig).
  - Vanilla-indelingen als referentie, niet als pixels: planken 4 × 4 rijen met donkere naad en verspringende stuiknaden; log-zijkant verticale nerf/schors; kopse kant jaarringen + 1 px rand; zijkant van een grasblok = exact de pixels van de bijbehorende aarde met een rafelige band bovenaan; deur/valluik met gaten (cutout).
  - Ertsen: tweelaags (vanilla-basis als verwijzing + eigen overlay). Kristallen met een donkere 1-px-omlijning, lichte linkerkant, schaduw rechts, highlight op de punt. De overlay is **emissive** (NeoForge `neoforge_data`: block/sky light 15, zonder ambient occlusion: gloeit in het donker, het blok geeft daardoor geen extra licht) en **pulseert**: 8 frames, `frametime` 4, `interpolate` (≈ 1,6 s per cyclus), alleen de kleur verandert, nooit de alpha. Alle overlays van één materiaal hebben dezelfde frames, dus ze pulseren gelijk op. Items: dezelfde puls, niet emissive.
  - **Uitzondering (change 24):** edelstenen zonder gloed (turkoois) hebben een matte, niet-emissive overlay zonder puls en geen bloklicht; `oreWithOverlay` heeft daarvoor een `emissive`-vlag. Gloeiende ertsen (zielkristal, zonnesteen) blijven emissive.
  - **Aanvullingen (change 37):** de gloeikleur van de puls is per materiaal (zonnesteen `FFF0B0`, dauwkristal `F0FCFF`); opake bewegende textures (water, slijm) zijn `cycling`-animaties zonder interpolate (periodiek in de fase, zelfde seed per frame, volledig opaak); emissive kruis-/vlakmodellen (dauwstadia, gloeischimmel) krijgen eigen elementen met `emissivity(15, 15)` zonder AO; het script controleert ook "zijkant = grond" (`SIDE_RULES`) en dat `cycling` opaak is; één contactblad per ring met secties per dimensie (ring 2: `RING 2` en `NETHER RING 2`, plus een extra-blok met kloofkeien-varianten, strata, klei en zoutvlakte).
  - **Aanvullingen (change 37b):** **selectieve omlijning** voor items, kristallen (kruismodellen) en erts-overlays (37b-3 A): randpixels boven/links in een donkere materiaaltint, onder/rechts de donkerste tint (zoals vanilla's items; één kleur rondom oogt als sticker); ring 1 volgt later (backlog). **Pulsweging:** de dimfase leunt 12 % naar een donkere materiaaltint (zonnesteen `9A4A0C`, dauw `5E9FB4`) in plaats van 22 % naar zwart, en alleen tonen lichter dan die tint bewegen (omlijning en schaduw blijven), zodat de rustframe goud/helder blijft en de piek het volume niet wegwast. **Vlakheidscontrole:** het script waarschuwt bij een opaak blok met < 5 kleuren of één kleur op > 50 % van de pixels (vanilla: 6–9 kleuren, 20–37 %), een sprite met < 5 kleuren en een item met één omlijningskleur; uitzonderingen met reden in `FLAT_OK`. **Vergelijkingsblad:** `tools/textures/VanillaCompare.java` (37b-6 A) leest vanilla alleen uit de moddev-resources-jar en schrijft alleen `build/texture-preview/ring2-vs-vanilla.png` (vanilla als referentie, nooit in `src/` of git). Items met een sterk silhouet mogen als hand-gelegde sprite in het script staan (`sprite(rows, legend)`: eigen pixels, deterministisch).
  - Beoordeling: het script schrijft een contactblad (`build/texture-preview/ring1.png`, niet in git: 8×, 3×3-tegeling, alle frames). Met de hand bijgewerkte textures komen in de lijst `HAND_EDITED` van het script; het script slaat ze daarna over.
- Alle mod-blokken en -items staan in **één eigen creative tab** "The Corrupted Adventure" (`corruptedadventure:main`), per ring gesorteerd (registratievolgorde); niet in de vanilla-tabs.

### Geluid (productiemethode, change 32)
- **Eigen, deterministisch synthesescript, geen vanilla-audio in de repo:** `tools/sounds/Ring2Sounds.java` (single-file Java, alleen JDK, buiten Gradle en de jar) schrijft mono WAV's naar `build/sounds/` (vaste seed per bestand, `StrictMath`, twee keer draaien = identiek) en laat ffmpeg (libvorbis, `-ac 1 -q:a 4`) er mono Ogg Vorbis van maken in `assets/corruptedadventure/sounds/`. De `.ogg`-bestanden staan in git; build en CI hebben ffmpeg niet nodig.
- Varianten: `drone` (laag, zwellend, overtonen), `ambient` (vogels/insecten); `loop` komt in change 36. Sjabloon voor de geluiden van ring 3-5 (changes 50, 70, 90).
- Positionele geluiden zijn mono; de pitch-klem van vanilla is 0,5-2,0, dus echt diepe klanken vragen eigen audio. Kwaliteit is "nette programmer-audio", later te vervangen door eigen opnames zonder andere wijzigingen.

### 1.5 Grensmuur (ontwerp nu, bouw in fase 5)
- Elke grens krijgt een **eigen soort muur** die zicht op de volgende ring vaag houdt tot de speler de bewaker van die grens heeft verslagen. Type per grens: zie de ringsecties.
- De muur is **per speler** (volgt de per-speler unlocks). Rendering client-side in de overgangszone, staat server-side en gesynct.
- In fase 1 wordt de muur alleen ontworpen; de bouw hoort bij het grenssysteem (`progression`, fase 5).

### 1.6 Weermodel
- **Vanilla regen/onweer blijft** en is alleen zichtbaar in biomes met neerslag (ring 1, deels ring 4).
- Daarnaast **ringweer**: per ring een eigen weercyclus (typen per ring: zie ringsecties) in `SavedData`, per dimensie.
- Server bepaalt en synct (`CustomPacketPayload`) de weerstaat per ring; de client rendert alleen (lucht, fog, neerslag, particles) vanuit `client/`.
- Gameplay-effecten van weer (bijv. extra hitte bij asregen) lopen via de condities-tick op de server, nooit op de client.

**Uitwerking (change 20, gebouwd):**
- **Typen:** Java-enum `RingWeatherType` met ring en dimensie per waarde: `clear` + `sandstorm`, `heat_wave` (ring 2), `ash_rain`, `eruption` (ring 3), `dense_fog`, `toxic_rain` (ring 4), `glitch_storm`, `reverse_rain` (ring 5). Ze bestaan al, maar hebben nog geen effect: rendering en gameplay komen in changes 28, 45, 66 en 85. Tot dan "regent" het onzichtbaar (alleen in F3 en `/ca weather`). Ring 1 heeft geen ringweer (alleen vanilla regen/onweer).
- **Staat:** per ring type, resterende en totale duur en een `seed` (getrokken bij elk nieuw weer, bijv. voor de windrichting van change 28). `SavedData` per dimensie, alleen voor dimensies met typen in `corrupted_world`-werelden: nu alleen de overworld (`<wereld>/data/corruptedadventure_ring_weather.dat`). **Geen Nether-ringweer.** Een onleesbare of niet-passende opgeslagen staat wordt `clear` met een nieuwe duur.
- **Cyclus:** elke 20 ticks op de server; na een weer altijd `clear`, na `clear` een type van die ring op gewicht. **Vanilla-ritme** (keuze gebruiker): helder 600–9000 s (12.000–180.000 ticks), weer 600–1200 s (12.000–24.000 ticks), gelijke gewichten; per ring instelbaar in de SERVER-config (`[weather.ring2]` … `[weather.ring5]`: `clearMinSeconds`, `clearMaxSeconds`, `weatherMinSeconds`, `weatherMaxSeconds`, `<type>Weight` 0–100). Live gelezen: een wijziging geldt vanaf het volgende weer. Gewicht 0 = niet in de cyclus (wel via `/ca weather`).
- **Vanilla-weer:** onafhankelijk (`/weather` raakt het ringweer niet), maar de cyclus staat stil zolang `doWeatherCycle` uit staat (ook de fade).
- **Overgang:** 30 s fade-in en -out in de tijd (`intensity`); ruimtelijk mengt de rendering over de overgangszone (smoothstep: het weer van de binnenring dunt uit naar de buitenring). Gameplay wisselt hard op de ringgrens (`ringAt(…).ring()`, gewicht 0,5, zoals de biome). De void-rand volgt het weer van ring 5.
- **Sync:** elke speler krijgt de **volledige** ringweerstaat van zijn dimensie bij login, dimensiewissel en respawn, en alle spelers van de dimensie bij elk nieuw weer, elke `/ca weather`-wijziging en elke `doWeatherCycle`-wissel. De client rekent de resterende tijd zelf uit (verschil in game time sinds de verzending, dus ook bij serverlag gelijk aan de server) en bepaalt zelf welke ring(en) hij voelt; geen per-tick-packets.
- **Client-hook:** `client/weather/RingWeatherClientHooks` berekent per tick/frame de actieve typen met sterkte (tijd-fade × ruimtelijk aandeel) op de camerapositie en roept de renderer per type aan (`RingWeatherRenderer`: `clientTick`, `renderFog`, `fogColor`, `renderLevel`). Weer-changes schrijven alleen een renderer. Biome-kleuren blijven biome-data; het weer legt er alleen iets overheen. Lucht- en neerslagkleur hebben in NeoForge 1.21.1 geen event: de eerste weer-change die het nodig heeft, kiest de aanpak.
- **Server-API voor condities:** `RingWeather.state(level, ring)` en `RingWeather.stateAt(level, x, z)`; geen weer-veld in `ConditionContext`.
- **Debug:** `/ca weather` (per ring type, resterende tijd, intensiteit), `/ca weather <ring> <type> [seconden]` (zonder seconden een duur uit de config), F3-regel `CA weather (client): …`.

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

**Uitwerking (change 19, gebouwd):**
- **Model:** Java-enum `Condition` (nu alleen de blijvende test-conditie `debug`); per conditie precies één regel (`ConditionRule`, punten per seconde, + op / − af) en N drempels. Opslag op naam (lenient: een onbekende naam wordt overgeslagen), sync op volgorde.
- **Rate-pijplijn (elke 20 ticks, alleen ringwerelden):** regel → alle `RateModifier`s in registratievolgorde (ringweer, gear, zuiverend Nether-4, …; ook buiten de zone van de conditie) → in creatief/spectator nooit omhoog (wel omlaag) → meter klemmen op 0–100.
- **Drempels cumulatief:** alle drempels ≤ meter zijn actief. Mob effects worden elke seconde ververst met 12 s duur, ambient en zonder particles (geen knipperend icoon); onder de drempel haalt de server "ons" effect direct weg (alleen als het actieve exemplaar ambient is, dus een actief drankje blijft staan). Schade-drempels: interval in seconden + ondergrens per conditie (`DifficultyFloor`: altijd / zoals vanilla-verhongeren / nooit dodelijk). In creatief/spectator geen effecten.
- **Dood:** alle meters naar 0. Bewaard bij uitloggen, serverherstart en dimensiewissel. Moeilijkheidsgraad alleen via de schade-ondergrens.
- **Config:** SERVER-config `corruptedadventure-server.toml` (standaard in `config/`, per wereld te overschrijven via `<wereld>/serverconfig/`), alleen getallen, door NeoForge naar de client gesynct. Welk effect bij welke drempel hoort staat in Java. Drempels strikt oplopend, anders standaardwaarden.
- **Sync:** volledige momentopname (afgerond, 1 byte per conditie) alleen naar de eigen speler, alleen als een afgeronde waarde verandert (≤ 1/s), plus bij login, respawn en dimensiewissel. Andere spelers krijgen andermans meters nooit.
- **HUD:** per meter > 0 een balk (81 × 5 px) rechts boven de hongerbalk, gestapeld boven luchtbellen, met streepjes op de drempels; verborgen met F1, in creatief en spectator. Placeholder-look (vlakken), definitief in een `textures`-change.
- **Debug:** `/ca condition get [<spelers>]`, `/ca condition set <spelers> <conditie> <0–100>`, `/ca condition debug <spelers> rise|fall|off` (stuurt de `debug`-conditie: rise +5/s, anders −5/s; drempels 50 traagheid, 75 zwakte, 100 schade 1/s).

### 1.10 Biome-varianten
- Ring 1: kopieën van vanilla biomes (zie §1.7).
- Ring 2–5: **4–5 varianten per ring** + **1 overgangsbiome per grens**. Nether-ringen: aantal per ring in de ringsecties.
- Bouwvolgorde per ring: eerst 2–3 kernvarianten, de overige als latere changes binnen dezelfde ring.
- Verdeling binnen een ring (vanaf ring 2): regio-assen in de router + `multi_noise` (§1.2). Vlekken in ring 2 ~500–1.000 blokken.
- Ring 3 (change 39a): regio-assen `region_heat`/`region_ground` op firstOctave −11, één octaaf grover dan ring 2. Reden: 8 biomes op twee assen geven kleinere vlakken; op −10 zou een biome langs een lijn gemiddeld elke ~310 blokken wisselen (ring 2: ~500), op −11 ~580. Nether-3 op dezelfde voet (÷4): −9, drie biomes ~270 Nether-blokken (Nether-2: ~240).

### 1.11 Ores (algemeen)
- Ring 1 heeft alle vanilla-ores.
- Naar buiten toe **bouwen vanilla-ores af**: ijzer en kolen schaarser, diamant bijna afwezig. Ringores nemen het over.
- Netherite/ancient debris blijft zoals vanilla. Ring-gear is per ring een tier hoger (fase 2), zodat netherite rond ring 3 al achterloopt.
- **Uitwerking (change 25) — afbouwmechanisme voor ring 2–5 (changes 25, 41, 61, 81):**
  - Per ring en vanilla-erts een eigen placed feature `corruptedadventure:ring_<n>/<vanilla-pad>` = vanilla's eigen placed feature (zelfde configured feature, aantallen, hoogtes en air-exposure) met één `ring_chance` (`progress`, vaste kans = factor) direct vóór de biome-filter. Buiten de ring is de kans 0. Helper `RingVanillaOres` (`placed`, `keys`, `factors`); vanilla's getallen worden gelezen, niet gekopieerd.
  - De biomes van een ring krijgen géén vanilla-ertsen (alleen `underwater_magma` uit `addDefaultOres` blijft); de geschaalde ertsen en de ringores komen via één biome modifier per ring (`add_ring_<n>_ores`, `UNDERGROUND_ORES`, vaste volgorde).
  - Vaste factor per ring (geen verloop binnen de ring); `withered_expanse` (zone 1→2) houdt volle vanilla-ertsen. Vanilla-ertsen alleen in steen/deepslate, nooit in ringgesteente.
  - **Ore veins** (grote ijzer-/koperaders uit de noise router) alleen in ring 1: router-`vein_toggle` via `ring_blend` (ring 1 vanilla, ring 2–5 en void 0); in de zone 1→2 lopen ze uit. Ring 1 blijft blok voor blok gelijk.

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
- `/ca tpring <1–6> [nether]` (6 = void-rand): naar het radiale midden van de nominale ring (uit de snapshot; met de standaardwaarden sinds de sparring ring 3: 750 / 2.500 / 4.750 / 6.900 / 8.650 / 9.750, daarvóór 4.500 / 6.500 / 8.500 voor ring 3–5; Nether ÷4), **in je huidige richting vanaf 0,0** (op 0,0 zelf: oost). Zonder `nether` gaat het naar de overworld.
- `/ca tpboundary <1–5> [nether]`: naar de echte, verruisde grens (gewicht 0,5) tussen ring n en n+1, in dezelfde richting. Bedoeld voor test W.
- `/ca border` (change 7): grootte, straal en midden van de border in de huidige dimensie (plus doel en resttijd als hij beweegt). Nodig omdat vanilla `/worldborder get` ook in de Nether de overworld-border toont. Kleine uitzondering op de categorie: gebouwd in change 7 (`worldgen`).
- Landingshoogte: overworld bovenste blok (heightmap); Nether de hoogste plek met 2 blokken lucht op een stevige vloer, onder het dak. Geen veilige plek (void, lava)? Dan toch teleporteren naar een vaste Y (overworld 100, Nether 64), met een waarschuwing in de chat.

### 1.14 Ring-BiomeSource en placeholder-biomes (change 5)
- `corruptedadventure:ring` kiest per positie de ring (via `ringAt`) en geeft de keuze door aan een **geneste biome-source per ring**: nu `minecraft:fixed` met een placeholder, later bijv. `minecraft:multi_noise` voor de varianten binnen een ring (ring 1: `corruptedadventure:vanilla_climate` sinds change 8, zie §1.7; Nether-ring 1 sinds change 14).
- **Placeholders (tijdelijk):** `placeholder_ring_1…5` (overworld) en `placeholder_nether_ring_1…5` (Nether); ring 1 is ze kwijt sinds change 8 (overworld) en change 14 (Nether). Alleen een **eigen kleur** (lucht, fog, water, gras, blad; Nether vooral fog) en **eigen weer** (neerslag en temperatuur: ring 1 en 4 met regen, ring 2, 3, 5 en de Nether droog). **Geen features, geen mob-spawns**, geen `has_structure/*`-tags. Ze verdwijnen zodra de echte biomes van een ring er zijn.
- **Void (blijvend):** `void_edge` (overworld) en `nether_void_edge` (Nether), voorbij ring 5; aparte biomes zodat sfeer later per dimensie kan.
- Overgangszone: harde wissel op gewicht 0,5 tot er overgangsbiomes zijn (§1.3).
- **Overgangsbiomes (change 11):** optioneel veld `transitions` (sleutel = binnenring van de grens, `ring_1` = grens 1→2; `void` is ongeldig), per grens een geneste biome-source. Binnen de hele zone (`inTransition`, ~128 blokken, Nether ~32) wint die source; grenzen zonder overgang wisselen hard op 0,5. Nu overworld grens 1 (`withered_expanse`) en grens 2 (`dry_riverbeds`, change 31); de Nether heeft geen overgangen.

### 1.15 `ring_blend` en placeholder-terrein (change 6)
- **Chunk-generator `corruptedadventure:ring_noise`** (overworld en Nether): vanilla noise-generatie zonder vloeistof in de void-zone. Vanilla vult alle open ruimte onder zeeniveau met water (overworld) of lava (Nether) en zet daarnaast lokale aquifer-vloeistof (onder Y -10 lava, bijv. onder zwevende eilanden); `ring_noise` doet geen van beide vanaf de zone ring 5 → void (vloeistof-kiezer + aquifer-wrapper). Later ook bruikbaar voor ringen met een eigen vloeistofregel.
- **Mengcurve:** smoothstep op het lineaire gewicht van `ringAt`.
- **Placeholder-terrein (tijdelijk; elke ring vervangt het in zijn eigen terreinchange):**

| Ring | Overworld: oppervlak | Nether: vloer (dak vanaf Y ~110) |
|---|---|---|
| 1 | vanilla (change 9) | vanilla-terrein (change 14) |
| 2 | eigen terrein (change 23): vlakte Y 66–74, plateau Y 125–140, kloofbodem Y ~70 | Y ~40 |
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
- **Uitwerking (change 13):** het "randbiome" is `withered_expanse` (geen apart randbiome). Lucht, fog, water en waterfog zijn de vanilla-vlaktekleuren van ring 1 (`BiomeAmbience.Palette.RING_1`: `0x78A7FF`, `0xC0D8FF`, `0x3F76E4`, `0x050533`), 40 % naar grijs gemengd (`desaturate`, luma): `0x89A5DA`, `0xC9D7EE`, `0x5374B6`, `0x070723`. De overgang is vanilla's client-side biome-blending (geen harde lijn, wel kort: ~10–20 blokken). Binnen ring 1 geen radiaal verloop: de client kent de ringdata pas na change 18 (backlog).

**Biomes**
- Eigen kopieën van de vanilla overworld-biomes (zie §1.7), inclusief deep dark. **Geen** mushroom fields.
- Overgangsbiome 1→2: **verdorde vlakte** (`withered_expanse`, werktitel): bladloze bossen, vaal gras, zandsteenranden, een voorbode van ring 2.
- **Uitwerking (change 11):** `corruptedadventure:withered_expanse` vult de hele zone van grens 1, ook boven water.
  - Bodem (surface rule bovenaan `above_preliminary_surface` in `noise_settings/overworld.json`): dood gras als hoofdbodem, vlekken grove aarde en verdorde aarde (surface-ruis), zandsteen op hellingen (`steep`), verdorde aarde eronder en onder water.
  - Features: kale bomen (`dead_tree`, `dead_tree_forked`: `dead_log` zonder blad, 70 % gevorkt), dode struiken en kort gras, zandsteenkeien en vlekken zand met grove aarde. Verloop via placement modifier `ring_chance` (signaal `transition` van grens 1): bomen 1,0 → 0,2, keien 0,1 → 1,0, zandvlekken 0 → 0,8 van de ring 1- naar de ring 2-kant. Zandvlekken komen vóór de begroeiing (stap `LOCAL_MODIFICATIONS`), zodat er geen gras op zand staat. Aantallen zijn startwaarden (test W).
  - Klimaat: als savanne (temperatuur 2,0, geen neerslag). Spawns: vanilla-monsters ('s nachts), als enige dier konijnen (gewicht 4, zoals de woestijn). Konijnen spawnen alleen op `#rabbits_spawnable_on` (o.a. zand, niet op dood gras): in de zone dus vooral op de zandvlekken.
  - Kleuren: gras `0xA8A070`, blad `0x9A9060`; lucht, fog en water sinds change 13 (zie Terrein en sfeer).
  - Vanilla-features in vanilla-volgorde (`globalOverworldGeneration`, ores, soft disks), eigen features achteraan per stap; geen `has_structure/*`- of `is_overworld`-tags.

**Unieke blokken (fase 1, placeholders)**
- Dood gras / verdorde aarde, dood hout (log + planks) voor kale plekken en het overgangsbiome.
- **Uitwerking (change 10):** `dead_grass_block`, `withered_dirt` en de dood-hout-set (`dead_log`, `dead_wood`, `stripped_dead_log`, `stripped_dead_wood`, `dead_planks` + houtset). Geen bladeren, geen sapling (dode bomen komen alleen uit worldgen).
  - **Dood gras gedraagt zich als gras**, maar binnen zijn eigen familie: bedekt/geen licht → verdorde aarde; in het licht spreidt het alleen naar verdorde aarde. Vanilla-gras en dood gras beïnvloeden elkaar niet. `snowy`-state met eigen sneeuwzijkant. Schop → pad, schoffel → farmland op beide blokken (pad en farmland worden later vanilla-aarde: een speler kan een dode plek zo "genezen"; geaccepteerd voor fase 1).
  - Vaste kleur (geen biome-tint). Silk Touch → dood gras, anders verdorde aarde. In `#minecraft:dirt` (planten groeien erop, carvers snijden erdoor), niet in `#animals_spawnable_on` (geen dieren op dode grond).
  - Brandbaarheid zoals eikenhout (log/wood 5/5; planks, trap, plaat, hek, poort 5/20; deur, valluik, knop, drukplaat branden niet).
- **Textures (change 17, definitief; stijl in §1.4):** dood hout verbleekt grijsbruin (drijfhout), dood gras vaal geelbruin (stro) op grijsbruine verdorde aarde, geen biome-tint. Paletten (donker → licht):
  - Verdorde aarde: `4A3F36 5A4D42 6B5D50 7B6C5D 8C7D6B` — klonterige korrel, lichte kluitjes met schaduw, haarscheurtjes.
  - Dood gras: `6E6544 857B53 9A8F62 AEA272 C2B686` — platliggende sprieten (donkere voet, lichte punt), een enkele kale plek; zijkant: rafelige strorand (2–6 px) over dezelfde pixels als verdorde aarde.
  - Schors: `3F3832 524A42 655C52 786E62 8B8174` — slingerende groeven met schaduw, onderbroken in platen, één afgebladderde plek waar het kale hout doorschijnt.
  - Dood hout (planks, gestript, kopse kant, deur, valluik): `7E7466 918777 A39988 B4AA98 C4BBA9` — kopse kant met afgeronde jaarringen en een radiale droogscheur (gestript: zelfde ringen, houtrand i.p.v. schors).
  - Sneeuwrand (`dead_grass_block_snow`): `C9D3D8 DCE6EA EDF3F5 FFFFFF`.

**Ores**
- Alle vanilla-ores, vanilla-verdeling.

**Grotten**
- Vanilla noise caves en carvers, inclusief deep dark (zonder ancient cities; beslissing in fase 3).
- **Stille zones**: plekken waar ambient geluid wegvalt of vreemde cave sounds vaker klinken, vaker richting de rand (client-side ambient).
- **Uitwerking (change 13):** puur data, via de vanilla-`mood_sound` per biome (de client speelt die alleen in het donker zonder hemellicht, dus in de praktijk alleen ondergronds). Biomes zijn 3D en gaan met de chunk mee, dus de zones zijn voor alle spelers gelijk; wanneer een geluid klinkt verschilt per speler (vanilla-toeval).
  - **Stil** (geen `mood_sound`): de kopieën van dark forest, old growth pine taiga, old growth spruce taiga en dripstone caves (`VanillaBiomeCopies.MOODS`).
  - **Vreemd**: `withered_expanse`, de hele kolom, dus ook in de grotten onder de grens. Eigen sound event `corruptedadventure:ambient.ring_1.strange_cave` (`tick_delay` 1800 ≈ 1,5 min in het donker, `block_search_extent` 12, `offset` 3). Klinkt als vanilla's `ambient.cave` met pitch 0,5/0,6/0,7 en volume 0,8 (`sounds.json`, type `event`, geen eigen audio). Ondertitel "Something stirs in the dark" / "Er beweegt iets in het donker".
  - Alle andere kopieën (ook deep dark en lush caves) houden vanilla's grotgeluid. Muziek blijft overal vanilla.
  - Onnatuurlijke tunnels (change 12) krijgen geen eigen stille zone: ze erven het grotgeluid van hun biome (stil, vreemd of vanilla).
  - Bekend: stilte valt pas op na lang verblijf (vanilla speelt zelf al maar ~1× per 5 min); onderwater speelt nooit een mood-geluid.

**Weer en condities**
- Alleen vanilla regen/onweer. Geen ringweer, **geen conditie**.

**Horror-elementen (fase 1)**
- Zeldzame **onnatuurlijke 2×1-tunnels** in grotten en bergen (worldgen-feature).
- **Kale plekken met dode bomen**, vaker richting de rand.
- Stille zones in grotten (zie Grotten).
- Events (fakkels, silhouetten) → fase 5, zie backlog.
- **Uitwerking (change 12):** beide horror-features zijn deterministische worldgen-features, via NeoForge-biome-modifiers in de ring 1-kopieën (niet in de Nether, niet in `withered_expanse`).
  - **Kale plekken** (`bare_patch`): rafelige plek (straal 4–9) dood gras met 1–2 lagen verdorde aarde, alleen op gras/aarde/grove aarde/podzol/geworteld aarde (water, zand en steen blijven). Bestaande bomen en planten **verdorren** (antwoord 1a): bladeren weg (tot 3 blokken buiten de rand met afnemende kans: rafelige kroonrand), stammen → `dead_log` in hun oorspronkelijke vorm, bloemen/gras weg. Daarop 1–3 kale bomen (`withered_trees` van change 11) en wat dode struiken. Alleen in begroeide land-biomes (vlaktes, bossen, taiga, savanne, windswept, jungle, moeras, cherry grove; niet in oceanen, rivieren, stranden, woestijn, badlands, sneeuw/ijs, pieken, mangrove, grotten).
  - Bekende beperking: een plek reikt tot ~12 blokken in de buurchunks; een buur die later decoreert, kan daar nog levende vanilla-planten op zetten (zie backlog, test W beoordeelt het).
  - **Verloop** (antwoord 2a): kans per chunk via `ring_chance` met signaal `progress` (voortgang over de hele ring, 0 op 0,0 → 1 op de grens): 0 tot een derde van de ring (~500 blokken), dan kwadratisch oplopend tot 0,12 per chunk aan de rand. Gemeten (seed 20260930, RCON, ~12×15 chunks per band): centrum geen, ~750 blokken 1 plek, rand 10–18 chunks met dood gras.
  - **Tunnels** (`unnatural_tunnel`, antwoord 3a): 1 breed, 2 hoog, kaarsrecht en perfect horizontaal, 50 % kans op één haakse bocht; segment 1 12–28, segment 2 6–14 blokken, doodlopend, kaal. De mond ligt waar de tunnellijn uit open lucht (grot of buitenlucht) in rots gaat, dus in een grotwand of berghelling. Alles-of-niets: geen water/lava in of naast de tunnel, geen zand/grind in het dak, ≥ 70 % rots. Een **feature, geen carver** (een feature ziet het hele 3×3-chunkgebied en kan het pad eerst valideren; werkt met `/place feature`); prijs: max. ~45 blokken, de grote rasters zijn voor ring 5.
  - **Verdeling tunnels** (antwoord 4a): gelijkmatig zeldzaam over heel ring 1, ook bij spawn, Y −48…160, ook in de cave biomes. Startwaarde 1 poging per 12 chunks; ~3 % van de pogingen levert een tunnel op (gemeten via RCON, 6 van 200 willekeurige posities, Y −40…60), dus ~1 tunnel per ~400 chunks (~3 binnen 300 blokken van spawn). Afstemmen in test W.

**Grensmuur 1→2** (fase 5): dichte witte mist.

**Nether-tegenpool — Nether-ring 1 (0 – 375)**
- Kopieën van nether wastes, crimson forest, warped forest (geen soul sand valley, geen basalt deltas). Vanilla Nether-terrein.
- **Verdeling (change 14):** `corruptedadventure:vanilla_climate` met `preset: minecraft:nether`; `replace` zet soul sand valley en basalt deltas om naar nether wastes, dus crimson en warped liggen precies waar vanilla ze legt. De klimaatruis van de Nether-router (`temperature`/`vegetation`, `xz_scale` 0,25 → 1,0) is 4× fijner, zodat alle drie de biomes in de kleine ring voorkomen; dat geldt voor **alle** Nether-ringen.
- **Terrein (change 14):** vanilla's Nether-`final_density` zonder de buitenste `squeeze`/`× 0,64` (teken gelijk, dus blok voor blok vanilla; grootte vergelijkbaar met de placeholder-invoer van Nether-ring 2, zodat de overgang over de hele zone mengt). Lavazee op 32, vanilla-dak.
- **Structures (change 14):** vanilla fortress, bastion en Nether-ruined-portals via de vanilla-tags van de kopieën (`#is_nether`, `#has_structure/bastion_remnant`), vanilla-spacing. Geen nether fossils (alleen soul sand valley). Een gegarandeerde fortress per seed volgt in fase 3 (backlog).
- Vanilla Nether-ores (quartz, goud, ancient debris).
- **Sleutelmateriaal: zielkristal** — nieuw, zeldzaam erts in de crimson-bodem en netherrack, alleen in Nether-ring 1. Nodig voor bewaker 1→2 (fase 5); gebruik in fase 2.
  - **Blokken (change 15):** twee varianten: `soul_crystal_ore` (netherrack met kristal) en `crimson_soul_crystal_ore` (crimson nylium met kristal; change 16 vervangt er crimson nylium mee). Beide: hardheid 3 zoals Nether-kwartserts, **ijzeren pickaxe** nodig (`needs_iron_tool`), XP 3–7, **licht 3**, geen particles. De crimson-variant heeft nylium-geluid en -kaartkleur, maar geen nylium-gedrag (spreidt niet, geen bonemeal) en zit **niet** in `minecraft:nylium`: er groeien geen fungi of roots op, de kale plek is een vindplaats-hint.
  - **Drop:** 1 `soul_crystal` (Fortune III: 1–4); Silk Touch → de eigen variant (zonder XP). Ook de crimson-variant laat zonder Silk Touch niets anders achter (geen netherrack); het blok wordt lucht. `soul_crystal` is tot fase 2 een placeholder-item.
  - **Model:** tweelaags (vanilla netherrack / crimson nylium als **verwijzing**, eigen transparante overlay erover, cutout); overlays `soul_crystal_ore_overlay`, `crimson_soul_crystal_ore_overlay_top`/`_side` uit het texturescript.
  - **Textures (change 17, definitief):** zielblauw/cyaan, palet `0B3446 17758C 2DB8C8 5FE3E6 A8F7F4 E6FFFF` (omlijning → highlight). Netherrack-overlay (ook de onderkant van de crimson-variant): 3–4 rechtopstaande clusters van 1–3 scherven; crimson-bovenkant: kristalpunten van bovenaf gezien; crimson-zijkant: clusters onder de bovenste 6 rijen (de rode nylium-rand blijft zichtbaar). Overlays emissive en pulserend (§1.4), beide ertsen gelijk op. Item `soul_crystal`: drie kristallen naast elkaar (zonder overlap), dezelfde puls.
  - **Plaatsing (change 16):** alleen in Nether-ring 1, via biome modifiers (`NetherRing1Features`, `Ring1BiomeModifiers`), stap `underground_decoration` (zoals vanilla's Nether-ores), dus vóór de fungi: de crimson-ertsvloer blijft kaal.
    - **Aders** in alle drie de kopieën (`#is_nether_ring_1`): één `minecraft:ore`, grootte 4, 2 per chunk, Y 10–117 (absolute ankers), `discard_chance_on_air_exposure` 0 (zichtbaar in wanden en vloer). Netherrack → `soul_crystal_ore`, crimson nylium → `crimson_soul_crystal_ore`; basalt, blackstone en warped nylium niet.
    - **Crimson-vloer** alleen in crimson forest (`#has_feature/crimson_soul_crystal_ore`): nylium is één laag, dus de aders raken hem zelden (gemeten ~1 per 15 crimson-chunks). Daarom een kleine ader (grootte 3, alleen crimson nylium → crimson-variant): in de helft van de chunks 20 pogingen, elk vanaf een willekeurige Y 32–127 omlaag naar de dichtstbijzijnde crimson-vloer (`environment_scan`, max. 32 blokken). De scan stopt ook op het crimson-erts zelf, zodat het resultaat niet afhangt van de generatievolgorde (`count_on_every_layer` deed dat wel).
    - **Gemeten** (seed 20260930, 900 chunks rond 0,0 in Nether-ring 1): gemiddeld **2,39 erts per chunk** (crimson forest 2,89, nether wastes 2,23, warped forest 2,24); 71 % van de chunks heeft er minstens één. Crimson-variant: **0,67 per crimson-chunk**, in een derde van de crimson-chunks minstens één (soms 5–7 bij elkaar). Nether-ring 2 en de overworld: 0. Twee verse werelden (de tweede in omgekeerde volgorde geladen): per chunk identiek.
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
- **Uitwerking (change 31, gebouwd)** — overgangsbiome 2→3 `dry_riverbeds`; startwaarden voor de W-check in teststop 3:
  - **Biome:** `transitions.ring_2` → `fixed dry_riverbeds`, de hele zone (~110–180 blokken met de grensruis). Ring 2-worldgen (zelfde vanilla-features, ertsen via `#is_ring_2`, carvers, spawns als canyonlands, grotgeluid ring 2); lucht/fog/water `Palette.RING_2` 30 % naar grijs (placeholder; definitief in change 32). `is_ring_2` betekent "biomes met ring 2-worldgen": systemen die later alle ring 2-biomes via de tag kiezen, krijgen de zone mee. Geen `minecraft:`-tags (geen vanilla-structures tot fase 3). De Nether-grens 2→3 blijft hard.
  - **Beddingen (terrein, puur JSON):** een meanderend netwerk op eigen ruis (`dry_riverbeds_channel`, ~1 bedding per 200 blokken): bodem ~20–30 blokken breed en 5 diep, oevers met twee treden, 3–6 diep ten opzichte van de oever. Het zonemasker is het smoothstep-gewicht van ring 3 als density function (`ring_weight/overworld/ring_3`, `ring_blend` met getallen, geen Java): de beddingen beginnen kort na de binnenrand van de zone, zijn vol tot 80 % en doven uit vóór ring 3 zelf. De snede zit in **beide** terreininvoeren (ring 2 én de placeholder van ring 3, in de eenheden van hun eigen gradiënt): de ring 2-invoer verzadigt in de zone vaak (het gemengde oppervlak ligt dan ver van ring 2's eigen hoogte), dus een snede alleen in ring 2 is daar onzichtbaar. Nooit onder Y 66 in de ring 2-term; geen water in de beddingen. Op lage terreinregio's (zout, duindal, oase-vlakte) is er in het ring 2-deel van de zone alleen een lint gebarsten klei via de surface rule (dezelfde ruis); verder in de zone liggen die regio's door ring 3 op Y ~100–125 en snijdt de bedding gewoon.
  - **Poelen vallen droog** met een eigen steile ramp (0 → 1 over de eerste 3 % van het ring 3-gewicht), niet met het beddingenmasker: geen water boven Y 60 in de zone, ook niet in de eerste blokken. Ondergrondse reservoirs blijven.
  - **Bodem:** bedding gebarsten klei (grindvlekjes), oevers en vlekken grove aarde (~30 %), rood zand eromheen, `scorched_sandstone` op steile oevers; kloofwanden die de zone in lopen houden de kloofsteenlagen. Harde bodemwissel aan de zonegrens (zoutkorst/oasegras stoppen); de biome-kleur van change 32 moet die rand maskeren.
  - **Geblakerd richting ring 3:** vlekken van `blackstone`/`basalt`/`scorched_sandstone` (placeholders tot de as van change 38/39) via `ring_chance` (`transition`), 0 aan de ring 2-kant → dicht aan de ring 3-kant.
  - **Leven ("levendig" is de norm):** half begraven fossielen (bestaande `surface_fossil`, ~1 per 25 chunks), losse botten (~1 per 5 chunks), keien van cobblestone/andesiet/blackstone op de oevers, dode bomen (met grove aarde onder de stam) langs bedding en oevers (dunner richting ring 3), dorre struiken/dode struiken/tonnetjescactus, droog gras op grove aarde en cactusgroepjes op zand. Alles chunk-lokaal en per seed deterministisch.
  - **Ertsen:** de ring 2-set via `#is_ring_2` (alleen in de ring 2-helft van de zone); de ring 3-helft volgt in change 41.

**Unieke blokken (placeholders eerst)**
- **Kloofsteen-set**: gelaagde canyonsteen in 3 tinten + cobble, bricks, slab, stairs, wall.
- **Zout-set**: zoutkorst (bovenlaag zoutvlakte), zoutblok, gepolijst zout, zoutkristal (decoratie, zwak licht).
- **Palm-set**: log, wood, planks, leaves, sapling + houtset (deur, trapdoor, hek, poort, trap, slab, knop, drukplaat); eigen tree feature.
  - **Definitie "houtset"** (geldt voor elke set: dood hout, palm, druipzwam, verkoold, rijpzwam, cipres): deur, valluik, hek, poort, trap, plaat, knop, drukplaat, plus **gestripte log en gestript wood** als vast onderdeel (bijl werkt zoals vanilla, via `StrippableLogBlock`). Geen borden, hangborden of boten (zie backlog).
- **Woestijnflora + scorched sandstone**: scorched sandstone-set, gebarsten klei, dorre struik, tonnetjescactus, woestijnbloem.
- **Uitwerking (change 21)** — 29 blokken, placeholders uit `Ring2Textures.java` (definitief in change 37):
  - **Kloofsteen-set (11):** `red_`, `orange_`, `cream_canyon_stone` (drie aparte natuurlijke tinten; change 23 legt er lagen mee, geen harde laaglijn in de texture), `canyon_cobblestone` (gemengde tinten) en `canyon_stone_bricks` (rood), elk met trap/plaat/muur. Tinten droppen kloofkeien (Silk Touch → tint); kloofkeien tellen voor stenen tools, oven (`#stone_tool_materials`, `#stone_crafting_materials`) en `c:cobblestones`. Tag `corruptedadventure:canyon_stones` (de tinten) voor surface rules en ertsdoelen (24/25). Niet in `#base_stone_overworld`; tinten, keien, natuurlijke scorched sandstone, gebarsten klei en los zand wel in `#overworld_carver_replaceables`.
  - **Scorched sandstone-set (10):** zoals vanilla-zandsteen (`scorched_sandstone` + trap/plaat/muur, `cut_` + plaat, `chiseled_`, `smooth_` + trap/plaat); dropt zichzelf; `c:sandstone/*`.
  - **Los zand** `loose_sand`: ziet eruit als zand, valt als zand (vanilla `ColoredFallingBlock`), en stort in als een **speler** (geen mob, geen spectator; creatief wel) er binnen ~2 blokken onder komt: elke 10 ticks per speler een vak van footprint ±1 × (bovenkant hoofd … +2) → stof (`falling_dust`) + kraakgeluid → na 15 ticks (0,75 s) de vanilla-val, buren volgen vanzelf. Dropt `minecraft:sand` (Silk Touch → zichzelf; een vallend blok dat niet kan landen, bijv. op een fakkel, dropt zichzelf zoals vanilla — geaccepteerd: geplaatst los zand kan nooit hangen); niet in `#minecraft:sand`. Hangen kan alleen zo uit worldgen (geen blok-updates); change 26 zet het als zandgrotplafond.
  - **Woestijnflora:** `cracked_clay` (schop, dropt zichzelf, packed-mud-geluid, niet in `#dirt`, ook in `#dead_bush_may_place_on`), `dry_shrub` (als dode struik: shears → zichzelf, anders 0–2 stokken; brandbaar), `barrel_cactus` (10 × 10 × 10 px, vorm definitief; 1 schade bij echte aanraking, groeit niet, mobs mijden hem), `desert_flower` (`#small_flowers`, stew Fire Resistance 4 s; brandbaar) + drie bloempotten (geen item). Alle drie de planten groeien op `corruptedadventure:desert_plant_soil` = `#sand`, `#terracotta`, `#dirt`, `cracked_clay`.
- **Uitwerking palm-set (change 22)** — 16 blokken, placeholders uit `Ring2Textures.java` (definitief in change 37):
  - **Houtset:** `palm_log`, `palm_wood`, `stripped_palm_log`, `stripped_palm_wood`, `palm_planks`, `palm_stairs`, `palm_slab`, `palm_fence`, `palm_fence_gate`, `palm_door`, `palm_trapdoor`, `palm_pressure_plate`, `palm_button` (zoals de dood-hout-set: eikengedrag en -geluid via `BlockSetType` `corruptedadventure:palm`, bijl strip, vuur als eik). Uiterlijk: grijsbruine schors met horizontale ringen, licht zandgeel hout zonder jaarringen (kaartkleur `TERRACOTTA_BROWN` / `SAND`). Geen borden, hangborden of boten.
  - **Bladeren** `palm_leaves`: vaste heldergroene kleur in de texture (**geen biome-tint**), `cutout_mipped`, vanilla-verval op `#logs`; loot zoals eikenbladeren (schaar/Silk Touch → bladeren, anders zaailing 1/20 met Fortune hoger en stokken 1/50); brandbaar en composteerbaar als vanilla-bladeren.
  - **Zaailing** `palm_sapling`: groeit op `#dirt` (en farmland) **en** `desert_plant_soil` (zand, rood zand, terracotta, gebarsten klei) — dezelfde grondregel als de woestijnflora. Potplant `potted_palm_sapling` (geen item). Vóór change 23 bestaat de feature `corruptedadventure:palm_tree` nog niet: bonemeal wordt verbruikt, de zaailing gaat naar stadium 1 maar groeit niet, geen crash. Change 23 registreert de feature onder precies die key.
- **Uitwerking zout-set (change 29)** — 7 blokken + placeholder-item, placeholders uit `Ring2Textures.java` (definitief in change 37):
  - **Blokken:** `salt_crust` (zoutkorst), `salt_block` (zoutblok), `polished_salt` (gepolijst zout) met `polished_salt_stairs`/`_slab`/`_wall`, `salt_crystal` (zoutkristal). Kaartkleur korst `SNOW`, de rest `QUARTZ`. Niet brandbaar.
  - **Zoutkorst:** vol blok dat **niet valt** (de vlakte ligt boven grotten), sterkte en geluid als zand, geen tool nodig, schop sneller. Dropt **4 × `salt`** (geen Fortune, zoals klei → kleiballen), met Silk Touch zichzelf. Zijkant = zoutblok met een korstband van 2–3 px bovenaan, bodem = zoutblok.
  - **Zoutblok en gepolijst zout:** als calciet (0,75, pickaxe nodig, calcietgeluid); droppen zichzelf. Gepolijst zout en zijn vormen hebben tot fase 2 geen survival-bron (recipes in `items`).
  - **Zoutkristal:** vanilla-gedrag van de amethistcluster (`AmethystClusterBlock(7, 3)`, vorm definitief): op alle zes zijden, waterlogbaar, breekt zonder stevige steun; **licht 3**, geen emissive (dat beslist change 37); dropt zichzelf. Kruismodel cutout, plat item. Een projectiel-treffer geeft het amethist-belgeluid (geaccepteerd).
  - **Tags:** korst en zoutblok in `#overworld_carver_replaceables` (grotten en kloven snijden erdoor); zout in **geen** plant- of grondtag (`#sand`, `#dirt`, `#base_stone_overworld`, `desert_plant_soil`, `#dead_bush_may_place_on`, `#animals_spawnable_on`): flora groeit niet op zout, de zoutvlakte blijft kaal. Item `salt` in `c:dusts/salt` (in `c:dusts`).
  - **Overdracht naar change 30 (`salt_flats`):** surface rule `corruptedadventure:salt_crust` bovenop, `corruptedadventure:salt_block` eronder (diepte kiest 30); kristallen via een feature met `salt_crystal` in de standaardstaat `facing=up` (filter: korst eronder, lucht erop; `would_survive` werkt). Dierenspawns en eventuele flora op de vlakte beslist change 30; kristallen houden in hun buurt monsters weg (bloklicht > 0).

- **Uitwerking terrein en kernbiomes (change 23)** — `canyonlands` en `oasis_flats`; terrein in `density_function/ring_2/overworld/`, waarden zijn startwaarden voor de terreincheck (teststop 2):
  - **Regio-assen:** `relief` (rotsig ↔ vlak; ruis `ring_2_relief` + 0,035) en `moisture` (nog ongebruikt). `canyonlands` = relief ≥ 0, `oasis_flats` = relief < 0 (`multi_noise` op `temperature`). Gemeten (seed 20260930): canyonlands 59 % van ring 2, op r = 2.500 50 %; vlekken langs r = 2.500 gemiddeld ~950 blokken (max ~2.200). De offset geeft ~55 % gemiddeld over seeds. Tot change 30 is er naar verhouding veel oase (~45 %).
  - **Indeling sinds change 30:** rotsig → `canyonlands` (vochtig/midden) en `hoodoo_fields` (droog); vlak → `oasis_flats` (vochtig), `dune_sea` (midden), `salt_flats` (droog), via `humidity`-bereiken op `moisture` (zie Uitwerking change 30); de offset 0,035 op `relief` is vervallen (rotsig : vlak = 50 : 50).
  - **Hoogtes (keuze gebruiker, diep):** vlakte Y 66–74 (70 ± 4), poelen tot Y 59 (water tot zeeniveau 63), plateau Y 125–139 (132 ± 7), kloofbodem Y 67–71; buiten de poelen nergens onder Y 66 (geen zeewater in de kloven).
  - **Steilrand** vlakte ↔ plateau (~55–70 blokken): `relief_weight`, 3 steile stappen met 2 treden; de biomegrens ligt halverwege. **Kloven:** meanderende hoofdkloven (`ring_2_canyon`) met zijtakken (`ring_2_canyon_branch`) die vanaf de hoofdkloof ondieper worden en in het plateau eindigen; bodem ~10–40 blokken breed, 4 steile wanden van ~15 blokken met 3 treden van ~6 blokken, bovenrand ~60–110 breed. Lichte 3D-ruis maakt kloofwanden en steilranden ruw. Treden smaller dan ~4–6 blokken verdwijnen door de interpolatie van het terrein (cellen 4 × 8 × 4).
  - **Grotten:** vanilla noise-grotten, ingangen en noodles (de ring 2-invoer is vanilla's final density met het eigen terrein) + vanilla carvers (sinds change 26 alleen diep, zie Grotten). Overgang 1 → 2 mengt vanilla en het plateau: steil (tot ~75 blokken); 2 → 3 mengt met placeholder ring 3 (Y ~120).
  - **Kloofsteenlagen:** horizontale banden van 3–6 blokken, sinds change 23b (keuze gebruiker) **rood 22, oranje 12, crème 14 per periode van 48 in 4 crème banden van 3–4** (was rood 28 / oranje 12 / crème 8 in 3 crème banden), vanaf Y 8 tot het oppervlak in canyonlands (en in `oasis_flats` vanaf Y 74: de oasekant van de steilrand); de grenzen golven een paar blokken mee met de surface-diepte; beide kloofwanden tonen dezelfde lagen.
  - **Oppervlak canyonlands:** rood zand op plateau en treden; kloofbodem rood zand met gebarsten klei (drooggevallen beekjes) en grind; rode zandsteen eronder en onder plafonds; steile vlakken tonen de lagen. **Oasis_flats:** zand (zandsteen op steile vlakken en vanaf ~6 diep), zand onder water, gras alleen op de lage oever (boven water, onder Y 66).
  - **Oase:** poelen uit het terrein (ruis `ring_2_pools`), ~5 % van de oase is water (change 23b: ~6 %, zie hieronder); palmen vooral aan de oever (24 pogingen per chunk op gras), verspreid ook op het zand (1 per 4 chunks); kort gras, varens, woestijnbloemen op de oever; vanilla-suikerriet.
  - **Palm** (`corruptedadventure:palm_tree`, ook uit de zaailing): stam 6–10, eerste derde recht, dan 1–2 blokken overhellend; kroon = topblad + 4 rechte bladwaaiers van 2–3 met een hangend topje + 2–4 diagonale waaiers één laag lager; elk blad ≤ 5 stappen van de stam (vanilla-verval 6); zand onder de stam.
  - **Canyonlands-flora:** dorre struik (~2,7 per chunk), dode struik, tonnetjescactus (~0,45), woestijnbloem (zeldzaam).
  - **Spawns tot fase 4:** vanilla-woestijn ('s nachts monsters met vooral husks, konijnen), in de oase ook kamelen (op gras).
  - **Tags:** `corruptedadventure:is_ring_2`, `c:is_hot/overworld`, `c:is_dry/overworld`; geen `minecraft:`-tags (geen vanilla-structures tot fase 3).
  - **Sfeer:** lucht/fog/water uit `BiomeAmbience.Palette.RING_2` (placeholder: lucht `9FC4E8`, fog `E8D7A8`, water `32A598`); geen regen; definitief in change 32.
- **Uitwerking terreinvormen (change 23b)** — na teststop 2 (het plateau was te vlak en te makkelijk over te steken). Alles density functions en surface rules, puur per seed; aankleding met features volgt in change 23c. Alle maten staan in JSON (`ring_2/overworld.json` en `ring_2/overworld/*`), startwaarden voor teststop 2b (na change 30):
  - **Spleetkloven** (`cell_edges` + `canyon_details`): breuknetwerk van gejitterde cellen van ~110 blokken met meanderende breuken (domain warp 14), ~60 % van de randen open; dichte randen zijn landverbindingen tussen de plateau-eilanden. Per open rand: breedte 3,5–6, diepte 25–45 onder het plateau (bodem nooit onder max(Y 70, kloofbodem + 4)), onderin 1–2 breder, lichte golving van de wand. Om de ~70 blokken (± 15) langs een rand een overdekt stuk van 3–6 lang (de bovenste 3–6 blokken blijven staan) of (30 %) een klemblok van 2–3 dik halverwege de diepte. Alleen op het plateau (niet in de hoofdkloven, niet in de zones); waar een spleet een hoofdkloof raakt, mondt hij hangend uit in de wand. Spleten snijden ook door tafelbergen.
  - **Tafelbergen:** getrapte spline op `ring_2_mesa` (vlekken ~150–300): +12 / +24 / +36 boven het plateau, wanden van ~10–16; ~32 % van het plateau ligt boven +0, ~22 % van de plateaukolommen ≥ Y 145, toppen tot ~Y 174. De treden lopen af naar kloofranden (~20 blokken), steilrand en ringzones (geen tafelberg tegen een kloofwand).
  - **Kloofbodem:** treden van 6 (0 / +6 / +12 / +18) op `ring_2_canyon_floor` met een droge kolk (−3, nooit onder Y 66) aan de voet van elke valwand; hangende zijtakken (bodem op ~30 % van de wandhoogte, valwand bij de monding); **puinhellingen** (~1:1, `ring_2_talus`) op ~20 % van de wanden, oppervlak grind met rood zand. Kloofbodems Y 66–89.
  - **Rotsvormen** (`rock_pillars`, generiek, ook voor de hoodoos van change 30): plateau-naalden (8–20 hoog) en half begraven keien (straal 2–4), samen ~0,45 per plateau-chunk (cel 16); vinnen (5–25 lang na inpassen in hun cel van 24, 2–4 dik, 6–18 hoog, per gebied evenwijdig) ~0,2 per plateau-chunk, ~19 % met een gat (boog); torens van 25–50 hoog in brede kloofbodems, ~1 per 120 kloof-chunks. Nooit zwevend: elke kolom loopt door tot de laagste vloer − 2 én tot Y 50 (door grotten onder de voetafdruk heen); niet in spleten, niet op wanden of treden.
  - **Kloofwanden** (`canyon_details`): richels van 1–3 op de crème banden (~50 % van de wandlengte), nissen (4–7 hoog, 3–8 diep, vlakke vloer ≥ Y 66) in de band eronder, overhangende kaprots van 2–3 aan de plateaurand met een smalle ondergraving. Alleen op steile wanden (helling ≥ 2) en niet in het lage, ruwe deel van een wand (de grond 3 blokken vóór de wand ligt ≥ 6 lager dan de richel). Een richel hangt altijd aan zijn wand (zie `CODEBASE.md`, valkuilen).
  - **Oase (keuze gebruiker, 23b-8):** één extra grove octaaf in `ring_2_pools` maakt poelen plaatselijk breder; ~6,3 % van `oasis_flats` is water (was ~4,9 %); de oever rond de waterlijn is vlak (Y 66) en een brede grasrand (mediaan ~10 blokken, was ~3), daarbuiten een overgangsring (gras met `coarse_dirt`, naar buiten zand) uit dezelfde poelruis.
  - **Lagen** (23b-13 B): zie Kloofsteenlagen hierboven; gemeten in 8 kloofchunks Y 60–145: rood 50 %, oranje 22 %, crème 28 %. Oasekant van de steilrand (23b-11 A): steile vlakken en ondergrond boven Y 74 in `oasis_flats` tonen dezelfde lagen.
  - **Kosten:** zie `CODEBASE.md` (meting change 23b); budget noise-stap ring 2 ≤ 1,75× vanilla, gedeeld met changes 26/30/31.
- **Uitwerking aankleding (change 23c)** — features bovenop het terrein van 23b, chunk-lokaal en volgorde-onafhankelijk (determinisme-regels in `Ring2Features` en `CODEBASE.md`); startwaarden voor teststop 2b:
  - **Kloof (23c-3 A, 23c-5 A):** keienvelden op ~21 % van de kloofbodem (`ring_2_boulder_fields` ≥ 0,27; keien 1–3 groot, half ingegraven, de drie tinten + `canyon_cobblestone`, één materiaal per kei), puin (1–2) op de puinhellingen (`ring_2_talus`, Y 88–120), rotsval (losse kloofkei of tint) aan wandvoeten (≥ 6 hoger binnen 3), af en toe een kei op plateau en tafelbergen (1 per 2 chunks). Gemeten in 400 kloofgebied-chunks: 526 keien, 0 zwevend.
  - **Wandbronnen (23c-4 C; vervolg 2026-10-02: A, kerf):** bron 3–8 boven de vloer in een wand die binnen 3 blokken ≥ 8 stijgt, in een uitgehakte kerf van 1 breed (kloofwanden lopen onderaan flauw uit); val als smal gordijn, stroompje van 3–6 (val meegeteld, ≤ 1 knik) in een geul van 1 × 1, kolk 2–3 × 2–3 en 1–2 diep met een rand van mos/gras, varens en suikerriet. Water in vanilla's eindtoestand zonder fluid ticks, alles in de eigen chunk; vloer Y 66–100 (niet op het plateau, niet in spleten). Gemeten ~1 per 10 kloofchunks (14 in 400 chunks met 140 kloofchunks).
  - **Oase (23c-1 A, 23c-2 A, 23c-6 A):** per zone op `ring_2_pools` (dezelfde ruis als de surface rules): aan het water suikerriet 1–3 en hoog gras/(grote) varens (eigen chunk-lokale plaatsingen `oasis_shore_cane`/`oasis_shore_plants` i.p.v. vanilla's `random_patch`); op de grasoever kort gras, varens, woestijnbloemen (10 pogingen); in de overgangsring dorre/dode struiken, spaarzaam kort gras, woestijnbloem en af en toe een zandsteenkei. Palmen in groepjes op de buitenrand van de oever en het binnenste deel van de overgangsring (`ring_2_palm_groves` ≥ 0), struikpalmen (stam 1–2) ertussen, een losse palm op het zand (~1 per 6 chunks); alle palmstammen op één raster (cel 8), ≥ 5 uit elkaar. Gemeten: gemiddeld 2,3 palmen per poel (18 poelen), 0 stammen dichter dan 5 bij elkaar. Palmvorm ongewijzigd (23c-7 A).
  - **Kosten:** zie de bouwnotities van plan 23c (features-stap en hele chunk op de meetplekken van 23b).
- **Uitwerking extra biomes (change 30)** — `hoodoo_fields`, `salt_flats`, `dune_sea`; alles density functions (JSON), surface rules en datagen; alle maten zijn startwaarden voor teststop 2b:
  - **Indeling op twee assen** (`multi_noise` van ring 2; drempels = percentielen van de stationaire ruis, `Ring2RegionSharesTest`):

    | Biome | `temperature` (relief) | `humidity` (moisture) | Doel | Gemeten (seed 20260930, cirkels r 2.000/2.500/3.000) |
    |---|---|---|---|---|
    | `canyonlands` | [0, 2] | [−0,073, 2] | 30 % | 35,9 % (hele ring 36,5 %) |
    | `hoodoo_fields` | [0, 2] | [−2, −0,073] | 20 % | 15,9 % (18,4 %) |
    | `salt_flats` | [−2, 0] | [−2, −0,185] | 13 % | 12,4 % (12,7 %) |
    | `dune_sea` | [−2, 0] | [−0,185, 0,204] | 25 % | 22,9 % (20,7 %) |
    | `oasis_flats` | [−2, 0] | [0,204, 2] | 12 % | 12,9 % (11,6 %) |

    Op de vochtas grenzen alleen buren: zout — duin — oase (de oase ligt in de duinen en raakt het zout nooit); de hoodoos grenzen aan zout en duinen, de kloven aan duinen en oase. Per wereld wijken de aandelen een paar procentpunt af (ring 2 bevat maar enkele tientallen regio's van ~500–1.000 blokken); op seed 20260930 is rotsig : vlak 55 : 45 en komen de kloven ~6 pp boven hun doel (stationair, over 3 seeds, binnen ±3 pp). In de zone 2→3 mengt de router `relief` en `moisture` met 0; de grens op relief 0 blijft exact, de vochtgrenzen schuiven daar iets (change 31).
  - **Hoogtes:** zoutvlakte Y 66 (bovenste blok 65, kurkdroog); duinen Y ~66–100 (basis 67 ± 2, amplitude 10–32 per gebied); hoodoovloer Y 85–91 (88 ± 3) onder het plateau, met een steilrand van ~45 blokken en twee treden (`hoodoo_weight` op `moisture`, dezelfde terrasopbouw als `relief_weight`); vlakke biomes onderling geleidelijk (± 0,03 op `moisture`), de oasepoelen alleen binnen de oase.
  - **Duinen:** richtingloze ruggen: scherpe kam waar |ruis| = 0, brede troggen (spline op |`shifted_noise`| met domain warp, `xz_scale` 1,4: kammen ~70–100 blokken uit elkaar); de hoogte wisselt per gebied (`ring_2_dune_amplitude`, vlekken ~500 blokken): lage golvende duinen en hoge duinzeeën. Zand tot ~4–12 diep op een zandstenen kern (tot Y 60); geen ruwe wanden.
  - **Hoodoos:** `rock_pillars` per blok buiten de interpolatie, twee overlappende pilaarrasters (cel 17 en 23, kans 0,75; één raster van 12 gaf zichtbare rijen) + bogen (cel 24, kans 0,12). Pilaar = naald van 15–45 hoog (× gewicht), voet 2,5–4 + uitloper 0,5–1, hals 1,5–2,5, 35 % met een balancerende kap (hals + 1–2, 3–5 dik); boog = vin van 10–16 lang, 3 dik, 10–16 hoog met een gat. Pilaarkolommen ~11 % van het hoodooveld. In de overgangszones vervallen ze (kernmasker). Materiaal: kloofsteenlagen (het lagenblok loopt door van de kloofwand naar de pilaren) met een kaprots van geschroeide zandsteen boven Y 94 (toppen, kappen, bogen en hun onderkant; ook de bovenste trede van de steilrand); vloer rood zand met gebarsten klei en grind, rode zandsteen eronder. Tafelbergen, spleten, plateaurotsen en kloofbodemtorens blijven weg uit het hoodooveld en van zijn steilrand; de kloven lopen er via terrassen in uit.
  - **Zoutvlakte:** zoutkorst, 2–4 zoutblok, 3 zandsteen, dan steen; aan de duinkant zandvlekken over de korst (geleidelijke rand); steile randen zandsteen of (boven Y 74) lagen.
  - **Features en spawns:** zout: een zoutkristal per ~1 chunk (op de korst, licht 3), alleen monsters (en vleermuizen), geen dieren; duinen: ~0,2 dorre/dode struik per chunk, spawns als de kloven; hoodoos: de kloofflora (~1,5 per chunk) en rotsval aan de pilaarvoet, spawns als de kloven. Alles chunk-lokaal (`jittered_grid`). **Bijgewerkt in change 30c:** de duinen zijn niet meer vrijwel kaal (begroeide dalen, kale kammen), de hoodoos en zout-eilandjes hebben meer flora; zie "Uitwerking begroeiing en aankleding (change 30c)".
  - **Zonnesteen in de hoodoos** (randvoorwaarde change 24): de pilaren en kappen bestaan uit kloofsteen-tinten en geschroeide zandsteen in het zonnesteenbereik (Y 95–180); gemeten 25–47 blokken per 25 hoodoo-chunks (~1–2 per chunk, deels bloot in pilaarwanden; kloven ~27 per chunk).
  - **Kosten:** zie `CODEBASE.md` (meting change 30). De ring 2-noise-stap is door een `cache_2d` in het preliminary surface goedkoper geworden dan vóór change 30.
- **Uitwerking zout- en duinelementen (change 30b)** — na teststop 2b (zout en duinen te kaal); vormen en water, de begroeiing volgt in change 30c. Alles puur per seed: een eigen surface-conditie, één terreinterm en chunk-lokale features (`jittered_grid` → `WORLD_SURFACE_WG` → filters → `biome`; determinisme-regels van 23c). Startwaarden voor teststop 2c:
  - **Barstenpatroon (30b-2 A):** eigen surface-conditie `corruptedadventure:polygon_edge`: een Voronoi-patroon van een driehoeksrooster met jitter 0,3 (= licht onregelmatige zeshoeken), cellen 7,5 blokken, randen van precies 1 blok breed als gesloten lijn, vlak in de korst (`salt_block` i.p.v. `salt_crust`; niet in de zandvlekken en niet op de eilandjes). Gemeten 22,9 % randkolommen (doel 15–25 %; met breedte 0,8 vielen gaten in de lijnen, daarom breedte 1 en een iets grotere cel dan 7). Kosten ~0,08 ms per zoutchunk.
  - **Zout-eilandjes (30b-6 A):** in `flat_height` wordt de zoutterm `(66 + s) · salt_weight` met `s` = 0 → 2 over `ring_2_salt_islands` 0,50–0,62 (flauwe rand). Surface rule: ≥ 0,46 zand (een zandrand op de korst rond de verhoging), ≥ 0,55 met surface-ruis > 0,3 rood zand, eronder zand. Gemeten 4,8 % van de zoutvlakte verhoogd (bovenste blok Y 66–67; 5,4 % zand), eilandjes ~30–40 breed (met zandrand tot ~60). Rotsjes: `rubble` zandsteen/geschroeide zandsteen/rode kloofsteen 1–3, raster 8 kans 0,3.
  - **Pekelpoelen (30b-1 A):** feature `surface_pool` (`brine_pool`): rafelige schijf straal 2–5, 1 diep bronwater in de korst (waterspiegel Y 65 = gelijk met de vlakte), witte zoutblokbodem, korstrand van 1 met kristallen (0,12), af en toe een kristal onder water (`waterlogged`, 0,04). Gesloten kom: geen fluid ticks, geen stromend water. Raster 16 kans 0,15: gemeten 36 poelen in 256 zoutchunks (~1 per 7), 9–53 waterkolommen, allemaal bronnen, 0 water op een chunkrand. **Wijkt bewust af van 30-3 A ("geen water" in het zout).** Te ondiep om af te koelen (uitdroging telt ≥ 1 blok water boven de voeten); scheppen met een fles kan (backlog `[conditions]`).
  - **Zoutpilaren (30b-3 A):** feature `outcrop` (`salt_pillar`): 1–3 toelopende schoorstenen van zoutblok, 2–6 hoog, soms een kristal erop (0,3). Velden waar `ring_2_salt_pillars` ≥ 0,4 (gemeten 11,5–12 % van de vlakte; raster 8 marge 2 kans 0,5) + los (raster 16 kans 0,06). Gemeten in 256 zoutchunks: 142 pilaargroepen in 74 chunks, nooit zwevend.
  - **Kristalvelden (30b-3 A):** `salt_crystal` op raster 4 kans 0,35 waar `ring_2_salt_crystal_fields` ≥ 0,4 (gemeten ~10 % van de vlakte; 0,35 gaf 13 %), na de losse kristallen; ~5–13 kristallen per veldchunk. 's Nachts zwak licht (3), geen monsters ernaast.
  - **Rotsuitstekels in de duinen (30b-4 A):** feature `outcrop` (`dune_rock`), geen density function (materiaal precies zoals bedoeld, 0 kosten in de noise-stap): 2–5 knobbels, straal 1,5–3,5, tot 12 boven de grond (absolute top: bergop deels begraven, bergaf hoger), 2 ingegraven, ≤ 11 breed gemeten; palet zandsteen/glad zandsteen/geschroeide zandsteen (3) of rood/oranje/crème kloofsteen (2) in banden van 2–3; kans 0,35 op een kraag (schaduw). Raster 16 kans 0,1: gemeten 20–27 rotsen per 256 duinchunks (~1 per 10–13), ~40 % met een kraag. **Wijkt bewust af van 30-8 A ("geen herkenningspunten")**: zeldzame oriëntatiepunten.
  - **Verborgen mini-oases (30b-5 A):** `surface_pool` (`dune_oasis`): water ~3 × 3 tot ~6 × 6, kern 2 diep (afkoelen kan), rand van 2 gras met kort gras/varens/hoog gras/woestijnbloem (0,5), suikerriet 1–2 (0,3), 1–2 palmen (palm 3 / struikpalm 1) op de buitenrand, stammen ≥ 4 van de chunkrand en ≥ 5 uit elkaar. Alleen bij hoge duinen (`ring_2_dune_amplitude` ≥ 0,15, ~32 % van de duinen) op een dalbodem (bovenste blok ≤ Y 71, relief ≤ 2 binnen 4); kolommen tot 2 boven de waterspiegel worden afgegraven. Afgestemd: alleen ~5 % van de hoge-duinkolommen haalt de dalfilters, dus raster 16 kans 0,25 (startwaarde 0,08 en bodem ≤ Y 68 gaven 0 oases in 512 duinchunks). Gemeten: 18 oases in 768 chunks met (vooral) hoge duinen (~1 per 40), water Y 64–70, 14 met 1–2 palmen, kleinste stamafstand 5,8.
  - **Volgorde:** zout `LOCAL_MODIFICATIONS` = poelen, pilaarvelden, losse pilaren, eilandrotsjes (pilaren en rotsen vallen af op water); `VEGETAL_DECORATION` = kristallen, kristalvelden. Duin: oases, dan rotsen (vallen af op water of gras). Elke placed feature hoort bij één biome.
  - **Toetsen (test S):** water blijft staan (10 poel- en 5 oasechunks, blok op elk randwater gezet en weggehaald: identiek, geen water in de buren); geen zwevende blokken van eigen vormen; twee werelden in omgekeerde laadvolgorde gelijk.
  - **Kosten:** zie `CODEBASE.md` (meting change 30b).
- **Uitwerking begroeiing en aankleding (change 30c)** — na teststop 2b (oase, duinen, zout en hoodoos voelden kaal); antwoorden 30c-1 t/m 30c-6 allemaal A (matig: levendig, nog steeds woestijn). Alles chunk-lokaal en per seed (`jittered_grid` → `WORLD_SURFACE_WG` → filters → `biome`; groepjes uit geseede ruis via `noise_band`, geen `random_patch`). Startwaarden voor teststop 2c; gemeten op seed 20260930 (256 chunks per gebied, drie gebieden met hoge en één met lage duinen):
  - **Duindalen begroeid, kammen kaal (30c-1 A, 30c-3 A):** harde dalbodem in de surface rule van `dune_sea` (niet steil, bovenste blok onder Y 70): `minecraft:surface` > 0,4 → grove aarde, < −0,5 → gebarsten klei, 1 diep (gemeten 5–20 % van de dalbodem ≤ Y 69 per gebied, gemiddeld ~12 %). Planten: dal (bovenste blok ≤ Y 71) dorre struik 6 / dode struik 4 / tonnetjescactus 2 / woestijnbloem 1 (raster 4 kans 0,25) + droog gras (kort gras 4 / hoog gras 1) op de grove aarde, met de droge grastint van de duinen; flank (Y 72–78) de oude duinflora (raster 8 kans 0,2); kammen (> Y 78) niets. Gemeten ~4,4–5,3 planten per 256 dalkolommen (= per volle dalchunk; zonder de mini-oases, met 7–9), lage duinen 4,7 (was 0,2), flank 0,6–1,2, kam 0 (was 0,2).
  - **Cactusgroepjes (30c-6 A):** `cactus_column` 1–3 hoog (meest 1) op raster 4 met marge 1 (≥ 3 uit elkaar, alle buren in de eigen chunk) kans 0,85, waar `ring_2_cactus_groups` ≥ 0,4 (firstOctave −5: vlekken ~10–25 breed, ~9 % van het land), bovenste blok ≤ Y 74, `would_survive`. Gemeten 34–43 cactussen per 256 duinchunks, meest in groepjes van 2–5 (losse ook), eens per ~250 chunks een veld (gemeten één van 29 in een breed dal); hoodoos dezelfde ruis ≥ 0,45, kans 0,5 (118 per 256 chunks, op rood zand).
  - **Fossielen en botten (30c-5 A):** eigen feature `surface_fossil` (`SurfaceFossilFeature`): vanilla-sjablonen `fossil/spine_1…4`, `skull_1…4` (alleen botblokken, max. 13 breed: past in elke rotatie in een chunk met 1 marge), 0,4–0,7 van de hoogte onder de laagste grond van de voetafdruk (afgerond op hele lagen: 1 van 3 tot 3 van 4), verweerd (85 % per blok; brokken die niet meer met de grond verbonden zijn vallen weg: ~78 % blijft), alle voetafdrukkolommen zand. Duinen: raster 16 kans 0,2 in dal en lage flank (≤ Y 76): gemeten ~1 per 26 duinchunks, allemaal in hun chunk, 0 zwevend. Losse botten (`bone_rubble`: botblok liggend x/z 2, staand 1, grootte 1–2, half ingegraven): raster 8 kans 0,3, ≤ Y 74: ~1 per 5 duinchunks. Zout-eilandjes: fossiel elke chunk één poging (alleen ~5 % is eiland; gemeten 6 per 256 zoutchunks), botten raster 8 kans 0,15. Vanilla's `FossilFeature` is onbruikbaar: diep begraven en schrijft in buurchunks.
  - **Oase (30c-2 A, 30c-4 A):** dichter gras en varens (kort gras 6 / varen 3 / hoog gras 1 / grote varen 1, ~26 gespreide pogingen per chunk op gras): gemeten ×2,8 planten op de oever t.o.v. change 30. Bloemen in kleurvlekken op een eigen geseede ruis `ring_2_flower_patches` (vanilla's `noise_provider` heeft een vaste seed): warm (klaproos 3 / paardenbloem 2 / woestijnbloem 2) < −0,2, wit (margriet 3 / korenbloem 2 / woestijnbloem 1) tot 0,2, paars (allium 3 / korenbloem 2) daarboven; alleen op de grasoever (poelruis ≥ 0,12); gemeten ~5,4 bloemen per 256 graskolommen, bloemen binnen 6 blokken van elkaar hebben in 71 % dezelfde kleur (willekeurig 34 %). Struiken aan de rand: azalea 3 / bloeiende azalea 1 / rijpe zoete bes 2 in de band 0,05–0,16 van de poelruis (buitenrand oever + binnenste overgangsring), raster 4 marge 1 kans 0,25, ná de palmen (een palm blokkeert een struik, nooit andersom): gemeten 72 per 256 oasechunks, op gras en grove aarde (azalea groeit in 1.21.1 niet op zand). Overgangsring 8 pogingen (was 4). Geen waterplanten en geen bredere groene gordel (sparring).
  - **Hoodoovelden:** kloofflora raster 4 kans 0,4 + flora aan de pilaarvoet (`slope` rise ≥ 8 binnen 2; dorre struik 4 / dode struik 2 / tonnetjescactus 2 / woestijnbloem 1, raster 4 kans 0,6) + cactusgroepjes: gemeten 6,7 planten per chunk (was 1,3), ~27 % aan een pilaarvoet.
  - **Zout-eilandjes:** dorre struik 5 / dode struik 3 / tonnetjescactus 2 (raster 4 kans 0,35) en cactus 1–2 (raster 8 kans 0,2) alleen op zand/rood zand (eilandjes en de zandvlekken aan de duinkant): gemeten ~6 planten per 256 eilandkolommen, 0 op de korst.
  - **Volgorde:** fossielen en botten in `LOCAL_MODIFICATIONS` achter de vormen van 30b (vallen af op water, gras en rots); planten in `VEGETAL_DECORATION` achteraan; oase: bloemen en struiken ná de palmen. Elke placed feature hoort bij één biome; configured features (`cactus_column`, `bone_rubble`, `desert_fossil`) worden gedeeld.
  - **Feature-index:** de nieuwe placed features verschuiven ook in ring 2 de decoratie-seeds van bestaande features (bekende valkuil): pekelpoelen en zoutpilaren liggen op seed 20260930 op andere plekken dan na change 30b; duinrotsen en mini-oases niet.
  - **Kosten:** zie `CODEBASE.md` (meting change 30c).

**Ores**
- **Zonnesteen** (hoofdmateriaal, kandidaat ring 2-gear-tier): gloeiend erts hoog in canyonwanden en hoodoos.
- **Turkoois**: zeldzaam, diep in kloofsteen.
- Vanilla-ores afgebouwd: ijzer en kolen minder dan in ring 1, diamant zeldzaam.
- **Uitwerking (change 24, blokken; plaatsing in change 25):**
  - **Eén ertsblok per gastgesteente** (naam `<gastgesteente>_<erts>_ore`, zoals vanilla `deepslate_iron_ore`), zodat elk erts wegvalt in zijn eigen laag. Zonnesteen: `red_canyon_sunstone_ore`, `orange_canyon_sunstone_ore`, `cream_canyon_sunstone_ore`, `scorched_sandstone_sunstone_ore`. Turkoois: `red_canyon_turquoise_ore`, `orange_canyon_turquoise_ore`, `cream_canyon_turquoise_ore` (geen diepleisteen-variant).
  - **Drop:** ruw materiaal, zoals ijzererts → ruw ijzer: `raw_sunstone` / `raw_turquoise` (placeholder-items; 1 per blok, Fortune III 1–4), Silk Touch → de eigen ertsvariant. Smelten, het gesmolten product en ruwe opslagblokken volgen in fase 2 (`items`, backlog).
  - **Tool en XP:** beide ijzeren pickaxe (`needs_iron_tool`), hardheid als ijzererts (3,0); XP zonnesteen 2–5, turkoois 3–7 (alleen bij breken door een speler, niet met Silk Touch).
  - **Licht/gloed:** zonnesteen bloklicht 3 + emissive overlay (zichtbaar klimdoel in de wanden, ook 's nachts); turkoois geen licht en een matte overlay (moet gezocht worden).
  - **Model:** tweelaags (`oreWithOverlay`): de texture van het gastgesteente als basis (scorched-variant: top/zijkant/bodem) + één overlay per erts, gedeeld door alle gastgesteenten (`sunstone_ore_overlay`, `turquoise_ore_overlay`). Placeholders uit `Ring2Textures.java` (stil; werkpaletten zonnesteen `5A2A08 9A4A0C D9801A F2B23A FFE07A FFF6C8`, turkoois `0E3B3A 1C6B66 2E9C92 4FC6B5 8FE6D6` + ader `3A2A1E`); definitieve textures en de puls van zonnesteen in change 37.
  - **Tags:** `c:ores/sunstone`, `c:ores/turquoise` (in `c:ores`), `c:ore_rates/singular`, `c:raw_materials/sunstone`, `c:raw_materials/turquoise` (in `c:raw_materials`); geen `ores_in_ground`, niet in `#overworld_carver_replaceables` of `#base_stone_overworld`.
  - **Overdracht:** change 25 plaatst per erts één `minecraft:ore` met per gastgesteente een `block_match` → variant (een tag-target kan niet per tint een variant kiezen) en telt via `#c:ores/sunstone` / `#c:ores/turquoise`. Turkoois "diep in kloofsteen" leunt op de kloofsteenlagen van change 23 (Y 8 tot het oppervlak in `canyonlands`). **Randvoorwaarde voor change 30:** hoodoos bestaan uit kloofsteen-tinten of `scorched_sandstone` (vanilla-zandsteen krijgt geen zonnesteen-variant), anders zit er geen zonnesteen in.
- **Uitwerking (change 25, plaatsing):**
  - **Zonnesteen:** één `minecraft:ore` (grootte 8, discard 0) met per gastgesteente een doel (rood/oranje/crème kloofsteen, `scorched_sandstone`); 14 aders per chunk, `trapezoid` Y 95–180 (`plateau` 60, vol ~108–168): bovenste helft van de kloofwanden, spleet- en tafelbergwanden. Gemeten (seed 20260930, 99 `canyonlands`-chunks): **19,7 per chunk** (~¼ ring 1-ijzer, 75,8; keuze gebruiker na meting), 99,5 % in Y 95–180, 161 blokken boven Y 150, ~5 % bloot; tafelbergen 47 per chunk, lage kloofgebieden bijna niets. 0 in `oasis_flats`, ring 1, ring 3, Nether.
  - **Turkoois:** `minecraft:ore` (grootte 4, discard 0) in de drie tinten; 4 aders per chunk, `uniform` Y 10–40 (onderste kloofsteenlagen). Gemeten **7,1 per `canyonlands`-chunk** (~¼ ring 1-diamant, 23,8; keuze gebruiker na meting), Y 8–40.
  - Beide zijn afstemwaarden voor teststop 2b (alleen getallen in datagen).
  - **Vanilla-ertsen:** kolen/ijzer/koper ×0,6; goud/redstone/lapis ×0,5; diamant ×0,25 (vaste factor in heel ring 2); geen ore veins; geen vanilla-ertsen in kloofsteen. Gemeten per band (erts per 10.000 steen + deepslate; ring 2 ÷ ring 1 zonder aders / ring 2 na ÷ vóór op hetzelfde terrein): kolen 0…63 0,55 / 0,59; ijzer −64…−1 0,64 / —, 0…63 0,60 / 0,61; koper −64…−1 0,83 / 0,59, 0…63 0,53 / 0,56; goud −64…−1 0,50 / 0,51, 0…63 0,47 / 0,42; redstone −64…−1 0,47 / 0,49, 0…63 0,60 / 0,52; lapis −64…−1 0,48 / 0,50, 0…63 0,60 / 0,53; diamant −64…−1 0,26 / 0,28. Volledige tabel in plan 25.

**Grotten**
- **Ondergrondse waterreservoirs** (aquifers) die de oases voeden; oriëntatie en lore.
- **Instortende zandgrotten**: plafonds van los zand (`loose_sand`, change 21: vanilla-zwaartekracht + speler-trigger, server-side).
- **Uitwerking (change 26)** — startwaarden voor teststop 2b; alles worldgen (router, aquifer-wrapper, surface rules, carvers), puur per seed:
  - **Basisgrotten** (vraag 1 vervallen): vanilla noise-grotten uit change 23, alleen in `final_density`. **Carvers (keuze gebruiker, A):** vanilla's `cave`, `cave_extra_underground` en `canyon` als eigen kopieën met start-Y ≤ 40 (`ring_2/*`) en een kans die meeschaalt, zodat er onder Y 40 even veel gangen zijn als in vanilla. Vanilla-carvers hollen ná de surface rules uit en lieten ~10 hangende zandblokken per oasechunk en hangend rood zand in de kloven achter; nu 0 boven Y 60. Diep snijden ze nog door de zandgrotten.
  - **Waterreservoirs (vraag 2 A):** floodedness per ring via `ring_blend` (ring 1 en 3–5 vanilla). Ring 2 = vanilla-ruis + een diepteband: +0,65 van Y −40 tot 30, aflopend naar 0 bij Y 60 en bij Y −54. Router-`erosion` is in ring 2 0, anders zet vanilla's deep-dark-check de aquifer onder ~Y 0 in een derde van de ring uit. Ring 2 heeft nergens een preliminary surface onder ~Y 55, dus vanilla's drempels zijn daar hoog (0,8 voor de grondwaterspiegel op Y 63, 0,4 voor een lokaal niveau). Gemeten (seed 20260930, 200 ring 2-chunks, Y −54…62): **26 % van de holtes staat vol water** (vóór change 26: 1,8 %; ring 1: 11,0 %, dus 2,3×). **Change 30b (30b-7 A): band +0,65 → +0,575** (teststop 2b: te veel volgelopen grotten). Gemeten in dezelfde 200 chunks (60 s uitgezakt, alleen onder een dak): nulmeting op change 30 22,2 %; +0,45 → 8,4 %, +0,50 → 10,3 %, +0,55 → 13,4 %, **+0,575 → 15,2 %** (doel 15 ± 2); ring 1 ongewijzigd 11,0 %; zone 1→2 65 → 62 %; water onder een dak op Y 60–62 gelijk. Het water boven Y 60 is niet toegenomen, de oasepoelen zijn ongewijzigd. Reservoiroppervlakken liggen vooral op de lokale niveaus (Y ~−24, ~16, ~53) en de lavalaag onder Y −54. In de zone 1→2 loopt de band mee (geaccepteerd, B2).
  - **Geen lokale lava (vraag 3 A):** aquifer-lava boven Y −54 wordt water (`RingFluidPicker.waterOnly`, ook in carvergangen); op Y −54 blijft een blok steen tussen dat water en de lavalaag. In de ring 2-biomes geen lavameren en geen lavabronnen (`spring_lava`). De vanilla-lavalaag onder Y −54 blijft. Gemeten: 0 lava boven Y −54 in 200 ring 2-chunks.
  - **Zandgrotten (vragen 4–7: A, B, A, A):** top-level surface-rule-tak vóór het lagenblok. Waar de 2D-ruis `sand_cave_zone` ≥ 0,21 is (**25 % van ring 2**, vlekken ~200–400 blokken), krijgen de plafonds van noise-grotten 2–3 blokken `loose_sand` en de vloeren 1 blok vanilla-zand. Dat geldt alleen onder het preliminary surface en vanaf Y 8: nooit aan het oppervlak (in het rotsgebied ligt het preliminary surface op de laagste kloofbodem), nooit in de deepslate-laag. Gemeten: plafonds op Y 8–58 (gevallen zand ligt lager), los zand in 30 % van de ring 2-chunks; het grootste aaneengesloten plafond telde 1.548 blokken (775 hangend). Biomes: `canyonlands`, `oasis_flats`; **nieuwe ring 2-biomes (changes 30/31) komen in de `biome_is`-lijst van deze tak en gebruiken `Ring2Biomes.generation` (diepe carvers, geen lokale lava)**.
  - **Instorten (vraag 5 B):** gedrag van `loose_sand` (change 21). Gemeten met de quick-play-speler `Dev` onder het grootste plafond: het blok erboven was na 1,0 s weg, de kettingreactie nam 548 blokken mee, tick P99 13–19 ms. Eén plafondblok weghalen (`setblock`): 268 van 329 blokken vielen, P99 29–57 ms (in twee van de drie rondes één tick boven de 50 ms; gemiddeld 5–8 ms). Een NoAI-zombie eronder en een plafond zonder iets eronder bleven hangen. **Boven reservoirs** zakt een deel van de plafonds al in na het laden: vanilla-aquiferwater stroomt dan en geeft het zand erboven een buur-update (keuze gebruiker: accepteren; in het sterkst overstroomde oasegebied 0/−2.500 hangt na 45 s nog maar 44 van de ~1.500 blokken, in 0/2.500 nog 1.710 van ~2.100, in de kloofgebieden alles).
  - **Determinisme:** twee verse werelden, de tweede omgekeerd geladen, 60 s laten uitzakken: blok voor blok gelijk op vanilla's ondergrondse klodders en ertsen na (graniet/dioriet/andesiet/tufsteen, aarde/grind, kolen). In change 26 zijn ook vanilla's disks in ring 2 chunk-lokaal gemaakt (origin ≥ 6 van de chunkrand). Hun schrijven in buurchunks was de oorzaak van de verschuivende oeverplanten (`oasis_vegetation`) uit change 25.
  - **Kosten:** noise-stap gelijk aan vóór change 26 (binnen de spreiding); zie `CODEBASE.md` (meting change 26).

**Weer (ringweer)**
- **Zandstorm**: zicht sterk beperkt (client fog + particles), lichte wind-duw (server).
- **Hittegolf**: lucht trilt (client), uitdroging loopt 2× sneller (server).
- **Uitwerking (change 28)** — gameplay server-side (`weather/SandstormWind`, `weather/HeatWaveDehydrationModifier`, waarden in `[weather.ring2]` van de SERVER-config); beeld en geluid client-side (`client/weather/`), alleen uit de gesyncte staat:
  - **Windrichting:** één willekeurige richting per storm, afgeleid uit de `seed` van de weerstaat (gehele kompasgraden waar de wind vandaan komt; 0 = noord, 90 = oost). Server, particles, F3 en `/ca weather` (`wind 225° (from SW)`) gebruiken dezelfde functie: alle clients zien dezelfde richting. Blijft gelijk na een herstart.
  - **Duw:** de server stuurt elke `sandstormGustTicks` (10) een additieve stoot `richting × sandstormPushStrength (0,08) × intensiteit` (`wind_gust`) naar elke speler in ring 2 (hard op 0,5) die de hemel ziet op ooghoogte (`canSeeSky`, dezelfde regel als direct zonlicht); niet in creatief/spectator of als passagier, niet in de Nether. Gemeten: ~3,4 blokken per 10 s drift (0,34 blok/s) op een vlak platform; onder een dak 0.
  - **Blootstelling (beeld/geluid):** hemellicht op de camera ÷ 15, gladgestreken over ~2 s: binnen en ondergronds verdwijnen fog, particles, trilling en wind geleidelijk. Gameplay blijft hard (aan/uit).
  - **Zandstorm in beeld:** fog naar ~24 blokken (terrein én lucht), kleur naar zand `#C9A46B` (× daglicht), ~40 particles `sand_gust` per tick in een kader van ±16 rond de camera, alleen boven de `MOTION_BLOCKING`-heightmap, met de wind mee; vanilla's instelling "Particles" telt mee. In water/lava/poedersneeuw en met blindness/darkness blijft vanilla-fog staan.
  - **Hittegolf in beeld:** post-shader `heat_haze` (zacht golvend beeld, sterkst onderin, hand en GUI niet), sterkte × "Distortion Effects"; lichte warme waas (fog-afstand × 0,85, kleur 30 % naar `#F2DDAA`).
  - **Uitdroging ×2:** factor `1 + (heatWaveDehydrationMultiplier (2,0) − 1) × intensiteit` op een positieve rate (alleen de zon), hard op de ringgrens; schaduw, water en oase blijven gelijk. Gemeten: `+0.80/s, sun` bij volle intensiteit, `+0.52/s` bij 0,30.
  - **Geluid:** wind-loop `weather.ring_2.sandstorm` (verwijzing naar vanilla `item.elytra.flying`, pitch 0,7; volume 0,6 × sterkte × blootstelling, weer-schuif); de hittegolf is stil. Keuze van het vanilla-geluid valt in de W-achteraf.

**Conditie: uitdroging**
- Meter ↑ overdag in direct zonlicht (hemel zichtbaar); 2× in een hittegolf.
- Meter ↓ in schaduw, in water, in het oase-biome, 's nachts, en ~30 punten per vanilla-waterfles.
- Drempels (config): sneller honger → traagheid → mining fatigue → lichte schade.
- Tegenmaatregel in fase 2: veldfles/waterzak.
- **Uitwerking (change 27)** — alles server-side (`condition/DehydrationRule`, `WaterBottleDrink`), waarden in `[conditions.dehydration]` van de SERVER-config:
  - **Zone:** alleen de overworld, hard op de ringgrens: `ringAt(...).ring() == RING_2` (wissel op blend 0,5, zoals biomes en ringweer). Nether-ring 2 heeft geen conditie.
  - **Opbouw** alleen in direct zonlicht overdag in de zone, droog en buiten de oase: **+0,4/s** (0 → 100 in ~4 min). "Overdag" = de zon boven de horizon volgens de tijd van de dag (vanilla-zonnehoek: dagtijd 23.215 t/m 12.785); vanilla-regen en -onweer tellen niet. "Direct zonlicht" = hemellicht 15 op ooghoogte (`canSeeSky`): daken, overhangen, nissen en bladeren geven schaduw, glas niet.
  - **Afname** (de sterkste wint, niets stapelt): schaduw overdag 0,5/s, nacht 0,5/s (beide in de zone), **water** 2,0/s (ondergedompeld tot boven het middel: water ≥ 1 blok boven de voeten; zwemmen en een waterval tellen, een plasje of 1 laag in een geul niet; overal), **oase** (`oasis_flats`) 2,0/s ook in de volle zon, **buiten ring 2** 0,5/s (ring 1, 3–5, void, Nether). Alleen de zon geeft een positieve rate; de hittegolf (change 28) verdubbelt precies die.
  - **Waterfles:** een gedronken vanilla-waterfles (`Potions.WATER`) haalt 30 punten af, overal, zonder cooldown. Splash/lingering en andere drankjes niet.
  - **Drempels** (cumulatief): 25 honger I, 50 traagheid I, 75 mining fatigue I, 100 schade.
  - **Schade:** 1 (half hartje) per 4 s zoals verhongeren, door harnas heen; peaceful geen, easy tot 10 HP, normal tot 1 HP, hard dodelijk. Eigen damage type `corruptedadventure:dehydration` (tags als vanilla `starve`), doodsbericht "%1$s dried out" / "%1$s is uitgedroogd".
  - **Creatief/spectator:** de meter stijgt nooit en drempels gelden niet (change 19). Na de dood staat de meter op 0.
  - **Wandbronnen (23c):** echt water; de val telt als water, een kolk alleen waar hij 2 diep is, het stroompje niet (gemeten in change 27; of dat zo mag: backlog).

**Horror-elementen (fase 1)**
- **Zingend zand**: laag zoemend geluid in de duinenzee, vooral 's nachts (client-only ambient op de biome-key `dune_sea`, `SingingSandAmbience`).

**Uitwerking sfeer (change 32)**
- **Kleuren** (lucht / fog; water / waterfog overal `32A598` / `0B3A35`, tenzij vermeld): basis `AEC8E0` / `E4D2A6` (bleek geelblauw, zanderige fog, geen lichtwijziging). Per biome een nuance, vooral in de fog: `canyonlands` fog `E2BC93` (roodoranje stof); `oasis_flats` `9EC5EC` / `E8DDBA` (frisser, "veilig"); `salt_flats` `BCD3E8` / `EEEAE0` (verblindend), pekelwater `5AC8B8` / `1B5048` (melkachtig turkoois tegen de korst); `hoodoo_fields` fog `DDC291` (oker); `dune_sea` `B4CADA` / `EACF96` (goudgeel, het warmst); `dry_riverbeds` = basis 35 % ontzadigd en 10 % donkerder (grauw, geblakerd, voorbode van ring 3). Gras en blad blijven worldgen. Zandstorm en hittegolf (change 28) leggen zich als overlay over deze basiskleuren.
- **Zingend zand:** alleen in `dune_sea`, alleen onder open hemel (niet in de grotten eronder). Kans per tick naar de tijd van de dag: 's nachts gemiddeld 1x per ~40 s, overdag 1x per ~4 min, zachte overgang in de schemering, cooldown 12 s. Positioneel, 8-16 blokken van de speler uit een wisselende richting; per speler eigen toeval, niet gesynct. Geen netwerk, geen serverstaat.
- **Oase-ambient:** `oasis_flats` heeft als enige ring 2-biome een `additions_sound` (`ambient.ring_2.oasis`: vogel, krekel, tjilpjes; `tick_chance` 0,004, laag en zacht). De mini-oases in `dune_sea` blijven stil.
- **Audio:** eigen synthese, zie "Geluid" onder §1.4.
- Fata morgana → fase 5 (backlog).

**Grensmuur 2→3** (fase 5): wervelende zandstorm.

**Nether-tegenpool — Nether-ring 2 (375 – 875): doorweekte schimmel-Nether**
- **Terrein: zwamkathedralen** — hoge open holtes, reuzenzwammen als zuilen van vloer tot plafond; de lavazee ligt lager en is deels bedekt met sporenmos-eilanden.
- **Biomes**: druipende reuzenzwamwouden (hoofdbiome), lichtgevende sporengrotten. Dauwbronnen als feature in beide.
- **Blokken**: reuzenzwam-set (druipzwam-steel, -hoed, planks + houtset), doorweekt netherrack, sporenmos (spreidt zoals nylium), slijmstalactieten (dripstone-gedrag), lichtgevende schimmel + zwamlamp, dauwbron.
- **Dauwbron**: eigen natuurblok dat water-achtig oogt en druppelt; **geen echte waterfluid**, de Nether blijft `ultrawarm`.
- **Sleutelmateriaal: dauwkristal**, groeit langzaam aan dauwbronnen (zoals budding amethyst). Nodig voor bewaker 2→3; in fase 2 de basis van hittebescherming voor ring 3.
- **Sfeer**: dichte grijsgroene fog, constante druppelgeluiden, zwevende sporen-particles (alles biome-data).
- **Uitwerking (change 36):** fog `56645A` in `dripshroom_forest`, `3E5A50` (donkerder, blauwer) in `spore_caves`; fog-dichtheid vanilla-Nether (alleen de kleur is data, geen client-code). Geluid: `ambient_sound` = eigen gesynthetiseerde naadloze loop (`ambient.nether_ring_2.drip_loop`, `Ring2Sounds.java`), `additions_sound` = vanilla dripstone-druppels (`ambient.nether_ring_2.drips`) met `tick_chance` 0,01 (woud) en 0,02 (grotten). Particles: `spore_blossom_air` 0,0005 (woud), `glow` 0,004 (grotten). Mood en muziek zoals change 35 ze zet.
- Geen conditie.
- **Uitwerking (change 33, gebouwd)** — 20 blokken, placeholders uit `Ring2Textures.java` (definitief in change 37); alleen blokken, plaatsing in de wereld is change 35:
  - **Namen:** `dripshroom_*` (stem, hyphae, gestript, planks + houtset, `_cap`, `_lamp`), `soaked_netherrack`, `spore_moss`, `slime_stalactite`, `glow_mold`, `spore_sprouts`. Tag `corruptedadventure:fungal_growths` (stam, hyfen, gestript, hoed, lamp, stalactiet, gloeischimmel, sporenspruiten) is het contract met 34/35.
  - **Houtset = Nether-hout (antwoord 2 A):** `stem`/`hyphae`, in `#logs` maar niet `#logs_that_burn`, geen vuurkansen, items in `#non_flammable_wood` (geen ovenbrandstof), `BlockSetType`/`WoodType` `corruptedadventure:dripshroom` met nether-hout-geluiden. Hoed = wratblok (antwoord 3 A), lamp = shroomlight (licht 15). Geen kleine druipzwam (4 A, backlog).
  - **Sporenmos (5 A + 9 A):** random tick binnen de eigen familie: bedekt → doorweekt netherrack; onbedekt: één poging per tick naar onbedekt doorweekt netherrack in 3×5×3; nooit naar vanilla-netherrack; geen licht-/dimensie-eis; niet in `#nylium`. Bonemeal op doorweekt netherrack naast sporenmos → sporenmos; bonemeal op sporenmos met lucht erboven → `spore_sprouts` in vlekken. Silk Touch → sporenmos, anders doorweekt netherrack.
  - **Doorweekt netherrack (6 A):** netherrack-hardheid, modder-geluid, geen eeuwig vuur, `#base_stone_nether` + `c:netherracks`.
  - **Slijmstalactiet (7 A):** generieke `StalactiteBlock` (vorm per dikte, samenvoegende punten, valt als kolom en doet schade, stalagmiet-valschade, trident breekt hem; geen groei, geen druppels; slijmgeluid), bedoeld voor hergebruik door de ijsstalactieten van Nether-ring 3 (change 51).
  - **Gloeischimmel (8 A):** multiface zoals glow lichen (licht 7, alleen met schaar, bonemeal spreidt), eigen `GlowMoldBlock`; **sporenspruiten (9 A):** kruismodel, alleen op sporenmos/doorweekt netherrack, geen random tick, composteerbaar.
  - **Compost:** hoed 0,85, lamp 0,65, schimmel 0,5, spruiten 0,3. **Overdracht 35:** stalactieten alleen met steun en expliciete dikte-states; reuzenzwam via `huge_fungus` of een eigen feature; gloeischimmel via `multiface_growth`; quartz/goud/magma gebruiken `block_match` netherrack en dus niet doorweekt netherrack.
- **Uitwerking (change 34, gebouwd)** — de dauwbron en het dauwkristal als speelbaar mechaniek (placeholders uit `Ring2Textures.java`, definitief in change 37; plaatsing in de wereld is change 35):
  - **Dauwbron `dew_source` (1 A, 2 A):** eigen `DewSourceBlock`, vol opaak blok met water-achtige texture (geen waterfluid), kopie van `budding_amethyst` (hard, piston vernietigt); **dropt niets, ook niet met Silk Touch**, dus elke bron komt uit worldgen en het kristal blijft aan Nether-ring 2 gebonden. Licht 0. Druppelt zichtbaar (7 A): `animateTick` met `DRIPPING_WATER` als er geen sturdy blok onder zit.
  - **Groei (3 A, 5 A):** random tick, server-side, kans 1/5 per tick op een willekeurige zijde (constante `GROWTH_CHANCE`); lucht of volle waterbron wordt `small_dew_crystal_bud`, daarna alleen vanaf de eigen zijde (`facing`) `medium_`, `large_dew_crystal_bud` en `dew_crystal_cluster`. Vanilla-tempo: ~34 min per stadium per zijde met een speler in de buurt, ~1 cluster per ~23 min per vrije bron.
  - **Stadia:** vanilla `AmethystClusterBlock` (maten van amethist), licht 1/2/4/5 (6 A), in `fungal_growths` en `c:buds`/`c:clusters`.
  - **Drops (4 A):** placeholder-item `dew_crystal` (`c:gems/dew_crystal`); cluster: pickaxe geeft 4 (Fortune), anders 2, Silk Touch geeft het cluster; knoppen alleen met Silk Touch. Nut en recipes in fase 2.
  - **Overdracht 35:** blok en stadia op registry-naam en `facing` (richting van de bron af); de bron zit in geen carver-/stone-/nylium-tag, dus als feature na de carvers plaatsen.
- **Uitwerking (change 35, gebouwd)** — terrein, biomes, lavazee, zwammen en dauwgeodes (afstemwaarden in `src/main/resources/data/corruptedadventure/worldgen/`; W-checklist onderaan `docs/plans/change-35.md`, nagelopen in teststop 3):
  - **Terrein (2 A, 5 A):** hallen met vloer Y ~28 ± 5 (woud) en plafond Y ~108 (woud) / ~70 (grotten); dikke wanden en ribben uit 3D-ruis met doorgangen (`walls`), een dichter grottenstelsel in de sporengrotten (`caves`), rare massieve rotspijlers van vloer tot plafond (`masses`, drempel 0,6), lavabekkens met sporenmos-eilanden (vloer net boven de lava) en een dam van doorweekt netherrack langs beide ringgrenzen. Terreinvorm = `ring_2/nether/terrain`, ingevoerd als ring 2 in `ring_blend`.
  - **Lavazee (1 A):** Y 22 via `fluid_levels` in de preset + de dam (`ring_edge`, vast tot Y 33 binnen 48 blokken van de grenzen). Vanilla's Nether-carver zet zelf lava tot Y 31: de ring 2-biomes gebruiken `ring_nether_cave` (eigen carver-type, vloeistof uit de aquifer); ring 1 blijft vanilla.
  - **Biomes (3 A, 9 A):** regio-as `temperature` (= `ring_2/nether/region`) in de Nether-router; `multi_noise` met drempel −0,10: `dripshroom_forest` erboven (gemeten 74,0 % (r 460), 56,1 % (r 625), 74,8 % (r 780)), `spore_caves` eronder. Geen overgangsbiome. Tags `is_nether_ring_2`, `c:is_nether`, `c:is_hot/nether`, `c:is_wet/nether`, `c:is_nether_forest` (woud), geen `minecraft:`-tags (geen vanilla-structures tot fase 3). Spawns (8 A): zombified piglin, enderman, strider, af en toe een ghast.
  - **Oppervlak en ertsen (7 A):** de hele ring is `soaked_netherrack` (surface-rule-tak vóór vanilla's `hole` → lava); `spore_moss` op eilanden en oevers (Y 23–26) en in vlekken (woud ~80 %, grotten ~40 %); vanilla's Nether-ertsen als kopieën met doel `soaked_netherrack` en vanilla's tarieven (absolute Y's).
  - **Zwammen en groeisels (4 A):** `giant_fungus` (zuilen en vrijstaande reuzenzwammen), `slime_stalactite`, `mold_patch` (gloeischimmel) en `spore_sprouts_patch` (33-9 A), allemaal volgens de determinismeregel: lezen alleen terrein, schrijven op rang (`GrowthPriority`), nooit uitwijken. Geen lavabronnen, vuur, glowstone of basalt in de biomes.
  - **Dauwgeodes (6 B):** holte uit de density function `dew_geodes` (puur van seed en positie, wandtest op het terrein), bekleding (~5 % van de binnenwand → `dew_source`, knoppen op ~25 % van de vrije zijden, enkele plekken gloeischimmel) uit de feature `dew_geode`. Gewijzigd t.o.v. het plan om het tarief te halen: 48 pogingen per cel in plaats van één, geodes ook in de rots boven de plafonds en in de pijlers, kleinere bollen (straal 3–5 woud, 3–4 grotten, schil 2). Gemeten: woud 1 per 11,2 chunks (doel 8–12), grotten 1 per 6,8 (doel 5–6), gem. 19 (woud 3–45, grotten 6–34) bronnen per geode; geen losse bronnen buiten geodes.
  - **Prestatie (35-10 A, budget ≤ 1,75×):** noise-stap Nether-ring 2 ≈ 1,95× (budget 1,75×: niet gehaald, voorgelegd aan de hoofdsessie) vanilla-Nether.
  - **Overdracht:** Nether-3/4 hergebruiken `fluid_levels`, `ring_edge`, `ring_nether_cave`, `giant_fungus` (andere blokken), `FungalTerrainScan`/`GrowthPriority`; change 36/37 hebben fog, geluid en textures van de blokken al geleverd.

- **Textures (change 37, definitief; stijl in §1.4):** de ring 2- en Nether-ring 2-textures komen uit `tools/textures/Ring2Textures.java`; paletten van de sets zoals in de werkpaletten van het script.
  - **Kloofkeien (8 A):** één dominante oranje middentint met ~15 % rode of crème gast-stenen en een warme voeg `6B3A22`; de laagkleuren leven in de wanden, de keien zijn hun gemiddelde.
  - **Zout (9 A):** `salt_crust_top` effen en rustig (fijne korrel, zachte vlekken, 3 lichte tinten, geen polygonen: de zeshoeken zijn de `salt_block`-lijnen uit de wereld); `salt_block` donkerder en grover zodat die lijnen afsteken; zijkant = korstband op `salt_block`.
  - **Eigen planten (10 A):** `dry_shrub` grijs met lichte takpunten, `desert_flower` zes-puntige ster (koraal, crème toppen, turkoois hart), `barrel_cactus` blauwgroen met stekelkrans en roze knop met gele kern; `bone_block` blijft vanilla (eigen botblok = backlog `blocks`).
  - **Gloed en beweging:** zonnesteen-overlay en `raw_sunstone` pulseren (goud), turkoois en zoutkristal blijven still en mat; dauwstadia en `dew_crystal` bijna kleurloos helder (`1B4A5C 5E9FB4 82BACB A6D3E0 D6EEF4 F4FCFF`), emissive en pulserend; `dew_source` golft traag (`cycling` 16 × 4); druipzwam-stam met glijdend slijm (`cycling` 16 × 8); `glow_mold` emissive en statisch; zwamlamp, hoed en stalactieten still.
  - **Particles (7 A):** `sand_gust_0..3` (8×8, lichte grijswaarden, door `SandGustParticle` zand getint).
  - **Palmbladeren:** ≈ 23 % doorzichtig (was 42 %), zodat ze bij Graphics: Fast niet donker ogen.
- **Textures (change 37b, herschilderd na gebruikersfeedback 2026-10-06: "plat", "half getekend", "erts past niet in doorweekt netherrack"):**
  - **Doorweekt netherrack (37b-1 A):** op de toonladder van vanilla-netherrack, iets donkerder: `3C1314 4A191A 59201F 682828 773231 863E3C` + natte porie `2A0E10`, glans `9A5650`, vocht `3A4634`; klonten op netherrack-schaal (16 cellen) met licht linksboven. Natter gemaakt na de tussenstand: zachte vochtzones (~één toon donkerder, met natte glans), poriën in de voegen en 4 losse vochtspikkels die geen rij, kolom of diagonaal delen; geen losse plassen (die lazen als 16-px-raster); het vocht loopt zacht over de hele tegel en over de naden, en de gemiddelde helderheid is die van vanilla-netherrack (~0,20), zodat vanilla-ertsen erin zitten zonder vierkant en de muur geen kader per tegel toont; van 8 lay-outs uit dezelfde seed blijft die met de egaalste rijen, kolommen en diagonalen (`stripeScore`). Vanilla-kwarts/-gouderts (72–87 % netherrack-pixels) zitten er zonder rood vierkant in; "doorweekt" komt uit glans, poriën, vochtspikkels en het sporenmos. `spore_moss_side` volgt (SIDE_RULES).
  - **Zonnesteen (bijsturing 2026-10-06):** oranjerood `5A1A06 9A300C D95A16 F2862A FFB85A FFEBC0`, gloei `FFE0A8`, dimtint `9A300C`; geldt voor `raw_sunstone` en de gedeelde overlay (alle vier de zonnesteenertsen). Vervangt het gele werkpalet hierboven.
  - **Items:** `raw_sunstone` als trosje brokken, `raw_turquoise` als één hoekige ruwe brok (hand-gelegde sprites, `sprite(…)`), `dew_crystal` als brede, asymmetrische scherf (polygoon: scherpe punt, breed midden, gebroken voet) met licht vlak, rug en schaduwvlak, `salt` als ongelijk hoopje korrels met kubusjes, `slime_stalactite` slank met glanslijn; allemaal met selectieve omlijning (§1.4).
  - **Nether-ring 2:** sporenmos 6 tonen `34403A 45533F 586A4E 6E8160 879877 A3B28C` (kussentjes); druipzwam-schors `2C2734 3A3444 4B4558 5E5A6E 747286` met golvende vezels en slijm `4E7458 6E9A78 9CC4A2`; hout donkerder `48523F 5A654F 6D7962 818E74 97A388` (37b-4 A, gem. ~0,40); hoed `3A3046 4A3F58 5D506E 72648A 8A7CA4` als knobbels met slijmdruppels; lamp met olijf celwanden `6E8A3A 8FA84E` en kern `FAFFE0`; slijmstalactiet één stap donkerder `3E6E3E 528A4E 6AA460 86BC78 A8D896` + glans `C8ECB4`.
  - **Ring 2:** `canyon_stone_bricks` in de `stone_bricks`-indeling (37b-5 A, 2 rijen van 16×8, warme voeg); kloofkeien met bolle stenen en twee half-gemengde gast-stenen (8 A blijft); gebarsten klei met 1-px-scheuren, lichte lip en haarscheurtjes (5 tonen, + `D0BFA6`); geschroeid zandsteen met zachte roetvlekken (geen losse zwarte pixels) en korte laagjes; palmstam met ringen van 3–5 px en verspringende V-kerven; erts-overlays als onregelmatige brokjes/knolletjes met facetten en selectieve omlijning; deuren en valluiken met verzonken ramen, lichte/donkere lijst, scharnier en schoor.

**Client/server-risico's**
- Zandstorm-duw en hittegolf-effect: server-side (condities-tick/ringweer); fog, particles en luchttrilling alleen client (`client/`).
- Instortend zand, dauwkristalgroei en sporenmos-spreiding: vanilla-mechanismen / random ticks, server-side.
- Uitdroging-meter: attachment + sync; HUD client-only.

### Ring 3 — Vulkanische ring (3.500 – 6.000; was 3.500 – 5.500)

**Uitwerking sparring ring 3 (2026-10-06)** — gaat vóór de tekst van sessie 2 hieronder waar die botst. Bron: `docs/plans/ring-3-sparring.md` (vragen 1–22); contract en golven: `docs/plans/ring-3/contract.md`, `docs/plans/ring-3/waves.md`.
- **Visie:** de "midden"-ring wordt de langste en zwaarste overleving, levendig (les ring 2) en met een wow-moment bij binnenkomst. **Richting "Smeulend Vuurland"**: een levend vulkanisch land, aangevuld met zwevende eilanden en lavavallen (episch) en een dag/nacht-ritme (survival). **Fantasy: hoog**: reuzenskeletten, zielvuur en titanen-rotsen horen erbij.
- **Wow bij de grens 2→3:** horizon-vulkanen. Vanuit `dry_riverbeds` zie je kegels boven de wolkenlaag (Y 192) met gloed en een rookpluim (particles). Op de grens slaat de lucht om naar asrood met opstijgende vonken.
- **Breedte:** ring 3 = 3.500 – 6.000 (2.500 breed, de grootste ring); ring 4 = 6.000 – 7.800; ring 5 = 7.800 – 9.500; Nether-3 = 875 – 1.500 (625 breed, lost de ruimte-check op). De standaardwaarden in `CommonConfig` staan hier sinds werkpakket 37c op. Een gewijzigde config geldt alleen voor nieuwe werelden; bestaande testwerelden houden hun snapshot.
- **Sub-biomes (7):** de vijf uit sessie 2 (`ashen_wastes`, `volcanic_peaks`, `dried_seabed`, `basalt_flows`, `sulfur_fields`) plus:
  - **`ember_woods`** (smeulwoud: verkoolde stammen met gloeiende askronen, vuurflora; het "levendige" biome);
  - één fantasy-biome **`titan_fields`** (werktitel; titanenveld met half begraven reuzenskeletten en zielvuur rond de skeletten). De skeletten zijn **worldgen-features**, geen structures en geen loot.
  - Overgang 3→4 blijft `steam_fens`.
- **Terrein-signatuur:**
  - lavarivieren met spaarzame lavavallen;
  - zeldzame zwevende basalteilanden (boven calderameren en lavarivieren);
  - caldera's, deels slapend en groen.
  - **Basaltzuilvelden zijn geen signatuur** (niet gekozen); `basalt_flows` blijft "gestolde stromen met gloeiende scheuren". *Herzien na teststop 4a (39d): zuilenkliffen op de treden van de basaltplateaus, zie "Reliëf en vorm per biome (39d)".*
- **Oase = slapende kraters:** een koele, groene caldera als rustpunt (de rol van `oasis_flats` in ring 2). Uitgewerkt in V8 hieronder.
- **Overlevingskern: hitte-ritme + schaarste.**
  - Hitte stijgt in de zon overdag van **0 naar 100 in ~3 min** (uitdroging ~4 min), **×1,5–2** bij hittebronnen en in de pieken. Ze daalt 's nachts, ondergronds, in water en in een slapende krater.
  - Drempels: **25 traagheid → 50 zwakte → 75 mining fatigue → 100 in brand**. Fire resistance halveert de opbouw (sessie 2).
  - Water verdampt bovengronds (sessie 2); water blijft schaars.
- **Voedsel:** **zeldzaam vee: geiten op de pieken** (`volcanic_peaks`); dat wijkt af van de aanbevolen optie "geen vee". Eetbare vuurflora en waterschaarste blijven (V7 hieronder: geen botsing).
- **Weer:** asregen + uitbarsting (sessie 2) + een **zeldzame aswolk** (pyroclastisch: donker, extra hitte, schuilen). **Lavabommen** vallen ringbreed rond spelers onder open lucht, dichter bij de pieken.
- **Ertsen:** zoals sessie 2 (vulkaniet, zwavel, vuurrobijn; vanilla verder afgebouwd) + **vuurkristal-geodes** (zeldzaam, gloeiend; nut in fase 2).
- **Horror:** as-afgietsels, voetsporen en gerommel blijven. Daarbij komen **as-afgietsels die 's nachts zwak gloeien**.
- **Nether-ring 3:** het ontwerp van sessie 2 blijft. Daarbij komen de levendig-eis en een eigen wow: **bevroren lavavallen**, rijpwoud met **ijspegels** en **gloeiende scheuren als warme eilandjes**.
- **Prestatie:** noise-stap **≤ 1,75× vanilla** (zoals ring 2); stromende lava bij generatie alleen met een cap per chunk.
- **Detailbesluiten (vervolgvragen V1–V8, 2026-10-06, alle A; `contract.md` §9):**
  - **V1 — reuzenskeletten en titanen-rotsen:** skeletten 20–40 blokken lang (`bone_block` + verweerd basalt), half begraven; titanen-rotsen als liggende of geknielde reuzensilhouetten van basalt (15–35 hoog). Beide als sjabloon-features binnen 3×3 chunks (hergebruik `surface_fossil`, eigen `.nbt`-sjablonen uit een deterministisch script). Kolossale exemplaren zouden een structure zijn (fase 3).
  - **V2 — zielvuur:** vanilla zielvuur op zielgrond rond de skeletten: blijvend, licht 10, brandschade bij aanraking, **geen** hittebron.
  - **V3 — smeulwoud:** verkoolde stammen met `ember_leaves`: emissive texture, **geen bloklicht**; bomen 6–12 hoog, dicht genoeg voor een woud.
  - **V4 — zwevende eilanden:** Y 150–210, alleen boven calderameren en lavarivieren, ~1 cluster van 1–4 eilanden (8–30 breed) per ~3 caldera's, met lavavalletjes eraf.
  - **V5 — horizon-vulkanen:** de grootste kegels Y 230–260 (boven de wolken, ruimte voor de pluim tot de bouwlimiet), gewone kegels Y 140–200. *Herzien in 39d: reuzen Y 250–285, middelgrote kegels 80–140 boven de vlakte.*
  - **V6 — aswolk:** gewichten asregen 45 / uitbarsting 40 / aswolk 15; aswolk 120–240 s, met ~20 s waarschuwing (gerommel, verdonkering).
  - **V7 — voedsel:** geiten alleen in `volcanic_peaks` (lage weight, groepjes 1–3, op basalt/blackstone/as); één zeldzame eetbare plant (`ember_berry_bush`), alleen in `ember_woods` en de slapende krater.
  - **V8 — slapende krater:** een eigen micro-biome **`dormant_caldera`** (8e biome naast de 7 sub-biomes; hitte ↓ 2,0/s zoals `oasis_flats`, groen, eigen kleuren en geluid), **zonder natuurlijk water**. Water in ring 3 alleen diep ondergronds (aquifers onder Y ~0). Gekozen via een derde router-as: `weirdness` = `ring_3/overworld/caldera_dormant` (`ring_blend/overworld_weirdness`; ring 1 blijft vanilla).
  - **Verkoold hout is niet brandbaar** (besluit gebruiker).
- **Besluiten tijdens golf 1 en integratie 1 (2026-10-06/07)** — details in de secties hieronder en in `02-beslissingenlog.md`:
  - **Gloed in twee lagen:** gloeiende blokken (smeulsteen, smeulende as, smeulblad, gloeiende scheur) zijn een gewoon blok + een emissive overlay met alleen de gloeipixels; 's nachts gloeien alleen de scheuren/vlekjes.
  - **Horizonreuzen in elke regio behalve de zeekom**, met de biome van die regio; vanuit `dry_riverbeds` is zo overal een vulkaan te zien.
  - **Ondergrond ring 3 (V1 = A, 39c):** een vulkanisch pakket dat het oppervlak volgt; geen grijs steen in wanden en grotmonden.
  - **Monsters ring 3** tot fase 4: de woestijnset van ring 2 (husks), geen dieren behalve de geiten (wijkt af van contract "vanilla").
  - **Hitte en ringweer:** de weerfactor geldt alleen onder open lucht; tijdens de waarschuwing van de aswolk nog geen factor.
  - **Water verdampt** onder open lucht ook als generatoren daardoor onmogelijk worden.
- **Werkwijze (infra):**
  - contract-first in golven; stromen parallel in eigen git-worktrees; één integratie-agent (merge, `runData`, prestatie, docs);
  - drie teststops: **4a** terrein-wow (blokkerend), **4b** overleving + sfeer, **4c** eindcheck incl. Nether-3;
  - textures direct als goede eerste versies in de 37b-stijl (geen placeholder-ronde; uitzondering op §1.4 "placeholders eerst");
  - nummers 38–57 blijven als werkpakketten, nieuwe pakketten krijgen een letter;
  - de hoofdsessie orkestreert handmatig met agents (geen Workflow-tool).

**Terrein en sfeer**
- Zwart-grijs aslandschap met vulkaankegels, kraters, lavameren en gestolde lavastromen. Lucht rood-oranje, fog dicht en asgrijs.
- Eigen ring 3-invoer voor `ring_blend`, met per biome-regio een eigen vorm (kegels, stromen, zeekom).
- **Router (change 39a):** `temperature` = `ring_3/overworld/region_heat` (heet: pieken, stromen ↔ koel: zeekom, as), `vegetation` = `ring_3/overworld/region_ground` (begroeid/vreemd ↔ mineraal), `weirdness` (router-`ridges`) = `ring_3/overworld/caldera_dormant` via `ring_blend/overworld_weirdness` (ring 1 blijft letterlijk vanilla). Het klimaat wisselt vanaf zone 3→4 hard op de middellijn (`switch_at`): de ring 3-kant van die zone leest de eigen assen, ook vóór `steam_fens`. De gedeelde terreinsleutels (`height`, `caldera_mask`, `caldera_dormant`, `lava_channel`, `island_mask`) zijn sinds 39 ingevuld.
- **Terrein (change 39, 2026-10-06/07):**
  - asvlakte Y ~80–100 met lage heuvels en asruggen; zeekom Y 30–45 met kustterrassen; titanenribben (tot 8 hoog) in `titan_fields`; getrapte stroomlobben in `basalt_flows`;
  - vulkanen op een jittered raster: horizonreuzen (rand Y 230–260) in een band achter zone 2→3, **in elke regio behalve de zeekom** (optie A); gewone kegels Y 140–200; lage caldera's; ~30 % kegels en ~45 % caldera's slapend (vlakke bodem, geen lava);
  - **lava via de aquifer** (kratermeren en rivieren), nooit tegen open lucht: waar lava aan lucht zou grenzen komt een korst van steen; geen water boven Y 0;
  - **zone 2→3:** ring 3's hoogte mengt met w² naar ring 2 (helling max. ~0,75–1,3); de beddingen van `dry_riverbeds` lopen uit vóór ring 3;
  - **zegel (`ring3Seal`):** in ring 3's helft van de zones 2→3 en 3→4 blijft open ruimte onder Y 63 rots, zodat geen water tegen lucht staat (grotten en carvers verdwijnen in die strook onder Y 63);
  - zwevende eilanden (V4): clusters boven **actieve** (niet-slapende) kraters met een rand ≤ 174 (`island_max_y` − 36; lage caldera's én lage kegelkraters), plus aparte clusters boven lavakanalen (`channelCluster`, cel 960, kans × 0,6); alleen met ≥ 10 blokken vrij boven het terrein.
- **Reliëf en vorm per biome (39d, na teststop 4a; feedback en besluiten in `docs/plans/ring-3/teststop-4a-feedback.md`):**
  - **"Ruig":** tussen de vulkanen Y ~70–150 (gemiddeld ~113), brede hooglanden en laagtes (−15…+45 op ~1–1,5 km), deining ±16, golving ±7, heuvels tot 40, kliffen en plateaus 15–40. Op de vlakte een fijn 2D-microreliëf (±2 blokken, ~16–32 breed) tegen rechte traplijnen; 3D-crags alleen op steile wanden, kliffen en kegels (prestatie). Het reliëf groeit over 240 blokken achter zone 2→3 (geen klif in de w²-overgang).
  - **Vorm per biome:** elke vorm loopt uit naar 0 op de biomegrens, zodat vormen nooit in een muur eindigen.

    | Biome | Vorm |
    |---|---|
    | `ashen_wastes` | duinzee: asduinen 10–25 in één windrichting met steile lijzijde; daartussen geulen met hoodoos 5–15 |
    | `ember_woods` | glooiend laagland (−6), zachte heuvels; het bos (47) doet de rest |
    | `sulfur_fields` | laag bekken (−14) met zwavelterrassen (treden van 3 met randje), fumarole-heuveltjes en zinkgaten |
    | `basalt_flows` | getrapte lavaplateaus (3 treden van 12) met zeshoekige zuilenkliffen en lavabuizen (ingestorte sleuven en tunnels); karakter van 39 behouden |
    | `titan_fields` | stenen ribben als bogen uit de grond (20–60) en zeldzaam een ribbenkast met wervels; de lage ribben van 39 blijven |
    | `volcanic_peaks` | bergketen (+35): kegels verbonden door kammen en graten, het hoogste deel van de ring (Y 150–250); karakter van 39 behouden |
    | `dried_seabed` | kustterrassen (39), rifruggen en zeepilaren 15–40 |

  - **Overal:** riftdalen (20–40 diep, getrapte flanken, niet in zeekom/titanenveld/zwavelbekken), velden van slakkenkegels (10–40 met minikrater, niet bij vulkanen, rivieren of rifts).
  - **Vulkanen overal zichtbaar:** kans in as/smeulwoud/zwavel ~3× (≈ 1 per 500–600 blokken), middelgrote kegels 80–140 boven de vlakte (onder de reuzen), reuzen Y 250–285. Eilanden per oppervlak ongeveer gelijk.
  - **Getraceerde lavarivieren:** per actieve vulkaan 1–2 vanaf de voet bergaf, om vulkanen heen (nooit een krater in) en in riftdalen en afgronden.
    - Het niveau daalt stroomafwaarts nooit; elke daling van 1 blok is een klein lavavalletje (geen korst over de rivier). Korst alleen op losse randblokken.
    - Breedte 6 → 16, ondiep (lava 2 onder de oevers, zichtbaar van ver). Einde in een lavapoel.
    - Oevers 3 blokken breed, weinig magma.
    - Deterministisch, één keer per wereld getraceerd en gedeeld.
  - **Fantasy (alle drie de groepen):**
    - zwevende archipels boven lage actieve caldera's (hoofdeiland 60–100 breed, 2–5 kleinere, lavavallen via bronnen met een fluid-update, basaltpilaren naar de grond);
    - een zeldzame **zwevende vulkaan** (Y ~200–255) met kratermeer en lava die over de rand naar beneden stort;
    - basaltbogen over rivieren en riftdalen, zuilenkliffen, obsidiaannaalden 60–120;
    - **afgronden** tot Y ~2 met een lavazee (Y 9) en smalle natuurlijke bruggen.
    - Kleine vormen elke paar honderd blokken, grote landmarks enkele per ring.
- **Bijschaven na 4a (39f, 2026-10-08/09; V1–V4 = A):**
  - **eilanden onder de wolken:** archipels alleen boven actieve caldera's met rand ≤ 150, tops ≤ 185 (hoofdeiland max(rand + 32, 160–185)); rotsige onderkant (kom + 2–3 hangende lobben) i.p.v. een kegel; grillige, uitlopende, leunende basaltpilaren met richels van de kraterbodem tot in het eiland; echte lavavallen (kolom vallende lava van de rand tot grond, kratermeer of eiland eronder; één blok breed);
  - **zwevende vulkaan:** plaat Y 165–175 onder de wolken, alleen de kegel (tot ~205–225) steekt erdoor; lavavallen tot de grond;
  - **vanilla-grotingangen** sluiten ~8 blokken onder het maaiveld (de rechthoekige "put" was er een); de openingen zijn eigen grillige **instortingen** (~1 per 250–350 blokken op de vlaktes, 12–22 diep in treden, soms een overhangende lip, ~45 % met een schacht naar de grotten);
  - rafelige traplijnen (2D-microtreden), `ember_woods` zonder vlakke grove-aardevlekken (ring 3-blokken met randen langs de treden), obsidiaannaalden met kronkelende gloei-aders, dunne vormen (naalden, pilaren, bogen, eilanden) helemaal geschilderd, kruinen van ribben en bogen met de biome-surface;
  - **riftbogen:** ~1 per riftdal (21 op de testseed), elkaar > 220 blokken uit de weg;
  - ring 3-surface sneller (één tak voor alle biomes, snelle weg voor diepe blokken); koude prestatie-uitschieters (titanenveld 2,00×, smeulwoud 1,89×) geaccepteerd als JIT-koude start (gebruiker, 2026-10-09).
- **Surface (39c):** vulkaankegels hebben in elk biome dezelfde stijl (basalt met as, kap van puimsteen boven Y ~196); actieve kraters blackstone met magma en smeulsteen; lavaoevers blackstone met magma. **Ondergrond (V1 = A):** onder de surface een vulkanisch pakket van ~8–14 blokken dat het oppervlak volgt (tuf/smooth basalt → basalt met blackstone-aders), daaronder steen/deepslate. Askorst alleen op daken van één blok boven een holte; askorst boven carver-grotten, aslagen (47) en geblakerde vlekken in `dry_riverbeds` (47*) volgen in golf 2.
- Het gestolen water is overal zichtbaar: geen natuurlijk water, een lege zeekom, en geplaatst water verdampt bovengronds.

**Biomes (7 + micro-biome `dormant_caldera`; was 5)**
| Biome (werktitel) | Karakter |
|---|---|
| `ashen_wastes` | Grijze asvlakte met verkoolde bomen en aslagen — kernbiome |
| `volcanic_peaks` | Vulkaankegels met kraters en lavastromen; heetste plek — kernbiome |
| `dried_seabed` | Lege zeekom: diepe kom met dode koraalbanken (vanilla dead coral), het gestolen water |
| `basalt_flows` | Gestolde lavastromen met gloeiende scheuren (smeulsteen) |
| `sulfur_fields` | Gele zwavelkorst en fumarolen |
| `ember_woods` | Smeulwoud: verkoolde stammen met gloeiende askronen, vuurflora (sparring ring 3, 2026-10-06) |
| `titan_fields` | Fantasy-biome (werktitel): titanenveld met half begraven reuzenskeletten, zielvuur rond de skeletten, titanen-rotsen (sparring ring 3) |
| `dormant_caldera` | Micro-biome (8e, de "oase"): slapende krater, koel en groen, zonder natuurlijk water; gekozen via router-as `weirdness` (V8) |

- **Indeling (39b):** heet (pieken, stromen), midden (zwavel, smeulwoud), koel (zeekom, as, titanen) op de regio-assen; elk biome ~1/7; `dormant_caldera` via weirdness. **Spawns tot fase 4:** de woestijnmonsters van ring 2 (husks) en vleermuizen, geen dieren; alleen geiten in `volcanic_peaks` (zeldzaam, groepjes 1–3; besluit gebruiker).
- Overgangsbiome 3→4: **stoomvenen** (`steam_fens`): hete modderpoelen, geisers en stoomkolommen. Hier komt het gestolen water terug en kookt het tegen de hitte. Stoom is biome-particles (data).
- **Uitwerking aankleding (W-biomes 41–49, 2026-10-08/09; G2-WB1..WB13 = A, startwaarden, afstemmen in 4b):** alle ring 3-features staan in één globale volgorde en lezen alleen het pure terrein (lava, grond, landmarks), zodat ze deterministisch blijven en nooit op naalden, bogen, ribkruinen, brugdekken, lavaoevers, steile wanden, vulkaanflanken of in actieve kraters komen.
  - **`sulfur_fields`:** een fumarole met zwavelkraag op (bijna) elke heuveltop en in de kommen, modderpoeltjes midden in de zinkgaten, niets in instortingen; **47d (teststop 4b, besluit 2):** gekleurde kookpoelen gelijk met de grond met een verhoogde calcietrand en terrassen van gele/oranje terracotta en calciet (travertijn), modder geel/groen/oranje (3 : 2 : 2, één kleur per poel) met `sulfur_steam`; zwavelkristallen op 35 % van de kraag van de zwavelfumarolen en in groepjes op de korst; fumarolen en modder niet op de travertijn;
  - **`basalt_flows`:** hornitos met gloeiende kap, blackstone-tongen en smeulventjes; **`volcanic_peaks`:** inslagkraters met obsidiaan en magmakern (nooit boven vloeistof), rotsen; **`ashen_wastes`:** verkoolde dode bomen in de geulen, puimsteenkeien; aslagen in de lijzijde en geulen (windruis), ook rond de smeulwoudstammen;
  - **`dried_seabed`:** dode koraalriffen en zeemonsterfossielen (eigen sjablonen);
  - **`titan_fields`:** schedels, wervels en botten (bot, half begraven), zielvuur op zielgrond rond de ribvoeten en onder de wervelkolom (47d, besluit 5 van 4b: twee vlekken per ribvoet/wervel, vuur op de helft van de kern), **titanen-rotsen** (basaltreuzen 20–29 hoog: knielend, gebogen of als buste, per cel van 256 blokken, kans 0,4); geen complete skeletten;
  - **`ember_woods`:** gemengd smeulwoud (smeulbomen 6–9, smeulreuzen 10–12 met spar-kroon, kale verkoolde stammen) met open plekken (omgevallen stammen, smeulbessen waar het licht ze laat teruggroeien, vuurflora) en aslaagjes rond de stammen;
  - **`dormant_caldera`:** oase met varens/gras, azalea-struiken, af en toe een azaleaboom als baken, bessen aan de voet van de wand, glow lichen tegen de wand; **47d (teststop 4b, besluit 1): overwoekerde krater**: mosheuveltjes in de weides, mostapijt, azalea-bosjes (vanilla azaleaboom met struiken) waar de groeiruis hoog is, varens en gras, dripleaf in kommen, af en toe een omgevallen eikenstam, ranken en hangende wortels langs de wand; geen water (startwaarden in de beslissingenlog);
  - **lavavallen** (feature): een bron in een kraterwand, kloof- of afgrondwand met ≥ 10 val, alleen waar hij echt in bestaande terreinlava landt, max. 2 per chunk, alleen in de ring 3-kern; de eilandvallen van 39f blijven terrein;
  - **eilanden:** smeulende as met aslagen boven, hangende vuurkristallen eronder (≥ 4 lucht eronder), soms een smeulboom op grote eilanden;
  - **`dry_riverbeds`:** de geblakerde vlekken zijn as met aslagen en verkoolde stronken (ring 3-kant van de ring 2-overgang);
  - **striders** (zeldzaam) op lavarivieren en kratermeren in alle ring 3-biomes behalve de pieken, **zonder ruiter** (gebruiker); geiten blijven op de pieken.
- **`steam_fens` (48, 2026-10-09):** het overgangsbiome van zone 3→4 (`transitions.ring_3`; de klimaatassen mengen nu ook in 3→4, `switch_at` alleen nog 4→5 en 5→void): nat (downfall 0,9), kookpoelen alleen boven Y 63 (water in eindtoestand), modderpoelen, geisers die vanaf het genereren hun ritme lopen (`startCycle`) en stoomventen; surface modder, packed mud, klei, grind/as, zwavelkorst langs scheuren, tuf op steile wanden, nooit vanilla-gras. Het hoort bij de ring 3-lijsten met tags (`is_ring_3`, heet, nat), niet bij de eigen `multi_noise`.

**Unieke blokken (placeholders eerst)**
- Basis is vanilla basalt / blackstone / magma. Eigen blokken:
- **As**: asblok + aslaag (gedraagt zich als sneeuwlaag).
- **Puimsteen-set**: puimsteen, gepolijst, bricks, slab, stairs, wall.
- **Smeulsteen**: gloeiende scheuren, geeft licht, hitte ↑ in de buurt.
- **Verkoold hout-set**: log, wood, planks + houtset.
- **Zwavelkorst + fumarole**: fumarole geeft stoompuffen (client-particles via `animateTick`), hitte ↑ erboven.
- **Stoomvenen**: kokende modder (vertraagt, hitte ↑) en geiser (duwt periodiek omhoog, server-side).
- **Uitwerking 38–46 (2026-10-06/07):**
  - **gloed in twee lagen** (besluit gebruiker): smeulsteen (licht 7), smeulende as (licht 2), smeulblad (geen bloklicht, V3) en de gloeiende scheur hebben een gewone texture + een emissive overlay met alleen de gloeipixels; nooit het hele blok fullbright;
  - aslaag als sneeuwlaag die nooit smelt; **askorst stort alleen in van onderen** (zoals los zand; van bovenaf: beslissen na teststop 4b);
  - verkoold hout, smeulblad en vuurflora zijn **niet brandbaar** (Nether-hout-patroon, geen brandstof);
  - **zwavelkorst dropt zichzelf** (zwavel komt alleen uit de ertsen); **geiser alleen met Silk Touch**;
  - kokende modder = de traagheid van honing; geiser duwt elke 5 s ~5 blokken omhoog, geen valschade bij landen, een bedekte opening blokkeert;
  - vuurkristalblok (geodewand) gloeit niet, alleen de kristalstadia.
- **Uitwerking 46-rest (2026-10-08/09; G2-B1/B2 = A):** de geiser heeft een eigen ritme per positie (80–200 ticks, deterministisch per wereld en dimensie), 1 s waarschuwing (borrelen + kleine stoompuf, voor alle clients gelijk) en duwt ~5 blokken; een veld loopt niet in de maat. Fumarole en geiser stomen met `steam_puff`; eigen geluiden voor fumarole, geiser en kokende modder, per client begrensd zodat een veld niet dreunt. Kokende modder blijft traag + hittebron.
- **Uitwerking afronding (46b, 50c; 2026-10-09, teststop 4b):** de geiser waarschuwt met een eigen geluid (`block.geyser.warning`, gegrom + bubbels, uitgestorven vóór de burst); `glowing_fissure` en `smoldering_stone` krijgen 32 willekeurige varianten (rotaties + spiegeling) tegen het tegelraster, de scheuren vormen dan een netwerk; nieuwe zwavelblokken: modder in geel/groen/oranje (kokende modder met `sulfur_steam`) en zwavelkristallen in vier stadia (amethist-gedrag, licht 1–3, cluster dropt zwavel).

**Ores**
- **Vulkaniet** (hoofdmateriaal, kandidaat ring 3-gear-tier): diep in basalt nabij lava. Risico is beloning.
- **Zwavel**: veel en ondiep bij fumarolen en in `sulfur_fields`; nut in fase 2 (buskruit, vuur, brandstof).
- **Vuurrobijn**: zeldzame edelsteen in de wanden van magmakamers.
- Vanilla-ores verder afgebouwd dan in ring 2: ijzer en kolen schaars, diamant bijna afwezig.
- **Uitwerking 41 (2026-10-08; G2-WB1 = A, startwaarden):** vanilla-ertsen ×0,35 (kolen/ijzer/koper) / ×0,3 (goud/redstone/lapis) / ×0,1 (diamant); vulkaniet aders 9 (Y −48…40) plus extra pogingen nabij de eigen terreinlava (binnen 4 blokken); zwavel aders 7, ondiep (4–30 onder de terreinhoogte), ringbreed en extra in `sulfur_fields`; vuurrobijn aders 4, Y −40…30, zeldzaam; blootliggende ertsen blijven. De ring 3-ertsen liggen ook in de ring 3-helft van `dry_riverbeds` en in `steam_fens` (via `is_ring_3`).
- **Ertsblokken (40, 2026-10-07):** "één stap boven ring 2" (hardheid + 0,5; vulkaniet en vuurrobijn ijzeren houweel, zwavel stenen); gastgesteenten steen, deepslate en basalt (zwavel: steen en basalt), **geen blackstone** (gebruiker); **`basalt_fire_ruby_ore`** voor de basaltwanden van magmakamers (gebruiker); vuurrobijn licht 3 en gloeiend.

**Grotten**
- **Lavabuizen**: lange, gladde basalttunnels, soms met een lavarivier; bruikbaar als reisroute.
- **Magmakamers**: grote holtes boven een lavameer, met vulkaniet en vuurrobijn in de wanden.
- **Uitwerking 42 (2026-10-09; G2-WT1..3 = A):**
  - per ~400 blokken een buissysteem: een **droge hoofdbuis** (ellips met vlakke vloer, 5–9 breed, 5–7 hoog, Y 20–80, helling ≤ 0,3) die opent in een ingestorte 39d-sleuf of op een goede plek, met 1–3 daklichten waar ze ondiep loopt; een **zijtak** met een **lavagootje** (poeltjes met korstlipjes, geen stromende lava) naar het lavameer van een **magmakamer** (30–60 breed, koepel 8–16, kom 6–11, Y −40…10; 224 kamers bij 312 systemen op de testseed);
  - **wanden** smooth basalt (huid) en basalt met blackstone-aders (schil), korst bij lava, ook diep (geen deepslate in de wand); de buis is een schuilroute (dak −0,5/s), een lavazijtak is een hittebron;
  - **vanilla-grotten** krimpen geleidelijk (over 20 blokken): weg boven Y 146 en in vulkaanlichamen; ze blijven in de vlaktes; geen carver of grot snijdt een kamer, gootje of geode aan;
  - **water** alleen diep (eigen floodedness: geen lokale aquifervloeistof boven Y ~−8); erosion ring 3 = 0 (geen deep dark);
  - **vuurkristal-geodes** als holte (cel 48, straal 4–7, Y −50…20, ~1 per 12 chunks; buizen mogen ze aansnijden); de bekleding volgt in 41b.
- **Uitwerking 41b (2026-10-09; G2-WB2 = A):** de geode is gesloten en bekleed in drie lagen die de echt open holte volgen (vuurkristalblok, blackstone, smooth basalt); knoppen en clusters (40 % klein, 30 % middel, 20 % groot, 10 % cluster) op de binnenwand; een door een lavabuis aangesneden geode houdt zijn lava, en waar terrein de holte vulde komt geen losse bekleding. Elke chunk bekleedt zijn eigen deel (deterministisch); alleen rots wordt vervangen.
- **Uitwerking 42b (2026-10-09, golf 2-afronding; V1–V3 = A):** **geen lava tegen lucht** in ring 3 behalve de vastgelegde vallen: de aquifer rekent de echte, geïnterpoleerde grond naast lava (celhoeken om de 4 blokken), korst ook boven open lucht eronder, carvers en openingen blijven uit de band rond lava, geen 39d-tunnels onder lava, en **geen lokale aquiferlava** meer in ring 3 (diep water onder Y 0 blijft). **Riviertreden** blijven een kleine vastgelegde lavaval, maar alleen als de lagere buurkolom die laag zeker met lava vult, anders een korstlip. **Wanden van diepe sneden** (afgrond, rifts, rivierkloven, instortingen) onder het grondpakket in gelaagd vulkanisch gesteente (tuf in banden, blackstone-aders, basalt), schachten basalt; geen vanilla `stone`-banden. Een **dak van één blok** boven een opening wordt `ash_block` (valt niet); worldgen legt geen vallende askorst meer.

**Water: verdampt bovengronds**
- Geplaatst water (emmer, dispenser, gebroken/smeltend ijs) verdampt direct **op plekken waar de lucht zichtbaar is**, net als in de Nether. Ondergronds en onder een dak werkt water normaal.
- Server-side check met `ringAt` + lucht zichtbaar bij plaatsing.
- **Uitwerking 44 (2026-10-07):** alleen nieuwe waterbronnen en nieuw waterlogged blokken verdampen (stromend water niet), via één hook voor alle routes. Water verdampt ook als **obsidiaan- en cobble-generatoren** daardoor onder open lucht onmogelijk worden, zoals in de Nether; ondergronds of onder een dak werken ze wel (besluit gebruiker).

**Weer (ringweer)**
- **Asregen**: zicht omlaag, as-particles (client); hitte ×1,5 (server).
- **Uitbarsting**: rode lucht, gerommel (client); de server laat **lavabommen** (vanilla small fireballs) uit de lucht vallen, die vuur zetten (volgt de `mobGriefing`-gamerule) maar niet ontploffen; hitte ×2.
- **Aswolk** (45): bijna zwart, fog ~16, vonken en as; hitte ×3. De opbouw van 20 s (gerommel, verdonkering) is de waarschuwing.
- **Uitwerking 45 + integratie 1 (2026-10-06/07):** lavabommen per speler (cap per speler per minuut, dichter in `#heat_peaks`); de luchtkleur via één client-mixin. **De weerfactor op hitte geldt alleen onder open lucht** en **tijdens de waarschuwing van de aswolk nog niet** (pas daarna ×3) (beide besluit gebruiker).
- **Uitwerking 50 (2026-10-08):** eigen geluiden voor asregen, uitbarsting en aswolk (`Ring3Sounds.java`); de wolken in ring 3 zijn donker asgrijs (besluit 39f), vloeiend over de grens.
- **Uitwerking 50c (2026-10-09, teststop 4b):** lavabommen stapelen niet bij meerdere spelers (een plek in de schijven van k spelers wordt met kans 1/k gehouden, dus per oppervlak zoveel als bij één speler) en leven hooguit 10 s, ook niet terug uit een opgeslagen chunk; de lucht bij een uitbarsting wordt diep karmijn (`8A1A0C`, vol na de halve fade-in), 's nachts blijft een rode gloed.

**Conditie: hitte**
- Meter ↑ bovengronds overdag (langzaam); sneller binnen ~4 blokken van lava/magma/smeulsteen/fumarolen/kokende modder, in `volcanic_peaks` en tijdens ringweer (×1,5 / ×2).
- Meter ↓ 's nachts, ondergronds (lucht niet zichtbaar en niet bij lava) en in water. Water werkt alleen ondergronds, dus **grotten zijn de schuilplaats**.
- **Fire resistance halveert** de opbouw (maakt niet immuun).
- Drempels (config): traagheid → zwakte → in brand vliegen.
- **Uitwerking 43 (2026-10-06/07):** opbouw in de directe zon of binnen 4 blokken van een hittebron (een bron telt ook 's nachts en ondergronds); de sterkste factor wint (bron ×1,5, piek ×2), ze stapelen niet, en een piek alleen verwarmt nooit zonder zon of bron. Daling: 's nachts en onder een dak −0,5, in de schaduw van bladeren −0,25, water en `dormant_caldera` −2,0 (ook in de zon), buiten ring 3 −0,5. Ringweer (×1,5 / ×2 / ×3) alleen onder open lucht. Volgorde van de vermenigvuldigers: weer → gear → fire resistance → zuiverend. Bij 100: eigen hitteschade (1/s, eigen doodsbericht) met een vlam, niet in water, regen, bubbelzuil of poedersneeuw.
- **Uitwerking 55b (2026-10-09, teststop 4b):** een gedoofd kampvuur is geen hittebron (zoals bij de kou); **ijs smelt** in ring 3 op zijn random tick waar de hittemeter zou stijgen (overdag onder open lucht of naast een brandende bron; niet in `dormant_caldera` of onder water; smeltwater onder open lucht verdampt) en een **natte spons droogt** waar water verdampt (met het Nether-effect; niet direct na het opzuigen, anders een pomp). Beide via een server-side mixin; `packed_ice`/`blue_ice` smelten nooit (vanilla).
- **43-afstemming (2026-10-08):** ~7 % van het ring 3-oppervlak ligt binnen 4 blokken van een hittebron (lava ~1,6 %): "bron ×1,5" geldt dus niet bijna overal; de regel blijft. Onder zwevende eilanden telt het dak ("roof").
- Tegenmaatregel in fase 2: hittebescherming op basis van dauwkristal (Nether-2).

**Horror-elementen (fase 1)**
- **As-afgietsels**: Pompeii-achtige figuren van asblokken in menselijke houdingen (kruipend, knielend). Deterministische feature.
- **Voetsporen in de as**: een spoor van indrukken in aslagen dat abrupt stopt of in een krater eindigt. Feature.
- **Uitwerking 49 (2026-10-09; G2-WB9/WB10 = A, startwaarden):** afgietsels (5 houdingen, as + smeulende as die 's nachts gloeit) in groepjes van 1–3, ~1 groep per 3 chunks in `ashen_wastes`, zeldzaam in smeulwoud en titanenveld, op vlakke grond en gekeerd naar de dichtstbijzijnde vulkaan; voetsporen als pure paden (~1 per 20 chunks, 15–40 stappen) over chunkgrenzen in asvlakte, titanenveld en pieken, alleen op vlakke as; een spoor stopt abrupt, eindigt bij een afgietsel of aan de rand van een instorting of krater, en stopt aan de biomerand.
- **Gerommel en trillingen**: onregelmatig diep gerommel (biome-ambient) en een lichte camera-shake; client-only, per speler willekeurig en bewust niet gesynct.
  - **Uitwerking 50 (G2-A4 = A):** in de vlaktes elke 2–5 min (1 op 3 met een lichte schok), op de pieken elke 1–2,5 min (2 op 3, iets sterker); niet tijdens uitbarsting/aswolk; binnen en ondergronds zachter en zonder schok; "Screen Effects" schaalt de schok.

**Sfeer per biome en rookpluimen (50, 50b; 2026-10-08/09; G2-A1..A3 = A)**
- Eigen tint per biome binnen het asrode ringpalet (as grijsbeige, pieken rood-oranje, smeulwoud amber, stromen roodbruin, zwavel geelgroen, zeekom bleek zandblauw, titanen koel violet, slapende krater groener); particles subtiel tot middel (as, opstijgende vonken, zwavelstoom, zielvonkjes, stof, pollen; `steam_fens` stoom); loops in smeulwoud (knisperen), titanenveld (laag koor) en slapende krater (rust); borrelende lava als additions op pieken en stromen.
- **Rookpluimen zonder terreinmist (50c, teststop 4b):** de pluimen zijn een eigen client-render-pass: tot 512 blokken zichtbaar met een eigen waas naar de mistkleur (geen mistsilhouet meer); in een uitbarsting of aswolk staan ze ín de mist van het weer.
- **Rookpluimen** boven de actieve vulkanen (slapende niet): de server stuurt per speler de actieve vulkanen binnen 512 blokken (nieuwe payload), de client tekent de 8 dichtstbijzijnde als kolom (~80–110 blokken die bovenin uitwaaiert), zichtbaar tot 512 blokken met minder, grotere wolken op afstand; bij een uitbarsting dikker en hoger met rode gloed. Voorbij de render distance is de pluim door vanilla's mist een silhouet in mistkleur (beoordelen in 4b).

**Grensmuur 3→4** (fase 5): zwarte aswolk met vonken.

**Nether-tegenpool — Nether-ring 3 (875 – 1.500; was 875 – 1.375): bevroren Nether**
- **Terrein**: de lavazee is gestold tot een enorme vlakte van ijs en koudlava, met scheuren waarin nog gloeiende lava ligt (zeldzame warme plekken). Daarnaast gletsjermassa's met smalle kloven en tunnels.
- **Router en lava (change 39a):** Nether-`temperature` in ring 3 = `ring_3/nether/region`, vanaf zone 2→3 hard gewisseld op de middellijn (`switch_at`: elke kant kiest zijn biome op de eigen regio, de as die ook zijn terrein volgt); `fluid_levels.ring_3` = Y 22, gelijk aan ring 2 (antwoord 2026-10-06): geen zichtbare lavazee (vloer Y ~35), lava alleen onder het ijs (diepe holtes) en in de gloeiende scheuren. Een ander niveau dan ring 2 gaf lavawanden op de middellijn 2→3, waar de carvers van ring 2 door de dam snijden (Y 8: 195, de standaard 32: 81, Y 22: 0). De trede 3|4 (22/32) moet het terrein van 53 afdekken (contract §4, harde eis); sinds 53 doet een eigen dam dat (vast tot Y 33 binnen 48 blokken van beide grenzen; gemeten 0 lavawanden op r 875 en r 1.500).
- **Terrein (change 53, 2026-10-06/07):**
  - **regio-as** (gebruiker, vraag 1 = A): midden `frozen_lava_sea` (drempels ±0,18 op `ring_3/nether/region`; hoofdbiome het grootst, elke overgang via de vlakte), hoog `rime_forest`, laag `glacier_rifts`. Gemeten (seed 20260930): zee 41,5 %, rijp 35,8 %, kloven 22,7 %;
  - `frozen_lava_sea`: open vlakte, vloer Y ~35, plafond Y ~108, ijspilaren van vloer tot plafond (zandloper, 7–17 breed, 80–135 open tussen); smalle **warme scheuren** in vlekken met magma in de wanden en lava onderin (Y 17–22); geen lavapoelen buiten de scheuren;
  - `rime_forest`: hallen 30–100 breed met wanden en ribben;
  - **`glacier_rifts`** (gebruiker, vraag 2): ijsmassa tot Y ~90–100 met een luchtspleet tot het plafond, kloven 8–20 breed tot ~Y 32 (zichtbaar van bovenaf, niet beklemmend), tunnels door de massa;
  - gletsjer, wanden, pilaren en scheuren lopen binnen 48 blokken van de ringgrenzen af;
  - **prestatie** (gebruiker, optie A, 2026-10-07): Nether-3 mag even duur zijn als Nether-2 (gemeten 1,59–1,88× vanilla tegen Nether-2 1,62–1,95×; het terrein zelf 0,88–1,05×, de rest is het ringraamwerk).
- **Surface (39c):** zeevloer koudlava met platen Nether-ijs, pilaren met een huid van Nether-ijs en een kern van blauw ijs, gletsjermassa verpakt Nether-ijs, scheurwanden gloeiende scheur, rijpwoud berijpt netherrack. **Oerijs als dunne kern in de sterkste pilaren** (ruis ≥ 0,66, Y 45–95; ~60 pilaren in heel Nether-3, kernen meestal 1–3 breed; besluit gebruiker, herziene V2 = A; 0,72 bleek vrijwel leeg). Een kern in élke pilaar kan alleen met een feature die het midden van een pilaar kent (53b).
- **Uitwerking 53b/54 (2026-10-08/09; G2-WN1..WN5 = A):**
  - **`frozen_lava_sea`:** ijspegels aan plafond en pilaarkoppen, platen Nether-ijs en rijpvlekken op de koudlava, **bevroren lavavallen** als ribben langs de pilaren en **bevroren lavagordijnen** aan het zeeplafond (terreinvorm + verf), **warme kringen** van 4–6 blokken rond de scheuren (netherrack, magma, gloeiende scheur, geen ijs); open lavaspiegels op Y 22 onderin de scheuren (bewuste uitzondering, nooit naast lucht); de mist is er zeer dicht (zicht ~40 blokken): vanilla-Nethermist zoals in alle Nether-ringen, geen biome-data (56 raakt de fog-afstand niet, G2-A5);
  - **`rime_forest`:** bevroren reuzenzwammen 12–22 zonder lamp met ijspegels onder de hoed (ruimte 29 tot het plafond), kleine rijpzwammen 3–6 met een dunne stam, berijpte vloeren;
  - **`glacier_rifts`:** **ijsbruggen** (2–6 dik, dunner naar de randen, 5 onder de gletsjertop, alleen in volle gletsjer) over de kloven, **ijsvallen** van blauw/verpakt ijs op de kloofwanden, blauw-ijsaders in de massa, ijspegels in kloven en tunnels, zeldzame **gloeiende naden** op de kloofbodem (de "scheur-adertjes"; de kloven reiken niet tot de lava);
  - **Uitwerking 54b (2026-10-09, golf 2-afronding):** in de kloven zijn de vloeren onder Y 30 (rond de open lavaspiegels op Y 22) de warme kring: koudlava met wat magma, geen ijsplaten en geen rijp (G2-WN3); gletsjermassa's en ijsvallen beginnen op Y 30.
  - **wezens:** strays, skeletten en ghasts (geen piglins/hoglins; strays via een eigen spawnregel, herzien in fase 4); **geen carvers** (de dam van trede 3|4 blijft heel);
  - featurevolgorde vast; de nieuwe features verschuiven de decoratie in Nether-1/2 (feature-index, gemeten in integratie 2).
- **Biomes (3)**: `frozen_lava_sea` (hoofdbiome: open vlakte, ijspilaren van vloer tot plafond), `rime_forest` (verstijfde reuzenzwammen onder rijp), `glacier_rifts` (ijsmassa's met kloven en tunnels). De biome-schaal is kleiner dan in de overworld (biomes wisselen vaker). **Ruimte-check** in de W-test: voelt het krap, dan verbreden we ring 3 via de config (alleen voor nieuwe werelden).
- **Blokken**: koudlava-set (gestolde lava: blok, gepolijst, bricks, slab, stairs, wall), Nether-ijs (smelt niet, glad; plus verpakte variant), rijp-netherrack, rijpzwam-set (steel, hoed, planks + houtset), ijsstalactieten (dripstone-gedrag, vallen), gloeiende scheur (warmtebron). Vanilla packed/blue ice waar het kan.
  - **Uitwerking 51/52 (2026-10-06/07):** Nether-ijs is **opaak** (zoals `packed_ice`, eigen rookgrijsblauw; gebruiker), glad en smelt niet, alleen met Silk Touch; koudlava heeft de eigenschappen van basalt; rijpzwam is Nether-hout (niet brandbaar); **gloeiende scheur licht 7** (gebruiker; niet 10), gloed in twee lagen; **berijpt netherrack is bevroren grond**: niet `infiniburn` (vuur dooft) en geen nylium via beendermeel (gebruiker); **oerijs** = hardheid en gladheid van blauw ijs, maar de **blastweerstand van obsidiaan** (gebruiker: ghast-vuurballen en creepers vernietigen het niet zonder drop), alleen met Silk Touch.
- **Sleutelmateriaal: oerijs**, ijs dat bevroor vóór de corruptie (zuiver water). Het zit als kern in ijspilaren en is alleen met silk touch te winnen (anders breekt het). Nodig voor bewaker 3→4; in fase 2 smelt je het tot zuiver water, de basis van gifbescherming voor ring 4.
- **Sfeer**: blauwwitte fog, zwevende ijskristallen (particles), gierende wind en krakend ijs (ambient). Alles biome-data; geen ringweer in de Nether.
  - **Uitwerking 56 (2026-10-08; G2-A3/A5 = A):** fog blauwwit (zee), iets violet (rijpwoud), kouder blauw (kloven); `ice_glint`-glinstering; eigen wind-loop en krakend ijs; **ademwolkjes** bij spelers en ademende wezens (geen undead); geen vriesmist (fog-afstand is geen biome-data).
- **Sparring ring 3 (2026-10-06):** ontwerp blijft; levendig-eis en eigen wow: bevroren lavavallen (ijsvormen langs wanden), rijpwoud met ijspegels (`ice_stalactite`), gloeiende scheuren als warme eilandjes. Ring 625 breed (875 – 1.500) door de verbreding van ring 3; de ruimte-check van change 54 blijft als controle.
- **Conditie: kou/bevriezing**
  - Meter ↑ overal in Nether-3, sneller op ijs en in de open vlakte.
  - Meter ↓ bij warmte: lava, vuur, kampvuur, magma, gloeiende scheur, smeulsteen (uit ring 3).
  - **Leren armor remt** de opbouw (vanilla-tag `freeze_immune_wearables`).
  - Drempels: traagheid → mining fatigue → vanilla-bevriezing (`ticksFrozen`: frost-overlay en schade, door vanilla al gesynct).
  - **Uitwerking 55 (2026-10-08; G2-C1..C4 = A):** in Nether-3 overal +0,42/s (0 → 100 in ~4 min), ×1,5 op ijs (het blok onder de voeten, ook Nether-ijs en oerijs) of in `frozen_lava_sea` (tag `cold_exposed`; sterkste wint, geen stapeling); binnen 4 blokken van een brandende warmtebron (lava, vuur, kampvuur, magma, gloeiende scheur, smeulsteen) −2/s, ook op ijs; buiten Nether-3 −0,5/s. Leer −15 % per stuk (vol leer −60 %, nooit immuun). 33 traagheid I · 66 mining fatigue I · 100 bevriezing: vanilla's frost-overlay en bevroren hartjes (ook zichtbaar voor anderen) plus eigen schade 1 per 2 s (doodsbericht "froze in the cold of the Nether"); de bevriezing halveert ook de loopsnelheid (beoordelen in 4b); een brandende speler bevriest niet. Zwaveldamp in ring 3 is alleen sfeer.

**Sparring golf 2 (2026-10-08, alle 33 vragen = aanbevolen; details in `docs/plans/ring-3/golf-2-*.md` §4)**
- **Ertsen:** vanilla-ertsen in ring 3 ×0,35 (gewoon) / 0,3 (midden) / 0,1 (zeldzaam); vulkaniet is het hoofdmateriaal.
- **Vuurkristal-geodes:** gesloten, diep (Y −50…20), ~1 per 12 chunks, straal 4–7; soms aangesneden door een lavabuis.
- **Lavavallen (47c):** alleen waar de val in bestaande lava eindigt (kraterwand, rivierkloof, afgrondwand); ~1 per 2–4 vulkanen, max. 2 bronnen per chunk.
- **Smeulwoud:** gemengd woud, ~6–8 bomen per chunk (smeulbomen 6–9, smeulreuzen 10–12, kale en omgevallen stammen), bessen en vuurbloemen in open plekken.
- **Slapende krater:** mos, grove aarde, varens, azalea, glow lichen/hangende wortels langs de wand, zeldzaam een levende azalea als baken, `ember_berry_bush` aan de rand. Na teststop 4b (47d) een **overwoekerde krater** (zie de aankleding per biome hierboven).
- **Titanenveld:** schedels en losse botten, zielvuur op zielgrond rond de ribbenkasten, zeldzame titanen-rotsen (15–35); geen complete skeletten naast de 39d-ribben.
- **Zeekom:** dode koraalriffen op de rifruggen, gedroogde kelpbalen en dode struikjes, zeemonsterfossielen op de kustterrassen.
- **Zwavel/basalt:** modderpoeltjes in de zinkgatbodems; hornitos (spatkegeltjes 2–4) en gestolde lavatongen.
- **Asvlakte/pieken:** `ash_drift`-laagjes (ook op de treden van de vlakte, besluit 39f), verkoolde dode bomen in geulen, puimsteenkeien; inslagkraters van lavabommen en lage blackstone-rotsen op de pieken.
- **Horror:** afgietsels ~1 groepje (1–3) per 3–4 chunks in `ashen_wastes`, zeldzaam in smeulwoud/titanenveld, kijkend naar de dichtstbijzijnde vulkaan; voetsporen ~1 per 20 chunks (15–40 stappen) die eindigen bij een afgietsel, kraterrand of afgrond.
- **Wezens:** zeldzame striders op lavarivieren en kratermeren (naast de geiten); Nether-3: strays, ghasts en skeletten, geen piglins/hoglins.
- **`steam_fens`:** biome, surface en aankleding nu (modderpoelen, geisers, stoomkolommen, kookpoelen boven Y 63); terreinvorm volgt ring 4.
- **Eilanden:** as en smeulende as op de toppen, 1–3 smeulbomen op grote eilanden, vuurkristalclusters aan de onderkant.
- **Lavabuizen (42):** droge basalttunnels 5–9 breed, Y 20–80, open bij de 39d-sleuven en via daklichten; lava alleen in lagere zijtakken met korstrand. **Magmakamers** 30–60 breed, 15–30 hoog, Y −40…10, ~1 per 400×400, altijd verbonden met een buis. **Vanilla-grotten** blijven in de vlaktes, gedempt op kraterranden/kegelflanken, weg boven Y ~140.
- **Nether-3:** bevroren reuzenzwammen 12–22 zonder lamp met ijspegels; bevroren lavavallen + ijsbruggen over de gletsjerkloven; warme kring van 4–6 blokken rond elke scheur; geen carvers.
- **Sfeer:** rookpluim boven vulkanen tot ~256 blokken (server stuurt vulkaanposities binnen ~512 blokken; nieuwe payload); eigen tint per biome binnen één asrood ringpalet; particles subtiel tot middel (dichter in smeulwoud en `steam_fens`); gerommel elke 2–5 min, 1 op 3 met lichte schok (volgt "Screen Effects"); aswolken (besluit 39f); Nether-3: ademwolkjes, glinstering, wind, krakend ijs.
- **Blokken:** geiser met eigen ritme per positie (80–200 ticks, deterministisch), 1 s waarschuwing, duw ~5; kokende modder blijft traag + hittebron.
- **Kou:** 0 → 100 in ~4 min, ×1,5 op ijs en in `frozen_lava_sea`; 33 traagheid I · 66 mining fatigue I · 100 bevriezing; per leren stuk −15 % opbouw; warmte binnen 4 blokken −2/s. Zwaveldamp is alleen sfeer.
- **Textures:** twee korte bijstuurrondes vóór 4b (r1 op het 39d-terrein, r2 na de aankleding); de grote ronde blijft 57.

**Besluiten tijdens golf 2 en integratie 2 (2026-10-08/09)** — details in de secties hierboven en in `02-beslissingenlog.md`:
- **Werkwijze (infra):** lichte tests per pakket (build, `runData`, unit tests); in-game, determinisme en prestatie één keer in de integratie; review per stroom blijft (gebruiker). 39f, 46-rest en 42 gingen vooruit naar main; integratie 2 merget T → W-nether → C → A, W-biomes als laatste.
- **Textures 57a-r1:** smeulsteen roder met minder gloed, smeulende as lichter, as/askorst rustiger, puimsteen donkerder en zachter, zwavelkorst warmer geel, gloeiende scheur smaller met de onderrand dicht; koudlava, Nether-ijs en rijp-netherrack ongewijzigd (gebruiker: "alles zoals voorgesteld").
- **Wezens:** striders in ring 3 zonder jockeys en zonder groepsbaby's (gebruiker; `Ring3StriderSpawns`, herzien in fase 4).
- **W-biomes (41–49, 41b):** `steam_fens` staat los van `Ring3Biomes.ALL` (eigen `TRANSITION`/`WITH_TRANSITION`), zodat de tests van andere stromen niet veranderen; per-chunk-features zonder biome-filter (de datagen-melding "missing BiomeFilter" is bewust); titanen-rotsen en de geodebekleding schrijven per chunk hun eigen deel van een pure vorm (uitzondering op "plaatsing begint in de eigen chunk").
- **Prestatie:** koude uitschieters titanenveld/smeulwoud (39f) geaccepteerd als JIT-koude start (gebruiker); de officiële meting van integratie 2 kijkt opnieuw.
- **Teststop 4b (2026-10-09):** akkoord met een golf 2-afronding: overwoekerde krater en zwavelkristallen/gekleurde kookpoelen (te kaal), lava tegen lucht en de terreinrestjes (W-terrein), meer zielvuur en een rodere uitbarstingslucht, eigen geiser-waarschuwing, lavabommen niet stapelen, verre pluimen zonder terreinmist, rotaties, kampvuur/ijs/spons; de rest van `teststop-4b.md` is goedgekeurd (o.a. mist Nether-3 zo laten, bevriezing, 21 riftbogen, lavavallen van 1 blok). Zie `docs/plans/ring-3/teststop-4b-feedback.md`.

**Client/server-risico's**
- Water verdampen, geiser-duw, lavabommen en de hitte- en kou-meters: allemaal server-side. De lavabommen zijn gewone server-entities, dus alle clients zien dezelfde.
- Camera-shake, gerommel, as- en stoomparticles, uitbarstingslucht: alleen client (`client/` of biome-data). Fumarole-particles via `animateTick` (draait alleen op de client-level, geen client-class nodig).
- `ticksFrozen` is vanilla entity-data en wordt al gesynct. Onze meter stuurt de waarde server-side aan.
- Water-check: alle plaatsingsroutes afvangen (emmer, dispenser, ijs), anders ontstaan er gaten in de regel.

### Ring 4 — Moerasring (6.000 – 7.800; was 5.500 – 7.500)

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

**Nether-tegenpool — Nether-ring 4 (1.500 – 1.950; was 1.375 – 1.875): droge kristal-/bottenwoestijn**
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

### Ring 5 — Corrupted ring (7.800 – 9.500; was 7.500 – 9.500)

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

**Nether-tegenpool — Nether-ring 5 (1.950 – 2.375; was 1.875 – 2.375): 'zuivere' witte, stille Nether**
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
| 10 | `blocks` | Dood gras/verdorde aarde, dood hout-set (log, wood, planks + houtset). Placeholder-textures (eigen pixels via het texturescript). | B, D; creative tab, loot, tags (log, planks) kloppen |
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
| 35 | `worldgen` | Nether-2-terrein (zwamkathedralen) + biomes reuzenzwamwoud en sporengrotten + dauwbron-features. | B, D, S, W (Nether) |
| 36 | `atmosphere` | Nether-2-sfeer: grijsgroene fog, druppelgeluiden, sporen-particles (biome-data). | runClient |
| 37 | `textures` | Definitieve textures ring 2 + Nether-ring 2. | runClient |

### E. Ring 3 (+ Nether-ring 3)

**Herzien 2026-10-06 (sparring ring 3):** de nummers 38–57 blijven als werkpakketten; ze worden in parallelle golven gebouwd (stromen, contract en integratie: `docs/plans/ring-3/waves.md`, `docs/plans/ring-3/contract.md`). Nieuwe pakketten:

| # | Categorie | Pakket | Golf |
|---|---|---|---|
| 37c | `infra` | Standaard-ringradii 1.500 / 3.500 / **6.000 / 7.800** / 9.500 in `CommonConfig`; contract-sleutels als constanten (`ModBiomes`, `ModTags`); lang-helpers per stroom | 0 |
| 37d | `infra` | Terreinkaart-debugcommando (PNG: hoogte, biome, doorsnede; backlog) voor de review van teststop 4a | 1 |
| 39a | `worldgen` | Ring 3-regio-assen + router-klimaat (`ring_blend`), derde router-as `weirdness` (`caldera_dormant`, V8), stubs van alle gedeelde ring 3-DF's, Nether-3-regio-as | 0 |
| 38b | `blocks` | Smeulwoud- en vuurflora-set, instortende askorst, gloeiend as-afgietsel | 1 |
| 40b | `blocks` | Vuurkristal (blok, knoppen, cluster, item) | 1 |
| 39b | `worldgen` | Biome-skelet ring 3 (7 sub-biomes + `dormant_caldera`), klimaatindeling, kleuren, voorlopige surface met vanilla-blokken | 1 |
| 39c | `worldgen` | Echte ring 3-blokken in de surface (na integratie van `blocks`), vóór teststop 4a | 1 (na merge) |
| 41b | `worldgen` | Vuurkristal-geodes (holte + bekleding, sjabloon dauwgeodes) | 2 |
| 47b | `worldgen` | `titan_fields`: reuzenskeletten, zielvuur, titanen-rotsen (features) | 2 |
| 47c | `worldgen` | `ember_woods` en slapende kraters: bomen, vuurflora, aankleding; lavavallen; geiten-spawns | 2 |
| 53b | `worldgen` | Nether-3-biomes + aankleding (oerijs-pilaren, bevroren lavavallen, ijspegels, warme scheuren) | 2 |
| 57a | `textures` | `Ring3Textures.java`: goede eerste versies van alle ring 3- en Nether-3-textures (37b-stijl) | 1 |

Rij 39 wordt alleen het **terrein** (kegels, caldera's, lavarivieren, zwevende eilanden, asvlaktes, zeekom, titanenveldvorm) + de vloeistofregel. De biomes zitten in 39b/39c, de verkoolde-boom-feature in 47c. Rij 53 wordt alleen het Nether-3-**terrein**; de biomes zitten in 53b. Rij 57 is de eindronde na teststop 4b.
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
