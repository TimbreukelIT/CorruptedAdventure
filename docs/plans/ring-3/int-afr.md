# Integratie golf 2-afronding (INT-afr) — ring 3

Scope: `infra` (integratie, snel protocol van main 2026-10-09). Branch `ring3/int-afr` op main `c6cd90d`, worktree `.claude/worktrees/int2`. Mergevolgorde: B → W-biomes → A nu, W-terrein (42b, 54b) als laatste na het signaal van main.

## Voortgang
Voortgang: 2026-10-09 start (regels afronding gelezen)
Voortgang: 2026-10-09 20:23 B (`943844e`), W-biomes (`a5d8358`), A (`2442dd0`) gemerged zonder conflicten; surface-scripts idempotent; B `--rerun-tasks` groen (693 tests), D groen, `git diff --exit-code src/generated` leeg
Voortgang: 2026-10-09 20:41 docs-delta's B/C, W-biomes, A samengevoegd (`a76853d`); S-smoke (server 25579 + Dev, wereld `afr-smoke`): beide mixin-configs laden, geen client-class-fout, join, `tpring 3`, `tpring 3 nether`, `tpboundary 3` (`steam_fens`), portal ring 3 ↔ Nether-3; log zonder ERROR/far chunk (alleen "moved too quickly"); shots in `build/afr-shots/` (overwoekerde krater, zwavelpoelen met kalkranden en gekleurde modder, zwavelkristallen, rode lucht + pluim bij `eruption`). Determinisme, feature-index en lavascan na de W-terrein-merge
Voortgang: 2026-10-09 21:20 W-terrein gemerged (`c637968`, geen conflicten), surface-scripts idempotent, B `--rerun-tasks` 700 tests groen, D groen, `git diff --exit-code src/generated` leeg; docs W-terrein (`d4c60ac`)

## Resultaten
- **Lavascan** (`afr-det-a`, 26 gebieden, `ow_lava.py` + `lava_why.py`, indeling: riviertrede = de buurkolom heeft lava op Y−1; eilandval = binnen 3 blokken van een getraceerde val uit de pure vorm): riviertreden B 88, C 33, D 49, E 31, caldera 21, basalt 20, geode 35, archipel/arch 19, pit 8, pieken 3, A 4, lavarivieren 1 contacten (in lijn met de offline telling van 42b); eilandvallen A 309, archipel 827 contacten; **overig in ring 3: één plek** in de afgrond (pit): (4.409–4.412, Y 5–9, −510…−508), 24 contacten in 4 kolommen: de rand van de lavazee (Y 9) tegen een smalle luchtschacht tot Y 5. Ring 1 (vanilla) niet meegeteld. Vóór 42b: B 146, D 132, basalt 143, geode 73, caldera 66, C 69, pit 117 wandblokken.
- **Determinisme** (`afr-det-a` vooruit, `afr-det-b` omgekeerd, Nether eerst): Nether-3 1.280/1.280 identiek; ring 3 8.111/8.614, verschillen alleen vloeistof (water/lava/lucht, obsidiaan en 1 aslaag onder vloeiende lava bij de archipelvallen) en erts-klodders over chunkgrenzen (vanilla en mod, zoals 41). De 9 mod-blokken van INT2 (aslaag op erts, vallende askorst) zijn weg.
- **Feature-index** ring 1/2, zones, Nether-1/2 tegen INT2 (`int2-det-a`, code = main `c6cd90d`): biomes 100 % identiek; terrein ring 1 en Nether-1/2 0 verschil, ring 2/zones alleen op decoratieplekken (ring 2-zand-/kloofsteen-features verschuiven door de nieuwe features).
- **S-smoke:** zie voortgang 20:41 (vóór W-terrein); W-terrein raakt alleen worldgen. Serverlogs van `afr-det-a`/`-b` (met W-terrein): geen ERROR, geen far chunk.
- **Prestatie:** zie hieronder (na de merge, go van main).
Voortgang: 2026-10-09 21:25 review INT-afr (main) verwerkt; nachecks:
- **IJs bij de warme scheuren (54b):** 6 losse blootliggende ijsblokken binnen 3 blokken van de 107 open lavakolommen in de kloven, vóór en na 54b dezelfde (de "29" van INT2 telde paren); zee/zuid/noord 0 → backlogregel blijft open.
- **Wanden van diepe sneden (42b):** blootliggend vanilla-gesteente naast open lucht (tuf niet meegeteld): pit 4.053 → 146, B 646 → 87, archipel 30 → 9, C/D/E 0–1 → afgehandeld met een klein residu.
- **`glowing_fissure`/`smoldering_stone` rotaties (46b):** in-game gezien (`build/afr-shots/c_glowing_fissure_rot*.png`, `c_smoldering_stone_rot_dev.png`).
- **S-smoke na W-terrein** (verse wereld `afr-smoke2`, server + Dev): `tpring 1|2|3`, `tpring 1|2|3 nether`, `tpboundary 2|3`; portal Nether-3 (891, 36, 787) → ring 3 (3.566, 38, 3.146) en ring 3 (4.758, 99, 17) → Nether-3; log zonder ERROR/far chunk.
- **Lek (4.409–4.412, Y 5–9, −510…−508):** besluit main A: backlog, fix in golf 3.

## Prestatie (na de merge, main `5abd7f7`, 2026-10-09 21:25–22:12)
JFR `minecraft.ChunkGeneration`, mod tegen vanilla-datapackdimensies in één server, 16 × 16 chunks per gebied, warm-up over alle ringtypes, verse wereld per ronde, volgorde per gebied wisselend; 3 warme rondes (mediaan) + 1 koude ronde (verse JVM, geen warm-up; ring 2, horizonreus, caldera, titanenveld, smeulwoud, lavabuis, afgrond). Nieuw gebied: de afgrond bij (4.531, −413). INT2 = de 4 warme rondes van integratie 2 (zelfde gebieden en methode). Mod/vanilla:

| Gebied | noise (3 warm) | min–max | surface | features | noise koud | surface koud | noise INT2 | surface INT2 |
|---|---|---|---|---|---|---|---|---|
| ring 1 | 0,89 | 0,87–1,03 | 1,44 | 0,89 | – | – | 0,95 | 1,48 |
| ring 2 | 1,17 | 0,98–1,17 | 2,80 | 0,82 | 1,04 | 2,86 | 1,05 | 2,85 |
| horizonreus | 1,22 | 1,18–1,25 | 2,59 | 0,42 | 1,21 | 2,65 | 1,19 | 2,59 |
| caldera | 1,11 | 1,08–1,13 | 2,21 | 0,47 | 1,11 | 2,21 | 1,10 | 2,24 |
| lavarivieren | 1,02 | 1,01–1,02 | 2,18 | 1,31 | – | – | 1,02 | 2,18 |
| asvlakte | 1,03 | 1,00–1,03 | 2,09 | 1,11 | – | – | 1,00 | 2,04 |
| zeekom | 1,39 | 1,14–1,41 | 1,73 | 0,89 | – | – | 1,36 | 1,77 |
| titanenveld | 1,10 | 1,09–1,15 | 1,90 | 0,96 | 1,27 | 1,92 | 1,19 | 1,93 |
| pieken | 1,05 | 1,03–1,09 | 2,21 | 1,00 | – | – | 1,06 | 2,28 |
| basaltstromen | 1,06 | 0,98–1,07 | 2,14 | 0,34 | – | – | 1,10 | 2,11 |
| smeulwoud | 1,26 | 1,14–1,35 | 1,95 | 0,89 | 1,15 | 1,98 | 1,22 | 1,94 |
| zwavelvelden | 1,21 | 1,10–1,23 | 2,39 | 0,38 | – | – | 1,19 | 2,45 |
| slapende krater | 1,08 | 1,05–1,13 | 1,96 | 0,90 | – | – | 1,05 | 1,92 |
| steam_fens | 1,12 | 1,05–1,14 | 1,67 | 0,34 | – | – | 1,06 | 1,68 |
| lavabuis (12, −2) | 1,08 | 1,07–1,09 | 1,99 | 0,26 | 1,29 | 2,02 | 1,08 | 1,96 |
| magmakamer (−12, −1) | 1,09 | 1,08–1,11 | 2,08 | 0,97 | – | – | 1,12 | 2,09 |
| afgrond (4.531, −413) | 1,24 | 1,12–1,60 | 2,21 | 0,72 | 1,06 | 2,22 | – | – |
| Nether-1 | 1,15 | 1,09–1,20 | 1,07 | 0,55 | – | – | 1,12 | 1,06 |
| Nether-2 | 1,24 | 1,18–1,34 | 0,76 | 0,09 | – | – | 1,33 | 0,76 |
| Nether-3 rijpwoud | 1,33 | 1,22–1,35 | 1,42 | 0,48 | – | – | 1,36 | 1,41 |
| Nether-3 zee | 1,15 | 1,00–1,41 | 1,05 | 0,17 | – | – | 1,17 | 1,06 |
| Nether-3 zuid | 1,23 | 1,21–1,29 | 1,17 | 0,42 | – | – | 1,32 | 1,16 |
| Nether-3 noord | 1,06 | 1,06–1,08 | 1,03 | 0,23 | – | – | 1,15 | 1,01 |
| Nether-3 kloven | 1,23 | 1,10–1,41 | 0,95 | 0,26 | – | – | 1,35 | 0,95 |

- **Noise:** alle gebieden ≤ 1,39 (budget 1,75); hoogste losse ronde afgrond 1,60 (ronde 1), daarna 1,12/1,24. Koud ≤ 1,29. 42b (randen via de exacte interpolatie, carvers uit de lavaband) is in de noise-stap niet meetbaar: de verschillen met INT2 liggen binnen de spreiding tussen rondes.
- **Surface:** ring 3 1,7–2,6×, gelijk aan INT2 (de `deep_wall`-verf in de snelle weg kost niets meetbaars); ring 2 2,8× (niet geraakt).
- **Features:** ring 3 0,26–1,31× vanilla.
