# Opdracht 05 — Ring 3: terrein bijschaven (39f) + golf 2 (aankleding)

Je bent de **hoofdsessie (orchestrator)** voor The Corrupted Adventure. Je bouwt zelf niets. Je plant, stelt mij de ontwerpvragen, start de agents, stuurt reviews door, merget en rapporteert (memory "subagent-per-change"). Communiceer in het Nederlands.

## 0. Eerst lezen (in deze volgorde, stop zodra je genoeg weet)
1. Memory: `ring2-build-status.md` en `subagent-per-change.md`. Kijk de stand na tegen `git log`/`git worktree list`.
2. `CLAUDE.md` en `CODEBASE.md`.
3. `docs/plans/ring-3/waves.md`: golven, stromen, worktree-protocol §4, eigenaarschap §5, integratie §6, lessen §7.
4. `docs/plans/ring-3/contract.md`.
5. Rond teststop 4a: `teststop-4a.md`, `teststop-4a-feedback.md`, `change-39d.md` (afronding, open punten) en `change-39e.md` (aanbevelingen, open punten).
6. `docs/backlog.md`, de ring 3-punten.

## 1. Stand bij de start (2026-10-08)
- **`main` = `66628d3`.** Daarin zitten:
  - golf 1 + integratie 1 (`bf63d17`);
  - 39e, worldgen-prestatie (`099967b`): RingFillGate, ring_reach-surfaceconditie, per-kolomtabel in RingAquifer;
  - 39d (`66628d3`): ring 3-reliëf, vorm per biome, ~3× meer vulkanen, getraceerde lavarivieren, en fantasy-landvormen (archipels, zwevende vulkanen, afgronden, naalden, ribbenkasten, bogen).
- **Prestatie** (noise, mod/vanilla, 16×16, warm): ring 3 1,09–1,59×, ring 2 1,64×, Nether 1,11–1,45×. Budget ≤ 1,75×. De surface-stap van ring 3 is nog 1,5–2,8×.
- **Teststop 4a, mijn oordeel:** "stukken beter, in principe al aardig goed". De resterende punten worden opgelost in **39f**, parallel aan **golf 2**. Leg dat vast als akkoord-met-fixes in `teststop-4a-feedback.md` en in de beslissingenlog.
- **Opruimen na dat akkoord:** de golf 1-stroomworktrees en -branches zijn allemaal gemerged. Ruim ze op: worktrees `.claude/worktrees/agent-*`, branches `ring3/*-g1`, `ring3/perf-39e` en `worktree-agent-a2ab639ca59dbc1eb`. Controleer eerst per branch met `git branch --merged main`. Gooi niets weg dat niet gemerged is: meld het mij.

## 2. Spoor 1 — change 39f: terrein bijschaven (`worldgen`, één agent, eigen worktree)
Bronnen: mijn screenshots in `P:\Projecten\Java\The Corrupted Adventure\run\screenshots\` (vooral `2026-10-08_13.20.26.png` en `2026-10-08_13.21.50.png`). Bekijk ze zelf en geef ze de agent mee.

**Uit de screenshots**
1. **Archipel**, bv. (2622, −2738):
   - De basaltpilaren onder het hoofdeiland zijn kaarsrechte vierkante kolommen. Dat oogt kunstmatig. Maak ze onregelmatig: zuilvormig, verlopend, met uitsteeksels.
   - De lavavallen aan de eilandranden zijn korte, statische oranje platen. Ze vallen niet door tot op de grond of in het kratermeer.
   - De eilanden hangen in de vanilla-wolkenlaag (Y ~192). De wolken snijden er dwars doorheen.
   - Kijk ook naar de kleine hangende kegel onder het hoofdeiland.
2. **Rechthoekige put** (tweede screenshot):
   - Een asgerichte, rechthoekige schacht met kaarsrechte wanden en hoeken van 90°. Waarschijnlijk een zinkgat, lavabuis-instorting of afgrondeinde. Dat oogt kunstmatig.
   - Lichtgrijze verticale banden op de wanden.
   - Een strakke laag (grove) aarde langs de rand.
3. **Vlakte:** de traplijnen van 1 blok zijn nog duidelijk zichtbaar. Het 2D-microreliëf breekt ze niet genoeg.
4. **Losse vlekken:** de vlakke vlekken grove aarde/zand en de gestreepte naald op de achtergrond ogen nog wat "half".

**Open punten uit 39d**
- De kruinen van ribben en bogen krijgen de gewone biome-surface. Daardoor zijn de ribben donker met bleke voeten.
- Er staan maar 3 bogen over rifts in de hele ring.
- De ring 3-surface-stap kost 1,5–2,8×. Tip van 39e: zet goedkope condities vóór `noise_threshold`/`volcanic_threshold`, en gedeelde takken binnen de biome-condities.

**Open punten uit 39e** (laag, wel meenemen; of als aparte kleine `infra`/`worldgen`-change als het niet past)
- Een gedeelde gate-interface, zodat de resolve-lussen in `CellEdges`, `RockPillars` en `VolcanicField` door `RingGate`/`RingFillGate` heen kijken.
- Verouderde Javadoc in `RingBlend` en `RingFillGate`.
- De Fill-cache sleutelen op (dimensie, ring).
- `RingBlend.fillArray`: één scan per unieke kolom.
- `iterateNoiseColumn` in `duringFill` wikkelen.
- De officiële 39e-meting (≥ 3 rondes over 19 gebieden, plus koude rondes) en een client-smoke.

Zijn er ontwerpkeuzes (bv. hoe hoog de eilanden t.o.v. de wolken, of de wolkenhoogte in ring 3 aanpassen; dat laatste is `atmosphere`), vraag ze mij **vóór** het bouwen.

## 3. Spoor 2 — golf 2: inrichting, overleving en sfeer (parallel)
Pakketten volgens `waves.md` §2 "Golf 2":

| Stroom | Pakketten | Inhoud |
|---|---|---|
| W-biomes | 41, 41b, 47, 47b, 47c, 48, 49 | ertsen, vuurkristal-geodes, fumarolen/zwavel/zeekom-koraal, `titan_fields` (skeletten, zielvuur, titanen-rotsen), smeulwoud + slapende krater + lavavallen + geiten, `steam_fens`, horror (afgietsels, voetsporen), `dry_riverbeds`-vlekken |
| W-terrein | 42 | lavabuizen + magmakamers, geode-holtes |
| W-nether | 53b, 54 | Nether-3-aankleding, `glacier_rifts` |
| B | 46 (rest) | geluiden aan fumarole/geiser/modder |
| C | 55 | kou Nether-3, hitte afstemmen |
| A | 50, 56 | sfeer, particles, ambient, `Ring3Sounds` |
| T | 57a-rondes | textures bijsturen, nieuwe blokken |

Werkwijze:
1. **Plannen eerst.** Controleer of er per golf 2-stroom een plan is (`docs/plans/ring-3/golf-2-<stroom>.md`). Zo niet, laat één plan-agent ze schrijven, met contract, W-checklists, eigenaarschap en de 39d-terreinstand: nieuwe biomevormen, rifts, afgronden en rivieren beïnvloeden waar features mogen staan.
2. **Sparren vóór bouwen** (CLAUDE.md §3):
   - Verzamel de open ontwerpvragen per stroom en stel ze mij via AskUserQuestion: max. 4 per ronde, meerdere rondes mag, de aanbevolen optie bovenaan met "(aanbevolen)", en waar het kan een optie "Alles aanbevolen".
   - De focus ligt op **aankleding met wow en overleving** (avontuur/survival, fantasy, levendigheid per biome).
   - Leg de antwoorden vast in de plannen, het design-doc en de beslissingenlog.
3. **Agents zuinig inzetten:**
   - Max. **3–4 agents tegelijk** (incl. 39f).
   - Een verse agent per pakket(groep) in plaats van één agent die opzwelt.
   - Volgorde-advies: start W-biomes (41/47-reeks) + A of C + T; daarna de rest.
   - **W-terrein 42 pas ná 39f** (zelfde bestanden: `VolcanicField`/`Landmarks`/`ring3_surface.py`), of laat de 39f-agent daarna 42 doen.
   - Respecteer het eigenaarschap van gedeelde bestanden (`waves.md` §5).
4. **Integratie 2** volgens `waves.md` §6:
   - vaste merge-volgorde;
   - determinisme met twee werelden, de tweede omgekeerd geladen;
   - de feature-indexverschuiving in ring 1/2 meten;
   - de officiële prestatiemeting.
5. **Teststop 4b (blokkerend):** checklist met wat klaar moet zijn volgens `waves.md`, dedicated server + 2 clients. Presenteer hem mij en wacht op mijn oordeel.

## 4. Regels voor elke agent (meegeven in de prompt)
- **Scope en documentatie:**
  - Eén categorie per change, benoemd.
  - CODEBASE.md, design-doc en beslissingenlog bijwerken.
  - Buiten-scope-zaken in `docs/backlog.md`.
- **Testen:** B/D/S/determinisme/W-checklist in het eigen plan. Worldgen: verse wereld met seed `20260930`, elke ring + portal.
- **Prestatie:**
  - JFR `minecraft.ChunkGeneration`, mod vs vanilla als datapack-dimensie in één server, 16×16 chunks, warm-up over alle ringtypes, ≥ 3 rondes.
  - Budget noise ≤ 1,75× per gebied (mediaan).
  - Zit het erboven: stoppen en mij cijfers + opties geven.
- **Review:** de agent meldt "klaar voor review-doorsturen: <change>" met een korte diff-samenvatting. Jij start `/code-review medium` (Skill `code-review`) op de worktree-diff en stuurt de uitslag door. Pas daarna wordt er gecommit.
- **Commit:**
  - Engels, met categorieprefix.
  - Trailer **alleen** `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  - Stage per pad, nooit `git add -A`.
  - Nooit `docs/prompts/`, `.claude/` of kladtests committen.
  - Niet pushen. Agents mergen niet zelf; jij merget (fast-forward waar het kan).
- **Worktree-protocol:**
  - Eigen worktree.
  - Eigen poorten 25570+n / RCON 25580+n. RCON alleen lokaal tijdens de test, daarna weer uit.
  - JVM-slots `C:/Temp/ca-jvm-slot-{1,2,3}` en lock `C:/Temp/ca-test.lock`: eerst het lock, dan de slots. **Nooit wachten met het lock vast.** Altijd vrijgeven. Prestatiemetingen nemen lock + alle 3 slots, en nooit twee stromen tegelijk.
- **Lessen** (niet herhalen):
  - **Nooit `./gradlew --stop`:** dat stopt alle Gradle-daemons op de machine. Stop alleen je eigen PIDs. Ik draai soms zelf `runClient` vanuit de hoofdwerkmap; die nooit stoppen.
  - **Verse `run/`:** zet `run/options.txt` met `onboardAccessibility:false` en `skipMultiplayerWarning:true`.
  - **Geen grote tijdelijke bestanden op C:.** Een JFR-analyse vulde C: ooit met 35 GB. Zet ze op P: en ruim ze op.
  - **Screenshots:** eerst laten genereren, dan fotograferen. Geen `save-all flush` tijdens het fotograferen.
  - **Vloeistoffen:** lava/water nooit tegen lucht zonder korst. Geen neighbour-chunk-placements. Worldgen deterministisch per seed en onafhankelijk van de laadvolgorde.
  - **Berichten:** agents in een shell-wachtlus krijgen geen berichten. Ze moeten idle wachten.
- **Orchestratie** (voor jou):
  - Controleer of een agent echt nog leeft (ListAgents, mtimes van logs en locks) voordat je zegt dat hij "nog bezig" is.
  - Na een herstart zijn agents alleen via hun **agent-ID** bereikbaar, niet via hun naam.
  - Geef eerlijke tijdsinschattingen en meld het als ze uitlopen.
  - Laat agents niet meer meetrondes draaien dan nodig.

## 5. Rapportage aan mij
- Kort en in het Nederlands. Bij beelden: stuur kaarten en screenshots (vóór/na) zodra ze er zijn.
- Stop alleen voor:
  - een nieuwe ontwerpvraag;
  - een test die faalt en niet te fixen is;
  - een plan dat niet meer klopt;
  - een overschrijding van het prestatiebudget;
  - teststop 4b.
- **Raakt context of limiet op:** stop na een gecommitte change, schrijf "Status bij onderbreking" in de betreffende plannen en werk memory `ring2-build-status.md` bij (inclusief agent-ID's en worktrees).

Begin met §0 en §1: lees in, controleer de stand, leg het 4a-akkoord vast en ruim op. Start daarna 39f. Begin parallel met de plannen en sparvragen voor golf 2.
