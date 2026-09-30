# The Corrupted Adventure — Technische afspraken
Technische architectuur en afspraken. De werkafspraken voor Claude Code staan in `CLAUDE.md`; de kaart van de repo in `CODEBASE.md`.

## Stack
- Minecraft 1.21.1, NeoForge 21.1.x (Java 21, Gradle, NeoForge MDK / ModDevGradle)
- Mod-ID: `corruptedadventure`
- Package: `nl.corruptedadventure`
- Registries via `DeferredRegister`; namen in snake_case (`scorched_sandstone`, `ash_ore`)
- Assets en data zoveel mogelijk via **datagen** (blockstates, models, loot tables, tags, recipes, lang)

## World preset
- Eigen preset: `corruptedadventure:corrupted_world`
- Singleplayer: te kiezen in het menu "Create World → World Type"
- Server: `level-type=corruptedadventure:corrupted_world` in `server.properties`
- De preset definieert een eigen overworld én een eigen Nether (beide met de ring-BiomeSource). De vanilla-presets blijven onaangetast.

## Ringarchitectuur (fase 1)
- Eigen **BiomeSource** die de ring bepaalt op basis van afstand tot (0,0), met ruis op de grens, zodat ringen geen perfecte cirkels zijn.
- Binnen een ring bepalen noise-parameters welke biome-variant verschijnt.
- Overgangszones tussen ringen met eigen overgangsbiomes.
- Terrein per ring via density functions en noise settings (JSON), met Java alleen waar data-driven niet volstaat.
- Grotten via carvers en density functions per ring; ores via placed features per biome.
- Ringgrenzen staan in één centrale config/constante. Er is één functie `ringAt(dimension, x, z)` die door worldgen, condities, het grenssysteem en het kompas wordt gebruikt.

## Nether (fase 1)
- Eigen Nether-dimension type (`corruptedadventure:corrupted_nether`) met **`coordinate_scale` 4** en een BiomeSource met dezelfde ringlogica.
- `ringAt` rekent Nether-coördinaten om met factor 4, zodat de grenzen exact overeenkomen met de overworld. Nether-ringen zijn daardoor ongeveer 375–500 blokken breed.
- Portals landen automatisch in de overeenkomstige ring.
- Hoogtelimiet van de Nether: bedrock-plafond op Y 128. Structures plat en breed houden.

## Weer, sfeer en omgevingscondities (fase 1)
- **Weer per ring:** de weerstaat (per ring of per regio) leeft server-side en wordt naar de client gesynct. De client rendert alleen (lucht, fog, neerslag, particles) in de `client`-package.
- **Sfeer:** lucht-, fog- en waterkleuren zoveel mogelijk via biome-data (data-driven); alleen afwijkend gedrag (bijv. glitch-fog in ring 5) via client-code.
- **Omgevingscondities** (hitte, gif, void-effecten): een server-side tick-handler (bijv. elke 20 ticks) bepaalt via `ringAt` en biome wat een speler ondervindt en past effecten toe. Geen condities berekenen op de client.
- Condities hebben een tegenmaatregel (gear, item of blok) die in fase 2 komt; in fase 1 alleen de conditie zelf, met config-waarden voor sterkte.

## The End uitschakelen
- Eigen biomes krijgen geen `has_structure/stronghold`-tag, dus er genereren geen strongholds en er ontstaat geen End-portal.
- De End-dimensie blijft technisch in de preset staan (weglaten kan onverwacht gedrag geven), maar is onbereikbaar.
- Shulker boxes blijven beschikbaar: de vanilla-blokken en het recipe bestaan nog; alleen de bron van `shulker_shell` wordt vervangen via loot tables en/of een recipe (data-driven, geen Java nodig).

## Structures en vaste posities (fase 3)
- Structures via het jigsaw-systeem (template pools) waar mogelijk.
- Bewakersstructures (4 per grens) en kastelen (4 in ring 5) komen op berekende posities: een hoek (windrichting + seed-offset) en een radius.
- Implementatie: een eigen `StructurePlacement`-type dat precies deze posities teruggeeft. Het kompas en `/locate` gebruiken dezelfde berekening.
- Het terrein rond deze posities moet vlak genoeg zijn: terrain adaptation (beard) of een eigen fundering.

## Grenzen afdwingen (fase 5, voorstel)
- Server-side check op spelerpositie (elke 10–20 ticks), in de overworld én in de Nether: staat de speler in een ring waarvoor hij de grens nog niet geopend heeft, dan krijgt hij oplopende effecten (darkness, schade, terugduwen).
- Werkt tegen alle omwegen (ender pearls, boten, teleport, portals), omdat het naar positie kijkt.
- Unlock-status per speler in een player attachment; sync naar de client alleen voor HUD/fog.

## Corruptie en post-game (fase 5)
- Onderscheid tussen **statische corruptie** (het landschap van ring 5, gegenereerd, blijft bestaan) en **dynamische corruptie** (verspreiding, decoratie, sfeer, events).
- Blokken die moeten vervallen of genezen (kasteelblokken, corrupted varianten) krijgen **random ticks**. Bij een tick controleren ze de server-vlag "Herobrine verslagen" en vervallen ze tot een ruïne- of gezonde variant.
- Voordeel: geen dure chunk-scans; de verandering gebeurt vanzelf in gebieden waar spelers komen. Werkt volledig server-side.
- Sfeer (lucht, fog, muziek) leest dezelfde vlag via sync naar de client.
- Tip voor fase 1: blokken die later moeten vervallen of genezen, kunnen nu al als aparte "corrupted"-variant worden ontworpen, zodat fase 5 alleen gedrag toevoegt.

## Multiplayer-regels (vanaf dag 1)
- Alle gameplaylogica draait server-side; de client toont alleen.
- Client-only code (renderers, schermen, fog, sky, weer-rendering, particles) strikt gescheiden (`Dist.CLIENT`, aparte client-classes). Een dedicated server mag nooit client-classes laden.
- Synchronisatie via `CustomPacketPayload` + `StreamCodec` (1.21-netwerk-API).
- Persistente staat via `SavedData` (wereld: weer, corruptie, Herobrine verslagen, actief gevecht) en attachments (speler: unlocks, quests).
- Elke feature wordt ook getest met `runServer` + client, niet alleen singleplayer.

## Testen
- `gradlew runData` na elke wijziging aan datagen
- `gradlew runClient` voor visuele checks, `gradlew runServer` voor servercompatibiliteit
- Worldgen-test: nieuwe wereld met vaste seed, teleporteren naar elke ring (overworld en Nether) en naar elke vaste structure-positie; portal-test vanuit elke ring
