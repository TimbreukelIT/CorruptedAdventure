# Change 13 — Stille zones in grotten en valere kleuren aan de rand van ring 1

**Scope: `atmosphere` — change 13**

**Doel:** ring 1 voelt richting de rand steeds minder pluis, zonder events: in grotten valt het ambient geluid op sommige plekken weg of klinken vreemde grotgeluiden vaker, en aan de rand (overgangsbiome `withered_expanse`) worden lucht, fog en water subtiel valer. Alles **data-driven via biome-effecten** (`mood_sound`, kleuren); geen gameplaylogica, geen netwerk, geen client-code.

---

## Afhankelijkheden en aannames

- Changes 0–7 zijn klaar (zie `CODEBASE.md`).
- **Change 8** heeft de ring 1-kopieën van de vanilla biomes in datagen gebouwd met `OverworldBiomes` (aanname: één bootstrap-class, bijv. `worldgen/biome/Ring1Biomes`, die per kopie `context.register(key, OverworldBiomes.xxx(…))` doet). Onder de grond zijn dat de oppervlaktebiomes (depth 0/1) en de grotbiomes (dripstone caves, lush caves, deep dark) van vanilla's multi-noise.
- **Change 11** heeft `withered_expanse` gebouwd als overgangsbiome 1→2 (zone ~128 blokken rond de verruisde grens, hele kolom, dus ook in grotten) met al **vaal gras/blad** (`grass_color`/`foliage_color`). Change 13 stemt de overige kleuren daarop af en voegt het geluid toe; het vale gras zelf blijft van change 11.
- Change 12 (horror-features) raakt deze change niet.
- **Aanname "randbiome = overgangsbiome" (akkoord gebruiker, 2026-09-30):** `fase-1-wereld.md` (Ring 1, Terrein en sfeer) zegt "in de buitenste zone subtiel valer (via het randbiome/overgangsbiome)". Ik lees dat als `withered_expanse`; er komt geen apart randbiome en geen extra biome in ring 1 (dat zou `worldgen` zijn).
- **Geen client-ringdata:** de client kent de seed en de ringgrenzen pas na change 18. Daarom geen client-side kleur-resolver of geluidslogica op basis van `ringAt`; "richting de rand" komt uitsluitend uit welk biome de client op zijn positie heeft (dat synct vanilla al mee in de chunkdata).
- Biome-effecten staan vast in de datapack-registry (datagen). Waarden zijn dus constanten in de bootstrap, niet in config.

---

## Hoe het werkt (achtergrond)

**Stille zones = `mood_sound` per biome.**
- Vanilla speelt grotgeluiden via `BiomeAmbientSoundsHandler` (client, per speler): elke tick een willekeurig blok binnen `block_search_extent`; is dat lucht zonder hemellicht, dan loopt een "moodiness"-teller op met ~1/`tick_delay` per tick (minder bij bloklicht). Bij 1 speelt het `sound`-event op `offset` blokken afstand. Vanilla: `ambient.cave`, `tick_delay` 6000 (~5 min volledig donker), extent 8, offset 2.
- Het werkt alleen in donkere open ruimte, dus in de praktijk **alleen in grotten**: een biome zonder `mood_sound` is aan het oppervlak niet anders, maar in de grotten eronder valt het grotgeluid volledig weg.
- Biomes zijn 3D (kwartblokken van 4×4×4) en worden meegestuurd met de chunk: de client kiest het biome op zijn eigen positie. Zones zijn daardoor zo groot als de biome-vlekken onder de grond, en in multiplayer voor iedereen gelijk (per seed deterministisch).
- **"Vaker richting de rand":** `withered_expanse` ligt als hele kolom rond de grens, dus in de grotten daaronder klinken de vreemde geluiden het vaakst. Binnen ring 1 is er geen radiale gradiënt (de ring 1-biomeverdeling is klimaat, niet afstand).

**Valere kleuren = effecten van `withered_expanse`.**
- `sky_color`, `fog_color`, `water_color`, `water_fog_color` van `withered_expanse` worden ontzadigd t.o.v. de vanilla-waarden van ring 1 (grijzer, iets lichter).
- De overgang is de client-side biome-blending van vanilla: kleuren (gras, blad, water) mengen over de ingestelde "Biome Blend"-straal, lucht en fog via vanilla's sampler over enkele kwartblokken (~10–20 blokken). Geen harde lijn, wel een relatief korte overgang.

---

## Plan

1. **Sound event** — `registry/ModSoundEvents.java` (nieuw): `DeferredRegister<SoundEvent>` met `ambient.ring_1.strange_cave` (`SoundEvent.createVariableRangeEvent`). Registreren in `CorruptedAdventure` (common: de server moet de id kennen, anders laadt de biome-JSON niet).
2. **Sound definitions (datagen)** — `datagen/ModSoundDefinitionsProvider.java` (nieuw, NeoForge `SoundDefinitionsProvider`), aanhaken in `DataGenerators` (client-assets-provider). Besloten (vraag 3, A): entries van type `EVENT` die naar vanilla-events wijzen (`minecraft:ambient.cave`) met lagere pitch (0,5–0,7) en iets lager volume, zodat ze herkenbaar maar "fout" klinken. Subtitle `subtitles.corruptedadventure.ambient.ring_1.strange_cave` ("Something stirs in the dark" / "Er beweegt iets in het donker") in `EnglishLanguageProvider` en `DutchLanguageProvider`.
3. **Ambience-helper** — `worldgen/biome/BiomeAmbience.java` (nieuw, pure functies, herbruikbaar voor de sfeer-changes van ring 2–5):
   - `withMood(Biome, Optional<AmbientMoodSettings>)` en `withColors(Biome, …)`: bouwt een biome opnieuw met NeoForge's `BiomeSpecialEffectsBuilder.copyOf(…)` en `ClimateSettingsBuilder`/`getModifiedClimateSettings()`, zodat features, carvers, spawns, spawn-costs, temperatuur-modifier en neerslag exact blijven.
   - `desaturate(int rgb, float amount)`: menging naar de grijswaarde, zodat de vale palette reproduceerbaar is (geen losse magische kleurcodes).
   - Profielen als constanten: `SILENT` (geen `mood_sound`) en `STRANGE` (event uit stap 1, `tick_delay` ~1.200–2.400, extent 12, offset 3 — definitief na de runClient-check).
4. **Ring 1-kopieën** — in de bootstrap van change 8 (besloten, vraag 1 A): `dark_forest`, `old_growth_pine_taiga`, `old_growth_spruce_taiga` en `dripstone_caves` door `BiomeAmbience` met profiel `SILENT` (per key een profiel in een kleine map). Alle overige kopieën ongewijzigd vanilla, ook deep dark (eigen geluidsontwerp) en lush caves.
5. **`withered_expanse`** — in de bootstrap van change 11: profiel `STRANGE` (sterkste variant) + vale lucht/fog/water via `desaturate` (startwaarde ~35–40 %, afgestemd op het vale gras van change 11). Gras/blad-kleur niet aanraken.
6. **Muziek** — ongewijzigd vanilla (besloten, vraag 2 A); `music` van de biomes niet aanraken.
7. **Unit test** — `src/test/.../worldgen/biome/BiomeAmbienceTest.java`: `withMood`/`withColors` veranderen alleen het bedoelde effect (generation settings, mob settings en climate blijven gelijk); `desaturate(x, 0)` = x, `desaturate(x, 1)` = grijs.
8. **`runData`** en de gegenereerde biomes + `sounds.json` + lang committen.
9. **Documentatie** — `CODEBASE.md` (nieuwe classes, `ModSoundEvents`, `sounds.json`-provider, registry-tabel "Overige": sound event), `fase-1-wereld.md` ring 1 (Grotten: uitwerking stille zones; Terrein en sfeer: randbiome = `withered_expanse`), beslissingenlog (drie regels, 2026-09-30, `atmosphere`: verdeling stil/vreemd per biome; muziek blijft vanilla; vreemde geluiden = vanilla-samples via eigen sound event), backlog (zie Risico's).

Bestanden: nieuw `registry/ModSoundEvents.java`, `worldgen/biome/BiomeAmbience.java`, `datagen/ModSoundDefinitionsProvider.java`, `BiomeAmbienceTest.java`; gewijzigd `CorruptedAdventure.java`, `DataGenerators.java`, de bootstraps van change 8 en 11, beide lang-providers, docs.

---

## Client/server-impact

- **Server:** registreert alleen het sound event (common `DeferredRegister`) en laadt de biome-JSON uit de datapack. Geen tick-handler, geen logica.
- **Client:** geen eigen Java-code. Vanilla leest de biome-effecten (via de vanilla registry-sync bij het joinen) en `sounds.json` (client-asset). Geen `client/`-class nodig, dus niets dat een dedicated server kan laden.
- **Netwerk:** geen eigen payloads; biomes en de sound-event-registry synct vanilla/NeoForge al.
- **Multiplayer:** zones zijn per biome en dus voor alle spelers gelijk; wanneer een geluid klinkt verschilt per speler (vanilla-gedrag, client-side toeval, bewust niet gesynct).

---

## Tests

- **B** — `./gradlew build` (incl. `BiomeAmbienceTest`).
- **D** — `./gradlew runData` slaagt; output gecommit (niet in de rij van het bouwplan, wel verplicht bij datagen-wijzigingen).
- **S** — `tools/server-smoke-test.sh`: server start met de nieuwe biome-JSON (sound-event-id bekend op de server). Extra t.o.v. de rij, omdat een ontbrekende registry-entry pas op een dedicated server zou opvallen.
- **runClient** — checklist (nieuwe wereld, preset `corrupted_world`, seed `20260930`, ondertitels aan):
  - [ ] `/playsound corruptedadventure:ambient.ring_1.strange_cave ambient @s` speelt een hoorbaar vervormd grotgeluid met de eigen ondertitel (NL en EN).
  - [ ] `/ca tpboundary 1`, afdalen in een grot onder `withered_expanse` (F3: biome), fakkels uit, in het donker wachten: vreemd grotgeluid binnen ~1–2 min, herhaaldelijk.
  - [ ] Een stil biome (dark forest, old growth taiga of dripstone caves; F3 in de grot): ~10 min in het donker zonder grotgeluid. Ter vergelijking een vanilla-kopie (bijv. plains-grot): normaal ~1× per 5 min. *(Traag om te testen; eventueel `/tick rate` hoger zetten — niet geverifieerd of vanilla's client-ambient daarop meeloopt.)*
  - [ ] Aan het oppervlak geen verschil in geluid tussen de biomes.
  - [ ] Bij `/ca tpboundary 1`: lucht, fog en water zichtbaar valer dan midden in ring 1 (`/ca tpring 1`); overgang zonder harde lijn; screenshot van beide voor de afstemming met de gebruiker.
  - [ ] Idem bij regen/onweer en 's nachts: geen vreemde kleur (te donker/te wit).
  - [ ] Deep dark, lush caves en de Nether ongewijzigd.

---

## Risico's

- **Rebouwen van vanilla-biomes:** een vergeten veld (temperatuur-modifier van frozen ocean, spawn-costs, carvers) verandert de wereld stil. Mitigatie: NeoForge-builders `copyOf` + de unit test.
- **`SoundDefinitionsProvider`-validatie:** entries van type `SOUND` die naar vanilla-`.ogg`'s wijzen, vereisen dat de vanilla-client-assets in de `runData`-resources zitten; entries van type `EVENT` (naar `minecraft:ambient.cave`) vermijden dat. Faalt de validatie, dan type `EVENT` gebruiken.
- **Stilte valt nauwelijks op:** vanilla-grotten zijn al stil op een grotgeluid per ~5 min na. Het "wegvallen" merk je pas na lang verblijf; de vreemde geluiden aan de rand dragen het effect. Eerlijk melden bij de runClient-check.
- **Geen radiale gradiënt:** zonder client-ringdata (change 18) is "vaker richting de rand" alleen het verschil binnen/buiten `withered_expanse`, en valer alleen in die ~128 blokken. Voorstel voor de backlog: `[atmosphere]` optionele client-side gradiënt (kleur en geluid) over de buitenste ~300 blokken van ring 1 op basis van gesyncte ringdata, pas ná change 18.
- **Afhankelijk van de vorm van change 8 en 11:** zitten de bootstraps anders in elkaar, dan verschuift alleen waar `BiomeAmbience` wordt aangeroepen.
- **Mood werkt niet in water:** onderwatergrotten krijgen geen mood-geluid (vanilla-gedrag).

---

## Open ontwerpvragen

### 1. Welke ring 1-biomes krijgen welk grotgeluid?
- **A (aanbevolen):** `withered_expanse` sterk vreemd (elke ~1–2 min in het donker); een handvol "donkere" kopieën **stil** (dark forest, old growth pine/spruce taiga, dripstone caves); de rest vanilla. — Pure data, duidelijk contrast, onrust concentreert zich aan de rand; zones zo groot als die biomes.
- **B:** alleen `withered_expanse` vreemd, alle ring 1-kopieën vanilla. — Minste werk, maar "stille zones" binnen ring 1 bestaan dan niet.
- **C:** alle ring 1-kopieën iets vaker vreemd (bijv. elke ~4 min), `withered_expanse` sterk; geen stille biomes. — Overal lichte onrust, minder contrast; stilte als effect vervalt.
- **D:** echte kleine zones via een extra grotbiome in ring 1 (bijv. `hushed_hollows`, alleen op diepte). — Meest "plekken"-gevoel, maar dat is `worldgen` (multi-noise, ores/features, feature-volgorde): eerst een aparte worldgen-change, change 13 wordt groter.

Antwoord: **A** (2026-09-30) — `withered_expanse` sterk vreemd; dark forest, old growth pine/spruce taiga en dripstone caves stil; overige kopieën (incl. deep dark en lush caves) vanilla.

### 2. Valt in stille zones ook de muziek weg?
- **A (aanbevolen):** nee, muziek blijft vanilla. — Geen bijwerkingen; stilte = alleen geen grotgeluid.
- **B:** ja, de stille biomes krijgen een "lege" muziek-event (`replace_current_music`). — Sterker horroreffect, maar biome-muziek geldt voor de hele kolom: ook aan het oppervlak van die biomes geen muziek meer.

Antwoord: **A** (2026-09-30) — muziek blijft vanilla.

### 3. Waar komen de vreemde grotgeluiden vandaan?
- **A (aanbevolen):** eigen sound event met vanilla-samples (grotgeluiden, lager gepitcht en zachter) via `sounds.json`. — Geen nieuwe audio nodig, geen licentievragen; later te vervangen door eigen `.ogg`'s zonder de biomes aan te passen.
- **B:** eigen `.ogg`-bestanden (nu maken of aanleveren). — Unieker, maar er is nu geen audio; blokkeert de change tot de bestanden er zijn.
- **C:** gewoon vanilla `ambient.cave`, alleen vaker. — Nul werk, maar niet "vreemd"; spelers herkennen het direct.

Antwoord: **A** (2026-09-30) — eigen sound event met vanilla-samples (lager gepitcht, zachter) via `sounds.json`.
