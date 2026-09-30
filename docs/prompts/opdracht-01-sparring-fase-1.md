# Opdracht 1 — Sparringsessie Fase 1: Wereld

Plak dit als eerste bericht in Claude Code, in de root van de repo.

---

Scope: fase 1 (Wereld). Dit is een **sparringsessie: schrijf nog geen code.**

Lees eerst `CLAUDE.md`, `CODEBASE.md`, `docs/design/00-overzicht.md` en `docs/design/01-techniek.md`. Ga niet verder zoeken in de repo; die informatie is genoeg voor deze sessie.

**Doel van deze sessie**
Samen fase 1 zo concreet maken dat we daarna in kleine changes, elk binnen één categorie, kunnen bouwen. Fase 1 omvat:
- `worldgen`: world preset (overworld + Nether, schaal 4), `ringAt`, ringgrenzen met ruis en overgangszones, terrein per ring, biome-varianten, grotten, ores
- `blocks`: custom blocks per ring (natuur, bouwblokken, ores, decoratie)
- `textures`: aanpak voor textures (placeholders eerst of direct definitief, stijl)
- `atmosphere`: weer, lucht, fog, kleuren, geluid per ring
- `conditions`: omgevingscondities per ring (bijv. hitte in ring 3, gif in ring 4, void/glitches in ring 5) en de mechanics die daarbij horen
- De Nether-tegenpool van elke ring, voor zover het terrein, blokken en sfeer betreft

**Hoe**
- Stel je vragen in rondes met `AskUserQuestion` (max. 4 per ronde, concrete opties, aanbevolen optie bovenaan, per optie de consequentie voor complexiteit, multiplayer en speelgevoel).
- Begin met het **fundament**: de volgorde van bouwen, het kale skelet (preset + BiomeSource + `ringAt` + één placeholder-biome per ring), de breedte van overgangszones, en de aanpak voor textures.
- Werk daarna **ring voor ring** (1 t/m 5, telkens met Nether-tegenpool) door het sjabloon uit `00-overzicht.md`: terrein, biomes, blokken, ores, grotten, weer, condities, mechanics, horror-elementen. Structures en mobs slaan we over (fase 3 en 4); noteer ideeën daarvoor wel in `docs/backlog.md`.
- Doe ook voorstellen: kom met ideeën voor blokken, weer en condities, zodat ik kan kiezen en bijsturen.
- Benoem bij elk idee meteen of het client/server-risico's heeft.

**Vastleggen**
- Leg elke beslissing direct vast in `docs/design/02-beslissingenlog.md` (maak dit bestand aan).
- Schrijf de uitwerking naar `docs/design/fase-1-wereld.md`, met per ring een sectie volgens het sjabloon.
- Werk `00-overzicht.md` en `01-techniek.md` bij als een beslissing daar iets verandert.

**Afsluiting van de sessie**
Sluit af met een **bouwplan** onderaan `fase-1-wereld.md`: een genummerde lijst kleine changes, elk met één categorie, in de volgorde waarin we ze bouwen (beginnend met het skelet), en per change hoe we hem testen. Bouw daarna nog niets; ik geef zelf het startsein voor change 1.

We hoeven niet alles in één sessie af te ronden. Als het veel wordt, stop dan na een logisch punt (bijvoorbeeld na het fundament en ring 1–2), leg alles vast en zeg waar we de volgende keer verdergaan.
