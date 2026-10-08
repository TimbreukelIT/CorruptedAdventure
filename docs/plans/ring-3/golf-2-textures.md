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

## CODEBASE-delta

## Beslissingen

## Backlog
