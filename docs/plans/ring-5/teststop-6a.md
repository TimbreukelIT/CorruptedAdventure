# Teststop 6a — terrein-wow ring 5 + Nether-5 (checklist)

Blokkerende check na golf 1 van ring 5 (`golf-1.md` §5 "Teststop 6a"; `waves.md` §2). Stand: `main` na INT1 (alle zeven
stromen van golf 1: 88/88r/89/90, 99a, 84/85/83, 86, 87, 91, 92 + de INT1-fixes, 2026-10-22/23). **Vóór de test moeten
86b, 87b en 86c op main staan** (echte ring 5-/Nether-5-blokken in de surface, gespleten rots op de breukzone); tot dan
zijn de oppervlakken vanilla-placeholders (tuff, coarse dirt, calcite, smooth quartz) en staat er geen gespleten rots.
Lokaal door de gebruiker (Windows, client). Golf 2 begint pas na akkoord.

**Beoordeeld wordt (blokkerend):** de vorm van ring 5 en Nether-5 (A–C) en de prestatie (G). **Meedraaien, oordeel in
6b:** blokken en textures (D), bordjes (E), de void-meter (F1) en het ringweer (F2): kijken of ze werken en niets
breekt; hoe ze voelen beslist 6b. Nog niet in golf 1: grotten, aankleding, horror-features, sfeer (93–98).
Elke sectie eindigt met **akkoord / opmerking**.

## Voorbereiding
- **Wereld:** **vers**, seed **`20260930`**, `level-type=corruptedadventure\:corrupted_world`, bijv.
  `level-name=ring5-6a` in `run/server.properties` (`online-mode=false`, RCON uit). Standaardradii 1.500 / 3.500 /
  6.000 / 8.000 / 9.700, void-rand 10.200 (Nether ÷ 4: ring 5 = 2.000–2.425, border ~2.550).
- **Server en client** (zoals teststop 5c; speel-lock `C:/Temp/ca-user-test.lock` zet main):
  ```powershell
  cd "P:\Projecten\Java\The Corrupted Adventure"
  .\gradlew.bat runServer --no-daemon
  .\gradlew.bat runClient --no-daemon          # tweede venster; Multiplayer → 127.0.0.1:<server-port>
  ```
  Server-console: `op Dev`; `stop` om te stoppen. `/gamemode creative` om te reizen (spectator om door de grond te
  kijken).
- **Hulp:** `/ca ring` (ring, band, overgang), `/ca tpring 5 [nether]`, `/ca tpboundary 4 [nether]` (zone 4→5),
  `/ca tpboundary 5` (zone 5→void), `/ca castles` en `/ca castles tp <1–4>`, `/locate biome corruptedadventure:<naam>`,
  `/time set noon|midnight`, F3 voor coördinaat, biome en de regels `CA ring (client)` / `CA weather (client)`. Wijkt een
  plek af, ga uit van wat je ziet en noteer de F3-coördinaat.
- **Plekken op seed 20260930** (`tools/test/areas/castles.txt`, `ring5.txt`, `perf-r5.txt`; blokcoördinaten, de Y kies je
  zelf of via `/ca tpring`):

  | Plek | x, z | Bron |
  |---|---|---|
  | zone 4→5 oost (breukzone) | 8000, 0 | `perf-r5.txt` `zone45`, `ring5.txt` `z45e` |
  | binnenband `corrupted_wastes` | 8400, 0 | `wastes` |
  | binnenband `glitch_fields` | 0, −8400 | `glitch` |
  | binnenband `false_meadow` | 8464, −832 | `meadow` |
  | bandlijn oost (overgang ~200) | 8500 → 9200, 0 | `ring5.txt` `r5le` (lijn op ~8.850 ± 255) |
  | buitenband `void_scars` | −512, −9056 | `scars` |
  | buitenband `inverted_hills` | 0, −8800 | `inverted` |
  | buitenband `shattered_expanse` | 8992, 96 | `shattered` |
  | kasteel #1 (327,5°, r 49, top 127, gordel 75) | 7794, −4962 | `/ca castles tp 1` |
  | kasteel #2 (60,1°, r 50, top 124, gordel 62) | 4653, 8078 | `/ca castles tp 2` |
  | kasteel #3 (154,3°, r 51, top 129, gordel 58) | −8342, 4012 | `/ca castles tp 3` |
  | kasteel #4 (245,4°, r 47, top 139, gordel 77) | −3871, −8440 | `/ca castles tp 4` |
  | zone 5→void / buitenrand oost | 9540 → 10200, 0 | `ring5.txt` `z5ve`, `perf-r5.txt` `voidedge` (9696, 0) |
  | Nether `white_halls` | 2192, 0 (Nether) | `perf-r5.txt` `halls` |
  | Nether `hollow_cathedral` | 2248, 212 (r 90) (Nether) | `golf-1-nether.md` (echte chunks gecontroleerd); ook (2189, −240) r 72, (2197, 588) r 79, (148, 2286) r 62, en via `/locate biome` rond (−192, −2208) |

  Controleer de kasteelplekken met `/ca castles` (ze komen uit `Ring5Field`, onveranderd sinds golf 0).

## A. Kaarten vooraf (main legt ze voor, vóór de game)
`/ca terrainmap` via RCON op de 6a-wereld, PNG's in `<wereld>/corruptedadventure/maps/`:
- [ ] Overzicht oost `terrainmap 8900 0 1400 4` (hoogte + biome): zone 4→5, binnenband met drie biomes, bandlijn, buitenband
      met drie biomes, kasteel in deze kwart, buitenrand tot de border.
- [ ] De vier kasteelplekken `terrainmap <x> <z> 300 1` (coördinaten hierboven).
- [ ] Doorsneden: radiaal oost `terrainmap section 7700 0 10300 0`, dwars door zone 4→5 `section 7800 -600 8200 600`,
      binnenband `section 8000 0 9000 0`, en radiaal door één pinakel.
- [ ] Nether: `/execute in minecraft:the_nether run ca terrainmap 2200 0 300 1` (hallen, zuilenraster, vlaktes,
      kathedralen) en `section 1900 212 2500 212` (door de kathedraal bij 2248, 212 en zone 4→5).
- Let op: de bovenkaart toont dunne dekken (bruggen, ruggen, springstenen) gestippeld (grove stap `ColumnScan`, backlog);
  de doorsnede toont ze heel.

## B. Ring 5 overworld in-game (blokkerend)
| Punt | Plek | Let op |
|---|---|---|
| Breukzone 4→5 | `/ca tpboundary 4`, of 8000, 0 | grotendeels dichte grond (~75 %), droge spleten 2–8 breed en 30–60 diep met puinbodem, opgeheven/gekantelde schotsen, losse brokken buiten; **overal te voet over te steken** (om de 24–34 blokken een dichte oversteek), **geen landbruggen, geen afgrond, geen water**; na 86c: de gespleten rots op een vlakke opgeheven schots (~elke 650 langs de zone) |
| Dode binnenband | 8400, 0; 0, −8400; 8464, −832 | dode grond Y 70–95 (rond 78); `corrupted_wastes` te regelmatige trappen (treden exact 4 hoog; mogen tot ~Y 119, besluit 2026-10-22) en perfect ronde ondiepe kuilen; `false_meadow` te gladde, symmetrische golf (Y 73–79); `glitch_fields` blokkerig (blokken van 4), verschoven schijven 4–6 hoog, zwevende kubussen 3–8 |
| Losse scheuren binnen | binnenband | smal (2–6), 20–120 lang, tot in de void zonder bedrock; ~1 per 256 × 256 |
| Overgangsband | 8500 → 9200, 0 | bandlijn op ~8.850 met tongen (±250), overgang ~200; binnen vast, buiten begint het uiteenvallen; biomes wisselen hard op de lijn |
| Uiteenvallen naar buiten | buitenband, 8992, 96; −512, −9056; 0, −8800 | schotsen (toppen Y ~70–110, onderkant van ~Y −40 bij de lijn naar Y 20–40 bij de rand, rafelig), steeds meer void (~85 % aan de ringrand), brokken Y 40–160; `void_scars` scheuren en kraters tot in de void; `inverted_hills` omgekeerde heuvels onder schotsen (vorm; bomen ondersteboven zijn 95) |
| Kasteelpinakels + bruggen | `/ca castles tp 1` … `4` | plateau (Ø 80–110, top 124–139) met verwoeste rand, rotswand tot ~Y −40, open void-gordel 40–80; per kasteel 2 natuurlijke bruggen (5–9 breed, licht doorhangend, brokkelige randen): één naar binnen naar vaste grond, één schuin naar een grote schots; van ver zichtbaar als baken |
| Ruggen en springstenen | buitenband tussen schotsen | smalle ruggen (3–5 breed) om de ~500 langs de ring, stroken springbare brokken (gaten ≤ 3, hoogteverschil ≤ 1); geen garantie dat alles bereikbaar is (V6-A) |
| Buitenrand tot de border | `/ca tpboundary 5`, 9540 → 10200, 0 | hetzelfde veld loopt door, steeds kleinere brokken, leeg vanaf ~10.050, border op 10.200; geen oude zwevende eilanden meer |
| Bodem en vloeistof | overal in ring 5 | geen water; lava alleen diep onder −54 in de binnenband (niet bij scheuren, niet in de buitenband); geen losse 1–2-blokgroepjes boven de void; geen bedrock boven de void |
| Biomes en surface | F3 per plek | binnen 3 / buiten 3 biomes op de verwachte plek (P1: glitch ~33 %, meadow ~13 %, wastes ~54 %; buiten `shattered_expanse` bij veel void, rest half/half); pinakel = `void_scars`; na 86b de echte blokken (corrupted gras/aarde/steen/zand), `false_meadow` blijft vanilla gras |
| Ringbreedte | `/ca tpring 5`, `/ca tpboundary 4` | ring 5 voelt breed genoeg (8.000–9.700) en de twee banden ongeveer 50/50 |

## C. Nether-5 (eerste blik, blokkerend voor de vorm)
- [ ] `/ca tpring 5 nether` en `/execute in minecraft:the_nether run tp @s 2192 40 0`: `white_halls` — vlakke vloer
      precies op Y 31 (na 87b spiegelsteen), plafond ~Y 104, vierkante zuilen 4 × 4 om de 24 in strakke rasters (per cel
      van 400 een eigen offset), lage witte vlaktes 2–6 hoog op ~30 % van de vloer; genoeg loopruimte.
- [ ] **Vlaktes:** de randen zijn 2–6 hoog, zonder blokken kom je er niet op (99,3 % van de vloer is wel met stap ±1
      begaanbaar). Zo laten, een trede, of lagere randen? (backlog)
- [ ] `hollow_cathedral` bij 2248, 212 (r 90): ronde lege hal Ø 120–180, dak opgetild tot Y 120 over een rand van 8–16,
      één reuzenzuil Ø 20–30 van vloer tot dak; leeg en stil.
- [ ] **Aantal kathedralen:** 27–32 per seed (unit test, 5 seeds; kans 0,9 per cel van 400 i.p.v. ~0,8): te veel, te
      weinig, goed?
- [ ] `/ca tpboundary 4 nether`: aansluiting op de bottenduinen van Nether-4 (vloer ~Y 43), geen lavawand op de trede 4|5;
      zone 5→void sluit aan op de Nether-void.
- [ ] Geen lava in Nether-5; portal vanuit ring 5 (bijv. bij 8.400, 0 → Nether ~2.100, 0) en terug naar hetzelfde portal.

## D. Blokken en textures (meedraaien; oordeel 6b, textures 99b/99)
Creatief, tab "The Corrupted Adventure" (ring 5 na Nether-4, dan Nether-5):
- [ ] De 9 corrupted varianten (gras, aarde, steen, keien, zand, stam, hout, planken, bladeren): grijs uitgeloogd met dunne
      violet-zwarte aders; gras zonder tint en zonder verspreiding; zand valt; steen → keien.
- [ ] Corrupted houtset: gestript, trap, plaat, hek, poort, deur, luik, knop, drukplaat; recepten zoals eik (13).
- [ ] `glitch_block`: kapot missing-texture-schaakbord, geanimeerd; breken geeft 1–3 glitchscherven.
- [ ] `voidstone`: zwart met twinkelende sterren (emissive, zie 's nachts), af en toe een vanilla reverse-portal-deeltje uit
      een open zijde.
- [ ] `nullite_ore` / `voidstone_nullite_ore`: violet-zwarte kristallen met violette rand, zwak emissive (net zichtbaar in
      het donker), geen bloklicht; diamant-houweel nodig.
- [ ] Nether-5: kalkrack (krijtwit, anders dan het beige gebleekte netherrack), witsteen-set, witsteenzuil, spiegelsteen
      (glanzend, normale wrijving), stil licht (licht 15, geen geluid/deeltjes), witte-kernerts (licht 3, zachte gloed).
- [ ] Items: glitchscherf, ruw nulliet, witte kern, deur/bord/hangbord; contactblad `build/texture-preview/ring5.png`
      (`java tools/textures/Ring5Textures.java`) ter vergelijking.

## E. Bordjes (client, nog nooit in-game getest)
De route (vanilla block-entity-types via `BlockEntityTypeAddBlocksEvent`, `Sheets.addWoodType`) is alleen headless
gecontroleerd.
- [ ] Staand bord en bord aan de muur plaatsen, tekst schrijven en bewerken; rendering met de eigen texture
      (`entity/signs/corrupted`).
- [ ] Hangbord aan het plafond en aan de muur; het bewerkscherm toont `gui/hanging_signs/corrupted`.
- [ ] Breken: alle vier geven het juiste item terug (wandbord → bord, hangend wandbord → hangbord).
- [ ] Geen fout in de client-log bij het laden van de wereld (sign-materialen).

## F1. Void-meter (meedraaien; oordeel 6b)
Overleven (`/gamemode survival`), difficulty normal. De meter staat als violette balk boven de hongerbalk.
- [ ] `/ca condition get`: meter, rate en label (`inner`, `outer`, `source`, `light`, `outside`); in de binnenband ~6 min
      naar 100, in de buitenband ~3 min, 's nachts en bij ringweer ×1,5–2 (de grootste telt).
- [ ] Bloklicht ≥ 12 op ooghoogte (fakkels rondom) laat de meter zakken, ook in de buitenband; daglicht telt niet.
- [ ] Naast `glitch_block`/`voidstone` (binnen 4) stijgt hij sneller (`source`).
- [ ] `/ca condition set @s void 80` en dan kijken: **binnenband** elke ~20 s een korte teleport van 1–5 blokken, alleen op
      vaste grond, nooit in een blok, in vloeistof of naar een rand/de void (mag door muren, besluit 2026-10-21);
      **buitenband** darkness in plaats van teleports.
- [ ] `/ca condition set @s void 95`: darkness + void-schade (1 per 2 s; peaceful niets, easy tot 10 HP, normal/hard
      dodelijk); doodsbericht "was unmade by the void".
- [ ] Buiten ring 5 zakt de meter; snel in Nether-4.

## F2. Ringweer ring 5 incl. stilstand (meedraaien; oordeel 6b; client-rendering nog nooit in-game getest)
- [ ] `/ca weather` toont voor ring 5 `… (inner band) | outer band: …`; F3 `CA weather` ` [ring_5 inner …, outer …]`.
- [ ] `/ca weather 5 glitch_storm 120`: beeldruis-overlay en scanlines, af en toe een hotbar-hapering (1–2 ticks, nooit
      vastlopend), korte geluidsdips, violet-zwarte flitsen in de fog; F1 verbergt de overlay.
- [ ] `/ca weather 5 reverse_rain 120`: zwart-violette druppels die van de grond omhoog gaan, stoppen onder een dak,
      ook in biomes zonder neerslag.
- [ ] `/ca weather 5 standstill 60`: regenstrepen staan stil, wolken remmen af en staan stil (en drijven daarna verder
      zonder sprong), alle wereldgeluid en muziek gedempt tot ~15 % (UI niet), witte vlakke fog.
- [ ] Met de cyclus (zonder vast type) kan binnen ander weer zijn dan buiten; over de bandlijn vloeit het beeld over
      (~200 blokken). Optioneel: twee clients in verschillende banden.
- [ ] Open punt 6b (niet nu beslissen): trekt de binnenband stilstand, dan krijgt de buitenband in dat korte venster een
      korte zwakke bui van zijn eigen type (backlog).
- [ ] Optioneel: wolken met Sodium (de wolkenmixin past alleen de drift-local aan).

## G. Prestatie (INT1, ter info; lokaal opnieuw alleen als je wilt)
INT1 lokaal (`perf.sh --areas tools/test/areas/perf-r5.txt`, 3 warm + 1 koud, `build/test/int1-r5/table.md`), noise
t.o.v. vanilla, mediaan warm: `wastes` 0,52, `glitch` 0,52, `meadow` 0,53, `scars` 0,42, `inverted` 0,46,
`shattered` 0,39, **`zone45` 1,49** (1,30–1,57), `castle1` 0,36, `voidedge` 0,50, Nether `halls` 1,00, `cathedral` 0,86
(koud 1,17). Budget ≤ 1,75×: gehaald. Surface het hoogst in zone 4→5 (1,69).
- [ ] Eerste bezoek / teleport naar de breukzone en een kasteel: hapering?
- Opnieuw meten (niet spelen tijdens de meting): `bash tools/test/perf.sh --areas tools/test/areas/perf-r5.txt`.

## H. `/ca`-commando's
- [ ] `/ca ring` (ring 5, band, overgang), `/ca castles` (vier regels, coördinaten zoals hierboven) en `/ca castles tp <n>`.
- [ ] `/ca tpring 5`, `/ca tpring 5 nether`, `/ca tpboundary 4`, `/ca tpboundary 4 nether`, `/ca tpboundary 5`.
- [ ] `/ca weather`, `/ca weather 5 <glitch_storm|reverse_rain|standstill|clear> [seconden]` (een ring 4-type in ring 5 geeft
      een foutmelding).
- [ ] `/ca condition get`, `/ca condition set @s void <0–100>`, `/ca condition debug @s rise|fall|off`. Let op:
      `/ca condition` vraagt altijd een subcommando; via RCON (geen speler) heeft een kale `get` geen doel.
- [ ] `/ca terrainmap` (zie A).

## I. INT1-resultaten (ter info)
Lokaal, seed 20260930: `build` en `runData` groen, `src/generated` schoon; S-smoke (verse wereld) zonder ERROR en zonder
client-class-fout; `det.sh` op `ring5.txt` (omgekeerd geladen) 991/991 chunks identiek; `voidscan.py --strict` (met
`--rings`) 0 fouten; `waterscan.sh` op `ring4.txt` 0 (zone 4→5 zonder water); feature-index ring 1–4 (`fi3.txt`)
gelijk op één bekende Nether-cel na (backlog); prestatie zie G; `ModNetwork.PROTOCOL_VERSION` `"9"` (client en server
moeten dezelfde build draaien).

## Bekende open punten
- **Review B (rest):** commentaar en tests rond de wandbordjes aanscherpen (backlog `[blocks]`). Regel voor later
  (niet zichtbaar in 6a): plaatst een latere golf `corrupted_leaves` buiten een boom, dan `persistent=true` of binnen 6
  van een `corrupted_log`, anders vergaan ze (backlog `[worldgen]`).
- **Nether-5:** aantal kathedralen 27–32 per seed (C) en de vlakteranden 2–6 hoog (C): oordeel gebruiker.
- **Client nog nooit in-game getest:** de client-mixins van 92 (`LevelRendererCloudsMixin`-uitbreiding,
  `SoundEngineVolumeMixin`, alleen met `javap` gecontroleerd), de GUI-lagen van de glitchstorm en de bordjes-route (E).
  Een crash of fout in de client-log hierbij is een blokkerende bevinding voor die stroom, niet voor de terreinvorm.
- **Nog niet in golf 1:** 86b/87b/86c (zie boven), grotten (93), aankleding en horror-features (94–96), sfeer en
  stilte-meter (97, 98); teleport zonder geluid/deeltjes (98).

## Uitkomst
*(in te vullen na de test)*

## Terugmelden
Per sectie **akkoord / opmerking**, per bevinding kort:

```
Plek: B <punt>            F3: x y z
Klopt:      …
Mis/lelijk: …
Screenshot: ja/nee
```

Bijsturing van de terreinvorm gaat als getallen terug naar W-terrein/W-nether (vóór golf 2). Antwoorden komen in dit
bestand (§Uitkomst) en waar nodig in de beslissingenlog.
