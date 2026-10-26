# Opdracht 06 — Ring 3: 39f afronden, golf 2 afmaken, integratie 2, teststop 4b

Je bent de **hoofdsessie (orchestrator)** voor The Corrupted Adventure. Je bouwt zelf niets. Je plant, stelt mij de ontwerpvragen, start de agents, stuurt reviews door, merget en rapporteert (memory "subagent-per-change"). Communiceer in het Nederlands.

## 0. Eerst lezen (in deze volgorde, stop zodra je genoeg weet)
1. **Memory:**
   - `ring2-build-status.md`, vooral het blok "2026-10-08 ~17:35 STOP FOR THE DAY";
   - `subagent-per-change.md`.
2. **Stand nakijken:** `git log --oneline -5`, `git worktree list`, `git branch`, en `ls C:/Temp | grep ca-` (er mogen geen locks of slots meer staan).
3. **Regels voor de agents:** `build/agent-rules-golf2.md` (staat niet in git). Geef elke agent dit pad mee als verplichte lectuur.
4. **Plannen:**
   - `docs/plans/ring-3/waves.md`: §2 golf 2, §4 worktree-protocol, §5 eigenaarschap, §6 integratie;
   - `docs/plans/ring-3/contract.md`;
   - de plannen per stroom staan in hun eigen worktree, met onderaan "Status bij onderbreking".
5. Opdracht 05 (`docs/prompts/opdracht-05-ring-3-golf-2.md`) §4 en §5 blijven gelden: de agent-regels, de lessen, de review- en commitflow en de rapportage.

## 1. Stand bij de start (einde 2026-10-08)
- **`main` = `f81d76a`.** Daarin zitten:
  - het 4a-akkoord;
  - de 39f-besluiten;
  - de golf 2-plannen en het sparrenantwoord: alle 33 vragen = A;
  - de contracttoevoegingen `cold_exposed` en de ambient-particles;
  - 50-0 van atmosphere (merge `b30963e`).
- **Klaar op een branch, nog niet op main** (merge in integratie 2):

  | Branch | Commits | Inhoud |
  |---|---|---|
  | `ring3/conditions-g2` | `77c491d`, `dd3a191` | 55 kou Nether-3; 43 hitte-afstemming. Stroom klaar. |
  | `ring3/atmosphere-g2` | `9928e8e`, `fc441d4`, `6ebd45b` | 50-0, 50, 56. Alleen 50b (rookpluim) staat nog open. |
  | `ring3/textures-g2` | `90d7703` | 57a-r1. Ronde r2 volgt na de aankleding. |
  | `ring3/biomes-g2` | `a389396` | 41 ertsen. |

- **Gestopt en nog niet gecommit.** Elke stroom heeft een eigen worktree onder `.claude/worktrees/` en een "Status bij onderbreking" in zijn plan:

  | Change | Worktree / branch | Stand | Nog te doen |
  |---|---|---|---|
  | **39f** (agent `a2e6b63fe809808ca`, ~645k tokens: liever een verse agent) | `agent-a2e6b63fe809808ca`, `ring3/terrain-39f` (basis `088708e`) | Punten 1–9 + de accessors gebouwd. B groen (553 tests), D zonder diff, determinisme 2846/2880, S + portal OK. 3 warme perfrondes: noise ≤ 1,58×, surface 1,6–2,5×. | Koude perfronde, ná-screenshots in een verse wereld, `/code-review medium`, commit, merge. |
  | **W-biomes 47/47b/49** (teammate `g2-biomes-2`) | `agent-a26041cd066434c4c`, `ring3/biomes-g2` | De code van 47 staat er en compileert, maar is nog ongetest. 47b en 49 zijn concepten in `build/draft47/`. | Na de 39f-merge: 47c (inclusief striders) en 48. 41b komt pas na 42. |
  | **W-nether 53b** (agent `a78930bee6d548506`) | `agent-a78930bee6d548506`, `ring3/nether-g2` | Build, runData en serverstart groen. | In-game, lavawanden, determinisme, prestatie (noise + features), review. Rebase op 39f vóór de commit en draai daarna beide surface-scripts opnieuw. 54 is alleen ontworpen. |
  | **B 46-rest** (agent `a945f84b54317ff61`) | `agent-a945f84b54317ff61`, `ring3/blocks-g2` | Build, D en S (RCON) groen. | Client-check (screenshots en ondertitels), review, commit. |

- **Afspraken tussen stromen:**
  - **39f merget als eerste.** Daarna rebasen W-biomes en W-nether erop. W-biomes gebruikt dan de 39f-accessors (`volcanoesWithin`, `nearestVolcano`, `moundAt`, `sinkholeAt`, `collapseAt`, `ribcageNear` op `VolcanicField`).
  - **W-nether** verplaatst het Nether-deel van `tools/worldgen/ring3_surface.py` naar `nether_ring3_surface.py`. Gegenereerde output wordt nooit met de hand opgelost.
  - **Geiser-API (B):** `startCycle(level, pos)` vervangt `INTERVAL_TICKS`. Dat raakt 48.
  - **`cold_exposed`:** W-nether gebruikt een lokale TagKey; INT voegt die samen met C's `ModTags.Biomes.COLD_EXPOSED`.
  - **39f heeft drie tests van anderen aangepast** (o.a. `Ring3BiomesTest`). Controleer dat bij de rebase van W-biomes.
- **Nog te starten:**
  - **50b (rookpluim):** een verse agent, ná de 39f-merge. De concepten staan in `build/worktree-leftovers/atmosphere-g2/draft50/` (en in de A-worktree). De payload kan pas worden afgemaakt als 39f gemerged is, want hij gebruikt de accessor `volcanoesWithin`.
  - **W-terrein 42** (lavabuizen, magmakamers, geode-DF): ná de 39f-merge. Daarna volgt 41b (W-biomes).
  - **T r2:** na de aankleding.

## 2. Volgorde voor vandaag
1. **Lock en slots:** controleer dat ze vrij zijn.
2. **39f afronden:** start een verse agent in de bestaande worktree met de lijst "Nog te doen" uit de tabel hierboven. Daarna de review, de commit en de **merge naar main**.
3. **Parallel aan stap 2**, max. 3–4 agents tegelijk:
   - B 46-rest afmaken (client-check, review, commit);
   - W-nether 53b verder tot vlak vóór de commit.
4. **Na de 39f-merge:**
   - W-biomes rebasen, dan 47 testen → 47b → 49 → 47c (+ striders) → 48;
   - W-nether rebasen, dan 53b committen en 54 bouwen;
   - 50b (verse agent);
   - W-terrein 42, daarna 41b;
   - T r2.
5. **Integratie 2** volgens `waves.md` §6:
   - vaste merge-volgorde;
   - `PROTOCOL_VERSION` 5 → 6 (de condition-payload is 1 byte langer en er komt een pluim-payload bij);
   - de delta-secties van alle golf 2-plannen samenvoegen in `CODEBASE.md`, de beslissingenlog en de backlog;
   - determinisme met twee werelden, de tweede omgekeerd geladen;
   - de feature-index van ring 1/2;
   - de officiële prestatiemeting.
6. **Teststop 4b (blokkerend):**
   - een checklist uit de W-lijsten, met dedicated server + 2 clients;
   - extra in 4b: de geluiden (niet beluisterd door A), Particles: Minimal en FPS, bevriezing die ook de loopsnelheid halveert, 20 riftbogen (onderkant van de bandbreedte), lavavallen van 1 blok breed;
   - presenteren en wachten op mijn oordeel.

## 3. Aandachtspunten
- **Worktrees van golf 1:** er staan er nog drie (`agent-a2ab639ca59dbc1eb`, `agent-a336bc6c9f306650c`, `agent-a4a0f288d2f055c3d`). Verwijderen vraagt `--force`, dus vraag mij dat. De inhoud is al veiliggesteld in `build/worktree-leftovers/`.
- **Agents met een grote context:** start liever een verse agent in dezelfde worktree en verwijs hem naar de "Status bij onderbreking".
- **Agent-ID's:** na een herstart zijn agents alleen via hun **ID** bereikbaar, niet via hun naam.
- **Liveness:** controleer met de voortgangsregels in het plan, de mtimes en de owner-bestanden in `C:/Temp/ca-*`.
- **Stoppen:** controleer na een stopmelding zelf dat er geen processen meer draaien (`ps -ef`) en dat de locks weg zijn. Gisteren pakte een koude perfronde van 39f na de stopmelding nog alle slots.
- **De client van de gebruiker niet aanraken:** de screenshotscripts van agents mogen alleen hun eigen client pakken. Gisteren pakte er één per ongeluk het venster van mijn eigen client.

Begin met §0 en §1: lees in en controleer de stand. Start daarna 39f-afronding, B en W-nether parallel.
