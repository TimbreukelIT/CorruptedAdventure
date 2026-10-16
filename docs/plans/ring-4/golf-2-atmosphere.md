# Ring 4 — golf 2, stroom A (sfeer): 80-0 (voorpakket), 80 sfeer ring 4, zone 4→5, Nether-4

Scope: `atmosphere`. Branch `ring4/atmosphere-g2` (poort n = 5), basis `main` c5b7d11. Plan: `golf-2.md` §2 (A-0, A), contract §6 (geluid, particles) en §8 (lang). Precedent: `docs/plans/ring-3/golf-2-atmosphere.md` (50-0).

## 80-0 — voorpakket (op main, vóór B)
Alleen registratie, zodat B (72) en de andere stromen de ids kunnen gebruiken; de echte audio en het gedrag van de sfeer komen in 80.

- **Sound events** (`ModSoundEvents`, ids onder `corruptedadventure:`): `ambient.ring_4.{water, night_choir, reeds, bubbles, sundered_drone}`, `ambient.nether_ring_4.{wind, crystal_hum}`, `block.swamp_gas.ignite`, `block.bubble_spring.bubble`, `block.updraft.flow`, `block.sucking_mud.slurp` (contract-delta golf 2). De drie weer-events `weather.ring_4.*` bestonden al (70).
- **`sounds.json`**: vanilla-placeholders (type `event`) tot het ring 4-geluidsscript van 80: water = `block.water.ambient` 0,8; nachtkoor = `block.note_block.harp` 0,8 / 0,3; riet = `block.grass.break` 0,7 / 0,4; bellen = `block.bubble_column.upwards_ambient` 0,8; drone = `ambient.warped_forest.loop` 0,5; Nether-wind = `ambient.soul_sand_valley.loop` 1,1; kristalgezoem = `block.amethyst_block.resonate` 0,8 / 0,5; gas = `item.firecharge.use` 0,8; bellenbron = `block.bubble_column.bubble_pop`; opwaartse stroom = `block.bubble_column.upwards_inside`; zuigmodder = `block.honey_block.slide` 0,6. Loops (water, drone, Nether-wind, kristalgezoem) zonder ondertitel, zoals vanilla.
- **Ondertitels** (en + nl) in `datagen/lang/Ring4AmbienceText` (nachtkoor, riet, bellen en de vier blokgeluiden).
- **Particle types** (`ModParticleTypes`): `will_o_wisp`, `blossom`, `firefly`, `swamp_gas`, `toxic_bubble`, `updraft_bubble`, `glitch_speck`, `bone_dust`; `particles/<naam>.json` met de 71a-sprites `<naam>_0..3` (alle aanwezig, geen placeholder nodig).
- **Providers** (`client/particle/`, alleen via `CorruptedAdventureClient`, `Dist.CLIENT`): `GlowMoteParticle` (dwaallicht: koel groencyaan, zachte puls; vuurvlieg: geelgroen, knippert; beide dwalen zelf en full bright), `BlossomParticle` (dwarrelt, zwaait en draait; eigen kleur), `DriftingHazeParticle` (moerasgas: subtiel geel, stijgt heel traag; botstof: beige, zakt), `RisingBubbleParticle` (gif: groen, traag, kort; opwaarts: wit, snel; een bel die in een vloeistof begint knapt als hij eruit komt), `GlitchSpeckParticle` (springt, wisselt sprite, flikkert; eigen kleuren, full bright). Kleuren en gedrag zijn een eerste versie; 80 stemt ze af.
- Client/server: alleen registraties (beide kanten) en client-assets; geen payload, geen gameplay.
- Tests: B (incl. `ClientIsolationTest`), D (`runData` tweemaal, `git diff --exit-code src/generated` schoon).

## 80 — sfeer ring 4, zone 4→5, Nether-4
Volgt na de merge van 80-0 (zie `golf-2.md` §2 A).

## Voortgang
- 2026-10-16: 80-0 gebouwd; B en D groen; review (low, 1 bevinding: plafondcheck van `RisingBubbleParticle` op `yd`, vanilla zet `yd` niet op 0) opgelost; B opnieuw groen.

## CODEBASE-delta
- `registry/ModSoundEvents`: 11 nieuwe ring 4/Nether-4 events (zie 80-0).
- `registry/ModParticleTypes`: 8 ring 4/Nether-4 particle types.
- `client/particle/`: `GlowMoteParticle` (`Glow`-record, `WILL_O_WISP`, `FIREFLY`), `BlossomParticle`, `DriftingHazeParticle` (`Haze`, `SWAMP_GAS`, `BONE_DUST`), `RisingBubbleParticle` (`Bubble`, `TOXIC`, `UPDRAFT`), `GlitchSpeckParticle`; geregistreerd in `CorruptedAdventureClient.onRegisterParticleProviders`.
- `datagen/ModSoundDefinitionsProvider`: helper `placeholder(event, pitch, volume)`; `ModParticleDescriptionProvider`: 8 sprite-sets.
- `datagen/lang/Ring4AmbienceText`: ondertitels ring 4-ambience en -blokgeluiden (nu ook `BLOCK`-sleutels).
- `src/generated`: `sounds.json`, `lang/en_us.json`, `lang/nl_nl.json`, `particles/{will_o_wisp,blossom,firefly,swamp_gas,toxic_bubble,updraft_bubble,glitch_speck,bone_dust}.json`.

## Beslissingen
- 2026-10-16 | atmosphere | 80-0: vier provider-klassen met een stijl-record per familie (licht, bel, waas) i.p.v. acht klassen; dwaallicht/vuurvlieg, gif-/opwaartse bel en moerasgas/botstof delen een klasse | patroon `SteamPuffParticle` (tint per type), kleine klassen met één gedrag.
- 2026-10-16 | atmosphere | 80-0: blokgeluiden met ondertitel, loops zonder (vanilla-patroon, zoals ring 3) | geen ontwerpkeuze, conventie.

## Backlog
- `[blocks]` (B, 72) `bubble_spring` "eigen bel-particle": er is geen apart type in het contract; voorstel `updraft_bubble` (knapt aan het wateroppervlak omdat hij in water begint). Een eigen type alleen via main.
- `[atmosphere]` (80) het silhouet onder water (Q18) is een client-only particle zonder registratie (direct aan de particle engine), of anders een contract-delta via main.
- `[textures]` (T, 71b) particle-check in-game (backlog r. 273): tint en grootte van de 8 sprites met de providers van 80-0.
