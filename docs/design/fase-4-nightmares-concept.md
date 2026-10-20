# Fase 4 — Nightmares (concept)

**Status:** concept uit een vooruitblik-sparring (2026-10-20). Er wordt niets gebouwd vóór fase 4; de fase 4-sparring maakt dit definitief. Scope: `mobs`.

## Idee
Een groep **Nightmares**: één per ring (5 totaal), groter dan gewone mobs, bedoeld om kippenvel te geven. Rondzwervende overworld-ringbosses, ingezet vanuit Herobrines legioen.

- **Rolverdeling:** bewakers zijn *poortwachters op een vaste plek* (jij gaat naar hen toe); Nightmares zijn *dreiging die jou vindt*. Herobrine blijft de enige die "persoonlijk" is.
- **Rode draad:** Herobrines witte ogen als handtekening van het legioen. Elke Nightmare geeft betekenis aan iets wat fase 1 al in de wereld heeft gelegd.

## Beslissingen (sparring 2026-10-20)
1. **Los en optioneel** t.o.v. de bewakers: verslaan levert iets waardevols op, maar is geen progressie-eis.
2. **Eén unieke Nightmare per ring per server.** De server houdt een virtuele positie bij (`SavedData`) en laat hem bij spelers in zijn ring verschijnen; ook als zijn chunks niet geladen zijn, "bestaat" hij.
3. **Komt sterker terug** na verslaan (timer en/of gekoppeld aan wereldcorruptie, fase 5).
4. **Ring 1: volwaardig bossgevecht.** De Holle valt ook bovengronds actief aan; de stalk-opbouw blijft als aanloop.

## Rooster (werktitels, ter discussie)
| Ring | Nightmare | Haak in de bestaande wereld |
|---|---|---|
| 1 | **De Holle** (The Hollow) — nabootser | 2×1-tunnels, kale plekken, "Something stirs in the dark" |
| 2 | **De Luchtspiegeling** — gestalte in de hittetrilling | fata morgana, oases met villagers, uitdroging |
| 3 | **Het Askoor** — reus van samengesmolten as-afgietsels | as-afgietsels, lavabuizen, gestolen water |
| 4 | **Wat onder de bellen leeft** | backlog: "iets groots onder water (bij de bellen)", het silhouet is de voorbode |
| 5 | **De Onvoltooide** — half geladen, glitchende wachter | void, glitches, kastelen (niet overlappen met "beweegt als je niet kijkt") |

## Schets ring 1 — De Holle (werktitel)
**Uiterlijk:** ~4 blokken hoog, uitgemergelde Steve-proporties maar uitgerekt: te lange nek, armen tot de knieën, vingers als kale takken. Huid in het dood-hout-schorspalet (`3F3832 524A42 655C52 786E62 8B8174`, zie `fase-1-wereld.md` ring 1), dus familie van de kale plekken. Steve-vormig hoofd zonder gezicht, op twee witte pixel-ogen na (Herobrine), net te ver uit elkaar.

```
        ▄▄▄▄
       █ ·  ·█      <- glad hoofd, twee witte ogen
        ▀██▀
          ██        <- te lange nek
       ▄██████▄
      █ ██████ █
     █  ██████  █
    ▐   ██████   ▌  <- armen tot de knieën
    ▐    ████    ▌
   ╱╲    █  █    ╱╲ <- takvingers
         █  █
         █  █
        ▀▀  ▀▀
```

**Lore:** hij groef de onnatuurlijke 2×1-tunnels (spelergroot, want hij doet de mijnwerkers na die hij jaagt). Waar hij rust verdort het leven: de kale plekken zijn zijn slaapplaatsen.

**Opbouw van een ontmoeting:**
1. *Tekenen* — in grotten: voetstappen, pikhouweelgeluid, een kist die opengaat waar niemand is (jouw eigen geluiden).
2. *Stalken* — in de schemering staat hij roerloos tussen de bomen aan de rand van je zicht; kom je dichterbij, dan loopt hij achteruit het bos of een tunnel in. Verdwijnt niet bij kijken (dat is ring 5).
3. *Jacht* — ondergronds of 's nachts vouwt hij zich op en kruipt door een 2×1-tunnel naar je toe: iets van 4 blokken uit een gat van 1×2.
4. *Gevecht* — volwaardig bossgevecht, ook bovengronds (beslissing 4). Bossbar zodra het gevecht begint. Dooft fakkels in de buurt, grijpt spelers en sleept ze richting een tunnel, laat een kale plek achter waar hij vecht.

## Haalbaarheid in 1.21.1 / NeoForge
- **Goed te doen:** grote modellen (4–6 blokken), bossbar (`ServerBossEvent`, multiplayer-proof), eigen AI-goals, positionele geluiden server-side (vanilla-events met pitch, zoals change 32), particles en client-effecten via payload.
- **Lastig maar haalbaar:** pathfinding van grote mobs (vanilla-navigatie loopt vast in bossen en grotten → hitbox kleiner dan het model, of eigen beweging: kruipen, graven, zwemmen); de unieke, persistente Nightmare (virtuele positie in `SavedData`, pas materialiseren bij spelers); houdingwissel met hitbox (kruiphouding, zoals een zwemmende speler) plus eigen navigatie door 1×2.
- **Af te raden:** per-speler hallucinaties met één gedeelde entity (vereist client-only nep-entities); zware render-glitches/shaders (ring 5) → houd het bij model-swaps, stotter-teleports en geluid.
- **Client/server:** alle logica (positie, AI, aanvallen, fakkels doven, terugkeertimer) server-side; de client toont alleen model, animatie en effecten.

## Open voor de fase 4-sparring
- Balans van het ring 1-gevecht met ring 1-gear (ijzer/diamant): HP, schade, schaal met het aantal spelers.
- Terugkeer: interval, hoeveel sterker, wat er verandert (nieuwe aanval of fase).
- Beloning/drop per Nightmare (waardevol, niet verplicht).
- Zonsopgang: trekt hij zich terug of vecht hij door?
- Animatietechniek: GeckoLib (dependency, makkelijke complexe animaties) of vanilla keyframe-animaties (geen dependency, meer werk).
- Concepten ring 2–5 uitwerken; Nightmares in de Nether-ringen (ja/nee).
