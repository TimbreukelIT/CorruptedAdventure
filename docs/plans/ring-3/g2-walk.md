# G2 ring 3 — nulmeting, rondgang en voorstellen

Scope: `worldgen` (opknapbeurt G2, `docs/design/fase-1-wereld.md` §G2). Stroom `g2-ring3` (poort n = 7), worktree
`wt/g2-ring3`, branch `g2/ring3`. Dit document is het materiaal voor de sparring over ring 3; er is **niets gebouwd**
behalve kleine bugfixes (zie "Opgelost"). Lat: de aankleding van ring 4 na teststop 5b (proefstuk + uitrol: bosjes i.p.v.
raster, grondvulling, dichtheid per biome, 1–2 blikvangers per biome per wereldcel van ~320).

## 1. Inventaris per biome (stand `main` `bac7f266`)

Bronnen: `Ring3Features.ORDER` en de featureklassen (`Ring3DressingFeatures`, `TitanFieldsFeatures`, `EmberWoodsFeatures`,
`DormantCalderaFeatures`, `SulfurFieldsFeatures`, `SteamFensFeatures`, `Ring3HorrorFeatures`, `Ring3IslandFeatures`,
`Ring3LavaFallFeatures`), de surface (`tools/worldgen/ring3_surface.py`) en de terreinvormen (`VolcanicField`,
`Ring3Relief`, `CellShapes`, `Landmarks`, `CinderCones`, `TitanBones`). Dichtheid "per chunk" = kans × (16/cel)² van de
`jittered_grid`, vóór de filters (helling, grond, biome); de echte dichtheid ligt daar vaak ver onder.

### Ringbreed (terrein, in alle "ruige" biomes)
| Vorm | Dichtheid | Opmerking |
|---|---|---|
| Vulkanen (kegels 80–140 boven de vlakte, caldera's, horizonreuzen Y 250–285) | ~1 per 500–600 blokken in as/smeulwoud/zwavel (39d) | ~30 % kegels / ~45 % caldera's slapend |
| Obsidiaannaalden 60–120 | cel 192, kans 0,3 (≈ 1 per ~350 blokken) | niet in zeekom/titanenveld, vrij van vulkanen |
| Instortingen (12–22 diep, treden, soms schacht) | cel 224, kans 0,85 op de vlaktes | ~1 per 250–350 |
| Slakkenkegelvelden (10–40 met minikrater) | cel 112, geclusterd per veld | niet bij vulkanen/rivieren/rifts |
| Riftdalen (20–40 diep) + riftbogen | 21 bogen op de testseed | niet in zeekom/titanenveld/zwavelbekken |
| Lavarivieren (getraceerd, 6 → 16 breed) | 1–2 per actieve vulkaan | met lavavalletjes per trede |
| Afgronden met lavazee Y 9 | cel 1.600 | zeldzaam |
| Zwevende archipels / zwevende vulkaan | boven lage actieve caldera's; vulkaan cel 2.000 | eilanden met smeulende as, smeulbomen, hangende vuurkristallen |
| Lavavallen (feature) | max. 2 bronnen per chunk, ~1 per 2–4 vulkanen | alleen waar ze in lava landen |
| Aslaagjes (`ash_drift`) | ruis + windrichting | as 1,0 · pieken 0,6 · titanen 0,5 · zeekom 0,3 |
| Voetsporen in de as | ~1 per 20 chunks | as, titanen, pieken |

### Per biome
| Biome | Terreinvorm (39d) | Features nu (per chunk vóór filters) | Bodemvariatie (surface) |
|---|---|---|---|
| `ashen_wastes` | duinzee 10–25 met lijzijde, geulen met hoodoos 5–15 (cel 12 in de geulen) | puimsteenkeien 0,48 · verkoolde dode bomen 0,35 (alleen aan de voet van een geulwand op as/grind/puimsteen) · as-afgietsels 0,3 groepjes · aslaagjes | as, puimsteen-vlekken, smooth basalt/basalt, grind, alle in dezelfde grijswaarden |
| `volcanic_peaks` | bergketen +35, kegels met kammen | inslagkraters 0,15 · blackstone-rotsen 0,48 · aslaagjes 0,6 · geiten | as/puimsteen, blackstone, smooth basalt, magma-scheurtjes |
| `dried_seabed` | zeekom Y 30–45, kustterrassen, rifruggen, zeepilaren 15–40 | dode koraalriffen 0,7 (op kammen) + dood koraal erop · kelpbalen 0,48 · zeemonsterfossielen 0,12 · dode struiken 1,28 | zand/as, puimsteen, grind, klei |
| `basalt_flows` | 3 lavaplateaus (treden van 12), zuilenkliffen, lavabuizen/sleuven | lavatongen 0,3 · hornitos 0,2 · smeulventjes 0,6 · basaltkeien 0,6 | basalt/smooth basalt, blackstone-vlekken, smeulsteen-/magma-scheuren, as |
| `ember_woods` | glooiend laagland −6 | smeulbomen/reuzen/kale stammen 7,2 (cel 4, kans 0,45) · open plekken met omgevallen stammen 0,3, bessen 0,6, vuurflora (cel 2, in plukken) · aslaagjes onder de bomen | as, smeulende as (scheurtjes), tuf/puimsteen op de treden, grove aarde |
| `sulfur_fields` | bekken −14, zwavelterrassen, fumarole-heuveltjes (cel 36), zinkgaten (cel 64) | fumarole op elke heuveltop + 0,32 los · modder in zinkgaten · gekleurde kookpoelen met travertijn 0,5 · zwavelkristallen in groepjes | zwavelkorst, smooth basalt, gele terracotta, calciet, as |
| `titan_fields` | stenen ribben 20–60 als bogen, ribbenkasten (cel 448) met wervels, lage ribben | schedels bij ribbenkasten · losse botten 0,15 · zielvuur rond ribvoeten/wervels · titanen-rotsen (cel 256, kans 0,4) · aslaagjes 0,5 · zeldzame afgietsels | zielgrond/as, basalt/smooth basalt |
| `dormant_caldera` | kraterbodem (micro-biome) | mosheuveltjes 0,8 · azaleabomen 0,6 in bosjes · azaleastruiken · omgevallen eikenstammen 0,2 · varens/gras · dripleaf · bessen aan de wand · mostapijt · ranken/hangwortels en glow lichen aan de wand | mos, grove aarde, as, basalt |
| `steam_fens` (3→4) | menging met ring 4 | kookpoelen 0,3 (boven Y 63) · modderpoelen 0,7 · geisers 0,3 · stoomventen 1,0 | modder, packed mud, klei, grind/as, zwavelkorst langs scheuren, tuf |

### Zwakke plekken t.o.v. G2 (vóór de rondgang; hypothesen uit de code)
- **Kleine features zijn dun.** Behalve smeulwoud, slapende krater en de dode struiken van de zeekom liggen alle grondfeatures
  op ≤ 0,7 per chunk vóór filters; de ringvormige "ground"-filter (geen flanken, oevers, wanden) haalt daar nog veel af.
  In ring 4 na het proefstuk: kleine stenen op raster 8 overal, keien ≤ 1 per chunk, stronken/stammen, plantvulling op raster 2.
- **Geen subzones met andere flora.** Variatie zit alleen in de bodemkleur via ruis (P/D/G/S) en die blijft in dezelfde
  grijs-zwarte tonen (as ↔ basalt ↔ puimsteen). Alleen het smeulwoud (open plekken) en de slapende krater (groeiruis)
  hebben bosjes/open stukken zoals ring 4 (`ring_4_groves`).
- **Flora vrijwel afwezig buiten smeulwoud en krater.** `ashen_wastes`, `volcanic_peaks`, `basalt_flows`, `titan_fields`
  hebben geen enkele plant; `dried_seabed` alleen dode struiken. De visie ("levend vulkanisch land", "Smeulend
  Vuurland") vraagt om iets dat leeft of gloeit in de vlaktes.
- **Bezienswaardigheden:** de grote vormen (naalden ~350, instortingen ~250–350, vulkanen ~500–600, slakkenkegels)
  halen de 300–500 ringbreed waarschijnlijk al, maar ze zijn grotendeels **dezelfde in elk biome**. Biome-eigen
  blikvangers: alleen titanen-rotsen/ribbenkasten (titanen), zeemonsterfossielen/zeepilaren (zeekom), kookpoelen
  (zwavel). As, pieken, basalt en smeulwoud hebben er geen eigen.
- **`dried_seabed`:** de grootste vlakke kom van de ring; risico op een lege zandvlakte tussen de rifruggen.
- **`volcanic_peaks`:** grondfeatures alleen op vlakke stukken (`SlopeFilter.flat`), terwijl het biome vooral helling is.

## 2. Rondgang (2026-10-25)

**Werkwijze.** Verse wereld seed 20260930 (`run/g2r3`) met 12 plekken uit `tools/test/areas/g2-ring3.txt` (`world.sh
--only "ash1 ash2 peak1 sea1 sea2 bas1 emb1 sul1 tit1 dor1 b23e b34e"`); biome per plek gecontroleerd met `execute if biome`
(alle 10 binnenplekken kloppen; `b23e` = `dry_riverbeds`, `b34e` ligt al in ring 4). Poses uit de heightmap
(`mkposes.py` van walk 1: `_top` 42 boven/52 terug, `_low` 20/28, `_far` 70/160, `_eye_*` op ooghoogte), `walk.py` met
`CA_SHOT_F2=1`, FOV 70 (`studio.sh prepare`), dag behalve `emb1_eye_n_night`. Daarna 9 extra poses: de pieken opnieuw
(`peak1` stond op een kegelflank, de ooghoogtefoto is alleen rots) en `steam_fens` (`sf`, 5.880 / 0).
Foto's: `build/test/shots/g2-ring3/` (51 stuks; niet in git).

| Plek | Foto's | Oordeel |
|---|---|---|
| `ashen_wastes` (5.544, 616) en (−4.472, 3.032) | `ash1_low`, `ash1_eye_n`, `ash2_top`, `ash2_eye_w` | **Saai en monotoon.** Eén grijswaarde van horizon tot voeten; het microreliëf leest van boven als een **hoogtelijnenkaart** (concentrische traplijntjes). Puimsteenkeien zijn de enige vlekjes; dode bomen zie je bijna niet (alleen aan geulwanden), de afgietsels zijn roodbruine stipjes. Duinen en geulen zijn er wel, maar zonder accenten verdwijnen ze. Losse 1-blok-putjes in het microreliëf lezen van boven als gaatjes (`ash1d_down`: smalle treden, geen fout). |
| `volcanic_peaks` (−1.256, −3.944) en (3.832, 1.176) | `peak1_far`, `peak1_dist`, `peak2_top` | Vulkaan + rookpluimen tegen de oranje lucht = sterk silhouet. Van dichtbij: **kale grijze hellingen**, blackstone-rotsjes en inslagkraters alleen op de vlakke stukken; de flanken zelf (het grootste deel van het biome) zijn leeg. Kegels hebben een herhalend streeppatroon van puimsteen/basalt. |
| `dried_seabed` (−3.960, 856) en (3.656, 3.368) | `sea1_far`, `sea1_eye_e`, `sea1_top`, `sea2_low` | **Zeepilaren zijn de beste blikvanger van de ring.** Daartussen: grote zandvlakte met hoogtelijnen en grijze vlekken, kelpbalen als losse groene blokjes (lezen als "neergezet"), dode struiken klein. Dode riffen nauwelijks te zien. |
| `basalt_flows` (4.904, −712) | `bas1_top`, `bas1_low`, `bas1_eye_n` | **Werkt redelijk:** gloeiende scheuren, hornito's, lavarivier op de flank. Maar vlak grijs zonder subzones; blackstone-tongen zijn platte vlekken (geen reliëf). |
| `ember_woods` (3.496, −2.216) | `emb1_top`, `emb1_eye_n`, `emb1_eye_n_night` | 's Nachts mooi (gloeiende kronen). Van boven een **gesloten, uniform tapijt van identieke ronde kronen** (open plekken niet te zien); onder de bomen donker en eentonig. |
| `sulfur_fields` (−2.968, −4.536) | `sul1_top`, `sul1_far`, `sul1_eye_n` | Het levendigste biome (kleur, stoom, kristallen). Maar de kookpoelen zijn **allemaal even groot en vierkant en staan als een raster**: leest onnatuurlijk. Overal dezelfde gele korst. |
| `titan_fields` (−3.336, 2.440) | `tit1_top`, `tit1_far`, `tit1_eye_e` | Ribboog + botten + zielvuur lezen goed als thema. Tussen de ribben een **lege vlakte** van grijs met bruine zielgrondvlekken; geen titanen-rots in beeld op deze plek. |
| `dormant_caldera` (−824, 3.960) | `dor1_top`, `dor1_eye_n` | **Goed** (oase-gevoel, wand met ranken, kegel erachter). Klein punt: de azaleastruiken staan vrij gelijkmatig verdeeld; grove-aardevlekken rauw. |
| Grens 2→3 (3.420, 0) | `b23e_east`, `b23e_top` | `dry_riverbeds` met verkoolde stronken en botten: overgang leest. Geen vulkaan op de horizon vanaf deze plek (mist). |
| Grens 3→4 (5.880–6.000, 0) | `sf_top`, `sf_eye_n`, `b34e_top` | `steam_fens` is een **vlakke bruine modderplaat** met een paar poelen; **harde rechte rand** zand (zeekom) ↔ modder; daarna de groene `verdant_falls`. |

**Wat goed werkt:** de grote silhouetten (vulkanen met pluimen, zeepilaren, ribbogen), het kleurverschil per biome in de
lucht, de nacht in het smeulwoud, de zwavelvelden als geheel, de slapende krater.
**Wat niet werkt:** de **tussenruimte**. Op ooghoogte is elke vlakte (as, zeekom, titanen, pieken) een lege grijze of
beige plaat met hoogtelijnen; kleine features zijn te klein, te schaars of staan alleen op vlakke grond. Er is geen
plantengroei buiten smeulwoud en krater, en geen subzones (elke 500 blokken van een biome lijken op elkaar).

**Opgelost:** niets. Er zijn geen echte bugs gevonden (geen zwevende blokken of verkeerde bodem op de plekken); de
1-blok-putjes en het hoogtelijnenpatroon zijn ontwerp (microreliëf van 39f) en staan hieronder als voorstel.

## 3. Prestatie-baseline
Op verzoek van main geen eigen perf-run vooraf: één gezamenlijke run met g2-ring2 na beide walks (1 warm + 1 koud),
ring 3-gebieden `ember`, `sulfur`, `great` (en eventueel `titan`) uit `tools/test/areas/perf-g2-ring3.txt`. De tabel komt
hier zodra die run klaar is. Referentie INT3 (`teststop-4c.md` §J, warm, ronde 1–2): noise 1,03–1,33× (smeulwoud 1,33,
zwavel 1,22, horizonreus 1,18), surface 1,7–2,5×, features 0,26–1,32×; koud ≤ 1,69. **Inschatting G2:** alle voorstellen
hieronder zijn features of surface-regels; de noise-stap (het budget van 1,75×) verandert niet, behalve bij voorstel R4
optie B (microreliëf herzien).

## 4. Featurevoorstellen

Uitgangspunt (lat ring 4): (1) **grondvulling** zodat geen vlak leeg is, (2) **subzones/bosjes** uit één geseede ruis,
(3) **1 biome-eigen blikvanger** per biome per wereldcel van ~400 (≈ 1 per 300–500 blokken samen met de ringbrede
vormen). Alles chunk-lokaal op `jittered_grid` met `VolcanicFilter` (determinisme zoals nu); perf in de feature-stap.
Complexiteit: S = configuratie van bestaande features, M = nieuwe feature-klasse of sjabloon, L = nieuw systeem.

### Ringbreed
| # | Voorstel | Waar | Dichtheid | Perf | Complexiteit |
|---|---|---|---|---|---|
| R1 | **Grondvulling**: kleine stenen (1–2 blokken, biome-materiaal: puimsteen, basalt, blackstone, tuf) op raster 8, keien (bestaande `rubble`) ≤ 1 per chunk ook op hellingen tot ~45°, puinwaaiers (grind/tuf/blackstone) aan de voet van kliffen en kegelflanken | alle ruige biomes | ~3–4 objecten per chunk | features +0,05–0,1× | S–M |
| R2 | **Subzones**: één ringruis `ring_3_patches` (zoals `ring_4_groves`, golven ~150–300 blokken) die per biome twee vlek-types kiest (andere bodem + andere features), zie per biome | alle 7 sub-biomes | ~40 % van elk biome "anders" | surface +1 ruis (~+0,05×) | M |
| R3 | **Biome-eigen blikvanger** per cel van ~400 (kans ~0,5), `/ca landmarks` noemt ze | per biome (zie hieronder) | ~1 per 550 blokken per biome | template-feature, verwaarloosbaar | M per stuk |
| R4 | **Hoogtelijnen breken**: het 2D-microreliëf (39f) leest van boven als een kaart. A: met features (heuveltjes 2–4 hoog van biome-materiaal, keien, puinwaaiers) de lijnen onderbreken; B: microreliëf zelf herzien (onregelmatiger, in de noise) | as, zeekom, titanen | A ~1 heuveltje per 2 chunks | A features; B noise (risico op het budget) | A M, B L |
| R5 | **Vuurflora buiten het smeulwoud**: een verkoolde struik / asgras als eigen plant op as en basalt (in plukken), zodat de vlaktes iets "levend" hebben | as, pieken, basalt, titanen | plukken, ~1–2 per chunk | verwaarloosbaar | M (+ `blocks`/`textures`, backlog) |

### Per biome
| Biome | Grondvulling / kleine vormen | Subzones (R2) | Blikvanger (R3) |
|---|---|---|---|
| `ashen_wastes` | dode bomen ook in de lijzijde (niet alleen geulwanden), in groepjes van 3–8 met omgevallen stammen; puimsteenkeien in plukken i.p.v. gelijk verspreid; aslaagjes als ribbels op de loefzijde | "verbrand bos" (dicht staande verkoolde staken + stammen, ~1/3 van het biome) ↔ "puimsteenveld" (lichte puimsteenbodem met veel keien) | **verkoolde reuzenstam**: dode 2×2-stam 15–25 hoog met 2–3 afgebroken takken, half in een duin |
| `volcanic_peaks` | blackstone-rotsen en inslagkraters ook op de flanken (helling-filter ruimer); puinwaaiers onder de kammen; magma-ventjes op de kammen | "verse lava" (zwarte blackstone met gloeiende scheuren) ↔ "puinhelling" (tuf/grind) | **obsidiaanstroom**: gestolde obsidiaan-tong 30–60 lang langs een flank, met huilend obsidiaan |
| `dried_seabed` | kelpbalen in plukken half in het zand (niet los bovenop); meer en grotere dode riffen op de kammen; dood koraal ook los in kommen; losse visbotjes (bot) | "kleiplaten" (klei/terracotta met krimpscheuren-patroon) ↔ "schelpenbank" (calciet en bot in het zand) | **walvisskelet**: groot zeedier van bot-blokken (25–35 lang, ribben als bogen), opschaling van de bestaande `sea_serpent`-sjablonen |
| `basalt_flows` | basaltzuiltjes 2–5 hoog in groepjes; blackstone-tongen met 1 blok reliëf (lobben) i.p.v. platte vlekken | "touwlava" (gladde smooth/polished basalt in banden) ↔ "aa-lava" (ruwe blackstone-brokken) | **zuilenorgel**: cluster van zeshoekige basaltzuilen 8–20 hoog |
| `ember_woods` | grotere en meer open plekken (vuurflora, bessen, stammen zichtbaar van boven); variatie in kronen (half verbrande boom met zwarte kroon, kale reus) | bosjes ↔ "brandplekken" (smeulende as zonder bomen, gloeiende stronken) | **vuurreus**: smeulboom met 2×2-stam 20–28 hoog en gloeiende holte in de stam |
| `sulfur_fields` | kookpoelen **onregelmatig** (vorm, straal 1–6, in clusters met lege stukken ertussen) i.p.v. even grote vierkanten op raster | "gele korst" ↔ "kalksinter" (wit calciet met oranje randen) | **sinterterrastrap**: Pamukkale-achtige trap van 4–6 kookpoelterrassen, 20–40 breed |
| `titan_fields` | botten in plukken (bottenvelden) i.p.v. 0,15 los; zielgrond met zielvuurtjes ook los op de vlakte (niet alleen bij ribben) | "bottenveld" ↔ "zielgrondvlakte" | **titanenwapen**: half begraven reuzenzwaard/speer van basalt/polished deepslate, schuin uit de grond (20–30) |
| `dormant_caldera` | azaleastruiken in bosjes (groeiruis sterker); minder rauwe grove-aardevlekken | – | – (het biome is zelf de oase) |
| `steam_fens` | poelen en geisers in clusters; zachtere bodemrand naar de zeekom | – | – (terrein volgt ring 4) |

**Volgorde-advies:** R1 + R2 + de per-biome grondvulling eerst (grootste winst tegen "leeg"), dan de kookpoelen en het
smeulwoud (gerichte fixes), dan de blikvangers (R3). R5 alleen met een `blocks`/`textures`-placeholder (backlog).

## 5. Sparvragen (AskUserQuestion-vorm)

1. **Dichtheid grondvulling (R1)?**
   - **A. Matig, zoals het ring 4-proefstuk (aanbevolen):** kleine stenen raster 8, keien ≤ 1/chunk, puinwaaiers. Vlaktes niet meer leeg, ring 3 blijft een woest land; features +~0,1×.
   - B. Licht: alleen keien en puinwaaiers. Minder werk, de vlaktes blijven vrij kaal.
   - C. Dicht: ook raster 4 en meerdere soorten. Druk beeld, kans op "rommelig", meer determinisme-werk.
2. **Subzones (R2)?**
   - **A. Eén gedeelde ringruis met per biome 2 vlek-types (bodem + features) (aanbevolen):** één systeem, voorspelbaar, ~+0,05× surface.
   - B. Alleen bodemvlekken (surface), geen andere features. Goedkoper, maar de vlek leest alleen als kleur.
   - C. Per biome een eigen ruis en eigen regels. Meeste variatie, meeste werk en tuning.
3. **Biome-eigen blikvangers (R3)?**
   - **A. Eén per biome (7, de lijst hierboven), per cel ~400 (aanbevolen):** haalt "1 per 300–500" binnen elk biome; 7 kleine sjabloon-features.
   - B. Alleen voor de 4 biomes zonder eigen blikvanger (as, pieken, basalt, smeulwoud). Minder werk; zeekom/zwavel/titanen houden wat ze hebben.
   - C. Twee per biome. Rijker, dubbel werk, risico dat ze elkaar "goedkoop" maken.
4. **Flora op de kale vlaktes (R5)?**
   - **A. Eén eigen vuurflora-plant (verkoolde struik/asgras) in plukken (aanbevolen):** de vlaktes "leven"; vraagt een blok + texture (placeholder nu, backlog `blocks`/`textures`).
   - B. Alleen vanilla (dode struik via een tag op as). Geen nieuwe blokken; leest als woestijn, niet als vulkaan.
   - C. Geen flora; alleen stenen en botten. Puur mineraal, past bij "gestolen water", blijft stiller.
5. **Hoogtelijnenpatroon van het microreliëf (R4)?**
   - **A. Breken met features (heuveltjes, keien, puinwaaiers) (aanbevolen):** geen noise-wijziging, budget veilig.
   - B. Het microreliëf zelf herzien in de noise. Beste resultaat van boven, maar raakt de noise-stap en de 39f-afstemming (perf- en regressierisico).
   - C. Zo laten. Van ooghoogte valt het minder op.
6. **Kookpoelen in `sulfur_fields`?**
   - **A. Onregelmatige vormen en maten in clusters (aanbevolen):** haalt het rasterbeeld weg; aanpassing van `TerracePoolFeature`.
   - B. Alleen de dichtheid laten variëren via de subzoneruis. Minder werk, poelen blijven vierkant.
   - C. Zo laten.
7. **Smeulwoud?**
   - **A. Grotere open plekken + variatie in kronen (half verbrand, kale reus) (aanbevolen):** van boven geen tapijt meer, onder de bomen meer licht.
   - B. Alleen grotere open plekken. Eenvoudig (drempel van de open-plekruis).
   - C. Zo laten (het is het "dichte" biome).
8. **`steam_fens` en de grens 3→4 in deze ronde?**
   - **A. Alleen kleine aankleding: poelclusters en een zachtere bodemrand naar de zeekom (aanbevolen):** de harde zand↔modder-lijn verdwijnt; terrein blijft zoals het is.
   - B. Niet in deze ronde (G2 = de 7 sub-biomes; de venen horen bij de overgang naar ring 4).
   - C. Volledig opknappen inclusief terreinvorm. Raakt de ring 4-kant; grotere change.
