# Fase 2 golf 3 + 4 — stroom A (pijlen)

Branch `f2g34/arrows` op main `9becd290`, n = 68. Categorie `items`. Bron: `fase-2-items.md` §1.2, §6.4, §6.7.

## Wat
- `fire_arrow` (ring 3): zwavel + 4 pijlen → 4; een geraakt doel brandt 5 s; de pijl zelf staat nooit in brand, dus hij
  steekt geen blokken aan (vanilla laat een brandende pijl TNT, kampvuren en kaarsen aansteken).
- `poison_arrow` (ring 4): gifkristalscherf + 4 pijlen → 4; vanilla Gif I, 5 s (de volle tijd, niet het achtste van een
  tipped arrow).
- Eén klasse `entity/EffectArrow` (`AbstractArrow`) voor beide, met het effect als enum `entity/ArrowEffect`; het
  effect volgt uit het entity-type (de constructor van `AbstractArrow` vraagt al om het pickup-item voordat een veld
  gezet kan worden). Eigen entity-types `corruptedadventure:fire_arrow` / `poison_arrow` (nieuw register
  `ModEntityTypes`). Item `item/EffectArrowItem` (`ArrowItem`): boog, kruisboog (via `#minecraft:arrows`) en dispenser
  (`DispenserBlock.registerProjectileBehavior` in common setup; vanilla registreert zijn pijlen bij bootstrap).
- Renderer `client/entity/EffectArrowRenderer` (`ArrowRenderer`), alleen via `CorruptedAdventureClient` (`Dist.CLIENT`).

Client/server: het effect (brand, gif) zet de server in `doPostHurtEffects`; vanilla synct entity, brand en effect. De
client tekent alleen de pijl en het spoor-deeltje.

## Voortgang
- 2026-10-27 19:39 start; setup n = 68.
- 19:55 code af; build gestart.

## Voorlopig (bevestigen bij de teststop)
- Receptvorm (beide): materiaal in het midden, de 4 pijlen op de vier zijden (` A ` / `AMA` / ` A `) → 4.
- Spoor in de vlucht (client): een vlammetje per tick (brandpijl), een groen effect-deeltje zoals een vanilla gifpijl
  (gifpijl); in de grond geen deeltjes.
- Schade en vlucht zoals een gewone pijl (basisschade 2).
- In `#minecraft:arrows` (entity-tag) zoals vanilla's pijlen: ze breken dus ook puntig druipsteen, chorusbloemen en
  versierde potten (`#impact_projectiles`).
- NL-namen: "Brandpijl", "Gifpijl".

## W-checklist (INT)
- Boog en kruisboog schieten beide pijlen; Infinity geeft geen gratis pijlen (zoals spectral/tipped).
- Brandpijl op een mob: brandt ~5 s; op TNT, kampvuur (uit), kaars: niets gaat aan; geen vuurblok.
- Gifpijl op een mob: Gif I ~5 s, groene swirl in de vlucht.
- Dispenser schiet beide; de pijl is op te rapen en geeft het eigen item terug.
- Dedicated server + 2 clients: renderer alleen op de client, pijlen zichtbaar voor beide spelers.
- Creative: `fire_arrow` in tab ring 3, `poison_arrow` in tab ring 4.

## Textures (placeholder, T vervangt)
`item/fire_arrow`, `item/poison_arrow` (16×16), `entity/projectiles/fire_arrow`, `entity/projectiles/poison_arrow`
(32×32, layout van vanilla's `arrow.png`: zijkant x 0–15 / y 0–4, achterkant x 0–4 / y 5–9). Getekend door een
kladscript buiten de repo, zonder vanilla-pixels.

## CODEBASE-delta
- **Registries:** nieuw `registry/ModEntityTypes` (`DeferredRegister<EntityType<?>>`): `fire_arrow`, `poison_arrow`
  (afmetingen/tracking als vanilla's pijl). Registratie in `CorruptedAdventure` na `ModItems`, plus
  `EffectArrowItem.register` (dispenser-gedrag in common setup).
- **Classes:** `entity/ArrowEffect` (enum FIRE/POISON: effect bij een treffer, spoor-deeltje, entity-type, item),
  `entity/EffectArrow` (de pijl), `item/EffectArrowItem` (`ArrowItem` + dispenser), `client/entity/EffectArrowRenderer`
  (`EntityRenderersEvent.RegisterRenderers`).
- **Items:** `ModItems.FIRE_ARROW` (einde ring 3-sectie), `ModItems.POISON_ARROW` (einde ring 4-sectie).
- **Datagen:** `lang/ArrowText`, `model/ArrowItemModels`, `tags/ArrowItemTags` (`#minecraft:arrows`),
  `recipe/ArrowRecipes`, nieuwe provider `ModEntityTypeTagsProvider` (entity-tag `#minecraft:arrows`).
- **GameTests:** `gametest/ArrowGameTests` (5) met template `structure/gametest/arrow_range.nbt` (9 × 3 × 3, leeg).
- **Textures:** de vier hierboven.

## Beslissingen
- (geen nieuwe; alles volgt §6.4/§6.7 of staat onder "Voorlopig")

## Backlog
- (geen)
