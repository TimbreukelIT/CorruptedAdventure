# CLAUDE.md — The Corrupted Adventure

NeoForge-mod voor Minecraft 1.21.1: avontuur/horror/survival rond Herobrine. De wereld bestaat uit 5 concentrische ringen (vanilla → droog/canyons → vulkanisch → moeras → corrupted) plus een Nether met tegenpool-ringen. Communiceer in het Nederlands; code, bestandsnamen, registry-namen en commits in het Engels.

---

## 1. Altijd eerst lezen (token-zuinig werken)

Lees in deze volgorde, en stop zodra je genoeg weet:

1. **`CODEBASE.md`** — de kaart van de repo: waar wat staat, welke classes wat doen, welke registries en databestanden er zijn. **Dit is je eerste bron, altijd.**
2. **Het design-document voor de huidige scope** in `docs/design/` (zie tabel onderaan). Dit is de bron van waarheid voor *wat* we bouwen.
3. Pas daarna: gericht de bestanden openen die `CODEBASE.md` aanwijst.

Regels:
- **Geen Explore-agents of brede zoekacties** zolang `CODEBASE.md` het antwoord geeft. Zoek alleen gericht (Grep/Glob op een naam) als de kaart tekortschiet.
- Klopt `CODEBASE.md` niet met de werkelijkheid, of mist er iets: **werk het direct bij** in dezelfde change.
- Lees geen bestanden in `src/generated/` om te begrijpen hoe iets werkt; lees de datagen-provider die ze maakt.

---

## 2. Scope: één categorie per change

We werken altijd binnen **precies één categorie**. Een sessie of change raakt geen andere categorie aan.

| Categorie | Omvat |
|---|---|
| `worldgen` | world preset, dimensies, BiomeSource, `ringAt`, biomes, terrein (density functions, noise settings), carvers, ores/features |
| `blocks` | custom blocks, blockstates, loot tables, tags, block-gedrag (random ticks e.d.) |
| `textures` | textures en models (blocks, items, entities), kleuren, animaties |
| `atmosphere` | weer per ring, lucht, fog, kleuren, ambient sounds, particles |
| `conditions` | omgevingscondities per ring (hitte, gif, void-effecten) en de bijbehorende mechanics |
| `items` | tools, gear, items, recipes, loot-integratie |
| `structures` | structures, jigsaw-pools, vaste structure-posities (bewakers, kastelen) |
| `mobs` | monsters, bewakers, bosses, AI, spawning |
| `progression` | quests, grenssysteem, bewakerskompas, unlocks, lore |
| `corruption` | wereldcorruptie, horror-events, Herobrine, final battle, post-game |
| `infra` | build, Gradle, config, netwerk-basis, SavedData/attachments, tooling |

Regels:
- Benoem aan het begin van elke change de categorie: *"Scope: `worldgen`"*.
- Kom je iets tegen uit een andere categorie (een bug, een idee, een benodigdheid): **niet bouwen**. Zet het in `docs/backlog.md` met categorie en één regel uitleg, en ga verder.
- Heeft een change echt iets uit een andere categorie nodig om te werken (bijv. een placeholder-blok voor worldgen), gebruik dan een **minimale placeholder** en noteer dat in de backlog.

---

## 3. Sparring: vragen stellen vóór beslissen

Deze mod wordt samen ontworpen. Claude beslist ontwerpkeuzes nooit stilzwijgend.

- **Start van elke fase:** eerst een sparringsessie. Geen code, alleen vragen en voorstellen, tot de fase-scope helder is. Leg de uitkomst vast (zie hieronder) voordat er gebouwd wordt.
- **Start van elke grotere feature binnen een fase:** korte vragenronde over de open punten.
- **Tijdens het werk:** komt er een keuze langs die niet in de design-docs staat, of is iets onduidelijk? **Stop en vraag.** Niet aannemen, niet "voorlopig maar iets kiezen".
- Vraagvorm: gebruik `AskUserQuestion` met concrete opties (max. 4 vragen per ronde, meerdere rondes mag). Zet je aanbevolen optie bovenaan met *(aanbevolen)* en geef per optie de consequentie (complexiteit, multiplayer-impact, speelgevoel).
- Wees eerlijk over complexiteit en haalbaarheid in 1.21.1.

**Vastleggen:** elke beslissing gaat direct in:
1. het relevante design-document in `docs/design/` (de inhoud zelf), en
2. `docs/design/02-beslissingenlog.md` — één regel: datum, categorie, beslissing, korte reden.

---

## 4. Werkwijze per change

1. **Scope benoemen** (één categorie) en `CODEBASE.md` + design-doc lezen.
2. **Plan** in een paar regels: wat verandert er, welke bestanden. Bij twijfel: sparren (§3).
3. **Bouwen** in kleine, afgeronde stappen.
4. **Testen** (§8). Nooit een change afsluiten die niet compileert of `runData` breekt.
5. **`CODEBASE.md` bijwerken** met nieuwe/gewijzigde classes, registries en databestanden.
6. **Design-doc en beslissingenlog bijwerken** als er iets is besloten.
7. **Code review:** sluit elke change af met **`/code-review`**. Kan Claude die zelf niet starten, meld dan expliciet: *"Klaar voor `/code-review`"* en wacht. Los de bevindingen op (of leg vast waarom niet) vóór de commit.
8. **Commit** met een Engelse message, prefix met de categorie: `worldgen: add ring biome source`.

---

## 5. Fasering

| Fase | Inhoud | Hoofdcategorieën |
|---|---|---|
| **1 — Wereld** | world preset (overworld + Nether), `ringAt`, ringverdeling, terrein, biomes, grotten, ores, custom blocks, weer per ring, omgevingscondities per ring en de mechanics die daarbij horen | `worldgen`, `blocks`, `textures`, `atmosphere`, `conditions` |
| **2 — Items en tools** | tools, gear, items en recipes per ring; materialen uit fase 1 krijgen hun doel | `items`, `textures` |
| **3 — Structures** | structures per ring en per Nether-ring, loot, bewakersstructures en kastelen als gebouw, vaste structure-posities | `structures`, `blocks` |
| **4 — Mobs en bosses** | monsters per ring, ringbewakers, bosses | `mobs`, `textures` |
| **5 — Progressie en corruptie** | quests, grenssysteem, bewakerskompas, wereldcorruptie, horror-events, Herobrine, final battle, post-game | `progression`, `corruption` |

Een fase begint altijd met een sparringsessie (§3). Werk niet vooruit in een latere fase.

---

## 6. Stack en architectuur

Details: `docs/design/01-techniek.md`. Samenvatting:

- Minecraft 1.21.1, NeoForge 21.1.x, Java 21, Gradle met ModDevGradle.
- Mod-ID `corruptedadventure`, package `nl.timtoys.corruptedadventure`.
- Registries via `DeferredRegister`; namen in snake_case (`scorched_sandstone`, `ash_ore`).
- Eigen world preset `corruptedadventure:corrupted_world` met eigen overworld en eigen Nether (`coordinate_scale` 4). The End is onbereikbaar (geen strongholds).
- **Eén ringfunctie:** `ringAt(dimension, x, z)` in de `ring`-package. Worldgen, grenssysteem, kompas en condities gebruiken allemaal deze functie. Ringgrenzen staan alleen in config, nergens anders hardcoded.
- Data-driven waar het kan (JSON worldgen, datagen voor assets/tags/loot/recipes), Java waar het moet (BiomeSource, StructurePlacement, gedrag).

---

## 7. Harde regels: multiplayer en dedicated server

Dit is een eis, geen wens. Een change die dit breekt is niet af.

- Alle gameplaylogica draait **server-side**. De client toont alleen.
- Client-only code (renderers, schermen, fog, sky, weer-rendering, particles) staat in de `client`-package en wordt alleen geladen via `Dist.CLIENT`. **Een dedicated server laadt nooit een client-class.** Geen `Minecraft.getInstance()` buiten `client/`.
- Weer en condities: de **staat** leeft op de server en wordt gesynct; de client rendert alleen.
- Netwerk via `CustomPacketPayload` + `StreamCodec`.
- Persistente staat: `SavedData` voor wereld/server, attachments voor spelers.
- Geen random zonder seed in worldgen; worldgen moet deterministisch zijn per seed.
- Benoem in je plan altijd kort de client/server-impact van een change.

---

## 8. Testen

- `./gradlew build` — moet slagen.
- `./gradlew runData` — na elke wijziging aan datagen; commit de gegenereerde output.
- `./gradlew runClient` — visuele checks.
- `./gradlew runServer` — **verplicht** bij elke change die gameplay, netwerk of client/server-code raakt: de server moet opstarten zonder client-class-fouten en een client moet kunnen joinen.
- Worldgen-changes: nieuwe wereld met vaste testseed, teleporteer naar elke ring (overworld én Nether) en test een portal vanuit elke ring.

---

## 9. Conventies

- Geen handmatige edits in `src/generated/`; alles daar komt uit datagen.
- Handgeschreven worldgen-JSON (noise settings, density functions, dimension types, preset) staat in `src/main/resources/data/corruptedadventure/worldgen/` en dergelijke.
- Registry-namen: Engels, snake_case, ring-thema herkenbaar (`ash_`, `swamp_`, `corrupted_`).
- Lang-keys via datagen; Engelse naam verplicht, Nederlandse vertaling (`nl_nl`) welkom.
- Kleine classes met één verantwoordelijkheid; geen "Utils"-dumpbakken.
- Commentaar alleen waar het *waarom* niet uit de code blijkt.

---

## 10. Documenten

| Bestand | Inhoud |
|---|---|
| `CODEBASE.md` | Kaart van de repo — **altijd eerst lezen** |
| `docs/design/00-overzicht.md` | Visie, ringen, Nether, progressie, fasering |
| `docs/design/01-techniek.md` | Technische architectuur en afspraken |
| `docs/design/02-beslissingenlog.md` | Log van alle beslissingen |
| `docs/design/fase-N-*.md` | Uitwerking per fase (ontstaat na elke sparringsessie); fase 2: `fase-2-items.md` |
| `docs/backlog.md` | Zaken buiten de huidige scope |

## 11. Definition of done

- [ ] Eén categorie, benoemd
- [ ] `build` en `runData` slagen; `runServer` getest waar nodig
- [ ] Geen client-class bereikbaar vanaf server-code
- [ ] `CODEBASE.md` bijgewerkt
- [ ] Design-doc en beslissingenlog bijgewerkt (indien iets besloten)
- [ ] Buiten-scope-zaken in `docs/backlog.md`
- [ ] `/code-review` gedaan en bevindingen afgehandeld
- [ ] Commit met categorie-prefix
