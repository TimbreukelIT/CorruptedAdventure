# Ring 5 golf 2 — stroom W-terrein: 93 (grotten + gespiegelde dubbelgangers)

Scope: `worldgen` — pakket 93 in branch `ring5/93`, worktree `P:/Projecten/Java/wt/r5-93`, poort n = 8 (server 25578,
RCON 25588; 0–5 en 9 zijn in gebruik, 6/7 zijn T/I), basis = main `061a20c4` (na INT2 deels: 83b, 96, 97, 98).
Parallel: 94 (W-biomes, `wt/r5-94`) en straks 95/99b; 93 raakt hun bestanden niet (§Bezit).

Bronnen: `fase-1-wereld.md` → "Ring 5" (blok "Sparring ring 5": grotten en horror; "Grotten"; "Herzien bij teststop 6a");
`docs/plans/ring-5-sparring.md` ronde 3 (grotten) en ronde 4 (horror: dubbelgangers); `contract.md` §4 (`ring_5/overworld/
caves`, kopieerveld `Ring5Copies`, features worden niet gekopieerd); `waves.md` §1, §2 golf 2, §5; `golf-1-terrain.md`
(architectuur van `Ring5Field`, 83/83b: scheuren, dam, corrupte plekken); beslissingenlog 2026-09-30 ("Ring 5-grotten:
onmogelijke gangen (te perfecte rasters) en void-grotten").

**Wat er al vastligt (niet opnieuw vragen):** onmogelijke gangen = rechte, te perfecte 2×1- en 3×3-tunnels in
rasterpatronen, doodlopend of in lussen (broertje van de ring 1-tunnels); void-grotten = grotten met gaten in de bodem naar
de void; gekopieerde grotten = exact herhaald of gespiegeld ("dezelfde gang drie keer naast elkaar, een kamer die zichzelf
spiegelt"); buitenband nauwelijks grotten; dubbelganger = een stuk terrein dat verderop exact gespiegeld nog eens staat, is
**terrein** (geen feature), features worden in fase 1 **niet** gekopieerd; ring 5 droog behalve vanilla's lavalaag onder
Y −54 in de binnenband achter de dam (32 van de bandlijn, 6 rond elke opening naar de void, V9-B); scheuren en gaten tot
onder `min_y` zonder bedrock (V3-A).

**Bezit** (`waves.md` §1/§5): `Ring5Field` (+ `Ring5Output`), nieuw `Ring5Caves`, nieuw `Ring5Copies`, `density_function/
ring_5/overworld/**` (o.a. `caves.json`, `ring_5/overworld.json`), eventueel `noise/ring_5_*` (nieuw), `RingFluidPicker`/
`RingAquifer` (alleen de ring 5-regel: void-gaten in de dam), het blok `// ring 5 — W-terrein` in
`ModDensityFunctionTypes`, de ring 5-terreintests. **Niet:** surface rules (wandblokken, voidsteen-randen van de gaten),
features, carvers (`Ring5Carvers` blijft leeg), biomes (W-biomes); `docs/prompts/`.

**Status:** fase B. **Besluit gebruiker 2026-10-23: V93-1 … V93-8 = A** (de aanbevolen opties van §6); vastgelegd in
`fase-1-wereld.md` → Ring 5 "Grotten" en het beslissingenlog.

---

## 1. Architectuur

### 1.1 Grotten in het veld, exact per blok
Ring 5's terrein is één exact Java-veld (`Ring5Field`, geen `interpolated`). De eigen grotten volgen dat patroon:
- **`Ring5Caves`** (nieuw, constanten + pure vormen, zoals `Ring5Inner`/`Ring5CorruptSpots`): per cel (grootte in §2) de
  beslissingen — net aan/uit, niveau, soort, randen, kamers, gaten, kopieën — **eenmaal per wereld** in `Decisions`
  (`KeyedSlots`-memo, zoals de corrupte plekken), puur uit de wereldzout (`ring_5_cells`) en de cel; nooit via een
  `flat_cache`, geen volgorde-afhankelijkheid (les 75c).
- Per kolom zet `fill` een korte lijst **open intervallen** (`caveBottom[]`/`caveTop[]`, zoals de exacte `spans`) en een
  vlag **"open tot de void"** (void-gat: vanaf de grotvloer tot onder `min_y`). `terrain(x, y, z)` geeft −1 in een open
  interval; spans (bruggen/brokken) gaan vóór. Rastergangen zijn as-uitgelijnd, dus een kolom heeft per gang één
  interval: per blok alleen een paar vergelijkingen.
- **`ring_5/overworld/caves`** wordt echt: `ring_5_output` uitgang `caves` (nieuw, 3D), een code per blok zoals
  `ring_4/overworld/tunnels` (0 rots, 1 gang, 2 kamer, 3 void-gat; voorstel), lezer W-biomes (ertsen 95, eventueel
  wandblokken en voidsteen-randen via de surface rule).
- **`height`**, `initial_height`, `breakup`, `scars` blijven zoals ze zijn (grotten tellen niet mee, contract §4), behalve
  dat een void-gat dat aan de oppervlakte uitkomt (bijv. via een scheur) niets aan `scars` verandert.

### 1.2 Natuurlijke grotten (alleen bij V93-1 A)
Vanilla's noise-grotten (`minecraft:overworld/caves/*`: cheese, spaghetti, entrances; geen noodles/pillars-varianten
afhankelijk van de afstemming) als `min` met de ring 5-dichtheid in `ring_5/overworld.json`, gedempt en **gesloten**:
alleen in de binnenband (band < 0 met een marge), niet binnen de dam van scheuren, plekgaten en void-gaten (anders lava
of open ruimte tegen de void), niet in het bovenste stuk van een kam, niet in de breukzone 4→5 (± 32). Dit is het enige
deel dat geïnterpoleerd is (4 × 8 × 4, zoals ring 4); de sluiting rekent daarom met een marge zoals `Ring4Tunnels`
(`LIFT_SIDE`/`LIFT_UP`). De natuurlijke grotten worden **niet** gekopieerd (§1.4).

### 1.3 Vloeistof en bodem
- Ring 5 blijft droog; grotten zijn lucht. Onder Y −54 in de binnenband geeft `RingAquifer` in open grotruimte vanilla's
  lavalaag (V9-B; nu ontstaat die laag pas echt, `81b-infra.md` backlog).
- **Void-gaten tellen mee in `innerCrackDistance`** (zoals het gat van een corrupte plek in 83b): de dam van 6, de
  lavaregel en de `scars`-rand gelden er zonder eigen code in `RingFluidPicker`. Een void-gat zelf is open tot onder
  `min_y`, zonder bedrock; ernaast onder −54 rots.
- Geen losse blokjes boven de void (voidscan `loose`): elke rand van een gat ≥ 3 dik of vast aan de wand.

### 1.4 Kopieën
- **Gekopieerde grotten** (in `Ring5Caves`): een element (gang, net of kamer) krijgt een transformatie mee — herhaling
  (verschoven, N keer naast elkaar) of spiegeling in zichzelf (symmetrisch om een as door het midden). Puur: de kolom
  vraagt het bronelement terug via de inverse transformatie. Maten: V93-5.
- **Gespiegelde dubbelgangers** (`Ring5Copies`, contract §4): paren bron/doel-vlakken in de binnenband; `source(x, z)`
  geeft voor een doelkolom de gespiegelde bronpositie. `fillInner` van een doelkolom vult de kolom uit de bron (hoogte,
  "net fout"-vormen, scheur of plekgat, glitchvormen) en de eigen grotten in 3D uit de bron; de rand volgt V93-7. Bron en
  doel liggen geheel binnen de binnenband (marges zoals de corrupte plekken: ≥ 32 van bandlijn en zone, ≥ 16 buiten elk
  kasteelbereik), niet over elkaar en niet over een corrupte plek (een plek wordt niet gekopieerd; voorstel). Features
  en de surface volgen de biome en de vorm, dus de surface lijkt vanzelf; bomen e.d. niet (contract).
- Zoekradius per kolom ≤ één cel (constanten zo kiezen dat een kolom alleen zijn eigen cel en de buurcellen vraagt).

### 1.5 Determinisme en prestatie
Alles per cel uit de salt van `ring_5_cells` (nieuwe salts 9301–9310), accessors alleen lezen, memo per wereld. Prestatie:
ring 5 meet nu noise 0,47–0,54 (83b); de eigen grotten zijn per kolom één opzoeking van de cel-elementen + een paar
vergelijkingen per blok. Natuurlijke grotten (V93-1 A) zijn het duurste deel; budget ≤ 1,75, gemeten met `perf.sh perf-r5`.

## 2. Pakket 93 — inhoud (voorstel; getallen na de antwoorden)

1. **Onmogelijke gangen** (V93-2, V93-3): rechte as-uitgelijnde gangen van precies 3 × 3 (hoofdgangen) en 2 × 1 (smalle
   zijgangen, 2 hoog × 1 breed), op vaste niveaus om de 16 (Y 48, 32, 16, 0, −16, −32; ≥ 12 rots boven het plafond, anders
   valt het stuk weg behalve bij een geplande ingang), haakse hoeken, kruisingen, doodlopers en lussen uit een raster
   waarvan elke rand een kans heeft; rechte schachten (3 × 3, verticaal) tussen niveaus. Wanden onbekleed (het blok is van
   de surface/default; W-biomes kan later bekleden).
2. **Void-grotten** (V93-4): kamers met gaten in de vloer tot onder `min_y` (zonder bedrock), diep in de binnenband;
   gaten in de dam-administratie (§1.3); de uitgang `caves` markeert gat en rand (lezer W-biomes, voidsteen-rand).
3. **Gekopieerde/gespiegelde grotten** (V93-5): een deel van de netten en kamers herhaald of in zichzelf gespiegeld.
4. **Natuurlijke grotten** (V93-1): gedempte vanilla-grotten of niets.
5. **Gespiegelde dubbelgangers** (V93-6, V93-7): `Ring5Copies`.
6. **Buitenband** (V93-8): volgens het antwoord; standaard geen grotten in pinakels, gordels, bruggen, ruggen en brokken.
7. **Kaart/inspectie:** geen nieuw debugcommando (command/* is van I); de doorsneden van `/ca terrainmap section` tonen de
   grotten. Een lijst van de dubbelgangers (`/ca copies`) zou handig zijn voor de teststop → backlog `[infra]`, of main
   vraagt I.

## 3. Tests (unit, in `build`)
- `Ring5CavesTest` (nieuw): puur (twee velden, zelfde seed = zelfde grotten; andere seed andere); gangen exact 3 × 3 en
  2 × 1, recht en as-uitgelijnd, alleen op de vaste niveaus; minstens één lus en één doodloper per x netten; schachten
  verbinden twee niveaus; rotsdek ≥ 12 behalve bij ingangen; void-gaten tot onder `min_y` zonder vast blok, alleen in
  kamers, `innerCrackDistance` ≤ 0 op het gat en ≤ 6 rondom; geen open ruimte onder −54 binnen 6 van een gat, scheur of
  plekgat; geen grotten in de marges (bandlijn, zone 4→5, kastelen) en in de buitenband volgens V93-8; kopieën: herhaalde
  elementen blok voor blok gelijk na de verschuiving, gespiegelde kamers symmetrisch; aandeel open ruimte per niveau
  gemeten.
- `Ring5CopiesTest` (nieuw): puur; elk doelvlak is blok voor blok de spiegeling van zijn bron (hoogte, grotten, scheuren)
  buiten de randstrook; de randstrook volgens V93-7; bron/doel geheel in de binnenband, niet over een plek of elkaar;
  afstand en aantal per oppervlak volgens V93-6.
- `Ring5JsonTest`: `ring_5/overworld/caves` geen stub meer; `ring_5/overworld` met/zonder natuurlijke grotten volgens V93-1.
- `RingFluidPickerTest`/`RingAquiferTest`: dam rond void-gaten; lavalaag alleen in open grotruimte onder −54 buiten de dam.

## 4. Gates (fase B)
- **B** `./gradlew build --offline` groen (incl. `ClientIsolationTest`); **D** `runData --offline` schoon (verwacht geen
  wijziging in `src/generated`: alleen handgeschreven JSON).
- **S-smoke** `tools/test/server.sh start r5-93 --fresh` (slot via `slots.sh`; bij `C:/Temp/ca-user-test.lock` wachten):
  log 0 ERROR, geen client-class; RCON `/ca terrainmap 8900 0 1400 4`, `section 8000 0 9000 0`, doorsneden door een net,
  een void-grot en een dubbelganger (bron + doel); kaarten naar `build/test/maps/93/`.
- **Voidscan** `world.sh` op `tools/test/areas/ring5.txt` + `voidscan.py --strict --rings` = 0, `lavascan.py --strict`
  lek 0 (nu met echte lavalaag).
- **det.sh** `--areas tools/test/areas/ring5.txt`: own 0.
- **perf.sh** `--areas tools/test/areas/perf-r5.txt`: noise ≤ 1,75 per gebied.
- `/code-review medium` op `main...ring5/93`, bevindingen opgelost of vastgelegd.

## 5. Client/server-impact
Puur worldgen (server-side): `Ring5Field`, `Ring5Caves`, `Ring5Copies` zijn common en worden alleen op de server
gebruikt; geen client-class, geen payload, geen `PROTOCOL_VERSION`, geen config. De client ziet alleen de blokken.

## 6. Open vragen voor de gebruiker (fase A)

Aanbevolen optie steeds eerst. "Perf" = de noise-stap tegen vanilla (budget ≤ 1,75; ring 5 nu ~0,5).

**V93-1 — Natuurlijke grotten in de binnenband?** Naast de vreemde grotten: een "gewoon" grotnet?
- **A (aanbevolen): gedempte vanilla-grotten** (cheese + spaghetti, ~half zo veel als vanilla, droog, alleen binnenband,
  dicht bij scheuren/gaten). De ondergrond voelt eerst gewoon en dan klopt er iets niet; ertsen (95) liggen bloot. *Perf:*
  het duurste deel (schatting noise ~0,9–1,3). *Complexiteit:* middel (sluiting rond dam en gaten zoals ring 4). Niet
  gekopieerd of gespiegeld (geïnterpoleerde ruis kan niet naadloos verplaatst worden). *Multiplayer:* geen.
- **B: geen natuurlijke grotten**, alleen gangen, void-grotten en kopieën. Alles ondergronds is onnatuurlijk en zeldzaam;
  de rest massief. *Perf:* goedkoopst. *Speelgevoel:* kaal, minder verkennen; ertsen alleen via graven en scheurwanden.
- **C: eigen Java-"natuurlijke" grotten** (wormgangen en kamers in het veld). Alles kopieerbaar en exact. *Complexiteit:*
  hoog (een tweede grotgenerator). *Perf:* laag tot middel.

**V93-2 — Hoe liggen de onmogelijke gangen?**
- **A (aanbevolen): losse netten** per cel van 128 (kans ~0,4), elk op één vast niveau (Y 48 / 32 / 16 / 0 / −16 / −32),
  een raster om de 16 (3 × 3) of 8 (2 × 1); elke rasterrand met kans ~0,6, dus vanzelf doodlopers en lussen; soms een
  rechte schacht naar een net op een ander niveau. *Speelgevoel:* een vondst, elk net een eigen doolhofje. *Complexiteit:*
  laag-middel. *Perf:* laag.
- **B: één doorlopend wereldraster** onder de hele binnenband (alle niveaus, randen met een kans). Je kunt ondergronds
  reizen; heel kunstmatig, kan eentonig worden. *Perf:* laag (meer open ruimte, iets meer lava diep).
- **C: zeldzame grote doolhoven** (~1 per 500 × 500, 3–4 niveaus, 150–250 breed). Memorabel, maar je vindt er weinig.

**V93-3 — Ingangen vanaf de oppervlakte?**
- **A (aanbevolen): te perfecte openingen** waar een gang een helling, klif of kuil snijdt (vierkante gaten in de
  heuvelwand) plus per net één rechte schacht (3 × 3) tot het oppervlak; void-grotten ook zichtbaar via scheuren en
  plekgaten. *Speelgevoel:* vindbaar en al van buiten "fout". *Complexiteit:* laag.
- **B: geen ingangen**: alleen via graven, scheuren of een plekgat. Meer horror, maar de meeste spelers zien ze nooit.
- **C: veel ingangen** (elke rasterrand die het oppervlak raakt mag doorbreken). Overal gaten; minder bijzonder.

**V93-4 — Hoe zien de void-grotten eruit?**
- **A (aanbevolen): kamers met gaten** — rafelige kamers Ø 12–30, 6–14 hoog, diep (Y −40 … 10), ~1 per 256 × 256, elk
  met 1–4 gaten Ø 2–6 in de vloer tot in de void; de gaten krijgen een voidsteen-rand (DF-uitgang nu, de surface regel
  W-biomes in 94/95). *Speelgevoel:* in het donker sta je ineens aan de rand; de voidsteen-deeltjes waarschuwen.
- **B: gaten overal**: elke grot of gang kan een gat in de vloer hebben (~1 per 64 blokken gang). Veel gevaarlijker,
  minder "plek". *Complexiteit:* laag.
- **C: ondergrondse afgronden**: zeldzame (~1 per 600 × 600) grote holtes waarvan de hele vloer weg is, met rotsrichels
  langs de wand. Spectaculair, zeldzaam. *Complexiteit:* middel (richels ≥ 3 dik, voidscan).

**V93-5 — Gekopieerde/gespiegelde grotten: hoe vaak en hoe ver?**
- **A (aanbevolen): dichtbij en zichtbaar**: ~1 op 4 netten en kamers is een kopie — een gang 2–3 keer parallel naast
  elkaar (8–24 blokken verschoven, blok voor blok gelijk) of een kamer die zichzelf spiegelt (symmetrisch om een as);
  alleen de eigen grotten, niet de natuurlijke (V93-1). *Speelgevoel:* je merkt het binnen één verkenning.
- **B: ver uit elkaar**: bron en kopie 100–300 blokken uit elkaar; pas te zien als je beide vindt. Subtiel, makkelijk
  gemist.
- **C: allebei** (A vaak, B af en toe). Iets meer werk, meer variatie.

**V93-6 — Gespiegelde dubbelgangers: grootte, afstand, aantal?**
- **A (aanbevolen): 48 × 48** (3 × 3 chunks) dat **64–160 blokken verderop gespiegeld** staat (spiegelas haaks op de
  lijn bron–doel, dus vanaf de bron zie je het spiegelbeeld), ~1 per 600 × 600 in de binnenband (~40–60 per wereld);
  vorm, scheuren, glitchvormen en de eigen grotten gaan mee, features niet (contract). *Speelgevoel:* een heuvel die je
  al kende, maar omgekeerd. *Perf:* laag (de doelkolom vraagt de bron).
- **B: klein en vaak**: 32 × 32, ~1 per 300 × 300 (~200 per wereld). Vaker raak, maar kleinere vormen zijn minder
  herkenbaar.
- **C: groot en zeldzaam**: een hele heuvel ~96 × 96, ~1 per 1.000 × 1.000 (~15 per wereld). Memorabel, een
  "bezienswaardigheid".

**V93-7 — De rand van een dubbelganger?**
- **A (aanbevolen): zachte rand van 8–12 blokken** naar het eigen terrein; je ziet het pas als je de vorm herkent.
  *Complexiteit:* laag.
- **B: harde naad**: een rechte trede of wand op de rand van het vlak (een geplakt stuk, past bij de corrupte plekken).
  Opvallend, minder subtiel.
- **C: alleen waar bron en doel aan de rand al bijna even hoog liggen** (plus een rand van 4). Het subtielst, maar minder
  plekken en meer rekenwerk bij de keuze (eenmaal per wereld).

**V93-8 — "Nauwelijks grotten" in de buitenband: hoeveel?**
- **A (aanbevolen): alleen zeldzame void-grotten in dikke schotsen** (≥ 40 dik, dichtbij de bandlijn): een kamer met een
  gat naar de void eronder, ~1 per 512 × 512; geen netten; niets in pinakels, gordels, bruggen, ruggen en brokken.
  *Speelgevoel:* af en toe een blik door de vloer in het niets.
- **B: helemaal geen grotten** buiten de bandlijn (de schotsen blijven massief). Eenvoudigst.
- **C: dezelfde netten als binnen, maar ~1/5 zo vaak**, ook in de schotsen (een gang kan aan de onderkant van een schots
  uitkomen). Meer verkennen, meer losse randen (voidscan-werk).

## Voortgang
- Voortgang: 2026-10-23 18:34 worktree `P:/Projecten/Java/wt/r5-93` op `ring5/93` vanaf `061a20c4`; `setup.sh W-terrein 8`
  (server 25578, RCON 25588); plan en vragen V93-1 … V93-8; wacht op antwoorden.
- Voortgang: 2026-10-23 18:54 antwoorden: V93-1 … V93-8 = A; vastgelegd in `fase-1-wereld.md` en het beslissingenlog; start bouw.
- Voortgang: 2026-10-23 19:21 `Ring5Caves`, `Ring5Copies`, haken in `Ring5Field`/`Ring5Output`, JSON (natuurlijke grotten,
  `caves`, `cave_closure`), dam via `innerOpeningDistance`; tests `Ring5CavesTest`, `Ring5CopiesTest`; B groen (1.358 tests,
  1 overgeslagen), D zonder wijziging; S-smoke `r5-93` (kaarten); afstemming natuurlijke grotten.
- Voortgang: 2026-10-23 20:05 bouwcommit `7455e1b8`; code-review: 8 punten (zie "Review van 7455e1b8").
- Voortgang: 2026-10-23 21:00 (hervat na onderbreking) reviewpunten 1–7 opgelost; gates: zie "Review van 7455e1b8".

## Review van 7455e1b8
1. **Sluiting te zwak** (vanilla-ingangen open op schotsen, pinakels, bandlijn, zone 4→5, rond scheuren/gaten): de
   uitgang `cave_zone` geeft 0 waar `cave_closure` = 2 (dan alleen `terrain`, geen entrances); in de overgang telt de
   sluiting 4 × bij de entrances (5 × entrances ≥ ~−6); entrances en natuurlijke grotten staan binnen hun `interpolated`
   in een `range_choice` op `cave_closure` (dicht = constante 1 / 2, geen ruis).
2. **Exacte vormen naar de diepe tak** (terrain = 1,0 op spans, glitch-lagen, kubussen, bruggen): de router kiest nu op
   `Ring5Field.caveZone` i.p.v. op `terrain`: 0 op een span, in de glitch-lagen (top − 16 en hoger) en dicht; 1 tot 8
   onder de top (bij lagen: onder top − 16); 2 dieper.
3. **Dubbelganger-doel:** sluiting en dam rekenen met de echte kolom (`openingDistance(x, z)`: eigen openingen én die van
   de bron, gespiegeld) en de kleinste ruimte van doel en bron (`naturalRoom`).
4. **Dam per vloeistofblok te duur:** `innerOpeningDistance` staat op `Column.opening` (in `fill` gezet voor de
   binnenband, anders lui; puur: `openingDistance`).
5. **`new int[4]` per kolom:** scratch `Column.chamberOut`.
6. **`NET_MARGIN` geschonden:** gekopieerde netten tellen de zijgangen van de laatste kopie en de schacht aan het begin
   mee; gridnetten de schachten/gangen ± 1 rond de buitenste knopen (was 1 te ver). Test: elk net ≥ 12 binnen zijn cel.
7. **Grotten op plekken en dubbelgangers:** `nearSpotOrCopy` (netten: hun omhullende; kamers: middelpunt ± reikwijdte)
   houdt ze ≥ `CAVE_CLEARANCE` 8 van de chunks van een corrupte plek en van beide vierkanten van een dubbelganger. Gevolg:
   in een dubbelganger zitten in de praktijk geen eigen grotten meer (V93-6/7 zei "eigen grotten mee"); open punt in de
   backlog voor de gebruiker.
8. **Niet gewijzigd:** de uitgang `caves` blijft (afnemer W-biomes 95: wandblok en voidsteen-rand); in de backlog.

Gemeten na de review (testseed): 587 netten (50 % breed, 113 met twee niveaus, 151 gekopieerd), 409 void-grotten
(100 gespiegeld, 1.007 gaten), 22 buitenkamers, 51 dubbelgangers. Scangebieden opnieuw bepaald in `build/test/areas-93.txt`.

**Gates na de review (2026-10-23):** B groen (1.360 tests, 1 overgeslagen); D schoon (geen wijziging in `src/generated`);
S-smoke `r5-93` (`world.sh --keep`, verse wereld) 0 ERROR, geen client-class; voidscan `--strict --rings` op `ring5.txt` +
`areas-93.txt` 0 fouten; lavascan `--strict` lek 0 (trede 0, val 0); det 1.277/1.277 chunks gelijk (own 0, other 0);
perf `perf-r5` noise-mediaan max 1,44 (zone45; wastes 1,07, halls 1,07, cathedral 1,06, rest ≤ 0,94) ≤ 1,75. Kaarten
(`/ca terrainmap 8900 0 1400 4`, doorsneden 8000→9000 op z 0, net, gekopieerd net, void-grot, dubbelganger) in
`build/test/maps/93/`. Her-review van de fixes: geen nieuwe fouten; één bestaand gat (grotten in hoge wanden van
corrupte plekken) in de backlog.

## CODEBASE-delta
Bijgewerkt in `CODEBASE.md` (op verzoek van main in deze change): `Ring5Caves`, `Ring5Copies` (bestandsboom en Belangrijke
classes), `Ring5Field` (93-alinea), `Ring5Output` (`caves`, `cave_closure`), `RingFluidPicker` (`innerOpeningDistance`),
de JSON-rij `ring_5/overworld/*` (`terrain`, `caves`, `cave_closure`, `cave_zone`, `natural_caves`, `ring_5/overworld.json`,
`void/overworld`), tests `Ring5CavesTest`, `Ring5CopiesTest`, `Ring5JsonTest`; na de review `caveZone`, `Column.opening`/
`room`/`chamberOut`, `nearSpotOrCopy`, `Spot.touches`, `Pair.touches`.

## Beslissingen
**Gebruiker 2026-10-23:** V93-1 … V93-8 = A (§6).

**Implementatiekeuzes (van mij):**
- **Eigen grotten in het veld, exact per blok:** per kolom open intervallen (max. 8) die `terrain` na de spans en vóór de
  void als −1 geeft; geen DF-wrapper, geen feature, geen carver. Netten als lijst van `Box`en (as-uitgelijnd, dus een kolom
  vergelijkt alleen gehele getallen); elk element ligt binnen zijn cel (netten ≥ 12, kamers ≥ 40), dus een kolom vraagt
  alleen zijn eigen cel.
- **Netten:** grid-knopen in het midden van de gang (breed: midden ± 1, smal: de kolom zelf); een net zonder enige rand
  vervalt; de ingangsschacht staat op een knoop met een gang op het bovenste niveau, de tussenschacht op een knoop met een
  gang op beide niveaus (anders krijgt het lagere niveau daar één rand). Geen rotsdek-regel: waar een gang hoger ligt dan
  de grond (dalen, verzakte plekken) snijdt hij open ("een gang die een helling snijdt"). Gemeten (testseed, na de
  review): 587 netten, 50 % breed, 113 met twee niveaus, 26 % gekopieerd; van de 436 gridnetten 435 met een doodloper en
  341 met een lus.
- **Gekopieerde netten:** de zijgangen alleen aan één kant en ≥ 2 rots voor de volgende kopie; elke kopie een eigen schacht
  (drie gelijke gaten op een rij aan de oppervlakte).
- **Void-grotten:** omtrek met drie harmonischen (k = 2–4, samen ±20 %); vloer stijgt 3 naar de wand, plafond een koepel
  (`height · √(1 − t²)`); gaten binnen 0,8 × de kleinste omtrek; gespiegeld = as langs x of z door een geheel
  middelpunt (blokken vallen op blokken), gaten in paren, een oneven laatste gat op de as. De rand (code 4) = de 2 blokken
  vloer binnen 2 van een gat. Gemeten (na de review): 409 kamers (24 % gespiegeld), 1.007 gaten.
- **Dam:** de gaten tellen mee in `Ring5Field.innerOpeningDistance` (nieuw; `innerCrackDistance` blijft de scheur/plek-
  afstand voor de vorm); `RingFluidPicker.CURRENT_BANDS` leest de nieuwe. In een dubbelganger telt ook de afstand op de
  bronpositie (de gespiegelde gaten).
- **Natuurlijke grotten (V93-1 A):** vanilla's cheese (`0,4 + cave_cheese` i.p.v. 0,27; gemeten: binnenband ~4 % lucht
  onder de grond tegen ~6 % in ring 1 met 0,37 — bij 0,4 iets minder, zie Tests), `cave_layer`, `entrances` en spaghetti,
  **zonder** noodles en pillars, in één `interpolated`; boven op de exacte dichtheid met `min`. Ondiep (de bovenste 8
  blokken) alleen `entrances`, zoals vanilla; na de review kiest de uitgang `cave_zone` de tak (niet meer `terrain` < 1). De sluiting (`cave_closure`, 0..2) is 2D uit het veld: dicht
  binnen 24 van de bandlijn, 32 na zone 4→5, 16 buiten kasteelbereik en 8 van een opening (scheur, plekgat, void-gat),
  over 12 blokken open; buiten de binnenband altijd dicht. `void/overworld` wijst naar `ring_5/overworld/terrain`
  (zonder natuurlijke grotten, die daar toch dicht zijn): scheelt de geïnterpoleerde ruis in de void-ring.
- **Buitenband (V93-8 A):** kandidaat per cel van 512, alleen 96–300 voorbij de bandlijn, 17 monsters (midden, 8 op de
  halve en 8 op de volle reikwijdte) moeten schots zijn zonder scheur, ≥ 40 dik; de vloer tussen onderkant + 4 en top − 6
  − hoogte. Gemeten: 22 kamers.
- **Dubbelgangers (V93-6/7 A):** spiegeling in het vlak halverwege, langs x of z (blok-exact; schuine richtingen zouden niet
  op het raster vallen). Tot 2 plekken per cel geprobeerd (de binnenband is maar ~650 breed): 51 paren op de testseed
  (met 1 poging 32, met 4 74). Beide vierkanten diep in de binnenband, geen scheur of plekgat binnen 16, geen plek binnen 8,
  geen glitchkubussen erop (een rand zou een kubus doorsnijden). De doelkolom binnen de rand wordt op de bronpositie gevuld
  (vorm, glitch, eigen grotten); verschoven schijven alleen ≥ rand + 6 binnen (hun buurkolom moet ook een kopie zijn), de
  verschuiving gespiegeld. De rand: alleen de grond gemengd (smoothstep over 8–12), eigen grotten. De biome volgt de eigen
  klimaatassen, dus een gekopieerde glitchvorm kan in een andere biome staan (net fout; features en surface volgen de
  biome).

## Backlog
Ook in `docs/backlog.md` ("Uit 93 ring 5 golf 2"):
- [infra] `/ca copies` (lijst + teleport naar bron en doel van de dubbelgangers) voor de teststop, zoals `/ca castles`;
  `command/*` is van I.
- [worldgen] (W-biomes, na 93) de surface van ring 5-grotten: wandblok van de gangen (bijv. glad `corrupted_stone`) en de
  voidsteen-rand rond de void-gaten lezen de uitgang `caves` (code 4 = rand).
- [worldgen] 6b: de ingangsschachten zijn recht, 3 × 3 en tot ~180 diep: zonder ladder dodelijk; een uitweg is een nieuw
  ontwerppunt.
- [infra] De gebieden van `build/test/areas-93.txt` (hieronder bij Tests) in `tools/test/areas/ring5.txt`.
