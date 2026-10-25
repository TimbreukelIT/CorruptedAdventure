# Fase 2 — Items en tools

Sparring gestart op 2026-10-25, direct na de afronding van fase 1. Dit document is de bron van waarheid voor *wat* fase 2
bouwt; de details per golf komen erbij na de vragenronde aan het begin van die golf (CLAUDE.md §3).

## 1. Kernbesluiten (sparring 2026-10-25)

### 1.1 Gear-tiers
- **Een volledige tier per ring:** ring 2 zonnesteen, ring 3 vulkaniet, ring 4 getijdezilver, ring 5 nulliet. Elke tier
  heeft een complete set: zwaard, bijl, houweel, schep, schoffel en 4 harnasstukken, elk een stap boven de vorige tier.
- **Plek ten opzichte van vanilla:** zonnesteen ≈ netherite (een alternatief pad). Vulkaniet, getijdezilver en nulliet
  liggen daarboven, dus netherite wordt in ring 3 zwak.
- **Alleen stats:** de tiers verschillen alleen in schade, snelheid, duurzaamheid, armor en toughness; er zijn geen
  perks.
- **Maken:** via een smithing-upgrade, zoals netherite: gear van de vorige tier + ingot van de nieuwe tier + een
  upgrade-template. Het template komt uit de Nether-ring, zodat de lus overworld → Nether → volgende ring terugkomt.
  Enchantments blijven behouden.
- **Mijn-gating op alle ring-ertsen:** elk erts van ring N+1 vraagt het gereedschap van tier N. Ring 2-ertsen vragen
  netherite/diamant-niveau, en vanaf ring 3 de tier van de vorige ring.

### 1.2 Wapens
- Per tier de 5 standaard tools. Daarnaast komen per ring een paar **thematische extra's** waar de materialen erom
  vragen, bijvoorbeeld brandpijlen uit zwavel in ring 3 en gifpijlen uit gifkristal in ring 4. De concrete lijst komt
  per golf.

### 1.3 Bescherming tegen condities
Dit geldt voor alle 6 condities: uitdroging (ring 2), hitte (ring 3), gif (ring 4), void (ring 5), kou (Nether-3) en
stilte (Nether-5).
- **Coating via smithing:** je brengt een coating aan op een harnasstuk naar keuze. Het stuk houdt zijn stats. Er past
  maximaal 1 coating op een stuk, dus maximaal 4 beschermingen tegelijk. De tooltip toont de coating. Er komen geen
  aparte beschermingsstukken en je hoeft geen harnas te wisselen.
- **Sterkte:** volledig beschermd (4 stukken met dezelfde coating) loopt de meter ongeveer **4× trager**. Je blijft
  schaduw, grotten en verbruiksitems nodig hebben. De coating werkt via de bestaande `RateModifier`-pijplijn van de
  condities (fase 1, §condities), dus de rekenlogica blijft op de server.
- **Verbruiksitems:** daarnaast komt per conditie een vroeg, eenvoudig item dat even helpt. Die items worden
  **gecraft en gevuld**, zoals een herbruikbare veldfles die je bij water of een dauwbron vult, en dranken uit
  ringmaterialen. Er wordt dus niets gebrouwen.
- **Zuivering (witte kern, Nether-5):** dit is de bescherming tegen void (ring 5) en stilte (Nether-5). In fase 5 wordt
  het uitgebreid tegen corruptie.

### 1.4 Items met een functie die verder reikt
- **De functie komt nu ook**, waar het systeem bestaat:
  - **Hemelglas-lens:** gebruik je hem zoals een verrekijker, dan toont hij met een omlijning de verborgen glitches:
    void-gaten onder de grond, valse of onzichtbare blokken, en de ontbrekende chunks in ring 5. Ook **dempt** hij de
    glitch-visuals, zoals de schermvervorming en de glitch-fog. De server bepaalt wat zichtbaar is; de client tekent
    alleen.
  - **Edelstenen** (turkoois, vuurrobijn, jade): die krijgen nu een eigen crafting-nut, zoals decoratieblokken,
    armor-trims en onderdelen van coatings of verbruiksitems. Handel komt bij de handelaars in fase 3 of 4.
  - **Zuiveringsgear:** zie §1.3.

### 1.5 Overig
- **Creative-tabs:** één tab per ring (ring 1–5), met daarin de blokken, items en gear van die ring. De bijbehorende
  Nether-ring zit in dezelfde tab.

## 2. Golven en teststops
Er wordt per ring gebouwd, in golven, en elke golf begint met een korte vragenronde.

| Golf | Inhoud | Teststop |
|---|---|---|
| 1 — Basis | Creative-tabs per ring; recepten voor de bestaande blok- en houtsets (dood hout, kloofsteen, palm, druipzwam, verdant, zout, tidestone e.d.); smelten van de ruwe ertsen; het tier-raamwerk (tiers, `needs_*_tool`-tags, smithing-templates); het coatingsysteem (data component + smithing + `RateModifier`); het raamwerk voor verbruiksitems | — |
| 2 — Ring 2 + Nether-1 | Zonnesteen-tier; de bescherming tegen uitdroging (coating + veldfles); nut voor turkoois en zout; zielkristal; ring 2-extra's | **Teststop na golf 1 + 2** |
| 3 — Ring 3 + Nether-2 | Vulkaniet-tier; hittebescherming met dauwkristal; zwavel (buskruit, vuur, brandstof, brandpijlen); vuurrobijn; druipzwam; ring 3-extra's | — |
| 4 — Ring 4 + Nether-3 | Getijdezilver-tier; gifbescherming met oerijs-water; koubescherming; gifkristal (gifpijlen); jade, aquamarijn en veen; ring 4-extra's | **Teststop na golf 3 + 4** |
| 5 — Ring 5 + Nether-4/5 | Nulliet-tier; hemelglas (blok, lens); zuivering met de witte kern (void + stilte); glitchscherven; ring 5-extra's | **Eindtest fase 2** |

Tussen de teststops test Claude zelf: build, runData, een server met 2 clients, en recepten en mijnbaarheid via een
testscript.

## 3. Open punten voor de golf-vragenrondes
- De exacte stats per tier (schade, duurzaamheid, armor, toughness, mijnsnelheid) en de curve.
- Per tier: uit welke Nether-ring het upgrade-template komt en hoe je het vindt (loot, recept, of een drop van het
  sleutelmateriaal).
- Per conditie: het coatingmateriaal, het verbruiksitem en wat dat precies doet (hoeveel punten, hoe lang).
- De visuele vorm van een coating: alleen de tooltip, of ook een overlay of trim op het harnas.
- De thematische extra's per ring.
- Wat de lens precies omlijnt, en hoe de server die informatie naar de client stuurt.

## 4. Golf 1 — basis (vragenronde 2026-10-25)
- **Coating = vanilla armor-trim:** elke coating is een eigen trim-patroon, aangebracht op de smithing table met een
  coating-template en het coatingmateriaal. Je ziet hem op het harnas en in de tooltip, en de rendering doet vanilla.
  Een stuk met een coating kan dus geen decoratieve trim meer hebben.
- **Coating-regels:** een nieuwe coating vervangt de oude. Bij een smithing-upgrade naar de volgende tier blijft de
  coating behouden, zoals een trim bij netherite.
- **Opbouw lineair per stuk:** elk stuk met dezelfde coating haalt ~19 % van de stijging af. 1 stuk geeft 0,81×,
  2 stukken 0,63×, 3 stukken 0,44× en 4 stukken 0,25×. Alleen een stijgende meter wordt vertraagd; het dalen blijft
  gelijk.
- **Mijn-gating:** de ring 2-ertsen (zonnesteen, turkoois) vragen **diamant** (netherite mag ook). Ring 3-ertsen vragen
  zonnesteen, ring 4-ertsen vulkaniet en ring 5-ertsen getijdezilver. De ertsen van Nether-ring N vragen hetzelfde als
  die van ring N.
- **Inhoud van golf 1:**
  - creative-tabs per ring;
  - recepten voor de bestaande blok- en houtsets en de opslagblokken;
  - smelten van de ruwe ertsen (oven + hoogoven);
  - het tier-raamwerk: 4 tiers met voorlopige stats (afstemmen in golf 2–5), `needs_*_tool`- en
    `incorrect_for_*_tool`-tags op de ertsen, en upgrade-template-items als placeholder;
  - het coatingsysteem: trim-patronen, coating-templates, een `RateModifier` die het harnas leest (server-side) en
    tests.
  - Het raamwerk voor de verbruiksitems schuift naar golf 2, waar de veldfles het eerste voorbeeld is.

### 4.1 Tabs, materialen en smelten (D)
- **Creative-tabs:** vijf tabs `ring_1` … `ring_5` ("Corrupted Adventure: Ring N"); Nether-ring N zit in tab N. Per
  tab eerst de blokken, dan de losse items, in registratievolgorde. De tab volgt uit de `// ---- Ring N ----`-sectie
  in `ModBlocks`/`ModItems`; nieuwe items komen achteraan in hun (Nether-)ringsectie. Tab-iconen: dood hout, ruwe
  zonnesteen, ruw vulkaniet, ruw getijdezilver, ruw nulliet.
- **Producten (voorlopige namen):** `sunstone_ingot`, `volcanite_ingot`, `tidesilver_ingot`, `nullite_ingot` en
  `turquoise` (de geslepen edelsteen uit `raw_turquoise`). Placeholder-textures.
- **Opslagblokken (9 in, 9 uit):** ruw (`raw_sunstone_block`, `raw_turquoise_block`, `raw_volcanite_block`,
  `raw_tidesilver_block`, `raw_nullite_block`; als `raw_iron_block`), metaal (`sunstone_block`, `volcanite_block`,
  `tidesilver_block`, `nullite_block`; als `iron_block`) en edelsteen (`turquoise_block`, `fire_ruby_block`,
  `jade_block`; als `diamond_block`). Mijnbaar met dezelfde tier als hun erts (ring 2: diamant; ring 3–5 volgen de
  tier-tags uit §4.3). **Voorlopig:** geen beacon-basis en geen beacon-betaling (backlog).
- **Zout en kristallen:** 4 zout ↔ zoutblok (beide kanten op). 4 scherven → kristalblok voor vuurkristal, gifkristal,
  aquamarijn en zuiverkristal, zoals amethist (geen weg terug).
- **Smelten (oven + hoogoven):** elk raw-item en elk erts (alle gesteente-varianten) naar zijn product: zonnesteen,
  turkoois, vulkaniet, getijdezilver en nulliet, plus zielkristal, zwavel, vuurrobijn, jade en witte kern uit hun
  ertsen. XP zoals het vanilla-analoog: metalen en turkoois 0,7 (ijzer/koper), nulliet en de edelstenen 1,0 (diamant),
  zielkristal 0,2 (kwarts), zwavel 0,1 (kolen).
- **Niet in golf 1:** opslagblokken voor zwavel, zielkristal, dauwkristal, witte kern, glitchscherven en de
  kristalscherven-als-materiaal; die volgen met hun nut in golf 2–5.
### 4.2 Recepten blok- en houtsets (A)
Alle recepten volgen de vanilla-set die het blok kopieert (vormen, aantallen, categorieën, groepen, unlock-condities),
met ids in onze namespace. Code in `datagen/recipe/` (`WoodSetRecipes`, `StoneSetRecipes`, `FloraRecipes`).
- **Houtsets** (zoals eik; de Nether-stammen zoals crimson): dood, palm, druipzwam, verkoold, rijpzwam, moonpetal,
  verdant en corrupted. Planken uit de log-tag (4), hout/hyphae uit 4 stammen (3, ook gestript), trap, plaat, hek,
  poort, deur, valluik, knop en drukplaat; corrupted ook bord en hangbord. Stokken, werkbank, kist, houten tools en
  houtskool komen al via de vanilla-tags.
- **Verdorde moonpetal (voorlopig):** heeft geen eigen planken; zijn stammen geven moonpetal-planken
  (`moonpetal_planks_from_withered_moonpetal_logs`), en 4 stammen geven het eigen (gestripte) verdorde hout.
- **Steensets zoals tuf** (vormen aan de werkbank, 4 van een trede → 4 van de volgende, steenzaag van een trede naar
  elke latere trede en alle vormen): puimsteen, koude lava, tidestone (tidestone → gepolijst → bakstenen),
  fossielsteen, witte steen (+ zuil uit 2 witte steen, zoals de kwartszuil) en zout (`salt_block` → `polished_salt`;
  4 zout ↔ zoutblok is stroom D).
- **Keien:** kloofkeien, cobbled tidestone en corrupted keien hebben hun vormen en smelten terug naar hun steen
  (0,1 XP, 200 ticks). Kloofkeien smelten naar **oranje** kloofsteen (de middelste tint en de kaartkleur van de keien;
  voorlopig). Kloofstenen bakstenen komen uit elke mix van de drie tinten (2×2), en elke tint geeft aan de steenzaag de
  bakstenen en hun vormen.
- **Mossy tidestone:** tidestone + ranken of mosblok (zoals mossy cobblestone, met zijn eigen vormen).
- **Zandsteen:** scorched sandstone krijgt de recepten van vanilla-zandsteen, maar **geen pakrecept**: ring 2 heeft geen
  eigen zand (`loose_sand` laat vanilla-zand vallen), dus de basis komt alleen uit de wereld (voorlopig). Lagoon
  sandstone houdt zijn pakrecept en glas uit lagoon-zand.
- **Overig:** `packed_nether_ice` uit 9 `nether_ice` (zoals packed ice); `desert_flower` → 1 oranje kleurstof.
- **Bewust geen recept:** `dripshroom_lamp` (shroomlight heeft er ook geen), `dripshroom_cap`/`rimeshroom_cap` (geen
  wrattenitem), `clawed_tidestone`/`clawed_stone`/`clawed_deepslate`, `aquamarine_stone`, `mirror_stone`, `chalkrack`,
  `bleached_netherrack`; de basisblokken zelf (kloofsteen, puimsteen, koude lava, tidestone, fossielsteen, witte steen)
  komen alleen uit de wereld, zoals tuf en basalt.
### 4.3 Tier-raamwerk (B)
**Stats (voorlopig, afstemmen in golf 2–5).** Curve: zonnesteen ≈ netherite, daarna per ring een stap. Bron in de code:
`item/GearTier` (tools: `Tier`; harnas: `registry/ModArmorMaterials`). `GearTierTest` bewaakt de curve (elke tier strikt
hoger op duurzaamheid, mijnsnelheid en schade, minstens gelijk op de rest; zonnesteen ≥ netherite op elke stat; volledig
harnas binnen de vanilla-grenzen armor 30 en toughness 20).

| Tier (ring) | Duurzaamheid tools | Mijnsnelheid | Schade-bonus | Enchant | Armor laars/been/borst/helm (dier) | Toughness | Knockback-weerstand | Duurzaamheidsfactor harnas |
|---|---|---|---|---|---|---|---|---|
| netherite (ter vergelijking) | 2031 | 9 | 4 | 15 | 3/6/8/3 (11) | 3 | 0,1 | 37 |
| zonnesteen (2) | 2031 | 9 | 4 | 15 | 3/6/8/3 (11) | 3 | 0,1 | 37 |
| vulkaniet (3) | 2500 | 10 | 5 | 15 | 4/7/9/4 (12) | 3,5 | 0,1 | 42 |
| getijdezilver (4) | 3000 | 11 | 6 | 16 | 4/7/10/4 (13) | 4 | 0,15 | 47 |
| nulliet (5) | 3600 | 12 | 7 | 18 | 5/8/10/5 (14) | 5 | 0,2 | 53 |

- Reparatie met `c:ingots/<tier>` (het ingot van stroom D); equip-geluid en layer zoals netherite. De layer-textures van
  het harnas komen met de harnasstukken.
- De tiers zijn een enum die `Tier` implementeert, zoals vanilla's `Tiers` (geen `SimpleTier`-velden): één plek voor de
  stats van tool en harnas.

**Mijn-gating.** Blok-tags `corruptedadventure:needs_<tier>_tool` en `incorrect_for_<tier>_tool`. De vanilla-tags
`incorrect_for_{wooden,stone,gold,iron,diamond,netherite}_tool` bevatten al onze needs-tags; `incorrect_for_<tier>_tool`
bevat de needs-tags van de hogere tiers.

| Ertsen | Vraagt | Tag |
|---|---|---|
| Nether-1: zielkristal (2 varianten) | ijzer (ring 1, ongewijzigd) | `minecraft:needs_iron_tool` |
| ring 2: zonnesteen (4), turkoois (3) | diamant (netherite mag) | `minecraft:needs_diamond_tool` |
| ring 3: vulkaniet (3), zwavel (2), vuurrobijn (3) | zonnesteen | `needs_sunstone_tool` |
| ring 4: getijdezilver (3), jade (3) | vulkaniet | `needs_volcanite_tool` |
| ring 5: nulliet (2); Nether-5: witte kern (1) | getijdezilver | `needs_tidesilver_tool` |
| — | nulliet | `needs_nullite_tool` (leeg, voor later) |

- Nether-2, -3 en -4 hebben geen echte ertsen (alleen kristallen/clusters), dus geen gating daar.
- **Zwavel** vroeg eerst een stenen pickaxe en vraagt nu, als ring 3-erts, zonnesteen (zoals besloten: álle ring-ertsen).
  Zwavelkristallen aan het oppervlak (`sulfur_crystal`) blijven vrij te oogsten.
- **Gevolg in golf 1:** er zijn nog geen zonnesteen-tools, dus de ertsen van ring 3, 4, 5 en Nether-5 zijn in survival
  tijdelijk niet te mijnen (ze breken traag en droppen niets). Zo bedoeld: de tools komen per golf.
- Test: `gametest/TierGatingGameTests` (GameTest met de echte tags; elk eigen erts × de pickaxe-ladder; een tier-pickaxe
  is een netherite-pickaxe met de `tool`-component van de tier) en `GearTierTest` (gegenereerde tags).

**Upgrade-templates (placeholder).** `sunstone_upgrade_smithing_template`, `volcanite_…`, `tidesilver_…`,
`nullite_…`: `SmithingTemplateItem` zoals de netherite-upgrade (item heet "Smithing Template", de tooltip noemt de
upgrade). Nog geen smithing-recept en geen vindplaats (golf 2–5). In de creative-tab van de ring van hun tier.
- *Voorlopig:* de tooltip zegt dat zonnesteen **diamanten** uitrusting opwaardeert (een alternatief pad naast netherite)
  en elke volgende tier de uitrusting van de tier ervoor. Of zonnesteen uit diamant of uit netherite komt, beslist golf 2.
