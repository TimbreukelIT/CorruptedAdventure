# Golf 2 — stroom A (sfeer): 50-0 (voorpakket), 50 ring 3-sfeer, 56 Nether-3-sfeer

Scope: `atmosphere` — één commit per pakket, branch `ring3/atmosphere-g2`, poort 5 (server 25575, RCON 25585). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

**Doel:** ring 3 en Nether-3 krijgen hun definitieve sfeer:
- lucht- en fogkleuren per biome;
- biome-particles (as, vonken, stoom, ijsglinstering);
- ambient-geluid (gerommel, vonken, lava, titanen-zoem, krater, wind, krakend ijs);
- het onregelmatige gerommel met lichte camera-shake buiten het weer;
- eigen gesynthetiseerde audio (`Ring3Sounds.java`), ook voor het ringweer (nu vanilla-verwijzingen) en de blokgeluiden van 46.

Teststop 4b beoordeelt "sfeer en geluid".

**Bron:**
- `fase-1-wereld.md` Ring 3 ("Terrein en sfeer", wow-moment grens 2→3, Horror: gerommel en trillingen), Nether-3 "Sfeer", §1.4/§1.6, "Geluid (productiemethode, change 32)";
- `contract.md` §6 (sound events, particle types);
- `golf-1-atmosphere.md` (45: `Rumble`, `RingWeatherFog`, `WeatherParticles`, `WeatherLoop`, mixin luchtkleur);
- backlog r. 182, 188, 201, 202;
- sjablonen `BiomeAmbience` (paletten, moods), change 32/36 (`Ring2Sounds.java`, `oasis`-additions, `drip_loop`), `tools/sounds/Ring2Sounds.java`.

## 1. Pakketten

### 50-0 — Voorpakket golf 2 (op main, sequentieel, vóór de andere stromen afsplitsen; ~1 uur)
Voorstel van de plan-agent (procesvraag voor main): net als golf 0 een klein, gedrag-neutraal fundament, zodat W-biomes, W-nether en B niet op A hoeven te wachten en niemand in andermans bestand schrijft.
1. **Haken in `BiomeAmbience`:** `ring3(ResourceKey<Biome>)` en `netherRing3(ResourceKey<Biome>)` geven de effecten (palet + later particles/geluid) per biome.
   - In 50-0 alleen de huidige voorlopige kleuren: `Ring3Biomes.PALETTES` verhuist hierheen (gedrag-neutraal: identieke biome-JSON); Nether-3 krijgt een voorlopig blauwwit palet.
   - `Ring3Biomes` roept de haak aan (één regel; met akkoord van W-biomes via main, vóór W-biomes afsplitst).
2. **Registraties** (vanilla-verwijzingen in `sounds.json`, zodat alles meteen werkt):
   - sound events `ambient.ring_3.rumble`, `.embers`, `.lava`, `.titan_hum`, `.caldera`, `ambient.nether_ring_3.wind`, `.ice_creak`, `block.fumarole.puff`, `block.geyser.burst`, `block.boiling_mud.bubble`;
   - particle types `steam_puff`, `ice_glint` met de bestaande sprites van T (57a) en een eenvoudige provider.
3. **Tests:** `runData` geeft voor de biomes **geen diff** (bewijs gedrag-neutraal); B/D groen.

### 50 — Sfeer ring 3 (overworld)
1. **Kleuren per biome** (G2-A2): lucht, fog, (grasbladkleur van `dormant_caldera`) definitief in `BiomeAmbience.ring3`.
   - Het omslaan van de lucht op de grens 2→3 blijft het wow-moment; zone 2→3 mengt via vanilla's biome-blend.
2. **Biome-particles** (`ambient_particle` in de biome-data; dichtheid G2-A3):
   - `ashen_wastes` `ash_flake`;
   - `ember_woods` `ember` (opstijgende vonken);
   - `sulfur_fields` en `steam_fens` `steam_puff`;
   - `volcanic_peaks` as + vonken;
   - `titan_fields` vanilla `soul` (zielvonkjes);
   - `basalt_flows` sporadisch vonken;
   - `dormant_caldera` niets of zachte pollen;
   - `dried_seabed` stof.
3. **Ambient-geluid** (biome-data: `ambient_sound` loops, `mood_sound`, `additions_sound`):
   - `ambient.ring_3.embers` (knisperen, smeulwoud);
   - `.lava` (borrelen, als additions bij pieken/stromen);
   - `.titan_hum` (laag koor/zoem, titanenveld; G2-A2-detail);
   - `.caldera` (slapende krater: rust, zachte wind, een enkel insect; geen vogels, want ring 3 heeft geen dieren behalve geiten).
4. **Gerommel en camera-shake buiten het weer** (`ambient.ring_3.rumble`): client-only, per speler willekeurig, bewust niet gesynct (design). Frequentie en sterkte: G2-A4.
   - Hergebruik `Rumble` (45); "Screen Effects" schaalt de schok.
   - Alleen in ring 3, sterker in `volcanic_peaks` (de client kent de biome, niet de vulkaanposities).
5. **Rookpluim van de vulkanen** (design "kegels met gloed en een rookpluim"): G2-A1. Wordt het A, dan een eigen sub-pakket `50b` met een payload (INT verhoogt `ModNetwork.PROTOCOL_VERSION`).
6. **`tools/sounds/Ring3Sounds.java`** (single-file, alleen JDK + ffmpeg, vaste seed; patroon `Ring2Sounds.java`):
   - eigen audio voor de ambient-events, de drie weertypes van 45 (backlog r. 202) en de drie blokgeluiden;
   - `.ogg` onder `sounds/ambient/ring_3/`, `sounds/weather/ring_3/`, `sounds/block/ring_3/`;
   - de verwijzingen in `ModSoundDefinitionsProvider` wisselen van vanilla naar eigen;
   - valkuil 36: alleen de nieuwe `.ogg`'s committen (het script herschrijft ook oude bytes).
7. **Ondertitels** (`Ring3WeatherText`, en + nl) voor alle nieuwe events.
8. **`steam_puff`-tint:** afspraak met T (backlog r. 182). Voorstel: lichte grijswaarden getint in de particle-klasse, zoals `ash_flake`/`ember`; zwavelvelden iets geelgroen.

### 56 — Sfeer Nether-3
1. **Kleuren** per Nether-3-biome (`BiomeAmbience.netherRing3`): blauwwitte fog, kouder in `glacier_rifts`, iets violet in `rime_forest`.
   - Fog-afstand zit niet in biome-data (valkuil 36): dichter alleen via een client-hook (G2-A5).
2. **Particles:** `ice_glint` (zwevende ijskristallen) per biome; dichtheid G2-A3.
3. **Geluid:** `ambient.nether_ring_3.wind` (loop, `stream`) en `.ice_creak` (additions); `mood_sound` passend.
4. **Extra kou-sfeer** (G2-A5): adem-wolkjes en/of vriesmist.

## 2. Bestanden
- **Eigen:**
  - `ModSoundEvents`, `ModSoundDefinitionsProvider`, `ModParticleTypes` (+ providers), `ModParticleDescriptionProvider`;
  - `client/particle/*` (`SteamPuffParticle`, `IceGlintParticle`), `client/weather/*` (ring 3) en eventueel `client/ambient/*` (gerommel buiten het weer, pluim, adem);
  - `BiomeAmbience` (ring 3- en Nether-3-builders), `tools/sounds/Ring3Sounds.java`, `assets/corruptedadventure/sounds/**` (ring 3), `Ring3WeatherText`;
  - `ServerConfig` `[weather.ring3]` (als er iets bijkomt).
- **Netwerk (alleen bij G2-A1 = A):** een nieuwe payload in `network/` + registratie; INT verhoogt de protocolversie.
- **Raakt niet:** `Ring3Biomes`/`NetherRing3Biomes` behalve de ene haakregel in 50-0 (akkoord via main), `ModBlocks`/blokklassen (B koppelt de blokgeluiden).

## 3. Afhankelijkheden
- **→ B (46-rest):** wacht op de sound events + `steam_puff` (50-0).
- **→ W-biomes/W-nether:** bouwen vanaf 50-0 tegen de haken. De definitieve effecten komen bij de merge van 50/56 vanzelf mee, omdat de haak in `BiomeAmbience` zit.
- **↔ T:** sprites van `steam_puff`/`ice_glint` bestaan (57a); tint afspreken (backlog r. 182).
- **← W-terrein/W-biomes** (alleen voor G2-A1 = A): de pluim-payload leest vulkaanposities uit de pure `VolcanicField`-beslissingen (alleen lezen, server-side). Een publieke accessor voor "vulkanen binnen R" via main aan 39f/W-terrein.

## 3b. Besluit 39f: aswolken
De wolken in ring 3 krijgen een donkere askleur (besluit gebruiker 2026-10-08 bij 39f; de zwevende eilanden zakken onder de wolkenlaag Y 192, alleen de kegel van de zwevende vulkaan steekt erdoor). Hoort bij pakket 50: client-only (wolkenkleur per ring, vloeiend over de grens zoals de lucht), geen payload nodig als de ring al client-side bekend is.

## 4. Open vragen
Aanbevolen optie eerst. **C** = complexiteit, **MP** = multiplayer, **S** = speelgevoel, **P** = prestatie.

**G2-A1 — Rookpluim boven de vulkanen (design: "met gloed en een rookpluim"): hoe?** Eerlijk over 1.21.1: de client kent de seed niet, dus ook geen vulkaanposities. Gewone particles verdwijnen al na ~32 blokken, en `animateTick` draait alleen dichtbij.
- **A. De server stuurt elke speler in ring 3 de vulkanen binnen ~512 blokken (één kleine payload bij binnenkomst en elke ~10 s); de client tekent een pluim als "altijd zichtbare" rook-particles boven de krater tot ~256 blokken ver, dikker bij een uitbarsting (aanbevolen).** C middel-hoog (payload + client-renderer, protocolversie omhoog). MP: per speler, licht verkeer. S: het horizon-wow vanuit `dry_riverbeds` is af. P client: particle-budget per pluim begrenzen.
- **B. Pluim alleen dichtbij (vent-particles op de kraterrand, zichtbaar tot ~32–64 blokken).** C laag. S: dichtbij sfeervol; vanaf de horizon geen pluim.
- **C. Geen pluim; de gloed van lavameren en smeulsteen is genoeg.** C geen. S: het ontwerpbeeld "rookpluim" valt weg.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-A2 — Kleurrichting per biome?**
- **A. Eén asrood ringpalet met een duidelijke eigen tint per biome: as grijsbeige, pieken rood-oranje, smeulwoud warm amber, zwavel geelgroen, stromen donker roodbruin, zeekom bleek zandblauw, titanen koel violet (zielvuur-contrast), slapende krater helderder en groener (aanbevolen).** C laag. S: elk biome herkenbaar, de ring blijft één geheel.
- **B. Eén ringpalet met alleen kleine nuances (zoals nu in 39b).** C laag. S: rustig, maar biomes lopen in elkaar over.
- **C. Sterke contrasten (elk biome een eigen lucht).** C laag. S: maximaal herkenbaar; harde, soms storende grenzen.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-A3 — Hoeveel biome-particles (as, vonken, stoom, zielvonkjes, ijsglinstering)?**
- **A. Subtiel tot middel (zoals basalt deltas/soul sand valley-licht), dichter in smeulwoud (vonken) en `steam_fens` (stoom) (aanbevolen).** C laag (biome-data). P: client, valt weg bij Particles: Minimal. S: levendig zonder zicht te ontnemen; het weer blijft het piekmoment.
- **B. Dicht overal (zoals crimson forest).** C laag. S: sterk, maar de asregen valt minder op; meer belasting op zwakke clients.
- **C. Alleen in 3–4 biomes (smeulwoud, zwavel, titanen, `steam_fens`).** C laag. S: rust in de andere biomes.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-A4 — Gerommel en camera-shake buiten het weer: hoe vaak en hoe sterk?** (client-only, per speler willekeurig)
- **A. Elke 2–5 min een diep gerommel, 1 op 3 met een lichte schok; vaker en sterker in `volcanic_peaks`; schok volgt "Screen Effects" (aanbevolen).** C laag (`Rumble` hergebruiken). S: onrustig, nooit vervelend.
- **B. Vaker (30–90 s).** C laag. S: constante dreiging; kan gaan irriteren.
- **C. Alleen gerommel, geen schok buiten een uitbarsting.** C laag. S: rustiger, minder voelbaar.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-A5 — Extra kou-sfeer in Nether-3?**
- **A. Adem-wolkjes bij spelers en mobs (client-side, kleine witte puf elke ~2 s), dwarrelende ijsglinstering en wind + krakend ijs (aanbevolen).** C laag/middel (client-tick in `client/`). MP: elke client tekent zelf, voor alle spelers in beeld. S: je voelt de kou zonder HUD.
- **B. Alleen glinstering + wind/kraak (biome-data).** C laag. S: sfeervol, minder lijfelijk.
- **C. A + vriesmist (dichtere fog via een client-hook, zoals bij het ringweer).** C middel. S: beklemmend; de grote hallen worden minder zichtbaar.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
- **Client-only** in `client/`: particles, gerommel/shake, adem, pluim-renderer, eventuele fog-hook. `ClientIsolationTest` bewaakt dat.
- **Data:** biome-effecten en `sounds.json`; de client rendert.
- **Server:** alleen bij G2-A1 = A (vulkanen binnen R per speler, payload); verder geen.
- Twee clients zien hetzelfde weer; gerommel en shake mogen per client verschillen (design).

## 6. Tests
- **B:** `./gradlew build` groen (unit tests, `ClientIsolationTest`).
  - Tests: `BiomeAmbienceTest` (+ ring 3/Nether-3-paletten, ieder biome een eigen palet, 50-0 gedrag-neutraal), payload-codec (G2-A1). `Ring3Sounds.java`: 2× draaien geeft dezelfde PCM (de ffmpeg-bytes mogen verschillen, valkuil 36).
- **D:** `runData` groen (sounds, particles, lang). Voor 50-0: geen biome-diff.
- **S:** server (25575) + 2 clients (quick-play; `options.txt` met `onboardAccessibility:false`, `skipMultiplayerWarning:true`).
  - Server start zonder client-class-fouten.
  - Per biome kleuren, particles en geluid.
  - Gerommel en shake per client verschillend.
  - Particles: Minimal → weinig particles, kleuren en geluid blijven.
  - FPS in smeulwoud 's nachts en in `steam_fens`.
  - Pluim (G2-A1) vanuit `dry_riverbeds`.
- **Review:** "klaar voor review-doorsturen: <pakket>" aan main; main draait `/code-review medium`; daarna committen.

## 7. W-checklist (voor 4b; dedicated server + 2 clients, seed `20260930`)
- [ ] `/ca tpboundary 2`, oostwaarts: de lucht slaat om; vanaf de grens rookpluimen boven de horizonreuzen (als G2-A1 = A).
- [ ] Per biome 1 min staan (coördinaten in `golf-2-biomes.md` §7): eigen lucht- en fogtint, eigen particles en geluid. Smeulwoud (4.750, 0) 's nachts: vonken en knisperen. Titanenveld (4.942, 160): zielvonkjes + titanen-zoem. Slapende krater: rust.
- [ ] Gerommel met lichte schok, vaker op de pieken (4.110, 672); "Screen Effects" 0 = geen schok.
- [ ] Eigen geluiden voor asregen, uitbarsting en aswolk (`/ca weather 3 …`), fumarole, geiser en modder.
- [ ] Nether-3 (`/ca tpring 3 nether`): blauwwitte fog, ijsglinstering, wind en krakend ijs; adem-wolkjes (als G2-A5 = A).
- [ ] Particles: Minimal en FPS acceptabel.

## Voortgang

## CODEBASE-delta

## Beslissingen

## Backlog
