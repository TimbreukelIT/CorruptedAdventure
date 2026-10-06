# Golf 1 — stroom I (infra): 37d terreinkaart-debugcommando

Scope: `infra` — werkpakket 37d; branch `ring3/infra-g1`. Merget als eerste in integratie 1.

**Doel:** de gebruiker en de stromen kunnen het ring 3-terrein beoordelen zonder te vliegen. Het commando `/ca terrainmap` schrijft PNG's (hoogte, biome, verticale doorsnede) uit de echte density functions en biome-source. Teststop 4a begint met deze kaarten (backlog r. 71; werkwijze change 23 in `CODEBASE.md`, valkuil "Terreinkaarten zonder chunks").

## Werk
1. `/ca terrainmap <x> <z> <straal> [schaal]` en `/ca terrainmap section <x1> <z1> <x2> <z2>` (alleen OP's, vaste Engelse tekst zoals de andere `/ca`-commando's).
   - Draait op een eigen thread en raakt geen chunks: named DF's via `registryAccess`, `mapAll`-bedrading zoals in de valkuil beschreven.
   - Biome via `getNoiseBiome(…, randomState.sampler())`.
2. **Uitvoer:** `<wereld>/corruptedadventure/maps/<naam>.png`.
   - Hoogtekaart met kleurverloop en contourlijnen elke 16 blokken, Y 192 (wolken) gemarkeerd.
   - Biomekaart met vaste kleuren per biome (ring 3 en Nether-3 erbij) en de ringgrenzen.
   - Doorsnede: steen/lucht/vloeistof, met de eilanden zichtbaar.
3. Voortgang en tijd per kolom in de chat; een cap op de grootte (bijv. 2.048 × 2.048 bij schaal 4), zodat een server niet vastloopt (`max-tick-time` niet nodig: niet op de main thread).
4. Ook voor de Nether (`/execute in minecraft:the_nether run ca terrainmap …`).

## Client/server-impact
Alleen server (commando + bestand); geen client-classes.

## Tests
- **B**, **S** (RCON): kaart van ring 2 (bekend terrein) + `dry_riverbeds`; tijd per kolom; een tweede run geeft een identieke PNG (md5).
- Een korte uitleg in de delta voor `CODEBASE.md`.

## Bij afronding
`waves.md` §4.
