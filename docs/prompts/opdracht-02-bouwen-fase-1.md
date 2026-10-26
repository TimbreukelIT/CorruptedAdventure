# Opdracht 2 — Bouwen fase 1: Wereld

Plak dit als eerste bericht in een schone Claude Code-sessie, in de root van de repo. Pas het changenummer aan als je verdergaat.

---

Scope: fase 1 (Wereld), bouwen. We beginnen met **change 0** uit het bouwplan.

Lees eerst `CLAUDE.md` en `CODEBASE.md`, daarna uit `docs/design/fase-1-wereld.md` **alleen** §3 (bouwplan: de rij van deze change en de standaardtests B/D/S/W) en de sectie(s) die deze change nodig heeft (fundament §1 en/of de betreffende ringsectie). Lees `01-techniek.md` alleen als de change architectuur raakt. Zoek niet breder in de repo dan `CODEBASE.md` aanwijst. De sparring is klaar: ga niet opnieuw in op vastgelegde beslissingen.

**Per change (werkwijze uit `CLAUDE.md` §4)**
1. Benoem de categorie: *"Scope: `<categorie>` — change N"*.
2. Geef een kort plan: wat verandert er, welke bestanden, en de client/server-impact. Wacht niet op akkoord, tenzij er een keuze langskomt die niet in de design-docs staat. Stop dan en vraag het met `AskUserQuestion` (concrete opties, aanbevolen optie bovenaan).
3. Bouw in kleine stappen, alleen binnen de categorie van deze change. Kom je iets uit een andere categorie tegen, zet het dan in `docs/backlog.md` en ga door (gebruik alleen een minimale placeholder als het echt nodig is, en noteer die).
4. Test volgens de testkolom van de change:
   - **B**: `./gradlew build` moet slagen.
   - **D**: `./gradlew runData`, en commit de gegenereerde output.
   - **S**: start `./gradlew runServer` en controleer in de log dat er geen client-class-fouten zijn. Het joinen met een client en visuele checks (**W**, runClient) kun je niet zelf doen. Schrijf daarvoor een korte checklist van wat ik in-game moet testen (commando's, coördinaten) en wacht op mijn uitslag.
5. Werk `CODEBASE.md` bij (classes, registries, databestanden, valkuilen). Werk het design-doc en het beslissingenlog alleen bij als er iets besloten is.
6. Draai `/code-review` en los de bevindingen op, of leg vast waarom niet.
7. Commit met een Engelse message en categorie-prefix (bijv. `infra: rename package to nl.timtoys.corruptedadventure`). Zet in de commit geen niet-gerelateerde bestanden.

**Change 0 (`infra`)**
Hernoem het package `nl.corruptedadventure` naar `nl.timtoys.corruptedadventure`: bronmappen (main en client), `mod_group_id` in `gradle.properties`, eventuele verwijzingen in `neoforge.mods.toml`, en alle paden en package-namen in `CODEBASE.md`. Tests: B en S.

**Na elke change**
Meld kort wat er gebouwd en getest is, wat ik nog in-game moet controleren, en wat de volgende change is. Ga niet vanzelf door naar de volgende change; ik geef per change het startsein ("ga door met change N"). Wordt de context vol, stop dan na een afgeronde en gecommitte change en zeg waar we verdergaan.
