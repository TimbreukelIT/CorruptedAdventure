# Golf 1 — stroom A (sfeer): 45 ring 3-weer

Scope: `atmosphere` — werkpakket 45; branch `ring3/atmosphere-g1`. (50 en 56, sfeer en geluid, volgen in golf 2.)

**Doel:** de drie weertypes van ring 3 (antwoorden 10 en 11), gameplay server-side en beeld client-side:
- **asregen:** zicht omlaag, `ash_flake`-particles, hitte ×1,5;
- **uitbarsting:** rode lucht, gerommel, lavabommen, hitte ×2;
- **aswolk** (nieuw `ASH_CLOUD`): donker, hitte ×3, schuilen; frequentie *V6*.

Bron: `fase-1-wereld.md` §1.6 (weermodel, client-hook), Ring 3 "Weer" + sparring-aanvulling; `contract.md` §5, §6; sjabloon change 28 (`SandstormWind`, `HeatWaveDehydrationModifier`, `client/weather/SandstormRenderer`, `HeatWaveRenderer`, `HeatHazeEffect`, `FogBlend`, `SkyExposure`, `RingWeatherRenderers`).

## Werk
1. `RingWeatherType.ASH_CLOUD` (ring 3, overworld); `[weather.ring3]` met gewichten (*V6*), duur en hitte-factoren.
   - Een waarschuwing vooraf (aswolk ~20 s gerommel/verdonkering bij *V6* = A) via de bestaande fade-in (`intensity`), zonder extra payload.
2. **Hitte-factoren:** alleen de config-waarden (`ashRainHeatMultiplier` 1,5, `eruptionHeatMultiplier` 2,0, `ashCloudHeatMultiplier` 3,0) en een publieke getter. De modifiers zelf bouwt C (`contract.md` §5), zodat A en C in golf 1 niet op elkaar wachten.
3. **Lavabommen (server):** tijdens een uitbarsting vallen vanilla `SmallFireball`s op willekeurige plekken rond spelers in ring 3 onder open lucht, met een hogere dichtheid in `#heat_peaks`.
   - Vuur volgt `mobGriefing`; geen explosie.
   - Hoeveelheid uit de config, met een cap per speler per minuut.
   - Alle clients zien dezelfde entities.
4. **Client** (`client/weather/`):
   - `AshRainRenderer`: fog dichter en grijs, `ash_flake`-particles boven de heightmap, geluid `weather.ring_3.ash_rain`;
   - `EruptionRenderer`: rode lucht en fog, gerommel, lichte camera-shake;
   - `AshCloudRenderer`: zeer donker, fog naar ~16, vonken.
   - Alles × sterkte × blootstelling (`SkyExposure`).
   - **Lucht- en neerslagkleur:** NeoForge 1.21.1 heeft hier geen event. Kies een client-aanpak (mixin op `ClientLevel#getSkyColor` of een `DimensionSpecialEffects`-wrapper) en leg het vast in de delta; dit is de eerste weer-change die het nodig heeft (§1.6).
5. **Particles** `ash_flake`, `ember` registreren (`ModParticleTypes` + provider); sprites van T (tot dan tijdelijk een bestaande sprite, niet gecommit).
6. **Sound events** `weather.ring_3.*` registreren. Audio: vanilla-verwijzingen in golf 1 (zoals de zandstorm-wind); eigen synthese in golf 2 (`Ring3Sounds.java`).

## Afhankelijk van open vragen
- *V6* (aswolk-frequentie en -keten).

## Client/server-impact
- **Server:** staat, cyclus, modifiers, bommen; geen nieuwe payloads (de ringweerstaat wordt al volledig gesynct).
- **Client:** renderers, fog/lucht, particles, geluid, camera-shake alleen in `client/` (`ClientIsolationTest`).
- Twee clients moeten dezelfde staat en dezelfde bommen zien; particles en shake mogen per client verschillen.

## Tests
- **B**, **D**, **S** met 2 clients:
  - `/ca weather 3 ash_rain|eruption|ash_cloud`;
  - bommen zichtbaar op beide clients;
  - `mobGriefing false` geeft geen vuur;
  - onder een dak geen effect;
  - de grens 2→3 en 3→4 mengt;
  - deeltjes-instelling Minimal;
  - FPS.
- **W-checklist** voor 4b.

## Bij afronding
`waves.md` §4.
