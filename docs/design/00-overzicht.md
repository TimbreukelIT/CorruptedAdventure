# The Corrupted Adventure — Designoverzicht

## Visie
Avontuur-, horror- en survivalmod rond Herobrine. De speler reist van een vertrouwde, vanilla wereld naar buiten, door steeds vijandigere ringen, tot aan Herobrines domein. Elke ring voelt als een eigen wereld: eigen blokken, ores, grotten, structures en later eigen mobs. Naarmate de speler verder komt, wordt de wereld steeds meer "fout" (corruptie).

## Wereldopbouw: 5 concentrische ringen
| Ring | Naam (werktitel) | Thema | Afstand tot spawn (blokken) | Nether (schaal 4) |
|---|---|---|---|---|
| 1 | Vanilla-ring | Vertrouwd Minecraft, rust vóór de storm | 0 – 1.500 | 0 – 375 |
| 2 | Droge ring | Canyons en oases met villagers | 1.500 – 3.500 | 375 – 875 |
| 3 | Vulkanische ring | Vulkanen en lavameren; het water is "gestolen" | 3.500 – 5.500 | 875 – 1.375 |
| 4 | Moerasring | Gestolen water, gif, enge beesten boven en onder de grond | 5.500 – 7.500 | 1.375 – 1.875 |
| 5 | Corrupted ring | Wastezone, void, glitches; Herobrines kastelen | 7.500 – 9.500 | 1.875 – 2.375 |
| — | Buitenrand | Afbrokkelend terrein en void; world border op 10.000 | 9.500 – 10.000 | 2.375 – 2.500 |

Grenzen hebben ruis (±96 blokken) en overgangszones (~128 blokken). Alle waarden staan centraal in config, zodat ze te balanceren zijn. Elke grens krijgt een eigen soort muur (mist, as, …) die de ring erachter vaag houdt tot de bewaker verslagen is. Uitwerking: `fase-1-wereld.md`.

Verhaallijn: het water uit ring 3 is gestolen en naar ring 4 gestroomd. Dit is een aanknopingspunt voor quests en lore.

## De Nether als tussenwereld
- De Nether volgt dezelfde ringindeling, met **coördinatenschaal 4**. Elke Nether-ring is de **tegenpool** van de bijbehorende overworld-ring: 1 vanilla-Nether · 2 doorweekte schimmel-Nether · 3 bevroren Nether · 4 droge kristal-/bottenwoestijn · 5 'zuivere' witte, stille Nether. Het sleutelmateriaal van Nether-ring N is thematisch een tegengif voor ring N+1.
- Een portal in overworld-ring N komt automatisch uit in Nether-ring N.
- De Nether-ringen bevatten speciale items en quests die nodig zijn voor progressie naar de volgende ring.
- Voorstel voor de progressielus per ring: overworld-ring N verkennen → Nether-ring N (tegenpool, sleutelmateriaal) → bewaker van grens N oproepen en verslaan → ring N+1 open.
- De zachte grensmuur geldt ook in de Nether, zodat je daar niet vooruit kunt lopen.

## The End
- Uitgeschakeld: geen strongholds en geen End-portal.
- End-items (elytra, dragon-loot) zijn bewust niet nodig in de game.
- **Uitzondering: shulker boxes blijven beschikbaar**, via een alternatieve bron voor shulker shells (zie Open vragen).

## Ringbewakers en grenzen
- Er zijn 4 grenzen (1→2, 2→3, 3→4, 4→5). Elke grens heeft **4 bewakersstructures**, ongeveer in de vier windrichtingen, met per grens een seed-afhankelijke draaiing.
- Een **bewakerskompas** (werktitel) wijst naar de dichtstbijzijnde bewakersstructure van de huidige ring. Het doel wordt server-side berekend.
- Een grens is gesloten totdat de speler de bewaker van die grens heeft verslagen.
- Bewakers zijn opnieuw te bevechten, opgeroepen bij hun structure. Iedereen die binnen een bepaalde straal meevecht krijgt de kill-credit.

## Herobrines domein
- **4 kastelen** in ring 5, één per windrichting.
- De final battle vindt **maar één keer** per server plaats, met altijd één groep tegelijk.
- Zolang een gevecht actief is, kan er geen tweede starten (vangnet, server-side vlag).

## Post-game (na het verslaan van Herobrine)
- De corruptie trekt zich terug uit de wereld (sfeer, lucht, fog, events, verspreiding).
- De andere kastelen en Herobrines onderdelen vervallen tot ruïnes, als teken dat je in de post-game zit.
- Idee: een post-game quest met een speciale final boss (later uit te werken).

## Progressie (server-breed én per speler)
- **Per speler:** geopende grenzen, verslagen bewakers, quests, lore.
- **Per server:** wereldcorruptie, "Herobrine verslagen", actief final-gevecht, grote wereld-events.

## Per ring uit te werken (sjabloon)
- Terrein en sfeer (hoogte, vorm, lucht, fog, kleuren)
- Biomes binnen de ring (varianten)
- Unieke blokken (bouwblokken, natuurblokken, decoratie)
- Ores en materialen (wat, op welke diepte, waarvoor nodig)
- Grotten (vorm, sfeer, gevaren)
- Weer en omgevingscondities (en de mechanics die daarbij horen)
- Horror-elementen
- **Nether-tegenpool:** thema, terrein, blokken, sfeer, sleutelitems voor progressie
- (Fase 2) Items en gear die hier gevonden of gemaakt worden
- (Fase 3) Structures (klein / middel / groot, loot), overgang naar de volgende ring, bewakersstructure
- (Fase 4) Mobs en bewaker

## Fasering
Elke fase start met een sparringsessie (vragenrondes) in Claude Code; beslissingen gaan naar `02-beslissingenlog.md` en `fase-N-*.md`.
1. **Fase 1 — Wereld:** world preset (overworld + Nether), `ringAt`, ringverdeling, terrein, biomes, grotten, ores, custom blocks, weer per ring, omgevingscondities per ring en de mechanics die daarbij horen
2. **Fase 2 — Items en tools:** tools, gear, items en recipes per ring
3. **Fase 3 — Structures:** structures per ring en Nether-ring, loot, bewakersstructures, kastelen, vaste structure-posities
4. **Fase 4 — Mobs en bosses:** monsters per ring, ringbewakers, bosses
5. **Fase 5 — Progressie en corruptie:** quests, grenssysteem, bewakerskompas, wereldcorruptie, horror-events, Herobrine, final battle, post-game

Werkcategorieën (één per change): `worldgen`, `blocks`, `textures`, `atmosphere`, `conditions`, `items`, `structures`, `mobs`, `progression`, `corruption`, `infra`.

## Werkwijze in de repo
- `CLAUDE.md` bevat de werkafspraken; `CODEBASE.md` is de kaart van de repo en wordt altijd eerst gelezen.
- Elke change: één categorie, afgesloten met `/code-review`.

## Vastgelegde beslissingen
- Minecraft 1.21.1, NeoForge; modnaam The Corrupted Adventure
- Eigen world preset; multiplayer en dedicated servers zijn een must
- Ringbreedtes zoals in de tabel (startwaarden)
- 4 bewakersstructures per grens, 4 kastelen in ring 5
- Progressie deels per speler, deels per server
- Final battle één keer per server, één groep tegelijk
- Na de overwinning: corruptie verdwijnt, kastelen worden ruïnes, post-game
- The End uit; End-items niet nodig, behalve shulker boxes
- Nether is een tussenwereld met tegenpool-ringen, coördinatenschaal 4
- Vijf fases: wereld → items/tools → structures → mobs/bosses → progressie/corruptie
- Eén werkcategorie per change; sparring bij elke fase en bij elke onduidelijke keuze; `/code-review` na elke change

## Open vragen
- Bron van shulker shells: welke ring/Nether-ring en hoe (loot, bewaker-drop, recipe)?
- Hoe hard is een gesloten grens? (voorstel: zachte muur)
- Post-game boss: wie of wat?
