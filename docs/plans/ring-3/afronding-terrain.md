# Golf 2-afronding — stroom W-terrein: 42b (ring 3) en 54b (Nether-3)

Scope: `worldgen`. Worktree `.claude/worktrees/terrain-g2`, branch `ring3/terrain-afr` op main `c6cd90d`. Regels: `build/agent-rules-afronding.md` (licht testen: B + D, offline tests; één review per stroom). Bron: `teststop-4b-feedback.md` ("Beslissingen" 3, 4), `int2.md`, `docs/backlog.md` ("Uit ring 3 golf 2 en integratie 2").

## 42b — ring 3 overworld
1. **Lava tegen lucht** (harde regel; uitzondering: de vastgelegde vallen). Diagnose in de INT2-wereld `int2-det-a` (`build/afr/why.txt`, `py/lava_why.py`):
   - ~de helft: rivierlava op Y naast de lucht boven een buurkolom met lava op Y−1 (de kleine treden van 39d);
   - ~de helft: lava die uitloopt op rots. De aquifer schatte de grond naast lava met de veldhoogte −1,5; het terrein is echter bilineair geïnterpoleerd tussen celhoeken om de 4 blokken. Waar de hoogtes verspringen (bedding naast oever, twee rivieren op verschillende hoogte) ligt de echte grond tot ~8 lager;
   - 39d-lavabuistunnels onder een rivier (de "diagonale wand", 4.874, −538);
   - carvers die door de lavaband snijden (afgrond, pit);
   - vanilla-aquiferlava in diepe grotten (Y −25 … −10).
   - Fix:
     - de aquifer rekent de echte, geïnterpoleerde grond uit (`RingAquifer.surfaceAt`). De stapel is lineair in y; rond lava houdt `guard` de grotten buiten, dus daar is de dichtheid exact `0,125 · (bilerp(hoogte) − y)` plus de crags. Dunne voids en dunne vaste vormen tellen mee;
     - korst ook boven open lucht eronder;
     - een riviertrede blijft een val alleen als de lagere buurkolom die laag zeker met lava vult, anders korst (V1);
     - geen 39d-tunnel door de band rond lava;
     - carvers krijgen rots in de band rond lava (aquifer `null` bij substance 0);
     - vanilla-aquiferlava: geen lokale aquiferlava meer in ring 3 boven Y −54 (V2 = A); diep water onder Y 0 blijft.
   - Treden apart (offline telling, `RingAquifer` op het testveld; open lavablokken / blokken die als val bij een riviertrede blijven / korstlipjes), na de review: B 5.497 / 75 / 179, C 7.936 / 27 / 42, D 20.171 / 43 / 189, caldera 18.668 / 19 / 56, basalt 1.754 / 17 / 21, geode 225 / 35 / 37, E (rivieren) 6.525 / 28 / 45. Overige lava naast lucht: 0 (test `ring3LavaNeverStandsAgainstOpenAir`, met het eigen besluit van de aquifer voor de lager gelegen buur).
   - Controle van de grondschatting tegen de INT2-wereld: 100.637 kolommen zonder lava/openingen/crags; de eerste open Y = ⌈bilineaire hoogte⌉ in 80 % (veldhoogte 73 %), afwijkingen vooral door de opgehoogde blokjes van het microreliëf (+1) en carver-gaten.
2. **Wanden van diepe sneden** in ring 3-gesteente:
   - per kolom `cutDepth` = max(`floorMargin`, kloofdiepte van een rivier + 2) en per blok `deep_wall`;
   - de snelle weg van de surface (`ring_3_deep`) verft die blokken onder het pakket als gelaagd vulkanisch gesteente: tuf in banden per hoogte (`tread`), blackstone-aders (`vein`), basalt;
   - schachten van instortingen krijgen een schil (`cave_wall` 2: basalt met blackstone-aders) via `addCave`.
3. **Askorst-valblok** (determinisme): de 5 blokken bij (4.858, 78, 75) vielen in beide INT2-werelden (in de ene nog als `falling_block`-entiteiten in de lucht bij het opslaan): het verschil was het moment van vallen. Een dak van één blok boven een opening wordt nu `ash_block` (valt niet, V3 = A); worldgen plaatst geen `ash_crust` meer.

## 54b — Nether-3
- In de kloven zijn de vloeren onder Y 30 (rond de open lavaspiegels op Y 22) de warme kring: koudlava met wat magma, geen ijsplaten en geen rijp. Via `tools/worldgen/nether_ring3_surface.py` (`RIFT_WARM_BELOW`); output geregenereerd, idempotent. De gletsjermassa's en ijsvallen beginnen al op Y 30.

## Noise-stap
42b raakt de noise-stap: de aquiferregels (randen, carvers) en een dunne-vorm-regel (39d-tunnels). Perf hermeten in INT. Daarnaast de surface-stap (`deep_wall` per blok in de snelle weg).

## W-checklist (INT)
- [ ] Lava-scan (`ow_lava.py`) in de INT2-gebieden: 0 lava naast lucht buiten de eilandvallen en de riviertreden (V1); de treden apart tellen: een contact waarvan de buurkolom op Y−1 lava heeft (`build/afr/py/lava_why.py` + de indeling "step-onto-lava" uit de diagnose). Verwacht in de orde van de offline telling hierboven.
- [ ] (4.134, 94, −3.458) geen luchtgeul door het lavameer; (4.874, 90–94, −538) geen lavawand langs een buiswand.
- [ ] Diepe grotten in ring 3 zonder aquiferlava (V2).
- [ ] Afgrondwanden, kloofwanden van rivieren en schachten van instortingen in basalt/tuf/blackstone, geen vanilla `stone`-banden.
- [ ] Askorst bij een buismond (4.858, 73–78, 75): gelijk in twee werelden, geen vallende blokken (V3).
- [ ] Nether-3-kloven: 0 ijsblokken binnen 3 blokken van de open lavaspiegels onder Y 30.
- [ ] Prestatie noise en surface (42b raakt beide).

## Review (main, `/code-review medium`, 2026-10-09)
1. De trede-uitzondering leunde op de veldlava van de buur op y−1, die de buur zelf tot korst kon maken: nu `becomesLava` (dezelfde beslissing als de aquifer daar neemt: lava, zeker open, niet `exposed`; recursie alleen omlaag). De offline tests gebruiken het aquifer-resultaat voor die buur.
2. Carvers herkennen aan `substance == 0.0` was fragiel: `RingChunkGenerator.applyCarvers` zet nu een vlag op de `RingAquifer` (`carving(true)` … `false`); een echte dichtheid van 0 blijft gewoon open ruimte. Test `ring3CarversStayOutOfTheBandAroundLava`.
3. `surfaceAt` nam aan dat elke celhoek de pure terreinterm geeft: de band rond rivieren reikt nu minstens 6 blokken voorbij de lavarand (`RIVER_GUARD`, meer dan een cel; kraters crest + 6, afgrond 0,3 · breedte). Instortingen (de `collapse`-term) blijven ≥ 12 blokken van rivieren en uit vulkanen en de afgrond.
4. `mayBeOpen`/`surelyOpen` samengevoegd tot `open(…, crags)` met marge met teken; de vier hoekkolommen één keer per vraag (`groundAt`); `islandAt` weg (`thinSolidAt` dekt eilanden).
5. `ring_3_deep` test eerst per kolom `cut_depth` ≥ 8 (nieuwe kolomuitgang, door de conditie per kolom onthouden), pas dan per blok `deep_wall`.
6. `MAX_CAVES` 7: zes voor de buizen, daklichten en kamers van 42, één voor de schacht van een instorting (die komt eerst). Randkolommen van een schacht houden `cave_walls` = 1: ze hebben echt een schil om te verven (tot 3 onder het maaiveld; zonder schil telt een kolom niet).
7. De guard rond lava wordt centraal gehandhaafd in `Column.addVoid` (dus ook voor `addCave`): een opening houdt haar grootste deel buiten de band. Test `noOpeningEntersTheBandAroundLavaWhateverShapeAsks`.
Daarna `build --rerun-tasks` groen (674 tests), `runData` zonder diff, `ring3_surface.py` idempotent.

## Voortgang
Voortgang: 2026-10-09 ~17:30 start; regels, 4b-besluiten, `int2.md` en de backlog gelezen; diagnose in de INT2-wereld (offline, Python op de regiobestanden); vragen V1–V3 naar main.
Voortgang: ~19:30 randen via de exacte interpolatie, carvers uit de lavaband, 39d-tunnels niet onder lava, wanden van diepe sneden, schachtwanden, 54b; antwoorden V1–V3 (alle A) verwerkt (V2 aquiferlava → lucht, V3 `ash_block` op dunne daken).
Voortgang: 20:10 `build --rerun-tasks` groen (673 tests), `runData` zonder diff, beide surface-scripts idempotent.
Voortgang: 20:27 reviewpunten 1–7 opgelost; `build --rerun-tasks` groen (674 tests), `runData` zonder diff; offline tellingen opnieuw.

## CODEBASE-delta
Kop: "Laatst bijgewerkt: 2026-10-09 — golf 2-afronding W-terrein (42b, 54b, `worldgen`): lava tegen lucht in ring 3 weg (exacte interpolatie in de aquifer, carvers uit de lavaband, geen 39d-tunnels onder lava, geen lokale aquiferlava), wanden van diepe sneden in ring 3-gesteente, dunne daken `ash_block` i.p.v. vallende askorst, ijsvrije warme kring in de Nether-3-kloven."

Classes:
- `worldgen/chunk/RingAquifer.java`: 42b — `exposed` kijkt naar de echte grond (`groundAt`: bilineaire hoogte van de celhoeken om de 4 blokken, plus crag-marge per hoekruwheid), naar openingen en dunne vaste vormen, ook eronder; een riviertrede blijft alleen een val als de lagere buur die laag echt lava wordt (`becomesLava`, dezelfde beslissing); carvers krijgen rots in de band rond lava (`carving`-vlag, gezet door `RingChunkGenerator.applyCarvers`; `guardBottom`/`guardTop` per kolom); **geen lokale aquiferlava meer in ring 3** boven Y −54 (lucht; CODEBASE r. ~442 over aquiferlava in ring 3-grotten klopt dan niet meer).
- `worldgen/chunk/RingChunkGenerator.java`: 42b — `applyCarvers` markeert de carver-pas op de `RingAquifer`.
- `worldgen/densityfunction/VolcanicField.java`: 42b — `Column.cutDepth` (max van `floorMargin` en de kloofdiepte van een rivier + 2), `deepWall(y)`, nieuwe publieke accessors `thinSolidAt(y)`, `guardBottom()`/`guardTop()`; `addVoid` houdt elke opening buiten de band rond lava; de rivierband reikt ≥ `RIVER_GUARD` (6) voorbij de lavarand; `MAX_CAVES` 7 (6 voor 42, 1 voor een schacht); `deepWall(x, y, z)`.
- `VolcanicOutput.java`: uitgangen `deep_wall` (per blok) en `cut_depth` (per kolom), voor de surface.
- `ThinShapes.java`: 42b — geen 39d-tunnel door de band rond lava; schachten van instortingen met een schil (`addCave`).
- `CellShapes.java`: `Collapse.shaftOutside`.

Databestanden:
- `noise_settings/overworld.json` (uit `ring3_surface.py`): de snelle weg verft blokken met `cut_depth` ≥ 8 (per kolom) en `deep_wall` (per blok) als gelaagd vulkanisch gesteente (tuf in banden, blackstone-aders, basalt); dunne daken in de korstbiomes `ash_block`.
- `noise_settings/nether.json` (uit `nether_ring3_surface.py`): kloofvloeren onder Y 30 koudlava/magma, geen ijs (`RIFT_WARM_BELOW`).
- `tools/worldgen/ring3_surface.py`: `deep_wall_rock()`, `ring_3_deep()` met wanden; `tools/worldgen/nether_ring3_surface.py`: `RIFT_WARM_BELOW`.

Tests: `Ring3TerrainFixesTest` (nieuw); bijgewerkt: `RingAquiferTest` (randen via de interpolatie, treden, "lava nooit tegen lucht" in 5 gebieden, carvers, geen aquiferlava), `Ring3TerrainJsonTest` (geen `ash_crust` uit worldgen), `NetherRing3JsonTest` (warme kring in de kloven).

Valkuilen:
- **Grond naast lava (42b):** het terrein ligt op de bilineaire mix van de celhoeken (om de 4 blokken), niet op de veldhoogte van de kolom; waar hoogtes verspringen scheelt dat tot ~8 blokken. Elke voorspelling "is dit blok open" moet die mix gebruiken (`RingAquifer.surfaceAt`).
- **Carvers en de aquifer:** de aquifer weet via de `carving`-vlag (gezet door `RingChunkGenerator.applyCarvers`) dat een carver vraagt; `null` = niet uithakken. Zo blijven ze uit de banden rond lava (en rond kamers, gootjes, geodes). Geen sentinel op de dichtheid: een echte dichtheid van 0 komt voor.

## Beslissingen
- 2026-10-09 · worldgen · Riviertreden blijven een vastgelegde kleine lavaval, maar alleen als de lagere buurkolom die laag volgens de exacte interpolatie zeker met lava vult; anders een korstlip · overige lava tegen lucht moet weg (harde regel), de treden horen bij de rivieren van 39d (V1 = A, gebruiker)
- 2026-10-09 · worldgen · Geen lokale aquiferlava in ring 3 (open ruimte wordt lucht); diep water onder Y 0 blijft (V8) · de poelen stonden tegen de lucht van hun grotten; lava alleen waar het terrein hem legt (V2 = A, gebruiker)
- 2026-10-09 · worldgen · Een dak van één blok boven een opening wordt bij worldgen `ash_block` (valt niet); `ash_crust` alleen waar hij steun heeft · de vallende askorst viel bij de eerste buurupdate, de wereld hing af van het moment (V3 = A, gebruiker)
- 2026-10-09 · worldgen · Wanden van diepe sneden (afgrond, rifts, kloven van rivieren, instortingen) onder het grondpakket: gelaagd vulkanisch gesteente (tuf in banden, blackstone-aders, basalt), schachten basalt · gesteente dat ring 3 al gebruikt (grondpakket 39c), geen vanilla `stone`-banden
- 2026-10-09 · worldgen · Nether-3-kloven: vloeren onder Y 30 (rond de lavaspiegels op Y 22) koudlava met wat magma, geen ijs of rijp · warme kring ijsvrij (G2-WN3)

## Backlog
- `blocks` | Een eigen askorstblok dat alleen instort als er een speler onder staat (niet door buurupdates); dan kan de worldgen het weer op dunne daken boven openingen leggen (V3 = A, 42b).
- `worldgen` | Lavavallen van eilanden en de 47c-lavavallen blijven lava tegen lucht (vastgelegd); riviertreden ook (V1). De INT-scan moet ze apart tellen.
