# Golf 3 — stroom T (textures): 57, definitieve textures ring 3 + Nether-3

Scope: `textures` — pakket 57 (ronde r1, zo nodig r2); branch `ring3/textures-g3`, poort n = 6 (server 25576, RCON 25586). Opdracht: `golf-3.md` §57; besluiten `fase-1-wereld.md` ("Sparring golf 3"), `teststop-4b-feedback.md`.

## Werk (r1)
1. **Placeholders van de afronding (46b) → eigen textures** in `tools/textures/Ring3Textures.java`:
   - `sulfur_crystal` + `small_/medium_/large_sulfur_crystal_bud` (nu getint zoutkristal/vuurkristalknoppen): gele zwavelnaalden (dubbelpiramides), still en mat zoals het zwavelerts (geen emissive in het model);
   - `yellow_/green_/orange_sulfur_mud` (nu getinte kokende modder): eigen `cycling`-modder (16 × 3, zelfde ritme als `boiling_mud`) per kleur.
2. **`glowing_fissure`: randpunten symmetrisch** (midden van elke rand, 8,0) met een recht, haaks stukje aan elke rand, zodat de 32 varianten (46b) in elke oriëntatie aansluiten; hetzelfde randprofiel op alle vier randen.
3. **`smoldering_stone`:** het celpatroon wrapt, maar met de 46b-rotaties sluiten de scheuren niet meer aan → de gloeiende scheuren blijven van de rand weg (alleen koude scheuren raken de rand), zodat er geen afgeknipte gloeistompjes ontstaan.
4. **Contactblad:** een vloer met willekeurige 46b-varianten (rotatie + spiegeling) voor `glowing_fissure` en `smoldering_stone`, dag en nacht; de zwavelblokken in een zwavelveld-scène.
5. **Aankleding (`teststop-4b.md` §I, 47d/46b):** in-game beoordelen; bijsturen alleen waar het beeld erom vraagt.
6. **`steam_puff`/`ice_glint`/`sulfur_steam`-tint** in-game beoordelen (tint zit in `client/particle`, categorie `atmosphere`: alleen de sprites zijn van T).
7. **Shots in een echte omgeving** (seed 20260930, eigen server + client, dag/nacht), contactbladen in `build/57-r1/`.
8. **Sodium/Iris-check** (max. ~30 min): losse test-instance in `build/sodium/`.
9. B + D.

## Client/server-impact
Geen (assets). Geen modellen/blockstates gewijzigd tenzij expliciet vermeld.

## W-checklist (INT3)
- [ ] Vloervlek `glowing_fissure` (kloofbodem Nether-3), dag/nacht: scheuren sluiten in elke variant aan, geen afgeknipte lavastompjes.
- [ ] `smoldering_stone` 's nachts: geen afgeknipte gloeistukjes aan blokranden.
- [ ] Zwavelvelden: kristallen en de drie modders passen bij korst en travertijn; stoom niet als grijze blokken.

## Voortgang
- Voortgang: Sat Oct 10 11:40 WEDT 2026 worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a9ce77269195b5bd3`, branch `ring3/textures-g3` (vanaf `e2a0366`); r1 gestart.
- Voortgang: Sat Oct 10 11:51 WEDT 2026 `Ring3Textures.java` r1: scheur met symmetrische randpunten + afgekoelde naad, smeulsteen-gloed van de randen weg, zwavelkristallen en drie zwavelmodders eigen textures, variantvloer op het contactblad; script lint 0, `checkGlowLayers`/`checkFissureEdges` groen.
- Voortgang: Sat Oct 10 11:53 WEDT 2026 B groen (`build --rerun-tasks`, 1 slot, daarna vrij).
- Voortgang: Sat Oct 10 12:05–12:23 WEDT 2026 eigen server (25576/RCON 25586, wereld `t57-r1`, seed 20260930) + client (lock + slot); doelen gevonden via RCON-probe rond de INT2-posities (`build/57-r1/found.json`), shots dag/nacht in `build/57-r1/shots/`; geel modderpalet bijgesteld (viel weg tegen de korst), Nether-shots opnieuw met nachtzicht en een camera in de lucht.
- Voortgang: Sat Oct 10 12:23–12:28 WEDT 2026 Sodium/Iris-check (timebox ~30 min, binnen 5 min klaar), zie Backlog.
- Voortgang: Sat Oct 10 13:00 WEDT 2026 server/client gestopt (RCON uit), lock en slots vrij, `run/mods`/`run/shaderpacks` weg; D groen (`runData`, `src/generated` ongewijzigd); script 2× → md5 identiek; `.mcmeta`'s (alleen LF/CRLF) teruggezet. Contactbladen `build/57-r1/sheet-r1-1-sulfur.png`, `-2-ember.png`, `-3-nether.png`, `-4-sodium.png` + `build/texture-preview/ring3.png` aan main ("57 r1 klaar voor oordeel"). Nog niet gecommit: wacht op het oordeel van de gebruiker.
- Voortgang: Sat Oct 10 2026 oordeel gebruiker: 57 r1 in zijn geheel goed (alle blokken "goed"), geen r2; zachtere stoomsprite naar de backlog. Commit.

## r1 — wijzigingen per blok
| Blok | Wijziging |
|---|---|
| `glowing_fissure` (+ `_glow`) | Randpunten in het midden van alle vier randen (8,0) met een recht, haaks stompje van 3 px: hetzelfde spiegelsymmetrische randprofiel op elke rand (`checkFissureEdges`), dus aansluiting in alle 32 varianten van 46b. De buitenste 2 rijen zijn een afgekoelde, niet-gloeiende naad (`LAVA[0]`); binnenin een hoofdscheur via twee knooppunten, twee dunne armen (boven/onder) naar hetzelfde knooppunt, één zijtak. Kanttekening: een volle vloer leest per blok als een "ster" (4 armen); in de kloven (lijnen) valt dat weg. |
| `smoldering_stone` (+ `_glow`) | De hitte dooft naar de randen (`awayFromEdges`): geen afgeknipte gloeistukjes aan blokranden met de 46b-rotaties; alleen koude scheuren raken de rand. |
| `sulfur_crystal`, `small_/medium_/large_sulfur_crystal_bud` | Eigen textures i.p.v. placeholders: gele zwavelprisma's met piramidepunt (2/3/4/6 kristallen), mat en still zoals het zwavelerts; selectieve omlijning in oker. |
| `yellow_/green_/orange_sulfur_mud` | Eigen `cycling`-modder (16 × 3, ritme van `boiling_mud`) in een eigen palet: citroengeel, olijfgroen, roestoranje; iets meer toonspreiding (`SULFUR_MUD_SPREAD`). `boiling_mud` ongewijzigd (md5 gelijk). |
| Overige ring 3/Nether-3 | Ongewijzigd (4b: "niet genoemd = akkoord"); in-game gefotografeerd ter beoordeling. |
| `steam_puff`/`sulfur_steam`/`ice_glint` | Tint past (stoom bijna wit, zwavelstoom licht geelgroen, glinsters wit-blauw); de stoomwolkjes ogen grof (8×8-sprite groot geschaald): eventueel zachter in r2 (sprite) of kleiner (particle-klasse, `atmosphere`). |

## CODEBASE-delta
- `tools/textures/Ring3Textures.java` (57 r1):
  - `glowingFissure`: `fissureSegments` nieuw opgezet (vier randstompjes `FISSURE_STUB`, randpunt `FISSURE_EDGE_POINT` = midden, binnenknopen `FISSURE_MARGIN` via `innerPoint`, afgekoelde naad `FISSURE_CRUSTED`); `nearestSegment` zonder wrap; `spur`, `crackPath`, `inside`, `FISSURE_ROW`/`FISSURE_COLUMN`/`FISSURE_LOWEST_NODE` vervallen; nieuwe controle `checkFissureEdges` (randpatroon op alle vier randen gelijk en spiegelsymmetrisch).
  - `smolderingStone`: hitteveld via `awayFromEdges(field)` (0 aan de rand, vol vanaf 3 px).
  - `sulfurCrystal(image, random, stage)` + `sulfurPrism` (zwavelkristal, vier stadia); `boilingMud(image, random, phase, mud, sheen, spread)` met paletten `YELLOW_/GREEN_/ORANGE_MUD` (+ `_SHEEN`) en `SULFUR_MUD_SPREAD`.
  - Contactblad: muren van de vier modders, zwavelkristallen in de slotrij, `VARIANT_FLOORS` (8×8-vloer in willekeurige oriëntaties, dag/nacht, `drawVariantFloor`, `oriented`).
- `textures/block/` (uit het script): `sulfur_crystal`, `small_/medium_/large_sulfur_crystal_bud`, `yellow_/green_/orange_sulfur_mud` (eigen pixels i.p.v. de 46b-placeholders; `.mcmeta`'s ongewijzigd), `glowing_fissure(_glow)`, `smoldering_stone(_glow)`.

## Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-10 | textures | `glowing_fissure`: randpunten in het midden van elke rand met een symmetrisch randprofiel; de naad tussen scheurblokken is afgekoeld (niet-gloeiend) | Besluit gebruiker (sparring golf 3): aansluiten in alle 32 varianten; met gloeiende naden werd een vlak 's nachts een raster van felle lijnen |
| 2026-10-10 | textures | `smoldering_stone`: gloed blijft van de blokranden weg | De 46b-rotaties sluiten de wrappende scheuren niet meer aan: afgeknipte gloeistukjes |
| 2026-10-10 | textures | Zwavelkristal: matte, stille zwavelprisma's (geen emissive/puls); zwavelmodders citroengeel / olijfgroen / roestoranje | Zwavel is mat (erts, item); het zwakke licht komt van het lichtniveau van het blok; geel moest afsteken tegen de korst |

## Backlog
- `[textures]` **Afgehandeld (57 r1):** gloeilagen in Sodium/Iris. Sodium 0.6.13 + Iris 1.8.12 (NeoForge 1.21.1, Modrinth, sha512 gecontroleerd; in de dev-client van de worktree): smeulsteen, smeulende as, smeulblad, gloeiende scheur en vuurkristal gloeien 's nachts identiek aan de vanilla-renderer; met shaderpack Complementary Reimagined r5.9.3 gloeien smeulsteen, smeulende as, smeulblad en vuurkristal ook. Vuurrobijn en de scheur met shaderpack niet expliciet gezien. Bewijs: `build/57-r1/sheet-r1-4-sodium.png`, `build/57-r1/shots/shader_*.png`.
- `[textures]` **Afgehandeld (57 r1):** placeholders zwavelvelden (46b) en randpunten `glowing_fissure` (46b-backlog).
- `[textures/atmosphere]` Stoomwolkjes (`steam_puff`/`sulfur_steam`) ogen grof en groot (8×8-sprite, `quadSize` × tot 2,5): later een zachtere sprite (textures) en/of kleinere wolkjes (particle-klasse, atmosphere). Niet in 57 (gebruiker 2026-10-10).
