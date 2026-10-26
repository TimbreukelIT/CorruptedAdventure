Scope van deze sessie: fase 1, **changes 31 t/m 37** uit het bouwplan (`docs/design/fase-1-wereld.md` §3): ring 2 en Nether-ring 2 afronden, tot en met de definitieve textures (37). Daarna teststop 3.

## Context
- Lees eerst `CLAUDE.md` en `CODEBASE.md`. Changes 0–30 zijn gebouwd en gecommit, plus de extra changes 23b, 23c, 30b en 30c. Laatste commit: `3abf6f3`.
- Teststop 1 (18–20), teststop 2, 2b en 2c (ring 2-terrein en -inrichting) zijn door mij in-game getest en goedgekeurd. Feedback en besluiten staan in `docs/plans/teststop-2-feedback.md` en `docs/plans/teststop-2b-feedback.md`. Heropen die niet; de huidige afstemwaarden van ring 1 en ring 2 blijven zoals ze zijn.
- Belangrijkste les uit de teststops: ring 2 moet **levendig** aanvoelen, niet kaal of leeg. Houd dat vast in 31 (`dry_riverbeds`), 32 (sfeer) en de Nether-ring 2-changes 33–36.
- De plannen `docs/plans/change-31.md` t/m `change-37.md` bestaan al, met ingevulde "Antwoord:"-regels en afstemmingssecties. Lees ook de secties "Gevolgen voor latere plannen" in `change-23b.md`, `change-23c.md`, `change-30b.md` en `change-30c.md`; die zijn nog niet allemaal in 31, 32 en 37 verwerkt. Openstaande punten voor 37: het palet van `canyon_cobblestone` (voelt raar), een rustigere `salt_crust`-texture (de zeshoeken zitten nu in de wereld), en of de eigen planten genoeg afsteken naast vanilla-planten en botblokken.
- Prestatiebudget noise-stap ring 2: ≤ 1,75× vanilla (hoogste meetpunt nu ~1,54×). Worldgen moet deterministisch zijn per seed.
- De bouwnotities en valkuilen staan in `CODEBASE.md` en onderaan elk plan.

## Rolverdeling
Jij (de hoofdsessie) orkestreert alleen; zie mijn geheugennotities "subagent-per-change" en "ring2-build-status". Al het lees-, plan- en bouwwerk doen subagents (general-purpose). Jij houdt je eigen context klein: je geeft opdrachten, stuurt reviews door, controleert commits met `git log`/`git status`, stelt mij vragen en rapporteert.

## Stap 1: plannen bijwerken (geen code)
Laat één subagent de plannen 31 t/m 37 vergelijken met wat er echt gebouwd is (vooral 23b/23c/30/30b/30c) en ze bijwerken: namen, bestanden, Y-waarden, hergebruik van bestaande features en placement modifiers, en de gevolgen-secties. Nieuwe ontwerpvragen die daaruit volgen, levert hij gebundeld aan. Leg ze mij voor met `AskUserQuestion`: max. 4 per ronde, aanbevolen optie eerst, en per ronde waar mogelijk de optie "alles aanbevolen". Verwerk de antwoorden als "Antwoord: …"-regels en commit: `docs: align plans 31-37 with built ring 2`.

## Stap 2: bouwen (loopt automatisch door)
Bouw strikt op volgorde, 31 → 32 → … → 37, één subagent per change, één tegelijk. Ze delen de werkmap, `CODEBASE.md` en testserverpoort 25565. De volgende change start pas als de vorige gecommit is; controleer dat zelf met `git log` en `git status`.

**Ga na elke change direct door met de volgende.** Stop alleen:
- bij een **nieuwe ontwerpvraag** die niet in de docs of het plan staat;
- als een test faalt en niet binnen de change op te lossen is;
- als een plan duidelijk niet meer klopt met de code (en de subagent het niet zelf binnen de categorie kan rechttrekken);
- na change 37, voor **teststop 3** (blokkerend).

Geef elke bouw-subagent deze opdracht mee:
1. Lees `CLAUDE.md`, `CODEBASE.md` en het eigen plan `docs/plans/change-NN.md`, plus de bouwnotities van de changes waarop het plan voortbouwt. Lees alleen de design-secties die het plan noemt. Geen Explore-agents of brede zoekacties.
2. Begin met `git log --oneline -5`. Controleer de aannames uit het plan tegen wat er echt gebouwd is. Wijkt iets af, pas het plan aan en meld het in het rapport.
3. Antwoorden in het plan zijn definitief. Komt er een nieuwe ontwerpkeuze langs: stop vóór dat deel en lever de vraag aan de hoofdsessie (SendMessage naar "main") met 2–4 opties, aanbevolen eerst. Implementatiedetails (welke vanilla-API, welke placement-stap) beslist de subagent zelf en noteert ze.
4. Bouw alleen binnen de categorie. Wat erbuiten valt (ook review-bevindingen uit een andere categorie) gaat naar `docs/backlog.md`.
5. Testen:
   - **B:** `./gradlew build`, inclusief unit tests.
   - **D:** `./gradlew runData` bij datagen-wijzigingen; commit de output.
   - **S:** `tools/server-smoke-test.sh` met een **nieuwe** `level-name` per change (bijv. `smoke-change31`). `run/server.properties` staat op preset `corruptedadventure:corrupted_world`, seed 20260930. Gebruik RCON voor extra checks (`/locate`, `/place`, tellingen, tags, loot). Voor client-checks: `./gradlew runClient -PquickPlay` (Dev) en `runClient2 -PquickPlay` (Dev2). Zet daarna RCON weer uit en stop server en clients. Ruim vergelijkings-worktrees op.
   - **Determinisme:** worldgen moet per seed identiek zijn. Controleer bij nieuwe features met een tweede verse wereld die in andere volgorde geladen wordt. Placements die van buurchunks afhangen (zoals `count_on_every_layer`, `random_patch`, vanilla-`disk_*` aan chunkranden) zijn niet toegestaan.
   - **Valkuil:** geforceloade chunks krijgen zonder speler geen random ticks. Random-tick-gedrag gaat dus naar de in-game checklist.
   - **W:** schrijf een precieze in-game checklist in het eigen plan onder "## In-game checklist (W)", met `/ca ring`, `/ca tpring`, `/ca tpboundary`, `/tp`, F3 en coördinaten op seed 20260930.
   - Changes met netwerk of client-code: de dedicated server mag geen client-class laden (`ClientIsolationTest` groen). Leg in het rapport vast wat twee clients moeten zien.
6. Werk `CODEBASE.md`, het design-doc en `docs/design/02-beslissingenlog.md` bij. Gebruik de beslissingenlog-regels uit het plan, met de datum van vandaag.
7. Draai `/code-review` via de Skill-tool (skill `code-review`, args `medium`) op de nog niet gecommitte diff. Komt de uitslag bij de subagent binnen, dan verwerkt hij die zelf. Anders stuurt de hoofdsessie de bevindingen via SendMessage door. De subagent lost ze op of legt vast waarom niet. Pas daarna committen.
8. Commit met een Engelse message en categorie-prefix (`worldgen:`, `atmosphere:`, `blocks:`, `textures:`), afgesloten met alleen de regel `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Stage alleen bestanden van de change (geen `git add -A`). Commit nooit bestanden uit `docs/prompts/`. Niet pushen. Laat `run/server.properties` met RCON uit achter.
9. Eindrapport aan de hoofdsessie, kort, in het Nederlands: wat er gebouwd is, afwijkingen van het plan, testresultaten (B/D/S + metingen), review-uitkomst, commit-hash, in-game checklist (kort) en backlog-items.

De bouw-subagent van change 37 schrijft daarnaast in `docs/plans/change-37.md` de **gebundelde checklist voor teststop 3**. Daarin komen de W-lijsten van 21, 22, 24, 28, 29, 31–37, plus wat uit eerdere teststops nog openstond (zandstorm en hittegolf in beeld en geluid, de duw bij richels en spleten, sfeer, Nether-ring 2, textures). Gegroepeerd per onderwerp, te doen in 1–2 sessies, met afstemvragen.

## Rapportage aan mij
- Na elke change: een kort bericht met commit, wat er gebouwd is, testresultaten en opvallende punten (afwijkingen, afstemwaarden die ik in-game moet beoordelen).
- Na change 37: de gebundelde checklist voor teststop 3 plus de afstemvragen. Wacht op mijn uitslag.
- Raakt jouw context of mijn limiet op, stop dan na een afgeronde en gecommitte change, of laat het werk ongecommit staan met een statusnotitie onderaan het plan ("Status bij onderbreking"). Werk de geheugennotitie "ring2-build-status" bij en meld waar we verdergaan.
