# Golf 2 — stroom T (textures): 57a-rondes (bijsturen na 4a, nieuwe blokken en particles uit golf 2)

Scope: `textures` — één commit per ronde, branch `ring3/textures-g2`, poort 6 (geen eigen server). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

**Doel:**
- de textures van 57a bijsturen op het echte in-game beeld: het 39d-terrein met de golf 2-aankleding;
- textures maken voor wat golf 2 nieuw nodig heeft.

De definitieve ronde is 57 (golf 3, na 4b). Dit zijn tussenrondes.

**Bron:**
- `golf-1-textures.md` (57a: script, paletten, gloeilagen, controles, valkuilen);
- `fase-1-wereld.md` §1.4 (stijl 37/37b);
- backlog r. 181 (bijsturen: smeulsteen/gloeiende scheur in een echte vloer, smeulblad in een woud, gloeilagen in Sodium/shaderpacks), r. 182 (`steam_puff`/`ice_glint`-tint), r. 151 (ontdubbelen: aparte `infra`-keuze, hier niet).

## 1. Rondes (één commit per ronde)

### 57a-r1 — Bijsturen op het 39d-terrein (kan direct)
1. **Beeld verzamelen.** T heeft geen eigen server. Vraag main om in-game shots, of neem lock + één slot voor een singleplayer-`runClient` met een verse wereld (seed `20260930`, creative, `/time set noon` en `midnight`). Vastleggen, minstens:
   - smeulsteen in de scheuren van `basalt_flows` (4.814, −288) dag en nacht;
   - smeulende as en as in de asvlakte (5.262, 480);
   - puimsteenkappen op een reus (~−3.895, 65);
   - zwavelkorst (4.270, 480);
   - gloeiende scheur en Nether-ijs in Nether-3 (`/ca tpring 3 nether`).
   - Screenshots: eerst laten genereren, dan fotograferen (geen `save-all flush` tijdens het fotograferen).
2. **Bijsturen** volgens G2-T1 en de 4a-indrukken van de gebruiker (afstemvraag 10 van `teststop-4a.md` is nog niet beantwoord). Vooral tegeling op grote vlakken (motif/stripe-score), toonbereik tegen de vanilla-buren (`VanillaCompare ring3`) en het gloei-aandeel 's nachts.
3. **Gloeilagen in Sodium/shaderpacks** (backlog r. 181): alleen nagaan en noteren (respecteren ze `neoforge_data`/emissive?). Een oplossing is een vraag aan main, geen eigen keuze.

### 57a-r2 — Golf 2-aanvulling (na de merge van A's 50-0 en de eerste W-biomes-pakketten)
1. **`steam_puff` en `ice_glint`:** tint of eigen kleur, afgesproken met A (backlog r. 182). Voorstel A: lichte grijswaarden, getint in de particle-klasse. Zo nodig de sprites aanpassen.
2. **Nieuwe blokken** die B in golf 2 via main toevoegt (bijv. `46b`): PNG's onder de contractnamen.
3. **Bijsturen op de aankleding:** smeulblad in een echt woud (47c), as-afgietsels van `smoldering_ash` 's nachts (49), vuurkristal in een geode (41b), rijpzwam/ijspegels in Nether-3 (53b).

## 2. Bestanden
- **Eigen:** `tools/textures/Ring3Textures.java`, `tools/textures/VanillaCompare.java` (alleen `ring3`-blad), PNG's en `.mcmeta`'s onder `assets/corruptedadventure/textures/` met ring 3-/Nether-3-namen.
- **Niet:** models, blockstates (B), particle-registratie/-klassen (A).
- **Valkuil:** `core.autocrlf` en opnieuw gegenereerde `.mcmeta`'s zonder inhoudsverschil niet meecommitten (`git checkout --`).

## 3. Afhankelijkheden
- **r1:** geen (beeld via main of een eigen client-slot).
- **r2:** ← A (50-0: `steam_puff`/`ice_glint` geregistreerd, tint afgesproken), ← W-biomes/W-nether (de aankleding staat in een wereld), ← B (alleen als er nieuwe blokken komen).
- **→ niemand** hard: B's models verwijzen al naar de paden van 57a.

## 4. Open vragen
Aanbevolen optie eerst. **C** = complexiteit, **S** = speelgevoel.

**G2-T1 — Hoe sturen we de textures bij vóór 4b?**
- **A. Twee korte rondes: r1 nu op in-game shots van het 39d-terrein (T maakt de shots, jij geeft per blok kort "goed / lichter / rustiger / meer gloed"), r2 na de aankleding; de grote ronde blijft 57 na 4b (aanbevolen).** C laag. S: de grote vlakken (as, basalt, puimsteen) kloppen al in 4b; weinig dubbel werk.
- **B. Eén ronde na de aankleding (alleen r2).** C laag. S: 4b ziet mogelijk nog storende tegeling op het nieuwe terrein.
- **C. Nu meteen een grondige ronde op alles.** C middel. S: de aankleding kan het beeld nog veranderen (dubbel werk).
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
Geen (assets).

## 6. Tests
- Script 2× → md5 identiek; lint 0; `checkGlowLayers` groen.
- **B** draaien (assets breken de build niet, maar toch).
- Na integratie: `runClient` zonder `Missing textures`.
- Contactblad `build/texture-preview/ring3.png` + `ring3-vs-vanilla.png` vóór/na aan main.
- **Review:** "klaar voor review-doorsturen: <ronde>" aan main; main draait `/code-review medium`; daarna committen.

## 7. W-checklist (in-game; voor 4b)
- [ ] Grote vlakken as, basalt, puimsteen, zwavelkorst zonder storende tegeling (van hoog en van dichtbij).
- [ ] 's Nachts: alleen de scheuren/vlekjes gloeien (smeulsteen, smeulende as, smeulblad, gloeiende scheur), het gesteente wordt donker.
- [ ] Smeulwoud 's nachts: kronen gloeien als vlekjes, geen fullbright-kubussen.
- [ ] `steam_puff`/`ice_glint` passen bij hun omgeving (zwavel/`steam_fens`, Nether-3).
- [ ] Rijp-netherrack met vanilla-Nether-ertsen erin oogt natuurlijk.

## Voortgang
- Voortgang: 2026-10-08 13:52 worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a6fec96cc15cd88a4`, branch `ring3/textures-g2` (vanaf dbb1cb9); ronde r1 gestart (G2-T1 = A).
- Voortgang: 2026-10-08 14:25 r1 beeld verzameld (B groen; server+client 25576/RCON 25586, lock + 2 slots, weer vrijgegeven): 18 views dag/nacht + Nether-3 in `build/t-r1/` (F2-screenshots; PrintWindow faalt na een venster-resize), overzicht `build/t-r1/r1-overview.png`, oppervlaktetelling per plek via RCON (`py/probe.py`); voorstellen per blok aan main, wacht op het oordeel van de gebruiker.
- Voortgang: 2026-10-08 14:30 oordeel gebruiker: voorstellen 1–8 akkoord. `Ring3Textures.java` bijgestuurd; script 2× → md5 identiek (181 PNG's), lint 0, `checkGlowLayers` groen; `.mcmeta`'s alleen LF/CRLF → teruggezet.
- Voortgang: 2026-10-08 14:36 B groen; ná-shots (zelfde wereld `t-r1-a`, zelfde views, dag/nacht) gestart.
- Voortgang: 2026-10-08 14:51 ná-shots klaar (`build/t-r1/after/`, vóór in `build/t-r1/before/`, vergelijking `build/t-r1/r1-before-after-1.png` en `-2.png`); server/client gestopt, lock en slots vrij, RCON uit. Klaar voor review.
- Voortgang: 2026-10-08 15:55 reviewbevindingen 1–10 verwerkt (zie Review); script 2× → md5 identiek, lint 0, B groen (15:30); ná-check in-game smeulende as dag/nacht en gloeiende scheur vloer/wand (`build/t-r1/r1-after-review.png`, `after2/`); lock/slots vrij, RCON uit. Commit.

## 57a-r1 — review (`/code-review` medium, 10 bevindingen)
- Opgelost:
  - 1: de kerntak van de gloeiende scheur was onbereikbaar → drempel `v < 0,45 && width > 0,5`, kleine `LAVA[4]`-kern;
  - 2: halo van smeulende as was een donker gat → eigen `SMOLDER_ASH_HALO` `6A4438` (roodgrijs, helderheid as);
  - 3: onderrand niet dicht (wrap) → `nearestSegment` wrapt alleen links/rechts; binnenknopen van de scheur ≤ `FISSURE_LOWEST_NODE` 11,5, rijen 14–15 nu schoon;
  - `crackPath` alleen nog horizontaal (parameter weg);
  - 4: commentaar zijtak gecorrigeerd;
  - 5/6: javadocs `glowingFissure`, `smolderingStone`, `smolderingAsh`, `fissureSegments`, `crackPath`, `nearestSegment` bijgewerkt;
  - 7: drempel `core = 1 − 0,55·SMOLDER_HOT_SHARE` benoemd, redundante `hot` weg;
  - 10: onzichtbare as-vlekjes weg, commentaar gelijkgetrokken.
- Bewust behouden:
  - 8: `LAVA[5]`/`EMBER[5]` blijven als bovenste trede van de paletladders (gedeeld met andere blokken; `checkGlowLayers` eist alleen gemarkeerd ⇒ gloeitoon);
  - 9: CODEBASE/beslissingenlog via de delta-secties (waves §4.8).

## 57a-r1 — uitkomst (in-game beoordeling op het 39d-terrein)
Meting (gemiddelde helderheid / standaardafwijking van de 16×16, vóór → na):

| Blok | Vóór | Na | Wijziging |
|---|---|---|---|
| `ash_block` | 0,368 / 0,065 | 0,358 / 0,053 | rustiger: zwakkere drift van één vlek per tegel, geen houtskool- en bleke spikkels meer, zachtere klontjes; lay-out via `bestOf` |
| `ash_crust` | 0,387 / 0,081 | 0,383 / 0,078 | platen iets gelijkmatiger, randlicht zachter |
| `pumice` | 0,626 / 0,126 | 0,564 / 0,073 | palet een stap donkerder en grijzer, poriën 15 → 10 en lichter, poriënrand een toon lager, minder korrel; `bestOf` |
| `polished_pumice`, `pumice_bricks` | 0,673 / 0,120 | 0,606 / 0,101 | volgen het nieuwe puimsteenpalet (voeg = lichtere porietoon) |
| `smoldering_stone` (+ `_glow`) | 0,212 / 0,155 | 0,176 / 0,099 | gloei-aandeel 0,4 → 0,25, minder en dunnere scheuren (12 → 9 cellen), gloeitonen roder (kern `FA8828`, geen geel-wit), smalle gloeirand alleen in het heetste deel |
| `smoldering_ash` | 0,231 / 0,086 | 0,304 / 0,086 | ondergrond lichter, warmgrijs dicht bij as; halo naast hete scheur roodgrijs (`SMOLDER_ASH_HALO`); gloei ongewijzigd |
| `sulfur_crust` (+ `fumarole_top`/`_side`) | 0,460 / 0,156 | 0,504 / 0,107 | warmer zwavelgeel (gemiddeld `877635` → `968132`), smallere toonladder, zachter knollicht, ondiepere spleten, minder bruine vlekken |
| `glowing_fissure` (+ `_glow`) | 0,383 / 0,219 | 0,293 / 0,150 | scheur van links naar rechts + een uitloper van de bovenrand erin; de onderrand blijft dicht (3 van de 4 randpunten), één zijtak (alleen omhoog/opzij), geen losse haarscheur; smaller; gloeitonen een stap donkerder met een kleine `FFCC5E`-kern (geen `FFF4C4`); rijen 14–15 schoon |

Ongewijzigd (oordeel goed): `cold_lava`, `nether_ice`, `packed_nether_ice`, `rime_netherrack`. Smeulblad en de bosaankleding staan nog niet in de wereld: r2.

**Implementatiedetail:** het voorstel noemde "2 (soms 3) randpunten, afhankelijk van een variant". De blockstate van `glowing_fissure` heeft geen varianten of rotaties (van B), dus één tegel. Gekozen: 3 randpunten (links, rechts, boven). Met 2 (alleen links-rechts) ontstaan strepen.

## CODEBASE-delta
- `tools/textures/Ring3Textures.java` (57a-r1):
  - `ashBlock`, `pumice` kiezen hun lay-out nu via `bestOf` (nieuwe `ashBlockLayout`, `pumiceLayout`);
  - `ASH_PALE` vervallen;
  - `pore` zet de lip een toon onder de lichtste;
  - `SMOLDER_HOT_SHARE` 0,25;
  - `spur(segments, random, edgeWidth, middleWidth)`: uitloper van de bovenrand (kolom `FISSURE_COLUMN`) naar de horizontale scheur;
  - `fissureSegments` = horizontale scheur + `spur` + één zijtak omhoog/opzij (onderrand dicht).
  - Paletten `PUMICE`, `PUMICE_PORE`, `SMOLDER_ASH`, `SULFUR_CRUST` bijgesteld; nieuw `SMOLDER_ASH_HALO`, `FISSURE_LOWEST_NODE` (11,5); `crackPath` alleen horizontaal; `nearestSegment` wrapt alleen links/rechts.
- `textures/block/`: ash_block, ash_crust, pumice, polished_pumice, pumice_bricks, smoldering_stone(+_glow), smoldering_ash, sulfur_crust, fumarole_top, fumarole_side (kopie/zelfde palet als zwavelkorst), glowing_fissure(+_glow) opnieuw gegenereerd; `.mcmeta`'s ongewijzigd.

## Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-08 | textures | 57a-r1: ronde bijsturen op in-game shots van het 39d-terrein (G2-T1 = A) | Gebruiker: "alles zoals voorgesteld" (1–8) |
| 2026-10-08 | textures | Smeulsteen: gloei-aandeel 0,25, dunnere scheuren, roder (geen geel-wit) | Gloeilaag is fullbright: overdag geel confetti met een herhaald vonkjesmotief |
| 2026-10-08 | textures | Smeulende as lichter (~0,30), warmgrijs dicht bij as | Vlekken lazen als donkere gaten in de as |
| 2026-10-08 | textures | As en askorst rustiger dichtbij (geen zwart/wit-spikkels, geen vaste vlek per tegel) | Raster op een asvlakte van dichtbij |
| 2026-10-08 | textures | Puimsteen ~0,56 en zachter (sd ~0,07); gepolijst en bakstenen volgen | Kappen lazen als sneeuw/dioriet; poriën vormden een raster |
| 2026-10-08 | textures | Zwavelkorst warmer geel en rustiger (sd ~0,11) | Van hoog olijfgroen; schubplaten herhaalden zich |
| 2026-10-08 | textures | Gloeiende scheur: 3 van 4 randpunten (onderrand dicht), één zijtak, smaller, gloed een stap donkerder | Vloervlek las als honingraat-behang; geen blockstate-varianten om per blok te variëren |
| 2026-10-08 | textures | Koudlava, Nether-ijs, gepakt Nether-ijs, rijp-netherrack: ongewijzigd | Goed in-game; rijp-netherrack op de toonladder van netherrack |

## Backlog
- `[textures]` Gloeilagen (`neoforge_data`-emissive; smeulsteen, smeulende as, smeulblad, gloeiende scheur, vuurkristal, vuurrobijn) in Sodium/Iris + shaderpacks controleren: niet te testen in de dev-run (geen Sodium/Iris); met een losse test-instance nagaan of ze 's nachts gloeien.
- `[blocks]` `glowing_fissure` (en eventueel `smoldering_stone`) een blockstate met willekeurige rotaties (zoals vanilla-stone/netherrack-varianten) geven: breekt het tegelraster van een vloervlek verder dan één texture kan.
