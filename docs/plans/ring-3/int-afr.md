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
- **Prestatie:** nog niet (na de go van main; 42b raakt noise en surface).
Voortgang: 2026-10-09 21:25 review INT-afr (main) verwerkt; nachecks:
- **IJs bij de warme scheuren (54b):** 6 losse blootliggende ijsblokken binnen 3 blokken van de 107 open lavakolommen in de kloven, vóór en na 54b dezelfde (de "29" van INT2 telde paren); zee/zuid/noord 0 → backlogregel blijft open.
- **Wanden van diepe sneden (42b):** blootliggend vanilla-gesteente naast open lucht (tuf niet meegeteld): pit 4.053 → 146, B 646 → 87, archipel 30 → 9, C/D/E 0–1 → afgehandeld met een klein residu.
- **`glowing_fissure`/`smoldering_stone` rotaties (46b):** in-game gezien (`build/afr-shots/c_glowing_fissure_rot*.png`, `c_smoldering_stone_rot_dev.png`).
- **S-smoke na W-terrein** (verse wereld `afr-smoke2`, server + Dev): `tpring 1|2|3`, `tpring 1|2|3 nether`, `tpboundary 2|3`; portal Nether-3 (891, 36, 787) → ring 3 (3.566, 38, 3.146) en ring 3 (4.758, 99, 17) → Nether-3; log zonder ERROR/far chunk.
- **Lek (4.409–4.412, Y 5–9, −510…−508):** besluit main A: backlog, fix in golf 3.
