# INT — integratie fase 4

Stroom `INT` (door H1), n = 61 (vanaf ronde 3: n = 68, want 61 botst met L1 = 71), worktree `P:/Projecten/Java/wt/f4-int`, branch `f4/int`, vanaf main `d03463e4`.
Niet pushen, niet naar main mergen (dat doet main).

## Ronde 1 (2026-10-28)
Merges in deze volgorde:
1. `f4/hollow` 4dbf35c3 (H1: De Holle stap 1+2, gedeelde Nightmare-basis): schoon.
2. `f4/director` 84cdab88 (D: regisseur, met de 1024-fix 4eb1e1a5): CODEBASE (union; verouderde dubbele D-regel
   weg), gegenereerde lang (opnieuw via runData).
3. `f4/items` b41ddc44 (I: zwakte-items): constructor, ServerConfig, lang-providers, beslissingenlog (union).
4. `f4/r3` 0f6bff6e (R3: De Herrijzende): CODEBASE, backlog, constructor, client-entrypoint, lang-providers (union).

## Ronde 2 (2026-10-28)
5. `f4/r4` 21b47fab (R4: De Verloren Hoeder): gedeelde bestanden (union); een javadoc die de union splitste hersteld.
6. `f4/r5` 2d3e36d0 (R5: De Onvoltooide): gedeelde bestanden (union).
Daarna `PROTOCOL_VERSION` "11" → "12" (`keeper_mist`), imports gesorteerd, runData (5621d39f), build, GameTests 67/67.
Nog: `f4/r2` als laatste.

## Ronde 3 (2026-10-29): `f4/r2`
7. `f4/r2` 0f73883e (R2: De Gelaagde) in `f4/int` 70e40552 → merge a1fc70b8. Conflicten (union): mod-constructor
   (`LayeredGrip.register`), client-entrypoint (renderer), `ServerConfig` (`LAYERED`), `ModEntityTypes`, entity-tags,
   `NightmareLoot`, `NightmareText`, taalproviders (alleen volgorde), CODEBASE (R2-blok na De Holle; R2's oudere
   Holle-regel niet overgenomen), backlog; gegenereerde bestanden van `f4/int` gehouden en via runData opnieuw (9f480c61).
   Design-doc en beslissingenlog zonder conflict (R2 voegde daar alleen het model toe).
   `strengthFactor()`: geen voorkomens. Geen nieuwe payloads in R2 → `PROTOCOL_VERSION` blijft "12".

## Ronde 4 (2026-10-29): leven-ronde deel 1 (L0, L4, L5)
8. `f4/live-base` 0f423e03 (L0: gedeelde reactielaag) → 29663118: schoon.
9. `f4/live-keeper` 4ce07a5a (L4) → ff5d6405: client-entrypoint (union: `keeper_current`-handler bij de Hoeder).
10. `f4/live-unfinished` b0d1d0cf (L5) → 77c1f1cb: mod-constructor (`UnfinishedTempBlocks`), `NightmareText`
    (`memory_shard`), backlog en beslissingenlog (union). `PROTOCOL_VERSION` "12" → "13" (`keeper_current`, L4).
    CODEBASE: per Nightmare één blok (stap 1 + leven-ronde), D-infra bij D. runData: geen diff.

## Ronde 5 (2026-10-29): leven-ronde deel 2 (L2, L3)
11. `f4/live-layered` 2348df32 (L2) → 58b8e650: beslissingenlog (union).
12. `f4/live-risen` e6a03058 (L3) → 1eddc05a: beslissingenlog (union). Geen nieuwe payloads (L2/L3: geen wijziging in
    `network/`, 8 registraties in `ModNetwork` zoals ervoor) → protocol blijft "13". runData: geen diff.

## Ronde 6 (2026-10-29): L1, review, 2-clienttest
13. `f4/live-hollow` 82c5df32 (L1) → a9b49117: beslissingenlog (union), gegenereerde lang opnieuw (b1f87b76). Geen payloads.
14. GameTest-crash na de merge: `geckolib:entity_anim_trigger may not be sent` naar een mockspeler → `Nightmare` stuurt
    animatietriggers alleen naar volgers met GeckoLib's kanaal (bef7faae).
15. `/code-review medium d03463e4..f4/int` in 8 delen; fixes per gebied (8 fix-agents + INT): regisseur a2b8f644, Holle
    26f29ce7, Herrijzende 837b7afa, Onvoltooide 95dbb853, client ee29deb1, dazzle 3e832063, items 9711dbec, basis ca92f326,
    Gelaagde 017c4076, Hoeder 58b7a05b (o.a. logout terwijl vastgehouden: boss verdween met de speler; sneaken bevroor de
    vastgehouden speler; zwaartekracht van de Hoeder nooit opgeslagen). Docs 8c624d31 (backlog: niet-opgeloste
    bevindingen; voorlopig 103–106).

## Stand bij afronden (2026-10-28 22:00)
- `f4/int` op f21c6b89: H1, D, I, R3, R4, R5 gemerged; protocol "12"; runData 5621d39f; compileJava, build en GameTests
  67/67 groen; 2-clienttest gedaan (zie Voortgang 21:58). Geen eigen processen of slots meer.
- Niet gedaan: merge `f4/r2`, korte check daarna, `/code-review medium`.

## Volgende stappen
1. ~~R2 afmaken~~ (R2, 2026-10-29).
2. ~~`f4/r2` mergen~~ (ronde 3, 2026-10-29).
3. Korte check: server + client, `/summon corruptedadventure:the_layered` + een korte gevechtscheck.
4. `/code-review medium d03463e4..f4/int`, bevindingen fixen, build + GameTests.
5. Overdracht aan main (niet zelf pushen of naar main mergen).

## Taken
- [x] compileJava na elke merge (ronde 1: na items en r3)
- [x] runData (+ commit 09d1b047), build, GameTests 58/58 na ronde 1
- [x] `strengthFactor()` → `strength()` overal (ronde 1: geen voorkomens; ronde 2 opnieuw nagaan)
- [x] `docs/plans/fase-4/voorlopig-fase-4.md` (Voorlopig van alle stromen, R2 erbij: 69 punten)
- [x] CODEBASE, design-doc, beslissingenlog één geheel (ronde 3)
- [x] flaky `adispensershootsglitcharrows` → backlog `[items]`
- [x] server-smoke + 2 clients: per Nightmare `/summon` + korte check; De Holle echte komst via `/ca nightmare threat` + `arrive`
- [x] `/code-review medium d03463e4..f4/int` (ronde 6, 8 delen)

## Voortgang
- 2026-10-29 15:25 ronde 6 klaar: compileJava, runData, build groen; GameTests 115/115 (ook `theDirectedArrival…`).
  2-clienttest (`int6`, vers, `build/int-client-test6.sh`, niet in git; sheets in `build/test/shots/int-report/round6/`):
  De Holle echte komst via `/ca nightmare arrive 1 Dev` (voortekens, scheur, eruit), gevecht, scheurstappen, glitchpixels;
  De Herrijzende vliegt, vuurtegels, aswandelaars; De Hoeder in het meer: bellen, gifadem, grijpen; De Onvoltooide:
  brokken en ontladen grond (spelers in de void-put); doelwissel naar Dev2 na zware treffers bij alle vijf (`on target`).
  De Gelaagde: arena verkeerd opgezet (hij zat achter een heuvel), niet goed in beeld; de herkansing in een arena-wand liep
  niet (script brak af). Logout terwijl vastgehouden (via `/ride`): De Gelaagde blijft (count 1), Dev2 rijdt na login
  niet meer; De Hoeder blijft ook, maar `/ride` maakte Dev2 daar geen passagier (afgedekt door de GameTest
  `aHeldFighterGetsOffByATeleportAndLogsOutFreeWhileTheKeeperStays`). Serverlog 0 ERROR/Exception; clientlog alleen
  logrotatie en OpenAL bij afsluiten. Wankelen en zoeken niet duidelijk op de screenshots te zien.
- 2026-10-29 12:28 ronde 5 klaar: compileJava, runData (geen diff), build groen, elk apart. GameTests 97/97 (ook
  `theDirectedArrival…`, die flaky is; de fix komt met L1 in ronde 6). Server-smoke (`int-live5`, vers): `/summon` van alle
  vijf Nightmares werkt, 0 ERROR/Exception. `voorlopig-fase-4.md` punten 83–95 (L2, L3); punt 38 (landen, R3) gemarkeerd
  als vervangen door het grondgevecht (besluit 1a). CODEBASE: koppen R2/R3 met L2/L3. Volgt: L1 (ronde 6).
- 2026-10-29 11:00 ronde 4 klaar: compileJava, runData (geen diff), build groen, elk apart. GameTests 84/85: alleen
  `theDirectedArrivalOpensTheRiftAndTheHollowComesThrough` (bekend, L1). Server-smoke (`int-live`, vers): start zonder
  fouten, `/summon` van Hoeder, Onvoltooide en Gelaagde werkt, 0 ERROR/Exception. `voorlopig-fase-4.md` punten 70–82
  (L0, L4, L5; wat al in design-doc/beslissingenlog staat weggelaten; achteraan gezet zodat 1–69 blijven staan).
  Volgt: L1, L2, L3.
- 2026-10-29 10:33 ronde 3 (`f4/r2`) klaar: compileJava, runData (9f480c61), build groen, elk in een eigen aanroep.
  GameTests 70/71: alleen `theDirectedArrivalOpensTheRiftAndTheHollowComesThrough` faalt ("Did not expect hollow_rift
  to exist"): bekend, L1 zoekt hem uit. Server-smoke (`server.sh start int-r2 --fresh`): start zonder fouten, GeckoLib
  4.9.3 laadt, `/summon corruptedadventure:the_layered` werkt (450 HP, valt en tickt), 0 ERROR/Exception in het log.
  Voorlopig van R2 in `voorlopig-fase-4.md` (punten 24–35, plus "worp zonder wand is hoog"); CODEBASE bijgewerkt.
- 21:58 clienttest (server + Dev + Dev2, vers wereld `int1`, `build/int-client-test.sh`, niet in git; sheets in
  `build/test/shots/int-report/`): De Holle via `/ca nightmare arrive 1 Dev` met twee survival-spelers: voortekens,
  scheur 9 blokken achter Dev (0,5 62 21,5), max-HP 450 (1,5×), status "here", gevecht. De Herrijzende 600 HP, vliegt,
  as-particles, vuurrode bossbar. De Hoeder 1024 (geplafonneerd, 1200 virtueel) in een meer: lokroep/grijpen trok Dev het
  water in tot de bodem, schade 2 per keer, misselijkheid (gif); staart niet apart gezien. De Onvoltooide 1024 (geplafonneerd),
  witte bossbar, lens-zicht werkt, beweegt naar de speler. Geen ERROR/Exception in server- of clientlogs.
- 21:50 ronde 2 (r4, r5) groen: compileJava, runData, build, GameTests 67/67; protocol 12. Clienttest gestart.
- 2026-10-28 21:42 ronde 1 groen: 4 merges, compileJava, runData, build, GameTests 58/58. CODEBASE fase 4 samengevoegd
  (gedeelde basis + regisseur, De Holle, I, R3), `voorlopig-fase-4.md` (35 punten, ronde 1).
