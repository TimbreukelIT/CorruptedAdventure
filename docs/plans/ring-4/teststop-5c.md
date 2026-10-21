# Teststop 5c — eindcheck ring 4 + Nether-4 (checklist)

Blokkerende eindcheck van ring 4 en Nether-4 na golf 3 (`golf-3.md`; `waves.md` "Teststop 5c"). Stand: `main` `4bd8e444` (incl. ring 5 golf 0) na INT-G3 (I3, 66c–66g, 71, 71c, 74b, 75c, 77b, 77c + review-fixes). Lokaal door de
gebruiker (Windows); de cloud kon geen client draaien. Ring 5 begint pas na akkoord.

**Beoordeeld wordt:** standaard-W (elke ring en grens, ook Nether, portal per ring, overworld ring 4 → Nether-4), de
golf 3-punten in-game, de Sodium-wolkentest en een prestatiemeting. Determinisme en water zijn in de cloud gedaan (§F).
Elke sectie eindigt met **akkoord / opmerking**.

## Voorbereiding
- **Wereld:** **vers** (golf 3 verandert ertsen, riet, mangroves, lagune-oppervlak en tunnelwanden), seed **`20260930`**,
  `level-type=corruptedadventure\:corrupted_world`. Bijv. `level-name=ring4-5c` in `run/server.properties`
  (`online-mode=false`, RCON uit). Standaardradii 1.500 / 3.500 / 6.000 / 8.000 / 9.700 (Nether ÷4).
- **Server en client** (zoals teststop 5b §1; speel-lock `C:/Temp/ca-user-test.lock` zet main):
  ```powershell
  cd "P:\Projecten\Java\The Corrupted Adventure"
  .\gradlew.bat runServer --no-daemon
  .\gradlew.bat runClient --no-daemon          # tweede venster; Multiplayer → 127.0.0.1:<server-port>
  ```
  Server-console: `op Dev`; `stop` om te stoppen. `/gamemode creative` om te reizen.
- **Hulp:** `/ca ring`, `/ca tpring <1–5> [nether]`, `/ca tpboundary <1–4> [nether]`, `/ca weather 4 clear 6000`,
  `/time set noon|midnight`, `/locate biome corruptedadventure:<naam>`, F3 voor coördinaat en biome. Wijkt een plek af,
  ga uit van wat je ziet en noteer de F3-coördinaat.

## A. Standaard-W: ringen, grenzen, portals
- [ ] `/ca tpring 1` … `/ca tpring 5`: elke ring zijn eigen terrein; geen fouten, geen water tegen lucht (ring 5 is nog
      placeholder).
- [ ] `/ca tpboundary 1`, `2`, `3` (`steam_fens` → `verdant_falls`), `4` (`sundered_reach`): overgangen zoals in 5b.
- [ ] `/ca tpring 1 nether` … `tpring 4 nether`, `/ca tpboundary 1 nether` … `3 nether`: geen lavawand, de dam is heel.
- [ ] Portal per ring (overworld ↔ Nether, `coordinate_scale` 4): ring 1 (~500, 0), ring 2 (~2.500, 0), ring 3
      (~4.750, 0) en **ring 4 → Nether-4** (bijv. bij 7.000, 0 → Nether ~1.750, 0) en terug naar hetzelfde portal.
      Bij elke wissel de juiste meter (hitte ring 3, kou Nether-3, gif ring 4, zuiverend Nether-4).

## B. Golf 3 in-game
| Punt | Plek (verse wereld, seed 20260930) | Let op |
|---|---|---|
| Lagunezand + lagunezandsteen | `crystal_lagoon` gebied `lag` (x 6144..6239, z −128..−17), bijv. `/tp @s 6188 62 -68`; noord `/tp @s 0 66 7000` | wit-crème zand met aquamarijnkorrels op oevers, bodem en zandbanken; geen vanilla zand; onder overhangen/plafonds **lagunezandsteen** (bijv. rond 6159 60 −37), geen vanilla zandsteen; ligt de oude luchtplek 6188 59 −68 dicht? |
| Klauwkras in tunnels | gebied `tme` (x 6560..6815, z −352..−97; koepelhallen 6625 10 −337, 6588 −206) en `lagn` (x −128..127, z 6880..7135; bijv. `/tp @s -67 6 6980`, deepslate rond −65 5 6964) | klauwsporen volgen de wand: `clawed_stone` in steen, `clawed_deepslate` in deepslate (tidestone in tunnels komt niet voor); niet te druk |
| Tidestone op kliffen | `mist_fjords` `/tp @s 7000 80 32` | dunne lichte en dikke donkere band, geen herhalend motief (gespiegelde/gedraaide varianten) |
| Verwelkte maanbloesem | `withered_gardens` `/tp @s 7000 170 64` | stronken rot en donkerbruin, niet meer "grijze steen" |
| `blighted_glowcap` | bedorven band, `/tp @s 64 66 7128` (overdag) | ziekelijk olijf/grijs, duidelijk anders dan de blauwe `glowcap` |
| Gifpoel | `/tp @s 71 67 7124` | troebel olijf met schuimvlokjes naast het gifgroene water |
| Fossielsteen + botblokken | Nether `/execute in minecraft:the_nether run tp @s 1699 60 -429` | fossielsteen-muren, botblokken steken af tegen bottenzand |
| Silhouet | diep water `crystal_lagoon` `/tp @s 0 50 7000` (zeldzaam) | vorm leest bij bijna zwart |
| Lagunezandsteen als bouwblok | creatief: plaats de set (vooral `chiseled_lagoon_sandstone`), smelt `lagoon_sand` → glas | lotus leest als bloem; korrels herhalen niet storend in een grote muur; recepten zoals vanilla zandsteen |
| Eilanden: brokken | `islands.txt`: `inner` (x 6128..6383, z −128..127), `lotus` (x 6880..7135, z −128..127), `skyw1` (x −6960..−6865, z 144..239) | geen boom, kei, stam of plant op een zwevend brokje; bovenop een eilandtop boven een brok wel |
| Eilanden: bogen | dezelfde gebieden, rotsboog `/tp @s 7345 125 187` | geen kruisende bogen (X-knoop) |
| Riet onder overhangen | `cascade_vale` `/tp @s -7041 70 -60`, `lotus_shallows` `/tp @s 6932 66 -3` | riet langs open oevers naast water dat onder een eilandoverhang ligt |
| Reuzenmangrove | `glowroot_swamp` west rond `-6975 70 284` | staat er (was per laadvolgorde wel/niet) |

## C. Sodium-wolkentest (backlog `ring4/atmo-sky`)
- [ ] Met Sodium (los, zonder Iris): ring 4-wolkenlaag op Y 300 en de wolkenfog; valt de laag vanaf zeeniveau weg?
      Zonder Sodium ter vergelijking.

## D. Prestatie (lokaal, niet spelen tijdens de meting)
```bash
bash tools/test/perf.sh --areas tools/test/areas/perf-islands.txt
bash tools/test/perf.sh --areas tools/test/areas/perf-r4.txt
bash tools/test/jfr-prof.sh --areas tools/test/areas/perf-islands.txt     # hete methodes
```
- [ ] Noise ≤ 1,75× vanilla (mediaan warm). Eerste bezoek/teleport naar een eilandgebied: hapering?
- [ ] JFR: aandeel van `IslandArches.archKept` / `IslandLandmarks.candidatePairs` (kruisingscheck r. 429 vraagt paren
      tot ~150 blokken om een boog). Alleen bij een duidelijke piek een vervolgpakket.

## E. Optioneel: det/waterscan lokaal
Niet nodig: gedaan in de cloud (§F). Wil je toch: `bash tools/test/det.sh --areas tools/test/areas/islands.txt` en
`bash tools/test/waterscan.sh ring4-5c-ws tools/test/areas/int2.txt`.

## F. Cloud-resultaten (INT-G3, ter info)
Op `c6f917f` (golf 3 compleet), Linux-container, seed 20260930:
- `build` groen (1.118 tests, 0 fouten), `runData` zonder verschil in `src/generated`.
- **Determinisme** (`det.sh`, wereld b omgekeerd geladen): `islands.txt` 1.051/1.060 chunks identiek, own 9 (alleen
  ertswissels over chunkgrenzen, bekende soort), other 0; `perf-islands.txt` 1.268/1.280, own 11 (ertswissels), other 1
  (−7008 61 380 zeegras ↔ mangrovewortels op lagunezand, chunkrand; backlog). Geen verschil meer in mos, strengen of
  aankleding onder de eilanden. Vóór de review-fixes (`d77a385`): 1.050/1.060 own 8, 1.269/1.280 own 11 other 1.
- **Server-smoke** (verse wereld, ring 4, grenzen 3|4 en 4|5, lagune, Nether-4 en grens 3|4 Nether): 0 ERROR behalve de
  onschuldige Yggdrasil-`UnknownHostException` (geen internet), geen client-class.
- **Lagune** (`lag` en een deel van `lagn` in de smoke-wereld): 0 vanilla zand en zandsteen; `lagoon_sandstone` 504
  resp. 177 blokken onder plafonds; 6188 59 −68 dicht (zand op 58–59, geen lucht). ~100–160 lagunezandblokken per gebied
  liggen boven grot-/zeewater en vallen bij een blokupdate (zoals vanilla zand; backlog).
- **Klauwkras** in de tunnels: `clawed_stone` en `clawed_deepslate` (`lag` 5 + 11, deel `lagn` 18 + 4);
  `clawed_tidestone` komt in tunnels niet voor (tunnelwanden zijn steen/deepslate; verwacht).

## Uitkomst (2026-10-21)
**Akkoord** voor ring 4 zoals hij nu op `main` staat (`4bd8e444`), in het geheel. De gebruiker heeft de secties A–D niet
afzonderlijk gemeld; het akkoord geldt voor de hele ring 4 en Nether-4. Eventuele losse opmerkingen zijn niet
vastgelegd en gaan naar de backlog als ze terugkomen.

## Terugmelden
Per sectie **akkoord / opmerking**, per bevinding kort:

```
Plek: B <punt>            F3: x y z
Klopt:      …
Mis/lelijk: …
Screenshot: ja/nee
```

Plus: de cijfers van §D (`build/test/perf-<stamp>/table.md`, JFR-aandeel `archKept`), de uitkomst van de Sodium-test,
en per golf 3-punt "zo laten" of "anders: …". Antwoorden komen in dit bestand (§Uitslag) en waar nodig in de
beslissingenlog.
