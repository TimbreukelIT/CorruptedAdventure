# Fase 2 golf 5 — stroom L (hemelglaslens)

Branch `f2g5/lens` op main `d55ff491` (merge-base bij de start; review `/code-review low d55ff491..f2g5/lens`), n = 61.
Categorie `items` (+ client-rendering). Bron: `fase-2-items.md` §1.4, §7.2; `golf-5.md` rij L.

## Wat
- `sky_glass_lens` (EN "Sky Glass Lens", NL "Hemelglaslens"), stack 1, tab 4 (achteraan Nether-4).
- Verrekijkergedrag zonder mixin: `item/SkyGlassLensItem` erft van `SpyglassItem`. NeoForge patcht `Player.isScoping()`
  naar `canPerformAction(ItemAbilities.SPYGLASS_SCOPE)` en de spyglass-acties erven mee, dus vanilla doet zoom (FOV
  0,1 in first person), verborgen hand, tragere muis, arm-pose en de geluiden (`spyglass.use`/`stop_using`) zelf. De
  scope-overlay krijgt onze texture via `IClientItemExtensions.getScopeOverlayTexture` (client).
- Server (`item/lens/`): zolang een speler door de lens kijkt (`isUsingItem` + de lens) meteen en daarna elke 20 ticks
  een scan rond hem; payload `lens_scan` alleen naar die speler; stopt hij, dan één lege payload. Alleen geladen chunks
  (`getChunkNow`), nooit een chunk-load.
  - **Glitchblokken:** elk `glitch_block` binnen een bol van 32 rond het oog; per sectie eerst de palette
    (`maybeHas`), alleen secties met een glitchblok worden doorlopen. Elke dimensie.
  - **Void-gaten onder de grond:** kolommen binnen 32 (horizontaal) in de binnenband van ring 5 waar het onderste blok
    (`min_y`) lucht is maar er grond boven staat (heightmap): de gaten van de void-grotten (93) en alles wat een speler
    over de void bouwt. Gemarkeerd op vloerhoogte (de vloer naast het gat). Uit blokken, dus een dichtgemaakt gat
    verdwijnt. Buitenband niet (daar zweeft alle grond boven de void).
  - **Ontbrekende chunks:** de schone ontbrekende chunks (94) puur uit de worldgen (`Ring5MissingChunks.missing`, per
    cel gememoriseerd, ≤ 25 chunks per scan): goedkoper en exact; een box van `min_y` tot de grond.
- Client (`client/item/`, `client/weather/GlitchVisuals`): omlijning via `RenderLevelStageEvent` (`AFTER_LEVEL`, eigen
  lijnen-RenderType zonder dieptetest, door de grond heen) zolang je kijkt; blokken als buitenrand van hun vereniging.
  Glitch-fog (stotter + violet) en de schermvervorming (ruis, scanlines, hotbar-hapering van storm en void-meter)
  gedempt via een gedeelde factor.

Client/server: de server bepaalt wat zichtbaar is en stuurt het alleen naar de kijker; de client tekent en dempt
alleen. Protocol `"10"` → `"11"` (nieuwe payload).

## Voortgang
- 2026-10-28 16:28 start; setup n = 61; ontwerp gelezen, plan.
- 16:40 code af; build (incl. unit tests) groen (7 min, slot-wachttijd).
- 17:45 commit `97f21903`; merge main (H pakket 1 + C, conflicten in `ModItems`, lang, item-modellen, `ModBlocksTest`
  opgelost: lens achteraan Nether-4 na `sky_light_draught`), recept `LensRecipes` (` G `/` T `/` T `); runData groen,
  output commit `99ee09a7`; build 2 + `/code-review low d55ff491..f2g5/lens` gestart.
- 17:54 build 2 groen (incl. unit tests, `ClientIsolationTest`), GameTests 26/26 groen, server-smoke n = 61 groen
  (Done in 8 s, geen client-class-fouten); server gestopt, slots vrij.
- 18:15 review (low) zonder bevindingen. Merge main (T: definitieve lens-PNG's, `--theirs`) `46d0c6f4`; runData schoon,
  build 3 groen.

## Voorlopig (bevestigen bij de teststop)
- Kleuren omlijning: glitchblokken violet `#B15CFF`, void-gaten cyaan `#4FE3F0`, ontbrekende chunks bleekwit
  `#EDEBFF`; vanilla-lijndikte, geen dieptetest (door de grond heen), geen pulseren.
- Dempen: glitch-fog en schermvervorming naar 25 % zolang je kijkt, in/uit over ~5 ticks.
- Niet gedempt: de waas en flitsen van de glitchstorm zelf (weer, geen glitch-fog).
- Lens in de hand: plat item-model (vanilla's 3D-verrekijker hangt aan `Items.SPYGLASS`); T kan een eigen model maken.
- Plafonds per scan: 512 glitchblokken, 256 gatkolommen, 16 chunks (de dichtstbijzijnde).
- Receptvorm: verrekijkervorm (` G ` / ` T ` / ` T `).

## W-checklist (INT)
- Rechtsklik ingedrukt: zoom, eigen scope-overlay, verrekijkergeluiden; loslaten: weg. Derde persoon: arm-pose.
- In ring 5 binnenband: glitchblokken in de muur omlijnd (door de grond heen), void-gaten van een void-grot op
  vloerhoogte, een ontbrekende chunk als box; buiten ring 5 alleen glitchblokken.
- Glitch-fog en ruis zichtbaar zwakker zolang je kijkt.
- Twee spelers: alleen de kijker ziet de omlijning; de ander niets.
- Dedicated server start zonder client-class-fouten.

## CODEBASE-delta
- **Mappenstructuur / Belangrijke classes (L, `items`):** `item/SkyGlassLensItem` (`SpyglassItem`, stack 1;
  `lookingThrough(entity)`); `item/lens/LensScan` (scan rond het oog: `glitches` (secties via `maybeHas`, bol 32),
  `holes(Ground, …)` (open onderkant + grond erboven, binnenband ring 5 per chunk beslist via hoeken + midden, groepen
  4-verbonden, vloer = laagste van ≤ 4 monsters ernaast), `missingChunks` (`Ring5MissingChunks.missing`), plafonds);
  `item/lens/LensWatch` (`PlayerTickEvent.Post`: meteen + elke 20 ticks, lege payload bij stoppen, opgeruimd bij
  uitloggen). Client: `client/item/LensView` (scope-texture via `IClientItemExtensions`, demping per tick, registratie),
  `LensMarkers` (laatste scan als lijnstukken, per level), `LensOutline` (vouwranden van een blokvereniging, box),
  `LensRenderer` (`AFTER_LEVEL`, eigen RenderType `corruptedadventure_lens_lines`: vanilla-lijnen zonder dieptetest);
  `client/network/LensScanHandler`; `client/weather/GlitchVisuals` (schaal 0–1 op glitch-fog (`Ring5Fog.screenEffects`)
  en `GlitchStormOverlay` (ruis × schaal, hapering-kans × schaal)).
- **Registries / Items:** `sky_glass_lens` (Nether-4, tab 4, achteraan; `SkyGlassLensItem`), recept na H pakket 1.
- **Netwerk:** `corruptedadventure:lens_scan` (`LensScanPayload`: glitchblokken ≤ 512, gatblokken ≤ 256 als `BlockPos`,
  ontbrekende chunks ≤ 16 als (cx, cz, bodem, top)) server → alleen de kijkende speler, meteen + elke 20 ticks zolang
  hij kijkt, één lege bij stoppen. **`PROTOCOL_VERSION` `"10"` → `"11"`** (nieuwe payload).
- **Databestanden:** `textures/item/sky_glass_lens.png`, `textures/misc/sky_glass_lens_scope.png` (placeholders L, T
  vervangt); lang `LensText` (EN/NL); item-model `basicItem` in `ModItemModelProvider`.
- **Tests:** `item/lens/LensScanTest`, `item/SkyGlassLensItemTest`, `network/LensScanPayloadTest`,
  `client/item/LensOutlineTest`; `ModBlocksTest` (Nether-4-volgorde) filtert de lens.

## Beslissingen
- 2026-10-28 | items | Hemelglaslens: verrekijkergedrag via `SpyglassItem` (NeoForge `SPYGLASS_SCOPE`), geen mixin;
  void-gaten onder de grond = binnenband-kolommen met lucht op `min_y` en grond erboven (uit blokken); ontbrekende
  chunks puur uit `Ring5MissingChunks`; omlijning door de grond heen | Goedkoop, exact, multiplayer-veilig

## Backlog
- [worldgen] Lees-accessor voor de weggevallen chunk van een corrupte plek (`Ring5CorruptSpots.Spot.fallen`,
  package-private), zodat de lens ook die als ontbrekende chunk omlijnt.
