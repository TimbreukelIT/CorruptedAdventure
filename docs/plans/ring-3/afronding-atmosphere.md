# Golf 2-afronding — stroom A (sfeer): 50c

Scope: `atmosphere`. Branch `ring3/atmosphere-afr` op main `c6cd90d`, worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-afb7325c2a1a62d1d`. Bron: `teststop-4b-feedback.md` "Beslissingen" 4 (sfeer en geluid, verre rookpluimen) en 5 (rode lucht), backlog ring 3 golf 1/2 (lavabommen r. 32/36, geiser-waarschuwing r. 44, pluimen voorbij de render distance r. 54).

Testafspraak (afronding): alleen B (`./gradlew build --rerun-tasks`) + D (`runData`, `git diff --exit-code src/generated`); de rest in INT (W-checklist hieronder). Raakt de noise-stap niet.

## 50c — pakket
1. **Geiser-waarschuwing** `block.geyser.warning`: eigen geluid (borrelen + laag gegrom, ~1 s, 2 varianten) uit `tools/sounds/Ring3Sounds.java`, ondertitel en + nl; `GeyserBlock.warn` speelt het (één regel in het blok van B, met akkoord van main).
2. **Lavabommen:**
   - niet stapelen: de rate per speler gedeeld door het aantal in aanmerking komende spelers binnen `lavaBombRadius` (horizontaal, zichzelf meegeteld), zodat een groep samen ongeveer de dichtheid van één speler krijgt;
   - opruimen: de server houdt de eigen bommen bij en verwijdert ze na 10 s (200 ticks; ook in een chunk die niet meer tikt); een bom die van schijf geladen wordt (de chunk was weg) komt de wereld niet meer in.
3. **Verre rookpluimen zonder terreinmist:** de wolken van de pluimen worden geen vanilla-particles meer, maar een eigen client-only render-pass (`RenderLevelStageEvent.AFTER_WEATHER`) met de shader `position_tex_color` (geen mist). Eigen, zachte waas naar de mistkleur op afstand; daglicht uit `getSkyDarken`; niet onder water/lava, blind of in duisternis; voorbij het verre vlak van de projectie (render distance × 4) naar binnen getrokken langs de kijkrichting (zelfde hoek en grootte). Zelfde simulatie en budget als 50b (8 pluimen, LOD, Particles-optie).
4. **Rode lucht bij een uitbarsting:** de lucht van `volcanic_peaks` is al oranjerood, dus het rood van 45 viel weg. Dieper, donkerder karmijn, volle sterkte (share 1,0) en die sterkte sneller bereikt (de eerste helft van de fade-in), waas sterker; nog steeds een gloed die 's nachts blijft (45) en alleen de kleur (geen extra effecten).

**Payload:** geen wijziging (geen protocolbump).

## Voortgang
Voortgang: 2026-10-09 start 50c (plan)
Voortgang: 2026-10-09 50c gebouwd: geiser-waarschuwing (`Ring3Sounds` 2× identieke WAV's; alleen de 2 nieuwe `.ogg`'s, de herschreven oude teruggezet), lavabommen (delen + opruimen), pluimen als eigen render-pass, rode lucht; B groen (`--rerun-tasks`, 675 tests incl. `ClientIsolationTest`, nieuw: `PlumeBillowTest` 4, `EruptionRendererTest` 1, `LavaBombsTest` +2, `VolcanoPlumeRendererTest` +1); D groen (diff: alleen `sounds.json` + en/nl-ondertitel van `block.geyser.warning`, meegecommit)

## W-checklist (INT, dedicated server + 2 clients, seed `20260930`)
- [ ] Geiser in `steam_fens`: 1 s vóór elke uitbarsting het eigen borrel/gromgeluid met ondertitel "Geyser gurgles" (nl "Geiser gorgelt"), daarna de burst.
- [ ] `/ca weather 3 eruption` met 2 spelers naast elkaar op de pieken (4.110, 672): ongeveer evenveel bommen als met één speler (niet 2×); daarna één speler ver weg laten lopen: de bommen rond de achterblijver blijven komen.
- [ ] Bommen opruimen: tijdens een uitbarsting wegteleporteren (> render distance) en na > 10 s terug: geen stilstaande vuurballen in de lucht; na een herstart van de server ook niet.
- [ ] Pluimen vanaf `dry_riverbeds` (`/ca tpboundary 2`, oostwaarts) bij render distance 6, 12 en 32: overal zichtbaar tot 512 blokken, geen mistsilhouet; vulkanen dichtbij: de kegel verdwijnt correct achter bergen ervoor, de pluim gaat achter de wolken langs; geen pluim onder water of met blindheid.
- [ ] Pluimen 's nachts donker (alleen de gloed bij een uitbarsting licht op); Particles: Minimal → dunner; FPS met 8 pluimen in beeld (pieken) en met Fabulous-graphics.
- [ ] Uitbarsting: de lucht wordt duidelijk donker karmijnrood (vergelijk met `w_peaks_clear`/`w_peaks_eruption` van INT2), al halverwege de fade-in (~15 s na de start); 's nachts blijft een rode gloed.

## CODEBASE-delta
- **50c, geluid:** sound event `block.geyser.warning` (`ModSoundEvents.GEYSER_WARNING`, ondertitel "Geyser gurgles" / "Geiser gorgelt" in `Ring3AmbienceText`), eigen audio `sounds/block/ring_3/geyser_warning_1..2.ogg` (variant `gurgle` in `tools/sounds/Ring3Sounds.java`: laag gegrom 50–80 Hz + steeds snellere, hogere bubbels, 1,2/1,4 s). `GeyserBlock.warn` speelt het (0,8 / 1,0) in plaats van `block.boiling_mud.bubble` (één regel in het blok van B).
- **50c, lavabommen (`weather/LavaBombs`):** de rate per speler wordt gedeeld door 1 + het aantal andere spelers die in aanmerking komen binnen `lavaBombRadius` (horizontaal; `shared`), dus een groep krijgt samen de bommen van één speler. Eigen bommen worden bijgehouden (`Flight`: bom + gametijd) en na `MAX_AGE_TICKS` = 200 (10 s) verwijderd (`ServerTickEvent`, elke seconde; ook in een chunk die niet tikt); een bom die met zijn chunk van schijf geladen wordt, wordt geweigerd (`EntityJoinLevelEvent.loadedFromDisk`).
- **50c, pluimen:** `client/particle/VolcanoPlumeParticle` is weg; nieuw `client/weather/PlumeBillow` (dezelfde simulatie: stijging × 0,996, drift × 0,985 + wind, max. 0,05/tick, zwelt ~3×, 320–480 ticks, `prewarm`) die `VolcanoPlumeRenderer` zelf tikt (max. 2.000) en tekent in een eigen pass op `RenderLevelStageEvent.Stage.AFTER_WEATHER`: shader `position_tex_color` (geen mist), particle-atlas, achter naar voor gesorteerd, diepte-test aan, diepte schrijven alleen bij Fabulous (zoals vanilla-regen), geen culling; daglicht via `getSkyDarken` (gloeiende voet zelflichtend), eigen waas naar de mistkleur (`RenderSystem.getShaderFogColor`) tot 0,45 op 512 blokken (`haze`); voorbij 0,85 × `getDepthFar()` naar binnen getrokken langs de kijkrichting (zelfde hoek en grootte); niet bij water/lava in beeld, blindheid of duisternis (`RingWeatherRenderer.clearView`). Wolken van een pluim die uit de lijst valt leven uit; leeg bij uitloggen of buiten de overworld.
- **50c, uitbarsting (`client/weather/EruptionRenderer`):** lucht diep karmijn `8A1A0C` (was `B3361A`) met share 1,0 (was 0,85), waas `6E1C0E` (was `7A2A14`) met share 0,85 (was 0,7); beide via `colourAmount(strength)` = min(1, 2 × strength), dus volle kleur na de halve fade-in (15 s). Nachtgloed 0,45 en mistafstand 0,6 ongewijzigd.

## Beslissingen
- 2026-10-09, atmosphere: lavabommen bij meerdere spelers: de rate per speler gedeeld door het aantal spelers in aanmerking binnen `lavaBombRadius`; bommen leven hooguit 10 s en komen niet terug uit een opgeslagen chunk (50c; backlog 45/INT1).
- 2026-10-09, atmosphere: rookpluimen als eigen client-render-pass zonder terreinmist (na het weer), met een eigen waas tot 45 % mistkleur op 512 blokken; verre wolken naar binnen getrokken binnen het verre vlak (50c; 4b-besluit 4).
- 2026-10-09, atmosphere: rode lucht bij een uitbarsting = diep karmijn `8A1A0C`, volle share, vol na de halve fade-in; waas `6E1C0E` 0,85 (50c; 4b-besluit 5: sterker kleuren).

## Backlog
