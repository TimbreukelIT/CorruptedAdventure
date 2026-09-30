# Change 15 — Zielkristal-erts

**Scope: `blocks` — change 15**

**Doel:** het sleutelmateriaal van Nether-ring 1 bestaat als blok, in twee varianten: `soul_crystal_ore` (netherrack met kristal) en `crimson_soul_crystal_ore` (karmozijnrood nylium met kristal). Met blockstates/models, placeholder-textures van eigen pixels, loot tables (Fortune + Silk Touch), mining-tags, XP-drop, zwak licht, lang en een plek in de creative tab. De drop is een minimaal placeholder-item `soul_crystal` (gebruik in fase 2). Plaatsing volgt in change 16 (`worldgen`: netherrack-aders + crimson nylium vervangen door de crimson-variant), definitieve textures in change 17 (`textures`).

Bron: `fase-1-wereld.md` §2 Ring 1 → Nether-ring 1 ("nieuw, zeldzaam erts in de crimson-bodem en netherrack, alleen in Nether-ring 1; nodig voor bewaker 1→2 (fase 5); gebruik in fase 2"), §1.4, §1.12, bouwplan rij 15. Antwoorden van de gebruiker (2026-09-30): zie "Ontwerpvragen" onderaan.

---

## Afhankelijkheden en aannames

- **Change 10 is gebouwd zoals gepland** (`docs/plans/change-10.md`) en levert de blok-infrastructuur. Aangenomen (volg de echte namen uit `CODEBASE.md` na change 10):
  - `registry/ModBlocks` (`DeferredRegister.Blocks`) met een helper die ook het `BlockItem` registreert; `registry/ModItems`; `registry/ModCreativeTabs` met één mod-tab;
  - datagen: `ModBlockStateProvider` (blockstates + blockmodels), item-models, block-loot (`BlockLootSubProvider`), block- en item-tags, lang (en + nl), aangehaakt in `DataGenerators`; de data-run draait met `--existing src/main/resources` (texturecheck);
  - het **texture-script met eigen pixels** in `tools/textures/` (bijv. `Ring1Textures.java`, single-file Java, alleen JDK, deterministisch). Deze change voegt daar tekenmethodes aan toe; er komen **geen vanilla-pixels** in de repo.
  - Een plek voor eigen `TagKey`s (bijv. `registry/ModTags`); bestaat die niet, dan maakt deze change hem aan.
- **Change 17** (`docs/plans/change-17.md`) maakt de textures definitief met dezelfde bestandsnamen en hetzelfde tweelaags-model (vanilla-basis als verwijzing + eigen overlay). Deze change legt dat model al aan, zodat change 17 alleen PNG's (en eventueel emissive/animatie) hoeft te wijzigen.
- Change 14 (biomes Nether-ring 1) is niet nodig; het erts is los van de wereld testbaar.
- Change 16 gebruikt de registry-namen `corruptedadventure:soul_crystal_ore` (in netherrack, `base_stone_nether`) en `corruptedadventure:crimson_soul_crystal_ore` (vervangt `minecraft:crimson_nylium`). Replace-regels horen bij change 16.
- Het drop-item valt onder `items` (fase 2). Volgens het precedent in de backlog (glitchscherf, zuiverkristal-scherf) registreren we het als **minimaal basis-item** en noteren dat in de backlog.

---

## Plan

1. **Blokken registreren** in `ModBlocks`, beide als `DropExperienceBlock(UniformInt.of(3, 7), props)` (geen eigen class):
   - `soul_crystal_ore` — properties als vanilla `nether_quartz_ore`: `mapColor(MapColor.NETHER)`, `instrument(BASEDRUM)`, `requiresCorrectToolForDrops()`, `strength(3.0F, 3.0F)`, `sound(SoundType.NETHER_ORE)`, plus `lightLevel(state -> 3)`.
   - `crimson_soul_crystal_ore` — zelfde, maar `mapColor(MapColor.CRIMSON_NYLIUM)` en `sound(SoundType.NYLIUM)`. Hardheid blijft 3,0 (het is erts, niet gewoon nylium). Geen nylium-gedrag: spreidt niet, geen bonemeal, wordt niet netherrack als er iets op ligt.
   - Licht 3: puur een blockstate-property; geen effect op mob-spawning in de Nether (`monster_spawn_block_light_limit` 15).
2. **Item registreren** in `ModItems`: `soul_crystal` — `new Item(new Item.Properties())`, verder niets (geen rarity, geen recipes).
3. **Creative tab:** beide ertsen en `soul_crystal` in de bestaande mod-tab, na de ring 1-blokken van change 10.
4. **Blockstates en models (datagen, `ModBlockStateProvider`)** — tweelaags-model, `render_type` `cutout` in de model-JSON (geen client-code):
   - `soul_crystal_ore`: element 1 = volle kubus met `minecraft:block/netherrack` op alle zijden (**verwijzing** naar de vanilla-texture, geen kopie); element 2 = volle kubus met `#overlay` = `corruptedadventure:block/soul_crystal_ore_overlay` op alle zijden, elk vlak met `cullface`. Zelfde techniek als vanilla's grasblok-overlay (twee even grote elementen, geen z-fighting). `particle` = netherrack.
   - `crimson_soul_crystal_ore`: element 1 als vanilla `crimson_nylium` (top `minecraft:block/crimson_nylium`, zijkanten `minecraft:block/crimson_nylium_side`, bodem `minecraft:block/netherrack`); element 2 = overlay met `corruptedadventure:block/crimson_soul_crystal_ore_top_overlay` op de bovenkant en `soul_crystal_ore_overlay` op zijkanten en bodem. `particle` = crimson nylium.
   - Eén herbruikbare helper in de provider (bijv. `oreWithOverlay(block, baseTextures, overlayTextures)`), zodat latere ringertsen hem hergebruiken.
   - Block-items: `simpleBlockItem`. Item-model `soul_crystal`: `basicItem`.
5. **Placeholder-textures met eigen pixels** — tekenmethodes in het texture-script uit change 10 (`tools/textures/…`), PNG's gecommit onder `src/main/resources/assets/corruptedadventure/textures/`:
   - `block/soul_crystal_ore_overlay.png` — 16×16, transparant (alpha alleen 0 of 255, cutout): 3–5 kleine kristalclusters in zielenblauw/cyaan (richting soul fire, ca. `#3FD8E0`, met een lichte en een schaduwtint), seeded en deterministisch.
   - `block/crimson_soul_crystal_ore_top_overlay.png` — idem, maar kristalpunten die tussen het nylium uitsteken (ander patroon, iets dichter), zodat de bovenkant herkenbaar is tussen gewoon crimson nylium.
   - `item/soul_crystal.png` — 16×16 kristal met facetten in dezelfde tinten.
   - Eerst het script draaien, dan `runData` (de model-provider controleert of de textures bestaan; vanilla-textures kent `ExistingFileHelper` zelf).
   - Placeholder-niveau; change 17 verfijnt dezelfde bestanden (en voegt eventueel animatie/emissive toe).
6. **Loot tables (datagen)**:
   - `soul_crystal_ore`: `createOreDrop(b, ModItems.SOUL_CRYSTAL.get())` — Silk Touch → het erts zelf; anders 1 `soul_crystal` met `ApplyBonusCount.addOreBonusCount` (Fortune III: 1–4) en `ApplyExplosionDecay`.
   - `crimson_soul_crystal_ore`: Silk Touch → `crimson_soul_crystal_ore`; anders identiek aan het netherrack-erts: alleen `soul_crystal` (Fortune); het blok wordt lucht (vraag 5 A).
   - XP loopt via `DropExperienceBlock`, niet via de loot table; Silk Touch geeft in 1.21 via de enchantment-component vanzelf 0 XP.
7. **Tags (datagen):**
   - Block, beide varianten: `minecraft:mineable/pickaxe`, `minecraft:needs_iron_tool`, `c:ores`, `c:ores/soul_crystal` (eigen `TagKey`), `c:ore_rates/singular`.
   - Block, alleen `soul_crystal_ore`: `c:ores_in_ground/netherrack`. De crimson-variant krijgt geen `ores_in_ground`-tag (er is geen conventietag voor nylium; andere mods behandelen hem dan niet ten onrechte als netherrack-erts).
   - **Niet** in `minecraft:nylium` (vraag 6 A): geen fungi/roots op de ertsplekken.
   - Item: `copy` van `c:ores`, `c:ores/soul_crystal`, `c:ores_in_ground/netherrack`, `c:ore_rates/singular`; `soul_crystal` in `c:gems` en `c:gems/soul_crystal`.
   - **Niet** in `minecraft:guarded_by_piglins` (dat is voor goud).
8. **Lang (datagen):**
   - en_us: `Soul Crystal Ore`, `Crimson Soul Crystal Ore`, `Soul Crystal`.
   - nl_nl: `Zielkristalerts`, `Karmozijnrood zielkristalerts`, `Zielkristal`.
9. `./gradlew runData`; gegenereerde output meecommitten.
10. **Documentatie:**
    - `CODEBASE.md`: beide blokken en het item in de registry-tabellen (Nether-ring 1; item: "placeholder, nut in fase 2"), eventuele `ModTags`, de overlay-helper in `ModBlockStateProvider`, nieuwe textures en tekenmethodes in het script, "Laatst bijgewerkt". Valkuil: tweelaags-ertsmodel = cutout, overlay alleen alpha 0/255.
    - `fase-1-wereld.md` §2 Ring 1 → Nether-ring 1: twee varianten (netherrack + crimson nylium), drop 1 zielkristal (Fortune, Silk Touch), ijzeren pickaxe, XP 3–7, licht 3, geen particles, antwoorden op vraag 5 en 6.
    - `02-beslissingenlog.md`: één regel per beslissing (vraag 1–6).
    - `docs/backlog.md`: `[items] Placeholder-item soul_crystal (drop van zielkristal-erts, change 15) als minimaal basis-item; nut, recipes (o.a. smelten van het Silk Touch-erts) en balans in fase 2. (bron: change 15)` — of de bestaande zielkristal-regel (regel 20) daarmee aanvullen.
11. Afsluiten met `/code-review`, bevindingen oplossen, commit `blocks: add soul crystal ores with loot and tags`.

**Niet in deze change:** plaatsing (change 16), definitieve textures/animatie/emissive (change 17), smelt-/craftrecipes en een opslagblok (fase 2 / `items`), gebruik voor bewaker 1→2 (fase 5), particles (besloten: geen).

---

## Client/server-impact

- Blokken en item zijn common registry-entries; NeoForge synct de registries bij het joinen. Geen netwerkcode, geen staat, geen SavedData, geen eigen block-classes.
- Models, textures en lang zijn resources; `render_type` staat in de model-JSON, dus geen client-registratie en geen code in `client/`.
- Licht is een blockstate-property die server en client allebei kennen; geen sync-werk.
- Het texture-script staat buiten `src/` en komt niet in de jar.

---

## Tests

Bouwplan: **B, D**. Aanbevolen extra, goedkoop: **S** (nieuwe registry-entries) en een loot-check via RCON.

Checklist (concept):
- [ ] Texture-script gedraaid; drie nieuwe PNG's aanwezig, overlays alleen alpha 0/255; geen vanilla-PNG's in `src/main/resources`.
- [ ] **B** — `./gradlew build` slaagt (incl. bestaande unit tests).
- [ ] **D** — `./gradlew runData` slaagt zonder "texture does not exist"; gegenereerd en gecommit:
  - blockstates, block-models (twee elementen, `render_type` cutout) en item-models voor `soul_crystal_ore`, `crimson_soul_crystal_ore`, `soul_crystal`;
  - `data/corruptedadventure/loot_table/blocks/{soul_crystal_ore,crimson_soul_crystal_ore}.json` met Silk Touch-tak, `apply_bonus` (ore_drops) en `explosion_decay`;
  - `data/minecraft/tags/block/mineable/pickaxe.json` en `…/needs_iron_tool.json` bevatten beide ertsen;
  - `data/c/tags/block/ores.json`, `ores/soul_crystal.json`, `ore_rates/singular.json` (beide), `ores_in_ground/netherrack.json` (alleen `soul_crystal_ore`) en de item-tegenhangers, plus `data/c/tags/item/gems.json` en `gems/soul_crystal.json`;
  - en_us en nl_nl bevatten de drie nieuwe keys.
- [ ] **S** (aanbevolen) — `tools/server-smoke-test.sh`: server start zonder fouten.
- [ ] Loot-check zonder client (RCON, zie `CODEBASE.md` valkuilen), per variant: `setblock` erts, dan `loot spawn ~ ~ ~ mine <pos> minecraft:iron_pickaxe` → 1 `soul_crystal` (ook bij de crimson-variant geen netherrack); met Fortune III → 1–4; met Silk Touch → de eigen variant. (`loot … mine` controleert de tool-tier **niet**.)
- [ ] runClient (optioneel, visueel): creative tab toont beide ertsen en het item; overlays zonder z-fighting of zwarte randen; crimson-variant herkenbaar naast gewoon crimson nylium; stenen pickaxe → geen drop, ijzeren → kristal + XP, Silk Touch → erts zonder XP; licht zichtbaar in het donker.

---

## Risico's

- **Aannames over change 10:** als helpers, providers of het texture-script anders heten of (nog) niet bestaan, vult deze change ze minimaal aan. Controleer eerst `CODEBASE.md` na change 10.
- **Tweelaags-model:** vraagt een model met elementen in datagen (`BlockModelBuilder.element()`), meer werk dan `cubeAll`. Overlay met halftransparante pixels breekt in cutout; het script moet alpha 0/255 afdwingen. Wel: geen vanilla-pixels in de repo, en change 17 hoeft alleen PNG's te vervangen.
- **Crimson-variant ligt aan het oppervlak** (nylium is één blok dik): makkelijker te vinden dan het netherrack-erts. De zeldzaamheid regelt change 16 (kleine aantallen). Licht 3 maakt hem extra opvallend — gewenst voor een sleutelmateriaal, maar houd het aantal laag.
- **Vegetatie op de crimson-variant:** crimson roots/fungi groeien alleen op blokken in `minecraft:nylium`; die tag krijgt de crimson-variant niet (vraag 6 A). De plekken blijven dus kaal; dat valt op en werkt als vindplaats-hint.
- **Tool-tier en XP** zijn balanswaarden; bijstellen kan later via tags en één constante.
- Geen worldgen in deze change: de ertsen zijn tot change 16 alleen via creative/`/give` te testen.
- **Change 17 moet de crimson-overlay kennen:** `crimson_soul_crystal_ore_top_overlay.png` staat nog niet in het plan van change 17.

---

## Ontwerpvragen

**1. Welke erts-varianten?**
- A: één blok `soul_crystal_ore` (netherrack-basis).
- B: twee blokken: `soul_crystal_ore` (netherrack) + `crimson_soul_crystal_ore` (nylium-look).
- C: zoals B, crimson-variant alleen als verklikker aan het oppervlak.

Antwoord (2026-09-30): **B** — ook `crimson_soul_crystal_ore`: karmozijnrood nylium met kristal, met eigen textures, eigen loot (Silk Touch) en tags. Change 16 vervangt crimson nylium door deze variant.

**2. Wat dropt het erts?**
- A: 1 `soul_crystal` (Fortune 1–4, Silk Touch → erts, XP 3–7). B: 2–4 scherven. C: dropt zichzelf, smelten in fase 2.

Antwoord (2026-09-30): **A**.

**3. Welke pickaxe is nodig?**
- A: ijzer. B: steen. C: diamant.

Antwoord (2026-09-30): **A** — ijzer (`needs_iron_tool`).

**4. Licht en particles?**
- A: zwak licht (3), geen particles. B: niets. C: licht + zielen-particles.

Antwoord (2026-09-30): **A** — licht 3, geen particles.

Textures (2026-09-30): eigen pixels via het texture-script uit change 10/17 (`tools/textures/…`), geen vanilla-pixels in de repo. Uitgewerkt in Plan §4–5 (vanilla-basis alleen als modelverwijzing, eigen overlay).

**5. Wat laat de crimson-variant achter zonder Silk Touch?** Vanilla crimson nylium dropt zonder Silk Touch netherrack; een erts laat normaal niets achter.
- **A (aanbevolen):** alleen `soul_crystal` (Fortune), het blok wordt lucht — identiek aan het netherrack-erts. Eenvoudig, één loot-helper, consistent voor spelers.
- **B:** `soul_crystal` + 1 netherrack (zoals nylium zonder Silk Touch). Voelt als "kristal uit de bodem breken"; één extra loot-pool, verder geen complexiteit.
- **C:** het kristal wordt geoogst en de bodem blijft staan: het blok wordt na het minen `crimson_nylium`. Sterk beeld, maar eigen block-class (`playerDestroy`/`onRemove`), afwijkend gedrag bij explosies en pistons, meer testwerk.

Antwoord (2026-09-30): **A** — zonder Silk Touch alleen `soul_crystal` (Fortune); het blok wordt lucht.

**6. Hoort de crimson-variant in de tag `minecraft:nylium`?** Die tag bepaalt of crimson/warped fungi en roots erop kunnen staan (ook bij worldgen-vegetatie na de ertsplaatsing).
- **A (aanbevolen):** nee. Geen vegetatie op de ertsplekken (valt op als kale plek = hint), geen bijwerkingen (bonemeal op naburig netherrack reageert op de tag maar doet dan niets).
- **B:** ja. Het erts gaat op in het crimson forest (fungi en roots erop), dus beter verstopt en natuurlijker; spelers kunnen er fungi op planten. Bijwerking: bonemeal op aangrenzend netherrack wordt verspild.

Antwoord (2026-09-30): **A** — niet in `minecraft:nylium`.
