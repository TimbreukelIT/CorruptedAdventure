# Opdracht 07 — Ring 3, golf 3: textures (57), backlog-opruiming en teststop 4c, met een sneller testritme

Je bent de **hoofdsessie (orchestrator)** voor The Corrupted Adventure. Je bouwt zelf niets. Je plant, stelt mij de ontwerpvragen, start de agents, stuurt reviews door, merget en rapporteert (memory "subagent-per-change"). Communiceer in het Nederlands.

## 0. Eerst lezen (in deze volgorde, stop zodra je genoeg weet)
1. **Memory:** `ring2-build-status.md` (vooral de blokken van 2026-10-09), `subagent-per-change.md`, `light-testing-per-package.md`.
2. **Stand nakijken:** `git log --oneline -8`, `git worktree list`, `git branch`, `ls C:/Temp | grep ca-` (geen locks of slots), en of er nog Java-processen van agents draaien.
3. **Regels voor de agents** (niet in git, geef ze elke agent mee als verplichte lectuur): `build/agent-rules-golf2.md` + `build/agent-rules-afronding.md` (gaat vóór). In golf 3 komt daar `build/agent-rules-golf3.md` bij (zie §3).
4. **Plannen en besluiten:**
   - `docs/plans/ring-3/waves.md` §2 "Golf 3 — afronding", §6 integratie (met het vangnet `git diff --exit-code src/generated`);
   - `docs/plans/ring-3/teststop-4b.md` en `teststop-4b-feedback.md` (besluiten 2026-10-09);
   - `docs/backlog.md` (secties ring 3);
   - `CODEBASE.md`.

## 1. Stand bij de start
- **`main` = `8724301`** (2026-10-09 avond; afronding `5abd7f7` + prestatiecijfers). Daarin:
  - golf 2 + integratie 2 (`c6cd90d`): 39f, 41/41b, 42, 46-rest, 47/47b/47c/48/49, 50/50b/56, 53b/54, 55/43, 57a-r1; protocol 6;
  - de **golf 2-afronding** (integratie `ring3/int-afr`, `e4a1886`): 42b (lava tegen lucht, diepe snedewanden, geen ring 3-aquiferlava, `ash_block`-daken), 54b (ijsvrije warme kring), 47d (overwoekerde krater, zwavelpoelen + kristallen, meer zielvuur, `in_ring`), 50c (geiserwaarschuwing, lavabommen per oppervlak, pluimen zonder mist, rode uitbarstingslucht), 46b (rotaties, zwavelblokken), 55b (kampvuur lit, ijs smelt, spons droogt; eerste common mixins);
  - teststop 4b: feedback + besluiten in `docs/plans/ring-3/teststop-4b-feedback.md`.
- **Prestatie:** na INT2 noise ring 3 1,00–1,36× (warm), koud ≤ 1,66×; Nether-3 1,15–1,36×. Ná de afronding (main `8724301`, `docs/plans/ring-3/int-afr.md`): noise ≤ 1,39× warm (zeekom het hoogst), koud ≤ 1,29×; surface ring 3 1,7–2,6×, features ≤ 1,31×; 42b niet meetbaar.
- **Open:**
  - één lavalek in de afgrond (4.409–4.412, Y 5–9, −510…−508) en 6 ijsblokjes bij de warme scheuren in Nether-3: backlog, fix in golf 3;
  - 1 aslaag onder vloeiende lava bij de archipelvallen (determinisme): backlog;
  - de textuurronde T r2 (shots in `.claude/worktrees/int2/build/teststop-4b/` en `build/afr-shots/`, tabel in `teststop-4b.md`) is niet door mij beoordeeld en gaat op in **57**;
  - de placeholder-textures van de zwavelblokken (57).
- **Worktrees:** alleen `.claude/worktrees/int2` staat nog (bron voor de testtooling, §3.1); de scripts van de andere worktrees zijn veiliggesteld in `build/worktree-leftovers/golf2-*`. De stroom-branches blijven tot teststop 4c akkoord is.

## 2. Inhoud van golf 3
1. **57 — definitieve textuurronde (stroom T):**
   - alle ring 3- en Nether-3-blokken, inclusief de golf 2-aankleding en de placeholders van de afronding (`sulfur_crystal` + buds, `yellow_/green_/orange_sulfur_mud`), en de randpunten van `glowing_fissure` sinds de rotaties (backlog);
   - werkwijze zoals 57a: T maakt in-game shots per blok in een echte omgeving, ik geef per blok kort "goed / lichter / rustiger / meer gloed"; maximaal twee rondes;
   - de Sodium/Iris-check van de gloeilagen (backlog) als losse test-instance, alleen als het zonder veel werk kan.
2. **Backlog-opruiming ring 3:** loop met mij de resterende ring 3-backlogregels door in één sparringronde (AskUserQuestion, max. 4 vragen per ronde, aanbevolen optie eerst, consequentie per optie). Alleen wat ik kies, wordt gebouwd; de rest gaat expliciet naar fase 2–5 of blijft staan.
3. **Teststop 4c — eindcheck ring 3 + Nether-3** (blokkerend): checklist uit alle W-lijsten, standaard-W (elke ring en grens, portal per ring, ook overworld ring 3 → Nether-3), prestatie en determinisme. Presenteren en wachten op mijn oordeel.

## 3. Sneller testen (hard verzoek van mij: niet meer de halve dag wachten)
Golf 2 kostte veel wachttijd door: gemiste meldingen van achtergrondtaken, agents die het test-lock lang vasthielden, een volledige prestatiemeting die liep terwijl ik wilde spelen, en testscripts die elke agent opnieuw bouwde. Daarom:

1. **Eerste pakket van golf 3: `infra` — testtooling in git** (`tools/test/`), gebouwd door één agent vóór de rest start (± 1 uur):
   - server starten/stoppen met eigen poort en level-name, RCON-client, PID-registratie en het opruimen van eigen processen;
   - lock/slots als één script (eerst lock, dan slots, nooit wachten met het lock vast, altijd vrijgeven, met een `owner`-bestand);
   - offline regio-scans: lava tegen lucht (met de indeling trede/val/lek), determinisme (twee werelden, omgekeerd geladen), feature-index ring 1/2;
   - prestatie (JFR `ChunkGeneration`, mod vs vanilla als datapack-dimensie in één server, noise/surface/features, mediaan per gebied);
   - screenshots alleen via de eigen client-PID;
   - de bestaande scripts uit `.claude/worktrees/int2/build/int2/`, `terrain-g2/build/afr/py/` en `tools/nether-lava-walls.py` zijn de bron; samenvoegen, niet opnieuw uitvinden.
   Daarna gebruikt elke agent deze tools in plaats van eigen scripts.
2. **Licht per pakket, zwaar één keer** (blijft): per pakket alleen build + runData + unit tests; één review per stroom; de integratie doet S, determinisme, offline scans.
3. **Speelbaar eerst:** de integratie merget naar main zodra B/D/S-smoke en de offline checks groen zijn en de review is verwerkt. Ik test dan vanuit de **hoofdwerkmap** (niet vanuit een worktree). De prestatiemeting komt pas daarna, als ik niet speel.
4. **Prestatie alleen waar nodig:** alleen als een pakket de noise- of surface-stap raakt; 3 warme rondes + 1 koude; liefst op een moment dat ik niet speel (bijv. aan het eind van de dag), onbewaakt maar met een harde stop na ± 45 min.
5. **Gebruiker speelt = machine van de gebruiker:** zodra ik zeg dat ik ga testen (of er een `runClient` vanuit de hoofdwerkmap draait), starten agents geen JVM's meer. Main legt dan het lock `C:/Temp/ca-user-test.lock` neer; agents respecteren dat en werken alleen nog zonder JVM. Main haalt het lock weg als ik klaar ben.
6. **Nooit wachten op een melding:** agents controleren zelf (logbestand, PID, uitvoerbestand) of een achtergrondtaak klaar is. Main controleert bij stilte na ~10 min zelf de voortgang (mtimes, `Voortgang:`-regels, locks) en spreekt de agent aan.
7. **Main merget vooruit** als een stroom van een andere afhangt (zoals 39f, B en 42 in golf 2), in plaats van te wachten op de integratie; of laat de afhankelijke stroom de branch zelf mergen als de review nog loopt.
8. **Tijden:** gebruik bij elke schatting de echte klok (`date`), niet een geschatte tijd.

Leg deze afspraken vast in `build/agent-rules-golf3.md` (niet in git) en het procesdeel in `docs/plans/ring-3/waves.md` §4/§6 (wel in git), en noteer de beslissing in de beslissingenlog (categorie `infra`).

## 4. Volgorde
1. Stand controleren (§0), daarna een korte vragenronde aan mij: de backlog-opruiming (§2.2) en eventuele keuzes voor 57.
2. **Infra-testtooling** (§3.1): één agent. Parallel daaraan: T begint aan 57 r1 (shots maken kan met de bestaande tools; hij krijgt de nieuwe tools zodra ze er zijn).
3. De gekozen backlogpakketten, parallel (max. 3–4 agents), licht testen, review per stroom.
4. **Integratie 3** (snel protocol), merge naar main, ik speel.
5. 57 r2 als nodig.
6. Prestatiemeting (als ik niet speel), daarna **teststop 4c**.

## 5. Aandachtspunten
- De commit-trailer is alleen `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. `docs/prompts/` en `.claude/` nooit committen.
- Worktrees die gemerged zijn: opruimen kost `--force` (untracked `build/`/`run/`); vraag mij dat één keer per golf.
- De client van de gebruiker nooit aanraken. Screenshots alleen via de eigen PID.
- Bij een stopmelding: zelf controleren dat er geen processen meer draaien en dat lock en slots weg zijn.
