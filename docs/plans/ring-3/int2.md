# Integratie 2 — ring 3, golf 2

Scope: `infra` (integratie). Protocol: `waves.md` §6. Branch `ring3/int2`, worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/int2`, basis main `6ef5a85`.

Mergevolgorde (afwijking van §6, akkoord main): T → W-nether → C → A, daarna W-biomes zodra 41b gecommit en gereviewd is. Al op main: 50-0, 39f, B 46-rest, 42.

## Voortgang
Voortgang: 2026-10-09 start (worktree + branch, regels en waves gelezen)
Voortgang: 2026-10-09 15:22 T, W-nether, C, A gemerged (één conflict: docstring `tools/worldgen/ring3_surface.py`, 42-tekst gehouden, Nether-regel eruit); integratiefixes: `NetherRing3BiomesTest` → `look(key).colours()`, Nether-3-biome-JSON geregenereerd, `PROTOCOL_VERSION` 5 → 6, `cold_exposed` één TagKey (`ModTags.Biomes.COLD_EXPOSED`); beide surface-scripts idempotent; B (`--rerun-tasks`, 637 tests, 0 fouten, incl. `ClientIsolationTest`) en D groen
