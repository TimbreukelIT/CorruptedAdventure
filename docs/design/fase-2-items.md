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
