# Change 32 — Ring 2-sfeer: lucht/fog-kleuren en zingend zand

Scope: `atmosphere` — change 32

**Doel:** ring 2 krijgt zijn definitieve kleuren (lucht, fog, water, waterfog) per biome en het horror-element **zingend zand**: een laag, zoemend geluid in de duinenzee (`dune_sea`), vooral 's nachts. De kleuren zijn biome-data (datagen). Het geluid is een eigen sound event met eigen audio. Hoe het afgaat hangt af van vraag 4: client-only code of puur biome-data. Geen gameplay, geen serverstaat, geen netwerk.

Bron: `fase-1-wereld.md` §2 Ring 2 ("Terrein en sfeer": droog, warm licht, lucht bleek geelblauw, fog zanderig; "Horror-elementen": zingend zand), §1.4 (productiemethode: eigen, deterministisch script), Ring 1-uitwerking change 13 (sjabloon `BiomeAmbience`), bouwplan rij 32 (test: runClient).

Buiten scope:
- **Zandstorm- en hittegolf-rendering** (change 28: fog-afstand, fog-kleur tijdens het weer, particles, trilling). Change 32 zet alleen de **basiskleuren** van de biomes. Het weer van change 28 legt zich daar via `RingWeatherRenderer.fogColor`/`renderFog` overheen. Geen eigen fog-hook, geen fog-afstand.
- **Gras- en bladkleur**: die zijn `worldgen` (change 23/30/31) en blijven ongemoeid, net als in change 13.
- **Grotgeluid (`mood_sound`) en muziek in ring 2**: blijven zoals change 23/30/31 ze zetten (vanilla). De beslissing van change 13 dat muziek vanilla blijft, geldt ook hier.
- Nether-ring 2-sfeer (change 36), fata morgana (fase 5, backlog), ring 2-mobs (fase 4).

---

## Afstemming (2026-10-01)

Afgestemd met 20, 23, 28, 30, 31 en 36 (basis: de aanbevolen opties).
- **Plannen 28/30/31 bestaan:** de aannames hieronder zijn bijgewerkt. Afspraak: biome-kleuren = basis (deze change), weer-fog = overlay (28, constanten in `client/weather/`). Vraag 3 is de enige plek voor de kleur van `dry_riverbeds` (31 vraag 5 gaat niet meer over kleuren).
- **`tools/sounds/Ring2Sounds.java` wordt hier aangemaakt** (bij vraag 5 A) met een generieke opzet (soort variant: `drone` nu, `loop` in change 36), zodat 36 uitbreidt i.p.v. herschrijft. Bij vraag 5 B is er geen script en is 36 vraag 3 B de aanbeveling.
- **`BiomeAmbience`-helpers** (`darken`, `Palette.darkened`, `withSky`, `withFog`, `withWater`) worden hier aangemaakt; 36 gebruikt ze.
- Teststop: geen eigen stop; de runClient-acceptatie gaat mee in de gebundelde eindcheck ring 2 + Nether-2 (teststop 3) ná change 37 (W-achteraf).

## Afstemming na teststop 2 (2026-10-01)

Afgestemd met de plannen 23b en 23c (antwoorden 2026-10-01). **Geen wijziging aan deze change.**
- **Windgeluid in spleetkloven** (fluiten/huilen in de smalle, diepe spleten van 23b) is een mooie aanvulling, maar vraagt ambient **per positie** (niet per biome) en dus client-logica die de spleten herkent. Niet in deze change: backlog `[atmosphere]` (toegevoegd 2026-10-01). Wordt het later gebouwd, dan kan het de synthese-pijplijn (`tools/sounds/Ring2Sounds.java`) van deze change hergebruiken.
- **Wandbronnen (23c-4 C)** maken geen eigen geluid; vanilla-water klinkt vanzelf.
- Change 30 is nu gevolgd door teststop 2b (terrein); deze change blijft in teststop 3.

---

## Afhankelijkheden en aannames

De code is gecontroleerd op de stand na change 17 (commit `7714b77`). **Changes 18–31 zijn nog niet gebouwd. Hun plannen zijn de bron. Alles hieronder dat uit 18–31 komt is een aanname en moet bij de start opnieuw worden nagelopen** in `CODEBASE.md` en de antwoorden in `docs/plans/change-18.md` … `change-31.md`.

- **Bestaat al (change 13):**
  - `BiomeAmbience` (`worldgen/biome/`) met `record Palette(int sky, int fog, int water, int waterFog)`, `Palette.RING_1`, `Palette.desaturated(float)`, `desaturate(rgb, amount)` (luma-menging), `withMood`, `SILENT`/`STRANGE`.
  - `ModSoundEvents` (common `DeferredRegister<SoundEvent>`, `ambient.ring_1.strange_cave` als variable-range event).
  - `ModSoundDefinitionsProvider` (`sounds.json`; tot nu toe alleen entries van type `event`).
  - Ondertitels in beide lang-providers.
  - Valkuil: sound events moeten common geregistreerd zijn, anders laadt een biome-JSON die ze noemt niet op de dedicated server.
- **Aanname change 23 (plan):** `BiomeAmbience.Palette.RING_2` = de placeholder-kleuren (lucht `9FC4E8`, fog `E8D7A8`, water `32A598`, waterfog `0B3A35`), **één plek** die change 32 definitief maakt. Alle ring 2-biomes gebruiken hem via `worldgen/biome/Ring2Biomes.java`. `ModBiomes.CANYONLANDS`, `ModBiomes.OASIS_FLATS` en de tag `corruptedadventure:is_ring_2` bestaan. Het overdrachtsblok van change 23 noemt voor change 32: "`Palette.RING_2` (één plek); mood/ambient per biome in `Ring2Biomes`". `Ring2BiomesTest` controleert "lucht/fog/water = `Palette.RING_2`": die test past deze change aan.
- **Uit het plan van change 30 (afgestemd 2026-10-01):** `salt_flats`, `hoodoo_fields` en `dune_sea` staan in `Ring2Biomes` met keys `ModBiomes.SALT_FLATS`, `HOODOO_FIELDS` en `DUNE_SEA`, en gebruiken ook `Palette.RING_2`. In `dune_sea` is de hele kolom dezelfde biome (de multi-noise van ring 2 laat `depth` op [−2, 2]), dus ook de grotten eronder.
- **Uit het plan van change 31 (afgestemd):** `dry_riverbeds` (overgang 2→3, `ModBiomes.DRY_RIVERBEDS`) staat in `TransitionBiomes`, naast `withered_expanse`, met placeholder `Palette.RING_2.desaturated(DRY_RIVERBEDS_FADE)` (0,3). Deze change vervangt dat door `Palette.DRY_RIVERBEDS` (vraag 3 is de enige plek voor die kleur). Staat hij ergens anders, dan verschuift alleen de plek waar de palette wordt gezet.
- **Uit het plan van change 28 (afgestemd):** de zandstorm kleurt de fog alleen tijdens het weer (`SandstormRenderer.fogColor`, constante `#C9A46B`, gemengd met `strength × exposure`; de hittegolf mengt licht naar warm wit-geel), via de client-hook van change 20 (`RingWeatherRenderer`, `ViewportEvent.ComputeFogColor`). Hij verandert geen biome-data. Zet change 28 toch biome-kleuren of een vaste "stoffige" fog-kleur, dan bij de start afstemmen: de biome-palette blijft van change 32, de weerkleur van change 28.
- **Aanname change 18/20 (plannen):**
  - `CorruptedAdventureClient` is de enige plek waar client-listeners worden aangehaakt.
  - Client-code staat in `client/…`.
  - `ClientRingLayout` bestaat, maar deze change heeft hem **niet** nodig: het zingend zand hangt aan de biome-key, niet aan `ringAt`.
- Vanilla regelt de lucht- en fogkleur bij nacht, regen en zonsondergang zelf. De biome-kleur is de daglichtwaarde.

---

## Achtergrond (uitgezocht, niet opnieuw doen)

**Biome-geluiden in vanilla (client, `BiomeAmbientSoundsHandler`, per speler, biome op de positie van de speler, 3D):**
- `ambient_sound`: een **loop** die doorloopt zolang je in de biome bent, met fade in/uit bij het binnen- en buitengaan (zoals de Nether-biomes). Dag en nacht gelijk.
- `additions_sound` (`sound` + `tick_chance`): elke tick een kans op een losse, niet-positionele klank. Klinkt overal: boven en onder de grond, dag en nacht gelijk.
- `mood_sound`: alleen in het donker zonder hemellicht, dus in de praktijk ondergronds (zie change 13). Niet geschikt voor duinen aan het oppervlak.
- **Geen van de drie kent de tijd van de dag.** "Vooral 's nachts" kan dus niet met pure biome-data (vraag 4).

**Pitch en audio:**
- Minecraft klemt de pitch van elke `SoundInstance` op **0,5–2,0**. Een vanilla-sample kan dus hooguit één octaaf omlaag. Een echt diep zoemen (rond de 70–110 Hz, zoals echte "booming dunes") lukt alleen met eigen audio (vraag 5).
- Positionele geluiden moeten **mono** zijn. Stereo-`.ogg`'s worden niet gedempt over afstand.
- `SoundDefinitionsProvider` controleert entries van type `sound` via `ExistingFileHelper` tegen de `.ogg`-bestanden in `src/main/resources`. Net als bij textures geldt dus: **eerst de audio, dan `runData`**.
- **Lokaal beschikbaar** (gecontroleerd 2026-10-01):
  - `ffmpeg` 7.0.1 met `libvorbis` (`/c/ffmpeg/bin/ffmpeg`).
  - Geen `oggenc` of `sox`.
  - De JDK kan zelf WAV schrijven (`javax.sound.sampled.AudioSystem.write`), maar geen Ogg Vorbis.

**Kleuren:**
- `sky_color` en `fog_color` uit de biome mengt de client over enkele kwartblokken: een korte overgang van ~10–20 blokken, zonder harde lijn (change 13).
- Regen dimt de lucht globaal, ook in een biome zonder neerslag. Dit is vanilla-gedrag: in ring 2 wordt de lucht grauw als het in de wereld regent, terwijl er geen regen valt (zie Risico's).
- Er is in 1.21.1 geen NeoForge-event voor de luchtkleur (change 20, risico). Data is hier dus de enige eenvoudige weg.

---

## Plan

### 1. Kleuren per ring 2-biome (vraag 1, 2, 3)

- **`BiomeAmbience`** (bestaande class, uitbreiden met pure functies):
  - `darken(int rgb, float amount)`: mengt naar zwart.
  - `Palette.darkened(float)`, `Palette.withSky(int)`, `Palette.withFog(int)`, `Palette.withWater(int water, int waterFog)`.
  - **Definitieve palettes** als constanten in `Palette`, zodat er één plek is voor alle kleuren:

| Constante | Lucht | Fog | Water / waterfog | Karakter (startwaarde, afstemmen in runClient) |
|---|---|---|---|---|
| `RING_2` (basis) | `AEC8E0` | `E4D2A6` | `32A598` / `0B3A35` | bleek geelblauw, zanderige fog |
| `CANYONLANDS` | basis | `E2BC93` | basis | roodoranje stof van de kloofsteen |
| `OASIS_FLATS` | `9EC5EC` | `E8DDBA` | basis | frisser, iets blauwer: "veilig" |
| `SALT_FLATS` | `BCD3E8` | `EEEAE0` | basis | verblindend, bijna wit |
| `HOODOO_FIELDS` | basis | `DDC291` | basis | oker |
| `DUNE_SEA` | `B4CADA` | `EACF96` | basis | goudgeel, het warmst |
| `DRY_RIVERBEDS` | `RING_2.desaturated(0,35).darkened(0,1)` | idem | idem (troebel) | grauw, geblakerd richting ring 3 |

  Bij vraag 1 B is er alleen `RING_2` (alle biomes dezelfde kleuren). Bij vraag 1 C komen er sterkere afwijkingen per biome. Bij vraag 2 wordt de lucht aangepast. Bij vraag 3 wordt `DRY_RIVERBEDS` aangepast.
- **`Ring2Biomes`** (change 23/30): elke biome krijgt zijn eigen palette i.p.v. `Palette.RING_2`. Dit is alleen het effects-blok: geen klimaat, features of spawns. Een atmosphere-change raakt hier een worldgen-bootstrap aan, net als change 13 dat deed met de bootstraps van change 8 en 11.
- **`TransitionBiomes`** (change 31): `dry_riverbeds` gebruikt `Palette.DRY_RIVERBEDS`.
- **Gras en blad**: blijven zoals ze zijn.

### 2. Sound event en audio (vraag 5)

- **`ModSoundEvents`**: `ambient.ring_2.singing_sand` (`createVariableRangeEvent`), common geregistreerd.
- **Audio (vraag 5 A):**
  - Een nieuw script `tools/sounds/Ring2Sounds.java`: single-file Java, alleen JDK, buiten Gradle en de jar, zoals het texturescript.
  - Het script synthetiseert per variant deterministisch een WAV (vaste seed per bestand) in `build/sounds/`, niet in git. Inhoud per variant: een **mono** drone van 4–8 s, grondtoon 70–110 Hz met 2–3 boventonen, trage zwelling (attack ~1,5 s, release ~2 s), lichte beat/tremolo en een zweem gefilterde ruis ("zand").
  - Daarna roept het `ffmpeg -c:a libvorbis -ac 1 -q:a 4` aan. Dat levert `src/main/resources/assets/corruptedadventure/sounds/ambient/ring_2/singing_sand_{1,2,3}.ogg` op.
  - De `.ogg`'s worden **gecommit**: CI en `build` hebben geen ffmpeg nodig.
  - Ontbreekt ffmpeg, dan stopt het script met een duidelijke melding. Het pad naar ffmpeg is in te stellen via een omgevingsvariabele of argument.
  - Tweemaal draaien geeft byte-identieke WAV's. De `.ogg` kan per ffmpeg-versie verschillen: dat is geaccepteerd, de WAV is de bron.
- **Audio (vraag 5 B):** geen script en geen `.ogg`. Het event krijgt in `sounds.json` entries van type `event` naar vanilla-geluiden (kandidaten bij de bouw beluisteren: `minecraft:block.beacon.ambient`, `minecraft:block.conduit.ambient`), met pitch 0,5–0,6.
- **`ModSoundDefinitionsProvider`**: entry `ambient.ring_2.singing_sand`.
  - Bij 5 A: drie entries van type `sound` (`corruptedadventure:ambient/ring_2/singing_sand_N`), volume 0,7–1,0, pitch 0,9–1,1, `attenuation_distance` 24.
  - Ondertitel `subtitles.corruptedadventure.ambient.ring_2.singing_sand`: "Sand hums" / "Het zand zingt".
- **Lang**: de ondertitel in `EnglishLanguageProvider` en `DutchLanguageProvider`.

### 3. Trigger zingend zand (vraag 4, 6, 7)

**Vraag 4 A (aanbevolen): client-only, `client/ambient/SingingSandAmbience.java`.**
- Listener op `ClientTickEvent.Post`, aangehaakt in `CorruptedAdventureClient` (game-bus). Niet tijdens pauze, alleen met een `LocalPlayer` in een `ClientLevel`.
- **Voorwaarden** (vraag 7):
  - de biome op de positie van de speler is `corruptedadventure:dune_sea` (`level.getBiome(pos).is(ModBiomes.DUNE_SEA)`);
  - de speler staat aan het oppervlak: hemellicht op de positie ≥ 10, zodat het niet in grotten onder de duinen klinkt. De exacte API (`getBrightness(LightLayer.SKY, …)`) bij de bouw verifiëren.
- **Kans per tick** (vraag 6) uit een pure functie `chancePerTick(float nightFactor)`, package-private en unit-testbaar:
  - `nightFactor` (0 overdag → 1 's nachts) komt uit de zonnehoek (`level.getTimeOfDay(partialTick)`) met een zachte overgang in de schemering. Bij de bouw verifiëren.
  - Startwaarden: nacht gemiddeld 1× per ~40 s (1/800 per tick), dag 1× per ~4 min (1/4.800).
  - Na elke klank een cooldown van 12 s, zodat klanken niet overlappen.
- **Afspelen:**
  - een positionele `SimpleSoundInstance` (`SoundSource.AMBIENT`, lineaire demping) op een willekeurig punt 8–16 blokken horizontaal rond de speler, op ooghoogte ± 2. Het klinkt dus "uit de duinen", uit een wisselende richting;
  - de random komt van `level.random` van de client. Bewust niet gesynct: ambient, zoals vanilla.
- Geen netwerk, geen serverstaat, geen `ClientRingLayout`.

**Vraag 4 B (puur data):** in `Ring2Biomes` krijgt `dune_sea` `additions_sound` = `ambient.ring_2.singing_sand` met `tick_chance` ~0,0006 (1× per ~80 s, dag en nacht gelijk). Er komt dan geen client-code. Nadelen:
- de klank is niet positioneel (klinkt "in je hoofd");
- hij klinkt ook in grotten onder de duinen;
- "vooral 's nachts" vervalt: het design-doc wordt hierop aangepast.

**Vraag 4 C (puur data, loop):** `dune_sea` krijgt `ambient_sound` = een loop-variant (`ambient.ring_2.singing_sand_loop`, met een naadloze loop-`.ogg` bij 5 A), die zacht maar constant loopt in de hele biome. Ook hier vervalt "vooral 's nachts".

### 4. Tests (unit)

- `BiomeAmbienceTest` uitbreiden:
  - `darken(x, 0)` = x en `darken(x, 1)` = zwart;
  - `withSky`/`withFog`/`withWater` veranderen alleen hun eigen veld;
  - de ring 2-palettes zijn onderling verschillend (bij 1 A) en anders dan `RING_1`.
- `Ring2BiomesTest` (change 23) bijwerken: elke ring 2-biome heeft **zijn eigen** palette, en `dry_riverbeds` heeft `Palette.DRY_RIVERBEDS`.
  - Bij 4 B/C ook: alleen `dune_sea` heeft het eigen additions-/loop-geluid, de andere ring 2-biomes niet.
  - Bij 4 A: geen enkele ring 2-biome heeft `additions_sound` of `ambient_sound` (het geluid zit in de client-code).
- Bij 4 A: `SingingSandAmbienceTest` op de pure functie:
  - `chancePerTick(0)` = dagwaarde, `chancePerTick(1)` = nachtwaarde;
  - monotoon stijgend over `nightFactor`;
  - nooit < 0 of > 1.

  De testclass laadt geen `Minecraft`-classes: de rekenfunctie staat in een kleine class zonder client-imports, binnen `client/ambient/` (werktitel `SingingSandTiming`).
- Bij 5 A wordt het script niet in Gradle getest. Controle: het script twee keer draaien geeft identieke WAV's in `build/sounds/` (handmatig, `cmp`).

### 5. Documentatie

- **`CODEBASE.md`:**
  - `BiomeAmbience`: palettes per ring 2-biome, `darken`, `with*`;
  - `ModSoundEvents`: nieuw event;
  - `client/ambient/` (bij 4 A);
  - `tools/sounds/Ring2Sounds.java` en `assets/corruptedadventure/sounds/` (bij 5 A);
  - registry-tabel "Overige": `ambient.ring_2.singing_sand` met `/playsound`-testcommando;
  - kopregel: change 32;
  - valkuilen: pitch-klem 0,5–2,0, mono voor positioneel, `.ogg` vóór `runData`, regen dimt de lucht ook in ring 2, plus wat er bij de bouw opduikt.
- **`fase-1-wereld.md` §2 Ring 2:** "Uitwerking (change 32)" met:
  - de hexwaarden per biome;
  - het mechanisme en de frequentie van het zingend zand;
  - de audiomethode.

  Pas bij 4 B/C de regel "vooral 's nachts" aan. Bij 4 A verandert "(biome-ambient, data)" in "(client-only ambient op de biome-key)".
- **`fase-1-wereld.md` §1.4 of een nieuw kopje "Geluid"** (bij 5 A): productiemethode audio = eigen, deterministisch synthesescript (JDK → WAV) + ffmpeg → mono Ogg Vorbis, gecommit. Geen vanilla-audio in de repo. Dit is het sjabloon voor de geluiden van ring 3–5 (changes 50, 70, 90).
- **`02-beslissingenlog.md`:** de regels hieronder.
- **`docs/backlog.md`:**
  - `[atmosphere]` regen dimt de lucht in ring 2 (geen neerslag): eventueel aanpakken met het ringweer (change 20/28) via een eigen `DimensionSpecialEffects` of een mixin;
  - `[infra]` audioscript in Gradle hangen (alleen als ring 3 een tweede script toevoegt, zoals bij de textures);
  - eventueel `[atmosphere]` zingend zand luider of stil tijdens een zandstorm (interactie met change 28), als dat ter sprake komt.

Bestanden:
- **nieuw:** `client/ambient/SingingSandAmbience.java` en `SingingSandTiming.java` (bij 4 A), `tools/sounds/Ring2Sounds.java` en 3 × `.ogg` (bij 5 A), `SingingSandAmbienceTest.java` (bij 4 A);
- **gewijzigd:** `BiomeAmbience.java`, `Ring2Biomes.java`, `TransitionBiomes.java`, `ModSoundEvents.java`, `ModSoundDefinitionsProvider.java`, beide lang-providers, `CorruptedAdventureClient.java` (bij 4 A), `BiomeAmbienceTest.java`, `Ring2BiomesTest.java`, de gegenereerde biome-JSON (6×), `sounds.json` en lang, docs.

---

## Client/server-impact

- **Server:** registreert alleen het sound event (common) en laadt de gewijzigde biome-JSON. Geen tick-handler, geen staat, geen payload.
  - Bij 4 B/C noemt de biome-JSON het event: zonder common registratie laadt de dedicated server de biome niet (valkuil change 13).
- **Client:** vanilla rendert de kleuren uit de gesyncte biome-registry.
  - Bij 4 A draait `SingingSandAmbience` alleen in `client/ambient/` en wordt alleen via `CorruptedAdventureClient` (`Dist.CLIENT`) aangehaakt. Common code noemt hem nergens. Hij leest alleen client-staat: biome, licht en tijd, die vanilla al synct.
  - `.ogg` en `sounds.json` zijn client-assets: de server leest ze niet.
- **Netwerk:** geen eigen payloads.
- **Multiplayer:** kleuren zijn per biome en voor iedereen gelijk. Het zingend zand speelt per speler op eigen toeval: twee spelers naast elkaar horen het op verschillende momenten (zoals vanilla-ambient, bewust niet gesynct; zo ook vastgelegd voor de horrorgeluiden van ring 3 en 4). Is dat ongewenst, dan zou de server het geluid moeten afspelen. Dat is buiten deze rij en niet aanbevolen.
- **Worldgen/determinisme:** niet geraakt. Alleen effects-velden van biomes veranderen; klimaat, features en spawns blijven gelijk.

---

## Tests

Bouwplan: **runClient**. Daarnaast verplicht **B** en **D** (datagen gewijzigd), en **S**: bij 4 A omdat er client-code bij komt, bij 4 B/C omdat de biome-JSON een eigen sound event noemt.

### B — `./gradlew build`
- [ ] `BiomeAmbienceTest`, `Ring2BiomesTest` (bijgewerkt) en, bij 4 A, `SingingSandAmbienceTest` slagen.

### D — `./gradlew runData`
- [ ] Bij 5 A: eerst het audioscript draaien, `.ogg`'s aanwezig.
- [ ] `runData` slaagt.
- [ ] De gegenereerde biome-JSON van `canyonlands`, `oasis_flats`, `salt_flats`, `hoodoo_fields`, `dune_sea` en `dry_riverbeds` heeft de nieuwe kleuren. Bij 4 B/C heeft `dune_sea` ook het geluid.
- [ ] `sounds.json` bevat `ambient.ring_2.singing_sand` met ondertitel, lang en/nl.
- [ ] Output gecommit.
- [ ] De diff van de gegenereerde biomes raakt alleen `effects` (geen features, spawns of klimaat).

### S — `tools/server-smoke-test.sh`
- [ ] Server start zonder fouten, met de nieuwe `level-name` (`smoke-change32`).
- [ ] Geen melding over een onbekend sound event.
- [ ] Geen client-class-melding: `SingingSandAmbience` wordt niet geladen.
- [ ] Een client kan joinen (runClient tegen de dedicated server, zie hieronder).

### W — blokkerende handmatige test: **nee** (W-achteraf)

**Reden:**
- Geen latere change bouwt op deze kleuren of dit geluid. Change 28 (zandstorm) komt eerder en kleurt alleen tijdens het weer.
- Alle waarden zijn constanten in datagen of in één client-class: bijstellen na de check kost een kleine follow-up zonder gevolgen voor werelden. Biome-effecten veranderen in bestaande werelden zonder naden.
- Wat automatisch kan (registratie, JSON-inhoud, kansfunctie, server-start) staat onder B, D en S.

De runClient-check hieronder is wel de acceptatie van de rij (afstemmen met de gebruiker, screenshots), maar blokkeert geen volgende change.

**W-achteraf (runClient, nieuwe wereld, preset `corrupted_world`, seed `20260930`, ondertitels aan):**
- [ ] `/ca tpring 2` in 4 richtingen. Per biome (F3) een screenshot overdag, bij zonsondergang en 's nachts. Lucht leest als "bleek geelblauw", fog als "zanderig"; elke biome is herkenbaar maar ze horen bij elkaar (vraag 1). Niet te wit of te grauw. Vergelijken met ring 1 (`/ca tpring 1`).
- [ ] Overgang tussen twee ring 2-biomes en bij `/ca tpboundary 1` (`withered_expanse` → ring 2): geen harde lijn.
- [ ] `/ca tpboundary 2`: `dry_riverbeds` grauw/geblakerd. Het verschil met ring 2 en met de placeholder van ring 3 is zichtbaar.
- [ ] Water in een oasepoel en onder water (waterfog): turkoois, helder genoeg.
- [ ] `/playsound corruptedadventure:ambient.ring_2.singing_sand ambient @s`: laag gezoem dat als "zingend zand" leest, met de ondertitel NL/EN. Niet irritant bij herhaling, geen klik aan begin of eind.
- [ ] In `dune_sea` (`/locate biome corruptedadventure:dune_sea`):
  - [ ] **Bij 4 A:** `/time set night` → binnen ~1–2 min meerdere klanken, uit wisselende richtingen (koptelefoon). `/time set day` → zelden (≤ 1 per ~4 min). In een grot onder de duinen: niets. Net buiten de biome: niets.
  - [ ] **Bij 4 B/C:** hoorbaar met de gekozen frequentie of als loop. Fade in/uit bij het binnen- en buitengaan (C).
- [ ] De ambient-schuif in de geluidsopties regelt het volume; op 0 is het stil.
- [ ] Tijdens een zandstorm (`/ca weather 2 sandstorm`, change 28): de fog van het weer ligt over de biome-fog zonder kleurbreuk bij het begin en einde van de storm.
- [ ] Wereldregen (`/weather rain`): de lucht in ring 2 wordt grauw (vanilla, bekend; backlog). Controleren dat het niet storend is.
- [ ] Join tegen een dedicated server (`runServer` + `runClient`): kleuren en geluid gelijk aan singleplayer. Twee clients horen het geluid op verschillende momenten (verwacht).

---

## Risico's

- **Changes 23/30/31 wijken af van de aannames** (andere plek of naam voor de bootstraps, `dry_riverbeds` niet in `TransitionBiomes`, `Palette.RING_2` niet gebruikt): dan verschuift alleen de plek waar de palettes worden gezet. Bij de start nalopen.
- **Overlap met change 28:**
  - Zet de zandstorm ook biome-kleuren, dan zijn er twee bronnen voor dezelfde fog. Afspraak: biome = basis (32), weer = overlay (28).
  - Bij een harde wissel tussen de biome-fog en de weer-fog: kleur in change 28 mengen met `strength`.
- **Regen dimt de lucht** ook in ring 2 (vanilla). Niet op te lossen met data. Backlog.
- **Geluid irritant of onhoorbaar:** een drone van 70–110 Hz is op laptopspeakers bijna onhoorbaar. Daarom de boventonen (vraag 5 A) en het afstemmen in runClient. Te vaak = irritant: cooldown en startwaarden zijn constanten.
- **Pitch-klem 0,5** (bij 5 B): vanilla-samples worden niet laag genoeg en blijven herkenbaar (een beacon-zoem). Daarom 5 A aanbevolen.
- **ffmpeg** is niet overal aanwezig: alleen nodig om de audio opnieuw te maken. De `.ogg` is gecommit, dus build en CI hebben het niet nodig. Gedocumenteerd in `CODEBASE.md`.
- **Hemellicht als oppervlaktecheck** (4 A): onder een overhang of 's nachts blijft het hemellicht gelijk (sky light ≠ daglicht), dus de check is "open hemel boven je", niet "licht". Bij de bouw verifiëren dat de client-lichtdata in nieuw geladen chunks klopt. Anders `level.canSeeSky(pos)`.
- **Biome is 3D:** in `dune_sea` is de hele kolom dezelfde biome (aanname change 30). Krijgt change 30 ook grotbiomes op diepte, dan beperkt de oppervlaktecheck het geluid toch tot het oppervlak.
- **Ondertitel-spam:** bij 4 A met de cooldown hooguit 1 per 12 s.

---

## Ontwerpvragen

Ronde 1 = kleuren (1–3), ronde 2 = zingend zand (4–7).

### 1. Hoe zijn de kleuren over de ring 2-biomes verdeeld?
- **A. Eén basispalet voor ring 2 met per biome een eigen nuance, vooral in de fog (aanbevolen).**
  - Canyonlands roodoranje, oase frisser blauw, zoutvlakte bijna wit, hoodoos oker, duinen goud.
  - Ring 2 voelt als één geheel, maar je merkt aan de horizon dat je een andere biome inloopt; ook nuttig voor oriëntatie.
  - Pure data, alles in `BiomeAmbience.Palette`.
- **B. Eén palet voor alle ring 2-biomes.**
  - Het eenvoudigst en zeer samenhangend.
  - Biomes zijn alleen aan het terrein te onderscheiden; de oase voelt niet anders.
- **C. Sterk eigen kleuren per biome** (bijv. rode lucht boven de canyons, witte waas boven de zoutvlakte).
  - Opvallend en goed herkenbaar, maar de ring valt uiteen in losse thema's.
  - Biome-overgangen tonen een duidelijke kleurwissel (~10–20 blokken).

Antwoord: **A** — één basispalet voor ring 2 met per biome een eigen nuance (vooral in de fog), alles in `BiomeAmbience.Palette`.

### 2. Hoe ver wijkt de lucht af van vanilla?
- **A. Bleek geelblauw (design-letterlijk): minder verzadigd dan vanilla, licht naar warm getrokken (`AEC8E0`), zanderige fog (`E4D2A6`) (aanbevolen).**
  - Leest direct als "droog en heet"; herkenbaar als lucht.
  - "Warm licht" = warme lucht- en fogtinten, geen lichtwijziging.
- **B. Vanilla-woestijnlucht (`6EB1FF`) met alleen een zanderige fog.**
  - Dicht bij vanilla en vertrouwd.
  - De ring voelt minder anders; het contrast met ring 1 zit vooral aan de horizon.
- **C. Sterk wazig: lucht en fog bijna gelijk, licht geelwit (hitte-waas).**
  - Sterke sfeer, maar overdag vlak en vermoeiend.
  - Overlapt visueel met de hittegolf (change 28).
- **D. A + een warme tint op het licht zelf** (client-side lightmap-aanpassing in ring 2).
  - Echt "warm licht" op blokken.
  - Kost eigen client-code (render-hook of mixin, ringdata via `ClientRingLayout`), test S, en mogelijk botsing met Sodium/shaders. Groter dan deze rij.

Antwoord: **A** — bleek geelblauw: lucht `AEC8E0`, zanderige fog `E4D2A6`; geen lichtwijziging.

### 3. Welke kleuren krijgt `dry_riverbeds` (overgang 2→3)?
- **A. Het ring 2-basispalet, 35 % ontzadigd en 10 % donkerder: grauw en geblakerd, een voorbode van ring 3 (aanbevolen).**
  - Zelfde aanpak als `withered_expanse` (change 13), reproduceerbaar met `desaturate` + `darken`.
- **B. Gelijk aan het ring 2-basispalet.**
  - Eenvoudig, maar de overgang naar ring 3 zit dan alleen in het terrein.
- **C. Halverwege ring 2 en de (placeholder-)kleuren van ring 3.**
  - Echte brug tussen de ringen, maar afhankelijk van de ring 3-kleuren, die pas in change 50 definitief worden; dan opnieuw afstemmen.

Antwoord: **A** — ring 2-basispalet 35 % ontzadigd en 10 % donkerder (`Palette.DRY_RIVERBEDS`).

### 4. Hoe gaat het zingend zand af?
Pure biome-data kent de tijd van de dag niet (zie Achtergrond).
- **A. Client-only class (`client/ambient/SingingSandAmbience`): in `dune_sea`, aan het oppervlak, kans per tick afhankelijk van de tijd (nacht vaak, dag zelden), positioneel uit een willekeurige richting (aanbevolen).**
  - Doet precies wat het design vraagt ("vooral 's nachts"); klinkt "uit de duinen".
  - Eén kleine client-class + een pure, geteste rekenfunctie.
  - Geen netwerk of serverstaat; test S erbij.
  - Wijkt af van "(biome-ambient, data)" in het design-doc (wordt bijgewerkt).
- **B. Biome-data: `additions_sound` op `dune_sea`.**
  - Geen code, past bij het design-doc ("data").
  - Dag en nacht even vaak; niet positioneel ("in je hoofd"); klinkt ook in grotten onder de duinen.
- **C. Biome-data: zachte, constante loop (`ambient_sound`) in `dune_sea`.**
  - Geen code; voelt als een altijd aanwezig zoemen met fade bij binnenkomst.
  - Geen nachtnadruk, en een constant geluid went snel of gaat irriteren.

Antwoord: **A** — client-only class `client/ambient/SingingSandAmbience` (kans per tick afhankelijk van de tijd, positioneel); het design-doc wordt bij de bouw bijgewerkt ("data" → client-class).

### 5. Waar komt het geluid vandaan?
- **A. Eigen synthese: `tools/sounds/Ring2Sounds.java` (JDK → WAV, vaste seed) + ffmpeg → 3 mono `.ogg`-varianten, gecommit (aanbevolen).**
  - Echt laag (70–110 Hz) en eigen; geen vanilla-audio in de repo (zelfde principe als de textures).
  - Deterministische bron; ffmpeg 7.0.1 met libvorbis staat al lokaal. Build en CI hebben het niet nodig.
  - Kwaliteit "nette programmer-audio", te verbeteren met iteraties of later te vervangen door eigen opnames zonder andere wijzigingen.
- **B. Vanilla-samples via `sounds.json` (type `event`, bijv. beacon-/conduit-zoem, pitch 0,5), zoals change 13.**
  - Geen audio, geen tooling.
  - Door de pitch-klem (≥ 0,5) niet echt laag, en spelers herkennen de beacon. Minder eng.
- **C. De gebruiker levert `.ogg`-bestanden aan.**
  - Meest eigen resultaat, maar de change wacht op de audio. Tot dan geldt B als placeholder.

Antwoord: **A** — eigen synthese `tools/sounds/Ring2Sounds.java` (JDK → WAV, vaste seed) + ffmpeg → 3 mono `.ogg`-varianten, gecommit.

### 6. Hoe vaak klinkt het? (bij 4 A; bij 4 B/C alleen het gemiddelde)
- **A. 's Nachts gemiddeld 1× per ~40 s, overdag 1× per ~4 min, cooldown 12 s, zachte overgang in de schemering (aanbevolen).**
  - Overdag een zeldzame vreemde gewaarwording; 's nachts duidelijk onrustig zonder te irriteren.
- **B. Alleen 's nachts (overdag nooit), 1× per ~30 s.**
  - Sterk contrast en heel voorspelbaar; overdag is de duinenzee gewoon stil.
- **C. Zeldzaam: 's nachts 1× per ~2 min, overdag nooit.**
  - Subtiel; veel spelers missen het, het blijft een gerucht.

Antwoord: **A** — 's nachts gemiddeld 1× per ~40 s, overdag 1× per ~4 min, cooldown 12 s, zachte overgang in de schemering.

### 7. Waar klinkt het?
- **A. Alleen in `dune_sea`, alleen aan het oppervlak (open hemel) (aanbevolen).**
  - Volgt het design ("in de duinenzee"); de duinen zelf "zingen"; ondergronds stil.
  - Hangt aan de biome-key van change 30.
- **B. In `dune_sea` vaker, in andere zandige ring 2-biomes (`oasis_flats`, `salt_flats`) zelden.**
  - Meer ring-breed onbehagen; de oase is dan minder "veilig".
  - Iets meer logica (kans per biome).
- **C. In `dune_sea`, ook ondergronds.**
  - Eenvoudigste check (alleen de biome); in grotten onder de duinen kan het verwarrend klinken.

Antwoord: **A** — alleen in `dune_sea`, alleen aan het oppervlak (open hemel).

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | atmosphere | Kleuren ring 2: één basispalet met per biome een nuance (vooral fog); definitieve palettes in `BiomeAmbience.Palette` (`RING_2` + per biome) | Ring 2 voelt als geheel; biomewissel zichtbaar aan de horizon |
| 2026-10-01 | atmosphere | Lucht ring 2: bleek geelblauw `AEC8E0`, fog `E4D2A6`; gras/blad blijven worldgen | Leest als droog en heet; geen client-lichtcode |
| 2026-10-01 | atmosphere | `dry_riverbeds`: ring 2-palet 35 % ontzadigd, 10 % donkerder | Grauw voorbode van ring 3; zelfde aanpak als `withered_expanse` |
| 2026-10-01 | atmosphere | Zingend zand, mechanisme: client-only class `SingingSandAmbience` (tijdafhankelijk, positioneel) | "Vooral 's nachts" kan niet met biome-data |
| 2026-10-01 | atmosphere | Audio zingend zand: eigen synthese `Ring2Sounds.java` + ffmpeg, 3 mono `.ogg`; event `corruptedadventure:ambient.ring_2.singing_sand` | Echt laag en eigen; geen vanilla-audio in de repo |
| 2026-10-01 | atmosphere | Frequentie en plek zingend zand: 's nachts ~1×/40 s, overdag ~1×/4 min, cooldown 12 s; alleen `dune_sea` aan het oppervlak | Onrustig zonder te irriteren; de duinen zelf zingen |
| 2026-10-01 | atmosphere | Biome-kleuren = basis (change 32), weer-fog = overlay via de client-hook (change 28) | Eén bron per laag, geen dubbele fog-kleur |

Commit (na `/code-review`): `atmosphere: add ring 2 sky and fog colours and singing sand ambience`.
