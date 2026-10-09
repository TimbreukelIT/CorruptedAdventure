# Golf 2-afronding ring 3 — blokken en condities (46b, 55b)

Worktree: `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a945f84b54317ff61`, branch `ring3/blocks-afr` (op main `c6cd90d`).
Bron: `teststop-4b-feedback.md` "Beslissingen" 4 (groep "blokken en condities"); backlog r. 198, 199, 219, 222.
Testen licht (`build/agent-rules-afronding.md`): per pakket B (`build --rerun-tasks`) + D (`runData`, `git diff --exit-code src/generated`). Geen server/client.

## 46b — `blocks`: willekeurige rotaties voor `glowing_fissure` en `smoldering_stone`
- `ModBlockStateProvider.rotatedGlowingCube`: 32 varianten per blok = de 16 x/y-rotaties van netherrack × het gewone model en een gespiegelde kopie `<naam>_mirrored` (alle vlakken `uv [16,0,0,16]`, zoals vanilla `cube_mirrored_all`). Elk vlak toont de texture zo in alle 8 oriëntaties (4 rotaties × spiegeling); de stone-variant (alleen y + spiegel) zou de zijkanten niet draaien.
- De rotatie van een variant draait het hele model, dus beide elementen; de gloeilaag (`neoforge_data`, emissief) blijft op zijn pixels. Het gespiegelde model spiegelt beide lagen gelijk.
- `overlayCube` gesplitst: `overlayModel(name, …, mirrored)` bouwt het model, `overlayCube` registreert het als enkele variant (ongewijzigde output voor de andere blokken).
- `smoldering_stone` past: zijn celtextuur wrapt, en het voorbeeld met rotaties is rustiger dan het huidige raster. `smoldering_ash` niet gevraagd, blijft één variant.
- **Gevolg voor de scheur:** de `glowing_fissure`-texture sluit aan op buren (randpunten links/rechts op rij 9,5, boven op kolom 6,5; 57a). Met rotaties lopen de scheuren van buren niet meer als doorgaande lijnen door, maar vormen ze een grillig netwerk met Y-knooppunten; de haarlijnranden maken de losse eindjes klein. Voorbeelden (8×8 vloer, zelfde seed): `build/b46/fissure_current.png`, `fissure_all8.png`, `fissure_mirror_only.png` (alleen spiegelen behoudt de lijnen maar blijft streperig), `smoldering_stone_current.png`/`_all8.png` (worktree, niet in git). Zie Backlog.
- Item-modellen ongewijzigd (ouder `block/<naam>`, de eerste variant).

Voortgang: 2026-10-09 46b datagen + runData (2 blockstates, 2 nieuwe `_mirrored`-modellen) — B groen (667 tests, 0 fouten, incl. `ClientIsolationTest`), D: output meegecommit.

## 55b — `conditions`
(volgt)

## W-checklist voor INT
- [ ] 46b: een vloervlek `glowing_fissure` en een vlak `smoldering_stone` (dag en nacht): geen tegelraster, gloeilaag valt in elke variant precies op de scheuren (geen verschoven gloei), geen z-fighting of ontbrekende vlakken bij x-rotaties, cullface klopt tegen buren.
- [ ] 46b: break-particles en het item in de hand ongewijzigd.

## CODEBASE-delta
- `datagen/ModBlockStateProvider`: `rotatedGlowingCube(block)` (32 varianten: 16 x/y-rotaties × gewoon/gespiegeld model, 46b) voor `glowing_fissure` en `smoldering_stone`; `overlayModel(name, particle, base, overlay, emissive, renderType, mirrored)` (model van `overlayCube`, nu ook gespiegeld).
- Gegenereerd: `models/block/glowing_fissure_mirrored.json`, `models/block/smoldering_stone_mirrored.json`; blockstates van beide blokken met 32 varianten.

## Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-09 | blocks | 46b: `glowing_fissure` en `smoldering_stone` krijgen 32 willekeurige varianten (netherrack-rotaties × gespiegeld model); de scheuren lopen daardoor niet meer als doorgaande lijn over buren door | Teststop 4b, beslissing 4 (tegelraster breken); spiegelen alleen bleef streperig |

## Backlog
- [textures] `glowing_fissure` met 46b-rotaties: overwegen de randpunten symmetrisch te leggen (midden van elke rand, rij/kolom 7,5–8) zodat geroteerde buren weer aansluiten; nu een netwerk met kleine haarlijn-eindjes. Beoordelen in INT. (bron: 46b)
