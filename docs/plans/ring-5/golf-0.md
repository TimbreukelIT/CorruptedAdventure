# Ring 5 — golf 0: fundament (81, 82, 81b) en open punten

Scope per pakket: één categorie (CLAUDE.md §2). Werkwijze: `waves.md` §2 (golf 0) en §4 (Linux-container); namen en
sleutels: `contract.md`. Start: na teststop 5c en de antwoorden op §6. Worktree `/home/user/wt/ring5-g0`, branch
`ring5/golf0` (lokaal; pushen alleen door main op verzoek van de gebruiker). Volgorde: **81 → 82 → 81b**, elk een eigen
commit met `/code-review` vóór de commit.

**Status:** concept (2026-10-21). Niets hiervan is gebouwd; §6 moet eerst beantwoord zijn. Getallen tussen ⟨⟩ hangen
af van een antwoord (Vn) en zijn hier ingevuld met de aanbevolen optie.

---

## 1. Pakket 81 — `infra`: contract-raamwerk ring 5

**Wat**
1. `ModBiomes`: keys `CORRUPTED_WASTES`, `GLITCH_FIELDS`, `FALSE_MEADOW`, `VOID_SCARS`, `INVERTED_HILLS`,
   `SHATTERED_EXPANSE` (naam volgens V12), `WHITE_HALLS`, `HOLLOW_CATHEDRAL`. De placeholders `placeholder_ring_5` en
   `placeholder_nether_ring_5` blijven tot 86/87 (weg in golf 3).
2. `ModTags`: `Blocks.CORRUPTED`, `RING_5_STONES`, `VOID_SOURCES`, `NETHER_RING_5_STONES`; `Biomes.IS_RING_5`,
   `IS_NETHER_RING_5`, `RING_5_INNER`, `RING_5_OUTER`, `SILENT`.
3. `registry/ModDataMaps` (nieuw): data map-type `corruptedadventure:healthy_block` (`DataMapType<Block, Block>`,
   registry `minecraft:block`, waarde-codec `BuiltInRegistries.BLOCK.byNameCodec()`), geregistreerd via
   `RegisterDataMapTypesEvent` (common; ook de client kent het type). Geen inhoud (B, 88).
4. Lang-helpers (leeg): `Ring5BlockNames`, `Ring5BiomeNames`, `NetherRing5BiomeNames`, `Ring5ConditionText`,
   `Ring5WeatherText`, `Ring5SignText`; aanroepen in de Engelse en Nederlandse provider.
5. Lege bootstraps, aangeroepen zoals ring 4: `Ring5Biomes`, `Ring5BiomeModifiers`, `Ring5Features`, `Ring5Carvers`,
   `Ring5BiomeTags`, de Nether-5-tegenhangers, `Ring5DamageTypes`.
6. **Config** alleen als V2 of V7 een zonebreedte wijzigt: nieuwe standaard voor `transitionWidthOverrides` en migratie
   van alleen de exacte oude lijst [0, 0, 0, 256, 0] (patroon 58, INFO-log; andere waarden WARN). Bij de aanbevolen
   antwoorden (V2-A, V7-A) **geen** configwijziging.

**Client/server-impact:** keys, tags en een data map-type (common, gesynct door NeoForge); geen payload, geen
`PROTOCOL_VERSION`, geen client-class.

**Tests:** B; D (lege tags en helpers schrijven niets; `src/generated` ongewijzigd); unit: `ModDataMapsTest` (type-id,
codec rondreis), bij een configwijziging `CommonConfigTest` + `RingSettingsTest` (migratie, oude snapshot zonder sleutel).

## 2. Pakket 82 — `worldgen`: ring 5-skelet

**Wat**
1. **Router** (`ring_blend/*`, contract §4): ring_5 → `region_rot` (temperature), `region_glitch` (vegetation), `band`
   (weirdness), `breakup` (erosion, V12-A), floodedness 0 (V9-A), initial `0,390625 + ring_5/overworld/initial_terrain`;
   Nether `nether_temperature` ring_5 → `ring_5/nether/region`, `ring_blend/nether` ring_5 → `ring_5/nether/terrain`
   (verwijzing naar de placeholder; Nether-5 bitgelijk). `switch_at` ongewijzigd.
2. **Klimaatassen:** `noise/ring_5_rot`, `ring_5_glitch` (−10, [1, 0,5, 0,25]); `noise/nether_ring_5_region` (−9,
   [1, 1, 0,5]); DF's als flat_cache.
3. **Stubs** (0): `ring_5/overworld/height`, `scars`, `glitch`, `inverted`, `caves`; `breakup` en `castles` als uitgangen
   van `ring_5_output` (zie 5), in golf 0 met alleen de kasteelgordel.
4. **Band-as** `ring/Ring5Band` + DF-type `ring_5_band` (`Ring5BandFunction`) + JSON `ring_5/overworld/band`. Getekende
   afstand `effectiveDistance − (r4 + ⟨0,5⟩ · breedte) + ⟨0,15⟩ · breedte · ruis`, ruis 2 simplex-octaven (⟨~600 / ~150⟩,
   0,7/0,3) op `noiseSeed` met hash `corruptedadventure:ring_5_band`; waarde = afstand / (`SLOPE` · ⟨200⟩ / 2), `SLOPE`
   gemeten zoals bij `Ring4Band` zodat de overgang ~⟨200⟩ blokken dwars op de lijn is. `RingLayout.ring5Band()` lazy.
5. **`Ring5Field`** (`worldgen/densityfunction`, type `ring_5_field`, JSON `ring_5/overworld/field.json` met hash-noise
   `ring_5_cells`; uitgangen via type `ring_5_output`: `breakup`, `castles`, `initial_height`), eigen `Column`, `ColumnTable`
   en `Decisions` (eenmaal per wereld), gevonden via `RingChunkGenerator.ring5Field(random)` en `worldgen/chunk/Ring5Fields`
   (register per dimensie bij `LevelEvent.Load`, geleegd bij `ServerStopped`). **Kasteelplekken echt:** θ0 uniform in
   [0°, 90°) uit de salt, plek k op θ0 + k · 90° ± ⟨8°⟩, radiaal op ⟨75 %⟩ van ring 5 gemeten in `effectiveDistance`
   (de plek zoekt langs zijn straal het punt met die effectieve afstand), straal ⟨40–56⟩, top Y ⟨120–140⟩, gordel ⟨40–80⟩.
   Accessors `castleSites()`, `castleAt(x, z)`; de overige (`bridgesWithin`, `fragmentsWithin`, `breakupAt`, `solidGround`)
   als lege skeletten (84).
6. **Skeletterrein `ring_5/overworld`** (tijdelijk, tot 83/84; alleen om 6-pre te laten zien):
   - `initial_height` = 85 + 8 · `placeholder_terrain` − ⟨6⟩ · max(0, band) (een **band-trede** van 6 blokken, zodat de
     bandlijn op de hoogtekaart zichtbaar is);
   - per kasteel een grove pinakel: schijf met de straal en top van 5, de wanden recht naar beneden tot ⟨V3: Y −40,
     daaronder void⟩; eromheen de gordel als void tot onder `min_y` (geen bedrock, de bodemregel van §4 Overig);
   - dichtheid zoals ring 3/4: `0,125 · (initial_height − y)` met de bekende slides; geen grotten.
7. **`initial_terrain`** zoals ring 3/4 (`0,125 · cache_2d(initial_height)` + gradiënt −64 → 8, 320 → −40).
8. **Vloeistofregel ring 5** (V9-A): `RingFluidPicker.ring5Dry` (ring 5 op `ring()`, ook de ring 5-helft van zone
   4→5): geen zee, geen lokale vloeistof, geen lavalaag; `RingAquifer` zet open ruimte op lucht;
   `RingAquifer.decidesCarverLava` ook in ring 5 (lucht). Zone 4→5 blijft in 82 zoals hij is (water/afgrond van 63) tot
   85; de naad 4\|5 ligt dan op de buitenrand van de zone tegen droge ring 5-grond (geen lava meer: backlog r. 259 weg).
9. **Breukzone-skelet:** `ZoneFracture` (puur; `strength(layout, effective)` = 0 overal in golf 0) + DF-type
   `zone_fracture` (record `argument`) om `noise_router.final_density` van `noise_settings/overworld.json`. Identiteit:
   `compute` en `fillArray` geven exact `argument`; `mapAll` geeft de visitor door. Accessors `crackAt`, `floeAt` leeg.
10. **Biomehaak** (V12-A): geen Java; de preset houdt `placeholder_ring_5` tot 86 (de `multi_noise` met de vier assen
    is van W-biomes). Bij V12-C: optioneel codec-veld `ring_5_biomes` in `RingBiomeSource` (zoals `island_biomes`).

**Client/server-impact:** puur worldgen, server-side. `Ring5Band` is common code op de gesyncte layout-seed (de client
kan hem rekenen, zonder payload); `Ring5Field`/`Ring5Fields` en de kasteelplekken bestaan alleen op de server (salt uit
de wereldseed). Geen client-class, geen `PROTOCOL_VERSION`.

**Determinisme en prestatie:** band op `noiseSeed`; `Ring5Field`-beslissingen puur per cel, nooit via een flat_cache;
`RingBlend` zet `RingGate`/`RingFillGate` vanzelf in de nieuwe ring 5-invoer (buiten ring 5 en zijn zones kost hij
niets). `zone_fracture` controleert per kolom eerst de zone (± 16) via `RingColumnCache` en is daarbuiten één
vergelijking.

**Bitgelijk na 82:** ring 1–4 en Nether 1–4 buiten zone 4→5; in zone 4→5 alleen de ring 5-helft van de menging
(`initial_density`, de dichtheid van het skeletterrein, de router-assen) en de vloeistof in die helft. Test vergelijkt
`final_density` op een raster in ring 1–4 vóór/na (zoals 59).

**Tests (unit, in `build`)**
- `Ring5BandTest`: randen (binnenrand altijd binnen, rand 9.700 altijd buiten), verdeling ~⟨50/50⟩ met tongen,
  overgangsbreedte ~⟨200⟩ (mediaan), seed-afhankelijk, server- en clientlayout geven dezelfde waarde.
- `Ring5FieldTest` / `CastleSitesTest`: precies 4 plekken; onderlinge hoeken 90° ± 2 · jitter; θ0 verschilt per seed en
  is gelijk voor dezelfde seed; elke plek ligt in de buitenband (`Ring5Band.outer` op het midden) en met pinakel + gordel
  binnen ring 5 (niet in zone 4→5 of 5→void); `castleAt` binnen de straal; kopie gelijk (`find`).
- `Ring5JsonTest`: router-sleutels, stubs, band-DF = Java op een raster, `initial_terrain` = skeletoppervlak, de trede,
  Nether-5 bitgelijk, preset ongewijzigd (placeholder).
- `ZoneFractureTest`: identiteit (DF met en zonder wrapper gelijk op een raster rond zone 4→5), `mapAll` laat de gates staan.
- `RingFluidPickerTest`/`RingAquiferTest`: ring 5 droog (geen water, geen lava, ook niet onder −54), carver-lava ring 5
  lucht, zone 4→5 en ring 1–4 ongewijzigd, zone 5→void ongewijzigd (`noFluid`).
- Aangepast: `Ring4JsonTest`/`Ring3JsonTest` waar ze "ring 5 = 0 in de router" aannemen.

## 3. Pakket 81b — `infra`: `/ca castles`, testgebieden, scans

1. `/ca castles` (OP): de vier plekken (index, hoek, x, z, straal, top, afstand tot de speler/bron);
   `/ca castles tp <1–4>` naar de top van een pinakel (landingsregel van `/ca tpring`). Leest `Ring5Fields.current`.
   Alleen in ringwerelden; anders een melding.
2. `tools/test/areas/ring5.txt` (vaste tegels op de testseed: zone 4→5 oost/noord, binnenband, bandlijn, buitenband,
   zone 5→void) en `castles.txt` (de vier pinakels; coördinaten na 82 uit `/ca castles` op seed `20260930`).
3. Scans: `lavascan.py`/`waterscan.py` leren de regel "ring 5 droog" (elke vloeistof in ring 5 telt); nieuw
   `py/voidscan.py` (bodemscan): bedrock alleen waar V3 dat toelaat, geen losse blokjes ≤ 2 boven de void, geen vloeistof
   tegen de void. `tools/test/README.md` bijwerken (INT mag).

**Tests:** B; unit `CastlesCommandTest` (formattering, tp-doel); `voidscan.py` op een `world.sh`-wereld van
`ring5.txt` geeft 0 (golf 0: alleen de kasteelgordels hebben void).

## 4. Integratie van golf 0

Geen stromen: één worktree, sequentieel. Na 81b: `build`, `runData` + `git diff --exit-code src/generated`, headless
S-smoke (`server.sh start r5-g0 --fresh`, log zonder ERROR/client-class, RCON `/ca castles`, `/ca ring`), determinisme
op `ring5.txt` (`det.sh`), `/code-review` medium op de golf-diff, dan de kaarten van §5. Docs (CODEBASE-delta,
beslissingen, backlog) in deze map tot INT ze samenvoegt.

## 5. Teststop 6-pre — wat de terreinpreview moet laten zien

**Direct na golf 0** (container, kaarten via RCON op een verse wereld seed `20260930`; PNG's naar
`build/test/maps/6-pre-0/`):
1. **Overzicht oost:** `/ca terrainmap 8900 0 1400 4` (zone 4→5 tot de border): de band-trede (bandlijn met tongen en de
   overgangsband van ~⟨200⟩), de pinakel in deze kwart (als θ0 dat geeft) met zijn gordel, en de bestaande zone 4→5
   (nog met landbruggen) als referentie.
2. **Vier kasteelplekken:** per plek `/ca terrainmap <x> <z> 300 1` (coördinaten uit `/ca castles`): ligging in de
   buitenband, grootte van de pinakel en de gordel, afstand tot de bandlijn en tot zone 5→void.
3. **Doorsneden:** `/ca terrainmap section 7700 0 10300 0` (radiaal oost) en een radiale doorsnede door één pinakel:
   hoogte binnen/buiten, pinakelwand, gordel tot onder `min_y`, de border.
4. **Nether:** `/execute in minecraft:the_nether run ca terrainmap 2200 0 300 1`: Nether-5 nog placeholder (bitgelijk).
5. Een tekstuele samenvatting door main: gemeten aandeel binnen/buiten per vijfde van ring 5, de vier hoeken en stralen.

**Vroeg in golf 1** (na de eerste versie van 84 en 85; `build/test/maps/6-pre-1/`): dezelfde kaarten plus een doorsnede
dwars door zone 4→5 (`section` langs de zone, bv. 7800 −600 → 8200 600) en een kaart van de buitenband met brokken en
bruggen; daarna een korte vliegtocht van de gebruiker lokaal (branch van main) langs de breukzone, de bandlijn en één
kasteel. **Beoordelen:** band 50/50 en de grilligheid, de gradiënt naar de void, de grootte en ligging van de pinakels,
de bruggen, de breukzone (dicht genoeg, overal over te steken, spleten diep genoeg). Niet blokkerend voor B, C, A, T.

---

## 6. Open punten voor de gebruiker

Alleen wat de sparring (rondes 1–5) niet beslist en wat golf 0 of het terrein van golf 1 blokkeert. Vragen over
blokken, meters en weer (namen, rates, drempels, per-band-weer) komen in de korte vragenronde aan het begin van golf 1.
Per vraag staat de aanbevolen optie bovenaan.

### V1 — Bandlijn binnen/buiten: hoe grillig?
Lijn op 50 % van ring 5 (~8.850), overgangsband ~200 (sparring). Open: hoe ver de ruis de lijn verschuift.
- **A *(aanbevolen)*: ±15 % (~250 blokken), twee octaven (~600 / ~150).** Puur dood tot ~35 % van de ring, puur "weg"
  vanaf ~65 %. *Complexiteit:* laag (kopie van `Ring4Band`). *Multiplayer:* geen (server rekent, client kan meerekenen).
  *Speelgevoel:* duidelijk twee helften met tongen van een paar honderd blokken; de kastelen (≥ 70 %) liggen altijd in
  de buitenband.
- **B: ±22,5 % (~380), zoals ring 4.** Diepere tongen en eilanden van de andere band; puur dood tot ~27 %, puur weg
  vanaf ~73 %. *Complexiteit:* laag. *Speelgevoel:* grilliger, minder voorspelbaar; kasteelplekken moeten verder naar
  buiten om zeker in de buitenband te liggen.
- **C: ±5 % (~85), bijna een cirkel.** *Complexiteit:* laag. *Speelgevoel:* strak en voorspelbaar, voelt als een
  getekende lijn.

### V2 — Buitenband en buitenrand (9.700 – 10.200): één verloop of twee delen?
- **A *(aanbevolen)*: één doorlopend verloop.** De grond valt van de bandlijn tot de ringrand weg van ~0 naar ~85 %
  void; daarna loopt hetzelfde veld door in de buitenrand (steeds kleinere brokken, leeg vanaf ~10.050). De oude
  void-invoer (eilanden Y 50–110, change 6) vervalt; pakket 93 gaat op in 84. Zonebreedte 5→void blijft 128.
  *Complexiteit:* middel (de void-invoer leest `Ring5Field`). *Multiplayer:* geen. *Speelgevoel:* naadloos "steeds meer
  weg" tot het niets.
- **B: buitenband tot ~60 % void, dan de bestaande zwevende eilanden.** Alleen de menging bijstellen, zone 5→void naar
  256 voor een zachte overgang. *Complexiteit:* laag. *Speelgevoel:* de rand voelt als een ander soort terrein (ronde
  eilandjes) na de brokken.
- **C: de buitenband is al vrijwel helemaal void,** alleen kastelen, bruggen en brokken; de buitenrand is leeg.
  *Complexiteit:* laag. *Speelgevoel:* dramatisch, maar weinig te verkennen in de buitenste ~40 % van de ring.

### V3 — Wat zit er onder de buitenband en onder de scheuren?
- **A *(aanbevolen)*: binnen volle grond, buiten zwevende schotsen; daaronder echte void.** Binnenband tot bedrock;
  scheuren (`void_scars`, losse scheuren) gaan tot onder `min_y` zonder bedrock. In de buitenband wordt de grond naar
  buiten dunner: schotsen met een onderkant van ~Y −40 (bij de bandlijn) tot ~Y 20–40 (bij de rand), daaronder lucht tot
  onder −64. Vallen = vanilla void-dood, items weg. *Complexiteit:* middel (onderkant per kolom in `Ring5Field`).
  *Multiplayer:* geen bijzonderheden (vanilla void-gedrag). *Speelgevoel:* je ziet de onderkant van de wereld; ernaast
  bouwen is eng.
- **B: overal volle kolommen tot bedrock, behalve waar void is.** De grond eindigt met een verticale wand tot −64.
  *Complexiteit:* laag. *Speelgevoel:* minder "zwevend", wel diepe kloven; nulliet in brokonderkanten wordt
  wandnulliet.
- **C: als A, en ook de binnenband zonder bedrockvloer.** Wie in ring 5 tot −64 graaft, breekt door naar de void.
  *Complexiteit:* laag (bedrockregel per ring). *Speelgevoel:* extra horror; risico op onverwachte dood bij diep mijnen.

### V4 — Hoogtes van ring 5
- **A *(aanbevolen)*: binnenband Y ~70–95** (dode vlakte rond Y 78, lage heuvels, "net fout"-trappen), aansluitend op
  de rand van zone 4→5 (Y ≥ 64); **buitenband**: schotsen met toppen Y ~70–110, brokken Y 40–160; **pinakeltop
  Y ~120–140**, boven alles uit en van ver zichtbaar. *Complexiteit:* laag. *Speelgevoel:* vlak en kaal binnen, de
  kastelen als bakens.
- **B: laag en vlak, Y 64–80 overal; pinakels Y 100–115.** *Complexiteit:* laag. *Speelgevoel:* eentonig dood, de
  pinakels vallen minder op.
- **C: hoogvlakte, ring 5 stijgt naar Y ~110–140; pinakels tot ~Y 180.** De breukzone wordt een opgang.
  *Complexiteit:* middel (de breukzone moet ~50 blokken overbruggen). *Speelgevoel:* imposant, maar botst met
  "dood en plat" en maakt de zone 4→5 steiler.

### V5 — Kasteelplekken en pinakel
Altijd: 4 plekken, één per windrichting, gedraaid met de wereldseed (alleen de server kent ze; ook F3 of de client kan
ze niet afleiden).
- **A *(aanbevolen)*: op 75 % van ring 5 (~9.275), ±8° speling per plek; plateau Ø ~80–110 (straal 40–56); rondom
  een void-gordel van 40–80 breed.** *Complexiteit:* middel. *Multiplayer:* geen (vaste plekken, server-side; fase 3/5
  lezen dezelfde functie). *Speelgevoel:* ruim binnen de buitenband, niet tegen de rand; flinke oversteek.
- **B: op 65 % (~9.100), plateau Ø 64–80, gordel 24–40.** *Complexiteit:* middel. *Speelgevoel:* makkelijker
  bereikbaar, minder spectaculair; met V1-B kan een plek in de overgangsband vallen.
- **C: op 85 % (~9.450), plateau Ø 110–145, gordel 80–120.** *Complexiteit:* middel. *Speelgevoel:* epische aanloop,
  maar krap tegen zone 5→void (~9.636); weinig ruimte voor kasteelomgeving aan de buitenkant.

### V6 — Bruggen
- **A *(aanbevolen)*: per kasteel 2 natuurlijke bruggen** (5–9 breed, licht doorhangend, brokkelige randen; één naar
  binnen naar vaste grond, één schuin naar een grote schots), **plus in de buitenband smalle ruggen** (3–5 breed) om de
  ~400–600 blokken langs de ring en stroken springbare brokken (gaten ≤ 3); geen garantie dat elk stuk bereikbaar is.
  *Complexiteit:* middel. *Multiplayer:* geen. *Speelgevoel:* verkennen met risico; soms zelf overbruggen.
- **B: per kasteel 1 brede brug (9–13), verder geen bruggen.** *Complexiteit:* laag. *Speelgevoel:* duidelijk één
  toegang (verdedigbaar, fase 3/5); de rest van de buitenband alleen met springen of bouwen.
- **C: per kasteel 2 bruggen en gegarandeerde bereikbaarheid:** elke schots ≥ 16 breed is via bruggen of springbare
  brokken verbonden met de binnenband. *Complexiteit:* hoog (graafregel per cel, tests). *Speelgevoel:* veilig
  verkennen, minder eng.

### V7 — Vorm van de breukzone 4→5
Altijd: grotendeels dichte grond, spleten, opgeheven/gekantelde schotsen, losse brokken, overal over te steken, geen
landbruggen (sparring ronde 5).
- **A *(aanbevolen)*: breedte 256 blijft; ~75 % dichte grond (Y ≥ 64); spleten 2–8 breed en 30–60 diep met een droge
  puinbodem; schotsen 8–30 omhoog (kanteling ≤ 30°); losse brokken vooral in de buitenste helft; om de ~30 blokken langs
  een spleet een smalle plek (≤ 3) of ingestorte schots als oversteek.** De afgrondzee op Y 0 vervalt.
  *Complexiteit:* middel. *Multiplayer:* geen. *Speelgevoel:* gebroken maar begaanbaar; vallen doet pijn, is niet dodelijk
  in de zin van de void. Geen water: geen waterwanden (les 5a).
- **B: als A, maar de spleten tot de bestaande afgrondzee op Y 0** (water onderin, overleefbaar vallen).
  *Complexiteit:* middel-hoog (de waterregel en de waterscan van zone 4→5 blijven nodig). *Speelgevoel:* diepere,
  donkere kloven met water, dichter bij het oude ring 4-gevoel.
- **C: smaller (192) en ondieper (spleten 15–30, alles springbaar).** *Complexiteit:* middel (config-standaard +
  migratie, ring 4-rand verschuift). *Speelgevoel:* rustiger overgang, minder dramatisch.

### V8 — Hoeveel van de oude zone 4→5 verdwijnt (en hoe gebouwd)?
- **A *(aanbevolen)*: nieuw gedeeld veld ná de blend over de hele zone** (`ZoneFracture`, één eigenaar). Uit ring 4
  verdwijnen landbruggen, afgrond/zee Y 0, de losse-brokken-logica, `IslandField.sunder`/`landBridgesNear` en hun tests
  (vervangen door nieuwe breukzone-tests). **De gespleten rots blijft als blikvanger**, verhuisd naar een opgeheven
  schots (~elke 650 langs de zone). `sundered_reach` (biome, kleur, sfeer, grijsfade) blijft. *Complexiteit:* middel
  (nieuw veld, ring 4-code eruit). *Multiplayer:* geen. *Speelgevoel:* de zone is één geheel, ring 4 en ring 5 zien
  dezelfde breuk.
- **B: als A, maar de gespleten rots vervalt** (met de bruggen). *Complexiteit:* lager. *Speelgevoel:* een blikvanger
  minder (draait het decor D-besluit terug).
- **C: ombouw binnen `IslandField` (ring 4 blijft eigenaar);** bruggen weg, afgrond wordt spleten; de ring 5-invoer
  volgt in de zone de ring 4-grondhoogte. *Complexiteit:* lager op korte termijn. *Speelgevoel:* in de buitenste helft
  maakt de menging de spleten vaag (bekend probleem, backlog r. 262/396).

### V9 — Vloeistof in ring 5
- **A *(aanbevolen)*: ring 5 is droog** — geen zee, geen grondwater, geen lavalaag; open ruimte is lucht. *Complexiteit:*
  laag, en lost de naad water/lava bij 4\|5 op. *Multiplayer:* geen. *Speelgevoel:* "dood": nergens water, wat de
  overleving zwaarder maakt (water meenemen); niets stroomt de void in.
- **B: droog, behalve een diepe lavalaag in de binnenband** (vanilla onder −54). *Complexiteit:* middel (dam tussen
  binnen en buiten, lavascan). *Speelgevoel:* gevaar en licht diep onder de dode grond.
- **C: vanilla (zee op 63, grondwater, lava).** *Complexiteit:* hoog (water dat in scheuren en de void stroomt, veel
  scans en fixes). *Speelgevoel:* vertrouwd, minder "dood".

### V10 — Indeling Nether-5
Altijd: geen lava in Nether-5; spiegelsteen-vloer waar de lavazee lag (Y ~31); zuilenrasters in `white_halls`.
- **A *(aanbevolen)*: `white_halls` als basis, `hollow_cathedral` als zeldzame grote holtes** (~1 per 400 × 400
  Nether-blokken, ~30 rondom; Ø 120–180, dak opgetild tot ~Y 120, één zuil Ø 20–30 van vloer tot dak met witte kern).
  *Complexiteit:* middel. *Multiplayer:* geen. *Speelgevoel:* je stuit onverwacht op een enorme lege zaal.
- **B: twee banden: binnen `white_halls`, buiten één doorlopende `hollow_cathedral`-band.** *Complexiteit:* laag-middel.
  *Speelgevoel:* de stilte wordt naar buiten groter en leger; minder verrassing.
- **C: vier kathedralen op vaste plekken onder de vier kastelen** (overworld-plek ÷ 4), de rest `white_halls`.
  *Complexiteit:* middel (koppeling aan `Ring5Field`, alleen server). *Speelgevoel:* verhaalkoppeling met de kastelen
  (fase 3/5); weinig `hollow_cathedral` om te vinden.

### V11 — Nummering van blok G
- **A *(aanbevolen)*: 81–99 blijven, met nieuwe inhoud volgens `waves.md` §8; extra's met lettercodes (81b, 86b, 99a/b);
  H blijft 100.** *Complexiteit:* geen. *Speelgevoel:* n.v.t.; verwijzingen in backlog en docs ("81–99") blijven kloppen.
- **B: nieuwe codes `R5-01` … `R5-nn`;** 81–99 vervallen. Duidelijk los van het oude plan, maar alle verwijzingen moeten om.
- **C: doornummeren na 100 (101–120), H wordt 121;** de oude rijen blijven als geschiedenis. Veel verwijzingen naar
  "100" (eindtest) moeten om.

### V12 — Biome-indeling van de buitenband (en de pinakel)
- **A *(aanbevolen)*: `shattered_expanse` = elke buitenbandkolom met veel void** (breukas ≥ ~0,6: void met brokken en
  bruggen); `void_scars` en `inverted_hills` verdelen de resterende grond via een klimaatas; binnen de drie biomes via
  twee klimaatassen. Alles via router-assen (de breukas op de erosion-plek), geen Java-haak. De pinakel krijgt
  `void_scars` (verwoeste grond, voidsteen-randen). De naam `shattered_expanse` blijft, tenzij je een andere geeft.
  *Complexiteit:* laag-middel. *Multiplayer:* geen. *Speelgevoel:* de biome zegt eerlijk "hier is niets onder je"; F3
  en sfeer volgen de void.
- **B: drie gewone regio's in de buitenband;** `shattered_expanse` is een regio waar het meeste void is, zonder masker.
  *Complexiteit:* laag. *Speelgevoel:* biomes en terrein lopen minder gelijk (void-scheuren in `inverted_hills`, grond in
  `shattered_expanse`).
- **C: als A, maar met een Java-haak in `RingBiomeSource`** (per brok exact, zoals de hoogte-biomes van ring 4) en de
  pinakel in `corrupted_wastes` ("laatste dode grond"). *Complexiteit:* middel. *Speelgevoel:* preciezer op brokken boven
  de void; de pinakel oogt als de binnenband.

---

## CODEBASE-delta
(Na de bouw invullen. Verwacht: `ModDataMaps`, ring 5-keys en -tags, lang-/bootstrap-haken, `Ring5Band`,
`Ring5BandFunction`, `Ring5Field`, `Ring5Fields`, `ZoneFracture`, DF-types `ring_5_band`, `ring_5_field`, `ring_5_output`,
`zone_fracture`, `RingFluidPicker.ring5Dry`, `RingChunkGenerator.ring5Field`, `/ca castles`, JSON- en noise-bestanden
ring 5, testgebieden en `voidscan.py`.)

## Beslissingen
(Na de antwoorden op §6: één regel per vraag voor `02-beslissingenlog.md`.)

## Backlog
- [infra] `slots.sh`: aantal JVM-slots per machine instelbaar (`CA_MAX_SLOTS`), zodat de container op 2 kan.
- [worldgen] Na 85: `CODEBASE.md`-valkuil "zonevorm ná de blend" en de oude `Ring4Sundered`-regels opruimen.

## 7. Antwoorden gebruiker (2026-10-21)
| Punt | Besluit |
|---|---|
| V1 bandlijn | A: ±15 % (~250), twee octaven (~600/~150) |
| V2 buitenrand | A: één doorlopend verloop tot ~85 % void, buitenrand loopt door tot leeg vanaf ~10.050; oude void-invoer vervalt, 93 gaat op in 84 |
| V3 onderkant | A: binnen volle grond tot bedrock, scheuren tot in de void zonder bedrock; buiten zwevende schotsen (onderkant ~Y −40 → Y 20–40), daaronder void |
| V4 hoogtes | A: binnen Y 70–95 (rond 78), buiten schotsen Y 70–110, brokken Y 40–160, pinakeltop Y 120–140 |
| V5 kastelen | A: 75 % (~9.275), ±8°, plateau Ø 80–110, void-gordel 40–80 |
| V6 bruggen | A: 2 natuurlijke bruggen per kasteel (5–9 breed) + smalle ruggen om de ~400–600 + springbare brokstroken; geen bereikbaarheidsgarantie |
| V7 breukzone | A: 256 breed, ~75 % dicht, droge spleten 2–8 breed en 30–60 diep, schotsen 8–30 omhoog (≤ 30°), oversteek om de ~30; zee op Y 0 vervalt |
| V8 ombouw | A: `ZoneFracture` (één eigenaar W-terrein); landbruggen/afgrond/`sunder` weg; gespleten rots blijft op een opgeheven schots; `sundered_reach` blijft |
| V9 vloeistof | **B: droog, behalve een diepe lavalaag in de binnenband (vanilla, onder −54)**: dam tussen binnen- en buitenband (geen lava in scheuren/void of de breukzone), lavascan in de INT |
| V10 Nether-5 | A: `white_halls` basis, `hollow_cathedral` als zeldzame holtes (~1 per 400×400, Ø 120–180, dak ~Y 120, één reuzenzuil met witte kern) |
| V11 nummering | A: 81–99 + lettercodes, H blijft 100 |
| V12 biomes buitenband | A: `shattered_expanse` = veel void (breukas ≥ ~0,6); `void_scars`/`inverted_hills` op de rest via klimaatas; pinakel `void_scars`; via router-assen |
