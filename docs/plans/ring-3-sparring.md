# Ring 3 — sparringvoorbereiding (vulkanische ring + Nether-ring 3)

Scope: fase 1, blok E van het bouwplan (changes 38–57). **Status: sparring afgerond (2026-10-06)**; de antwoorden staan direct hieronder en zijn verwerkt in `fase-1-wereld.md` (Ring 3 → "Uitwerking sparring ring 3", bouwplan E), `00-overzicht.md` en `02-beslissingenlog.md`. Vervolg: `docs/plans/ring-3/contract.md` en `docs/plans/ring-3/waves.md`.

## Antwoorden van de gebruiker (2026-10-06, definitief)

De antwoorden zijn genummerd 1–22, met vraag 13 (fantasy-biome) en 22 (orkestratie) als nieuwe vragen. Daardoor schuift de nummering ten opzichte van §4 vanaf 13 een plaats op. De beslissingenlog gebruikt de nummering hieronder.

| # | Onderwerp (§4-vraag) | Antwoord |
|---|---|---|
| 1 | Richting (1) | **A+**: Smeulend Vuurland + zwevende eilanden/lavavallen uit B + dag/nacht-ritme uit C |
| 2 | Fantasy (2) | **Hoog**: reuzenskeletten, zielvuur, titanen-rotsen erbij |
| 3 | Wow-moment (3) | **A**: horizon-vulkanen boven de wolken + lucht slaat om op de grens |
| 4 | Overlevingskern (4) | **A**: hitte-ritme + schaarste |
| 5 | Sub-biomes (5) | 5 huidige + `ember_woods` + één fantasy-biome = **7** |
| 6 | Terrein (6) | lavarivieren + lavavallen, zwevende basalteilanden, caldera's (deels slapend/groen); **geen** basaltzuilvelden |
| 7 | Oase (7) | **A**: slapende kraters |
| 8 | Breedte (8) | **A**: ring 3 3.500–6.000, ring 4 6.000–7.800, ring 5 7.800–9.500 (Nether-3 625 breed) |
| 9 | Hitte (9) | **A**: 0→100 ~3 min, ×1,5–2 bij bronnen/pieken, drempels 25/50/75/100 |
| 10 | Weer (10) | **A**: asregen + uitbarsting + zeldzame aswolk |
| 11 | Lavabommen (11) | **A**: ringbreed rond spelers onder open lucht, dichter bij de pieken |
| 12 | Voedsel/water (12) | **B (afwijkend)**: zeldzaam vee (geiten op de pieken); waterschaarste en eetbare vuurflora blijven tenzij dat botst → vervolgvraag in `contract.md` |
| 13 | Fantasy-biome (nieuw) | titanenveld met zielvuur rond de skeletten; skeletten als worldgen-features, geen structures/loot |
| 14 | Ertsen (13) | **A**: ontwerp + vuurkristal-geodes |
| 15 | Nether-3 (14) | **A**: ontwerp + levendig + eigen wow (bevroren lavavallen, ijspegels, warme scheur-eilandjes) |
| 16 | Prestatie (15) | **A**: ≤ 1,75× vanilla |
| 17 | Horror (16) | **B**: houden + 's nachts gloeiende afgietsels |
| 18 | Methodiek (17) | **A**: contract-first in golven, worktrees, één integratie-agent |
| 19 | Teststops (18) | **A**: drie (4a terrein-wow blokkerend, 4b overleving + sfeer, 4c eindcheck incl. Nether-3) |
| 20 | Textures (19) | **A**: direct goede eerste versies in 37b-stijl |
| 21 | Nummering (20) | **A**: 38–57 blijven, nieuwe met letter |
| 22 | Orkestratie (nieuw) | handmatig door de hoofdsessie met agents, **geen** Workflow-tool (§5.7 punt 6 vervalt) |
Dit document is de input voor de sparringsessie. De uitkomst gaat daarna naar `docs/design/fase-1-wereld.md` (ring 3-sectie en bouwplan blok E) en `docs/design/02-beslissingenlog.md`.

## 0. Uitgangspunt

**Visie van de gebruiker (letterlijk, 2026-10-06):**
> "Ik wil dat ring 3 (vulkanische gedeelte) wel echt een gave ring wordt ondanks het vulkanische thema, waarin je echt het avontuur/overleef-element terug ziet komen. Deze 'midden'-ring moet ook het langste duren om in te overleven, wat de lastigste ring technisch is. Wellicht nog wat fantasy-elementen erin gooien en echt zorgen dat als je de ring inloopt je denkt 'wow'."

**Les uit ring 2** (teststop 2b, `docs/plans/teststop-2b-feedback.md`): de wereld moet **levendig** aanvoelen, niet kaal of leeg. Dat is de norm. Ring 2 werd pas goed na twee extra rondes (30b/30c). Voor ring 3 nemen we "levendig" daarom vanaf het eerste plan mee: flora, beweging, licht, geluid en details per biome.

Wat dit concreet vraagt:
1. **Een wow-moment bij de grens.** Dat ontwerpen we bewust, niet als bijproduct.
2. **Vulkanisch + fantasy.** Geologie die net niet kan bestaan, zonder dat het een sprookjesbos wordt.
3. **De langste en lastigste overleving.** In fase 1 kunnen we daarvoor alleen omgeving, terrein, schaarste, condities en weer gebruiken. Mobs, gear en structures volgen later.
4. **Levendig**, ondanks as en lava.

---

## 1. Huidige stand

### 1.1 Al besloten ontwerp (sessie 2 fase 1, 2026-09-30)
Bron: `docs/design/fase-1-wereld.md` §2 "Ring 3" (r. 504–579); beslissingenlog r. 57–70.

| Onderdeel | Besloten |
|---|---|
| Terrein en sfeer | Zwart-grijs aslandschap: kegels, kraters, lavameren, gestolde stromen; lucht rood-oranje, fog dicht asgrijs; geen natuurlijk water, lege zeekom |
| Biomes (5) | `ashen_wastes`, `volcanic_peaks` (kern), `dried_seabed`, `basalt_flows`, `sulfur_fields`; overgang 3→4 `steam_fens` |
| Blokken | vanilla basalt/blackstone/magma als basis + as (blok + laag), puimsteen-set, smeulsteen (licht, hitte), verkoold hout-set, zwavelkorst, fumarole, kokende modder, geiser |
| Ertsen | vulkaniet (diep bij lava), zwavel (ondiep), vuurrobijn (magmakamers); vanilla verder afgebouwd |
| Grotten | lavabuizen (reisroute), magmakamers |
| Water | geplaatst water verdampt bovengronds (lucht zichtbaar); ondergronds normaal |
| Weer | asregen (zicht, hitte ×1,5), uitbarsting (rode lucht, gerommel, lavabommen = vanilla small fireballs, `mobGriefing`, hitte ×2) |
| Conditie hitte | ↑ bovengronds overdag, bij hittebronnen (~4 blokken), in `volcanic_peaks`, tijdens weer; ↓ 's nachts, ondergronds, in water; fire resistance halveert; drempels traagheid → zwakte → in brand |
| Horror (fase 1) | as-afgietsels, voetsporen in de as, gerommel + camera-shake (client-only) |
| Grensmuur 3→4 | zwarte aswolk met vonken (fase 5) |
| Nether-ring 3 | bevroren Nether: `frozen_lava_sea`, `rime_forest`, `glacier_rifts`; koudlava-set, Nether-ijs, rijp-netherrack, rijpzwam-set, ijsstalactieten, gloeiende scheur; sleutelmateriaal **oerijs** (kern van ijspilaren, silk touch); conditie **kou** (leren armor remt, top = vanilla `ticksFrozen`); ruimte-check, zo nodig ring 3 verbreden via config |

Alles hierboven mag in de sparring opnieuw ter discussie. De visie van de gebruiker vraagt meer wow, meer fantasy en meer overlevingsdruk dan het huidige ontwerp.

### 1.2 Wat al gebouwd is en ring 3 raakt

- **Ringgrenzen:** de standaardwaarden staan in `config/CommonConfig.java`: buitenradii 1.500 / 3.500 / 5.500 / 7.500 / 9.500, void 10.000, grensruis ±96, overgangszone 128. Per wereld komt er een snapshot in `<wereld>/corruptedadventure/ring_settings.json` (§1.3). Ring 3 is 2.000 blokken breed, ~56 mln m² (1,8× ring 2). Nether-ring 3 loopt van 875 tot 1.375 (500 breed). Een andere breedte geldt alleen voor nieuwe werelden. `/ca tpring` en `tpboundary` rekenen met de snapshot.
- **Placeholder-terrein:**
  - `worldgen/density_function/ring_3/overworld.json`: gradiënt rond Y ~120 + ruis, plus sinds change 31 de beddingsnede `−1/64 × ring_2/overworld/riverbed_cut`.
  - `ring_3/nether.json`: vloer Y ~55, dak Y ~110.
  - Placeholder-biomes `placeholder_ring_3` en `placeholder_nether_ring_3` (`worldgen/biome/PlaceholderBiomes.java`).
  - Het router-klimaat van ring 3 is 0 (`ring_blend/overworld_temperature`/`_vegetation`, §1.2). Floodedness en erosion zijn vanilla; change 42 kiest die zelf (backlog r. 89). `vein_toggle` = 0.
- **Overgang 2→3 `dry_riverbeds` (change 31):**
  - Bedding in beide invoeren; het masker dooft uit bij ring 3-gewicht 0,8–0,98 (`ring_weight/overworld/ring_3.json`).
  - Geblakerde vlekken van vanilla `blackstone`/`basalt` als placeholder. Change 39 vervangt ze door as en verkoold hout en herziet de snede in `ring_3/overworld.json` (backlog r. 122).
  - Ring 3-ertsen ook in de ring 3-helft van de zone (backlog r. 123).
- **Vloeistof:** `worldgen/chunk/RingFluidPicker.java` heeft de regels per ring: `noFluid` (void) en `waterOnly` (ring 2). Ring 3 krijgt hier zijn eigen regel: geen water, lava waar het ontwerp dat vraagt. Voor de Nether liggen er al `fluid_levels` per ring (preset), de `ring_edge`-dam en de carver `ring_nether_cave`. Nether-3 kan die hergebruiken met een eigen lavaniveau of zonder lava (CODEBASE valkuilen "Lavazee per ring", "Nether-carver zet zelf lava").
- **Prestatie:**
  - De ring gate werkt; de ring 3-placeholder kost 0,92× vanilla.
  - Budget ring 2: noise-stap ≤ 1,75× vanilla (oase 1,72–1,74×).
  - Nether-ring 2 ~1,95×, geaccepteerd (35-12 A).
  - Meetmethode (JFR, 16×16 chunks) en de valkuilen voor fijne vormen (interpolatie 4×8×4, geen `ThreadLocal` per DF-instantie, `ColumnTable`, `core_mask`) staan in `CODEBASE.md`.
- **Terrein-bouwstenen uit ring 2:**
  - `CanyonDetails` (rotsvormen per kolom: hoodoos, bogen, naalden);
  - `polygon_edge` (het zeshoekige patroon van 30b; bruikbaar voor basaltzuilen);
  - `noise_band` (groepjes);
  - de determinismeregels voor features (23c, en 35 voor de Nether);
  - de zwevende eilanden van de void-invoer (`void/overworld.json`, §1.8): een voorbeeld voor eilanden in een hoogteband.
- **Ertsen:** `RingVanillaOres` + biome modifier `add_ring_<n>_ores` (§1.11). De factoren voor ring 3 zijn nog open; ring 2 heeft ×0,6/0,5/0,25.
- **Condities (change 19, 27):**
  - De haken staan in `CODEBASE.md` ("Meter-systeem: haken"). Sjabloon: `condition/DehydrationRule.java`. `ConditionContext` kent `isSunUp`, `canSeeSky` en `isSubmerged`.
  - Bij 43 vastleggen in welke volgorde de modifiers draaien (backlog r. 95). Eigen damage type per conditie.
  - Vandaag geldt uitdroging alleen in ring 2. Buiten ring 2 zakt de meter met 0,5/s.
- **Weer (change 20, 28):**
  - `weather/RingWeatherType.java` heeft al `ASH_RAIN` en `ERUPTION` (ring 3, overworld), nog zonder effect.
  - Patroon: server-modifier (`HeatWaveDehydrationModifier`) + client-renderer (`client/weather/*Renderer`) + post-shader (`shaders/post/heat_haze.json`) + payload `wind_gust`. Een geiser-duw kan dezelfde payload gebruiken.
  - Lucht- en neerslagkleur hebben in NeoForge 1.21.1 geen event; de eerste change die ze nodig heeft, kiest de aanpak (§1.6). Dat is change 45 (rode lucht bij een uitbarsting).
- **Sfeer en assets:**
  - `worldgen/biome/BiomeAmbience.java` (biome-effecten).
  - Synthesescript `tools/sounds/Ring2Sounds.java`, sjabloon voor change 50.
  - Texturescript per ring: `Ring3Textures.java` wordt het derde. Ontdubbelen van de helpers staat in de backlog (r. 48, 151, 166). `tools/textures/VanillaCompare.java`.
  - De stijlregels van §1.4 gelden, inclusief 37b: selectieve omlijning, 5–7 tonen, vlakheidscontrole.
- **Debug:** `/ca tpring 3 [nether]`, `/ca tpboundary 2|3 [nether]`, `/ca weather 3 ash_rain|eruption`, `/ca condition …`. Een debugcommando voor terreinkaarten staat in de backlog (r. 71: PNG van hoogte, biome en doorsnede); dat wordt waardevol als we vormen vroeg willen beoordelen.

### 1.3 Wat in latere fases hoort (niet in deze ring 3-ronde)
- **Fase 2 `items`:** vulkaniet-gear-tier, zwavel → buskruit/vuur, vuurrobijn, hittebescherming op basis van dauwkristal, oerijs → zuiver water, voedsel uit ring 3-flora.
- **Fase 3 `structures`:** wrakken en havens in `dried_seabed`, verkoolde dorpen, putten, smidsen/ruïnes (fantasy), bewakersstructures 3→4.
- **Fase 4 `mobs`:** aswandelaars, magmawezens in lavabuizen, Nether-3: bevroren ghasts, ijs-strays; ring 3-bewaker.
- **Fase 5 `progression`/`corruption`:** grensmuur 3→4, kompas, lore "gestolen water".

Fase 1 kan wel de **haakjes** leggen: plekken in het terrein waar later een structure of mob logisch past (nissen, kraterbodems, lavabuizen), net zoals de wandnissen in ring 2.

### 1.4 Bouwplan blok E (huidige nummering)
38 blokken as/puimsteen/smeulsteen/verkoold hout · 39 terrein + `ashen_wastes`/`volcanic_peaks` · 40 erts-blokken · 41 ertsplaatsing · 42 lavabuizen + magmakamers · 43 hitte · 44 water verdampt · 45 weer · 46 zwavel/fumarole/modder/geiser · 47 extra biomes · 48 `steam_fens` · 49 horror-features · 50 sfeer · 51 Nether-3-blokken · 52 oerijs · 53 Nether-3-terrein · 54 `glacier_rifts` + ruimte-check · 55 kou · 56 Nether-3-sfeer · 57 textures.
Als de sparring het ontwerp verandert, verandert dit plan mee. Zie §5 voor een indeling in parallelle golven.

---

## 2. Waar "langst en lastigst overleven" in fase 1 vandaan kan komen

| Hefboom | Fase 1 (categorie) | Later |
|---|---|---|
| **Hitte met dag/nacht-ritme** — overdag bovengronds bijna onleefbaar, 's nachts en ondergronds herstel | `conditions` (43) | gear (fase 2) remt |
| **Water gestolen** — verdampt bovengronds; geen natuurlijk water | `conditions` (44), `worldgen` (vloeistofregel) | items: veldfles, zuiver water |
| **Uitdroging óók in ring 3** (optioneel): tweede meter naast hitte | `conditions` | — |
| **Voedselschaarste** — geen vee in ring 3 (biome-spawns), weinig eetbare flora | `worldgen` (spawns), `blocks` (flora) | items (eigen voedsel), mobs |
| **Terrein als obstakel** — lavarivieren, kraterwanden, kliffen, verticaliteit; oversteken kost tijd en materiaal | `worldgen` | structures: bruggen, wegen |
| **Verraderlijke ondergrond** — dunne korst boven lava/lavabuis die instort (hergebruik van het `loose_sand`-mechanisme), kokende modder, geisers | `blocks` | — |
| **Events** — asregen, uitbarsting met lavabommen, optioneel een zeldzame aswolk (pyroclastisch: schuilen of branden) | `atmosphere` (45) | corruption-events |
| **Giftige dampen** in kraters en zwavelvelden (zone-regel, geen gasblok) | `conditions` | gear |
| **Schaarse schuilplaatsen** — lavabuizen, diepe grotten, zeldzame "koele plekken" (zie vraag 7) | `worldgen` | structures |
| **Omvang** — bredere ring (config) | `infra`/config | — |
| **Waardevolle ertsen alleen op gevaarlijke plekken** (vulkaniet bij lava, vuurrobijn in magmakamers) | `worldgen` (41) | items |

Kanttekening: "lang overleven" moet spannend blijven, niet frustrerend. Ring 3 moet in fase 1 **zonder** gear te doen zijn, maar krap. Gear (fase 2) en mobs (fase 4) schuiven de balans later op; de config-waarden blijven bij te stellen.

---

## 3. Drie concept-richtingen

### Concept A — "Smeulend Vuurland": levend vulkanisch land, gematigde fantasy *(aanbevolen als basis)*

**Kern:** een vulkanisch land dat van het vuur leeft. Vuur-aangepaste flora, gloeiende lavarivieren, reuzenvulkanen met caldera's als landmark, en zeldzame koele toevluchtsoorden. Vooral **levendig en gevaarlijk**, niet dood.

- **Wow bij de grens:**
  - Vanuit `dry_riverbeds` (Y ~70) zie je aan de horizon de eerste vulkaankegels. Ze reiken tot Y ~240–280, dus boven de wolkenlaag op Y 192, met een gloeiende kraterrand en een rookpluim (particles).
  - Op de grens kantelt de lucht naar asrood en stijgen vonken op. Het eerste smeulwoud staat er met gloeiende kronen. Een lavarivier slingert door het dal met natuurlijke basaltbruggen erover.
- **Sub-biomes (6):**
  - `ashen_wastes` (kern): asvlakte met aslagen, verkoolde bomen, as-afgietsels.
  - `volcanic_peaks` (kern): kegels met caldera, lavastromen, fumarolen; de heetste plek.
  - `ember_woods` (**nieuw**): smeulwoud van verkoolde stammen met gloeiende askronen, vuurbloemen en askruid. Hier zit het "levendig".
  - `basalt_flows`: gestolde stromen met gloeiende scheuren (smeulsteen), basaltzuilvelden, lavarivieren.
  - `sulfur_fields`: gele korst, fumarolen, zwavelkristallen, giftige damp.
  - `dried_seabed`: lege zeekom, dode koraalbanken; het verhaal van het gestolen water.
  - Overgang 3→4 blijft `steam_fens`.
- **Terrein-signatuur:**
  - Kegels met caldera als eigen celvorm (Java-DF, zoals de hoodoos van change 30); sommige slapend, met een groene kraterbodem (koele plek).
  - Lavarivieren in kanalen (statische lava via de vloeistofregel of de surface).
  - Spaarzame lavavallen aan kraterranden.
  - Basaltzuilvelden (hergebruik `polygon_edge`) en golvende asvlaktes met aslagen.
- **Fantasy-element (gematigd):** gloeiende vuurflora (emissive), smeulbomen, opstijgende vonken, vuurkristal-geodes in magmakamers (gloeiend, decoratief), en hier en daar een **zwevend basalteiland** boven een calderameer als zeldzaam landmark.
- **Overlevingsdruk:** hitte met een sterk dag/nacht-ritme (overdag vooral reizen via lavabuizen en schaduw, 's nachts bovengronds), water verdampt, geen vee, weinig eetbare flora, instortende askorst boven lavabuizen, uitbarstingen. Koele plekken: slapende kraters en diepe grotten.
- **Nether-ring 3:** de bevroren Nether zoals ontworpen, met dezelfde levendig-eis: bevroren lavavallen als ijsvormen, rijpwoud met ijspegels, gloeiende scheuren als warme eilandjes, ijskristal-particles, oerijs in de ijspilaren.
- **Technische risico's (1.21.1):**
  - Lava aan het oppervlak geeft lichtupdates bij generatie en vuur. Daarom: verkoold hout en ring 3-flora niet brandbaar, geen brandbare blokken naast lava.
  - Stromende lava bij generatie plant fluid ticks in. Lavavallen alleen als features met een cap per chunk.
  - Rode lucht bij een uitbarsting heeft geen event (mixin of `DimensionSpecialEffects`).
  - Kegels met caldera vragen een eigen celtype (zelfde aanpak als `CanyonDetails`).
  - Zwevende eilanden alleen zeldzaam (techniek van de void-invoer).
- **Prestatie-inschatting:** kegels als 2D-hoogte + celvorm ≈ het niveau van ring 2 (1,3–1,6× vanilla); zwevende eilanden lokaal duurder. Lavalicht kost iets bij generatie, vergelijkbaar met de vanilla-Nether.
- **Bouwplan-impact:** 39/47 krijgen andere biomes (`ember_woods` erbij). 38/46 krijgen vuurflora en de instortende korst. 49 blijft.

### Concept B — "Rijk der Vuurreuzen": episch, hoge fantasy

**Kern:** schaal en onmogelijke geologie. Kolossale vulkanen tot de bouwlimiet, zwevende obsidiaan- en basalteilanden boven lavazeeën, lavavallen van eiland naar zee, en versteende skeletten van reusachtige vuurwezens.

- **Wow bij de grens:** het land breekt af. Je staat op een klif boven een gloeiende lavavlakte waarboven eilanden zweven, met in het midden een vulkaan die tot de bouwlimiet reikt. Het is een overzichtsmoment zoals bij een canyonrand, maar dan rood.
- **Sub-biomes (6):**
  - `volcanic_peaks`: titanenkegels.
  - `obsidian_skies`: zwevende eilanden boven een lavazee.
  - `lava_delta`: vertakte lavarivieren met eilandjes.
  - `titan_graveyard`: asvlakte met reuzenskeletten van bot en basalt.
  - `crystal_calderas`: kraters met vuurkristal-zuilen.
  - `dried_seabed`: leeg en verbrand.
  - Zwavel wordt een feature in de pieken.
- **Terrein-signatuur:** een lavazee op vast niveau in regio's van de ring (vloeistofregel ring 3), eilanden in een hoogteband (3D-ruis, zoals de void), een steile breukrand vlak achter de overgangszone, kegels tot Y ~300.
- **Fantasy-element (hoog):** zwevende eilanden, reuzenskeletten, kristalcaldera's; het voelt als een andere wereld.
- **Overlevingsdruk:** verticaliteit en navigatie (bruggen bouwen over lava), extreme hitte boven lava, vallen = lava = spullen kwijt; uitbarstingen. Minder nadruk op het dag/nacht-ritme.
- **Nether-ring 3:** "bevroren hemel": een bevroren lavazee met ijsschotsen aan het plafond en ijzige reuzenskeletten als spiegel.
- **Technische risico's (1.21.1), hoog:**
  - Een lavazee in de overworld betekent veel licht, lava-naden in de zone 2→3 en bij grotten (lava stroomt in carvers).
  - Eilanden + carvers geven zwevende brokken.
  - De bouwlimiet Y 320 knelt bij kegels van Y 300 met een pluim.
  - De breukrand botst met de smoothstep-menging van `dry_riverbeds`; de rand moet volledig in de ring 3-invoer liggen, achter de zone.
  - Veel nieuwe DF-typen.
  - Spullen verliezen in lava frustreert.
- **Prestatie-inschatting:** het hoogst: 1,8–2,5× vanilla door 3D-eilanden over een grote hoogteband plus de hoge kegels.
- **Bouwplan-impact:** 39/42/47 worden veel groter; waarschijnlijk 2–3 extra worldgen-changes.

### Concept C — "Asgraf": grimmig survival, lage fantasy

**Kern:** een Pompeii-achtig asland waar het water uit gezogen is. De vijand is de omgeving zelf: hitte, as, gas, honger. Fantasy zit alleen in het verhaal en in de resten van wat er was.

- **Wow bij de grens:** een muur van as. De lucht wordt donker-oranje, as valt als sneeuw, het geluid dempt, en in de verte flitst een uitbarsting. Het landschap is monochroom met gloeiende aders.
- **Sub-biomes (5):** `ashen_wastes` (dominant, asduinen), `pyroclast_fields` (bomkraters, puimsteen), `sulfur_fields` (gaskuilen), `dried_seabed`, `volcanic_peaks`.
- **Terrein-signatuur:** brede golvende asduinen, bomkraters, diepe spleten met lava op de bodem; weinig hoogteverschil.
- **Fantasy-element (laag):** versteende bossen, gloeiende aders; verder realistisch.
- **Overlevingsdruk (zwaarst):**
  - hitte én uitdroging samen;
  - giftige gaskuilen in kraters (verstikking als conditie-zone);
  - aslagen die tijdens asregen groeien (server-side blokwijzigingen);
  - overdag onleefbaar, dus 's nachts reizen;
  - geen voedsel.
- **Nether-ring 3:** bevroren, maar doodstil: een ijsvlakte met ingesloten asbellen.
- **Technische risico's:**
  - Grootste speelrisico: monotoon en kaal, precies wat ring 2 moest herstellen.
  - Groeiende aslagen zijn server-side blokwijzigingen over grote gebieden (random ticks, kosten, schade aan bouwwerken).
  - Hoog frustratiegevaar.
- **Prestatie-inschatting:** terrein licht (≤ 1,2×); weer en aslagen kosten server-ticks.
- **Bouwplan-impact:** worldgen kleiner; conditions en atmosphere groter.

### Aanbeveling
**Concept A als basis**, met 1–2 wow-elementen uit B (zeldzame zwevende basalteilanden boven calderameren; lavavallen aan kraterranden) en het overlevingsritme van C (dag/nacht, schaarste, geen vee), zonder de monotonie van C. A sluit het best aan op wat al gebouwd is (`CanyonDetails`, `polygon_edge`, `loose_sand`, het weerpatroon) en houdt de prestatie binnen het ring 2-budget.

---

## 4. Vragenlijst (rondes van max. 4; aanbevolen optie eerst)

### Ronde 1 — richting (bepaalt de rest)

**1. Welke overkoepelende richting krijgt ring 3?**
- **A. Concept A "Smeulend Vuurland" met wow-elementen uit B (zeldzame zwevende eilanden, lavavallen) en het dag/nacht-ritme van C (aanbevolen).** Levendig en gevaarlijk; complexiteit middel (één nieuw celtype voor kegels, rest hergebruik); prestatie ~ring 2.
- **B. Concept B "Rijk der Vuurreuzen" (episch).** Het grootste wow, maar de hoogste complexiteit (lavazee, 3D-eilanden, breukrand) en prestatie 1,8–2,5×; 2–3 extra worldgen-changes.
- **C. Concept C "Asgraf" (grimmig survival).** Het zwaarst overleven, het minste terreinwerk; risico op kaal en monotoon (les ring 2).
- **D. Concept A zuiver (zonder zwevende eilanden).** Het meest voorspelbaar; wow alleen uit schaal, licht en flora.

**2. Hoeveel fantasy?**
- **A. Gematigd: geologie die net niet kan (gloeiende vuurflora, smeulbomen, vuurkristal-geodes, zeldzame zwevende basalteilanden), geen magie of beschaving (aanbevolen).** Past bij de horror-toon en laat fantasy-gebouwen aan fase 3; weinig extra werk boven het bestaande ontwerp.
- **B. Hoog: daarbovenop reuzenskeletten, zielvuurvelden (blauw vuur), "titanen" als rotsvormen.** Sterker wow en verhaal (sluit aan op bewakers), meer features en textures; risico dat ring 3 sprookjesachtig wordt naast de horror.
- **C. Laag: realistisch-vulkanisch; sfeer via schaal, licht en weer.** Het minste werk; de gebruiker vroeg juist om fantasy.

**3. Wat is het wow-moment bij de grens 2→3?**
- **A. Horizon-vulkanen: vanuit `dry_riverbeds` zichtbare kegels boven de wolken met gloed en rookpluim; op de grens slaat de lucht om naar asrood met opstijgende vonken (aanbevolen).** Het wow begint al vóór de grens (nieuwsgierigheid); vraagt hoge kegels vlak achter de zone en particles (client, biome-data); geen multiplayer-impact.
- **B. Breukrand: achter de zone breekt het land af naar een gloeiende vallei 40–80 blokken lager (overzichtsmoment).** Heel sterk eerste beeld; de rand moet volledig in de ring 3-invoer achter de zone liggen (smoothstep-menging); een barrière die je moet afdalen.
- **C. Lavarivier langs de grens met natuurlijke basaltbruggen ("poort").** Duidelijke drempel en een avontuurgevoel; een lange rivier langs een verruisde grens is lastig deterministisch netjes te krijgen.
- **D. A + B samen.** Het maximale wow, met de complexiteit van beide.

**4. Wat is de overlevingskern, waardoor ring 3 het langst duurt?**
- **A. Hitte met sterk dag/nacht-ritme + schaarste: overdag bovengronds bijna onleefbaar (reizen via lavabuizen en schaduw), 's nachts herstel en reizen; water verdampt, geen vee, weinig eetbare flora; zeldzame koele plekken (aanbevolen).** Een duidelijk eigen speelritme dat ver vooruit plannen vraagt; alles server-side en past in het condities-systeem; nachten worden in fase 4 extra spannend door mobs.
- **B. Twee meters: hitte én uitdroging (uitdroging geldt ook in ring 3).** De meeste druk en logisch (gestolen water); twee HUD-balken tegelijk, kans op frustratie; extra regel in `DehydrationRule`.
- **C. Vooral terrein en gevaren (lava, korst, erupties, gas) met een mildere hitte.** Avontuur boven beheer; minder "lang overleven", meer "voorzichtig reizen".
- **D. Vooral omvang en zoektocht (bredere ring, schaarse ertsen) met de hitte zoals ontworpen.** Weinig nieuw werk; de lengte komt vooral uit afstand en kan saai worden.

### Ronde 2 — wereld

**5. Welke sub-biomes?**
- **A. Zes: de huidige vijf + `ember_woods` (smeulwoud met gloeiende kronen en vuurflora) als levendige biome (aanbevolen).** Lost "levendig" structureel op; één biome extra (surface, flora-features, kleuren).
- **B. De huidige vijf, ongewijzigd; levendigheid via aankleding per biome.** Minder werk, maar `ashen_wastes`/`basalt_flows` dreigen kaal te worden (les ring 2).
- **C. De zes van concept B (o.a. `obsidian_skies`, `lava_delta`, `titan_graveyard`, `crystal_calderas`).** Hoort bij richting B; veel nieuw terreinwerk.
- **D. Vier grotere biomes (asvlakte, pieken, stromen, zeekom), zwavel als feature.** Sterkere eigen identiteit per gebied; minder afwisseling.

**6. Welke terrein-signaturen? (meerkeuze)**
- **Lavarivieren en spaarzame lavavallen (aanbevolen).** Iconisch en levendig (licht, beweging, geluid); statische lava via de vloeistofregel; vallen als features met een cap (fluid ticks).
- **Reuzenkraters en caldera's, sommige slapend met een groene bodem (aanbevolen).** Landmark plus een schuilplaats; één celtype in Java (sjabloon `CanyonDetails`).
- **Zwevende basalt-/obsidiaaneilanden boven calderameren, zeldzaam (aanbevolen bij vraag 2 A/B).** Het fantasy-wow; techniek van de void-invoer, lokaal duurder; let op carvers (zwevende brokken).
- **Basaltzuilvelden (Giant's Causeway) in `basalt_flows`.** Een herkenbare vorm; hergebruik van `polygon_edge`; weinig risico.

**7. Wat is de "oase" van ring 3, de veilige plek?**
- **A. Slapende kraters: een koele caldera met vruchtbare grond, groen, wat eetbare flora en stoombronnen (aanbevolen).** Een wow-ontdekking en een rustpunt, zoals de oase in ring 2; hitte ↓ in die biome/zone (regel zoals `oasis_flats`); deterministisch via het kratercel-type.
- **B. IJsgrotten in lavabuizen (echt bestaand verschijnsel): koud, met smeltwater ondergronds.** Mooi en verrassend, verbindt met Nether-3; zit ondergronds en is dus moeilijker te vinden; vraagt een eigen grotfeature.
- **C. Beide, allebei zeldzaam.** Het rijkst; ook het meeste werk (twee systemen).
- **D. Geen aparte oase; alleen grotten en de nacht.** De hardste ring; mist een rustpunt en voelt eerder straffend dan avontuurlijk.

**8. Wordt ring 3 breder? (config, geldt alleen voor nieuwe werelden)**
- **A. Ja: ring 3 van 3.500 tot 6.000 (2.500 breed); ring 4 6.000–7.800, ring 5 7.800–9.500 (aanbevolen).** Ring 3 wordt de grootste ring (~75 mln m²) en Nether-3 krijgt 625 breed (de ruimte-check die al open stond); ring 4/5 zijn nog niet gebouwd, dus nu goedkoop; testcoördinaten van ring 2 veranderen niet.
- **B. Nee, 2.000 breed laten.** Geen wijziging; "het langst" moet dan uit gameplay komen.
- **C. Ring 3 breder zonder ring 4/5 te krimpen: world border naar ~10.500.** Alles blijft breed; de buitenrand en de border (§1.8, change 7) verschuiven.

### Ronde 3 — overleving, weer en voedsel

**9. Hoe hard is de hitte (fase 1, zonder gear)?**
- **A. Zon overdag 0 → 100 in ~3 min (sneller dan uitdroging, ~4 min); hittebronnen en pieken ×1,5–2; nacht, ondergronds en water ↓; drempels 25 traagheid → 50 zwakte → 75 mining fatigue → 100 in brand (aanbevolen).** Merkbaar zwaarder dan ring 2, maar met het dag/nacht-ritme te doen; waarden in de SERVER-config.
- **B. Gelijk aan uitdroging (~4 min) met de drempels uit het ontwerp.** Een vertrouwd tempo; ring 3 voelt dan niet zwaarder dan ring 2.
- **C. Hard (~2 min), extra schade bij 100.** Het zwaarst; zonder gear vrijwel alleen 's nachts en ondergronds te doen; risico op frustratie.

**10. Welk weer en welke events krijgt ring 3?**
- **A. Asregen + uitbarsting (zoals ontworpen) + een zeldzame aswolk: donker, hitte ×3, schuilen of branden (aanbevolen).** Een echt overlevingsmoment, gesynct en zichtbaar voor alle spelers; één extra `RingWeatherType`.
- **B. Alleen asregen en uitbarsting.** Minder werk; minder piekmomenten.
- **C. A + aardbevingen (camera-shake + losse rotsen die vallen, server-side).** Het meest dynamisch; vallende blokken zijn server-side blokwijzigingen (moeten `mobGriefing` volgen en spelersbouw sparen).

**11. Waar vallen de lavabommen van een uitbarsting?**
- **A. Ringbreed rond spelers onder open lucht, dichter in en bij `volcanic_peaks` (aanbevolen).** Iedereen in ring 3 voelt de uitbarsting, het gevaar is het grootst bij de vulkanen; eenvoudig (geen vulkaanposities nodig).
- **B. Alleen in en bij `volcanic_peaks`.** Realistischer; elders merk je weinig van het event.
- **C. Echte lavastromen die uit kraters over het terrein lopen.** Spectaculair, maar zware server-side blok- en vloeistofwijzigingen; niet aan te raden in fase 1.

**12. Voedsel en water in ring 3?**
- **A. Geen vee (biome-spawns leeg); wat eetbare flora (een blok met een vanilla-drop als placeholder, eigen voedsel in fase 2); water alleen ondergronds en in de koele plekken (aanbevolen).** Echte schaarste zonder dat het onmogelijk wordt; alleen data plus één blok.
- **B. Zeldzaam vee (bijv. geiten op de pieken).** Milder; minder schaarste.
- **C. Vanilla-spawns.** Geen voedseldruk; botst met "langst overleven".

### Ronde 4 — resources, Nether, prestatie

**13. Resources en ertsen?**
- **A. Zoals ontworpen (vulkaniet diep bij lava, zwavel ondiep, vuurrobijn in magmakamers) + vanilla verder afgebouwd + vuurkristal-geodes als zeldzaam fantasy-accent (gloeiend, nut in fase 2) (aanbevolen).** Risico = beloning blijft; geodes geven ontdekkingsplezier (sjabloon: dauwgeodes van change 35).
- **B. Zoals ontworpen, zonder geodes.** Minder werk.
- **C. Schaarser: vulkaniet alleen in magmakamers en lavabuizen.** Langer zoeken; risico dat progressie hapert.

**14. Nether-ring 3?**
- **A. De bevroren Nether zoals ontworpen, met de levendig-eis en een eigen wow (bevroren lavavallen, rijpwoud met ijspegels, gloeiende scheuren als warme eilandjes) (aanbevolen).** Het ontwerp blijft staan en sluit aan op oerijs en kou; vooral extra aankleding.
- **B. Zoals ontworpen, zonder extra's.** Het minste werk; risico op kaal (les ring 2).
- **C. Spiegel van het gekozen concept (bijv. zwevende ijsschotsen bij concept B).** Sterkere eenheid met ring 3; meer terreinwerk.

**15. Prestatiebudget (noise-stap ten opzichte van vanilla)?**
- **A. ≤ 1,75× (zoals ring 2); stromende lava bij generatie alleen met een cap per chunk (aanbevolen).** Bewezen haalbaar en speelbaar in multiplayer.
- **B. ≤ 2× voor de grote vormen (kegels, eilanden).** Meer ruimte voor wow; chunks laden merkbaar trager bij snel reizen.
- **C. ≤ 1,3× (streng).** Snel; sluit zwevende eilanden en hoge kegels vrijwel uit.

**16. Horror-elementen uit het ontwerp (as-afgietsels, voetsporen in de as, gerommel/camera-shake)?**
- **A. Houden zoals ontworpen (aanbevolen).** Al besloten, passen bij elk concept; weinig werk.
- **B. Houden + "smeulende silhouetten" (as-afgietsels die 's nachts zwak gloeien).** Extra sfeer; een emissive blokvariant.
- **C. Schrappen of naar fase 5.** Minder werk; ring 3 verliest de horrorlaag.

### Ronde 5 — werkwijze (zie §5)

**17. Welke implementatie-methodiek?**
- **A. Contract-first in golven, werkstromen parallel in eigen git-worktrees, één integratie-eigenaar die merget, `runData` draait en `CODEBASE.md` bijwerkt (aanbevolen).** Echt parallel met weinig conflicten; vraagt vooraf een contract en een integratiestap per golf.
- **B. Parallel in de gedeelde werkmap (zoals 35/36/37) met de lock `C:/Temp/ca-test.lock`, `git update-index --cacheinfo` en eigen PID's.** Geen worktree-setup; tests lopen achter elkaar (lock), en gemengde bestanden zijn foutgevoelig.
- **C. Plannen parallel, bouwen in twee banen (worldgen-baan en rest-baan), elk sequentieel.** Eenvoudig en veilig; minder snelheid.

**18. Welke teststops?**
- **A. Drie: 4a terrein-wow (na golf 1, eerst PNG-kaarten, dan in-game; blokkerend), 4b overleving + sfeer (na golf 2, server + 2 clients), 4c eindcheck incl. Nether-3 en textures (aanbevolen).** Het wow-oordeel komt vroeg (les ring 2: terrein eerst); drie momenten voor de gebruiker.
- **B. Twee: na terrein + biomes, en een eindcheck.** Minder onderbrekingen; overleving en sfeer pas aan het eind.
- **C. Eén eindcheck.** Het snelst; bijsturen pas als alles al gebouwd is.

**19. Textures: eerst placeholders of direct definitief?**
- **A. Een textures-stroom vanaf golf 1 die direct "goede eerste versies" tekent in de 37b-stijl (`Ring3Textures.java`); placeholders alleen zolang die nog niet klaar zijn (aanbevolen).** Teststop 4a ziet al echte textures (wow hangt ook aan kleur en gloed); de §1.4-regel "placeholders eerst" wordt soepeler.
- **B. Zoals ring 2: placeholders, definitief in change 57.** Bekend patroon; het wow-oordeel gebeurt met placeholder-kleuren.

**20. Bouwplan-nummering?**
- **A. De nummers 38–57 blijven als werkpakketten, gegroepeerd in golven; nieuwe pakketten krijgen een letter (39b, …) (aanbevolen).** Verwijzingen in backlog en docs blijven kloppen.
- **B. Blok E opnieuw nummeren per stroom (E-W1, E-B1, …).** Overzichtelijker per stroom; oude verwijzingen naar 39/41 moeten mee.

---

## 5. Implementatie-methodiek (voorstel)

De gebruiker wil niet meer change voor change sequentieel werken, maar onderdelen parallel bouwen met meerdere agents. Hieronder een concreet voorstel dat past bij deze repo en bij de lessen van 35/36/37.

### 5.1 Uitgangspunten
- **Elke change blijft één categorie** (CLAUDE.md §2). Parallel betekent: meerdere changes uit verschillende categorieën tegelijk, elk in een eigen stroom.
- **Contract vóór code:** alles waarop een andere stroom bouwt, ligt vooraf vast en verandert alleen via de hoofdsessie.
- **Eén eigenaar per gedeeld bestand.** Wat niet te splitsen is, gaat via de integratie-eigenaar.
- **De gebruiker beslist ontwerpvragen.** Agents verzamelen hun vragen; de hoofdsessie bundelt ze tot `AskUserQuestion`-rondes.

### 5.2 Werkstromen

| Stroom | Categorie | Werkpakketten (huidige nummers) | Eigen bestanden (globaal) |
|---|---|---|---|
| **W-terrein** | `worldgen` | 39 (terrein), 42 (grotten), 53/54 (Nether-3-terrein) | `density_function/ring_3/**`, `noise/ring_3_*`, `ring_blend/*` (alleen de ring 3-sleutels), nieuwe DF-types in `worldgen/densityfunction/` |
| **W-biomes** | `worldgen` | 39 (biomes, surface), 41 (ertsen), 47, 48, 49 | `Ring3Biomes`, `Ring3Features`, `noise_settings/overworld.json` (ring 3-tak), preset `corrupted_world.json` (ring 3-source) |
| **B-blokken** | `blocks` | 38, 40, 46, 51, 52 | `ModBlocks`/`ModItems`/`ModTags` (ring 3-blok), `block/*` (gedrag), blok-datagen |
| **C-condities** | `conditions` | 43, 44, 55 | `condition/Heat*`, `Cold*`, `WaterEvaporation*`, `ServerConfig` (eigen secties) |
| **A-sfeer** | `atmosphere` | 45, 50, 56 | `weather/*` (ring 3), `client/weather/*Renderer` (ring 3), `BiomeAmbience` (ring 3-builders), `tools/sounds/Ring3Sounds.java` |
| **T-textures** | `textures` | 57 (+ eerste versies vanaf golf 1) | `tools/textures/Ring3Textures.java`, `assets/…/textures/{block,item,particle}/` (ring 3-namen) |

W-terrein en W-biomes zijn twee stromen binnen `worldgen`. Hun bestanden overlappen niet, maar ze wisselen het contract uit (regio-assen en DF-namen).

### 5.3 Contract (golf 0)
Eén bestand `docs/plans/ring-3/contract.md`, opgesteld uit de sparringuitkomst en vastgezet vóór golf 1:
- **Registry-namen** van alle blokken en items (ring 3 + Nether-3), met blockstate-properties, kaartkleur, sterkte/tool-klasse en welke blokken licht geven.
- **Tags:** bijv. `corruptedadventure:heat_sources` (hitte ↑, gelezen door C), `cold_relief` (warmte voor kou), `is_ring_3`, `is_nether_ring_3`, `ring_3_stone` (ertsdoelen), `ash_layers`.
- **Biome-keys** (overworld + Nether + overgang), regio-assen en hun bereik.
- **DF- en noise-namen** met hun betekenis (bijv. `ring_3/overworld/region`, `heat`, `crater_mask`), zodat W-biomes en C ze kunnen lezen zonder op W-terrein te wachten.
- **Feature-keys**, de volgorde in de decoratiestappen en de determinismeregels.
- **Sound-events, particle-types, weertypes** (bijv. `ASH_CLOUD`), conditienaam(en), config-sleutels (`[conditions.heat]`, `[weather.ring3]`), payload-namen.
- **Lang-sleutels** (patroon) en texturepaden.

Wijzigt er iets in het contract, dan beslist de hoofdsessie en stuurt ze een bericht naar alle stromen.

### 5.4 Eigenaarschap van gedeelde bestanden

| Bestand | Eigenaar | Andere stromen |
|---|---|---|
| `ModBlocks`, `ModItems`, `ModTags`, blok-loot, blockstates, blok-tags | B-blokken | registreren niets; lezen alleen namen uit het contract |
| `ModBiomes`, preset, `noise_settings/*.json`, `ring_blend/*.json` | W-biomes (ring 3-tak); W-terrein alleen de ring 3-sleutels in `ring_blend` | niet aanraken |
| Lang (`EnglishLanguageProvider`, `DutchLanguageProvider`) | **opgesplitst in golf 0**: per stroom een helper (`Ring3BlockLang`, `Ring3BiomeLang`, …) die de provider aanroept; elke stroom bezit alleen zijn eigen helper | — |
| `DataGenerators` | integratie (golf 0 zet alle aanroepen klaar) | niet aanraken |
| `ServerConfig` | C-condities (`[conditions.*]`), A-sfeer (`[weather.ring3]`); aparte secties, merge per stuk | — |
| `ModNetwork.PROTOCOL_VERSION` | integratie: één verhoging per golf | nieuwe payloads alleen melden |
| `src/generated/**` | **integratie**: draait `runData` na de merge en commit de output | draaien `runData` lokaal om te testen; hun gegenereerde output wordt bij de merge overschreven |
| `CODEBASE.md`, `fase-1-wereld.md`, `02-beslissingenlog.md`, `backlog.md` | **integratie** | elke stroom schrijft zijn delta in het eigen plan (secties "CODEBASE-delta", "Beslissingen", "Backlog"); de integratie voegt samen |

### 5.5 Isolatie en de lessen van 35/36/37
- **Eigen git-worktree per stroom** (Agent met `isolation: "worktree"`, branch `ring3/<stroom>`). Elke stroom heeft dus een eigen `build/` en `run/`: geen Gradle-lock-gevechten en geen `git update-index --cacheinfo` voor gemengde bestanden.
- **Vaste poorten per stroom** in de eigen `run/server.properties` (server 25570 + n, RCON 25580 + n), een eigen `level-name` per test. Sluit aan op de valkuil "Parallelle servers en `run/`".
- **Alleen eigen processen stoppen:** elke stroom schrijft zijn PID's naar `build/<stroom>.pids` en stopt alleen die. Nooit breed `serverRun`-JVM's doden; `tools/server-smoke-test.sh` doodt elke `serverRun`, dus in parallel RCON `stop` gebruiken.
- **Lock blijft voor gedeelde zaken:** `C:/Temp/ca-test.lock` voor alles wat stromen toch delen (JFR-prestatiemetingen op een rustige machine, ffmpeg, runClient met de gedeelde quick-play-poort).
- **Machinebudget:** max. 3 Gradle/Minecraft-JVM's tegelijk (RAM). Prestatiemetingen (JFR) alleen in de integratiestap, niet terwijl andere stromen bouwen; anders zijn de cijfers onbetrouwbaar.
- **Review vóór commit:** elke stroom draait `/code-review` (medium) op de eigen diff en commit pas daarna in de eigen branch. Het voorval van 36/37 (commit vóór review) komt dan niet meer voor.
- **Liveness-checks:** de hoofdsessie controleert elke ~15 min per stroom de commits in de branch, de mtimes in de worktree en de agentstatus. Een stroom schrijft na elke stap een regel "Voortgang" in zijn plan. Een stille stroom (zoals build-35) wordt opnieuw gestart vanaf de laatste voortgangsregel.

### 5.6 Golven, integratie en teststops

| Golf | Parallel | Integratie / teststop |
|---|---|---|
| **0** (kort, deels sequentieel) | Plannen per stroom (parallelle plan-agents, vragen gebundeld naar de gebruiker); contract; `infra`: lang-helpers per stroom + terreinkaart-debugcommando (backlog r. 71) | contract vastgezet; plannen gecommit |
| **1** | B-blokken: álle ring 3- en Nether-3-blokken als skelet met gedrag en eerste textures · W-terrein: terreinvormen overworld + Nether-3 (vanilla basalt/blackstone als tijdelijke surface) · C-condities: hitte + water verdampt (43, 44) · A-sfeer: weer (45) · T-textures: eerste versies | **Integratie 1** (volgorde: B → W-terrein → C → A → T; `runData`, B/D/S, prestatie meten) → **teststop 4a: terrein-wow** (eerst PNG-kaarten, dan in-game; blokkerend) |
| **2** | W-biomes: biomes, surface, features, ertsen, `steam_fens`, horror (39 rest, 41, 47, 48, 49) · W-terrein: grotten + Nether-3-biomes (42, 53 rest, 54) · B: geiser/fumarole/korst-gedrag (46 rest) · C: kou (55) + afstemming · A: sfeer + geluid (50, 56) · T: definitief (57) | **Integratie 2** → **teststop 4b: overleving + sfeer** (dedicated server + 2 clients) |
| **3** | Fix-ronde "levendig" en feedback 4b, per categorie | **Teststop 4c: eindcheck** ring 3 + Nether-3 |

W-biomes en de ertsen zitten bewust in één stroom: beide raken `noise_settings`, biomes en de feature-volgorde. De valkuil "feature-indexverschuiving" (andere decoratie in ring 1/2) blijft zo binnen één eigenaar en wordt één keer gemeten.

### 5.7 Orkestratie door de hoofdsessie
1. De hoofdsessie leest dit document en de antwoorden, en laat golf 0 uitvoeren (plan-agents parallel, één contract-agent).
2. Per golf: één Agent per stroom (`isolation: "worktree"`, general-purpose), met de vaste bouwopdracht uit `docs/prompts/opdracht-04-ring-2-afronden.md` (punten 1–9), aangevuld met: worktree, eigen poorten, PID-bestand, contract, geen gedeelde docs, de delta in het eigen plan.
3. Vragen van stromen gaan naar main; main bundelt ze (max. 4 per ronde, "alles aanbevolen" als optie) en stuurt de antwoorden terug.
4. **Integratie** (een aparte integratie-agent, of main zelf als de context het toelaat): merge per branch in vaste volgorde, conflicten oplossen volgens §5.4, `runData`, B/D/S, prestatie meten, docs samenvoegen, `/code-review` op de integratiediff, commit (`merge: ring 3 wave N` + geregenereerde output).
5. Teststop voorbereiden: de gebundelde checklist uit de W-lijsten van de stromen.
6. **Optioneel via de Workflow-tool:** main schrijft per golf een workflow-script (fan-out van de stromen, join, integratie, review) en gebruikt daarbij de skill `workflow-authoring`. Dat is reproduceerbaar en houdt main's context klein, maar alleen als de gebruiker daarvoor kiest. Anders werkt het met parallelle Agent-aanroepen in één bericht.

### 5.8 Risico's
- **Contract-drift:** een stroom heeft een naam of tag nodig die niet in het contract staat. Mitigatie: de vraag gaat naar main; tijdelijk een placeholder in de eigen stroom, nooit zelf in een andermans bestand.
- **Mergeconflicten** in grote gedeelde JSON (`noise_settings/overworld.json`, preset). Mitigatie: één eigenaar (W-biomes); W-terrein raakt alleen `ring_blend` en eigen bestanden.
- **Gegenereerde output** botst bij de merge. Mitigatie: altijd opnieuw genereren in de integratie, nooit met de hand mergen.
- **Prestatiecijfers** zijn onbetrouwbaar als er parallel gebouwd wordt. Mitigatie: alleen meten in de integratie, op een rustige machine.
- **Machinebelasting** (RAM, CPU). Mitigatie: max. 3 JVM's; stromen zonder Minecraft-test (textures, plannen) tellen niet mee.
- **Te veel vragen tegelijk** voor de gebruiker. Mitigatie: main bundelt per golf; implementatiedetails beslissen de agents zelf.
- **Review-overload:** 5–6 reviews per golf plus de integratie. Mitigatie: reviews draaien in de stromen zelf; de integratiereview is alleen de mergediff.
- **Feature-volgorde en determinisme:** nieuwe features verschuiven decoratie-seeds. Mitigatie: één worldgen-eigenaar voor features; de determinismetest (twee werelden, omgekeerd geladen) in de integratie.
- **Een stroom die stil sterft.** Mitigatie: liveness-checks (§5.5).

### 5.9 Varianten (vraag 17)
- **A. Worktrees + integratie-eigenaar, contract-first in golven (aanbevolen).** Echt parallel, de minste conflicten, een schone geschiedenis per stroom; kost een contract en een integratiestap per golf.
- **B. Gedeelde werkmap (zoals 35/36/37):** lock `C:/Temp/ca-test.lock`, `git update-index --cacheinfo` voor gemengde bestanden, alleen eigen PID's. Geen setup, maar tests lopen achter elkaar en gemengde bestanden zijn foutgevoelig.
- **C. Twee banen** (worldgen sequentieel ‖ rest sequentieel), plannen wel parallel. Veilig en eenvoudig; ongeveer 2× sneller dan nu in plaats van 4–5×.
