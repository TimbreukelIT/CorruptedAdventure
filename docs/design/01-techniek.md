# The Corrupted Adventure — Technische afspraken
Technische architectuur en afspraken. De werkafspraken voor Claude Code staan in `CLAUDE.md`; de kaart van de repo in `CODEBASE.md`.

## Stack
- Minecraft 1.21.1, NeoForge 21.1.x (Java 21, Gradle, NeoForge MDK / ModDevGradle)
- Mod-ID: `corruptedadventure`
- Package: `nl.timtoys.corruptedadventure`
- Registries via `DeferredRegister`; namen in snake_case (`scorched_sandstone`, `ash_ore`)
- Assets en data zoveel mogelijk via **datagen** (blockstates, models, loot tables, tags, recipes, lang)

## World preset
- Eigen preset: `corruptedadventure:corrupted_world`
- Singleplayer: te kiezen in het menu "Create World → World Type"
- Server: `level-type=corruptedadventure:corrupted_world` in `server.properties`
- De preset definieert een eigen overworld én een eigen Nether (beide met de ring-BiomeSource). De vanilla-presets blijven onaangetast.

## Ringarchitectuur (fase 1)
- Eigen **BiomeSource** die de ring bepaalt op basis van afstand tot (0,0), met ruis op de grens, zodat ringen geen perfecte cirkels zijn.
- Binnen een ring bepalen noise-parameters welke biome-variant verschijnt. Ring 1 gebruikt eigen kopieën van de vanilla biomes (datagen, zonder stronghold-tag) met de vanilla-klimaatparameters.
- Overgangszones (~128 blokken, grensruis ±96) tussen ringen met eigen overgangsbiomes.
- Terrein per ring via één eigen density function `corruptedadventure:ring_blend`: kiest per positie (via `ringAt`) de JSON-density function van de ring en mengt in de overgangszone. Er is één noise settings per dimensie; al het overige terrein blijft JSON.
- Buitenrand: `ring_blend` heeft een aparte void-invoer (zwevende eilanden → leegte, 9.700 – ~10.200 sinds de sparring ring 4; was 9.500–10.000). World border op de void-rand (~10.200, was 10.000), server-side gezet bij het aanmaken van de wereld (alleen zolang hij nog op de vanilla-standaard staat). De Nether-border (~2.550, was 2.500) wordt bij elke start afgeleid als overworld ÷ `coordinate_scale`, omdat vanilla alleen het midden schaalt.
- Chunk-generator `corruptedadventure:ring_noise` (subclass van vanilla `noise`) met een ringbewuste globale vloeistof-kiezer en een wrapper om de aquifer van elke noise chunk: geen zee- of aquifer-vloeistof in de void. Beide zijn niet data-driven; twee access-transformer-regels openen de velden.
- Grotten via carvers en density functions per ring; ores via placed features per biome.
- Ringgrenzen staan in één centrale config. Bij het aanmaken van een wereld worden de waarden als snapshot in die wereld vastgelegd (vóór de eerste chunk); daarna gebruikt `ringAt` alleen die snapshot, zodat een latere config-wijziging geen breuklijnen geeft. De server synct de snapshot naar de client. Er is één functie `ringAt(dimension, x, z)` die door worldgen, condities, het grenssysteem en het kompas wordt gebruikt.

## Nether (fase 1)
- Eigen Nether-dimension type (`corruptedadventure:corrupted_nether`) met **`coordinate_scale` 4** en een BiomeSource met dezelfde ringlogica.
- `ringAt` rekent Nether-coördinaten om met factor 4, zodat de grenzen exact overeenkomen met de overworld. Nether-ringen zijn daardoor ongeveer 375–500 blokken breed.
- Portals landen automatisch in de overeenkomstige ring.
- Hoogtelimiet van de Nether: bedrock-plafond op Y 128. Structures plat en breed houden.

## Weer, sfeer en omgevingscondities (fase 1)
- **Weer per ring:** vanilla regen/onweer blijft (alleen zichtbaar in biomes met neerslag). Daarnaast een eigen weercyclus per ring (niet per regio), in `SavedData` per dimensie, die server-side leeft en naar de client wordt gesynct. De client rendert alleen (lucht, fog, neerslag, particles) in de `client`-package.
- **Sfeer:** lucht-, fog- en waterkleuren zoveel mogelijk via biome-data (data-driven); alleen afwijkend gedrag (bijv. glitch-fog in ring 5) via client-code.
- **Omgevingscondities** (hitte, gif, void-effecten): per conditie een meter 0–100 per speler (player attachment). Een server-side tick-handler (elke 20 ticks) bepaalt via `ringAt`, biome en ringweer of de meter oploopt of afneemt; drempels geven effecten (config). De waarde wordt bij verandering gesynct voor een HUD-balk (client-only). Geen condities berekenen op de client. Nether-condities alleen in Nether-3 en Nether-5.
- Condities hebben een tegenmaatregel (gear, item of blok) die in fase 2 komt; in fase 1 alleen de conditie zelf, met config-waarden voor sterkte.
- In Nether-4 zakken alle meters extra snel (zuiverend); een regel in dezelfde tick-handler.
- **Client-effecten uit meters** (glitch-visuals bij void, volume-demping en HUD verbergen bij stilte, vanilla `ticksFrozen` bij kou): de server bepaalt de meter, de client leest alleen de gesyncte waarde en rendert. `ticksFrozen` wordt door vanilla zelf gesynct.
- **Server-side ringregels** buiten de meters: water verdampt bovengronds in ring 3 (alle plaatsingsroutes: emmer, dispenser, ijs), geluid-detectie voor stilte via vanilla `GameEvent`s, korte void-teleports met een veilige-plek-check.
- **Kettingreacties** (moerasgas) altijd via scheduled ticks met een cap per tick, nooit recursief in één tick.

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
- Besloten in fase 1: corrupted blokken zijn aparte varianten van vanilla-natuurblokken, met tag `corruptedadventure:corrupted` en een data map `corrupted → vanilla` (de gezonde tegenhanger). Fase 5 voegt alleen het random-tick-gedrag toe dat die map leest.

## Multiplayer-regels (vanaf dag 1)
- Alle gameplaylogica draait server-side; de client toont alleen.
- Client-only code (renderers, schermen, fog, sky, weer-rendering, particles) strikt gescheiden (`Dist.CLIENT`, aparte client-classes). Een dedicated server mag nooit client-classes laden.
- Synchronisatie via `CustomPacketPayload` + `StreamCodec` (1.21-netwerk-API).
- **Netwerkversie (change 18):** eigen `ModNetwork.PROTOCOL_VERSION`, verplicht voor de client (niet `optional()`); ophogen bij elke wijziging aan een payload of codec en bij elke nieuwe payload. Een client met een andere versie krijgt NeoForge's disconnectmelding.
- **Clientbound handlers (change 18):** common code noemt nooit een class uit `client/` (ook niet in een lambda of method reference). Payload + codec in `network/`, handler in `client/network/`, koppeling via de dispatch-tabel `ClientboundHandlers` die `CorruptedAdventureClient` vult. `ClientIsolationTest` bewaakt de scheiding.
- **Ringen op de client (change 18):** de client krijgt bij login de ringgrenzen + een **ruis-seed** = SHA-256(wereldseed ‖ `corruptedadventure:ring_boundary`), nooit de wereldseed, en rekent daarmee exact dezelfde `ringAt` als de server (`ClientRingLayout`; client-code gebruikt nooit `RingLayout.current()`).
- **Ringwereld-check:** ringsystemen (layout-sync, later meters en ringweer) draaien alleen als `RingWorld.isActive(server)`; een wereld met een andere preset blijft vanilla.
- Persistente staat via `SavedData` (wereld: weer, corruptie, Herobrine verslagen, actief gevecht) en attachments (speler: unlocks, quests).
- Elke feature wordt ook getest met `runServer` + client, niet alleen singleplayer.

## Testen
- `gradlew runData` na elke wijziging aan datagen
- `gradlew runClient` voor visuele checks, `gradlew runServer` voor servercompatibiliteit
- Worldgen-test: nieuwe wereld met vaste seed, teleporteren naar elke ring (overworld en Nether) en naar elke vaste structure-positie; portal-test vanuit elke ring
