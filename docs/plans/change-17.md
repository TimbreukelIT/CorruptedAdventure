# Change 17 — Definitieve textures ring 1

Scope: `textures` — change 17

**Doel:** de placeholder-textures (getinte vanilla) uit change 10 en 15 vervangen door eigen, definitieve 16×16-textures voor dood gras / verdorde aarde, de dood-hout-set en het zielkristal-erts, in één herkenbare stijl die we voor ring 2–5 (changes 37, 57, 76, 95) hergebruiken. Alleen assets en block-/itemmodels; geen gedrag, geen registries.

---

## Afhankelijkheden en aannames

- Changes 0–16 zijn gebouwd zoals gepland in `fase-1-wereld.md` §3. De plannen van 10 en 15 bestonden nog niet toen dit plan werd geschreven; **controleer bij de start de echte registry-namen en blokken in `CODEBASE.md` (tabel Blocks/Items) en pas de lijst hieronder daarop aan.**
- Aangenomen namen (werktitels): `withered_dirt`, `dead_grass_block`, `dead_log`, `dead_wood`, `stripped_dead_log`, `stripped_dead_wood`, `dead_planks`, `dead_stairs`, `dead_slab`, `dead_fence`, `dead_fence_gate`, `dead_door`, `dead_trapdoor`, `dead_pressure_plate`, `dead_button`, `soul_crystal_ore`, `crimson_soul_crystal_ore` (+ eventueel item `soul_crystal`).
- Change 10 levert de models/blockstates via een datagen-provider (bijv. `ModBlockStateProvider` / `ModItemModelProvider`) en placeholder-PNG's in `assets/corruptedadventure/textures/block/` onder de **definitieve bestandsnamen**. Change 17 vervangt die PNG's 1-op-1 en past alleen models aan waar dat hieronder staat.
- **Change 10 bouwt al een eerste, eenvoudige versie van het texture-script** (`tools/textures/…`, alleen JDK, eigen pixels; opzet volgens dit plan). Change 17 schrijft het niet opnieuw, maar **verfijnt** het tot de definitieve textures: palet, patronen, animatie, contactblad, `handEdited`-lijst. Staan de zielkristal-placeholders (change 15) nog niet in het script, dan komen ze er in deze change bij.
- **Change 15 levert twee ertsen:** `soul_crystal_ore` (netherrack) en `crimson_soul_crystal_ore` (crimson nylium met kristal).
- Borden (sign/hanging sign) en boten zitten **waarschijnlijk niet** in change 10 (vragen een eigen `WoodType`, block entities en voor boten een enum-extensie). Staan ze er wel in, dan horen de voorwaardelijke textures hieronder erbij.
- Er zijn geen bladeren of sapling (bladloze bomen, §2 Ring 1).
- Niet opnieuw openen: placeholders eerst, per ring één textures-change (beslissingenlog 2026-09-30).

---

## Plan

### 1. Volledige texturelijst (16×16, tenzij anders vermeld)

Pad: `src/main/resources/assets/corruptedadventure/textures/…`

| Bestand | Voor | Opmerking |
|---|---|---|
| `block/withered_dirt.png` | verdorde aarde; ook onderkant en particle van het grasblok | naadloos tegelbaar |
| `block/dead_grass_block_top.png` | bovenkant dood gras | vaste kleur, geen biome-tint (vraag 4), naadloos |
| `block/dead_grass_block_side.png` | zijkant | volledig ingekleurd (gras-rand + verdorde aarde), geen aparte overlay als er geen tint is |
| `block/dead_grass_block_snow.png` | zijkant met sneeuw erop | **alleen als** change 10 de `snowy`-state heeft (vanilla `grass_block_snow` past niet: die heeft vanilla-aarde onderaan) |
| `block/dead_log.png` | zijkant log; ook alle zijden van `dead_wood` | verticale nerf, gebarsten schors |
| `block/dead_log_top.png` | kopse kant | jaarringen + schorsrand |
| `block/stripped_dead_log.png` | zijkant gestript; ook `stripped_dead_wood` | |
| `block/stripped_dead_log_top.png` | kopse kant gestript | |
| `block/dead_planks.png` | planks; ook stairs, slab, fence, fence gate, pressure plate, button (hergebruik, geen eigen textures) | 4 planken met naden, zoals vanilla-indeling |
| `block/dead_door_top.png`, `block/dead_door_bottom.png` | deur | eventuele gaten: alpha alleen 0 of 255 (cutout) |
| `block/dead_trapdoor.png` | valluik | idem |
| `item/dead_door.png` | deur in inventory | |
| `block/soul_crystal_ore_overlay.png` + `.mcmeta` | kristallen over netherrack; ook de onderkant van `crimson_soul_crystal_ore` | transparant, alpha 0/255; pulserende animatie als verticale strip 16×(16·n) |
| `block/crimson_soul_crystal_ore_overlay_top.png` + `.mcmeta` | kristallen die door de crimson nylium steken (bovenkant) | idem; kristallen in open plekken van het nylium-patroon |
| `block/crimson_soul_crystal_ore_overlay_side.png` + `.mcmeta` | kristallen op de zijkant | idem; bovenste rij pixels vrij laten, zodat de rode nylium-rand van vanilla `crimson_nylium_side` zichtbaar blijft |
| `item/soul_crystal.png` + `.mcmeta` | **alleen als** change 15 een `soul_crystal`-item als drop heeft | zelfde puls, geen emissive |

Voorwaardelijk (alleen als change 10 ze heeft):
- Borden: `entity/signs/dead.png` (64×32), `entity/signs/hanging/dead.png` (64×32), `gui/hanging_signs/dead.png` (16×16), `item/dead_sign.png`, `item/dead_hanging_sign.png`.
- Boten: `entity/boat/dead.png`, `entity/chest_boat/dead.png` (beide 128×64), `item/dead_boat.png`, `item/dead_chest_boat.png`.

Kern: **15 PNG's + 3 `.mcmeta`** (+ 1–2 voorwaardelijk voor sneeuw/item), plus maximaal 9 voor borden/boten. De drie overlays gebruiken hetzelfde aantal frames en dezelfde `frametime`, zodat beide ertsen gelijk pulseren.

### 2. Productie (realistisch voor een AI-agent)

Claude kan niet met de hand pixelen in een editor. Mogelijkheden (keuze: vraag 1 → **A**):

- **A. Generatorscript met eigen pixels (gekozen).** Het script dat change 10 opzet, bijv. `tools/textures/Ring1Textures.java` (single-file Java, draait met `java tools/textures/Ring1Textures.java`, alleen JDK: `BufferedImage` + `ImageIO`, headless; geen Python of extra dependencies nodig). Change 17 verfijnt het tot de definitieve versie. Het tekent elke texture uit **eigen** bouwstenen: een vast palet per materiaal (4–6 tinten, vastgelegd in het design-doc), seeded waarderuis voor korrel, en per soort een patroon (plank-naden, verticale nerf, jaarringen, grassprieten die over de rand hangen, kristalfacetten met lichte kant/schaduwkant). Vanilla-textures dienen alleen als **visuele referentie** voor indeling en contrast; er worden geen vanilla-pixels gekopieerd. Deterministisch (vaste seed), dus opnieuw draaien geeft exact dezelfde PNG's. Na beoordeling in runClient mag de gebruiker losse bestanden met de hand bijwerken (bijv. in Aseprite/Pixelorama/Blockbench); het script krijgt dan een lijst `handEdited` die het overslaat, zodat handwerk nooit wordt overschreven.
- **B. Script dat vanilla-textures hertint/bewerkt.** Snel en dicht bij de vanilla-look, maar de mod-jar bevat dan afgeleide Mojang-assets. Dat is in de modwereld gebruikelijk en wordt gedoogd, maar juridisch grijs; bovendien lijkt het resultaat sterk op bestaande houtsoorten.
- **C. Gebruiker tekent alles.** Claude levert de lijst, palet, afmetingen en een referentieblad; de gebruiker tekent. Beste kwaliteit, kost de gebruiker de meeste tijd; Claude doet daarna models, mcmeta en de check.
- **D. AI-beeldgenerator** (bijv. de `minecraft-imagegen`-skill): 16×16-pixelart uit een beeldmodel verkleint tot modder en past niet naadloos; niet aanbevolen voor bloktextures, hooguit als moodboard.

Uitvoering:
1. Gekozen stijl vastleggen in `fase-1-wereld.md` (nieuw kopje onder §1.4 "Stijl" + Ring 1-sectie) en in de beslissingenlog: script met eigen pixels; dood hout verbleekt grijsbruin (drijfhout); dood gras vaal geelbruin (stro) op grijsbruine verdorde aarde, zonder biome-tint; zielkristal zielblauw/cyaan, emissive en pulserend. Per materiaal een palet van 4–6 tinten (hex) noteren.
2. Script uit change 10 verfijnen: per texture een eigen tekenmethode met de definitieve paletten en patronen, gedeelde helpers (ruis, palet-kwantisering) in hetzelfde bestand; animatieframes + `.mcmeta` voor de drie overlays; de nieuwe overlays voor `crimson_soul_crystal_ore`. Pas bij ring 2 (change 37) gedeelde delen afsplitsen als dat nodig blijkt.
3. PNG's genereren direct in `assets/corruptedadventure/textures/…`, over de placeholders heen.
4. Contactblad (één PNG met alle textures 8× vergroot, naast een 3×3-tegeling) naar `build/texture-preview/` — niet in git — voor snelle beoordeling zonder game.
5. Models aanpassen (zie §4), `runData`, runClient-check, iteratie op palet/patroon met de gebruiker.

Opmerking scope: het script is de bron van de textures en hoort dus bij deze change; het wordt **niet** in Gradle gehangen (dat zou `infra` zijn → zo nodig backlog).

### 3. Animatie en gloed (zielkristal)

Gekozen (vraag 3 → A): emissive overlay + puls, kleur zielblauw/cyaan.

- Animatie: elke overlay (`soul_crystal_ore_overlay`, `crimson_soul_crystal_ore_overlay_top`, `…_side`) krijgt een `.mcmeta` met `{"animation": {"frametime": 4, "interpolate": true}}` over bijv. 8 frames: een langzame helderheidspuls (≈1,6 s per cyclus). `interpolate` mengt kleuren tussen frames, dus weinig frames volstaan. Alleen de overlays animeren, niet de netherrack/nylium.
- Emissive ("fullbright", gloeit in het donker zonder dat het blok licht geeft): NeoForge-uitbreiding op block models, `neoforge_data` met `block_light`/`sky_light` 15 op het overlay-element. In datagen via `BlockModelBuilder.element()…emissivity(15, 15)` + `renderType("cutout")` (API bij het bouwen verifiëren in NeoForge 21.1).
- Echte lichtbron (`lightLevel` op het blok) is blokgedrag → `blocks`, niet in deze change; zo gewenst naar de backlog.
- Items: geen emissive (NeoForge-itemlagen met licht zijn in 1.21.1 omslachtig); het item mag dezelfde puls-animatie krijgen via `.mcmeta`.

### 4. Model-wijzigingen (datagen-provider uit change 10/15)

- `soul_crystal_ore`: van één `cube_all` naar een **tweelaags-model**: onderlaag `minecraft:block/netherrack` (verwijzing naar de vanilla-texture, geen kopie in de jar), bovenlaag dezelfde kubus met `cullface` per zijde en `#overlay`, `render_type` cutout, emissive (`emissivity(15, 15)`). Dit maakt ook het licentieprobleem van een samengestelde erts-PNG weg.
- `crimson_soul_crystal_ore`: zelfde opzet met een `cube_bottom_top`-onderlaag die verwijst naar vanilla `minecraft:block/crimson_nylium` (top), `crimson_nylium_side` (zijkant) en `netherrack` (onder); de overlay-kubus gebruikt per vlak `#overlay_top`, `#overlay_side` en (onder) `soul_crystal_ore_overlay`. Emissive en cutout als hierboven. Het model is gedeeld opgezet (één helper in de provider voor "basis + emissive overlay"), zodat latere ringen het hergebruiken.
- `dead_grass_block`: vaste kleur, geen biome-tint (vraag 4 → A). Gebruikt change 10 het vanilla-grasmodel met `tintindex`/overlay, dan wordt het een eigen `cube_bottom_top` (top, side, bottom = `withered_dirt`) zonder tint; een eventuele `BlockColor`-registratie uit change 10 vervalt (die weghalen raakt `client/`-code: dan test S erbij).
- `dead_door` / `dead_trapdoor`: `renderType("cutout")` controleren (nodig zodra de textures gaten hebben).
- Overige blokken: geen modelwijziging, alleen nieuwe PNG's.
- Na de wijzigingen `./gradlew runData`; gegenereerde models committen. Let op: de NeoForge-modelproviders controleren of textures bestaan, dus **eerst de PNG's genereren, dan `runData`**.

### 5. Documentatie

- `CODEBASE.md`: `textures/` ✅ met de lijst per ring, het texture-script (uit change 10) bijwerken met de definitieve rol en de `handEdited`-lijst, valkuilen (cutout alleen alpha 0/255, textures vóór `runData`, emissive via `neoforge_data`).
- `fase-1-wereld.md` §1.4: stijlafspraken (palet per materiaal, productiemethode) als bron voor changes 37/57/76/95; Ring 1-sectie: definitieve paletten.
- `02-beslissingenlog.md`: één regel per beantwoorde vraag hieronder (alle vier beantwoord op 2026-09-30; bij de bouw vastleggen als dat nog niet is gebeurd).

---

## Client/server-impact

Alleen assets (PNG, `.mcmeta`, block-/itemmodels) die de client laadt; een dedicated server leest ze niet. Geen nieuwe Java-code in `src/main` behalve de datagen-provider (models). Alleen als change 10 een `BlockColor`-tint voor het grasblok registreerde, verdwijnt die uit `client/` (dan test S erbij). Het generatorscript staat buiten `src/` en komt niet in de jar. Geen netwerk, geen staat, geen invloed op worldgen of determinisme.

---

## Tests

- **B** — `./gradlew build` slaagt.
- **D** — `./gradlew runData` slaagt (models gewijzigd); output gecommit.
- **S** — alleen als er client-code verandert (een `BlockColor` uit change 10 weghalen).
- **runClient — visuele checklist:**
  - [ ] Log bevat geen `Missing textures in model` / `Unable to load model` voor `corruptedadventure`.
  - [ ] Creative tab: alle blokken en items tonen hun eigen texture (geen paars-zwart, geen placeholder meer).
  - [ ] Grasblok en verdorde aarde: 5×5-veld neerzetten → geen zichtbare naden of herhaling; zijkant sluit aan op de verdorde aarde eronder; geen groene tint in verschillende biomes (bijv. jungle en `withered_expanse`); sneeuwvariant indien aanwezig.
  - [ ] Log/wood/stripped: alle drie de assen (x/y/z), kopse kant klopt, `dead_wood` rondom schors; strippen met een bijl wisselt netjes.
  - [ ] Planks-familie: stairs (alle vormen), slab (boven/onder/dubbel), fence-verbindingen, fence gate open/dicht, button, pressure plate.
  - [ ] Deur: beide helften, open/dicht, links/rechts scharnier, van beide kanten; valluik boven/onder/open; gaten doorzichtig, geen zwarte pixels.
  - [ ] Zielkristal-erts in Nether-ring 1 (`/ca tpring 1 nether`): herkenbaar tussen netherrack en crimson; puls vloeiend; gloed zichtbaar in een donkere grot; geen z-fighting of flikkering van de overlay; check met mipmap-niveau 0 en 4 en met smooth lighting aan/uit.
  - [ ] `crimson_soul_crystal_ore`: naast gewone crimson nylium geen naad of kleurverschil in de basis; bovenkant, zijkant (rode rand intact) en onderkant kloppen; pulseert gelijk op met `soul_crystal_ore`.
  - [ ] Stijl naast vanilla: dood hout niet te verwarren met oak, spruce, dark oak, birch, mangrove, acacia; dood gras niet met gewoon gras of podzol/coarse dirt.
  - [ ] F3+T (resource reload) werkt zonder fouten.
  - [ ] Borden/boten (indien aanwezig): tekst leesbaar op het bord, hangend bord in de GUI, boot en chest boat.

---

## Risico's

- **Kwaliteit van gegenereerde textures** ("programmer art"): mitigatie via contactblad + iteratierondes met de gebruiker en de mogelijkheid tot handwerk (handEdited-lijst). Reken op 2–3 rondes.
- **Licentie**: bij optie B zitten afgeleide Mojang-pixels in de jar. Bij A alleen verwijzingen naar vanilla-textures (netherrack) in models, geen kopieën.
- **Namen/inhoud van change 10/15 wijken af** van de aannames (andere namen, wel/geen borden, boten, sneeuwstate, item-drop): lijst bij de start tegen `CODEBASE.md` leggen.
- **Emissive** wordt door de vanilla-renderer gerespecteerd, maar mogelijk niet (volledig) door Sodium/Embeddium of shaderpacks; dan valt het terug op een gewone, niet-gloeiende overlay. Geen crash-risico.
- **Cutout + mipmaps**: halftransparante pixels verdwijnen of geven randjes; mipmaps kunnen de overlay op afstand laten flikkeren. Alleen alpha 0/255 gebruiken, testen op mipmap-niveau 4.
- **`runData` faalt** als een model naar een nog niet bestaande texture verwijst: volgorde PNG's → `runData` aanhouden.
- **Handwerk overschreven** door het script opnieuw te draaien: `handEdited`-lijst verplicht zodra er met de hand is bewerkt.
- **Script uit change 10 wijkt af** van de opzet hier (naam, structuur): verfijnen binnen de bestaande opzet, niet herschrijven.
- **Crimson-erts naast vanilla-nylium:** omdat de basis naar vanilla-textures verwijst, past hij altijd; de overlay moet op het nylium-patroon passen, anders ogen de kristallen "geplakt".

---

## Open ontwerpvragen

### 1. Hoe maken we de definitieve textures?
- **A. Java-generatorscript met eigen pixels, gebruiker retoucheert waar nodig (aanbevolen).** Licentie-schoon, reproduceerbaar, herbruikbaar voor ring 2–5; kwaliteit "nette programmer art", verbetert met iteraties en optioneel handwerk. Weinig tijd voor de gebruiker.
- **B. Script dat vanilla-textures hertint/bewerkt.** Snelst en dicht bij de vanilla-look; afgeleide Mojang-assets in de jar (grijs gebied), en het lijkt sterk op bestaande houtsoorten.
- **C. Gebruiker tekent alles, Claude levert specificatie + models/mcmeta/test.** Beste en meest eigen resultaat; kost de gebruiker per ring enkele uren; bouwvolgorde wacht op het tekenwerk.
- **D. Script levert een basis, gebruiker tekent de blikvangers (zielkristal, deur, log-top) met de hand.** Middenweg: kwaliteit waar het opvalt, tijdsinvestering beperkt.

Antwoord (2026-09-30): **A** — Java-script met eigen pixels, de gebruiker retoucheert waar nodig. Change 10 zet een eerste, eenvoudige versie op; change 17 verfijnt die.

### 2. Welke stijl en kleur krijgen dood hout en dood gras?
- **A. Verbleekt grijsbruin hout (als drijfhout) + vaal geelbruin, stro-achtig gras op grijsbruine aarde (aanbevolen).** Leest direct als "dood", botst met geen vanilla-houtsoort in 1.21.1 en sluit visueel aan op de droge ring 2 (voorbode). Past bij de "verdorde vlakte".
- **B. Donker, bijna zwart hout (verkoold) + grijs, as-achtig gras.** Dreigender, maar lijkt op dark oak en neemt het vulkanische thema van ring 3 alvast weg.
- **C. Gebleekt wit-grijs hout (als botten) + bleek grijsgroen gras.** Spookachtig, maar dicht bij birch en mogelijk bij het corrupte thema van ring 5.

Antwoord (2026-09-30): **A** — verbleekt grijsbruin drijfhout + vaal geelbruin strogras op grijsbruine aarde.

### 3. Hoe ziet het zielkristal eruit en gloeit het?
Kleur in alle opties: zielblauw/cyaan (zoals soul fire), in contrast met rode netherrack/crimson — zeg het als je een andere kleur wilt.
- **A. Emissive overlay (gloeit in het donker) + langzame puls-animatie (aanbevolen).** Past bij een zeldzaam sleutelmateriaal en is goed vindbaar; kost een tweelaags-model met NeoForge-`neoforge_data` en een `.mcmeta`; geen code, geen server-impact. Mogelijk geen gloed met Sodium/shaders.
- **B. Emissive overlay, statisch.** Iets eenvoudiger, minder levendig.
- **C. Alleen puls-animatie, geen emissive.** Werkt overal hetzelfde, maar in donkere grotten niet opvallend.
- **D. Statisch, geen emissive.** Eenvoudigst; het erts valt nauwelijks op en voelt niet bijzonder.

Antwoord (2026-09-30): **A** — zielblauw/cyaan, emissive overlay + langzame puls; geldt voor `soul_crystal_ore` én `crimson_soul_crystal_ore`.

### 4. Krijgt dood gras een biome-tint?
- **A. Nee, vaste kleur in de texture (aanbevolen).** Overal even "dood", ook in jungle of savanne; geen client-code (geen `BlockColor`), alleen een eigen model.
- **B. Gedempte biome-tint** (grijze texture × ontkleurde biome-kleur via een `BlockColor` in `client/`). Past zich subtiel aan het biome aan; wel client-code en test S erbij, en in groene biomes kan het alsnog levend ogen.
- **C. Vanilla-grastint.** Geen extra werk als change 10 dat al zo doet, maar het gras wordt groen in groene biomes en is dan niet meer "dood".

Antwoord (2026-09-30): **A** — geen biome-tint, vaste kleur in de texture.
