# Golf 2 — stroom C (condities): 79 zuiverend, gifkoppelingen, zuigmodderschade

Scope: `conditions`, branch `ring4/conditions-g2` (basis `main` c5b7d11), poort 4 (niet gebruikt: licht testen). Bron: `golf-2.md` §2 (C), §4b (G1–G3); `contract.md` §3, §5, §8; `waves.md` §4–§5; `golf-1-conditions.md` (69).

## 1. Plan (gebouwd)

1. **Zuiverend (Nether-4):** `PurifyingModifier` in stage `RateModifier.Stage.PURIFYING` (als laatste, na weer, gear en fire resistance). In een biome uit `#corruptedadventure:purifying` (de drie Nether-4-biomes, gevuld door W-nether) wordt elke **dalende** meter (gif, hitte, kou, uitdroging, debug) × `fallMultiplier`; een stijgende of stilstaande meter blijft ongemoeid, en dan wordt het biome niet eens bekeken. Geen eigen conditie, geen HUD-balk, geen label.
2. **Config `[conditions.purifying]`** (`ServerConfig.PurifyingCondition`, eigen klasse en sectie, want het geldt voor alle meters): `fallMultiplier` 3,0 (bereik 1,0–10,0; 1,0 = uit).
3. **Damage type `corruptedadventure:sucking_mud`** (`Ring4DamageTypes.SUCKING_MUD`, msgId `corruptedadventure.sucking_mud`, zoals vanilla `in_wall`: geen exhaustion). Tags (`Ring4DamageTypeTags`): `minecraft:bypasses_armor`, `minecraft:no_knockback`, `c:is_environment`. **Niet** in `bypasses_cooldown` (zoals vanilla verstikking).
4. **API voor B:** `condition.SuckingMudDamage.hurt(LivingEntity entity, float amount)` (publiek, statisch): doet niets op de client, past `DifficultyFloor.LETHAL_FROM_NORMAL` toe (peaceful niets, easy tot 10 HP, normal/hard dodelijk), **voor spelers én mobs** (V1 = B), en deelt de schade uit als `sucking_mud`. Geeft terug of er schade was. B bepaalt wanneer (hoofd onder de modder) en hoeveel (V2 = A); geen config.
5. **Doodsbericht** (`Ring4ConditionText`, en + nl): `death.attack.corruptedadventure.sucking_mud` (+ `.player`): "%1$s was swallowed by the sucking mud" / "%1$s werd opgeslokt door de zuigmodder".
6. **Gifkoppelingen:** de regel (`PoisonRule`, 69) leest alleen `#poison_sources` en `#poison_gases`; B vult ze in 72. `PoisonCouplingsTest` controleert de gegenereerde tags: zodra `toxic_pool` resp. `swamp_gas` geregistreerd zijn, moeten ze in `poison_sources` resp. `poison_gases` staan (tot dan overgeslagen; een lege tag zonder JSON is geen fout). `sucking_mud`, `slime_trail`, `glowcap`, `blighted_glowcap`, het gifkristal (blok, budding, drie knoppen, cluster) en `minecraft:water` staan er nooit in.

## 2. Client/server-impact

- Alles server-side: modifier, schade (`SuckingMudDamage.hurt` stopt op de client). Geen client-classes, geen `Minecraft.getInstance()`.
- Geen nieuwe payload, geen nieuwe conditie: `condition_sync` ongewijzigd, `PROTOCOL_VERSION` niet verhogen.
- Prestatie: één `biome.is(tag)` per dalende meter per speler per seconde (biome-holder staat al in `ConditionContext`).

## 3. Tests (licht, 2026-10-16)

- `./gradlew runData --no-daemon`: geslaagd; alleen eigen output (`damage_type/sucking_mud.json` nieuw; `sucking_mud` in `bypasses_armor`, `no_knockback`, `is_environment`; lang en + nl +2 elk).
- `./gradlew build --no-daemon`: geslaagd. Nieuw: `PurifyingModifierTest` (5: ×3 op de daling, ~33 s voor een volle gifmeter, buiten het biome niets, stijgend/stil ongemoeid zonder biomevraag, factor 1 = uit, zuiverend na het weer in de pijplijn), `SuckingMudDamageTest` (5: vloer per moeilijkheid, geen schade bij 0, tags en damage type, doodsbericht en + nl), `PoisonCouplingsTest` (3). `PoisonDamageTypeTagsTest.generated()` is package-private gemaakt voor hergebruik.
- Geen runServer, determinisme of perf (INT).

## Testdatapack voor INT (S-smoke, zolang 72 er niet is)

`<world>/datapacks/poison-test/pack.mcmeta` (`pack_format` 48) met `data/corruptedadventure/tags/block/poison_sources.json` = `{"values":["minecraft:slime_block"]}` en `poison_gases.json` = `{"values":["minecraft:cobweb"]}` (zoals 69). Na de merge van 72 het datapack weglaten en met `toxic_pool` en `swamp_gas` herhalen:
- naast de gifpoel +3/s (`/ca condition get` → `pool`), hoofd in moerasgas +2/s (`gas`);
- naast `sucking_mud`, `slime_trail`, `blighted_glowcap` en gifkristal: geen stijging (`clean`, −1/s);
- `/ca tpring 4 nether` met een gifmeter van 90: −3/s (in ~30 s leeg); hitte en kou dalen daar ook ×3 (−1,5/s);
- zuigmodder (na 72): hoofd onder → schade `sucking_mud` met het eigen doodsbericht; peaceful geen, easy stopt op 10 HP; ook een mob (zombie, koe) zakt en krijgt dezelfde vloer.

## Code-review (32a2aef, 2026-10-16, medium)

Opgelost in de vervolgcommit:
1. **Easy-vloer niet begrensd:** `DifficultyFloor.LETHAL_FROM_NORMAL` keek op easy alleen of de gezondheid boven 10 lag, zodat één grote klap (bijv. 4 bij 11 HP) onder 10 ging. Nu op easy `min(amount, health − 10)`. Geldt ook voor de gif-wither (1,0 per hit: hooguit een halve hit minder vlak boven 10). Test: `SuckingMudDamageTest.onEasyOneLargeHitNeverGoesBelowTen`.
2. **Geneste tags:** `PoisonCouplingsTest` eist nu dat `poison_sources` en `poison_gases` alleen blokken noemen (geen `#tag`), zodat de "nooit gif"-controle alles ziet wat de regel leest.
3. **`PurifyingModifier`:** vroege return bij een stijgende of stilstaande meter (geen closure, geen config-lezing).

Bewust niet opgepakt:
- Eén globale factor voor alle dalende meters (ook debug): zo besloten (contract §5, "alle meters").
- `assertCoupled` slaat over zolang het blok niet bestaat: de namen `toxic_pool`/`swamp_gas` komen uit het contract; INT2 controleert na de merge van 72 dat de test actief is (backlog).
- `ModTags`-javadoc, `CODEBASE.md` en het beslissingenlog: van INT (`waves.md` §5); de delta staat hieronder.

## Voortgang
- 2026-10-16: branch aangemaakt op c5b7d11 (de worktree stond op 17c9eb4); V1/V2 naar main; antwoorden V1 = B, V2 = A verwerkt; runData en build groen; gecommit als 32a2aef.
- 2026-10-16: code-review 1–3 verwerkt; build groen (geen datagen-wijziging).

## CODEBASE-delta

- `condition/PurifyingModifier` (stage `PURIFYING`): in `#corruptedadventure:purifying` (Nether-4) elke dalende meter × `fallMultiplier`; stijgend/stil ongemoeid.
- `condition/DifficultyFloor.LETHAL_FROM_NORMAL`: op easy begrensd op `health − 10` (`EASY_HEALTH_FLOOR`), niet alleen een drempel.
- `condition/SuckingMudDamage`: publieke API `hurt(LivingEntity, float)` voor B (72): schade `Ring4DamageTypes.SUCKING_MUD` met `DifficultyFloor.LETHAL_FROM_NORMAL` voor spelers en mobs; server-side.
- `condition/Ring4DamageTypes`: `SUCKING_MUD` = `corruptedadventure:sucking_mud` (msgId `corruptedadventure.sucking_mud`, zoals `in_wall`).
- `condition/Ring4Conditions`: registreert ook `PurifyingModifier`.
- `config/ServerConfig`: `PURIFYING` = `PurifyingCondition` (`[conditions.purifying] fallMultiplier` 3,0, 1–10).
- `datagen/tags/Ring4DamageTypeTags`: `sucking_mud` in `bypasses_armor`, `no_knockback`, `c:is_environment`.
- `datagen/lang/Ring4ConditionText`: `death.attack.corruptedadventure.sucking_mud` (+ `.player`), en + nl.
- Gegenereerd: `data/corruptedadventure/damage_type/sucking_mud.json`, regels in `bypasses_armor`, `no_knockback`, `is_environment` en de lang-bestanden.
- `ModTags.Biomes.PURIFYING`: javadoc zegt nog "lower the poison meter"; het zijn alle meters (INT mag de javadoc bijwerken, `ModTags` is niet van C).

## Beslissingen
- 2026-10-16, conditions: de moeilijkheidsvloer van de zuigmodder (`DifficultyFloor.LETHAL_FROM_NORMAL`) geldt ook voor mobs: op peaceful en easy sterft ook geen mob in de modder. Besluit van de gebruiker (V1 = B).
- 2026-10-16, conditions: B's blokcode bepaalt hoeveel en hoe vaak de zuigmodder schaadt en roept `SuckingMudDamage.hurt(entity, amount)` aan; C bezit damage type, tags, doodsbericht en de vloer; geen config. Besluit van de gebruiker (V2 = A).
- 2026-10-16, conditions: zuiverend krijgt een eigen sectie `[conditions.purifying] fallMultiplier` (3,0), omdat het voor alle meters geldt; alleen dalende meters, stijgende blijven ongemoeid. Akkoord van de gebruiker.
- 2026-10-16, conditions: `sucking_mud` getagd zoals vanilla `in_wall` (door harnas, geen knockback, omgeving), niet in `bypasses_cooldown`. Implementatiekeuze.

## Backlog
- conditions (INT): `ModTags.Biomes.PURIFYING`-javadoc ("lower the poison meter") naar "every condition meter falls faster".
- conditions (5b): `fallMultiplier` afstemmen; B's schade en interval van de zuigmodder afstemmen.
- conditions (INT2): na de merge van 72 de S-smoke van hierboven met de echte blokken; `PoisonCouplingsTest` wordt dan vanzelf actief (controleren dat B de contractnamen `toxic_pool` en `swamp_gas` gebruikt, anders slaat de test stil over).
