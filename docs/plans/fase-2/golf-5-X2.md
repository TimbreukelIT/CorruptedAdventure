# Fase 2 golf 5 — X2 (GameTests, eindtestblok)

Branch `f2g5/gametests` · worktree `wt/f2g5-gametests` · n 63 · basis `2eacd11b` · categorie `items` (tests).

## Wat
GameTests aanvullen op wat in fase 2 het meest kan breken:
1. Smithing-ladder diamant → zonnesteen → vulkaniet → getijdezilver → nulliet (zwaard + harnasstuk met enchantment en
   coating) via de echte `RecipeManager`; netherite en overgeslagen stappen geen ingang.
2. Mijn-gating: elk eigen erts in `c:ores` echt gebroken door een survival-speler (`ServerPlayerGameMode.destroyBlock`):
   de pickaxe van de rang eronder dropt niets, die van de eigen rang wel.
3. Coatings: per conditie 4 stukken via het echte recept → stijgende rate 0,25× door de hele `RateModifier`-pijplijn;
   dalen en andere condities onveranderd; met het drankeffect erbij 0,125×; void neemt hemelglas, stilte witte kern.
4. Verbruiksitems: dosis (meter + effectduur) van alle 7, langste resterende tijd wint (beide kanten), jerky (+6 voeding,
   +8 uitdroging), fles terug uit een stapel, lege veldfles/handwarmer doen niets, Galmschelp-cooldown via het
   server-gebruikspad en breuk na 16 blazen.

## Voortgang
- 17:05 tests geschreven: `SmithingLadderGameTests`, `CoatingGameTests`, `GameTestPlayers` (survival-mockspeler),
  uitbreidingen in `TierGatingGameTests` en `ConsumableGameTests`.
- 17:46 build groen, runGameTestServer: alle 36 tests groen (10 nieuw). Geen bugs gevonden.

## CODEBASE-delta
- `gametest/GameTestPlayers.java` — survival-mockspeler (vanilla's mockspeler zegt altijd `isCreative()`).
- `gametest/SmithingLadderGameTests.java` — ladder diamant → nulliet (zwaard, borstplaat; enchantment en coating
  blijven), netherite/overgeslagen stappen geen recept; helpers `coat`/`hasCoating` (echt coating-recept).
- `gametest/CoatingGameTests.java` — 6 coatings × 4 stukken door de hele pijplijn (0,25×; met effect 0,125×), void =
  hemelglas, stilte = witte kern.
- `TierGatingGameTests.breakingARingOreDropsOnlyWithTheRightPickaxe` — echt breken met `destroyBlock`.
- `ConsumableGameTests` — jerky, alle doses + langste-tijd-wint, fles terug uit een stapel, lege staten, Galmschelp
  cooldown en breuk.

## Beslissingen
- Geen.

## Backlog
- Geen.
