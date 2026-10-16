# Golf 2 — stroom W-terrein: 73 tunnelnet

Scope: `worldgen`, branch `ring4/terrain-g2` (basis `main` d1a37f11, na `vf-perf2`), poort 1 (niet gebruikt: licht testen). Bron: `golf-2.md` §2 (W-terrein, Q5 = A, Q6 = A, Q10 = A, Q14 = A), `contract.md` §4, `waves.md` §4–§5, `golf-1-terrain.md`, `verdant-falls.md`. W-terrein-b (74) bouwt parallel in `ring4/terrain-g2b`; 73 raakt `IslandField` alleen in een eigen blok (zie CODEBASE-delta).

## 1. Plan (gebouwd)

**Vorm.** Eén nieuwe klasse `Ring4Tunnels` (in `IslandField`, beslissingen per wereld in `Decisions.tunnels`):
- Knooppunten op een raster van 128 (kans 0,8, ≥ 28 binnen de cel), hoogte = gladde basis (warp-ruis, ~500 blokken) + jitter; mythisch Y ~−12..20 ± 8, bedorven ~6..30 ± 6; altijd ≥ 11 + 6 dek onder de laagste hoek van het 4-raster rond de hal.
- Randen naar de oost- en zuidbuur (kans 0,62 elk): een gebogen middellijn (kwadratisch, ≤ 0,2 van de lengte opzij, zijwaartse golf 2–8), punten om de 3 blokken, per punt een halve breedte 2,5–6 (5–12 breed) en een halve hoogte 2,5–3,5 per rand; doorsnede = ellips in het verticale vlak, dus per kolom één open interval per tunnel (2D-dichtstbijzijnde stuk van de middellijn).
- Hallen (straal 7–11, rafelige omtrek ±20 %) op elk knooppunt met minstens één rand of ingang.

**Watermodel (Q5 = A).**
- **Mythisch, overstroomd tot zeeniveau:** een rand loopt van elk knooppunt eerst omlaag naar een dal, dan over toppen en dalen (top tot top ~44, dal tot top ≥ 20) naar het andere knooppunt. Elk knooppunt en elke top is dus een hoog punt: daar een **luchtkoepel** (hal: 4–6 lucht boven het waterniveau; top: tunnel + 1,5 breder, 3–5 lucht). Het waterniveau = het hoogste tunnelblok op dat punt (`floor(y + halve hoogte)`), vlak; binnen de omtrek ligt geen tunnelblok hoger (monotoon tot het volgende dal). Een koepel waarvan het water ≥ Y 62 zou staan is overbodig (daar is het al zee). Swimmen: ≤ ~44 blokken tussen twee luchtkoepels.
- **Bedorven, droog:** een rand stijgt van beide knooppunten naar een top in het midden (elk knooppunt een laag punt). Hallen: **gifpoel** 35 % (niveau onder elke tunnelvloer daar, 2–3 diep, alleen bronnen; niet bij een gemengde rand), **moerasgaskamer** 30 % (de hal vol `swamp_gas`), anders kaal.
- **Bandovergang (gesloten overgang):** een rand tussen een overstroomd en een droog knooppunt zakt naar een dal in het midden en wordt daar afgesloten door een prop van ≥ 6 blokken steen (twee doodlopende einden).
- **Afgesloten ruimtes** (koepels, het droge net, droge zinkgaten): vanilla-grotten dicht in een schil eromheen (`cave_lift` 2, 8 opzij en 12 boven/onder: het 4 × 8 × 4-raster van de stapel), carvers mogen er niet snijden (`RingAquifer`, `carving`).
- **Scheiding:** een droog element binnen 4 blokken van een overstroomd element valt weg; een koepel binnen 4 van een overstroomd element van een andere rand verliest zijn lucht (hal: water tot Y 62; top: weg); een gifpoel binnen 4 van een droog element van elders wordt een kale hal. Getoetst tegen de elementen van 5 × 5 cellen (vóór hun eigen toets, dus conservatief en zonder volgorde).
- **Rest van ring 4 (eigen floodedness, backlog r. 89, 290 (a) (b), 320, 321, `golf-1-terrain.md` r. 150):** alle andere open ruimte in ring 4 (ook de ring 4-helft van zone 3→4, niet zone 4→5) is water tot en met Y 62 en lucht erboven, stil (`RingFluidPicker.ring4Flooded`), i.p.v. vanilla's lokale aquiferniveaus; één niveau, dus geen watergordijnen meer, en grotten onder zee lopen vol. Steenlaag op Y −54 en droge bodem eronder blijven.

**Ingangen en zones (Q6 = A).**
- **Gatmeren op zeeniveau:** een overstroomd knooppunt (kans 0,75) zoekt het dichtstbijzijnde eilandgat 48–110 verder zonder eigen meer (de zee vult het) met bodem ≤ Y 56, buiten de gesloten band van elk meer boven zee; de tunnel zakt eerst naar een dal en stijgt dan (gem. ≤ 0,75) tot hij 3 blokken in het water van het gat opent.
- **Droge zinkgaten:** een veen-zinkgat is **droog** als zijn bodem onder zee ligt, de grond in drie ringen (r + 1,5, r + 4, r + 7, 16 punten) ≥ Y 64,5 staat, het ≥ 32 van de zones ligt en geen overstroomd element in de buurt komt; de kom is dan lucht van de bodem tot Y 62 (anders blijft hij zoals nu: zeewater). Een droog knooppunt (kans 0,75) stijgt (gem. ≤ 0,5) naar het dichtstbijzijnde droge zinkgat 48–110 verder; de tunnelvloer komt op de kombodem uit (oplopen).
- Geen ingang onder een meer boven zee; het dek (6) geldt overal behalve in het eigen gat/de eigen kom (+ 6 rond de kom, die de ringen al toetsen).
- **Zones:** open ruimte en schil ≥ 32 (effectieve afstand) van zone 3→4 en 4→5; in een chunk buiten die band wordt niets berekend (`elementsOfChunk` geeft meteen leeg).

**Uitgangen.**
- `thin` opent de tunnelblokken exact per blok (−1); `cave_lift` sluit de grotten in de schil; de aquifer vult (`Ring4Tunnels.Span`): water/koepellucht, droge lucht, `swamp_gas`, `toxic_pool` (bron), droge zinkgatlucht; nooit laten stromen.
- DF `ring_4/overworld/tunnels` (per blok, `island_output tunnels`): 0 niet in het net, 1 water, 2 lucht in het overstroomde net (koepel), 3 droge lucht, 4 gas, 5 gifpoel. Niet in de terreinstapel.
- **Accessor voor W-biomes (77):** `IslandField.tunnelsWithin(box)` (middellijnen: punt, hoogte, halve breedte; vulling; halve hoogte), `tunnelRoomsWithin(box)` (hallen en koepels: vulling, hal of top, midden, straal, laagste/hoogste blok, niveau) en `tunnelFill(x, y, z)`.

**Niet in 73 (golf-2.md):** aquamarijn-geodes en gifkristal in de wanden (77), oppervlaktegas onder de veenkorst en in zinkgatbodems (76), sporen en wortels (77/75).

**verdant_falls:** niets in de zone; de afdaling, kloven, voetrivier, randmeren, grotband, `verdantWater`, `ring3Seal` en dammen ongemoeid. Wel: diepe vanilla-grotten in de ring 4-helft van zone 3→4 onder Y 63 zijn nu water tot Y 62 (backlog r. 320), na alle `verdant`-regels (nissen/tunnels/richelpad blijven lucht).

## 2. Client/server-impact

Alles worldgen op de server; geen client-class, geen payload, geen `PROTOCOL_VERSION`. Gas en gifpoel zijn de blokken van 72 (`ModBlocks.SWAMP_GAS`, `TOXIC_POOL`), opgezocht bij het eerste gebruik in de aquifer.

## 3. Prestatie (verwacht; meten bij INT)

- Per blok in ring 4: in `thin` en `cave_lift` twee vergelijkingen tegen de omhullende van de kolom (een lege kolom: geen).
- Per kolom: één `KeyedSlots`-opzoeking van de elementlijst van de chunk; bij een tunnel een gang over de middellijnstukken binnen het vak (~50 punten, met vakfilter).
- Per wereld: per cel van 128 de beslissingen (gemeten: 460 cellen in ~0,75 s op één thread incl. JIT, ~1,6 ms per cel ≈ 25 µs per chunk). De oppervlakte langs een rand wordt op het 4-raster gelezen (met een lokale cache per beslissing).
- De aquifer is in ring 4 goedkoper dan vanilla (geen ruis per open blok meer).

## 4. Tests (licht, 2026-10-16)

- `Ring4TunnelsTest`: alle onderdelen aanwezig (testseed, wig x 5.800–8.200, z ±1.500: 143 koepelhallen, 137 overstroomde tunnels, 252 topkoepels, 67 droge tunnels, 31 gas-, 18 gifpoel- en 30 kale hallen, 18 proppen, 20 overstroomde en 15 droge ingangen); ≥ 32 van de zones (bollen en kolommen langs de randen); nooit in de gesloten band van een meer boven zee (ook de gatmeren van de wereldbomen, 74); **offline per blok** rond 6 elementen van elke soort (54 vakken van 81 × 81, ~335.000 open blokken, 0 fout): water nooit naast lucht/gas/gif of boven lucht, lucht en gas onder Y 63 alleen naast steen (onder het geïnterpoleerde oppervlak, grotten dicht op het blok en de 8 hoeken van zijn cel) of hun eigen soort, gifpoel alleen naast en boven steen of gif; ingangen openen (in het water van het gat; vloer = kombodem ±2,5); breedte/hoogte; determinisme (twee velden, omgekeerde volgorde); niets buiten ring 4.
- `RingAquiferTest`: één vlakke zee onder de grond in ring 4 (ook de ring 4-helft van zone 3→4, wat vanilla ook zou zetten), niet in zone 4→5; het net vult zijn eigen ruimte (gas, gif, koepels, droge lucht, water), carvers niet in de schil.
- `Ring4JsonTest`: `tunnels` is geen stub meer.
- Geen runServer, waterscan, determinisme in-game of perf (INT).

## 5. Wat INT moet controleren

- **Waterscan:** `ring4.txt`-gebieden (r4e/r4w, b34, b45, vf) — verwacht: de gordijnen van r. 290 (a)/(b), 320, 321 en r. 150 weg; nieuw te tellen: tunnelvakken in de mythische band (koepels), de bedorven band (droge tunnels, gifpoelen, droge zinkgaten) en de bandovergang (proppen). Gifscan (INT-tool): gif tegen lucht en water = 0.
- **Prestatie:** meetpunten in elke ring 4-biome plus een tunnelgebied (mythisch en bedorven); de aquifer is in ring 4 anders (vlakke zee), dus ook `cascade`, `fjord` en `steamfens` opnieuw.
- **In-game:** koepels (adem halen), proppen, gas- en gifhallen, ingangen onder gatmeren en in droge zinkgaten; diepe grotten in ring 4 nu onder water.

## Code-review (3908040a, 2026-10-16, medium, review-subagent)
1. **Dek te grof** (medium): `ceilingLimit` las alleen de hoeken van het 4-raster; peat-zinkgaten worden per blok gesneden (een kom van straal 3 kan ~12 dieper liggen dan de dichtstbijzijnde hoek) en een hal werd alleen op een ring buiten de hal bemonsterd. **Opgelost:** de diepste kombodem waarvan de schijf binnen 4·√2 van het punt komt telt mee (`sinkFloorNear`, kombodems per wereld onthouden), en de hal wordt over zijn hele voetafdruk om de 4 blokken bemonsterd.
2. **Hash-botsing** (laag): `KIND_ENTRANCE + 1` = `KIND_HALL`, zodat droge ingang en halsoort dezelfde waarde lazen. **Opgelost:** eigen soort `KIND_DRY_ENTRANCE`.
3. **`Memo` leegde zich bij 50.000** terwijl `finalCell`/`attached` op identiteit vergelijken (latent, alleen bij veel grotere radii). **Opgelost:** nooit leegmaken (begrensd door de oppervlakte van de ring).
4. **Schil van een topkoepel tot ~29 van de zone** (laag). **Opgelost:** de bandtoets rekent bij een top met de koepel (`DOME_REACH`) plus schil.
5. **Bollen lui zonder veilige publicatie** (nit). **Opgelost:** in de constructor.
Geen bevinding over recursie, regelvolgorde in de aquifer, vlakke niveaus, gifkommen, prop of kosten per blok.

## Merge 74 (main 5c5f0594)
Main (74: wereldbomen, hemelmeren, rotsbogen) in de branch gemerged, beide kanten behouden: in `IslandField` eerst de boomblokken (+1), dan de openingen van het net; in `RingAquifer` de hemelmeerregels vóór de tunnelregels. De gatmeren van de wereldbomen zijn `TREE`-eilanden in de cellen: hun meer boven zee (aan de rand) houdt het net buiten zijn band (`underLake`/`ceilingLimit` via `details`), een gatmeer van een boom op zeeniveau mag een ingang krijgen; wortels zijn massief (kunnen alleen een tunnel dichten). Hemelmeren liggen op de eilanden, ver boven het net. Test `theNetStaysOutOfTheBandsOfLakesAboveTheSea`.

## Voortgang
- 2026-10-16: branch aangemaakt, 73 gebouwd (build en runData groen), review verwerkt, main (74) gemerged.

## CODEBASE-delta
- Nieuw `worldgen/densityfunction/Ring4Tunnels` (73): het tunnelnet van ring 4 (knooppunten per cel van 128, randen met middellijn en profiel, hallen, topkoepels, gemengde randen met prop, ingangen naar zee-gatmeren en droge zinkgaten, droge zinkgaten, scheiding tussen de banden); per kolom `Span` (open intervallen met vulling, gesloten grotten); `Fill` (codes van de DF); beslissingen per wereld in `Memo` (`IslandField.Decisions.tunnels`), elementlijst per chunk in `KeyedSlots`.
- `IslandField`: veld `tunnels`; `Column` + `tunnelsFilled`, `tunnelSpan`, `tunnelLo/Hi`, `tunnelLiftLo/Hi` (eigen blok "73" onderaan de velden en in `reset`); `thin` opent het net, `caveLift` sluit de grotten eromheen (`ensureTunnels`); publiek `tunnelSpan(x, z)`, `tunnelFill(x, y, z)`, `tunnelsWithin(box)`, `tunnelRoomsWithin(box)` met records `TunnelPath`, `TunnelPoint`, `TunnelRoom`; `CAVE_FLOOR_MARGIN` package-private.
- `IslandOutput.Output.TUNNELS` (`tunnels`, per blok, 0..5); JSON `ring_4/overworld/tunnels.json` → `island_output tunnels` (was stub 0).
- `RingFluidPicker.ring4Flooded` (ring 4 incl. de ring 4-helft van zone 3→4, niet zone 4→5); `RingAquifer`: per kolom `flooded` en `tunnels` (span), regels na het meer boven zee: carvers niet in de schil, de vulling van het net, dan water ≤ Y 62 / lucht erboven (steen op Y −54).
- Tests: `Ring4TunnelsTest` (nieuw), `RingAquiferTest` (+2), `Ring4JsonTest` (stub weg).

## Beslissingen
- 2026-10-16 | worldgen | Ring 4-floodedness: alle open ruimte van ring 4 (ook de ring 4-helft van zone 3→4) onder Y 63 is water tot Y 62, erboven lucht, in plaats van vanilla's lokale aquiferniveaus; ook in de bedorven band (alleen het tunnelnet is daar droog, afgesloten) | Implementatiekeuze voor "eigen floodedness, geen vrij water tegen lucht" (golf-2.md 73): één niveau is de enige regel zonder gordijnen; droge grotten onder zee zouden elke grotmond onder water moeten dichten. **Ter bevestiging bij teststop 5b** (zichtbaar: diepe grotten in ring 4 onder water).
- 2026-10-16 | worldgen | Peat-zinkgaten met een bodem onder zee en grond ≥ Y 64,5 eromheen zijn droog (lucht tot Y 62, grotten eromheen dicht); de rest blijft zoals nu | Q6 = A ("zinkgaten (droog)") en Q10 = A (gaslagen in zinkgatbodems, 76).
- 2026-10-16 | worldgen | Luchtkoepels op elk overstroomd knooppunt (hal) en op elke top van een overstroomde rand (~44 blokken uit elkaar); gaskamers als hallen vol gas; gifpoelen als kom in een hal onder alle tunnelvloeren | Implementatiekeuze binnen Q5 = A (afstand tussen koepels: zwemmen ≤ ~10 s).
- 2026-10-16 | worldgen | Bandovergang: een rand tussen een overstroomd en een droog knooppunt wordt in het midden afgesloten met een prop van ≥ 6 steen; droge elementen dicht bij overstroomde vallen weg | Q5 = A ("gesloten overgangen").

## Backlog
- [worldgen] W-biomes (77): de wanden van het net vinden via `tunnelFill`/`tunnelsWithin`/`tunnelRoomsWithin`; ertsen en sporen mogen geen tunnel-, koepel- of komwand openen naar een ander medium (een erts dat de schil van een koepel vervangt is veilig, een lucht- of waterfeature niet). (bron: 73)
- [worldgen] W-biomes (76): gaslagen in de bodems van droge zinkgaten: alleen in de kom (de aquifer houdt hem droog tot Y 62); de natte zinkgaten (zeewater) houden geen gas. (bron: 73)
