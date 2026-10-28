# Fase 2 golf 5 — stroom A (glitchpijl)

Branch `f2g5/arrows` op main `d55ff491` (merge-base, ook de review-basis), n = 68. Categorie `items`. Bron:
`fase-2-items.md` §7.4; pijl-basis uit golf 3 + 4 (`golf-34-A.md`).

## Wat
- `glitch_arrow` (EN "Glitch Arrow", NL "Glitchpijl"): derde waarde `ArrowEffect.GLITCH` op de pijl-basis van golf 3 + 4
  (`EffectArrow`, `EffectArrowItem`, `EffectArrowRenderer`, dispenser in common setup: alles loopt over
  `ArrowEffect.values()`). Eigen entity-type `corruptedadventure:glitch_arrow`; item achteraan de ring 5-sectie (tab 5);
  in `#minecraft:arrows` (item- en entity-tag); recept ` A ` / `AMA` / ` A ` met `glitch_shard` → 4.
- Treffer op een mob (`Mob`, dus geen speler of pantserstandaard): `entity/GlitchHop` zoekt met de veilige-plek-zoektocht
  van de void-teleport (`condition/VoidTeleportTarget.find`, 12 richtingen, voeten 2 boven tot 3 onder) een plek op
  1–5 blokken. Eigen kolomregel: binnen de wereldgrens, 3 × 3 chunks geladen, nooit `Ring.VOID`, en in ring 5 (beide
  dimensies) nooit verder naar buiten (`effectiveDistance` neemt niet toe: dat is precies de maat van de void-grens).
  Daarbij moet de hele hitbox van de mob er passen (`Terrain.fits`: geen botsing, geen vloeistof), want de zoektocht is
  op een speler gemaakt. Geen plek: de mob blijft staan (geen geluid).
- Bij een sprong: `Ring5Glitches.teleported` (geluid `ambient.ring_5.glitch` + 12 `glitch_pixel` op vertrek en
  aankomst), vanilla `teleportTo`, valafstand weg, navigatie gestopt, `GameEvent.TELEPORT`.
- Spoor in de vlucht (client): één `glitch_pixel` per tick, zoals de andere pijlen; in de grond niets.

Client/server: de sprong zit in `doPostHurtEffects` (server); vanilla synct positie, geluid en deeltjes. De renderer
blijft de bestaande `EffectArrowRenderer` in `client/`. Kleine aanpassingen buiten `items` (alleen hergebruik):
`VoidTeleportTarget` public + `Terrain.fits` (default `true`, de void-teleport verandert niet) en
`Ring5Glitches.teleported(Entity, …)` (was `ServerPlayer`).

## Voortgang
- 2026-10-28 16:05 start; setup n = 68; basis `d55ff491`.
- 16:27 code af; build gestart.
- 16:35 build (incl. `GlitchHopTest`) + runData groen; 16:37 GameTests 25/25 groen; 16:38 server-smoke n = 68 groen (Done in 9 s, geen client-class-fouten).

## Voorlopig (bevestigen bij de teststop)
- Receptvorm ` A ` / `AMA` / ` A ` → 4 (zoals brand- en gifpijl).
- Een geraakte speler springt niet (ook geen pantserstandaard: alleen `Mob`).
- "Nooit richting de void": in ring 5 (overworld én Nether-5) springt een mob nooit verder naar buiten; daarbuiten alle
  richtingen; nooit in de void-rand.
- Niet: bazen (`#c:bosses`) en een mob met een berijder (die berijder kan een speler zijn); een mob die zelf rijdt
  stapt af en springt.
- Geen veilige plek (of een vliegende mob zonder vloer binnen 3 blokken): geen sprong, geen geluid.
- Het glitchgeluid op `PLAYERS` (zoals de void-teleport); een waterdier kan op het droge belanden (de zoektocht wil
  een vaste vloer zonder vloeistof).
- Schade en vlucht zoals een gewone pijl.

## W-checklist (INT)
- Boog en kruisboog schieten de glitchpijl; dispenser ook; oprapen geeft een glitchpijl terug.
- Een mob springt 1–5 blokken, nooit in een muur, lava of over een rand; glitchgeluid + pixels op beide plekken.
- In ring 5, buitenband: een mob springt nooit richting de void.
- Een speler die geraakt wordt springt niet.
- Dedicated server + 2 clients: beide spelers zien de sprong en het spoor.
- Creative: `glitch_arrow` in tab ring 5.

## Textures (placeholder, T vervangt)
`item/glitch_arrow` (16 × 16), `entity/projectiles/glitch_arrow` (32 × 32, layout van vanilla's `arrow.png`):
de eigen gifpijl-placeholders met het groen vervangen door magenta/cyaan (kladscript buiten de repo).

## CODEBASE-delta
- **Registries:** `ModEntityTypes.GLITCH_ARROW`; `ModItems.GLITCH_ARROW` (einde ring 5-sectie, vóór Nether-ring 5).
- **Classes:** `entity/ArrowEffect` + `GLITCH` (spoor `glitch_pixel`); nieuw `entity/GlitchHop` (server: de sprong,
  kolomregel `allowsRing`, `LevelTerrain` met `fits` voor de hitbox); `condition/VoidTeleportTarget` is public en
  `Terrain` heeft `fits` (default `true`); `weather/Ring5Glitches.teleported` neemt een `Entity`.
- **Datagen:** `ArrowText`, `ArrowItemModels`, `ArrowItemTags`, `ArrowRecipes`, `ModEntityTypeTagsProvider` + glitchpijl.
- **GameTests:** `ArrowGameTests` + 4 (sprong naar een veilige plek, nooit in blokken, alleen waar de hitbox past,
  dispenser); nieuw template `structure/gametest/glitch_pad.nbt` (11 × 4 × 11, leeg). Unit test `entity/GlitchHopTest`.
- **Textures:** de twee hierboven.

## Beslissingen
- (geen nieuwe; §7.4 of "Voorlopig")

## Backlog
- (geen)
