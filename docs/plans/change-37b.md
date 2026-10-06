# Change 37b — Textures ring 2 + Nether-ring 2: items, Nether-accenten en afwerking

Scope: `textures` — change 37b (extra change na 37, vóór de rest van teststop 3 / change 38)

**Doel:** de gebruikersfeedback van het teststop-moment van 2026-10-06 verwerken. De textures van change 37 zijn compleet en technisch in orde (controles, animaties, models), maar een deel oogt plat of half getekend, en in Nether-ring 2 past `soaked_netherrack` niet bij de vanilla-ertsen die erin zitten. Deze change herschildert in `tools/textures/Ring2Textures.java` de zwakste items en blokken in vanilla-stijl: licht van linksboven, 5–7 tonen per materiaal met hue-shift in schaduw en hooglicht, een selectieve omlijning en volume bij items. Daarnaast krijgt de Nether-grond een palet waar vanilla-ertsen in passen. Er komen alleen pixels bij (vraag 1 A: geen model-JSON). Er komen geen nieuwe blokken, geen registries en geen gedrag.

Bron:
- de feedback hieronder;
- `change-37.md`, met de besluiten 1–10 en "Afwijkingen bij de bouw". Die besluiten blijven staan, behalve waar een vraag hieronder ze expliciet heropent;
- `fase-1-wereld.md` §1.4 (stijl);
- de audit van 2026-10-06 (zie "Audit").

---

## Feedback van de gebruiker (2026-10-06, letterlijk)

> "Ik wil dat er voor ring 2 nog iets meer focus op de item textures en block textures wordt gedaan. Item textures voelen hier en daar beetje raar/plat aan en bij block textures vooral in de nether merk ik dat het niet goed op elkaar past, denk aan quartz ore met de soaked netherrack. Het matched niet qua accent. Ook zien textures er soms beetje half getekend uit."

Drie punten, in deze volgorde van prioriteit:
1. **Items voelen raar of plat.**
2. **Nether-accenten passen niet bij elkaar.** Het voorbeeld is kwartserts in `soaked_netherrack`.
3. **Textures ogen half getekend.**

---

## Audit (2026-10-06, stand `959249a`)

### Werkwijze
- `java tools/textures/Ring2Textures.java` draait schoon en is idempotent: na de run is `git status` gelijk, alleen met de al bestaande, niet-gerelateerde wijziging in `DewGeodeShapeTest.java` en de ongetrackte `docs/prompts/`. Het contactblad `build/texture-preview/ring2.png` is ververst.
- Vergelijkingsbladen staan alleen in `build/texture-audit/` (niet in git). Ze zijn gemaakt met wegwerp-scripts in dezelfde map. Vanilla-textures komen uit `build/moddev/artifacts/neoforge-21.1.252-client-extra-aka-minecraft-resources.jar` en zijn alleen gelezen en uitgepakt naar `build/texture-audit/vanilla/`:
  - `nether_blocks.png`: elke Nether-ring 2-texture naast zijn vanilla-buur, op 8× en als 3×3-tegeling;
  - `ring2_blocks.png`: idem voor ring 2;
  - `items.png`: elk item op 8×, op GUI-schaal 2× en 1× in een inventaris-slot, en op een lichte achtergrond;
  - `scenes.png`: muren van 7×5 blokken met vanilla-ertsen in `soaked_netherrack` en in vanilla-netherrack, plus een Nether-2-scène en een kloofwand;
  - `options.png` en `mockA.png`: mock-ups van de opties van vraag 1;
  - `frames.png`: de pulsframes.
- **Meetwaarden per texture** (wegwerp-`Flat.java`): het aantal kleuren, het aandeel van de meest voorkomende kleur ("dom"), de spreiding van de luminantie (Lsd) en het gemiddelde.
  - **Referentie vanilla:** 6–9 kleuren en dom 20–37 %. Uitzonderingen: zand 42 %, `sandstone_top` 44 % en `warped_wart_block` 50 %.
  - **Bij ons:** een reeks textures met **dom 50–76 % en 3–4 kleuren**. Dat is de meetbare kant van "half getekend": één tint vult het grootste deel van het vlak.
- **Ertsplaatsing Nether-ring 2** (`NetherRing2Features.java` r. 94–102): `minecraft:ore` met `BlockMatchTest(soaked_netherrack)` en de vanilla-blokken `nether_quartz_ore`, `nether_gold_ore`, `ancient_debris`, `magma_block`, `gravel` en `blackstone`. De groottes en placements zijn kopieën van vanilla. Surface: `spore_moss` op de vloer, de rest is `soaked_netherrack` (`noise_settings/nether.json`), met een lavazee.
  - Vanilla `nether_quartz_ore` is voor **183 van de 256 pixels** exact vanilla-netherrack, `nether_gold_ore` voor **223 van de 256**. `ancient_debris_side` heeft er 0 (een eigen blok). Magma, grind en blackstone zijn volle eigen blokken. **Alleen kwarts en goud tonen dus een "netherrack-vierkant".**
- **Vanilla-buren in ring 2:**
  - surface: `red_sand`, `sand`, `sandstone`, `red_sandstone`, `smooth_sandstone`, `gravel`, `coarse_dirt`, `grass_block`, `terracotta`/`orange_terracotta` en `calcite`;
  - flora: `dead_bush`, `cactus`, `short_grass`/`tall_grass`/`fern`, klaproos, allium, korenbloem, margriet, paardenbloem, azalea, `sweet_berry_bush`, `sugar_cane` en `moss_block`;
  - verder `bone_block` en water.
  - De vanilla-ertsen van ring 2 zitten alleen in vanilla-steen en -deepslate, dus niet in kloofsteen.

### Top-problemen
1. **`soaked_netherrack` ↔ vanilla-ertsen.** Doorweekt netherrack is bruin-kastanje (gemiddelde saturatie 0,43, hue 0–10°, zie het palet hieronder). Vanilla-netherrack is verzadigd rood (saturatie 0,62). Elk kwarts- en gouderts toont daardoor een **helderrood vierkant** in een bruine wand (`scenes.png`). Tegelijk dekken twee tinten 72 % van het blok (`3D2024` 94 px, `52302F` 92 px). Het blok leest daardoor als modder met ruis, niet als klonterig gesteente.
2. **Items hebben een te regelmatig silhouet en geen facetten:**
   - `raw_sunstone` en `raw_turquoise` zijn bijna perfecte bollen ("aardappel", "knikker"). Vanilla's `raw_gold`/`raw_copper` zijn trosjes brokken met glimpunten;
   - `dew_crystal` is een "pil" met afgeronde uiteinden;
   - `salt` is een symmetrisch koepeltje ("broodje");
   - het `slime_stalactite`-item is een brede kegel die het hele slot vult.
3. **Eén omlijningskleur rondom.** Alle eigen items en kristallen hebben één outlinekleur rondom (bijv. `raw_sunstone` 33 randpixels in `462106`). Vanilla gebruikt een **selectieve omlijning**: linksboven een donkere materiaaltint, rechtsonder de donkerste. `raw_gold` heeft bijvoorbeeld boven/links `B26411` + `6D4718` en onder/rechts alleen `6D4718`. Onze items ogen daardoor als stickers.
4. **De puls maakt zonnesteen bruin.** `pulseStrip` dimt de rustfase 22 % naar zwart en mengt de piek 28 % naar de gloeikleur. Frame 0 van `raw_sunstone` en de overlay is daardoor dof bruin, en op de piek vervalt de schaduw.
5. **Nether-ring 2-hout, hoed en lamp zijn het vlakst van de hele set:**
   - `dripshroom_cap` dom 73 %, 4 kleuren;
   - `dripshroom_lamp` dom 65 %, Lsd 0,07 (`shroomlight` 0,17);
   - `stripped_dripshroom_stem` dom 67 %, 3 kleuren;
   - `dripshroom_stem` dom 57 %, verticale "barcode" van kolomruis;
   - de hoed hergebruikt de groene streepjes van de stam en leest daardoor als dezelfde stam in paars.
6. **`dripshroom_planks` is te licht voor de Nether** (gemiddelde 0,59; crimson- en warped-planken 0,3–0,36). Het licht op in een donkere hal als berkenhout. Dit volgt besluit 6 A ("planken lichter grijsgroen").
7. **Deuren en valluiken ogen half getekend** (palm en druipzwam): grote zwarte rechthoeken als ramen, drie zwarte balken als valluik, geen paneelschaduw of beslag.
8. **De ring 2-erts-overlays ogen als stickers:**
   - zonnesteen bestaat uit 4–5 gelijke "eikeltjes" met een volle donkere rand;
   - turkoois bestaat uit 2 grote ronde "snoepjes";
   - vanilla-ertsen hebben grillige vlekjes zonder volle rand.
9. **Half getekende plekken in ring 2:**
   - `scorched_sandstone_top`/`_bottom` hebben losse zwarte roetpixels die op dode pixels lijken;
   - `scorched_sandstone` (zijkant) leest door de lange strepen als houten planken;
   - `canyon_cobblestone` herhaalt dezelfde crème "chip" elke 16 px, de stenen zijn plat en de voeg is breed. Het leest als gebarsten modder, terwijl `cracked_clay` juist als keienpad leest;
   - `salt_crystal` heeft geen donkere rand (in strijd met §1.4) en is op 1× een witte vlek.
10. **Flora en kristalstadia zijn dun en dof:**
    - de dauwstadia ogen grijs-staal in de rustfase en hebben facetten alleen als verticale strepen;
    - `spore_sprouts` en `dry_shrub` hebben 1-px-stengels in lage contrasten;
    - `desert_flower` heeft 1-px-sterarmen (36 pixels, oogt als een vonkje).

---

## Oordeel per texture

Legenda: **P1** = door de gebruiker genoemd (items, Nether-accent) of dom > 60 %; **P2** = half getekend of past niet naast een buur; **P3** = in orde, alleen meeliften als het palet of een gedeelde helper verandert.

Oordelen: *plat* (te weinig tonen of geen lichtrichting), *half* (half getekend: egale vlakken, ruis zonder vorm, losse pixels), *buur* (past niet naast zijn vanilla-buur), *ok*.

### Items (inventaris-sprites) — P1

| Texture | Oordeel | Oorzaak | Aanpak |
|---|---|---|---|
| `item/raw_sunstone` (puls) | plat, raar | `rawLump` tekent een bol (straal 4,5–6 met smoothstep): het silhouet is rond, met een radiale gradiënt, één outlinekleur en geen facetten; de gesteenteresten vallen weg. De rustframe is bruin (top-probleem 4). | Trosje van 2–3 hoekige brokken (zoals `raw_gold`). Per brok facetvlakken: links/boven licht `FFE07A`, rechts/onder `D9801A`→`9A4A0C`, glimpixel `FFF6C8` op een hoek. Kleine stukjes rode kloofsteen in de holtes, selectieve omlijning. Puls: zie §3.4. |
| `item/raw_turquoise` | plat, raar | Zelfde `rawLump`-bol; de crème matrix-band dwars erover leest als "oogbal/planeet". | Knollige tros van 2–3 ronde knobbels van ongelijke grootte, mat (geen glimpixel, zoals besloten in 24-5 A). Bruine adertjes `3A2A1E` alleen tussen de knobbels, crème gesteente als korst aan de onderkant, selectieve omlijning. |
| `item/dew_crystal` (puls) | plat | Diagonale "pil": afgeronde uiteinden, drie verticale banden, 6 kleuren maar weinig contrast in de rustframe. | Puntig prisma zoals `amethyst_shard`: scherpe punt boven, schuine afbraak onder, twee facetvlakken langs de lengte met een lichte rand `F4FCFF` linksboven. Kern `A6D3E0`, schaduw `5E9FB4` met een zweem naar lavendel/blauw (hue-shift), selectieve rand `1B4A5C`/`2E6F86`. Palet blijft besluit 2 A. |
| `item/salt` | plat, raar | Een symmetrische halve ellips (koepel) met een vloeiende gradiënt. | Hoopje **kubusvormige korrels** (zout kristalliseert kubisch): 4–6 blokjes van 2–3 px met een lichte bovenkant, middentint aan de linkerzijde en schaduw rechts, en een onregelmatige hoop-rand. Selectieve rand `8A8070`→`6E6656`. |
| `item/slime_stalactite` | raar | De kegel is 12 px breed en vult het slot. De rand is een vaste outlinekleur, de vulling bestaat uit verticale banden. | Slanke, licht diagonale druppelpegel (max. 6–7 px breed, zoals `pointed_dripstone`), met een ribbel/druipnaad, natte glanslijn links en een druppel aan de punt. |
| `item/palm_door` | half | Volgt `doorTop`/`doorBottom` met zwarte ramen. | Na de nieuwe deur-helper (§3.3): verkleinde deur met paneelschaduw en een klinkpixel. |
| `item/dripshroom_door` | half, plat | Idem, en het palet is laag-verzadigd. | Idem, met het herziene druipzwampalet. |

### Platte blok-items (kruismodellen, ook in de wereld) — P1 (item) / P2 (wereld)

| Texture | Oordeel | Oorzaak | Aanpak |
|---|---|---|---|
| `block/small_/medium_/large_dew_crystal_bud`, `dew_crystal_cluster` (puls) | plat | `dewSpike` tekent alleen verticale strepen (licht \| rug \| schaduw) met een volle rand. De rustframe is grijs-staal (gedimd). | Kristallen met facetten zoals de amethist-stadia: een schuine facetlijn, een lichte linkerflank, een heldere rug, een donkere rechterflank, een glimpixel op elke punt en een onderste rij in de schaduw. Selectieve rand. De vorm blijft (hoogtes uit `blocks`). |
| `block/salt_crystal` | half | Geen donkere rand: §1.4 vraagt een donkere 1-px-omlijning bij kristallen. Wit op wit, dus op 1× een vlek. | Selectieve rand `8C9AA6`/`A9B6C0`, facetlijn, de roze zweem als 1–2 px accent, en een donkerder voet. Still, niet emissive (besluit 3 A). |
| `block/dry_shrub` | plat | 1-px-takjes, alles licht grijs (gemiddelde 0,54). Naast `dead_bush` (0,32) oogt het als een spook. | Een 2 px dikke donkere hoofdtak `5A4630` die oploopt naar grijze twijgen en lichte punten. De signatuur van 10 A (grijs met lichte punten) blijft. |
| `block/desert_flower` | plat | Sterarmen van 1 px (36 pixels in totaal), dunne steel. | Gevulde zespuntige ster: blaadjes 2 px breed bij de basis, schaduw onder/rechts, crème toppen. Turkoois hart van 2×2 met een glimpixel. Twee blaadjes aan de steel. Signatuur 10 A blijft. |
| `block/palm_sapling` | ok/plat | Eenvoudig parapluutje, weinig tonen. | Bladnerf en schaduw onder de bladeren, een stammetje met 2 tonen. P2. |
| `block/spore_sprouts` | plat | Stengels van 1 px in drie grijsgroene tonen dicht bij elkaar. Op sporenmos valt het weg. | Stengels van 1–2 px met een donkere voet en lichte toppen, sporenkopjes van 2×2 met `SPORE_DOT` en een hooglicht. Meer contrast met de mosbodem. |
| `block/glow_mold` | ok | Decal, emissive. | Alleen meeliften (geen wijziging). |

### Nether-ring 2 blokken — P1

| Texture | Oordeel | Oorzaak | Aanpak |
|---|---|---|---|
| `block/soaked_netherrack` (+ `spore_moss_side` volgt via `SIDE_RULES`) | **buur**, half | Bruine kastanjetint (saturatie 0,43) tegenover het verzadigde rood van de vanilla-ertsbasis. Twee tinten dekken 72 %. Fijne ruis zonder leesbare klonten. | Vraag 1. Bij A: een eigen toonladder dicht bij die van vanilla-netherrack (hue ~0°, saturatie ~0,6, luminantie 0,12–0,33), klonten op netherrack-schaal (cel ~4 px) met licht linksboven en een donkere voeg. De eigenheid zit in natte glans (`9A5650`-achtig, alleen op ~⅓ van de klonten), donkere natte poriën en vochtig grijsgroene spikkels (≤ 8 % van de pixels). Mock-up `build/texture-audit/mockA.png`: met alleen de palet-wissel vallen de erts-vierkanten al bijna weg. |
| `block/spore_moss` | half | Dom 50 %, 5 kleuren, losse witte puntjes zonder vorm. | Mos-kussentjes (cel 4–5 px) met licht linksboven, 6 tonen grijsgroen met een warme hue-shift in de hooglichten en een blauwgroene in de schaduw. Sporen als groepjes van 1–3 px in 2 tonen. |
| `block/spore_moss_side` | ok | Volgt de grond. De mosrand is 3–5 px. | Meeliften. De rand krijgt 1–2 hangende plukjes (de regel "onder de rand exact de grond" blijft). |
| `block/dripshroom_stem` (cycling) | plat, half | Kolomruis maakt verticale strepen ("barcode"). Dom 57 %, 5 kleuren. | Licht gedraaide vezels zoals de vanilla-Nether-stammen (diagonale aders), 6 tonen grijspaars met hue-shift. De slijmstrepen volgen de vezel. De `cycling` blijft (16×8). |
| `block/dripshroom_stem_top` | ok | Ringen + rand. | Meeliften met het nieuwe palet (hout en schors). |
| `block/stripped_dripshroom_stem`, `_top` | half | 3–4 kleuren, dom 55–67 %. | Vezels met 5 tonen, een paar slijmglanzen. Kopse kant met ringen. |
| `block/dripshroom_planks` | plat, **buur** (te licht) | 4 kleuren en een gemiddelde van 0,59: licht als berkenhout in een donkere Nether. | Vraag 4. Planken met 5–6 tonen, nerf en spijkerpixels zoals de vanilla-indeling. Bij A een donkerder palet. |
| `block/dripshroom_door_top/_bottom`, `_trapdoor` | half | Zwarte ramen, drie zwarte balken. | Nieuwe deur-helper §3.3. |
| `block/dripshroom_cap` | half | Dom 73 %, 4 kleuren. De streepjes zijn dezelfde als op de stam. | Vlezige knobbels (cel 4–5 px, zoals de wart blocks) in 5 paarse tonen met licht linksboven. Slijmdruppels als ronde 1–2 px-vlekjes met een hooglicht in plaats van streepjes. |
| `block/dripshroom_lamp` | plat | Dom 65 %, Lsd 0,07. De celranden zijn amper donkerder. | Cellen zoals bij `shroomlight`: olijfgroene celwanden (`6E8A3A`→`8FA84E`), limoen-celkernen met een bijna witte kern `FAFFE0`, Lsd ≥ 0,14. Licht 15 blijft. |
| `block/slime_stalactite_*` (10) | half | Rechte kolom met willekeurige 1-px-kartels per rij, fel limoen (gemiddelde 0,67) tegen een donkere wereld. | Gladde taps met 1–2 ribbels, een natte glanslijn links en een donkere rechterflank. Palet 1 stap donkerder en minder fel. Breedtes blijven gelijk (vormen uit `blocks`). |
| `block/dew_source` (cycling) | ok | Verzadigd blauw, golft. | P3: alleen de verzadiging iets lager als vraag 1 A het grondpalet donkerder/roder maakt. |

### Ring 2 blokken — P2/P3

| Texture | Oordeel | Oorzaak | Aanpak |
|---|---|---|---|
| `block/sunstone_ore_overlay` (puls) | half, sticker | 4–5 gelijke achthoekjes met een volle rand `5A2A08`; dof in de rustframe. | P2. Grillige brokjes van 2–6 px in verschillende maten (1 groot, 2–3 klein). Facet licht linksboven, selectieve rand (donker goud alleen onder/rechts), glimpixel. Puls volgens §3.4. Leesbaar op de 4 bases (de ertsrij op het contactblad). |
| `block/turquoise_ore_overlay` | half, sticker | 2 grote ronde knobbels met een volle rand. | P2. 3–4 kleinere, onregelmatige knolletjes met een bruine ader en zonder volle rand. Mat. |
| `block/canyon_cobblestone` | half | Stenen met 3 tonen (licht geklemd op 0,25–0,75), een brede voeg (gap < 0,7) en 1 crème gast-steen die elke 16 px terugkomt. | Bolle stenen met 4–5 tonen per steen (hooglicht linksboven, schaduw rechtsonder), een voeg van 1 px en 2 kleine gast-stenen met minder contrast (crème naar oranje gemengd). Het besluit 8 A (dominant oranje, warme voeg) blijft. |
| `block/cracked_clay` | buur | 2 px brede scheuren en platen in één toon, waardoor het als keienpad leest. | Scheuren van 1 px, platen met een opgekrulde lichte rand en een holle schaduw in het midden. Naadloos in 4 rotaties (blijft zo). |
| `block/scorched_sandstone`, `_top`, `_bottom` | half | Losse zwarte roetpixels; lange horizontale strepen op de zijkant (leest als hout). | Roet als zachte vlekken van 2–3 tonen die van onder opstijgen, zonder losse zwarte pixels. Zijkant: kortere laagjes met korrel en een lichte bovenband zoals vanilla-zandsteen. |
| `block/cut_/chiseled_scorched_sandstone` | ok | Rand + vlak / ruitmotief. | Meeliften met de roet-aanpak. |
| `block/canyon_stone_bricks` | buur | De indeling van vanilla-`bricks` (8×4-stenen): leest als kleibakstenen, niet als steenbakstenen. | Vraag 5. |
| `block/palm_log` | plat | Ringen precies elke 4 px: een barcode op lange stammen. | Ringen om de 3–5 px met verspringende bladlittekens (ruitpatroon). |
| `block/palm_log_top`, `stripped_palm_log_top` | half | Het zandkleurige midden leest als zandsteen-top. | Vezelige stippen met meer contrast, een donkerder hart en een schorsrand van 1–2 px. |
| `block/palm_door_top/_bottom`, `palm_trapdoor` | half | Zie de deuren hierboven. | Nieuwe deur-helper §3.3. |
| `block/salt_crust_top`, `salt_block`, `polished_salt`, `salt_crust_side` | ok (bewust rustig, 9 A) | `salt_crust_top` dom 76 %, 3 kleuren: bewust rustig. | P3, geen wijziging. Alleen `polished_salt` krijgt optioneel een tweede bevel-ring (dom 66 %). |
| `block/red_/orange_/cream_canyon_stone`, `loose_sand`, `palm_planks`, `stripped_palm_log`, `palm_leaves`, `barrel_cactus_*` | ok | Passen naast `red_sand`, `red_sandstone`, `terracotta`, `sand` en `cactus`. | P3, geen wijziging. |
| `particle/sand_gust_0..3` | ok | — | Geen wijziging. |

---

## Afhankelijkheden en aannames

- **Stand:** `959249a` (change 37 + `soaked_netherrack`-herschildering). De besluiten van 37 (1–10) blijven staan, behalve 6 A voor de planken (vraag 4) en de stijlregel "kristallen met een donkere 1-px-omlijning" in §1.4 (vraag 3). Die worden hier alleen heropend als de gebruiker dat kiest.
- **Eigen pixels, geen vanilla-pixels in de repo (§1.4).** Vanilla mag alleen als referentie dienen. Models mogen naar vanilla-textures *verwijzen* (relevant voor vraag 1 B). Een vergelijkingsblad dat vanilla uit de moddev-jar leest en alleen naar `build/` schrijft, is referentiegebruik (vraag 6).
- **Vormen liggen vast** (`blocks`): cactus, zoutkristal, dauwstadia, stalactietbreedtes. Deze change tekent binnen die vormen.
- **Animaties blijven:** zelfde frames/frametime/interpolate, zelfde `.mcmeta`'s (9). Alleen `pulseStrip` krijgt een andere dim-/glow-weging (§3.4). Dat raakt alleen ring 2: `Ring1Textures.java` heeft een eigen kopie.
- **`SIDE_RULES` blijft:** `spore_moss_side` en `salt_crust_side` volgen hun grond automatisch.
- **MapColor:** kiest vraag 1 A of B, dan klopt `MapColor.TERRACOTTA_BROWN` van `soaked_netherrack` niet meer (beter `MapColor.NETHER`). Dat is `blocks` en gaat naar de backlog. Hetzelfde geldt voor `dripshroom_planks` bij vraag 4 A, als de kaartkleur sterk afwijkt.
- **Werkboom:** `DewGeodeShapeTest.java` (gewijzigd) en `docs/prompts/` (ongetrackt) horen niet bij deze change. Niet meecommitten.

---

## Plan

### 1. Volgorde (in kleine, afgeronde stappen; na elke stap het script draaien en het contactblad bekijken)

1. **Gedeelde helpers** (§3.1–3.4): selectieve omlijning, facet-licht, deur-helper, puls-weging. Daarna draait het script, en alleen de bewust geraakte PNG's veranderen.
2. **Nether-accent (P1):** `soaked_netherrack` volgens vraag 1, daarna `spore_moss`(_side). Controle met de muur "vanilla-ertsen in soaked" (§4).
3. **Items (P1):** `raw_sunstone`, `raw_turquoise`, `dew_crystal`, `salt`, `slime_stalactite`, en de deur-items na stap 5. De omvang volgt vraag 2.
4. **Nether-ring 2 vlakke blokken (P1):** hoed, lamp, stam, gestript, planken (vraag 4), stalactieten, en de dauwstadia (ook item).
5. **Deuren en valluiken** (palm + druipzwam) met de nieuwe helper.
6. **Ring 2 (P2), volgens vraag 2/5:** erts-overlays, `canyon_cobblestone`, `cracked_clay`, de roet-aanpak van `scorched_sandstone`, `palm_log`(_top), `salt_crystal`, en de flora (`dry_shrub`, `desert_flower`, `palm_sapling`, `spore_sprouts`).
7. **`HAND_EDITED`:** blijft leeg, tenzij de gebruiker na het contactblad pixels met de hand wil bijwerken.

### 2. Paletten (voorstellen; definitief na het contactblad, daarna vastleggen in `fase-1-wereld.md`)

| Set | Nu | Voorstel | Toelichting |
|---|---|---|---|
| Doorweekt netherrack (vraag 1 A) | `2A1618 3D2024 52302F 6A423E 84574D` + glans `A38F82`, vocht `34402F` | `3E1415 4C191A 5A2021 692828 793332` + natte porie `2A0E10`, glans `9A5650`, vocht `3A4634` | Hue en saturatie van vanilla-netherrack, iets donkerder; eigenheid via glans en vocht. Mock: `build/texture-audit/mockA.png`. |
| Sporenmos | `3E4A3C 556450 6E7F66 8C9A80` + `C8D4B0` | 6 tonen: `34403A 45533F 586A4E 6E8160 879877 A3B28C` + sporen `C8D4B0`/`E2EAC8` | Kussentjes; schaduw blauwgroen, hooglicht warm. |
| Druipzwam-schors | `3A3440 4E4A58 66667A` + slijm `5E8466 7FA88A` | `2C2734 3A3444 4B4558 5E5A6E 747286` + slijm `4E7458 6E9A78 9CC4A2` | Diagonale vezels; hue-shift naar blauw in schaduw. |
| Druipzwam-hout (vraag 4 A) | `6E7A6A 87947F A0AC94 B8C2A8` | `48523F 5A654F 6D7962 818E74 97A388` | Gemiddelde ~0,40 i.p.v. 0,59; zelfde hue. |
| Druipzwam-hoed | `4A3F55 5F516C 75688A` + `9FC3A8` | `3A3046 4A3F58 5D506E 72648A 8A7CA4` + druppels `7FA88A`/`B8DCC0` | Knobbels met licht linksboven. |
| Zwamlamp | `B8D27A CFE38F E3F0AE F4FAD2` | wand `6E8A3A 8FA84E`, cellen `B8D27A CFE38F E3F0AE`, kern `FAFFE0` | Contrast zoals bij `shroomlight`. |
| Slijmstalactiet | `4E8A4A 6AA860 8CC47A B2DE9E` | `3E6E3E 528A4E 6AA460 86BC78 A8D896` | Eén stap donkerder, glanslijn `C8ECB4`. |
| Zonnesteen (overlay/item) | `5A2A08 9A4A0C D9801A F2B23A FFE07A FFF6C8` | gelijk; rand alleen onder/rechts (`5A2A08`), boven/links `9A4A0C` | Puls §3.4, zodat de rustframe goud blijft. |
| Turkoois | `0E3B3A 1C6B66 2E9C92 4FC6B5 8FE6D6` + ader `3A2A1E` | gelijk; rand selectief | Vorm herzien, palet blijft. |
| Dauwkristal | `5E9FB4 82BACB A6D3E0 D6EEF4 F4FCFF` + rand `1B4A5C` | gelijk + schaduw-hue-shift `5A8CB8` + tweede randtoon `2E6F86` | Besluit 2 A blijft. |
| Zout (item/kristal) | `B8AE9A … F7F5EF`, rand `8A8070`; kristal zonder rand | item-rand `8A8070`/`6E6656`; kristal-rand `8C9AA6`/`A9B6C0` | §1.4-omlijning voor het kristal. |
| Overige sets | — | ongewijzigd | Alleen de tekenmethode verandert. |

### 3. Script `tools/textures/Ring2Textures.java`

1. **Selectieve omlijning** `outlineSelective(image, light, dark)`: randpixels die boven of links aan transparant grenzen krijgen `light` (een donkere materiaaltint), randpixels onder en rechts krijgen `dark`. Dit vervangt `outline(image, color)` voor items en kristallen (vraag 3). De oude `outline` blijft bestaan voor wat niet verandert.
2. **Facet-licht** `facet(dx, dy, nx, ny)`: één helper die per facetvlak een toon kiest uit de lichtrichting linksboven (licht = `-(nx + ny)`), plus een glimpixel op de hoek die het dichtst bij het licht ligt. Die wordt gebruikt door de ruwe brokken, kristalstadia, erts-overlays, zoutkorrels en het dauwkristal.
3. **Deur-helper** `doorPanel` (vervangt `window`/`rail` in `doorTop`/`doorBottom`/`trapdoor`/`doorItem`):
   - frame met lichte binnenrand linksboven en schaduw rechtsonder;
   - ramen met een binnenschaduw (2 donkere tonen in plaats van zwart) en een lichte vensterbank;
   - één scharnier- en klinkpixel;
   - valluik als rooster van 2×2 openingen met schaduw in plaats van drie zwarte balken.

   De gaten blijven cutout (alpha 0) waar ze nu ook doorzichtig zijn; alleen de inkleuring rond de gaten verandert.
4. **Puls-weging** in `pulseStrip`:
   - dim maximaal 10 % (nu 22 %) en naar een donkere materiaaltint in plaats van zwart (per `Animation` een `dim`-kleur, bijv. `9A4A0C` voor zonnesteen en `5E9FB4` voor dauw);
   - de piek mengt alleen de midden- en lichte tonen naar de gloeikleur (outline en schaduw blijven), zodat het volume op de piek zichtbaar blijft;
   - het alpha-masker blijft gelijk, net als het aantal frames, de frametime en interpolate.
5. **Rij-gebaseerde ruis eruit:** `dripshroomStem` gebruikt geen `columns[x]` meer, maar diagonale vezels (offset `x + y/3` met wrap). `canyonStone` en `scorchedSandstoneSide` gebruiken kortere strepen.
6. **Vlakheidscontrole (lint)** na het tekenen. Per texture worden het aantal kleuren, het dom-aandeel en (bij items) het aantal omlijningstonen geprint. Het script **waarschuwt** (het faalt niet) bij:
   - een opaak blok met < 5 kleuren of dom > 50 %;
   - een item of kruismodel met < 5 kleuren;
   - een item met één omlijningskleur.

   Uitzonderingen staan in een `FLAT_OK`-set met reden: `salt_crust_top` (9 A), `loose_sand` (zandkorrel), `glow_mold` (decal), `polished_salt`. De waarschuwingen komen ook bovenaan het contactblad.
7. **Contactblad** `build/texture-preview/ring2.png`: extra rijen:
   - "items in slot": elk item op 1×, 2× en 3× op slotgrijs `8B8B8B` en op de lichte inventarisachtergrond `C6C6C6`;
   - "deuren": beide helften gestapeld, met het item ernaast;
   - "Nether-wand": 7×5 `soaked_netherrack` met `spore_moss_side` bovenop, de stam, hoed, lamp, planken en een dauwbron (alleen eigen textures).

   De vanilla-vergelijking zit in een apart hulpmiddel (vraag 6), omdat `Ring2Textures` geen vanilla-pixels leest.
8. **Ongewijzigd:** de bestandsnamen, het aantal PNG's (70 + 4 particles), de `.mcmeta`'s (9), `SIDE_RULES`, de `cycling`-controles en de seeds per bestandsnaam. Een nieuwe tekenmethode krijgt dezelfde seed, dus een herhaalde run blijft byte-identiek.

### 4. Vergelijkingsblad (alleen bij vraag 6 A)

`tools/textures/VanillaCompare.java`: single-file Java, alleen JDK, buiten Gradle.
- Leest `build/moddev/artifacts/neoforge-*-client-extra-aka-minecraft-resources.jar` (via `java.util.zip`, alleen lezen) en de eigen PNG's.
- Schrijft `build/texture-preview/ring2-vs-vanilla.png` met:
  - paren eigen ↔ vanilla-buur (de tabellen hierboven);
  - de muur "vanilla-ertsen in `soaked_netherrack`" naast dezelfde muur in vanilla-netherrack;
  - de kloofwand met rood zand, grind en grove aarde;
  - items naast `raw_gold`, `raw_copper`, `amethyst_shard`, `quartz`, `sugar`, `pointed_dripstone` en deuren;
  - flora naast `dead_bush`, `poppy`, `amethyst_cluster` en `warped_roots`.
- Geen vanilla-pixels in `src/` of in git.
- Ontbreekt de jar, dan meldt het script "draai eerst `./gradlew build`" en stopt het.

### 5. Model-wijzigingen

- **Bij vraag 1 A, C of D: geen.**
- **Alleen bij vraag 1 B:** `ModBlockStateProvider` geeft `soaked_netherrack` een tweelaags model: `minecraft:block/netherrack` als onderlaag (verwijzing) plus een eigen cutout-overlay `soaked_netherrack_overlay` (glans, vocht, donkere poriën). Dit volgt het voorbeeld van `oreWithOverlay`, maar zonder emissive.
  - `spore_moss_side` krijgt dan hetzelfde opbouwschema: netherrack + natte overlay + mosrand-overlay.
  - `SIDE_RULES` verandert: de mosrand komt op de overlay, en de regel controleert dat de overlay onder de rand gelijk is aan `soaked_netherrack_overlay`.
  - Gevolg: twee quads per zichtbaar vlak op het meest voorkomende blok van de ring, met `render_type` cutout. Daarna `runData`.

### 6. Documentatie

- `fase-1-wereld.md`:
  - §1.4 aanvullen:
    - selectieve omlijning voor items en kristallen (bij vraag 3 A);
    - pulsweging: dim ≤ 10 % naar een materiaaltint, piek alleen op de midden- en lichte tonen;
    - de vlakheidscontrole;
    - het vergelijkingsblad (bij vraag 6 A);
  - de ring 2- en Nether-ring 2-texture-secties met de nieuwe paletten.
- `CODEBASE.md`:
  - tooling-tabel `Ring2Textures.java`: nieuwe helpers, de lint, contactbladrijen;
  - eventueel een nieuwe regel `VanillaCompare.java`;
  - de textures-regel (37b);
  - een valkuil: "vanilla-Nether-ertsen zijn voor 72–87 % netherrack-pixels: een eigen Nether-grond moet die toonladder volgen, anders ontstaat een erts-vierkant";
  - "Laatst bijgewerkt".
- `02-beslissingenlog.md`: één regel per beantwoorde vraag.
- `docs/backlog.md`:
  - `[blocks] soaked_netherrack MapColor → NETHER (na 37b vraag 1 A/B)`;
  - bij vraag 1 A/D: `[blocks]+[worldgen] eigen kwarts- en gouderts op soaked-basis, als de rest-naad in teststop 3 blijft storen`;
  - de backlogregels van change 37 (r. 149–150) bijwerken: de tweede paletronde is (deels) 37b.

---

## Client/server-impact

Bij vraag 1 A/C/D gaat het alleen om assets (PNG's). Bij 1 B komt er een model-JSON uit datagen bij. Een dedicated server leest geen van beide. Er verandert geen common- of clientcode en er komt niets in `client/` bij. De scripts staan buiten `src/` en komen niet in de jar. Het vergelijkingsblad leest de moddev-jar alleen lokaal en schrijft naar `build/`. Er is geen netwerk, geen staat en geen invloed op worldgen of determinisme.

Prestatie:
- bij 1 A/C/D gelijk;
- bij 1 B twee quads per vlak voor `soaked_netherrack` (cutout), wat meetbaar kan zijn in grote zwamkathedralen.

---

## Tests

- [ ] **Script:**
  - `java tools/textures/Ring2Textures.java` draait zonder fout; de controles slagen (afmeting, alpha 0/255, alpha-masker per frame, `SIDE_RULES`, `cycling` opaak);
  - de lint-waarschuwingen zijn leeg, op `FLAT_OK` na. Lijst de waarschuwingen in "Afwijkingen bij de bouw";
  - tweede run: `git status` en md5 zijn byte-identiek (ook de `.mcmeta`'s);
  - `Ring1Textures.java` en de ring 1-PNG's zijn ongewijzigd;
  - `git diff --stat` raakt alleen de bedoelde PNG's (de lijst per vraag 2/5) en het script.
- [ ] **Contactblad en vergelijkingsblad aan de gebruiker voorleggen vóór de game.** Feedback daarop is een iteratieronde binnen deze change. Bekijk vooral:
  - de rijen "items in slot", "deuren" en "Nether-wand";
  - bij vraag 6 A `ring2-vs-vanilla.png`: de muur met vanilla-ertsen in `soaked_netherrack` moet geen helderrood vierkant meer tonen (alleen een vage naad bij A);
  - de ertsrij met zonnesteen en turkoois op alle bases.
- [ ] **B:** `./gradlew build` slaagt (incl. `ClientIsolationTest`).
- [ ] **D:** `./gradlew runData` slaagt; de `git diff` van `src/generated` is leeg (bij 1 B: alleen het `soaked_netherrack`- en `spore_moss`-model).
- [ ] **runClient:** de log bevat geen `Missing textures`, `Unable to load model` of `Unable to parse animation metadata` voor `corruptedadventure`; F3+T gaat zonder fouten.
- **S (runServer):** niet nodig bij 1 A/C/D (alleen assets). Bij 1 B wel een client-join op `runServer` (`level-name=smoke-change37b`) als rooktest van het model.

### In-game checklist (W) — seed `20260930`, preset `corrupted_world`, creatief, GUI-schaal 2 en 3

1. **Ertsen in de Nether-grond (hoofdpunt):** `/ca tpring 2 nether`, ga op een open vloer staan.
   - `/fill ~2 ~ ~3 ~8 ~4 ~3 corruptedadventure:soaked_netherrack`
   - `/setblock ~3 ~1 ~3 minecraft:nether_quartz_ore`, `/setblock ~5 ~1 ~3 minecraft:nether_gold_ore`, `/setblock ~7 ~1 ~3 minecraft:ancient_debris`
   - `/setblock ~3 ~3 ~3 minecraft:magma_block`, `/setblock ~5 ~3 ~3 minecraft:gravel`, `/setblock ~7 ~3 ~3 minecraft:blackstone`
   - Ter vergelijking ernaast: `/fill ~2 ~ ~5 ~8 ~4 ~5 minecraft:netherrack` met dezelfde ertsen op `~5`.

   Beoordeel van dichtbij en op 10 en 30 blokken, met mipmap 0 en 4, bij lavalicht en in het donker. Kwarts en goud mogen geen opvallend rood vierkant meer tonen. Daarna in een natuurlijke wand: `/locate biome corruptedadventure:dripshroom_forest`, `/tp` erheen en zoek kwarts-aders in de hal-wanden. `soaked_netherrack` moet nog steeds herkenbaar "nat" zijn naast vanilla-netherrack (glans, vocht).
2. **Items in de inventaris:**
   - `/give @s corruptedadventure:raw_sunstone`, `raw_turquoise`, `salt`, `dew_crystal`, `slime_stalactite`, `palm_door`, `dripshroom_door`, `dry_shrub`, `desert_flower`, `palm_sapling`, `spore_sprouts`, `salt_crystal`, `small_dew_crystal_bud`, `dew_crystal_cluster`;
   - daarnaast `/give @s minecraft:raw_gold`, `raw_copper`, `amethyst_shard`, `quartz`, `sugar`, `pointed_dripstone`, `jungle_door`, `warped_door`, `dead_bush`, `poppy`, `amethyst_cluster`.

   Bekijk ze in de hotbar, in de inventaris, in de hand (eerste persoon) en als gedropt item (Q: de zijkanten van de 3D-extrusie tonen de omlijning). Ze moeten niet plat of als sticker ogen naast vanilla. `raw_sunstone` en `dew_crystal` blijven goud/helder in de rustfase van de puls.
3. **Nether-ring 2-set:**
   - `/fill ~2 ~ ~2 ~6 ~3 ~2 corruptedadventure:dripshroom_planks`, en `/setblock`'s van `dripshroom_stem`, `stripped_dripshroom_stem`, `dripshroom_cap`, `dripshroom_lamp`, `dripshroom_door` (onder- en bovenhelft, open/dicht) en `dripshroom_trapdoor` op een `soaked_netherrack`-vloer;
   - naast `crimson_planks`/`warped_planks`, `nether_wart_block`/`warped_wart_block` en `shroomlight`;
   - de planken lichten niet meer op als berkenhout (vraag 4); hoed en lamp hebben volume;
   - de stam-animatie loopt zonder sprong; in een zwamkathedraal (`/tp @s 250 70 433`) oogt de set als één familie.
4. **Stalactieten en dauw:** `/locate biome corruptedadventure:spore_caves`, `/tp @s -319 60 384`. Bekijk stalactieten in alle diktes (hangend en staand), en een dauwgeode met stadia die pulseren zonder grijs te worden.
5. **Ring 2:** `/ca tpring 2`, `/locate biome corruptedadventure:canyonlands`.
   - een keienveld en rotsval naast een kloofwand: `canyon_cobblestone` heeft geen 16-px-herhaling meer en leest als keien;
   - een `dry_riverbeds`-bedding: `cracked_clay` leest als gebarsten klei, niet als keienpad;
   - `hoodoo_fields`: geen zwarte losse pixels op `scorched_sandstone`, en de zijkant leest als zandsteen, niet als hout;
   - ertsen: `/give @s corruptedadventure:red_canyon_sunstone_ore` (en de andere 6 varianten), en op een wand plaatsen naast `minecraft:gold_ore`;
   - palmdeur en -valluik: `/setblock` naast `jungle_door`;
   - zoutkristal: `/setblock ~2 ~ ~ corruptedadventure:salt_crystal` op `salt_crust`, met een zichtbare rand.
6. **Stijl naast ring 1:** zielkristal, zonnesteen en dauwkristal in de inventaris zijn drie duidelijk verschillende kristallen. Ring 1-textures zijn ongewijzigd.

**Blokkerend: nee** (alleen assets). Het visuele oordeel valt in teststop 3 (sessie 1 punt 1 en sessie 2 punt 8 van `change-37.md`); deze W-lijst wordt daar het textures-deel.

---

## Risico's / aandachtspunten

- **Rest-naad bij vraag 1 A.** Eigen pixels kunnen vanilla's netherrack nooit exact volgen. Het mock-up toont een vage naad rond kwarts en goud: veel minder dan nu, maar niet nul. Alleen B en C geven een exacte aansluiting.
- **Eigenheid van `soaked_netherrack` (A/B).** Komt het grondpalet dicht bij vanilla, dan moet "doorweekt" uit glans, vocht en mos komen. Het W-punt "duidelijk anders dan vanilla-netherrack" (change 37) wordt daarmee "herkenbaar nat naast vanilla-netherrack".
- **Puls-weging** raakt alle pulserende ring 2-textures (zonnesteen + dauw, 7 `.mcmeta`-strips). Vergelijk vóór en na op het contactblad. Ring 1 blijft ongewijzigd (eigen script).
- **Deur-helper:** het alpha-masker van deuren en valluiken mag niet verschuiven ten opzichte van de modellen. De gaten blijven op dezelfde plek of worden alleen kleiner. Controleer in runClient beide scharnieren en open/dicht.
- **Kristallen en flora zijn ook wereldtextures:** een andere tekening verandert ook het uiterlijk in de wereld (kruismodellen). Beoordeel ze in de wereld, niet alleen in de inventaris.
- **Programmer art blijft een risico.** Plan 1–2 iteratierondes op het contactblad; daarna `HAND_EDITED` voor wat met de hand beter gaat.

---

## Ontwerpvragen

Ronde 1: vragen 1–3 (wat de gebruiker noemde). Ronde 2: vragen 4–6.

### 1. Hoe lossen we de mismatch tussen vanilla-ertsen en `soaked_netherrack` op?
Vanilla `nether_quartz_ore` en `nether_gold_ore` bestaan voor 72–87 % uit vanilla-netherrack-pixels. In de bruine `soaked_netherrack` vormen ze daardoor een rood vierkant. Ertsen worden geplaatst met `BlockMatchTest(soaked_netherrack)` in `NetherRing2Features`.
- **A. Het palet van `soaked_netherrack` naar de toonladder van vanilla-netherrack brengen** (eigen pixels: hue ~0°, saturatie ~0,6, iets donkerder). "Doorweekt" komt dan uit natte glans, donkere poriën en vochtig grijsgroene spikkels. **(aanbevolen)**
  - Werk: één painter en een palet, binnen `textures`; `spore_moss_side` volgt automatisch.
  - Speelgevoel: de ertsen zitten rustig in de wand. Er blijft een vage naad (mock-up `build/texture-audit/mockA.png`), en de Nether-ring 2 leest meer als "Nether, maar nat".
  - Backlog: `[blocks] MapColor → NETHER`.
- **B. Tweelaags model: `minecraft:block/netherrack` als onderlaag (verwijzing, toegestaan door §1.4) plus een eigen natte overlay.**
  - Ertsen sluiten exact aan, binnen `textures` (+ datagen-model, zoals `oreWithOverlay`).
  - Wel twee quads per vlak op het meest voorkomende blok van de ring, cutout, en `SIDE_RULES`/`spore_moss_side` moeten mee om.
  - De eigenheid zit alleen in de overlay.
- **C. Eigen erts-varianten op soaked-basis** (`soaked_quartz_ore`, `soaked_gold_ore` met eigen overlays). Dat zijn nieuwe blokken met loot, tags (`#minecraft:guarded_by_piglins`, `#c:ores`, mineable), XP en lang, plus een worldgen-wissel.
  - Perfecte aansluiting, en `soaked_netherrack` houdt zijn bruine eigen kleur.
  - Valt buiten `textures`: `blocks` + `worldgen` (+ `textures`) als 2–3 eigen changes vóór 38, en in 37b alleen een backlogregel.
- **D. Ertsen in "droge" netherrack-zakken laten spawnen** (de feature legt een klont vanilla-netherrack rond de ader).
  - `worldgen`-change; de ertsen passen dan bij hun directe omgeving.
  - Wel rode vlekken in een bruine wand (het probleem verschuift), plus aandacht voor de determinismeregel van Nether-ring 2.

Vraag: Welke oplossing kies je voor kwarts- en gouderts in `soaked_netherrack`?

Antwoord: **A** (2026-10-06) — palet van `soaked_netherrack` naar de toonladder van vanilla-netherrack (eigen pixels: hue ~0°, saturatie ~0,6, iets donkerder); "doorweekt" via natte glans, donkere poriën en vochtspikkels. Geen modelwijziging. Backlog: `[blocks]` MapColor → `NETHER`; de bestaande backlogregel over eigen erts-varianten (change 35) blijft staan als terugvaloptie als de rest-naad in teststop 3 stoort.

### 2. Hoe ver gaat de herschildering van items en blokken in 37b?
- **A. Alles met oordeel *plat*, *half* of *buur*:**
  - alle 7 items;
  - de 9 platte blok-items/kruismodellen (dauwstadia ×4, zoutkristal, dorre struik, woestijnbloem, palm-zaailing, sporenspruiten);
  - Nether-ring 2-hoed, lamp, stam, gestript, planken, stalactieten en deuren;
  - ring 2: de erts-overlays, `canyon_cobblestone`, `cracked_clay`, het roet van `scorched_sandstone`, `palm_log`(_top) en de palmdeur.

  Ongeveer 50 PNG's, de rest blijft. **(aanbevolen)**
  - Werk: groot, maar het meeste deelt 3–4 nieuwe helpers.
  - Speelgevoel: één consistente stijl in heel ring 2; de feedback is in één keer afgedekt.
- **B. Alleen wat de gebruiker noemde:**
  - de 7 items en de platte blok-items;
  - het Nether-accent (vraag 1) en de vlakke Nether-ring 2-blokken (hoed, lamp, stam, gestript, planken, stalactieten, deuren).

  Ring 2-overworld-blokken (P2) gaan naar de backlog.
  - Werk: ~30 PNG's.
  - Risico: de half getekende ring 2-plekken (roetpixels, keienherhaling, stickerertsen) komen terug in teststop 3.
- **C. Alleen de items (7) en de mismatch van vraag 1.**
  - Werk: het kleinst.
  - De "half getekend"-feedback over blokken blijft grotendeels liggen.

Vraag: Welke omvang krijgt 37b?

Antwoord: **A** (2026-10-06) — volledige omvang (~50 PNG's): alle items, de platte blok-items, de vlakke Nether-ring 2-blokken en de P2-plekken van ring 2; P3 blijft ongewijzigd.

### 3. Mogen items en kristallen een selectieve omlijning krijgen (vanilla-stijl), in plaats van één donkere rand rondom?
§1.4 schrijft voor kristallen "een donkere 1-px-omlijning" voor. Onze items hebben één outlinekleur rondom. Vanilla gebruikt boven en links een donkere materiaaltint en onder en rechts de donkerste tint.
- **A. Ja: selectieve omlijning voor items, kristallen en erts-overlays.** §1.4 wordt aangevuld; ring 1 (zielkristal) blijft zoals het is tot een eigen ronde. **(aanbevolen)**
  - Items krijgen volume en lezen als vanilla.
  - Kleine afwijking tussen ring 1 en ring 2 tot ring 1 meegaat (backlogregel `[textures]`).
- **B. Ja, en ring 1 (zielkristal-item en -overlay) gaat in 37b mee.**
  - Gelijke stijl over beide ringen.
  - Wel `Ring1Textures.java` openen (het plan van 37 hield ring 1 byte-identiek) en een tweede script raken.
- **C. Nee: de volle donkere rand blijft; alleen silhouet en facetten verbeteren.**
  - Geen regelwijziging.
  - Het sticker-effect blijft deels.

Vraag: Welke omlijningsregel geldt vanaf 37b?

Antwoord: **A** (2026-10-06) — selectieve omlijning (boven/links een donkere materiaaltint, onder/rechts de donkerste) voor items, kristallen en erts-overlays van ring 2 en Nether-ring 2; §1.4 wordt aangevuld. Ring 1 (zielkristal) volgt later: backlog `[textures]`.

### 4. Mogen de druipzwam-planken donkerder (heropent deels besluit 6 A "planken lichter grijsgroen")?
De planken hebben een gemiddelde lichtheid van 0,59; crimson- en warped-planken zitten op 0,30–0,36. In een donkere hal lichten ze op als berkenhout.
- **A. Zelfde grijsgroene tint, ~2 stappen donkerder (gemiddelde ~0,40) en 5–6 tonen met nerf.** **(aanbevolen)**
  - Past in de Nether-waardenreeks en blijft anders dan crimson, warped, dood hout en palm.
  - Mogelijk een `MapColor`-backlogregel (`blocks`).
- **B. Zoals besloten (licht), alleen meer tonen en nerf.**
  - Besluit 6 A blijft letterlijk staan.
  - Het "oplichten" in de Nether blijft.
- **C. Iets donkerder (gemiddelde ~0,50) als middenweg.**
  - Minder opvallend, maar nog steeds lichter dan de vanilla-Nether-houtsoorten.

Vraag: Hoe licht worden de druipzwam-planken?

Antwoord: **A** (2026-10-06) — zelfde grijsgroene tint, ~2 stappen donkerder (gemiddelde ~0,40), 5–6 tonen met nerf. Herziet besluit 37-6 A ("planken lichter grijsgroen") deels: de kleurrichting blijft, de lichtheid niet. Eventuele `MapColor`-bijstelling: backlog `[blocks]`.

### 5. Welke indeling krijgt `canyon_stone_bricks`?
Nu is het de indeling van vanilla-`bricks` (8×4-stenen, 4 rijen) in rood met een donkere voeg. Het leest als kleibakstenen.
- **A. De indeling van vanilla-`stone_bricks` (2 rijen van ~8 px hoog, grote blokken, verspringend) in rode kloofsteen met een warme voeg.** **(aanbevolen)**
  - Leest als "gehouwen kloofsteen" en is niet te verwarren met `minecraft:bricks`.
  - Werk: één painter; trap, plaat en muur volgen.
- **B. Indeling houden, alleen tonen en randschaduw verbeteren.**
  - Geen grote verandering.
  - Wel verwarring met vanilla-bakstenen in bouwwerken.
- **C. Gemengde maten (grote en kleine blokken, zoals tuff/deepslate bricks).**
  - Het meest eigen, maar het meeste tekenwerk.

Vraag: Welke indeling krijgen de kloofsteen-bakstenen?

Antwoord: **A** (2026-10-06) — indeling van vanilla `stone_bricks` (2 rijen grote, verspringende blokken) in rode kloofsteen met warme voeg; trap/plaat/muur volgen.

### 6. Komt er een vast vergelijkingsblad met vanilla-buren?
De audit gebruikte wegwerp-scripts in `build/`. `Ring2Textures` leest bewust geen vanilla-pixels.
- **A. Ja: `tools/textures/VanillaCompare.java`** (single-file, alleen JDK). Het leest de moddev-client-extra-jar en schrijft alleen `build/texture-preview/ring2-vs-vanilla.png`; geen vanilla-pixels in `src/` of git. **(aanbevolen)**
  - Herbruikbaar voor ring 3–5 en voor teststops.
  - Valt onder §1.4 "vanilla als referentie".
- **B. Nee: de wegwerp-scripts blijven alleen lokaal in `build/texture-audit/`.**
  - Geen extra tooling.
  - Elke volgende ring bouwt het opnieuw.
- **C. In `Ring2Textures.java` zelf** (extra sectie in het contactblad).
  - Eén plek.
  - Het script leest dan wel vanilla-pixels, wat het uitgangspunt in de javadoc breekt.

Vraag: Waar komt de vanilla-vergelijking?

Antwoord: **A** (2026-10-06) — `tools/textures/VanillaCompare.java` (single-file, alleen JDK): leest de moddev-client-extra-jar alleen, schrijft alleen `build/texture-preview/ring2-vs-vanilla.png`; geen vanilla-pixels in `src/` of git.

---

## Gevolgen van de antwoorden (2026-10-06)

Alle zes vragen: **A**.
- **1 A:** geen modelwijziging; §5 vervalt (alleen PNG's). `soaked_netherrack` krijgt het palet uit §2 (`3E1415 … 793332` + porie/glans/vocht), klonten op netherrack-schaal; `spore_moss_side` volgt via `SIDE_RULES`. De W-check "duidelijk anders dan vanilla-netherrack" (37) wordt "herkenbaar nat naast vanilla-netherrack". Backlog `[blocks]` MapColor `NETHER`.
- **2 A:** de volledige lijst P1 + P2 uit "Oordeel per texture" (≈ 50 PNG's); P3 ongewijzigd. Eerste batch (tussenstand aan de hoofdsessie): `soaked_netherrack` + ertsen naast elkaar en 2–3 items.
- **3 A:** helper `outlineSelective` voor items, kristallen (dauwstadia, zoutkristal) en erts-overlays; §1.4 in `fase-1-wereld.md` aangevuld; ring 1 ongewijzigd (backlog `[textures]`).
- **4 A:** `DRIPSHROOM_WOOD` donkerder (gemiddelde ~0,40); geldt ook voor gestript, deuren en valluiken (die delen het palet).
- **5 A:** `canyonStoneBricks` in de `stone_bricks`-indeling.
- **6 A:** `tools/textures/VanillaCompare.java`, ook in de CODEBASE-tooling-tabel.
- **Runtests:** S (runServer) niet nodig (alleen assets); D: `runData`-diff leeg.

---

## Bijsturing na de tussenstand (gebruiker, 2026-10-06)

De gebruiker bekeek batch 1 (`build/texture-preview/37b-batch1.png`): richting goed, met vier bijsturingen.
1. **`soaked_netherrack` was bijna niet van vanilla-netherrack te onderscheiden.** Antwoord: zichtbaar natter maken zonder de aansluiting op vanilla-ertsen te verliezen. Eerst gebouwd met twee donkere plassen per tegel; die lazen op een 6×6-muur als een 16-px-raster (opmerking hoofdsessie). Laatste ronde (gebruiker, vóór teststop 3) na twee resterende artefacten op de 6×6-muur (grijsgroene streepjes op dezelfde hoogte per tegel; een iets lichter vierkant rond kwarts/goud doordat het blok donkerder was dan vanilla): zachte vochtzones (brede, tegelbare ruis) die het gesteente ~één toon donkerder maken en de natte glans `9A5650` dragen; poriën in de voegen (`SOAKED_PORE_ODDS` 3 op `SOAKED_CREVICE_ROLLS` 18); vocht als `SOAKED_DAMP_SPECKS` = 4 losse spikkels die nooit een rij, kolom of diagonaal delen (`sharesLine`, met wrap) (een lay-out die ze niet alle 4 kwijt kan, valt af; past geen enkele, dan stopt het script met een fout). Een eerst geprobeerde lichtere randband zonder vocht gaf op de muur een licht kader per tegel (code-review) en is vervallen: het vocht loopt zacht over de hele tegel en over de naden (tegelbare ruis), en de klonten zijn zo belicht dat de gemiddelde helderheid die van vanilla-netherrack is (gemeten 0,20 tegen 0,20; rand 0,193, midden 0,202, vanilla 0,198/0,202), zodat vanilla-ertsen erin zitten zonder lichter of donkerder vierkant en zonder kader. Het script tekent 8 lay-outs uit dezelfde seed en houdt de lay-out met de kleinste helderheidssprong tussen naburige rijen, kolommen en diagonalen (`stripeScore`, met wrap). Gecontroleerd op een 6×6-muur naast vanilla met kwarts en goud (`build/texture-preview/37b-wall-6x6-voor-na.png`). De buitenste pixelring heeft geen glans, poriën of spikkels (wel dezelfde vochtzones en belichting als de rest). Opnieuw gecontroleerd op de muur met kwarts en goud (`37b-batch1b.png`, `ring2-vs-vanilla.png`).
2. **`dew_crystal` was nog een staafje.** Antwoord: een echte puntige scherf. Na twee sprite-pogingen (las nog als staafje) definitief als polygoon: scherpe punt rechtsboven, breedste deel in het midden (~7 px), stompere gebroken voet linksonder; een rug van punt naar voet scheidt het lichte vlak (helderst langs de linkerbovenrand) van het schaduwvlak (donkerst langs de rechteronderrand), voet in de schaduw; selectieve omlijning (`insidePolygon`, `edgeDistance`).
3. **`raw_turquoise` was te klonterig/cartoonachtig.** Antwoord: één ruwe, hoekige brok zoals vanilla's ruwe koper/ijzer. Gebouwd: vlak licht bovenvlak, middenvlak en schaduwvlak met rechte kanten, bruine aders, crème gesteente eraan, mat.
4. **`raw_sunstone` moest oranjer/roder ("zon"), verder van `raw_gold`.** Antwoord: het hele zonnesteenpalet schuift naar oranjerood: `5A1A06 9A300C D95A16 F2862A FFB85A FFEBC0` (was `5A2A08 9A4A0C D9801A F2B23A FFE07A FFF6C8`), gloei `FFE0A8` (was `FFF0B0`), dimtint `9A300C`. Dat geldt consistent voor `raw_sunstone` en voor `sunstone_ore_overlay`, dus voor alle vier de zonnesteenertsen (die delen de overlay). Aandachtspunt: op rode en oranje kloofsteen is het contrast van de overlay lager. De lichte facetten en de donkere rand houden hem leesbaar, en in het donker gloeit hij (emissive). Beoordelen in teststop 3.

## Afwijkingen bij de bouw (2026-10-06)

- **Omvang:** 56 PNG's veranderd (≈ 50 gepland). Erbij gekomen doordat gedeelde helpers meeveranderen: `stripped_palm_log` (nieuwe `strippedLog`, ook voor de druipzwam), `stripped_palm_log_top` en `palm_log_top` (vezelring), `dripshroom_stem_top` (nieuw palet + pit) en `chiseled_scorched_sandstone` (de zwarte middenpixel werd `SCORCHED_SANDSTONE[0]`). **Ongewijzigd (P3):** kloofsteen-tinten, `loose_sand`, `palm_planks`, `palm_leaves`, `barrel_cactus_*`, zout (korst, blok, gepolijst: de optionele tweede bevelring van `polished_salt` is niet gedaan), `dew_source`, `glow_mold`, particles. Ring 1 byte-identiek.
- **Items als hand-gelegde sprites (routinekeuze, gemarkeerd):** `raw_sunstone` en `raw_turquoise` staan als 16×16-tekenreeksen in het script (`sprite(rows, legend)`): eigen pixels, deterministisch, en de enige manier om een tros-silhouet zoals vanilla's ruwe ertsen te krijgen; de omlijning komt daarna uit `outlineSelective`. `dew_crystal` (diagonale scherf langs `x − y` / `x + y`) en `salt` (masker + korrels + kubusjes) zijn procedureel. Vastgelegd in §1.4.
- **Alpha-maskers:** deuren, valluiken en deur-items exact gelijk aan 959249a (gecontroleerd per pixel). Anders: `salt_crystal` (+35 px: nu met omlijning, §1.4), de stalactiet-dikten zonder kartels (bijv. `down_middle` 20 px; kruismodellen, geen hitbox), de items (nieuw silhouet).
- **Puls:** `pulsing(naam, gloed, dim, painter)`: dim 12 % naar `SUNSTONE_DIM` `9A4A0C` / `DEW_DIM` `5E9FB4`, gloed 30 %, alleen tonen lichter dan de dimtint bewegen. `.mcmeta`'s ongewijzigd.
- **`soaked_netherrack` (eindstand):** 16 klonten (cel ~16 px), basistint 0,55–0,90, helling −0,3, rand rechtsonder −0,12, voeg < 0,45; glans `9A5650` op 3/5 van de klonten, alleen in vochtzones; poriën, vochtspikkels en helderheid zoals onder "Bijsturing" punt 1 (geen plassen en geen randband meer). Met vanilla-kwarts/-goud erin blijft hooguit een zeer vage vlek over (muur in `37b-wall-6x6-voor-na.png`).
- **Sporenmos-zijkant:** 1-px-plukjes onder de rand (alleen waar de rand < 5 diep is, zodat `SIDE_RULES` vanaf rij 6 blijft gelden).
- **Contactblad:** twee extra banden onder de zoutvlakte ("items in a slot" 1×/2×/3× en een Nether-ring 2-muur), `EXTRAS_HEIGHT` 760 → 1230; het lint-totaal staat in de bandkop.
- **Lint `FLAT_OK`:** `salt_crust_top`, `salt_block`, `polished_salt` (9 A, glad), `loose_sand` (zandkorrel), `red_canyon_stone` (P3, 37-5 A), `barrel_cactus_top` (rand valt buiten het model), `glow_mold` (decal), `palm_leaves` (P3). Eindstand: 0 waarschuwingen.
- **Beelden (alleen `build/`, niet in git):** `build/texture-preview/37b-overview-vs-vanilla.png` (alle gewijzigde PNG's: voor | na | 3×3 | vanilla-buur), `37b-batch1b.png` (ertsmuur + items na de bijsturing), `37b-before-after.png` (alle 56 voor/na + 3×3), `37b-batch1.png` (ertsmuur voor/na + eerste items), `ring2-vs-vanilla.png` (`VanillaCompare`), `ring2.png` (contactblad).
- **`/code-review` (medium, 10 bevindingen):**
  - opgelost:
    1. `canyonStoneBricks` heeft één toon per steen per rij (geen toonsprong midden in de 16-px-steen);
    2. `soot` wikkelt niet meer verticaal (geen roet in de lichte bovenband);
    3. de V-kerf van de palmstam heeft een donkere punt (`PALM_BARK[0]`);
    4. `CODEBASE.md` noemt `Animation` met dimtint, `pulsing(…, dim, …)` en de vervallen `rawLump`/`outline`/`veins`;
    5. de turkoois-ader loopt door het knolmidden (`vein(image, random, x, y)`);
    6. het hart van de woestijnbloem is 2×2 turkoois met één lichte glinsterpixel (`8FE6D6`) i.p.v. een crème pixel;
    8. `VanillaCompare.findJar` weigert bij meer dan één resources-jar;
    10. de haarscheurtjes van `cracked_clay` alleen binnen grote platen.
  - Niet gedaan: 7 (het Voronoi-klontpatroon staat 5× in licht verschillende vorm; een gedeelde helper verandert de uitvoer en hoort bij de bestaande `[infra]`-backlogregel over ontdubbeling van de texturescripts) en 9 (`lint` rendert opnieuw: ~1 s extra, geen cache nodig).
- **Tests:** script 2× → md5 identiek; lint 0; `./gradlew build` groen (incl. `ClientIsolationTest`); `runData` groen, diff van `src/generated` leeg; `runServer` (level `smoke-change37b`, seed 20260930) + `runClient -PquickPlay`: Dev joint, via RCON `tpring 2 nether`, `setblock` van kwarts-/gouderts, debris, lamp, hoed en dauwcluster in `soaked_netherrack` en `give` van de items gelukt; de clientlog bevat geen `Missing textures`, `Unable to load model` of animatiefouten. Het visuele oordeel in de game (W-lijst hierboven) is aan de gebruiker in teststop 3.

---

## Beslissingenlog-regels

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-06 | textures | 37b-1 A: `soaked_netherrack` naar de toonladder van vanilla-netherrack (eigen pixels, hue ~0°, saturatie ~0,6, iets donkerder); "doorweekt" via natte glans, poriën en vochtspikkels | Vanilla-kwarts/-gouderts zijn 72–87 % netherrack-pixels; in de bruine grond toonden ze een rood vierkant |
| 2026-10-06 | textures | 37b-2 A: herschildering ring 2 + Nether-ring 2 in volle omvang (items, platte blok-items, vlakke Nether-2-blokken, P2-plekken ring 2; ≈ 50 PNG's) | Feedback "plat / half getekend" in één keer afdekken |
| 2026-10-06 | textures | 37b-3 A: selectieve omlijning (boven/links donkere materiaaltint, onder/rechts de donkerste) voor items, kristallen en erts-overlays van ring 2; §1.4 aangevuld; ring 1 later (backlog) | Vanilla-stijl; één randkleur rondom oogt als sticker |
| 2026-10-06 | textures | 37b-4 A: druipzwam-planken ~2 stappen donkerder (gemiddelde ~0,40), zelfde tint — herziet besluit 37-6 A ("planken lichter grijsgroen") deels | Lichte planken lichtten op in de donkere Nether (crimson/warped 0,30–0,36) |
| 2026-10-06 | textures | 37b-5 A: `canyon_stone_bricks` in de indeling van vanilla `stone_bricks` | Las als kleibakstenen (`minecraft:bricks`) |
| 2026-10-06 | textures | 37b-6 A: vergelijkingsblad `tools/textures/VanillaCompare.java` (leest de moddev-jar, schrijft alleen `build/`) | Herbruikbaar per ring; vanilla alleen als referentie (§1.4) |
| 2026-10-06 | textures | 37b-bijsturing: zonnesteen oranjerood `5A1A06 9A300C D95A16 F2862A FFB85A FFEBC0`, gloei `FFE0A8` (item, overlay en dus alle zonnesteenertsen); `soaked_netherrack` natter (zachte vochtzones met glans, vochtige voegen, geen 16-px-raster, buitenrand op de netherrack-ladder); `dew_crystal` brede puntige scherf (polygoon); `raw_turquoise` hoekige brok | Feedback gebruiker op de tussenstand: zonnesteen verder van `raw_gold`, doorweekt netherrack eigen naast vanilla, items minder staaf-/klontachtig |
| 2026-10-06 | textures | Pulsweging ring 2: dim ≤ 10 % naar een materiaaltint, piek alleen op de midden- en lichte tonen; vlakheidscontrole (lint) in `Ring2Textures.java` | Rustframe blijft goud of helder; "half getekend" meetbaar bewaakt |

Commit (na `/code-review`): `textures: polish ring 2 items and nether ring 2 accents`.
