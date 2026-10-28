# Fase 4 — voorlopige keuzes (teststop met de gebruiker)

Ontdubbeld uit de "Voorlopig"-secties van `H1.md`, `D.md`, `I.md`, `R3.md`, `R4.md` en `R5.md` (INT, 2026-10-28); R2
volgt bij de laatste merge. Per punt: **ok** of **anders, en hoe**. Alle getallen staan in de serverconfig
(`[nightmares.<naam>]`, `[nightmares.director]`, `[weakness_items]`).

## Gedeeld (alle Nightmares)
1. Wie telt als vechter (HP-schaal, aanvallen die spelers kiezen): levend, geen spectator of creative, binnen 32 blokken.
   Aftocht als er 30 s niemand (creative telt wel) binnen 64 blokken is: hij verdwijnt zonder drop.
2. Max-HP boven 1024 (vanilla-plafond): intern een virtuele max-HP; treffers worden geschaald zodat het aantal treffers
   en de bossbar kloppen.
3. Sterkte na elke nederlaag +25 % op max-HP en `attack_damage` (max. 3×); aanvallen met eigen config-schade doen zelf × sterkte.
4. `/kill` op een geregisseerde Nightmare telt als verslagen (`/ca nightmare reset <ring>` om te wissen).

## Regisseur (D)
5. Commando's onder `/ca nightmare …` (status, threat, arrive, reset).
6. Standaardkomst (voor wie geen eigen komst heeft): 30 s voortekens (grotgeluid, as, de laatste 5 s een hartslag), dan
   10 blokken achter het doelwit uit rook.
7. Dreiging: drempel 100, per speler en ring een tempo van 2–3 ingame-dagen tot de drempel, max. 2× de drempel; zakt
   niet buiten de ring; stijgt alleen in survival/adventure in de overworld van een ringwereld (de void-rand telt als
   ring 5); blijft bij de dood. Na een komst gaat de dreiging naar 0 voor het doelwit en elke vechter.
8. Doelwit moet tijdens de voortekens in de ring, online en levend blijven; anders stoppen ze en blijft de dreiging staan.
9. Geen plek om te verschijnen: elke seconde opnieuw tot 20 s, dan 180 s niet zoeken.
10. Peaceful: geen Nightmares (dreiging blijft staan, lopende voortekens stoppen).
11. Zwerven: elke 60 s 32 blokken, koers ±45°; buiten zijn ring loopt hij terug naar zijn band.
12. Een aanwezige Nightmare die 15 s niet tickt (chunk ontladen) gaat terug naar virtueel, geheeld, niet verslagen.
13. Tijd = `gameTime` van de overworld (`/time set` telt niet).

## De Holle (ring 1, H1)
14. Slag: windup 12 ticks (ontwijkbaar), bereik hitbox + 2 blokken, cooldown 30 ticks, 10 schade, knockback 1,5.
15. Verspringen elke 8–14 s na 1 s krakende waarschuwing; een echte scheur waar hij staat en een tweede 3 blokken achter
    het doelwit.
16. Uit fase elke 12–20 s voor 3–5 s (alleen met een doelwit); dan gaat alle schade erdoor, behalve `/kill` en de void.
17. Wissel van twee spelers alleen met ≥ 2 vechters, elke 25 s, 0,75 s waarschuwing.
18. Zielkristal: pint 4 s; een kristal op een al vastgepinde Holle doet niets en kost niets.
19. Doelwit ook zonder zicht (dimension shifter); volgafstand 48, snelheid 0,26, armor 4, niet vuurbestendig.
20. Beloning: 250 XP, 6–10 zielkristallen (+looting), 2–4 diamanten. Geluiden: vanilla op lage pitch (backlog).
21. Komst: scheur 9 blokken achter het doelwit; 30 s voortekens (haarscheur, vluchtende dieren, "achteruit" geluiden,
    fakkels die roken), 2 s openscheuren, 2,5 s ogen, 3 s eruit trekken (hand eerst). Zuigkracht straal 10.
22. Litteken alleen na een komst: straal 2,5, alleen natuurlijke grond (aarde, gras, steen, zand, grind), glitchblok in het
    midden; uit te zetten (`scarEnabled`).
23. Spoor tijdens het zwerven: 1 op de 4 stappen een haarscheur van 8 s, geen blijvend litteken.

## De Herrijzende (ring 3, R3)
24. Formaat: mockup op schaal 0,75 (spanwijdte ~25, hoogte ~15); hitbox lijf 3,5 × 8,5, vleugels en kop als deel-hitboxen
    (vleugels × 0,5 schade).
25. Litteken: basalt, magma en obsidiaan op natuurlijke blokken (tag `risen_meltable`); lava alleen waar hij niet wegstroomt;
    `meltPool` aan/uit + straal.
26. Landen: na 25 s vliegen landt hij 8 s naast zijn doelwit (snavelhak 9, vleugelslag 6 + duw straal 7); de hoorn laat hem
    vallen.
27. Asregen: effect `ashen` (max. 6 stapels, elk −1/6 snelheid en sprong; op 6 sta je vast); een medespeler slaat 2 stapels
    weg; buiten de regen −1 per 5 s; vast breek je na 6 s zelf los.
28. Koorschreeuw: windup 1,2 s, straal 16: zwakte 10 s + misselijkheid 6 s; dieren en mobs binnen 24 vluchten.
29. Slachtoffers: 2 aswandelaars per keer, max. 6 (placeholder-zombie, backlog).
30. Verstijven: zolang `stunned` + 3 s, daarna 10 s immuun; een schreeuw in windup breekt af.

## De Verloren Hoeder (ring 4, R4)
31. Lijf ~24 blokken: kop + 12 segmenten, 6 lijf-hitboxen (2,4 → 1,1) en een kroon-hitbox; bossbar wit.
32. Kroon 35 % groter dan de mockup, 6 takken; elke treffer breekt er één (×1,5 schade, 10 ticks cooldown), geen teruggroei.
    Kroon kapot in de lucht: hij valt (op land 8 % max-HP + 5 s verdoofd, in water 2,5 s); daarna zwemt hij alleen nog en
    kruipt hij op land.
33. HP 800, armor 6, beet 9, XP 500; cirkelt op 14 blokken rond zijn doelwit, 6 ± 4 blokken erboven.
34. Mistbank: dik binnen 40 blokken, dunt uit over 24, zicht 32 in het hart, komt op in 5 s; ook bij helder weer.
35. Lokroep elke 18–26 s, 6 s lang binnen 24 blokken en in zicht (trekt naar hem toe; stopt bij zijn kaken).
36. Gifadem elke 10–16 s: kegel 10 blokken, 25°, 15 gifpunten/s, volgt het doelwit langzaam; laat gifwolken achter.
37. Onder water trekken elke 12–18 s: grijpt binnen 3,5 blokken als er water binnen 6 blokken is, duikt naar het diepste
    water; max. 6 s, 2 schade/s; 30 schade van wie dan ook bevrijdt; zonder water een beet. Sluipen helpt niet.
38. Staartzwiep elke 8–12 s bij iemand achter hem: 8 ticks waarschuwing, 10 schade, ~15 blokken weg.

## De Onvoltooide (ring 5, R5)
39. Lijf 25 % schade; 4 zwakke plekken (één per geënt deel) onkwetsbaar tenzij ingeladen, dan 200 %.
40. Lens: één speler die door de hemelglaslens naar hem kijkt (≤ 48 blokken, kegel 12°, vrij zicht) laadt ze voor iedereen
    in, met 6 s nalooptijd.
41. Een zwakke plek die 15 % van de max-HP heeft geïncasseerd, scheurt zijn deel af (aanval weg).
42. Beweging zonder pathfinding: schrijdt recht op zijn doel af (draait eerst), stopt op 9 blokken, staat altijd bovenop
    (nooit onder overhangen), gaat door bladeren en bouwsels heen zonder iets kapot te maken; boven void staat hij stil.
43. Formaat ~20 blokken; hitbox 5 × 14 + deel-hitboxen.
44. Klap met de Holle-arm: windup 25 ticks, straal 4,5, 14 schade, cooldown 3–5 s.
45. Verspringen (Holle) elke 18–28 s of meteen als het doelwit > 24 blokken weg is; komt 9 blokken van het doelwit op.
46. Grijpen (Gelaagde): tilt een vechter 3 s op tot 16 blokken en laat los (valschade); 40 schade bevrijdt; cooldown 20–30 s.
47. Asregen (Herrijzende): wolk straal 6 voor 8 s, eigen eenvoudige as-versie; cooldown 20–30 s.
48. Lokroep (Hoeder) bij iemand > 12 blokken weg: 4 s trek; cooldown 25–35 s.
49. Void-trek: spelers binnen 24 blokken 4 s naar het dichtste void-gat getrokken, void-meter +8/s; cooldown 30–40 s.
50. Eén aanval tegelijk, 1,5 s pauze; elke aanval pas een halve cooldown na zijn komst.
51. Vuurbestendig, geen botsing, armor 10, XP 1000; 6–10 ruw nulliet (+looting) en 2–4 nullietstaven; bossbar wit.
52. Void-poel onder zijn voeten is alleen visueel; het litteken (scheur tot in de void) volgt in stap 2.

## Zwakte-items (I)
53. `dazzled`/`stunned` op elke mob (ook Nightmares): doelwit los; `stunned` ook snelheid, vliegsnelheid en sprong −100 %.
54. Speler met `dazzled`: warm wit scherm (max. 0,85), vervaagt de laatste 2 s; verder niets.
55. Zonnesteenflits: rechtsklik, kegel 30° (halve hoek), bereik 16, zichtlijn, 3 s `dazzled`; spelers alleen bij pvp; nooit
    de gebruiker. Duurzaamheid 64, cooldown 15 s.
56. Vulkanieten hoorn: 3 s blazen, straal 12 door muren, 2 s `stunned`, nooit spelers. Duurzaamheid 32, cooldown 30 s.
57. Recepten: flits ` S `/`SGS`/` T ` (zonnesteenstaaf, glasruit, turkoois); hoorn `VHV`/` R ` (vulkanietstaaf,
    geitenhoorn, vuurrobijn). Beide brandvrij; tab 2 resp. 3.
