# Golf 1 — stroom T (textures): 57a eerste goede versies

Scope: `textures` — werkpakket 57a; branch `ring3/textures-g1`. (57 = de eindronde na teststop 4b.)

**Doel:** direct goede eerste versies (antwoord 20) van alle ring 3- en Nether-3-textures uit `contract.md` §3 en §6 (particles), in de 37b-stijl. Teststop 4a beoordeelt het wow ook op kleur en gloed. Er is geen placeholder-ronde.

Bron: `fase-1-wereld.md` §1.4 (stijl, inclusief de aanvullingen van 37 en 37b), Ring 3/Nether-3-secties; `contract.md` §7; sjabloon `tools/textures/Ring2Textures.java` (`Animation`, `still`/`pulsing`/`cycling`, `sprite(rows, legend)`, `SIDE_RULES`, `lint`, contactblad) en `tools/textures/VanillaCompare.java`.

## Werk
1. **`tools/textures/Ring3Textures.java`:** single-file Java, alleen JDK, vaste seed per bestand. Helpers kopiëren uit `Ring2Textures.java`. De ontdubbeling staat in de backlog (r. 48/151/166) en is een aparte `infra`-keuze; hier niet doen tenzij main dat vraagt.
2. **Paletten** (5–7 tonen, hue-shift):
   - as (warm grijs) en puimsteen (licht, poreus);
   - smeulsteen en `smoldering_ash` (zwart met oranjerode emissive scheuren, puls);
   - verkoold hout (zwart, schors met gloeiende kerven op de log-zijkant?, als accent);
   - `ember_leaves` (donkere kronen met gloeiende vlekjes, emissive);
   - vuurflora; vulkaniet (donker metaal met paarsrode glans), zwavel (geel), vuurrobijn (diep rood, emissive puls);
   - vuurkristal (oranje-goud, emissive);
   - zwavelkorst, fumarole, kokende modder (`cycling`), geiser;
   - Nether-3: koudlava (grijsblauw gestold), Nether-ijs, rijp-netherrack (netherrack-toonladder met rijp, les 37b: vanilla-ertsen moeten erin passen), rijpzwam, ijsstalactiet, gloeiende scheur (emissive), oerijs (helder, bijna wit-blauw, licht pulserend?).
   - Gloed en puls volgens §1.4.
3. **Particles:** `ash_flake_0..n`, `ember_0..n`, `steam_puff_0..n`, `ice_glint_0..n` (grijswaarden als ze getint worden; afstemmen met A via main).
4. **Controles:** afmeting, alpha, `cycling` opaak, `SIDE_RULES`, `lint` (vlakheid), contactblad `build/texture-preview/ring3.png` met secties RING 3 / NETHER RING 3, plus `VanillaCompare` voor rijp-netherrack met vanilla-ertsen.
5. Alleen PNG's en `.mcmeta`'s onder de contractnamen; **geen** models of blockstates (die zijn van B).

## Client/server-impact
Geen (assets).

## Tests
- Script 2× → md5 identiek; lint 0.
- **B** (assets kapot = geen B-fout, maar draai hem toch).
- Na integratie met B: `runClient` zonder `Missing textures`.
- Contactblad aan main (main legt het aan de gebruiker voor vóór 4a).

## Bij afronding
`waves.md` §4.
