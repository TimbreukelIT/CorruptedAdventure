# Golf 1 — stroom W-terrein: 39 (ring 3-terrein overworld)

Scope: `worldgen` — werkpakket 39 (alleen terrein en vloeistofregel), branch `ring3/terrain-g1`.

**Doel:** het terrein van ring 3 dat het wow-moment draagt (teststop 4a, blokkerend). Het vervangt het placeholder-terrein (`ring_3/overworld.json`) door een eigen ring 3-invoer van `ring_blend`. De vormen:
- horizon-vulkanen boven de wolken;
- caldera's (actief met lavameer, slapend);
- lavarivieren in kanalen;
- zeldzame zwevende basalteilanden;
- golvende asvlaktes;
- de lege zeekom (`dried_seabed`);
- de vorm van het titanenveld (bijv. een vlakte met lage ruggen).

De gedeelde maskers uit `contract.md` §4 worden ingevuld. Surface tijdelijk met vanilla-blokken (basalt, blackstone, magma, smooth basalt); de echte blokken volgen in 39c.

Bron: `fase-1-wereld.md` Ring 3 + "Uitwerking sparring ring 3"; `contract.md` §4; `CODEBASE.md` valkuilen (interpolatie 4×8×4, `core_mask`, `ColumnTable`, geen `ThreadLocal` per instantie, `HeightGrid`, zwevende blokken); sjablonen `CanyonDetails` (rotsvormen per kolom), void-eilanden (`void/overworld.json`), `dry_riverbeds`-beddingen (change 31).

## Werk
1. **Hoogte (2D):** `ring_3/overworld/height` = asvlakte (Y ~80–100, golvend) + kegels + caldera's + zeekom-kom + kanalen. Regio's sturen via `region_heat`/`region_ground` (39a): pieken bij heet, zeekom bij koel.
2. **Kegels en caldera's:**
   - eigen Java-celtype (jittered grid, zoals de hoodoos), deterministisch per seed;
   - kegelprofiel met kraterrand en een calderabodem 20–50 onder de rand;
   - een fractie slapend (`caldera_dormant` = 1, bodem vlak en lager, geen lava);
   - hoogtes (V5 = A): de grootste kegels Y 230–260, gewone Y 140–200.
   - Kegels moeten vanuit `dry_riverbeds` boven de horizon uitsteken: de hoogste kegels liggen niet alleen diep in de ring.
3. **Lavarivieren:** `lava_channel` op eigen ruis (zoals `dry_riverbeds_channel`), bedding 1–3 diep onder de oever.
   - Lava via `RingFluidPicker` (ring 3-regel) of via de surface; kies en meet.
   - Geen stromende lava bij generatie behalve met een cap (spaarzame lavavallen zijn golf 2-features).
4. **Zwevende eilanden** (`island_mask`): alleen boven caldera's en kanalen; Y 150–210, ~1 cluster van 1–4 eilanden (8–30 breed) per ~3 caldera's (V4 = A); lavavalletjes eraf zijn golf 2-features; 3D-ruis alleen waar het masker > 0. Carvers mogen geen zwevende brokken maken (meten, zoals 23b).
5. **Vloeistof:** geen water boven ~Y 0 in ring 3 (de dried seabed blijft leeg). Lava in calderameren en kanalen. Aquifers (V8 = A): water alleen diep ondergronds (onder ~Y 0; floodedness in 42), geen water in de slapende krater; `caldera_dormant` stuurt ook de biome `dormant_caldera` (router-as `weirdness`). In `dry_riverbeds` blijft de regel van ring 2 tot gewicht 0,5.
6. **Zone 2→3:** de beddingsnede van change 31 herzien (backlog r. 122): de beddingen lopen uit vóór de grens. De kegels staan achter de zone (`core_mask`), zodat de smoothstep ze niet afkapt.
7. **Prestatie:** indicatief meten in de eigen worktree; officieel in INT. Budget ≤ 1,75×.

## Afhankelijk van open vragen
- Geen meer: V4, V5 en V8 = A (2026-10-06), verwerkt hierboven.

## Client/server-impact
Geen; puur worldgen, deterministisch.

## Tests
- **B**, inclusief JSON-tests voor `ring_3/overworld*` (zoals `Ring2TerrainJsonTest`).
- **D.**
- **S.**
- Ring 1/2 en zone 1→2 onveranderd (biomes + steen/deepslate).
- Naadcontrole zone 2→3: geen kliffen of zwevende brokken.
- Een terreinkaart (via 37d als die al gemerged is, anders het tijdelijke debugcommando van change 23, niet gecommit).
- **W-checklist** met coördinaten op seed `20260930` voor 4a: horizon-uitzicht vanuit `dry_riverbeds`, een actieve en een slapende caldera, een kanaal, een eilandcluster, de zeekom, het titanenveld.

## Bij afronding
`waves.md` §4: delta-secties onderaan, rapport aan main (met de gemeten hoogtes, frequenties en prestatie).
