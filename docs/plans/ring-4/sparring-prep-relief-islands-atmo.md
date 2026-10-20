# Sparring-voorbereiding ring 4: reliëf, eilanden, lucht/mist

Voorbereiding (2026-10-20, op main `b254b734`) voor drie sparringsessies na teststop 5b. **Geen besluiten**: alles
hieronder zijn voorstellen voor `AskUserQuestion`-rondes (max. 4 vragen per ronde, aanbevolen optie bovenaan). Na elke
sessie gaan de besluiten naar `fase-1-wereld.md` §Ring 4 en `02-beslissingenlog.md`.

| Onderwerp | Categorie | Eén change |
|---|---|---|
| A. Reliëf (vlak terrein, "strepen") | `worldgen` | `Ring4Relief` (+ evt. `IslandField`) |
| B. Zwevende eilanden (klein, ver uit elkaar) | `worldgen` | `IslandPlacement` / `IslandShape` (+ ranken/bogen) |
| C. Lucht en mist (vlak wit/grijs) | `atmosphere` | `Ring4Atmosphere` (biome-kleuren) + `client/weather/*` |

Bronnen gelezen: `CLAUDE.md` §3, `CODEBASE.md` (ring 4), `fase-1-wereld.md` §Ring 4 (incl. "Aankleding uitrol"),
`teststop-5b.md` §5.1, `02-beslissingenlog.md` (ring 4), `docs/backlog.md` regels 370–405, de code genoemd per onderwerp,
de screenshot van de gebruiker (`run/screenshots/2026-10-18_22.05.42.png`) en de contactbladen van de decor-proef en
decor C (`build/test/shots/after3-fjords.png`, `after3-sky.png` in hun worktrees). Geen server of client gestart.

---

## 0. Gemeenschappelijke randvoorwaarden

- **Determinisme:** worldgen puur per seed (geen ongeseede random); `IslandField` beslist per cel van 128 eenmaal per
  wereld (`Decisions`), terrein en vallen moeten in elke laadvolgorde gelijk zijn (`det.sh`). Alle aankleding van de
  decor-uitrol leest het terrein (bevroren heightmaps, `IslandField`-vragen): een terreinwijziging verschuift dus ook
  alle aankleding, blikvangers, liften en vallen.
- **Prestatie:** noise-stap ≤ 1,75× vanilla. Laatste metingen: ring 4 noise 0,59–0,74 (`perf-int2`, rustige machine),
  0,84–1,08 onder belasting (`perf-int2b`), bomen 0,96–1,02 (`perf-trees`); `verdant_falls` 1,09, `steam_fens` 1,14.
  Er is ruimte (~0,7×), maar 3D-vormen per blok (`thin`) zijn duur; 2D-ruis per kolom is goedkoop.
- **Vlakke zee (besluit 2026-10-16):** alle open ruimte van ring 4 onder Y 63 is water tot Y 62, erboven lucht; alleen
  het tunnelnet en droge zinkgaten zijn uitgezonderd. Gevolg: elk dal of elke kom onder Y 63 wordt zeewater; meren boven
  zee bestaan alleen als terrein (gatmeren op hoog land, hemelmeren) en vallen alleen als vastgelegde vallen
  (waterscan: 0 water tegen lucht buiten de vallen).
- **Liften en vallen:** `ring_4/island_lifts` (één `updraft`-kolom bij ~1 op 4 middel-/reuzeneilanden, geen lift
  binnen 6 van een val), valcap 3 per chunk (eilanden; `verdant_falls` 6), kluitvallen wijken voor liften en grotere
  vallen. Meer of andere eilanden veranderen liften en vallen automatisch mee; de regels blijven gelden.
- **Multiplayer:** gameplay server-side; atmosfeer-*staat* (ringweer) leeft op de server en gaat via
  `RingWeatherPayload` (`ModNetwork.PROTOCOL_VERSION` nu `"8"`) naar de client; rendering (fog, lucht, wolken,
  particles) alleen in `client/` via `Dist.CLIENT` en client-mixins. Biome-kleuren gaan als biome-data vanzelf mee.
- **Testritme (besluit 2026-10-19):** afstemrondes met de lichte lus (build, runData zonder diff, server-smoke,
  contactbladen); determinisme, perf en waterscan alleen vóór een formele teststop. A en B veranderen terrein: die
  moeten vóór de volgende formele teststop wél door `det.sh`, `perf.sh` en `waterscan.sh`.

---

## A. Reliëf — `worldgen`

### A.1 Probleem zoals vastgelegd

- `teststop-5b.md` §5.1: *"het terrein is overal vlak en in strepen, met weinig hoogteverschil;"* en *"grote vlakke
  bruine veenvlakten (`peat_moors`) met alleen losse lichtgroene vlekjes; geen planten, riet, plassen of reliëf erop;"*
- `backlog.md` r. 371: *"[worldgen] Ring 4-terrein voelt vlak en "in strepen" (teststop 5b §5.1): reliëf herzien. Eerst
  sparren, na het aankledings-proefstuk."*
- `02-beslissingenlog.md` 2026-10-19: *"Reliëf, zwevende eilanden (meer/groter, begroeide onderkant) en lucht/mist
  (atmosphere) als aparte changes na de aankleding, elk eerst met een sparring"*.
- Eerder goedgekeurd (blijft staan tenzij de gebruiker het herziet): heuvelfix 2026-10-15 *"karakter houden (terrassen,
  fjordkliffen), randen zachter"*, en teststop 5a2 *"dit ziet er top uit allemaal"*.

Wat de beelden laten zien: in de screenshot zijn de veenvlakten bruin en vrijwel plat met lange contourlijnen (de
1-bloksstappen van een zeer flauwe helling), de `cascade_vale`-terrassen lezen als evenwijdige banden; in het
fjord-contactblad (`after3-fjords.png`) is het plateau een zacht golvende vlakte en de fjordwand een lange, rechte,
gestreepte muur.

### A.2 Huidige implementatie

- **Code:** `worldgen/densityfunction/Ring4Relief.java` (basisterrein per kolom, 2D, puur); onderdeel van
  `IslandField` (veld `relief` in `density_function/ring_4/overworld/islands.json`). `IslandField` voegt de gaten van
  de eilanden, de fjordband per blok (`thin`, `withExactBand`), de zinkgaten en het tunnelnet toe; `IslandOutput`
  levert `height`/`surface`/`slope`/… aan de stack en de surface rules (`ring4_threshold` op `slope`, `rim`, `cliff`).
- **Ruis** (`worldgen/noise/`): `ring_4_hills` firstOctave −8 [1; 0,5; 0,25] (~256–300 blokken), `ring_4_detail` −5
  [1; 0,5] (~32–40), `ring_4_shoals` −7, `ring_4_warp` −7, `ring_4_water`/`ring_4_growth` −10 (klimaatassen = biome),
  plus `ring_4_rivers`/`ring_4_fjords`.
- **Landvorm per biome** (landvormen gekoppeld aan de biome-drempels: water-as ±0,125/0, groei −0,075/0; overgang
  `BLEND` 0,045 ≈ 25 blokken):

| Biome | Formule (blokken) | Bereik ongeveer |
|---|---|---|
| `crystal_lagoon` | 42 + 21,5·√shoal + 1,2·d | Y 42 (trog) – 63,5 (zandbank) |
| `lotus_shallows` | 59,5 + 2·shoal + d | Y 59–62,5 (onder zee) |
| `glowroot_swamp` | 63,3 + 2,5·hills + 1,5·d | Y ~59–67 |
| `cascade_vale` | 64 + (5 + 45·hill01)·inland + golf, daarna treden van **8** (60 % trede / 40 % stootbord) | Y ~69–114 |
| `mist_fjords` | plateau 71 + 32·inland·(0,55 + 0,45·hill01) + 1·coarse; geul bodem 40, halve breedte 12, wand 2:1 | plateau Y 71–103 |
| `drowned_forest` | 55 + 2,5·d + 2·hills | Y ~51–59 (onder zee) |
| `reed_marsh` | 63 + 1,2·hills + 0,8·d | Y ~61–65 |
| `peat_moors` | 65,5 + 1,5·hill01 + 0,6·d; zinkgaten (cel 40, kans 0,5, r 3–7, diep 7–16) | **Y ~65–67,6** |
| rivieren | bed Y 57, halve breedte 5,5, oever 0,6 per blok | door al het land |

  De bedorven band en het moeras hebben dus 2–6 blokken totaal hoogteverschil; alleen cascades en fjordplateau gaan
  hoger, en de cascades altijd in treden van 8. Er is geen 3D-reliëf (overhang, rotspunt) buiten de fjordwand
  (`rough`) en `verdant_falls`.
- **Afhankelijkheden van de hoogte:** eilandhoogte (`IslandPlacement.candidate`: bodem ≥ hoogste grond onder de voetafdruk
  + 15…200), gatmeren boven zee op hoog land, uitlopen naar open water (< Y 61), wereldboomplekken (grond Y 44–96),
  tunnels (`CAVE_FLOOR_MARGIN` 14), de voetrivier van `verdant_falls` (schuift naar binnen waar ring 4 aan de zonerand
  hoog ligt), zuigmodder-oevers (water binnen 2, bovenkant gelijk of tot 4 lager), gifpoelen in gesloten kommen,
  stenen op hellingen (`slope` ≥ 1,5/2), bosjes/aankleding op `ON_GROUND`.

### A.3 Randvoorwaarden specifiek

- Ring 4 blijft lager dan ring 3 (de afdaling van `verdant_falls` 28–45 vanaf een rand van 88–120).
- Land dat onder Y 63 zakt wordt zee (vlakke zee); reliëf in lage biomes kan dus alleen *omhoog* zonder het water-
  aandeel te veranderen (~50/50 land/water, besluit 2026-10-10).
- Biome = landvorm (dezelfde drempels): een nieuwe landvorm verschuift geen biomes, maar wel alles wat de aankleding
  op die landvorm doet.
- 2D-reliëf in `Ring4Relief` is goedkoop (één of twee extra ruiswaarden per kolom); 3D-rotsvormen via `thin` kosten per
  blok en moeten exact per blok gesneden worden (het 4×4-raster maakt anders zaagtanden, les van de heuvelfix).

### A.4 Vragen — ronde A1 (wat en hoeveel)

**A1-1 — Waar moet er reliëf bij?** (header: `Waar`)
1. **Alle landbiomes, elk op hun eigen manier (aanbevolen)** — veen bulten en ruggen, riet en moeras golvend,
   fjordplateau met heuvels en knobbels, cascades hogere/ongelijke terrassen. *Complexiteit:* middel (één change in
   `Ring4Relief`, per landvorm een term). *Perf:* + ~0,02–0,05 (2D). *Speelgevoel:* overal iets te ontdekken, behoudt
   de biome-karakters.
2. **Alleen de vlakste: `peat_moors`, `reed_marsh`, `glowroot_swamp`** — mythisch blijft zoals goedgekeurd in 5a2.
   *Complexiteit:* klein. *Speelgevoel:* lost de screenshot op, fjorden/cascades blijven zoals ze zijn.
3. **Eén ring-brede heuvellaag onder alle landvormen** (macroheuvels 0–20 boven op elke landvorm). *Complexiteit:*
   klein. *Risico:* vlakke biomes (lotus, lagune) kunnen uit het water komen of de 50/50 verschuiven; minder karakter
   per biome.
4. **Rotsformaties als terrein** (losse rotspunten, tors, zeestapels in de lagune, knobbels op het plateau) i.p.v.
   meer helling. *Complexiteit:* middel-hoog (3D, exact per blok). *Perf:* + ~0,05–0,15 lokaal. *Speelgevoel:*
   herkenningspunten, vlakke delen blijven vlak.

**A1-2 — Hoeveel hoogteverschil?** (header: `Hoogte`)
1. **Middel: heuvels tot ~15–35 boven het huidige land (aanbevolen)** — veen tot Y ~75–85 op ruggen, fjordplateau
   tot ~120, cascades tot ~130. *Gevolg:* eilanden komen mee omhoog (clearance gaat van de hoogste grond uit), meer
   gatmeren boven zee en meer vallen; past nog onder de rand van ring 3 (88–120) op de meeste plekken, de voetrivier
   van `verdant_falls` schuift waar nodig naar binnen (bestaande regel).
2. **Klein: +5–15** — subtiel, laag risico voor water, aankleding en eilanden. Mogelijk nog steeds "vlak" van boven.
3. **Groot: bergruggen tot Y 150+ en fjordwanden van 60+** — dramatisch; botst met "ring 4 lager dan ring 3", met de
   wereldboomplekken (grond ≤ 96), met eilandhoogtes (MAX_TOP 290) en kost meer aan de afdaling.
4. **Per biome verschillend, door jou te kiezen in ronde A2** — eerst per biome laten zien (contactbladen) en dan pas
   de maat vastleggen.

**A1-3 — De "strepen": wat bedoel je?** (header: `Strepen`, meerkeuze)
1. **De contourlijnen op de vlakke veen-/rietvlakten (aanbevolen)** — 1-bloksstappen van een zeer flauwe helling; met
   bulten/ruggen en een korte detailruis verdwijnen de lange evenwijdige lijnen.
2. **De terrassen van `cascade_vale`** — treden van precies 8 met evenwijdige stootborden; zie A2-2.
3. **De fjordwanden** — lange rechte gestreepte muren (aarde/steen in lagen); vooral oppervlak (surface rule) en
   rechte geulen.
4. **Alle drie.**

**A1-4 — Mag het water-aandeel verschuiven?** (header: `Water`)
1. **Nee, 50/50 blijft; reliëf alleen boven zee op bestaand land (aanbevolen)** — dalen tussen nieuwe heuvels blijven
   ≥ Y 64 of worden bewust rivier/zee. *Gevolg:* lagune en lotus blijven zoals ze zijn.
2. **Iets meer land** (zandbanken/atollen van de lagune en eilandjes in lotus hoger) — combineert met de aankleding
   van groep A (zandbanken, eilandjes).
3. **Iets meer water** (meer inhammen/meertjes tussen de heuvels in veen en moeras) — alles onder Y 63 wordt zeewater
   (vlakke zee); zuigmodder- en gifpoelregels werken automatisch op de nieuwe oevers.

### A.5 Vragen — ronde A2 (vorm per biome)

**A2-1 — `peat_moors`/`reed_marsh`: welke vorm?** (header: `Veen`)
1. **Bulten en lage ruggen (2–8 hoog) met kommen ertussen, zinkgaten blijven (aanbevolen)** — "hoogveen"; plassen en
   gifpoelen in de kommen (bestaande regels: gesloten kom). *Complexiteit:* klein.
2. **Grote zachte golven (10–20, ~200 blokken)** — van boven minder vlak, van dichtbij nog steeds glad.
3. **Veenterrassen/afgekalfde randen** (veenbanken van 2–4 met steile kanten langs water) — sterk karakter, meer werk
   (exacte randen per blok zoals de fjord).
4. **Zo laten** (de aankleding van decor B vult het al).

**A2-2 — `cascade_vale`-terrassen?** (header: `Terrassen`)
1. **Treden variëren: hoogte 5–12 per trede en soms een trede overslaan, plus hogere heuvels (aanbevolen)** — breekt
   de evenwijdige banden, karakter blijft. *Risico:* klein; de afronding (60/40, smootherstep) blijft.
2. **Terrassen alleen op een deel (bijv. 50 %), elders glooiende heuvels** — afwisseling, meer werk aan de mengzone.
3. **Hogere terrassen (tot Y ~130) met grotere watervallen** — spectaculairder; raakt de valcap en de waterscan.
4. **Zo laten** (goedgekeurd in 5a2).

**A2-3 — `mist_fjords`-plateau en wanden?** (header: `Fjorden`)
1. **Heuvels en rotsknobbels op het plateau, wand onregelmatiger (richels, uitstekende rotsen zoals de klif uit 5a)
   (aanbevolen)** — `rough` bestaat al; *perf:* + ~0,03–0,08 in fjorden.
2. **Alleen hoger plateau (rise 32 → 50) en diepere geulen** — klein, dramatischer, zelfde vorm.
3. **Losse rotspieken/zuilen in de fjorden (zeestapels)** — 3D, middel-hoog.
4. **Zo laten.**

**A2-4 — Lagune en lotus?** (header: `Lagune`)
1. **Zo laten; reliëf zit daar in de zandbanken en de aankleding (aanbevolen)** — groep A (decor) doet zandbanken,
   kristalspitsen en eilandjes.
2. **Rotseilandjes en zeestapels (tidestone, 5–20 boven water)** — terrein, 3D, middel.
3. **Hogere atollen met lagunemeertjes erin** — mooi, maar meertjes onder Y 63 zijn gewoon zee (vlakke zee).

### A.6 Werkwijze-vraag (kan in ronde A2 of A3)

**A-W — Hoe afstemmen?** (header: `Aanpak`)
1. **Proefstuk op één biome (`peat_moors`) met contactbladen, dan de rest (aanbevolen)** — zoals de decor-proef.
2. **Alle biomes in één change, één afstemronde.**
3. **Eerst een terreinkaart (`/ca terrainmap`) vóór/na in plaats van in-game beelden.**

---

## B. Zwevende eilanden — `worldgen`

### B.1 Probleem zoals vastgelegd

- `teststop-5b.md` §5.1: *"de zwevende eilanden zijn klein en ver uit elkaar, met dunne ranken; het grote eiland
  heeft een kale, donkere onderkant;"* en als richting: *"meer eilanden en vulling in de lucht"*.
- `backlog.md` r. 372: *"[worldgen] Zwevende eilanden klein en ver uit elkaar, dunne ranken, kale donkere onderkant op
  het grote eiland: meer/grotere eilanden en een begroeide onderkant. Eerst sparren; raakt `IslandField`, determinisme
  en perf."*
- `backlog.md` r. 405 (decor C): *"De 'kale donkere onderkant' is in de mythische band aangekleed; meer/grotere
  eilanden blijft open."* en r. 404: *"de onderkant van de eilanden boven de bedorven band (`withered_gardens`) blijft
  kaal; aankleden in de ronde van `withered_gardens`."*
- Het gaat dus om de **zwevende eilanden boven het land** (niet om eilanden in zee): aantal, grootte, afstand en de
  verbindingen (ranken). De onderkant is in de mythische band opgelost (decor C), in de bedorven band nog niet.

### B.2 Huidige implementatie

- **Code:** `IslandPlacement.java` (plaatsing per cel en details), `IslandShape.java` (vorm, gat), `IslandField.java`
  (raster `Settings.cellSize` 128, `MAX_ISLANDS` 8 per kolom, `MAX_REACH` 110, `FALLS_PER_CHUNK` 3), `IslandLandmarks`
  (wereldbomen, hemelmeren, eilandparen/rotsbogen), features `island_vines` (ranken), `island_underside`,
  `island_dressing`, `island_glow`, `island_lifts`; blikvangers `SkyLandmarks` (decor C).
- **Aantal per cel van 128²:** verwacht `0,25 + 3,25·p^1,5` (p = 0 binnenrand … 1 buitenrand van ring 4), max 5;
  overlappende kandidaten van buurcellen vallen weg (`kept`, `OUTRANK`: soort, grootte, positie). Alleen in ring 4
  zelf (niet in de zones; eiland + gat binnen één cel).
- **Soort en maat** (diameters): reus alleen bij p > 0,3 met kans 1,5 % + 6 %·p (80–150); middel 40 % + 20 %·p
  (20 + 40·s², dus scheef naar klein: mediaan ~30); de rest kluit (5–15). Dikte = diameter × 0,45/0,55/0,7 ÷ 0,9.
- **Hoogte:** bodem ≥ hoogste grond onder de voetafdruk + `MIN_CLEARANCE` 15; extra speling binnen 0–25, buiten
  0–185 (`lerp(p^1,3)`), top ≤ 290; hoogte-biome `sky_gardens`/`withered_gardens` bij top ≥ 140.
- **Gat:** elk eiland heeft een gespiegeld gat (×1,12 breder, diepte = kluitdikte) vol water (zeeniveau, of een
  meer boven zee op hoog land); lage gaten krijgen een uitloop naar open water.
- **Grove dekking** (schatting uit de formules, vóór de overlapregel): binnenrand ~0,35 eilanden per cel, vrijwel
  alleen kluiten en kleine middel → **~1 % van de lucht**; midden van de ring ~1,4 per cel → **~8 %**; buitenrand ~3 per
  cel met reuzen → **~20 %**. De gaten liggen ~1,25× zo groot op de grond. Dat verklaart "klein en ver": in de
  binnenste helft is vrijwel elk eiland een kluit of een klein middeleiland, met honderden blokken lucht ertussen,
  en ze hangen tot ~100+ hoog.
- **Verbindingen:** ranken (`island_vines`) van `mangrove_roots` tussen eilandparen zonder boog (40 % dubbel,
  1 blok breed); rotsboog bij ~1 op 2 geschikte paren (6–32 uit elkaar; in de testwig 8 op 190 paren); kettingen en
  bruggen zijn structures (fase 3, backlog r. 31).
- **Wereldbomen** (15) hebben hun eigen boomeiland (52–68) en vallen erbuiten.

### B.3 Randvoorwaarden specifiek

- **Gat onder elk eiland** (kernbesluit 2026-10-10): meer of grotere eilanden = meer en grotere gatmeren in de grond.
  Bij ~2× de huidige dekking ligt in de buitenband ~40–50 % van de grond in gaten: de water-/landverhouding en de
  aankleding op de grond veranderen mee.
- **Cel 128 en `groundReach ≤ cell`:** eilanden groter dan ~150 of dichter dan 5 per cel vragen een groter raster
  (192/256) of meerlaagse cellen: een grotere ingreep in `IslandField` (caches, `Decisions`, overlapregel over
  `NEIGHBOUR_CELLS`).
- **Kolomcap** `MAX_ISLANDS` 8: gestapelde eilanden (6 blokken tussenruimte) passen, maar dichte stapels kosten per blok.
- **Liften/vallen:** meer middel-/reuzeneilanden = meer liften en vallen; valcap 3 per chunk kan dan vaker knellen
  (vallen vallen weg, liften blijven ≥ 6 van een val).
- **Perf:** eilandkolommen worden per blok gesneden (`thin`), plus aankleding onder/op elk eiland. Verdubbelen van de
  dekking kost naar schatting + 0,1–0,2× vanilla in de buitenband (meten met `perf.sh` op `tds`, `skyw*`).
- **Determinisme:** plaatsing is puur per cel; meer eilanden verandert niets aan het principe, wel aan alle
  afgeleide plekken (liften, vallen, blikvangers, aankleding).

### B.4 Vragen — ronde B1 (dichtheid en maat)

**B1-1 — Hoeveel meer eilanden?** (header: `Aantal`)
1. **Binnenrand duidelijk meer, gradiënt blijft (aanbevolen)** — bv. `0,75 + 3·p^1,2` per cel (binnen ~3 % lucht,
   midden ~12 %, buiten ~22 %): je ziet overal eilanden, naar buiten nog steeds dichter (ontwerp). *Complexiteit:*
   klein (getallen). *Perf:* + ~0,05–0,1. *Gevolg:* meer gatmeren in de mythische band.
2. **Overal ~2× zo veel** — veel "vulling", buitenband ~40 %; gatmeren domineren daar de grond. Hogere perf-kosten.
3. **Clusters/archipels:** eilanden in groepen (3–8 rond een groot eiland) via een clusterruis zoals de bosjes, met
   lege lucht tussen de groepen — "vol" waar je kijkt, zonder dat de hele ring vol gaten zit. *Complexiteit:* middel
   (kandidaten per cel via ruis wegen, zoals `ClusterDensity`).
4. **Zo laten; alleen groter maken (zie B1-2).**

**B1-2 — Maatverdeling?** (header: `Maat`)
1. **Minder kluiten, grotere middeleilanden, reuzen al vanaf het midden (aanbevolen)** — middel 25–70 zonder
   scheefheid naar klein, reuzen vanaf p > 0,15 met 4 % + 8 %·p; kluiten vooral als satellieten rond grotere eilanden.
   *Complexiteit:* klein-middel. Blijft binnen cel 128.
2. **Alleen de verhouding kluit/middel draaien (60/40 → 30/70), maten gelijk** — klein, merkbaar.
3. **Echte megaeilanden (150–250) als landmarks** — vraagt een groter raster of aparte landmark-plaatsing (zoals de
   wereldbomen); groot werk, veel water eronder.
4. **Zo laten.**

**B1-3 — Hoogte en afstand?** (header: `Hoogte`)
1. **Lager en gelaagd: in de binnenste helft 15–60 boven de grond, soms twee lagen boven elkaar (aanbevolen)** —
   eilanden zijn dichtbij en "doemen op", stapels geven diepte. *Gevolg:* minder eilanden ≥ 140 binnen (minder
   `sky_gardens` aan de binnenrand), meer kolommen met 2+ eilanden (kosten per blok).
2. **Zoals nu (binnen laag, buiten tot ~250), alleen meer/groter.**
3. **Breder spreiden: elke hoogte overal** — chaotischer beeld, verliest het "naar buiten hoger".

**B1-4 — Wat gebeurt er met het gat onder de extra eilanden?** (header: `Gaten`)
1. **Elk eiland houdt zijn gat (aanbevolen, kernbesluit)** — consequent, meer meren; afstemmen op het water-aandeel.
2. **Kluiten (5–15) zonder gat of met een ondiepe "kras"** — minder swiss-cheese bij clusters van satellieten;
   breekt het kernbesluit voor de kleinste eilanden.
3. **Hoge eilanden (bodem > 60 boven de grond) met een ondieper gat (½ dikte)** — minder diepe gaten in de buitenband.

### B.5 Vragen — ronde B2 (verbindingen en onderkant)

**B2-1 — Ranken "te dun"?** (header: `Ranken`)
1. **Dikkere wortelbruggen: 2–3 breed, doorhangend, met mos/gloeiwortels en soms een kluit halverwege
   (aanbevolen)** — beloopbaar, lezen van ver. *Complexiteit:* middel (`island_vines`, determinisme zoals de huidige
   ranken; backlog r. 367: ranken trekken nog uit de chunk-random na hun luchtcheck, die rest meteen oplossen).
2. **Meer ranken (ook tussen verder uit elkaar liggende paren, tot ~40) maar dun** — klein.
3. **Meer rotsbogen (alle geschikte paren i.p.v. ~1 op 2)** — terrein; boog blijft max. één per eiland.
4. **Zo laten; bruggen en kettingen komen als structures in fase 3.**

**B2-2 — Onderkant van de bedorven eilanden (`withered_gardens`)?** (header: `Onderkant`)
1. **Mee in deze change: hangende dode wortels, grijs mos, gifkristal-knoppen (aanbevolen)** — sluit backlog
   r. 404 meteen af; hergebruikt `island_dressing`.
2. **Apart, in de ronde van `withered_gardens`** (zoals nu gepland).
3. **Kaal laten als contrast ("dood").**

**B2-3 — "Vulling in de lucht" naast eilanden?** (header: `Vulling`, meerkeuze)
1. **Kleine zwevende brokken/puin rond grote eilanden (1–4 blokken, terrein of grondobject) (aanbevolen)** — goedkoop,
   veel effect van ver.
2. **Watervallen die als nevel eindigen (geen water tot de grond)** — botst met de waterscanregel (vallen moeten in
   water landen); alleen als particle-effect (atmosphere) haalbaar.
3. **Hangende wortelgordijnen tot vlak boven de grond onder lage eilanden** — al deels (klimstrengen ≤ 40); meer en
   langer.
4. **Niets extra.**

---

## C. Lucht en mist — `atmosphere`

### C.1 Probleem zoals vastgelegd

- `teststop-5b.md` §5.1: *"de lucht is vlak wit en grijs, en de mist en sfeer voegen weinig toe."*
- `backlog.md` r. 373: *"[atmosphere] Ring 4-lucht vlak wit/grijs, mist en sfeer voegen weinig toe: lucht, mist en
  ambient herzien. Eerst sparren."*
- Ontwerp (`fase-1-wereld.md` §Ring 4 "Lucht en kleur"): *"mythisch helder turquoise, zachte witte fog, roze/gouden
  schemer; `mist_fjords` een eigen grijswitte mist; de bedorven band gifgroen (felgroen water, groenige lucht); zone
  4→5 loopt leeg naar grijs."*
- Nog open uit 5b: backlog r. 270 (*"Ring 4-weer in-game afstemmen: kleuren, fog-afstanden, volumes …"*) en r. 339
  (volumes, dichtheden, grijsfade, pulsering).

Wat de screenshot laat zien: bovenin licht turquoise, maar het grootste deel van het beeld (horizon) is egaal
witgrijs: de fog-kleur van de biomes ligt bijna op wit (`EAF4F2`, `F0F2EE`, `F2F8F8`) en vanilla mengt de lucht naar
die kleur aan de horizon. De vanilla-wolkenlaag (Y 192) snijdt dwars door het grote eiland.

### C.2 Huidige implementatie

- **Biome-kleuren** (datagen, `worldgen/biome/Ring4Atmosphere.java`, palet = lucht / fog / water / waterfog):

| Biome | Lucht | Fog | Opmerking |
|---|---|---|---|
| `cascade_vale` | `72D2E3` | `EAF4F2` | bloesem-particle 0,0012 |
| `crystal_lagoon` | `6CD6E8` | `E4F6F4` | |
| `lotus_shallows` | `78D4E4` | `F0F2EE` | |
| `glowroot_swamp` | `6FC9D6` | `DCEBE2` | |
| `mist_fjords` | `9FB8C4` | `C8D0D4` | "lage mist" = alleen particle `waterfall_mist` 0,001 |
| `sky_gardens` | `66D0EE` | `F2F8F8` | |
| bedorven (`peat_moors`) | `9CB08A` | `B4BE98` | `drowned_forest` `8FAE8A`/`A9BB98`, `reed_marsh` `96B28E`/`AEC09C` |
| `withered_gardens` | `A2B496` | `BCC4A8` | |
| `sundered_reach` | `98A88E` | `ACB49C` | + client-grijsfade tot 0,9 |
| `verdant_falls` | `86D6E0` | `D2F0E4` | |

- **Client:** `client/mixin/ClientLevelSkyColorMixin` (enige haak op de luchtkleur en de wolkenkleur in 1.21.1),
  `client/weather/RingWeatherFog` (fog-afstand en -kleur naar een doel), `DenseFogRenderer` (dikke mist: fog tot 32,
  kleur `D9DFE4`, lucht 80 % naar de mist), `SunderedFade` (grijsfade zone 4→5), `Ring4Ambience` (vogels/koor/gloed
  dag-nacht, geritsel, silhouet, glitch), `NetherRing4Fog` (pulserende fog, voorbeeld van een biome-gestuurde
  fog-hook), `AshClouds` (wolkenkleur ring 3, voorbeeld van een wolkenhaak).
- **Weer (server, gesynct):** `RingWeather` met één cyclus voor ring 4: helder 600–9000 s, weer 600–1200 s, mist /
  storm / gifregen elk gewicht 1; mist en storm alleen mythisch, gifregen alleen bedorven. Mist is dus **alleen** te
  zien tijdens een weerperiode; buiten het weer is er geen extra fog, ook niet in `mist_fjords`.
- **Niet gebouwd:** de roze/gouden schemer (vanilla zonsopkomst-kleur), een permanente lage mist, eigen wolken.
- **Dimensie:** de overworld gebruikt `minecraft:overworld`-effects (wolken op Y 192, vanilla lucht- en
  sterrenrender); geen eigen `DimensionSpecialEffects`.

### C.3 Randvoorwaarden specifiek

- Lucht/fog-rendering alleen in `client/` (mixins in de `client`-sectie van de mixin-config, events op de game-bus
  vanuit `Dist.CLIENT`). Wat alleen van biome, tijd, positie en ring-layout afhangt hoeft niet gesynct te worden (die
  kent de client al). Een **nieuwe weerstaat** (bv. ochtendmist) gaat via `RingWeather` + `RingWeatherPayload` en
  vraagt een protocolverhoging (`"8"` → `"9"`) en config (`[weather.ring4]`).
- Sodium/Iris: de fog-events en de sky-colour-mixin werken met Sodium; Iris-shaders overschrijven fog en lucht (eerder
  besluit: geen risico nemen met flikkerende lucht). Een eigen sky-renderer (eigen `DimensionSpecialEffects` voor de
  overworld) raakt het world preset en is het grootste risico.
- Biome-kleuren wisselen per biome (vanilla mengt ze over een paar blokken); per-band of per-hoogte-effecten
  (eilanden ≥ 140, mist in dalen) vragen een client-hook zoals `SunderedFade`.
- Kosten: fog/lucht-hooks zijn per frame en goedkoop; particles kosten per tick en per client (instelling
  "Particles"); geen worldgen-kosten.

### C.4 Vragen — ronde C1 (lucht en kleur)

**C1-1 — Luchtkleur overdag?** (header: `Lucht`)
1. **Diepere, verzadigde lucht en een getinte fog (aanbevolen)** — bv. mythisch lucht ~`4FC3E8`, fog licht aqua/
   pastel (~`BFE8EC`) i.p.v. bijna wit; bedorven fog duidelijk zieklijk geelgroen. Alleen biome-data (datagen),
   geen code, geen sync. *Gevolg:* de horizon leest gekleurd; verre eilanden verdwijnen in een kleur i.p.v. wit.
2. **Zoals 1, plus een hoogtegradiënt via de client-hook** — boven Y ~140 (bij de hemeleilanden) helderder en
   blauwer, laag bij het water zachter. *Complexiteit:* klein-middel (`ClientLevelSkyColorMixin` + fog-event).
3. **Eigen lucht-renderer** (gradiënt, eigen zon/maan, sterrenvelden) — eigen `DimensionSpecialEffects` voor de
   overworld; groot, raakt het preset, risico met Iris.
4. **Kleuren zo laten, alleen fog-afstand aanpassen** (zie C2).

**C1-2 — De roze/gouden schemer uit het ontwerp?** (header: `Schemer`)
1. **Ja: zonsop- en -ondergang roze/goud in de mythische band, groenig-vaal in de bedorven band (aanbevolen)** —
   client-mixin op de vanilla zonsopkomst-kleur en de lucht/fog-kleur rond schemer; geen sync (tijd is vanilla
   gesynct). *Complexiteit:* klein-middel.
2. **Alleen roze/gouden wolken bij schemer** (wolkenkleur via de bestaande haak, zoals `AshClouds`) — klein.
3. **Nee, vanilla schemer.**

**C1-3 — Wolken?** (header: `Wolken`)
1. **Wolkenlaag in ring 4 hoger leggen (boven de eilanden, ~Y 300) of dunner maken, en licht tinten (aanbevolen)** —
   de vanilla-laag op Y 192 snijdt nu door de hoge eilanden. Client-mixin op de wolkenhoogte, geleidelijk over de
   zones (per camerapositie, zoals `SunderedFade`). *Complexiteit:* klein-middel; met Sodium testen.
2. **Wolken in ring 4 uitzetten** — strak beeld, minder "lucht".
3. **Zo laten.**

**C1-4 — Bedorven band: hoe "ziek" mag de lucht?** (header: `Bedorven`)
1. **Duidelijk geelgroen waas, dichter richting ring 5 (aanbevolen)** — fog-kleur en fog-afstand per ringpositie
   (client-hook op de layout-blend, zoals de grijsfade). Sluit aan op "gevaren dichter richting ring 5".
2. **Alleen sterkere biome-kleuren (data)** — klein.
3. **Zo laten (grijsgroen).**

### C.5 Vragen — ronde C2 (mist)

**C2-1 — Permanente mist buiten het weer?** (header: `Mist`)
1. **Lage grondmist in `mist_fjords` en boven moeras/veen: fog dichterbij als de camera laag in een dal of boven
   water staat, open zicht vanaf de plateaus en eilanden (aanbevolen)** — client-hook (biome + camerahoogte t.o.v.
   zee), geen sync. *Speelgevoel:* fjorden "de mist in" zoals ontworpen, eilanden doemen op.
2. **Mistbanken als particles** (laaghangende `drifting_haze`-lagen over water en in dalen) — echte "zichtbare" mist,
   kost per client particles; afstellen op "Fast"/"Fancy".
3. **Ochtendmist als weerstaat** (server-side in `RingWeather`, bij zonsopkomst in de mythische band, gesynct) —
   ook bruikbaar voor de mist-hook van fase 4 (wezens tijdens mist); protocol `"9"`, config.
4. **Alleen de dikke mist (weer) vaker** (gewicht/frequentie in `[weather.ring4]`) — config, geen code.

**C2-2 — Zicht in helder weer?** (header: `Zicht`)
1. **Lichte nevel op afstand (fog-start iets dichterbij, gekleurd) zodat verre eilanden blauwig wegvallen
   (aanbevolen)** — luchtperspectief: diepte en "mythisch"; client-hook. Hangt samen met C1-1.
2. **Juist helder en ver (fog zo ver mogelijk)** — alles scherp, maar ook de lege stukken.
3. **Vanilla.**

**C2-3 — Nacht?** (header: `Nacht`)
1. **Zo laten (nachtgloed, vuurvliegjes, koor bestaan al); eerst in-game beoordelen (aanbevolen)** — backlog r. 339.
2. **Diepblauwe nachtlucht met meer sterren en een maanglans op het water** — eigen render (sterren/maan via
   mixin of eigen effects); middel-groot.
3. **Lichtende nevel boven het water 's nachts** (particles, alleen mythisch) — klein-middel.

**C2-4 — Wat meten we als "klaar"?** (header: `Toets`)
1. **Vaste camera-posities per biome op dag/schemer/nacht in contactbladen, daarna jij live (aanbevolen)** — zoals de
   decor-lus; screenshots via F2 (PrintWindow geeft na ~20:50 witte beelden, backlog r. 385).
2. **Alleen live vliegen.**

---

## D. Afhankelijkheden en volgorde

1. **Decor-uitrol eerst afronden** (A water-mythisch, B besmet, C hoogte, D overgang, de nieuwe blokken inhangen):
   die agents werken nu in `Ring4Features.ORDER`, de biome-generatielijsten en de decor-features. A en B veranderen
   het terrein waar al die aankleding op staat; beginnen vóór de merge geeft dubbele afstemrondes.
2. **A (reliëf) vóór B (eilanden)** *(voorstel)*: eilandhoogte, gaten, gatmeren boven zee, uitlopen, liften en vallen
   rekenen vanaf de grond (`baseHeight`, hoogste grond onder de voetafdruk). B afstemmen op terrein dat daarna nog
   verandert kost een tweede ronde. Beide raken `IslandField`/`IslandPlacement`/`Ring4Relief`: **niet parallel**.
   Alternatief als de gebruiker de eilanden belangrijker vindt: B eerst, A daarna met een korte B-controle.
3. **C (atmosphere) kan parallel** met A en B: andere bestanden (`Ring4Atmosphere`, `client/*`). Enige overlap: de
   gegenereerde biome-JSON (kleuren én featurelijsten in hetzelfde bestand) → na elke merge `runData` opnieuw draaien
   en `src/generated` meenemen. Wel inhoudelijk verbonden: de fog-kleur/-afstand bepaalt hoe ver je eilanden ziet
   (B) en hoe het reliëf van ver leest (A); C-beelden dus bij voorkeur na A/B nog één keer bekijken.
4. **Na A en B:** één decor-hercheck (dichtheden op het nieuwe terrein, `withered_gardens` + bedorven onderkant als
   dat niet in B zit), dan de formele metingen (`det.sh`, `perf.sh` met de eilandrijke gebieden `tds`/`skyw*`,
   `waterscan.sh`), en dan de rest van teststop 5b (checklist §3, §4).
5. **Open backlog die meeloopt:** ranken/riet/reuzenleliebladen-determinisme (r. 367/405) hoort bij B2-1; bomen in
   valkolommen (`fall_clearance`, laatste regel backlog, r. 406) wordt vaker bij meer eilandvallen (B); de kluitvallen halen
   ~17 % (r. 403), relevant als er meer kluiten bijkomen.

**Voorstel voor de drie sessies:** sessie 1 = A (rondes A1, A2 + werkwijze), sessie 2 = B (B1, B2), sessie 3 = C (C1,
C2) en tot slot één vraag over de volgorde (D) als die nog open is.
