# I — zwakte-items (zonnesteenflits, vulkanieten hoorn)

Stroom `I`, n = 66, worktree `P:/Projecten/Java/wt/f4-items`, branch `f4/items`.
Basis (`git merge-base main HEAD`): `f626c7cc`.
Bron van waarheid: `docs/design/fase-4-nightmares-concept.md`, secties "Fase 4-sparring alle Nightmares", Ring 2, Ring 3 en
"Zwakte-items (stroom I)".

## Wat
1. `items`: effecten `corruptedadventure:dazzled` en `stunned` (eerste, aparte commit voor R2/R3).
2. `items`: `sunstone_flash` (ring 2) en `volcanite_horn` (ring 3): gedrag, config `[weakness_items]`, geluid, recepten,
   tabs, lang en/nl, itemmodellen, GameTests.
3. `textures`: `tools/textures/Phase4WeaknessTextures.java` (2 items + 2 effect-iconen).

## Voortgang
- 2026-10-28 19:30 start; `setup.sh I 66` gedaan. Lock `ca-user-test.lock` staat: geen JVM, code geschreven zonder compiler.
- 19:38 commit `af62554f` (effecten); aan main gemeld.
- 19:45 commit `e09d2f0b` (doelwit ook direct bij de start los).
- 19:54 items, config, client-overlay, datagen-bronnen, GameTests, textures-script en docs geschreven; preview
  `build/texture-preview/phase4weakness-draft.png` naar main. Wacht op vrijgave voor build/runData/GameTests/server-smoke.
- 20:05 gebruiker keurt de textures goed; verfijning: meer gloed op de aders van de hoorn (extra `U=FFC860`, meer
  `t`/`u` in de banden).

- 20:40 "go I": textures-script gedraaid; build → 2 fouten in `ModBlocksTest` (materiaalvolgorde telde flits/hoorn mee) →
  filter uitgebreid; runData; build groen (6m44s).
- 21:02 GameTests 43/43 (batches `weakness_flash`, `weakness_horn`); 21:03 server-smoke `ismoke`: start zonder fouten,
  GeckoLib geladen, `/effect give` dazzled + stunned op een husk via RCON: snelheid 0,0. Slot vrij. Commits `8d0e15d6`
  (textures), `830af7c1` (generated + testfix). Daarna `/code-review low 7fff91ed..f4/items` (alleen eigen commits).

## Voorlopig (bevestigen bij de teststop)
- **Effecten:** op elke `Mob` (ook een Nightmare): doelwit los bij start en elke tick, nieuwe doelwitten geannuleerd
  (`LivingChangeTargetEvent`). `stunned`: modifiers −100 % op `movement_speed`, `flying_speed`, `jump_strength`, pad stopt;
  zwaartekracht blijft (vliegers met eigen beweging, zoals de phantom, kunnen nog drijven). Kleuren `FFD25E` / `9A8C84`.
- **Speler en `dazzled`:** warm wit scherm (alpha max. 0,85), vervaagt over de laatste 40 ticks; geen ander gameplay-effect.
- **Flits:** direct bij rechtsklik (geen houding), bereik 16 vanaf de ogen, kegel 30° (halve hoek), zichtlijn nodig (ogen tot
  ogen), 3 s `dazzled`; spelers alleen als `canHarmPlayer` (pvp + teams), nooit de gebruiker. Duurzaamheid 64, cooldown 15 s.
  Geluid: amethist-resonantie pitch 1,5 (bereik 48); deeltjes `flash` + waaier `end_rod`.
- **Hoorn:** houding en duur als de galmschelp (60 ticks), straal 12 vanaf de ogen tot de dichtstbijzijnde rand van de
  hitbox (grote vliegers tellen met hun dichtste deel), door muren; 2 s `stunned`; nooit spelers (geen friendly fire).
  Duurzaamheid 32, cooldown 30 s. Geluid: geitenhoorn-varianten 0 en 5, pitch 0,7 (bereik 96); stuurt ook
  `INSTRUMENT_PLAY` (breekt dus ook de stilte, zoals de galmschelp). Deeltjes: ring `small_gust`, `ash` op wie verstijft.
- **Recepten:** flits ` S `/`SGS`/` T ` (zonnesteenstaaf, glasruit, turkoois); hoorn `VHV`/` R ` (vulkanietstaaf, elke
  geitenhoorn, vuurrobijn). Categorie `COMBAT`; ontgrendeld met de zonnesteenstaaf resp. de geitenhoorn.
- **Namen:** "Sunstone Flash"/"Zonnesteenflits", "Volcanite Horn"/"Vulkanieten hoorn"; effecten "Dazzled"/"Verblind",
  "Stunned"/"Verstijfd"; ondertitels "Sunstone flashes"/"Zonnesteen flitst", "Volcanite horn sounds"/"Vulkanieten hoorn schalt".
- **Beide brandvrij**, zoals de zonnesteen- en vulkaniet-set. Itemmodellen plat (zoals de galmschelp).
- **Tabs:** flits in tab 2 na `salted_jerky`, hoorn in tab 3 na `fire_arrow`.

## Beslissingen
- 2026-10-28 gebruiker: textures van flits, hoorn en beide effect-iconen akkoord (aders van de hoorn feller).

## Backlog
- (nog niets)
