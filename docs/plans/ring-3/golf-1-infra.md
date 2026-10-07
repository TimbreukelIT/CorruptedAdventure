# Golf 1 — stroom I (infra): 37d terreinkaart-debugcommando

Scope: `infra` — werkpakket 37d; branch `ring3/infra-g1`. Merget als eerste in integratie 1.

**Doel:** de gebruiker en de stromen kunnen het ring 3-terrein beoordelen zonder te vliegen. Het commando `/ca terrainmap` schrijft PNG's (hoogte, biome, verticale doorsnede) uit de echte density functions en biome-source. Teststop 4a begint met deze kaarten (backlog r. 71; werkwijze change 23 in `CODEBASE.md`, valkuil "Terreinkaarten zonder chunks").

## Werk
1. `/ca terrainmap <x> <z> <straal> [schaal]` en `/ca terrainmap section <x1> <z1> <x2> <z2>` (alleen OP's, vaste Engelse tekst zoals de andere `/ca`-commando's).
   - Draait op een eigen thread en raakt geen chunks: named DF's via `registryAccess`, `mapAll`-bedrading zoals in de valkuil beschreven.
   - Biome via `getNoiseBiome(…, randomState.sampler())`.
2. **Uitvoer:** `<wereld>/corruptedadventure/maps/<naam>.png`.
   - Hoogtekaart met kleurverloop en contourlijnen elke 16 blokken, Y 192 (wolken) gemarkeerd.
   - Biomekaart met vaste kleuren per biome (ring 3 en Nether-3 erbij) en de ringgrenzen.
   - Doorsnede: steen/lucht/vloeistof, met de eilanden zichtbaar.
3. Voortgang en tijd per kolom in de chat; een cap op de grootte (bijv. 2.048 × 2.048 bij schaal 4), zodat een server niet vastloopt (`max-tick-time` niet nodig: niet op de main thread).
4. Ook voor de Nether (`/execute in minecraft:the_nether run ca terrainmap …`).

## Client/server-impact
Alleen server (commando + bestand); geen client-classes.

## Tests
- **B**, **S** (RCON): kaart van ring 2 (bekend terrein) + `dry_riverbeds`; tijd per kolom; een tweede run geeft een identieke PNG (md5).
- Een korte uitleg in de delta voor `CODEBASE.md`.

## Bij afronding
`waves.md` §4.

## Voortgang
Voortgang: 20:35 branch `ring3/infra-g1` op 8e0c637 (de worktree stond op 17c9eb4; schone branch met `reset --hard` verzet)
Voortgang: 20:52 code 37d geschreven (`command/terrainmap/*`) + unit tests; eerste build gestart
Voortgang: 21:43 build groen (na wachten op een JVM-slot sinds 20:55; 1 testfix in `ColumnScan.top`: laatste grove stap haalde `minY` niet)
Voortgang: 21:58 S: kaarten ring 2 / ring 3-stub / Nether, doorsnedes vergeleken met echte chunks (47/47 binnen 1 blok), md5 gelijk bij herhaling, cancel en cap getest; kolomcache voor `cache_2d`/`flat_cache` toegevoegd (1146 → ~520 µs per kolom bij schaal 4)
Voortgang: 22:05 build groen (441+ tests, incl. 3 nieuwe testklassen, `ClientIsolationTest`); S op de eindcode: `dry_riverbeds`-kaart 2× md5 gelijk, doorsnede 3300→3700 21/21 binnen 1 blok; server gestopt (RCON `stop`)

## Status bij onderbreking (2026-10-06 22:07) — achterhaald, zie Voortgang en Review
- **Gecommit:** niets (branch `ring3/infra-g1` staat nog op 8e0c637).
- **Ongecommit klaar op schijf:** `command/terrainmap/*` (13 klassen), `CaCommand` (hook + stop-listener), 3 testklassen onder `src/test/.../command/terrainmap/`, dit plan. Build groen, S gedaan.
- **Ontbreekt nog:** `/code-review` (`medium infra-g1 37d`) op de ongecommitte diff, bevindingen afhandelen, daarna commit `infra: add /ca terrainmap debug maps` (paden: `src/main/java/nl/timtoys/corruptedadventure/command/`, `src/test/java/nl/timtoys/corruptedadventure/command/terrainmap/`, `docs/plans/ring-3/golf-1-infra.md`). Optioneel: stop-tijdens-run-test (server `stop` terwijl een kaart loopt; de code annuleert en wacht ≤ 10 s).
- **Opgeruimd:** eigen server gestopt via RCON, geen eigen JVM's meer, JVM-slot vrijgegeven, RCON uit in `run/server.properties`. Hulpscripts (niet gecommit) in `build/infra-g1/` (`rcon.py`, `verify_section.py`, `server.sh`, `slot.sh`).
- Voorbeeld-PNG's: `run/infra-g1-terrainmap/corruptedadventure/maps/` (o.a. `top_overworld_2500_0_r1024_s4_height.png`, `_biome.png`, `section_overworld_3300_0_3700_0.png`, `top_the_nether_500_0_r384_s2_height.png`).

Voortgang: 08:16 (2026-10-07) hervat; stop-tijdens-kaart getest: `stop` tijdens een 2048²-run → "Terrain map cancelled", server stopt netjes binnen ~1 s, geen fouten
Voortgang: 08:19 `/code-review medium` binnen (10 bevindingen), afgehandeld (zie "Review"); rebuild + korte S
Voortgang: 09:05 build groen na de reviewfixes (compile + alle tests opnieuw); S: `dry_riverbeds`-kaart md5 gelijk aan vóór de fixes, doorsnedes overworld + Nether ok; server gestopt, RCON uit, slot vrij; commit

## Review (`/code-review medium`, 2026-10-07)
1. Een falende worker liet de andere doorwerken → `failed`-vlag, de rest stopt bij de volgende eenheid. **Opgelost.**
2. `Error` in render/write (OOM, JRE zonder AWT) gaf geen melding → coördinator vangt `Throwable`, logt en meldt. **Opgelost.**
3. Vloeistoftype kwam van het blok boven de grond (water boven de lavalaag als lava getekend) → type van het oppervlak (`fluidTop`). **Opgelost.**
4. Water onder een overhang (boog, hoodoo, eiland boven een meer) wordt in de doorsnede als grot getekend. **Vastgelegd** als bekende beperking (javadoc `SectionView.classify`, valkuil hieronder): de echte aquifer is zonder `NoiseChunk` niet na te bootsen; de hoogte- en grotvorm klopt wel.
5. Annuleren tijdens render/write werd genegeerd → check vóór render en per bestand; melding "cancelled; already written: …". **Opgelost.**
6. Na een time-out van de join bij het stoppen bleef `RUNNING` staan → wordt nu altijd vrijgegeven. **Opgelost.**
7. Dubbele regels (grondregel, ringcode, `hasRings`) → `TerrainSampler.ground(source, solid, step)`, `ringCode`, `hasRings`, `NO_RING`. **Opgelost.**
8./9. `CODEBASE.md`, beslissingenlog en backlog niet bijgewerkt → **bewust**: `waves.md` §4.8 verbiedt stromen die bestanden; de delta-secties hieronder voegt INT samen.
10. Memo-map hashte hele deelbomen (records) → `IdentityHashMap`. **Opgelost.**

## Testresultaten
- **B:** `./gradlew build` groen; nieuwe tests `ColumnScanTest`, `TerrainMapGeometryTest`, `TerrainMapRenderTest`.
- **S** (eigen server, poort 25577 / RCON 25587, seed 20260930, wereld `infra-g1-terrainmap`):
  - Doorsnedes vergeleken met echte chunks (forceload + `execute if block`, bovenste niet-vervangbare blok): dune_sea 2400→2600 21/21, canyonlands 1700→1900 (z −800, incl. hoodoo op Y 119) 26/26, `dry_riverbeds` 3300→3700 21/21 binnen 1 blok (verschil ≤ 1: geen celinterpolatie + surface).
  - Herhaalde run geeft een identieke PNG (md5) voor top-view overworld, Nether en doorsnede.
  - Tijd (4 workers, machine druk met 6+ andere JVM's): ring 2 schaal 4 ~520 µs per kolom per thread (512² in 34 s), schaal 1 ~130 µs (buren delen `ColumnTable`/`CellInterpolation`-caches), ring 3-stub 184 µs (1024² in 49 s), Nether schaal 2 71 µs, doorsnede 1,3 ms per kolom (384 Y). Een kaart op de cap (2048²) duurt dus ~3–9 min.
  - Serverbelasting: `tick query` tijdens een 1024²-run 0,2–0,3 ms per tick (P99 ≤ 1,0 ms), gelijk aan vooraf (zonder spelers).
  - Cap (`4100 4` → fout), schaal 0 (brigadier-fout), tweede kaart tijdens een run (fout), `cancel` (stopt binnen ~2 s) en `cancel` zonder run (fout) getest.

## CODEBASE-delta
Onder `command/`:
```
    terrainmap/                ✅ `/ca terrainmap` (37d): PNG-kaarten uit de density functions en biome source, zonder chunks, op eigen threads
      TerrainMapCommand.java   ✅ `/ca terrainmap <center> <radius> [scale]` (hoogte + biome), `section <from> <to>`, `cancel`; coördinaten in de dimensie van de bron (`/execute in minecraft:the_nether run ca terrainmap …`)
      TerrainMapJob.java       ✅ één kaart tegelijk: coördinator + ¼ van de cores (1–4) workers met lage prioriteit; voortgang per tiende (na ≥ 5 s) in de chat + log; schrijft `<wereld>/corruptedadventure/maps/*.png` (tmp + move); `ServerStoppingEvent` annuleert en wacht ≤ 10 s
      TerrainSource.java       ✅ record: generator, random state, Y-bereik, celhoogte, plafond, aquifers, zeeniveau, `RingLayout` (null buiten een ringwereld); op de server-thread gevangen
      TerrainSampler.java      ✅ per thread: `finalDensity` van de random state met de markers eruit (HolderHolder → waarde, `cache_2d` → kolomcache, `flat_cache` → quart-kolom op Y 0, overige markers → `wrapped`); `ground` (bovenste vaste blok, of onder een plafond de bodem van de hoogste opening), `fluid` (global fluid picker), `biome` (`getNoiseBiome` met `randomState.sampler()`), `ring`
      ColumnScan.java          ✅ puur: `top` (grove stap = celhoogte, dan per blok) en `floorOfTallestOpening` (plafond-dimensies, stap 4)
      MapArea.java / SectionLine.java ✅ pure geometrie + caps (2048 px per zijde, schaal 1–64; doorsnede ≤ 2048 kolommen, ≤ 16.384 blokken)
      MapWork.java, TopViewWork.java, SectionWork.java ✅ werk in eenheden (rij / kolom) voor de workers; daarna renderen
      TopView.java / SectionView.java ✅ pure samples; `SectionView.classify` (steen/lucht boven/lucht onder/water/lava)
      TopViewRenderer.java / SectionRenderer.java / LegendPanel.java / MapPalette.java / BiomeIds.java ✅ AWT-rendering met legenda rechts (tekst valt weg zonder fonts); vaste kleuren voor eigen biomes, stabiele kleur uit de id voor vanilla
```
Tests: `command/terrainmap/ColumnScanTest`, `TerrainMapGeometryTest`, `TerrainMapRenderTest` ✅.

Commando-tabel (`/ca …`): `/ca terrainmap <x z> <radius> [scale]` → `top_<dim>_<x>_<z>_r<radius>_s<scale>_height.png` en `_biome.png`; `/ca terrainmap section <x1 z1> <x2 z2>` → `section_<dim>_<x1>_<z1>_<x2>_<z2>.png`; `/ca terrainmap cancel`.

Valkuil "Terreinkaarten zonder chunks" vervangen door:
- **Terreinkaarten (`/ca terrainmap`, 37d):** rekent `final_density` per blok zonder `NoiseChunk`: geen celinterpolatie (randen ≤ 1 blok anders dan de echte wereld, gemeten), geen lokale aquifers (grotten onder zeeniveau droog getekend, behalve de lavalaag; ook open water onder een overhang telt als grot), geen structures, geen carvers/surface/features. `cache_2d`/`flat_cache` moeten als kolomcache blijven (anders ~2× trager). Bij schaal > 1 delen kolommen de per-chunk-caches van onze DF's (`ColumnTable`, `CellInterpolation`) niet: ~4× duurder per kolom dan schaal 1. RCON ziet alleen de startregel; voortgang en "done" staan in de serverlog. Nether-top-view: de bodem van de hoogste opening per kolom; in Nether-ring 2 (grottenstelsel) geeft dat een gespikkeld beeld — gebruik daar de doorsnede.

## Beslissingen
- 2026-10-06 | infra | `/ca terrainmap` rekent uit `final_density` van de random state (markers eruit, `cache_2d`/`flat_cache` als kolomcache) i.p.v. chunks; 1–4 workers (¼ cores, lage prioriteit), één kaart tegelijk, cap 2048 px per zijde / 2048 doorsnedekolommen | snel, raakt geen wereld, server blijft vrij; gemeten ≤ 1 blok verschil met de echte terreinhoogte.
- 2026-10-06 | infra | Nether-hoogtekaart = bodem van de hoogste opening per kolom (plafond-dimensies) | het "bovenste blok" is daar het plafond.

## Backlog
- [infra] `/ca terrainmap`: optionele celinterpolatie (`interpolated` op 4×8×4 hoeken) en lokale aquifers als de kaarten te veel afwijken bij fijne vormen (zwevende eilanden < 8 blokken dik worden in de top-view gemist: grove stap 8). (bron: 37d)
