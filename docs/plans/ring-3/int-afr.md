# Integratie golf 2-afronding (INT-afr) — ring 3

Scope: `infra` (integratie, snel protocol van main 2026-10-09). Branch `ring3/int-afr` op main `c6cd90d`, worktree `.claude/worktrees/int2`. Mergevolgorde: B → W-biomes → A nu, W-terrein (42b, 54b) als laatste na het signaal van main.

## Voortgang
Voortgang: 2026-10-09 start (regels afronding gelezen)
Voortgang: 2026-10-09 20:23 B (`943844e`), W-biomes (`a5d8358`), A (`2442dd0`) gemerged zonder conflicten; surface-scripts idempotent; B `--rerun-tasks` groen (693 tests), D groen, `git diff --exit-code src/generated` leeg
Voortgang: 2026-10-09 20:41 docs-delta's B/C, W-biomes, A samengevoegd (`a76853d`); S-smoke (server 25579 + Dev, wereld `afr-smoke`): beide mixin-configs laden, geen client-class-fout, join, `tpring 3`, `tpring 3 nether`, `tpboundary 3` (`steam_fens`), portal ring 3 ↔ Nether-3; log zonder ERROR/far chunk (alleen "moved too quickly"); shots in `build/afr-shots/` (overwoekerde krater, zwavelpoelen met kalkranden en gekleurde modder, zwavelkristallen, rode lucht + pluim bij `eruption`). Determinisme, feature-index en lavascan na de W-terrein-merge
