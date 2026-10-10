# Integratie golf 3 (INT3) — ring 3

Scope: `infra` (integratie, snel protocol `waves.md` §6 "Golf 3 — snel protocol"). Branch `ring3/int3` op main `678a7e4` (bevat I = `tools/test` en B = 38c). Poort n = 8 (server 25578, RCON 25588). Mergevolgorde: T (`ring3/textures-g3`) nu, W (`ring3/worldgen-g3`) na het signaal van main.

## Plan
1. Merge T → B (`build --rerun-tasks`), D (`runData`), vangnet `git diff --exit-code src/generated`.
2. Na het signaal: merge W → B, D, vangnet.
3. S-smoke: server + 1 client, verse wereld `ring3-wave3`: `/ca tpring 3`, `tpring 3 nether`, `tpboundary 2|3`, portal ring 3 ↔ Nether-3; serverlog zonder client-class-fouten. Shots (eigen client-PID): askorst-daken vóór buismonden (en instorten onder de speler), smalle lavaoevers, Nether-3-scheuren, gloeiende scheur.
4. Offline: `lavascan.py` (lek in de afgrond 0), `det.sh` (omgekeerd; `own` 0, 'other' per oorzaak), `featidx.py` ring 1/2, Nether-3-ijs (0 blootliggend ijs aan scheuren), W-checklists van B, T en W.
5. Docs samenvoegen (CODEBASE-delta's I, B, T, W; Beslissingen; Backlog + backlog-uitkomst `golf-3.md`).
6. "klaar voor review-doorsturen: INT3" aan main; fixes als extra commit. Daarna (op signaal) prestatie en `teststop-4c.md`.

## Voortgang
Voortgang: Sat Oct 10 15:20 WEDT 2026 worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a9d66a2a59f334c4e`, branch `ring3/int3` (vanaf main `678a7e4`); `setup.sh int3 8`.
Voortgang: Sat Oct 10 15:22 WEDT 2026 T gemerged (`5887a74`, tip `840d3fa`, geen conflicten).
Voortgang: Sat Oct 10 15:24 WEDT 2026 na T: B `--rerun-tasks` groen (711 tests, 0 fouten), D groen, `git diff src/generated` leeg.
Voortgang: Sat Oct 10 15:26 WEDT 2026 docs-delta's I (T0), B (38c), T (57 r1) samengevoegd in `CODEBASE.md`, `02-beslissingenlog.md`, `fase-1-wereld.md`, `docs/backlog.md` (incl. de backlog-uitkomst van `golf-3.md`: infra-ronde / fase 2–4 / ring 4 / blijft staan). W-deel na de W-merge.
Voortgang: Sat Oct 10 15:31 WEDT 2026 W gemerged (`71123b6`, tip `68d7f98`, geen conflicten).
Voortgang: Sat Oct 10 15:36 WEDT 2026 na W: B `--rerun-tasks` groen (721 tests, 0 fouten), D groen, `git diff src/generated` leeg; `ring3_surface.py` en `nether_ring3_surface.py` idempotent; docs-delta W samengevoegd (CODEBASE incl. kop/status, beslissingenlog, fase-1, backlog).
