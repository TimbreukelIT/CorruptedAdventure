# Fase 2 golf 5 — stroom C (verbruiksitems void + stilte)

Branch `f2g5/consumables` op main `d55ff491` (merge-base, ook de review-basis), n = 67. Bron: `fase-2-items.md` §7.3, op
het raamwerk van §5.3/§6.3/§6.6 (`golf-34-C.md`).

## Wat
1. **Effecten (`conditions`)**: `RateEffect.ANCHORED` (void ×0,5, `[conditions.void] anchoredFactor`) en
   `RateEffect.RESOUNDING` (stilte ×0,5, `[conditions.silence] resoundingFactor`), als vanilla statuseffecten
   `anchored`/`resounding` (langste tijd wint, melk haalt weg, relog loopt door). Doses in `ConsumableDoses.skyLight`
   en `echoShellBlow`.
2. **Items (`items`)**: `sky_light_draught` (Hemellicht, eind van Nether-ring 4, tab 4) en `echo_shell` (Galmschelp,
   eind van Nether-ring 5, tab 5), met recepten, modellen, lang en/nl, eigen geluid en placeholders.

Client/server: drinken, blazen, dosis, effect, slijtage en cooldown draaien op de server. De server speelt het geluid
van de schelp af (iedereen binnen ~64 blokken hoort het) en vuurt het game event van een geitenhoorn
(`INSTRUMENT_PLAY`); de cooldown komt bij de client via het vanilla-pakket. De client toont alleen houding, balkje en
icoon. Geen nieuwe client-code.

## Texturenamen (voor T)
`item/sky_light_draught`, `item/echo_shell`, `mob_effect/anchored`, `mob_effect/resounding` (18×18). Placeholders zijn
herkleurde kopieën (dauwdrank, handwarmer, het icoon van cooled), gemaakt met een kladscript dat niet in git staat.
Extra wens voor T (optioneel): een model `item/echo_shell` met een "tooting"-houding zoals de geitenhoorn; nu is het een
gewoon `item/generated`.

## Voortgang
- 2026-10-28 16:05 start; vraag naar main: telt blazen als geitenhoorn voor de stilte-regel (A aanbevolen: ja).
- 16:30 code af met A; build loopt. Main: gebruiker kiest A.

## Voorlopig (bevestigen bij de teststop)
- Hemellicht: shapeless zuiverkristalscherf + gloeisteenstof + waterfles (alleen water) → 1, stack 16, drinkduur 32
  ticks (zoals de dauwdrank).
- Galmschelp: recept ` F ` / `FWF` / ` F ` (F = fossielsteen, W = witte kern) → 1.
- Galmschelp: houding 3 s (`TOOT_HORN`), geluid `corruptedadventure:item.echo_shell.blow` op `SoundSource.PLAYERS`
  (niet "records" zoals de geitenhoorn, zodat wie muziek uitzet het toch hoort), volume 4 = ~64 blokken bereik.
- Geluid: eigen audio, variant `echo_shell` in `tools/sounds/Ring5Sounds.java` (`sounds/item/echo_shell/blow_{1,2}.ogg`):
  een lage schelphoorn (~175/165 Hz) van 1,4 s met drie verre echo's en een galmstaart, 3,6 s.
- Galmschelp: cooldown in de config (`[conditions.silence] echoShellCooldownSeconds` 45); geen betoveringen (niet in
  `#enchantable/durability`), geen reparatiemateriaal.
- Kleuren van de effecten: anchored `F4E6A6`, resounding `B9A8F0`.
- In creative kost een blaas geen duurzaamheid (vanilla).

## W-checklist voor INT
- [ ] Ring 5 (binnenband, 's nachts): Hemellicht −20 void, "Verankerd" 45 s (void stijgt half zo snel; `/ca condition
      get`), fles terug.
- [ ] Nether-ring 5: Galmschelp blazen: −20 stilte en 45 s "Galm"; de meter daalt daarna ~10 s (geitenhoorn-regel);
      een tweede speler binnen 16 blokken hoort hem en krijgt ook die ~10 s daling, maar geen "Galm".
- [ ] Cooldown 45 s zichtbaar op de hotbar; na 16 keer breekt de schelp; ondertitel "Galmschelp galmt".
- [ ] Iconen "Verankerd"/"Galm" met timer; melk haalt ze weg; relog: ze lopen door.
- [ ] Dedicated server + 2 clients: de tweede speler hoort het geluid op afstand.

## CODEBASE-delta
- `condition/RateEffect`: `ANCHORED` (void, `ServerConfig.VOID.anchoredFactor`) en `RESOUNDING` (silence,
  `ServerConfig.SILENCE.resoundingFactor`).
- `registry/ModMobEffects`: `ANCHORED` (`F4E6A6`, icoon `mob_effect/anchored`) en `RESOUNDING` (`B9A8F0`,
  `mob_effect/resounding`); lang "Anchored"/"Verankerd" en "Resounding"/"Galm".
- `ConsumableDoses`: `skyLight` (void −20 + 45 s anchored), `echoShellBlow` (stilte −20 + 45 s resounding).
- `ServerConfig`: `VOID.anchoredFactor` 0,5, `skyLightAmount` 20, `skyLightAnchoredSeconds` 45; `SILENCE.resoundingFactor`
  0,5, `echoShellAmount` 20, `echoShellResoundingSeconds` 45, `echoShellCooldownSeconds` 45.
- `SilenceSounds`: ongewijzigd gedrag; de Galmschelp vuurt `INSTRUMENT_PLAY` en breekt zo de stilte zoals een geitenhoorn.
- `item/EchoShellItem`: duurzaamheid 16, `UseAnim.TOOT_HORN` 60 ticks; server: geluid, game event, dosis (niet voor een
  `FakePlayer`), cooldown uit de config, 1 slijtage.
- `registry/ModSoundEvents.ECHO_SHELL_BLOW` (`item.echo_shell.blow`, variabel bereik); `sounds.json` via
  `ModSoundDefinitionsProvider` (ondertitel `subtitles.corruptedadventure.item.echo_shell.blow`).
- `tools/sounds/Ring5Sounds.java`: variant `echo_shell` (`item/echo_shell/blow_1`, `_2`). Let op: het script opnieuw
  draaien codeert ook de bestaande ring 5-ogg's opnieuw (andere bytes, zelfde geluid); commit alleen de nieuwe.
- `ModItems`: `SKY_LIGHT_DRAUGHT` (Nether-ring 4, `DoseDrinkItem`, stack 16), `ECHO_SHELL` (Nether-ring 5).
- Datagen: `ConsumableRecipes` (Hemellicht, Galmschelp; helper `waterBottle()` gedeeld met de dauwdrank),
  `ConsumableItemModels`, `ConsumableText`.
- Tests: `RateEffectsTest` (anchored, resounding), `ModBlocksTest` (Nether-4/5-materiaalvolgorde filtert
  `DoseDrinkItem` en `EchoShellItem`), `ModCreativeTabsTest` (tab 4 en 5); GameTest
  `ConsumableGameTests.skyLightAnchorsAndTheEchoShellResoundsWearsAndCoolsDown`.

## Beslissingen
Besloten door de gebruiker (via main, 2026-10-28):
- 2026-10-28 · conditions · De Galmschelp vuurt `INSTRUMENT_PLAY` zoals een geitenhoorn: iedereen binnen
  `soundReliefRadius` (16), ook de blazer, krijgt de verlichting (10 s dalen); de blazer krijgt daarbovenop −20 + 45 s
  Galm · een echt teamitem, consistent met "een hoorn breekt de stilte".

## Backlog
- `textures`: definitieve textures van Hemellicht, Galmschelp en de iconen `mob_effect/anchored`/`resounding` (stroom T);
  eventueel een tooting-model voor de Galmschelp.
