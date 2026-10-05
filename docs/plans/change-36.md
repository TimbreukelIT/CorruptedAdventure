# Change 36 — Nether-ring 2-sfeer: grijsgroene fog, druppelgeluiden en zwevende sporen

Scope: `atmosphere` — change 36

**Doel:** Nether-ring 2 (de doorweekte schimmel-Nether, overworld-equivalent 1.500–3.500, Nether 375–875) krijgt zijn sfeer: een dichte grijsgroene fog, constante druppelgeluiden en zwevende sporen-particles. Het design zegt "alles biome-data" (beslissingenlog 2026-09-30, "geen code nodig"). Daarom is de aanbevolen route puur datagen in de effects van de Nether-ring 2-biomes. Er komt geen gameplay, geen serverstaat en geen netwerk. Client-code komt er alleen bij als een vraag hieronder daarvoor kiest.

Bron:
- `fase-1-wereld.md` §2 Ring 2, "Nether-tegenpool": "Sfeer: dichte grijsgroene fog, constante druppelgeluiden, zwevende sporen-particles (alles biome-data)".
- Bouwplan rij 36 (test: runClient).
- Sjablonen: change 13 (`BiomeAmbience`, eigen sound events via `sounds.json`) en change 32 (palettes per biome in `BiomeAmbience.Palette`, audioscript `tools/sounds/Ring2Sounds.java`).

Buiten scope:
- **Terrein, biomes, features, klimaat, spawns en surface rules** van Nether-ring 2: die horen bij change 35 (`worldgen`). Deze change raakt alleen het effects-blok van die biomes.
- **Blokgeluiden en blokparticles:** druppels uit slijmstalactieten, het gloeien van lichtgevende schimmel en de dauwbron horen bij `blocks` (changes 33/34). Hier gaat het alleen om biome-ambient.
- **Muziek en `mood_sound`** van de Nether-ring 2-biomes blijven zoals change 35 ze zet. De beslissing van change 13 dat muziek vanilla blijft, geldt ook hier.
- **Nether-ringweer:** het design heeft er geen (change 20).
- **Definitieve textures** (change 37). Komt er bij vraag 4 B een eigen particle, dan levert deze change alleen een placeholder-texture.

---

## Afstemming (2026-10-01)

Afgestemd met 28, 32, 33, 34, 35 en 37 (basis: de aanbevolen opties). Namen in dit plan zijn inline vervangen.
- **Biome-namen uit 35 vraag 9 A:** `dripshroom_forest` (was `dripping_fungal_forest`) en `spore_caves` (was `glowing_spore_caves`); palettes `DRIPSHROOM_FOREST` en `SPORE_CAVES` naast `NETHER_RING_2`.
- **Bestaand uit eerdere changes:** `Palette.NETHER_RING_2` (35, placeholder), `BiomeAmbience`-helpers `darken`/`with*` (32), `tools/sounds/Ring2Sounds.java` (32, bij 32 vraag 5 A) — hier alleen een `loop`-variant toevoegen. `registry/ModParticleTypes` en `ModParticleDescriptionProvider` bestaan sinds 28: bij vraag 4 B alleen uitbreiden.
- **Blok-druppels:** de dauwbron druppelt al zelf (34 vraag 7 A, `DRIPPING_WATER`); slijmstalactieten hebben in 33 geen `animateTick`. De biome-druppels hier zijn dus de enige druppelgeluiden.
- **Geodes (35 vraag 6 = B):** dauwbronnen zitten in ingesloten geodes; de bron druppelt binnen de holte. Voor de biome-ambient van deze change verandert niets.
- Teststop: geen eigen stop; de runClient-acceptatie gaat mee in de gebundelde eindcheck ring 2 + Nether-2 (teststop 3) ná change 37 (W-achteraf).

---

## Afstemming na bouw 18–30c (2026-10-05)

Nagelopen tegen de stand na `3abf6f3`; geen wijziging aan de antwoorden en geen nieuwe vragen.
- **`registry/ModParticleTypes` en `ModParticleDescriptionProvider` bestaan** (28: `sand_gust`, een `SimpleParticleType` met provider in `client/particle/SandGustParticle`: 4-argumentconstructor zonder vanilla-snelheidsruis, geen zwaartekracht, getint). Bij vraag 4 B is `DriftingSporeParticle` een tweede klasse in `client/particle/` op dat patroon (lange levensduur, lichte zwaartekracht); de sprites komen uit `textures/particle/` (die map bestaat nog niet; `sand_gust` gebruikt vanilla `generic_0..7`).
- **`client/weather/FogBlend`** (28) is pure fog-rekenkunde (`far`, `near`, `colour`: van vanilla naar het weer, nooit verder dan vanilla) en `RingWeatherRenderer`/`ViewportEvent.RenderFog` is het vaste patroon voor fog-hooks. Bij vraag 2 B/C (dichtere fog) dit patroon volgen in `client/ambient/NetherRing2Fog` en `FogBlend` hergebruiken of uitbreiden voor de menging over de afstand tot de biomegrens, in plaats van een tweede rekenklasse. De weer-hook is in de Nether uit (geen ringweer), dus geen conflict.
- **`BiomeAmbience.Palette`** heeft nu `RING_1` en `RING_2` (placeholder, `desaturated(float)`); `darken`/`with*` en de ring 2-palettes komen in change 32; `Palette.NETHER_RING_2` in change 35.
- **`tools/sounds/` bestaat nog niet** (32 maakt `Ring2Sounds.java` met de variant `drone`); deze change voegt `loop` toe. Valt 32 vraag 10 B (eigen wind als loop), dan bestaat de loopvariant al.
- **`ModSoundEvents`** heeft `ambient.ring_1.strange_cave` en `weather.ring_2.sandstorm`; de Nether-2-events komen erbij.
- **Levendig:** de sfeer van de Nether-tegenhanger (druppels, sporen) is al in het plan een kernpunt; er zijn geen nieuwe keuzes. Bijen/vlinders zijn niet aan de orde (Nether).

## Afhankelijkheden en aannames

De code is gecontroleerd op de stand na change 17 (commit `7714b77`), ververst op 2026-10-05 (zie het blok hierboven). **Changes 18–30c zijn gebouwd; 31–35 volgen nog en hun plannen zijn de bron.** De plannen van 33, 34 en 35 bestaan inmiddels; namen hieronder zijn op 2026-10-01 daarop afgestemd. **Alles hieronder dat uit 18–35 komt is een aanname en moet bij de start opnieuw worden nagelopen** in `CODEBASE.md` en in de antwoorden in `docs/plans/change-18.md` … `change-35.md`.

- **Bestaat al (change 13):**
  - `BiomeAmbience` met `Palette(sky, fog, water, waterFog)`, `desaturate` en `withMood`.
  - `ModSoundEvents` (common) en `ModSoundDefinitionsProvider` (`sounds.json`).
  - Ondertitels in beide lang-providers.
  - Valkuil uit change 13: een sound event dat een biome-JSON noemt moet common geregistreerd zijn, anders laadt de biome niet op de dedicated server. Hetzelfde geldt voor een eigen particle-type (vraag 4 B).
- **Aanname change 32 (plan, antwoorden nog open):**
  - `BiomeAmbience` heeft `darken`, `Palette.darkened`, `withSky`, `withFog`, `withWater` en per-biome-palettes als constanten in `Palette`.
  - Bij change 32 vraag 5 A bestaat `tools/sounds/Ring2Sounds.java`. Dat script synthetiseert deterministische WAV's en zet ze met `ffmpeg … libvorbis -ac 1` om naar gecommitte mono-`.ogg`'s onder `assets/corruptedadventure/sounds/ambient/ring_2/`.
  - `ModSoundDefinitionsProvider` kent daarna ook entries van type `sound`.
  - **Is in change 32 vraag 5 B gekozen** (geen audioscript), dan vervalt vraag 3 A hieronder en wordt 3 B de aanbeveling.
- **Uit het plan van change 35 (afgestemd 2026-10-01; namen volgen change 35 vraag 9 A en het prefix `dripshroom_` van change 33):**
  - De Nether-ring 2-biomes zijn `corruptedadventure:dripshroom_forest` (hoofdbiome, "druipende reuzenzwamwouden") en `corruptedadventure:spore_caves` ("lichtgevende sporengrotten"). Keys: `ModBiomes.DRIPSHROOM_FOREST` en `ModBiomes.SPORE_CAVES`.
  - Ze worden gebouwd in een eigen datagen-bootstrap, werktitel `worldgen/biome/NetherRing2Biomes.java`, naar het patroon van `Ring2Biomes` (change 23). Ze staan in de tag `corruptedadventure:is_nether_ring_2`.
  - `placeholder_nether_ring_2` (fog `2E4A3A`) is weg.
  - De effects zijn voorlopig placeholder-kleuren, zonder particles en zonder `ambient_sound`/`additions_sound`.
  - Er is geen overgangsbiome tussen Nether-ring 1 en 2 (de Nether heeft er geen, §1.14).
  - **Andere namen of een andere plek: dan verschuiven alleen de keys en de plek waar de effects worden gezet.**
- **Aanname change 33:** slijmstalactieten hebben "dripstone-gedrag". Spawnen ze zelf drip-particles en -geluiden, dan klinken die positioneel bovenop het biome-ambient van deze change. Dat is gewenst en geen dubbel werk.
- **Aanname change 21:** `tools/textures/Ring2Textures.java` bestaat (placeholders ring 2), naar het patroon van `Ring1Textures.java`. Alleen nodig bij vraag 4 B.
- **Dimension type:** `corrupted_nether` is een kopie van vanilla `the_nether`, dus `effects: minecraft:the_nether`. Daardoor is de fog in de hele Nether al "foggy" (zie Achtergrond).

---

## Achtergrond (uitgezocht, bij de bouw verifiëren waar aangegeven)

**Fog in de Nether (client, vanilla `FogRenderer`):**
- **Kleur:** de `fog_color` van de biome, zonder dag/nacht-dimming (`NetherEffects` geeft de kleur ongewijzigd door). De client mengt die over enkele kwartblokken (~10–20 blokken). De harde biome-wissel op de ringgrens (gewicht 0,5) geeft dus geen harde kleurlijn.
- **Dichtheid:** de Nether-effecten zijn altijd "foggy". De fog begint op 5 % van de kijkafstand en eindigt op de helft daarvan, met een maximum van 96 blokken (bij 12 chunks: van ~10 tot 96 blokken). Biome-data kan de fog-**afstand** niet veranderen. Dichter dan vanilla-Nether kan alleen met een client-hook (`ViewportEvent.RenderFog`, vraag 2).
- `sky_color` speelt in de Nether geen rol: er is geen lucht. `water_color`/`water_fog_color` zijn bijna irrelevant, omdat water verdampt (`ultrawarm`) en de dauwbron geen fluid is. Ze blijven wel verplichte velden.

**Biome-geluiden (client, `BiomeAmbientSoundsHandler`; zie ook change 32):**
- `ambient_sound` is een **loop** zolang je in de biome bent, met fade in/uit. Vanilla Nether-biomes hebben er allemaal een (`ambient.crimson_forest.loop` enz.).
- `additions_sound` (`sound` + `tick_chance`) is elke tick een kans op een losse klank. **Niet positioneel**: die klinkt "in je hoofd".
- `mood_sound` werkt in de Nether overal: er is geen hemellicht. Die laat deze change ongemoeid.
- Vanilla heeft een echt druppelgeluid: `minecraft:block.pointed_dripstone.drip_water` (en `…drip_water_into_cauldron`). Een `event`-verwijzing in `sounds.json` zet geen vanilla-audio in de repo (zoals change 13).

**Biome-particles (client, `ClientLevel.animateTick`):**
- Per tick worden rond de speler ~1.300 willekeurige posities bekeken (binnen 16 en binnen 32 blokken). Per positie: als de biome een `particle` heeft en `random < probability`, dan spawnt er één particle zonder snelheid.
- De **levensduur** van het type bepaalt dus de dichtheid:
  - `minecraft:crimson_spore`: kort, vanilla `probability` 0,025.
  - `minecraft:warped_spore`: kort, vanilla 0,01428.
  - `minecraft:spore_blossom_air`: groenig, zweeft en zakt heel langzaam, leeft 500–1.000 ticks. Daarom moet de `probability` zeer laag zijn (orde 0,0005: ~0,7 per tick → enkele honderden tegelijk).
  - `minecraft:glow`: lichtgevend en full-bright, in glow-squid-tinten bleekgroen/donkerturkoois; kort levend, stijgt licht. Kleur en levensduur bij de bouw verifiëren in `GlowParticle`.
  - `minecraft:dust`: neemt een kleur mee in de JSON (`{"type":"minecraft:dust","color":[…],"scale":…}`), dus kan in elke kleur zonder code. Leeft wel kort en zweeft nauwelijks.
- Eén particle-type per biome (vanilla `AmbientParticleSettings`).
- **Eigen particle-type** (vraag 4 B) vraagt:
  - common registratie (`DeferredRegister<ParticleType<?>>`, omdat de biome-JSON het type noemt);
  - een client-provider via `RegisterParticleProvidersEvent` in `client/`;
  - `assets/corruptedadventure/particles/<naam>.json`;
  - een texture onder `textures/particle/`.

---

## Plan

### 1. Fog-kleuren (vraag 1, 2)

- **`BiomeAmbience.Palette`:** nieuwe constanten (startwaarden, afstemmen in runClient):

| Constante | Fog | Lucht / water / waterfog | Karakter |
|---|---|---|---|
| `NETHER_RING_2` (basis) | `56645A` | lucht `000000` (zoals `PlaceholderBiomes.NETHER_SKY`); water `3D8C7A` / waterfog `0F2A24` | grijsgroen, vochtig, middeldonker; leest als mist en niet als duisternis |
| `DRIPSHROOM_FOREST` | basis | basis | hoofdbiome, de "standaard" schimmelmist |
| `SPORE_CAVES` | `3E5A50` | basis | donkerder en iets blauwer, zodat de lichtgevende sporen afsteken |

  Bij vraag 1 B is er alleen `NETHER_RING_2`. Bij vraag 1 C worden de fogs lichter (melkachtig, bijv. `8A9A88`).
- **`NetherRing2Biomes`** (change 35): beide biomes krijgen hun eigen palette, alleen in het effects-blok. Een `atmosphere`-change raakt hier een worldgen-bootstrap aan, net als change 13 en 32 deden.
- **Dichtheid** (vraag 2 A): geen code. De Nether-fog is al dicht. Bij vraag 2 B/C: zie stap 5.

### 2. Druppelgeluiden (vraag 3)

- **`ModSoundEvents`:**
  - `ambient.nether_ring_2.drip_loop`: de loop voor `ambient_sound`, een gewoon (niet variable-range) event zoals vanilla-loops;
  - `ambient.nether_ring_2.drips`: losse druppels voor `additions_sound` (variable-range).
  - Beide common geregistreerd.
- **Biome-data** (`NetherRing2Biomes`), startwaarden:
  - beide biomes: `ambient_sound` = `drip_loop`;
  - `additions_sound` = `drips` met `tick_chance` 0,01 in het woud (~1 druppel per 5 s) en 0,02 in de sporengrotten (natter, ~1 per 2,5 s).
- **`ModSoundDefinitionsProvider`** (vraag 3 A, aanbevolen):
  - `drips`: entries van type `event` naar `minecraft:block.pointed_dripstone.drip_water` (gewicht 3) en `…drip_water_into_cauldron` (gewicht 1). Pitch 0,7–1,1 (variatie), volume 0,6–0,9. Ondertitel `subtitles.corruptedadventure.ambient.nether_ring_2.drips`: "Water drips" / "Water druppelt".
  - `drip_loop`: 1–2 entries van type `sound` met `stream: true`: `corruptedadventure:ambient/nether_ring_2/drip_loop_{1,2}`, uit het audioscript. Geen ondertitel, zoals vanilla-loops.
- **Audio** (vraag 3 A): `tools/sounds/Ring2Sounds.java` (change 32) uitbreiden met een tweede soort variant: een **naadloze mono-loop** van 8–12 s. Inhoud:
  - een zacht, laag gefilterd ruisbed (vocht, ruimte);
  - onregelmatige zachte druppels met korte galm;
  - vaste seed per bestand.

  Naadloos maken: de druppels op een periodiek raster binnen de looplengte zetten en de ruis aan begin en eind crossfaden. Uitvoer naar `src/main/resources/assets/corruptedadventure/sounds/ambient/nether_ring_2/drip_loop_N.ogg`, **gecommit**.

  Het script hoort bij ring 2 en Nether-ring 2 samen, net als `Ring1Textures` het zielkristal van Nether-ring 1 maakt. Daarom geen tweede script.
- **Bij vraag 3 B:** geen audio en geen script. `drip_loop` verwijst als `event` naar `minecraft:ambient.underwater.loop` (pitch 0,6–0,7, volume 0,4). Kandidaten bij de bouw beluisteren. Ziet change 35 al een vanilla-loop, dan is dat de fallback.
- **Bij vraag 3 C:** ook de losse druppels gesynthetiseerd (3 varianten `drip_N.ogg`), dus geen vanilla-referenties.
- **Lang:** de ondertitel in `EnglishLanguageProvider` en `DutchLanguageProvider`.

### 3. Sporen-particles (vraag 4)

**Vraag 4 A (aanbevolen, puur data):**
- `dripshroom_forest`: `particle` = `minecraft:spore_blossom_air`, `probability` 0,0005 (startwaarde). Groenige sporen die heel langzaam zweven en zakken.
- `spore_caves`: `particle` = `minecraft:glow`, `probability` ~0,004 (startwaarde). Lichtgevende bleekgroene/turkoois spikkels, passend bij "lichtgevende sporengrotten".
- Beide waarden worden afgestemd op beeld en fps in runClient.

**Vraag 4 B (eigen particle-type):**
- `registry/ModParticleTypes.java` (bestaat sinds change 28, common; hier uitbreiden): `drifting_spore` (`SimpleParticleType`). `ModParticleDescriptionProvider` (change 28) krijgt de twee nieuwe entries.
- `client/particle/DriftingSporeParticle.java` + provider, aangehaakt via `RegisterParticleProvidersEvent` in `CorruptedAdventureClient`. Gedrag naar `SuspendedParticle`/spore blossom: lange levensduur, lichte zwaartekracht, grijsgroene tint. In de sporengrotten een full-bright variant (`glowing_spore`, `getLightColor` = 15).
- `assets/corruptedadventure/particles/{drifting_spore,glowing_spore}.json` en placeholder-textures `textures/particle/spore_{0..3}.png` uit `Ring2Textures.java`. Definitief in change 37.
- Test S verplicht: client-provider niet bereikbaar vanaf de server.

**Vraag 4 C (puur data, `minecraft:dust`):** de exacte grijsgroene kleur via de particle-opties in de biome-JSON. Kort levend, nauwelijks zwevend: leest als "stof" en niet als "zwevende sporen".

### 4. Unit tests

- `BiomeAmbienceTest`: de palettes `NETHER_RING_2`, `DRIPSHROOM_FOREST` en `SPORE_CAVES` bestaan, zijn onderling verschillend (bij 1 A) en anders dan `Palette.RING_1` en de ring 2-palettes.
- `NetherRing2BiomesTest` (change 35, aanname) bijwerken. Per biome:
  - de eigen fog;
  - `ambient_sound` = `drip_loop`;
  - `additions_sound` = `drips` met de verwachte kans;
  - het verwachte particle-type.

  Klimaat, generation settings en spawns blijven gelijk aan de stand van change 35 (zelfde aanpak als `withMood`-test).
- Bij 4 B: een test dat `ModParticleTypes` common is (geen client-imports). Dat wordt in de praktijk door test S afgedekt.

### 5. Alleen bij vraag 2 B/C: extra dichte fog (client)

- `client/ambient/NetherRing2Fog.java`:
  - luistert op `ViewportEvent.RenderFog` (game-bus, via `CorruptedAdventureClient`);
  - alleen in `Level.NETHER` en als de biome op de camerapositie `#corruptedadventure:is_nether_ring_2` is (bij 2 C: alleen `spore_caves`);
  - zet de fog-eindafstand naar ~48 blokken (start ~4).
- De overgang mengt over de afstand tot de biomegrens, zonder harde sprong bij het binnenlopen. Bijvoorbeeld via een eigen sampler over kwartblokken, zoals vanilla de fog-kleur mengt. Bij de bouw uitwerken.
- Pure rekenfunctie (`NetherRing2FogMath`, zonder client-imports) voor de menging, met een unit test.
- Geen `ClientRingLayout` nodig: dit hangt aan de biome, niet aan `ringAt`.
- Stemt af met `RingWeatherClientHooks` (change 20): die doet niets in de Nether, dus er is geen conflict.

### 6. Documentatie

- **`CODEBASE.md`:**
  - `BiomeAmbience`: de Nether-ring 2-palettes;
  - `ModSoundEvents`: twee events;
  - registry-tabel "Overige": `ambient.nether_ring_2.drip_loop` en `.drips`, met `/playsound`-testcommando;
  - `tools/sounds/Ring2Sounds.java`: de loopvariant;
  - bij 4 B: `ModParticleTypes`, `client/particle/`, `particles/` en `textures/particle/`;
  - bij 2 B/C: `client/ambient/NetherRing2Fog`;
  - kopregel: change 36;
  - valkuilen:
    - particle-dichtheid = `probability` × levensduur;
    - fog-afstand niet via biome-data;
    - loop-`.ogg` met `stream: true` en naadloos;
    - eigen particle-types common registreren.
- **`fase-1-wereld.md` §2 Ring 2, Nether-tegenpool:** "Uitwerking (change 36)" met:
  - de fog-hexwaarden per biome;
  - het geluidsmechanisme (loop + druppels, kansen, audiobron);
  - particle-type en kans per biome;
  - de fog-dichtheid.

  Bij 2 B/C of 4 B: "(alles biome-data)" aanpassen naar de gekozen client-aanvulling.
- **`02-beslissingenlog.md`:** de regels hieronder.
- **`docs/backlog.md`:**
  - `[blocks]` positionele druppels uit slijmstalactieten/dauwbron, als change 33/34 dat niet al doen;
  - `[atmosphere]` eventueel een overgangsbiome Nether 1→2 voor een zachtere fog-wissel, als de runClient-check dat vraagt.

Bestanden:
- **nieuw:** 1–2 × `drip_loop_N.ogg` (bij 3 A/C), `drip_N.ogg` (bij 3 C). Bij 4 B: `client/particle/*`, `particles/*.json`, `textures/particle/*.png`. Bij 2 B/C: `client/ambient/NetherRing2Fog.java` en `NetherRing2FogMath.java` + test.
- **gewijzigd:** `BiomeAmbience.java`, `NetherRing2Biomes.java`, `ModSoundEvents.java`, `ModSoundDefinitionsProvider.java`, beide lang-providers, `tools/sounds/Ring2Sounds.java` (bij 3 A/C), `CorruptedAdventure.java` (bij 4 B: registratie), `CorruptedAdventureClient.java` (bij 2 B/C of 4 B), `BiomeAmbienceTest.java`, `NetherRing2BiomesTest.java`, de gegenereerde biome-JSON (2×), `sounds.json`, lang en docs.

---

## Client/server-impact

- **Server:** registreert alleen de sound events (en bij 4 B het particle-type), allemaal common, en laadt de gewijzigde biome-JSON. Zonder common registratie laadt de dedicated server de biomes niet (valkuil change 13). Er is geen tick-handler, geen staat en geen payload.
- **Client:**
  - Bij 1 A / 3 A / 4 A rendert vanilla alles uit de gesyncte biome-registry (fog-kleur, loop, druppels, particles). Er komt geen eigen client-code.
  - `.ogg`, `sounds.json`, particle-JSON en textures zijn client-assets.
  - Bij 4 B staan de provider en de particle-class alleen in `client/particle/`. Bij 2 B/C staat de fog-hook alleen in `client/ambient/`. Beide worden alleen via `CorruptedAdventureClient` (`Dist.CLIENT`) aangehaakt.
- **Netwerk:** geen eigen payloads.
- **Multiplayer:** kleuren, loop en particle-typen zijn per biome en voor iedereen gelijk. Losse druppels en particles vallen per speler op eigen toeval (vanilla-ambient, bewust niet gesynct).
- **Worldgen/determinisme:** niet geraakt. Alleen de effects-velden van twee biomes veranderen.

---

## Tests

Bouwplan: **runClient**. Daarnaast verplicht:
- **B** en **D**, omdat datagen wijzigt;
- **S**, omdat de biome-JSON eigen sound events noemt (en bij 4 B een eigen particle-type, bij 2 B/C een client-hook).

### B — `./gradlew build`
- [ ] `BiomeAmbienceTest` en `NetherRing2BiomesTest` (bijgewerkt) slagen. Bij 2 B/C ook de fog-mengtest.

### D — `./gradlew runData`
- [ ] Bij 3 A/C: eerst het audioscript draaien (`java tools/sounds/Ring2Sounds.java`), loop-`.ogg`'s aanwezig. Twee keer draaien geeft identieke WAV's in `build/sounds/` (`cmp`).
- [ ] Bij 4 B: eerst het texturescript (particle-placeholders).
- [ ] `runData` slaagt (`SoundDefinitionsProvider` controleert de `sound`-entries tegen de `.ogg`'s).
- [ ] De biome-JSON van beide Nether-ring 2-biomes heeft de nieuwe `fog_color`, `ambient_sound`, `additions_sound` en `particle`. De diff raakt alleen `effects`.
- [ ] `sounds.json` bevat beide events (`drip_loop` met `stream: true`). Lang en/nl bevatten de ondertitel.
- [ ] Output gecommit.

### S — `tools/server-smoke-test.sh`
- [ ] Server start met een nieuwe `level-name` (`smoke-change36`), zonder melding over een onbekend sound event of particle-type.
- [ ] Geen client-class-melding (bij 4 B / 2 B/C: provider of fog-hook niet geladen).
- [ ] Een client kan joinen en naar de Nether gaan (runClient tegen de dedicated server).

### W — blokkerende handmatige test: **nee** (W-achteraf)

**Reden:**
- Geen latere change bouwt op deze kleuren, geluiden of particles. Change 37 (textures) gebruikt hooguit de bestandsnaam van een particle-texture (bij 4 B) en die staat in dit plan vast.
- Alle waarden zijn constanten in datagen (of in één client-class). Bijstellen kost een kleine follow-up en raakt bestaande werelden niet: biome-effecten veranderen zonder naden.
- Wat automatisch kan (registratie, JSON-inhoud, server-start) staat onder B, D en S.

De runClient-check hieronder is wel de acceptatie van de rij (afstemmen met de gebruiker, screenshots), maar blokkeert geen volgende change.

**W-achteraf (runClient, nieuwe wereld, preset `corrupted_world`, seed `20260930`, ondertitels aan, kijkafstand 12 chunks):**
- [ ] `/ca tpring 2 nether` in 4 richtingen.
  - Per biome (F3) een screenshot. De fog leest als "dichte grijsgroene mist", niet als duisternis of als vanilla-Nether-rood.
  - De zwamkathedralen blijven als hoge ruimtes leesbaar.
  - Vergelijken met `/ca tpring 1 nether`.
- [ ] `/ca tpboundary 1 nether` (Nether-ring 1 → 2): de fog-kleur verloopt zonder harde lijn. Ook bij 2 B/C: de fog-afstand springt niet.
- [ ] `/locate biome corruptedadventure:spore_caves`: de fog is donkerder en de lichtgevende sporen steken af.
- [ ] Geluid:
  - de loop is constant hoorbaar, met fade bij het in- en uitlopen van de ring;
  - de loop-naad is niet hoorbaar (een minuut luisteren, geen klik);
  - losse druppels klinken met de ondertitel NL/EN, niet irritant bij herhaling;
  - de ambient-schuif regelt het volume.
- [ ] `/playsound corruptedadventure:ambient.nether_ring_2.drips ambient @s` en `…drip_loop`: beide hoorbaar.
- [ ] Particles:
  - zwevende sporen zichtbaar maar niet hinderlijk;
  - F3: het aantal particles (`P:`) stabiel, geen merkbare fps-daling ten opzichte van Nether-ring 1;
  - bij 4 B: de placeholder-texture is zichtbaar en lichtgevend in de grotten.
- [ ] Particles-instelling "Minimal": sporen nemen af (vanilla).
- [ ] Join tegen een dedicated server (`runServer` + `runClient`): de sfeer is gelijk aan singleplayer.

---

## Risico's

- **Changes 33–35 wijken af van de aannames** (andere biome-namen, andere bootstrap, al een vanilla-loop of particles in change 35). Dan verschuift alleen de plek. Bij de start nalopen, net als bij change 32.
- **Particle-dichtheid:** `spore_blossom_air` leeft 500–1.000 ticks. Een te hoge `probability` geeft duizenden particles en fps-verlies. Daarom een zeer lage startwaarde en meten met F3. Vanilla's limiet per particle-render-type (16.384) is het plafond.
- **`glow` is kort en klein** en valt mogelijk weg in de fog. Dan in runClient bijstellen, of 4 B kiezen.
- **Fog te licht:** een lichte fog-kleur maakt in de donkere Nether alles "mistig-licht" en vlak. Een te donkere fog leest als duisternis. Daarom middeldonker als start (`56645A`) en afstemmen met screenshots.
- **Loop-naad:** Ogg Vorbis kan aan het loop-punt een klik geven. Dat beperken we met een periodieke synthese en crossfade. Hoorbaar? Dan de loop langer maken of iets anders crossfaden.
- **Ondertitel-spam:** met `tick_chance` 0,01–0,02 staat "Water druppelt" bijna permanent in beeld als ondertitels aanstaan. Vanilla doet hetzelfde bij dripstone. Te druk? Dan de ondertitel alleen op de loop, of de kans lager.
- **Twee bronnen van druppels:** slijmstalactieten (change 33) kunnen eigen drip-geluiden geven. Samen met de biome-druppels kan dat te druk worden. Afstemmen in runClient: biome-kans omlaag.
- **Harde biome-wissel op de Nether-ringgrens** (32 blokken zone, wissel op 0,5). Vanilla mengt de fog-kleur over ~10–20 blokken, maar de loop wisselt direct (met fade). Dat is acceptabel, anders backlog (overgangsbiome).

---

## Ontwerpvragen

Ronde 1 = fog (1–2), ronde 2 = geluid en particles (3–4).

### 1. Hoe ziet de grijsgroene fog eruit?
- **A. Middeldonker grijsgroen (`56645A`) in het reuzenzwamwoud en donkerder blauwgroen (`3E5A50`) in de sporengrotten (aanbevolen).**
  - Leest als vochtige mist en contrasteert duidelijk met de rode/paarse vanilla-Nether.
  - De lichtgevende sporen en blokken steken af in de grotten.
  - Pure data in `BiomeAmbience.Palette`.
- **B. Eén grijsgroene fog voor beide biomes.**
  - Het eenvoudigst en meest samenhangend.
  - De twee biomes zijn alleen aan terrein en particles te onderscheiden.
- **C. Licht, melkachtig grijsgroen (bijv. `8A9A88`).**
  - Sterkste "mist"-gevoel.
  - Alles op afstand wordt vlak en licht. De Nether voelt minder benauwd en eerder als een wolk, en de zwamkathedralen verliezen diepte.

Antwoord: **A** — middeldonker grijsgroen `56645A` in het druipzwamwoud, donkerder blauwgroen `3E5A50` in de sporengrotten.

### 2. Hoe dicht is de fog?
- **A. Vanilla-Nether-dichtheid: al "foggy", eindigt rond 96 blokken. Alleen de kleur verandert (aanbevolen).**
  - Past bij "alles biome-data", zonder code.
  - De hoge zwamkathedralen blijven als ruimte zichtbaar.
- **B. Extra dicht in heel Nether-ring 2 via een client-hook op de biome (`ViewportEvent.RenderFog`, eind ~48 blokken, geleidelijk gemengd).**
  - Echt "dicht" en benauwd.
  - Kost een client-class + mengfunctie en test S.
  - Botst met het idee van hoge open holtes (je ziet het plafond en de overkant niet meer).
  - Wijkt af van "biome-data" in het design-doc (wordt bijgewerkt).
- **C. Zoals B, maar alleen in de sporengrotten.**
  - De woud-kathedralen blijven open, en de grotten voelen nauw en verstikkend.
  - Dezelfde kosten als B; de fog-afstand wisselt per biome (moet goed mengen).

Antwoord: **A** — vanilla-Nether-dichtheid; alleen de kleur verandert (geen client-code).

### 3. Hoe klinken de constante druppels, en waar komt de audio vandaan?
Vereist een keuze over de audiobron. Sluit aan bij change 32 vraag 5.
- **A. Loop + losse druppels: een eigen gesynthetiseerde, naadloze druppel/vocht-loop (`Ring2Sounds.java`, mono `.ogg`, gecommit) als `ambient_sound`, plus losse druppels als `additions_sound` via een verwijzing naar vanilla's dripstone-druppel (aanbevolen).**
  - "Constant" door de loop, met herkenbare echte druppels erbovenop.
  - Geen vanilla-audio in de repo.
  - Hergebruikt het script van change 32 (alleen als daar 5 A is gekozen).
- **B. Alles via vanilla-verwijzingen: druppels = dripstone-druppel, loop = vanilla `ambient.underwater.loop` (lager gepitcht).**
  - Geen audio en geen tooling.
  - De loop is herkenbaar als "onder water" en past minder. Aanbevolen als change 32 5 B koos.
- **C. Alles eigen synthese: loop én druppels uit het script.**
  - Volledig eigen klank, in één stijl met het zingend zand.
  - Gesynthetiseerde druppels klinken snel kunstmatig; meer iteratie nodig.

Antwoord: **A** — loop + losse druppels: eigen gesynthetiseerde loop via `Ring2Sounds.java` (32 vraag 5 = A) als `ambient_sound`, plus vanilla dripstone-druppels als `additions_sound`.

### 4. Welke sporen-particles?
- **A. Vanilla-particles via biome-data: `spore_blossom_air` (groenige, langzaam zwevende sporen) in het reuzenzwamwoud en `glow` (lichtgevende bleekgroene spikkels) in de sporengrotten (aanbevolen).**
  - Pure data, zoals het design zegt; geen code, geen texture.
  - Kleuren passen redelijk bij grijsgroen.
  - Spelers kunnen de spore blossom herkennen.
- **B. Eigen particle-type `drifting_spore` (+ full-bright `glowing_spore` in de grotten), grijsgroen getint, eigen texture.**
  - Precies de gewenste kleur en lichtgevendheid, en uniek voor de mod.
  - Kost een common registratie, een client-provider en particle-class (in `client/`), particle-JSON, een placeholder-texture (definitief in change 37) en test S.
  - Wijkt af van "alles biome-data".
- **C. `minecraft:dust` met een eigen grijsgroene kleur via de biome-JSON.**
  - Puur data in exact de gewenste kleur.
  - Kort levend en nauwelijks zwevend: leest als stof en niet als zwevende sporen. Niet lichtgevend.

Antwoord: **A** — vanilla-particles via biome-data: `spore_blossom_air` in het woud, `glow` in de sporengrotten.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | atmosphere | Fog Nether-ring 2: woud `56645A`, sporengrotten `3E5A50`; palettes `NETHER_RING_2` + per biome in `BiomeAmbience.Palette` | Vochtige mist met contrast tussen de twee biomes |
| 2026-10-01 | atmosphere | Fog-dichtheid Nether-ring 2: vanilla-Nether, alleen kleur | Pure biome-data; kathedralen blijven zichtbaar |
| 2026-10-01 | atmosphere | Druppelgeluiden Nether-ring 2: eigen gesynthetiseerde loop (`Ring2Sounds.java`) + vanilla dripstone-druppels; events `corruptedadventure:ambient.nether_ring_2.drip_loop` en `.drips` | Constant door de loop, herkenbare druppels erbovenop; geen vanilla-audio in de repo |
| 2026-10-01 | atmosphere | Sporen-particles Nether-ring 2: `spore_blossom_air` (woud), `glow` (grotten) via biome-data | Geen code of texture; zoals het design |
| 2026-10-01 | atmosphere | Muziek en `mood_sound` Nether-ring 2 blijven zoals change 35 ze zet; blok-druppels (slijmstalactieten, dauwbron) zijn `blocks` | Eén bron per laag; volgt change 13 |

Commit (na `/code-review`): `atmosphere: add nether ring 2 fog, drip ambience and spore particles`.
