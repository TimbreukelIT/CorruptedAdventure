# Ring 5 golf 2 — stroom T (textures): 99b bijsturen

Scope: `textures` — pakket 99b, branch `ring5/99b`, worktree `P:/Projecten/Java/wt/r5-99b`, poort n = 10, basis
`061a20c4` (main na INT2). Bronnen: `golf-1-textures.md` (99a: paletten, aanpak), `teststop-6a.md` §D/E,
`golf-2-biomes.md` §6 (op `ring5/94`: "wat 99b straks nodig heeft"), opdracht van main.

**Doel:** de ring 5-textures bijsturen op wat in het landschap van golf 2 zichtbaar wordt, in de stijl van 99a; geen
nieuwe blokken, geen ander gedrag, geen nieuw kleurenthema.

**Bezit:** `tools/textures/Ring5Textures.java` en zijn PNG's; in `ModBlockStateProvider.ring5()` alleen de modelvarianten
van `glitch_block`, `voidstone` en `corrupted_stone` (models zijn `textures`, zoals bij ring 1/2).

---

## 1. Werk

| Punt | Probleem | Aanpak |
|---|---|---|
| `voidstone` | één tegel per blok: de kruisster en de sterren stonden op grote vlakken in een raster | **32 oriëntaties** (`rotatedGlowingCube`, zoals `glowing_fissure`; de sterrenlaag draait mee); tegel: zachte nevelslierten (laag contrast, anders verraden ze het blokraster), sterren in drie helderheden, kruisster met zwakke armen (`STAR[1]` `8E7AC2`), andere sterren ook op de rand (afstand gemeten over de tegelrand), drie niet-gloeiende stofjes `STAR_DUST` `3E3058` |
| `glitch_block` | een vlek glitchblokken vormde één egaal schaakbord | basis-schaakbord 2–4 px opzij geschoven en één kwadrant als 4 × 4-schaakbord (vast over de frames; de storingen per frame blijven); **32 oriëntaties** (nieuwe helper `rotatedCube`), zodat buren nooit aansluiten |
| `corrupted_sign` | zwarte en vooral donkerrode tekst (verfkleur × 0,4) slecht leesbaar; een ader liep door de tekst | eigen lichter, rustiger palet `SIGN_WOOD` (`7E7A76`…`BFBBB6`, nog steeds grijs hout) voor bordvlak en randen, zachte naden, ader in de onderste naad onder de tekst; hangbord: binnenvlak `SIGN_WOOD`, ader in de onderkant van de lijst (ook in `gui/hanging_signs/corrupted`, het bewerkscherm) |
| `corrupted_leaves` | van onderen (vanilla schaduwt een ondervlak tot de helft) zwart kant-werk; één groot gat per tegel herhaalde zichtbaar | palet lichter (`353933`…`8E9488`, 7 tonen), opbouw uit kleine blaadjes van 3–5 px (diagonaal beide kanten op, ruitjes), lichte punt en donkere voet, ~60 % bedekt (eik ~67 %: nog steeds dunner); gemiddelde helderheid ≈ getinte eik (88 tegen 82–85) |
| `corrupted_stone` (extra) | op de onderkant van de schotsen (waar de omgekeerde bomen hangen) stonden de aders in een raster | **32 oriëntaties** (`rotatedCube`) i.p.v. alleen de 4 Y-rotaties van vanilla-steen; texture ongewijzigd |
| `corrupted_log` | als kale/hangende stam | in-game bekeken: leest goed, ongewijzigd |

## 2. Client/server-impact
Alleen assets en datagen-modellen (client-resources); geen code in de mod, geen payload. Blockstate-varianten hebben geen
gameplay-effect.

## 3. Tests
- Script: controles + `lint` 0 waarschuwingen; twee runs byte-identiek.
- `./gradlew build --offline` groen; `runData` schoon (alleen de bekende `BiomeFilter`-meldingen; 3 blockstates gewijzigd,
  2 modellen nieuw); dedicated server (verse wereld) 0 ERROR.
- In-game (studiodimensies, dev-client): voidsteenmuur dag en nacht, glitchvlekken in corrupted_stone, bord + hangbord
  met zwarte/rode tekst, omgekeerde boom naast een omgekeerde eik onder een stenen plaat; client-log zonder model- of
  texturefouten.

Beelden (niet in git) in `build/test/maps/99b/`: `ring5-before.png` (contactblad 99a), `preview-before.png` /
`preview-after.png` (muren vast vs. 8 oriëntaties, glitchvlekken + mip, borden met tekst, bladeren van onderen naast eik),
`leaves-old-new-oak.png`, `ingame-*.png` (tussenstand) en `after-*.png` (eindstand: `after-day-overview`,
`after-night-overview`, `after-signs`, `after-inverted-tree-vs-oak`). Contactblad 99b:
`build/texture-preview/ring5.png`.

## Voortgang
- 2026-10-24 11:30 worktree `ring5/99b` vanaf `061a20c4`.
- 2026-10-24 12:05 textures + modellen klaar, gates groen, in-game bekeken.
- 2026-10-24 INT3 (main): gemerged als `7c8e1cf6` (conflict alleen in de `CODEBASE.md`-regel van `Ring5Textures.java`); `/code-review` op `e6afdf8e` bij INT3 (zie `teststop-6b.md` §N). Open vragen voor 6b: is het lichtere bordgrijs goed, zijn de bladeren dun genoeg (§M).
- 2026-10-24 INT3-review van `e6afdf8e` (geen correctheidsfouten; op machtiging van de gebruiker opgelost): sterren en stof van de voidsteen weer 1 px van de tegelrand (met 32 oriëntaties konden twee randsterren van buren een dubbelster vormen), stof ook weg van de armen van de kruisster, `voidstone_nullite_ore_glow` (de sterren onder de kristallen, zelfde fase), het commentaar over de glitch-slip (de oriëntaties breken het schaakbord, niet de slip) — `21d75e65` `textures`; beide nullieterts in 32 oriëntaties en één oriëntatie-helper in `ModBlockStateProvider` — `a2e436ee` `blocks` (de blockstate-wijzigingen van 99b hoorden bij `blocks`; geschiedenis niet herschreven, vervolg onder `blocks:`). Bewust niet: het glitchblok blijft in 32 oriëntaties, ook al lopen de scheurstrepen dan op veel vlakken verticaal (zonder X-rotaties tegelt het weer tot één schaakbord; oordeel bij 6b); `clumps` in `corruptedLeaves` blijft (stuurt de helderheid van de blaadjes; voor de bedekking gebruiken gaf weer gaten, het oude probleem). In-game bekeken: voidsteenmuur, nullieterts in beide gesteenten en glitchblokken bij nacht (`build/test/shots/int3/ores-night.png`), client-log zonder modelfouten.

## CODEBASE-delta
- `ModBlockStateProvider`: nieuwe helper `rotatedCube(block)` (gewoon + `<naam>_mirrored` in de 16 x/y-rotaties, zonder
  gloedlaag); `glitch_block` en `corrupted_stone` via `rotatedCube`, `voidstone` via `rotatedGlowingCube`. Nieuw gegenereerd:
  `models/block/glitch_block_mirrored.json`, `voidstone_mirrored.json`.
- `Ring5Textures.java`: `SIGN_WOOD`, `STAR_DUST`, `STAR[1]` (armen kruisster), `LEAFLETS`, `starFits`/`dustFits`/
  `wrappedDistance`; herschilderd: `corrupted_leaves`, `glitch_block`, `voidstone` (+ `_glow`), `entity/signs/corrupted`,
  `entity/signs/hanging/corrupted`, `gui/hanging_signs/corrupted`.

## Beslissingen (implementatie, binnen 99a-stijl; geen gebruikersbesluit)
- Borden krijgen een eigen lichter grijs (`SIGN_WOOD`) i.p.v. het plankenpalet: leesbaarheid gaat voor gelijkheid met de
  planken; het blijft grijs uitgeloogd hout.
- Bladeren: helderheid gelijk aan getinte eik, bedekking 60 % (blijft "dunner dan vanilla" uit 99a).
- Voidsteen-nevel bewust zwak: elke zichtbare vlek per tegel verraadt het blokraster, ook met rotaties.
- `corrupted_stone` 32 oriëntaties: zelfde probleem als voidsteen, zelfde oplossing; geen texturewijziging.
- `voidstone_nullite_ore` en `nullite_ore` blijven in één oriëntatie (zeldzaam, overlay-model).

## Backlog
- Geen nieuwe punten.
