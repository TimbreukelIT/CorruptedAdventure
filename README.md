# The Corrupted Adventure

Een avontuur-, horror- en survivalmod voor Minecraft 1.21.1 (NeoForge) rond Herobrine. Reis vanaf spawn naar buiten door vijf steeds vijandigere ringen tot aan Herobrines domein.

## Snel starten

Vereist: JDK 21.

```bash
./gradlew build        # bouwen
./gradlew runClient    # client starten
./gradlew runServer    # dedicated server starten
./gradlew runData      # datagen draaien (output in src/generated/resources)
```

De wereld gebruikt een eigen world preset. Singleplayer: *Create World → World Type → The Corrupted Adventure*. Server: `level-type=corruptedadventure:corrupted_world` in `server.properties` (zodra de preset bestaat).

## Documentatie

- `CLAUDE.md` — werkafspraken voor Claude Code
- `CODEBASE.md` — kaart van de repo
- `docs/design/` — ontwerp (bron van waarheid)
- `docs/backlog.md` — zaken voor later

Gebaseerd op de [NeoForge MDK 1.21.1 (ModDevGradle)](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).
