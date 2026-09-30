# The Corrupted Adventure — Beslissingenlog

Eén regel per beslissing: datum · categorie · beslissing · reden. Nieuwste onderaan.

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-09-30 | infra | Minecraft 1.21.1, NeoForge 21.1.x, ModDevGradle | Stabiele versie |
| 2026-09-30 | infra | Mod-ID `corruptedadventure`, package `nl.corruptedadventure`, naam The Corrupted Adventure | — |
| 2026-09-30 | worldgen | Eigen world preset `corruptedadventure:corrupted_world` | Vanilla blijft ongemoeid; één regel in `server.properties` |
| 2026-09-30 | infra | Multiplayer en dedicated servers zijn een harde eis | Vanaf dag 1, achteraf is te duur |
| 2026-09-30 | worldgen | 5 ringen: 0–1.500 / 1.500–3.500 / 3.500–5.500 / 5.500–7.500 / 7.500–9.500, daarbuiten void + world border | Startwaarden, in config te balanceren |
| 2026-09-30 | worldgen | Nether als tussenwereld met tegenpool-ringen, `coordinate_scale` 4 | Ruimte voor content per Nether-ring, portals landen in dezelfde ring |
| 2026-09-30 | worldgen | The End uit (geen strongholds); shulker boxes blijven via alternatieve shell-bron | End-items niet nodig, shulkers wel handig |
| 2026-09-30 | structures | 4 bewakersstructures per grens, 4 kastelen in ring 5, op vaste berekende posities | Nooit ver teruglopen |
| 2026-09-30 | progression | Progressie deels per speler, deels per server | Samenspelen zonder dat iedereen alles apart moet grinden |
| 2026-09-30 | corruption | Final battle één keer per server, één groep; daarna corruptie weg en kastelen ruïnes | Post-game als duidelijk eindpunt |
| 2026-09-30 | infra | Vijf fases: wereld → items/tools → structures → mobs/bosses → progressie/corruptie | Gecontroleerd bouwen |
| 2026-09-30 | infra | Eén categorie per change; sparren bij elke fase en onduidelijke keuze; `/code-review` na elke change; `CODEBASE.md` altijd eerst lezen | Kwaliteit en token-zuinig werken |
