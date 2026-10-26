# Opdracht 3 — Plannen en bouwen: systemen + ring 2 (changes 18–37)

Plak dit als eerste bericht in een schone Claude Code-sessie, in de root van de repo.

---

Scope van deze sessie: fase 1, **changes 18 t/m 37** uit het bouwplan (`docs/design/fase-1-wereld.md` §3): eerst de systemen (C: 18–20, netwerk-basis, meter-systeem, ringweer), daarna ring 2 + Nether-ring 2 (D: 21–37).

## Context
- Lees eerst `CLAUDE.md` en `CODEBASE.md`. Changes 0–17 zijn gebouwd en gecommit (ring 1 + Nether-ring 1 compleet).
- Ring 1 is door mij in-game getest en goedgekeurd (2026-10-01): wereldgeneratie en mechanics werken. De huidige afstemwaarden van ring 1 blijven zoals ze zijn; heropen ze niet.
- De sparring voor ring 2 is klaar: de ontwerpkeuzes staan in `fase-1-wereld.md` (sectie "Ring 2 — Droge ring", de Nether-ring 2-sectie, condities en het bouwplan). Heropen vastgelegde beslissingen niet.
- Voor changes 18–37 bestaan nog **geen** uitgewerkte plannen in `docs/plans/`. Die maak je eerst (fase A).

## Rolverdeling
Jij (de hoofdsessie) orkestreert alleen; zie ook mijn geheugennotitie "subagent-per-change". Al het lees-, plan- en bouwwerk doen subagents (general-purpose). Jij houdt je eigen context klein: geef opdrachten, stuur reviews door, controleer commits met `git log`/`git status`, stel mij vragen en rapporteer.

## Fase A — Plannen (geen code)
1. Laat per change een subagent `docs/plans/change-NN.md` schrijven, in hetzelfde format als `docs/plans/change-08.md` t/m `change-17.md`: scope/categorie, aannames over eerdere changes (namen, classes, bestanden), aanpak en bestanden, client/server-impact, ontwerpvragen met 2–4 opties (aanbevolen eerst, consequenties), testplan (B/D/S/W) en de beslissingenlog-regels.
   - Plan-subagents mogen parallel draaien (bijv. in groepen van 4–5); ze schrijven alleen hun eigen planbestand.
   - Ze lezen alleen `CLAUDE.md`, `CODEBASE.md`, hun rij in het bouwplan en de design-secties die de change nodig heeft. Geen Explore-agents of brede zoekacties.
   - Per change markeert het plan of er een **blokkerende handmatige test** nodig is: alleen als iets niet automatisch (build, unit test, server + RCON) te controleren is én latere changes op de uitkomst bouwen. Al het andere is W-achteraf.
2. **Afstemmen:** laat één subagent alle plannen 18–37 naast elkaar leggen en tegenstrijdigheden oplossen. Denk aan gedeelde namen en registries, één gedeelde aanpak in plaats van twee (zoals in ring 1: één `vanilla_climate`-source, één `ring_chance`-filter, één texture-script per ring), en welke change wat als eerste aanmaakt. Vul de "Antwoord:"-regels nog niet in.
3. **Vragenronde(s):** bundel alle open ontwerpvragen en leg ze mij voor met `AskUserQuestion`: max. 4 per ronde, meerdere rondes mag, aanbevolen optie eerst. Bied per ronde ook de optie "alles aanbevolen" aan waar dat kan. Stel in deze ronde ook voor bij welke changes er een blokkerende teststop komt (zo min mogelijk; waarschijnlijk alleen na de systemen 18–20, omdat netwerk, HUD en weer-sync twee echte clients nodig hebben).
4. Verwerk mijn antwoorden als "Antwoord: …"-regels in de plannen en commit ze: `docs: add build plans for changes 18-37 (systems + ring 2)`.

## Fase B — Bouwen (loopt automatisch door)
Bouw strikt op volgorde, 18 → 19 → … → 37, één subagent per change, één tegelijk. Ze delen de werkmap, `CODEBASE.md` en testserverpoort 25565. De volgende change start pas als de vorige gecommit is; controleer dat zelf met `git log` en `git status`.

**Ga na elke change direct door met de volgende.** Stop alleen:
- bij een **nieuwe ontwerpvraag** die niet in de docs of het plan staat;
- bij een **blokkerende teststop** die in fase A met mij is afgesproken. Geef dan een korte, precieze checklist en wacht op mijn uitslag;
- als een test faalt en niet binnen de change op te lossen is;
- als een plan duidelijk niet meer klopt met de code (en de subagent het niet zelf binnen de categorie kan rechttrekken).

Geef elke bouw-subagent deze opdracht mee:
1. Lees `CLAUDE.md`, `CODEBASE.md` en het eigen plan `docs/plans/change-NN.md`. Lees alleen de design-secties die het plan noemt. Geen Explore-agents of brede zoekacties.
2. Begin met `git log --oneline -5`. Controleer de aannames uit het plan tegen wat er echt gebouwd is. Wijkt iets af, pas het plan aan en meld het in het rapport.
3. Antwoorden in het plan zijn definitief. Komt er een nieuwe ontwerpkeuze langs: stop vóór dat deel en lever de vraag aan de hoofdsessie met 2–4 opties, aanbevolen eerst. Implementatiedetails (welke vanilla-API, welke placement-stap) beslist de subagent zelf en noteert ze.
4. Bouw alleen binnen de categorie. Wat erbuiten valt (ook review-bevindingen uit een andere categorie) gaat naar `docs/backlog.md`.
5. Testen:
   - **B:** `./gradlew build`, inclusief unit tests.
   - **D:** `./gradlew runData` bij datagen-wijzigingen; commit de output.
   - **S:** `tools/server-smoke-test.sh` met een **nieuwe** `level-name` per change (bijv. `smoke-change18`). `run/server.properties` staat op preset `corruptedadventure:corrupted_world`, seed 20260930. Gebruik RCON voor extra checks (`/locate`, `/place`, tellingen, tags, loot). Zet daarna RCON weer uit en stop de server.
   - **Determinisme:** worldgen moet per seed identiek zijn. Controleer bij nieuwe features met een tweede verse wereld die in andere volgorde geladen wordt. Placements die van buurchunks afhangen (zoals `count_on_every_layer`) zijn niet toegestaan.
   - **Valkuil:** geforceloade chunks krijgen zonder speler geen random ticks. Random-tick-gedrag gaat dus naar de in-game checklist.
   - **W:** schrijf een precieze in-game checklist met `/ca ring`, `/ca tpring`, `/ca tpboundary`, `/ca border`, F3 en coördinaten op seed 20260930.
   - Changes met netwerk of client-code: de dedicated server mag geen client-class laden (`CLAUDE.md` §7). Leg in het rapport vast wat twee clients moeten zien.
6. Werk `CODEBASE.md`, het design-doc en `docs/design/02-beslissingenlog.md` bij. Gebruik de beslissingenlog-regels uit het plan, met de datum van vandaag.
7. Draai `/code-review` via de Skill-tool (skill `code-review`, args `medium`) op de nog niet gecommitte diff. De uitslag komt bij de hoofdsessie binnen, niet bij de subagent. De hoofdsessie stuurt de bevindingen via SendMessage door; de subagent lost ze op of legt vast waarom niet. Pas daarna committen.
8. Commit met een Engelse message en categorie-prefix (`infra:`, `conditions:`, `atmosphere:`, `blocks:`, `worldgen:`, `textures:`). Sluit af met de attributieregels die deze sessie voorschrijft. Stage alleen bestanden van de change (geen `git add -A`). Commit nooit bestanden uit `docs/prompts/`. Niet pushen.
9. Eindrapport aan de hoofdsessie, kort, in het Nederlands: wat er gebouwd is, afwijkingen van het plan, testresultaten (B/D/S + metingen), review-uitkomst, commit-hash, in-game checklist (W) en backlog-items.

## Rapportage aan mij
- Na elke change: een kort bericht met commit, wat er gebouwd is, testresultaten en opvallende punten (afwijkingen, afstemwaarden die ik in-game moet beoordelen). Bewaar de in-game checklists.
- Aan het eind (na change 37), of bij een teststop: één gebundelde in-game checklist, gegroepeerd per onderwerp, plus een lijst afstemvragen die ik na het testen kan beantwoorden.
- Wordt jouw context vol, stop dan na een afgeronde en gecommitte change. Meld waar we verdergaan (welke change, welke plannen klaar zijn).
