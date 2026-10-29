# Fase 4 — voorlopige keuzes (teststop met de gebruiker)

Ontdubbeld uit de "Voorlopig"-secties van `H1.md`, `D.md`, `I.md`, `R2.md`, `R3.md`, `R4.md` en `R5.md` (INT,
2026-10-28; R2 op 2026-10-29) en van de leven-ronde `L0.md`, `L2.md`, `L3.md`, `L4.md`, `L5.md` (2026-10-29; L1 volgt). Wat al in het
design-doc of de beslissingenlog staat, staat hier niet. Per punt: **ok** of **anders, en hoe**. Alle getallen staan in de serverconfig
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

## De Gelaagde (ring 2, R2)
24. Uiterlijk: ketenen om de voorpoten (uit de mockup) aan de schenen geknoopt; rugbanden ~4–12 px langs het lijf, palet uit
   de drie kloofsteenblokken; dode koppen draaien elk in eigen tempo naar de dichtstbijzijnde speler binnen 24 (alleen beeld).
25. Hitbox 2,5 × 2,5 (ogen 1,6), kop-part 1,3 × 1,3; 450 HP, armor 6, snelheid 0,28, beet 8 (knockback 0,5), XP 350;
   beloning 4–7 ruw zonnesteen (+0–2 per looting-niveau).
26. Beet: bereik 2 vanaf de snavelpunt, windup 10 ticks, cooldown 30 ticks.
27. Grijpen: dezelfde steek, maar hij houdt vast: elke 15–20 s (de eerste na een halve cooldown), alleen op de grond, 4 schade.
28. Meeklimmen: muur binnen 12 blokken; loslaten na 10 blokken klimmen, over de rand of na 6 s. Zonder muur: elke 0,5 s
   schudden (2 schade), na 3 s gooit hij je omhoog.
29. Worp zonder wand is hoog: beoordelen bij de teststop in echt terrein.
30. Bevrijden: 1 kopslag van een ander of 3 van de gegrepen speler zelf; sneaken helpt niet, een enderpearl of teleport wel.
31. Steenslag elke 10–16 s: 5 blokken de muur op, 14 ticks hameren; max. 6 blokken per aanval en 48 per leven (opgeslagen).
   Eerst de onderkant van overhangen boven het doelwit (straal 2, 3–12 hoog), anders blokken uit de muur naar het doelwit
   gegooid. Kloofsteen valt als kloofkei, zandsteen als los zand; schade 2 per blok val (max. 20); zijn eigen stenen doen
   hem niets. Alleen natuurlijke steen en zand (tag `layered_rockfall`), blijvend; uit te zetten (`rockfallEnabled`).
32. Pantserrug × 0,4; volle schade op de buik (aanvaller met ogen > 0,5 onder zijn voeten), op de kop of als hij op zijn rug
   ligt; schade zonder positie (vuur, gif) telt als buik.
33. Vuur of `dazzled` aan de wand: loslaten en vallen; bij de landing 12 schade en 4 s op zijn rug (alles volle schade).
34. `dazzled` op de grond: ook op zijn rug; daarna 8 s immuun voor omwerpen door flitsen (een val van de wand werpt altijd om).
35. Wandklim zoals de vanilla-spin (omhoog en zijwaarts, langzaam zakken), geen plafonds; de hitbox draait niet mee (het
   achterlijf hangt op de wand alleen in beeld onder de hitbox); smalle spleten en grotten haalt hij niet.

## De Herrijzende (ring 3, R3)
36. Formaat: mockup op schaal 0,75 (spanwijdte ~25, hoogte ~15); hitbox lijf 3,5 × 8,5, vleugels en kop als deel-hitboxen
    (vleugels × 0,5 schade).
37. Litteken: basalt, magma en obsidiaan op natuurlijke blokken (tag `risen_meltable`); lava alleen waar hij niet wegstroomt;
    `meltPool` aan/uit + straal.
38. *(Vervangen door het grondgevecht uit de leven-ronde, besluit 1a; zie punt 95.)* Landen: na 25 s vliegen landt hij 8 s naast zijn doelwit (snavelhak 9, vleugelslag 6 + duw straal 7); de hoorn laat hem
    vallen.
39. Asregen: effect `ashen` (max. 6 stapels, elk −1/6 snelheid en sprong; op 6 sta je vast); een medespeler slaat 2 stapels
    weg; buiten de regen −1 per 5 s; vast breek je na 6 s zelf los.
40. Koorschreeuw: windup 1,2 s, straal 16: zwakte 10 s + misselijkheid 6 s; dieren en mobs binnen 24 vluchten.
41. Slachtoffers: 2 aswandelaars per keer, max. 6 (placeholder-zombie, backlog).
42. Verstijven: zolang `stunned` + 3 s, daarna 10 s immuun; een schreeuw in windup breekt af.

## De Verloren Hoeder (ring 4, R4)
43. Lijf ~24 blokken: kop + 12 segmenten, 6 lijf-hitboxen (2,4 → 1,1) en een kroon-hitbox; bossbar wit.
44. Kroon 35 % groter dan de mockup, 6 takken; elke treffer breekt er één (×1,5 schade, 10 ticks cooldown), geen teruggroei.
    Kroon kapot in de lucht: hij valt (op land 8 % max-HP + 5 s verdoofd, in water 2,5 s); daarna zwemt hij alleen nog en
    kruipt hij op land.
45. HP 800, armor 6, beet 9, XP 500; cirkelt op 14 blokken rond zijn doelwit, 6 ± 4 blokken erboven.
46. Mistbank: dik binnen 40 blokken, dunt uit over 24, zicht 32 in het hart, komt op in 5 s; ook bij helder weer.
47. Lokroep elke 18–26 s, 6 s lang binnen 24 blokken en in zicht (trekt naar hem toe; stopt bij zijn kaken).
48. Gifadem elke 10–16 s: kegel 10 blokken, 25°, 15 gifpunten/s, volgt het doelwit langzaam; laat gifwolken achter.
49. Onder water trekken elke 12–18 s: grijpt binnen 3,5 blokken als er water binnen 6 blokken is, duikt naar het diepste
    water; max. 6 s, 2 schade/s; 30 schade van wie dan ook bevrijdt; zonder water een beet. Sluipen helpt niet.
50. Staartzwiep elke 8–12 s bij iemand achter hem: 8 ticks waarschuwing, 10 schade, ~15 blokken weg.

## De Onvoltooide (ring 5, R5)
51. Lijf 25 % schade; 4 zwakke plekken (één per geënt deel) onkwetsbaar tenzij ingeladen, dan 200 %.
52. Lens: één speler die door de hemelglaslens naar hem kijkt (≤ 48 blokken, kegel 12°, vrij zicht) laadt ze voor iedereen
    in, met 6 s nalooptijd.
53. Een zwakke plek die 15 % van de max-HP heeft geïncasseerd, scheurt zijn deel af (aanval weg).
54. Beweging zonder pathfinding: schrijdt recht op zijn doel af (draait eerst), stopt op 9 blokken, staat altijd bovenop
    (nooit onder overhangen), gaat door bladeren en bouwsels heen zonder iets kapot te maken; boven void staat hij stil.
55. Formaat ~20 blokken; hitbox 5 × 14 + deel-hitboxen.
56. Klap met de Holle-arm: windup 25 ticks, straal 4,5, 14 schade, cooldown 3–5 s.
57. Verspringen (Holle) elke 18–28 s of meteen als het doelwit > 24 blokken weg is; komt 9 blokken van het doelwit op.
58. Grijpen (Gelaagde): tilt een vechter 3 s op tot 16 blokken en laat los (valschade); 40 schade bevrijdt; cooldown 20–30 s.
59. Asregen (Herrijzende): wolk straal 6 voor 8 s, eigen eenvoudige as-versie; cooldown 20–30 s.
60. Lokroep (Hoeder) bij iemand > 12 blokken weg: 4 s trek; cooldown 25–35 s.
61. Void-trek: spelers binnen 24 blokken 4 s naar het dichtste void-gat getrokken, void-meter +8/s; cooldown 30–40 s.
62. Eén aanval tegelijk, 1,5 s pauze; elke aanval pas een halve cooldown na zijn komst.
63. Vuurbestendig, geen botsing, armor 10, XP 1000; 6–10 ruw nulliet (+looting) en 2–4 nullietstaven; bossbar wit.
64. Void-poel onder zijn voeten is alleen visueel; het litteken (scheur tot in de void) volgt in stap 2.

## Zwakte-items (I)
65. `dazzled`/`stunned` op elke mob (ook Nightmares): doelwit los; `stunned` ook snelheid, vliegsnelheid en sprong −100 %.
66. Speler met `dazzled`: warm wit scherm (max. 0,85), vervaagt de laatste 2 s; verder niets.
67. Zonnesteenflits: rechtsklik, kegel 30° (halve hoek), bereik 16, zichtlijn, 3 s `dazzled`; spelers alleen bij pvp; nooit
    de gebruiker. Duurzaamheid 64, cooldown 15 s.
68. Vulkanieten hoorn: 3 s blazen, straal 12 door muren, 2 s `stunned`, nooit spelers. Duurzaamheid 32, cooldown 30 s.
69. Recepten: flits ` S `/`SGS`/` T ` (zonnesteenstaaf, glasruit, turkoois); hoorn `VHV`/` R ` (vulkanietstaaf,
    geitenhoorn, vuurrobijn). Beide brandvrij; tab 2 resp. 3.

## Leven-ronde — gedeelde reactielaag (L0)
70. Wankelen: 6 % van de volle max-HP aan echt genomen schade binnen 4 s, of een treffer op de zwakke plek; 30 ticks
   (1,5 s); daarna 10 s geen nieuw wankelen (ook niet via de zwakke plek: geen stun-lock). Geen extra schade tijdens het
   wankelen (het open venster = hij valt niet aan en beweegt niet).
71. Standaard wankel-effect: `RAVAGER_STUNNED` (toon 0,6) + crit-vonken, wiebel 5°; per Nightmare te vervangen
   (`staggerEffects`, `staggerSway`, eigen animatie).
72. Tempo: na 2–4 aanvallen een rustvenster van 3–5 s; 50 % afstand nemen (naar 10 blokken), anders cirkelen (straal
   4–10, stap 0,5 rad per nieuw punt, richting per venster getrokken).
73. Zoeken: 3 s uit het zicht → 10 s zoeken, daarna "snuffelt hij je op" (laatste plek = waar je nu bent) en jaagt hij
   weer. Geraakt door het doelwit uit het zicht = gehoord: de plek wordt de schutter. Nooit gezien maar wel doelwit
   (door muren gevonden): de plek is meteen bekend. Tijdens het zoeken start hij geen aanval.
74. Doelwit: alleen spelers (vechters); score = schade van de laatste tijd (halfwaardetijd 10 s) + 15 als de speler
   binnen 8 blokken is en ≤ 35 % HP heeft; zonder scores de dichtstbijzijnde. Wisselen alleen bij > 1,5× de score van
   het huidige doelwit, daarna 5 s niet opnieuw. Andere aanvallers (wolven, golems) krijgen geen wraak meer.

## Leven-ronde — De Verloren Hoeder (L4)
75. Wankelen: de gedeelde standaard, de kroon als zwakke plek; tijdens het wankelen drijft hij stil (in de lucht met
   kroon blijft hij zweven).
76. Bellenval: elke 14–20 s, binnen 28 blokken, onder het doelwit + 1 andere vechter; 30 ticks bellen op een vaste plek
   (straal 1,8); stroom 40 ticks (−0,08 b/t per tick), windstoot 1,5 b/t omhoog; 4 schade bij vangen. Aankondiging: hij
   stopt en kijkt naar de plek (kop omlaag, kieuwen open, gegorgel).
77. Omsingelen: elke 22–32 s, binnen 32 blokken; middelpunt = gemiddelde van de vechters binnen 8 blokken van het
   doelwit; ≥ 9 van 12 proefpunten op de kring vrij; zwemt 1,4× en draait 2,2× zo snel. Op land is de staart 1,1 hoog,
   in water steekt hij 0,1 boven het water uit; onder het lijf door kan bij ≥ 3 diep. Terugduw 0,55 b/t (max. 2×/s),
   knijpen 0,9 omhoog. Niet in de lucht en niet kruipend (zonder kroon alleen in water).
78. Duik en uitbarsting: elke 16–24 s, binnen 40 blokken; ≥ 4 diep water onder of binnen 4 blokken naast het doelwit;
   duikt 6 blokken weg (max. 3 s), loert ≤ 6 diep onder het doelwit (≤ 2 s), 25 ticks borrelen op een vaste plek, barst
   omhoog met 2,6× zwemsnelheid: straal 2,5, 12 schade, 1,4 omhoog + 0,35 opzij. Met kroon 7 blokken boven het water uit,
   zonder 2.
79. Aanvalskeuze: grijpen > uitbarsting/gifadem > omsingelen/lokroep > bellenval/staartzwiep > cirkelen, elk met een
   eigen cooldown (de eerste keer na de halve). Op de oever en op land: bellenval omhoog, omsingelen alleen met kroon;
   in de lucht alleen de bellenval.

## Leven-ronde — De Onvoltooide (L5)
80. Glitchsprong: op 10–30 blokken, 20 ticks bevriezen, 4–6 stappen van 3–5 blokken (±55° opzij), 4–10 ticks pauze, de
   pilaar 6 ticks vooraf; cooldown 14–22 s.
81. Brokken: op 8–32 blokken, 12 blokken per brok, 30 ticks opstijgen, inslag straal 3, 12 schade; cooldown 16–24 s.
   Paletten: ring 1 aarde/bemoste kei/eik, ring 2 zandsteen/gebarsten klei, ring 3 basalt/blackstone/magma, ring 4
   getijdensteen (geen zand of gras: niets valt of groeit).
82. Ontladen grond: binnen 24 blokken, straal 3, 40 ticks waarschuwing, 80 ticks open, 16 lagen per tick, max. 6000
   blokken; alleen hele natuurlijke kolommen zonder vloeistof ernaast, niet binnen 5 blokken van hem; uit te zetten
   (`unloadEnabled`). Wie 12 blokken diep valt, vangt de void op (4 void-schade).

## Leven-ronde — De Gelaagde (L2)
83. Wankelen kan niet op zijn rug, tijdens een val of onder het zand.
84. Aanvalskeuze: grijpen > sprong > ingraven > steenslag > spuwen > beet, elk met eigen cooldown en voorwaarden; zoeken
   en rust gaan voor; tussen speciale aanvallen 30 ticks (`specialGapTicks`).
85. Sprong (naar de afzetplek): cooldown 8–12 s, windup 16 ticks, vanaf een rand ≥ 3 lager, bereik 3–14; inslag straal 3,
   10 schade, wegslaan 0,8 (+0,35 omhoog); vlucht 10–24 ticks (1,2 tick per blok), boog 1,5 boven het hoogste eind,
   daarna 12 ticks herstel.
86. Ingraven: cooldown 18–26 s, 30 ticks ingraven, bereik 3–20, 0,3 blok/tick (iets sneller dan sprinten), max. 8 s, 1 blok
   op/neer per stap; opkomen: 20 ticks golven, straal 2,5, 12 schade, 0,9 omhoog (hij springt zelf 0,45 op).
87. Spuwen: cooldown 9–14 s, windup 18 ticks, afstand 5–16 (de wolk vliegt 2 verder en blijft op de oude plek), 1,2
   blok/tick, straal 1, 3 schade per wolk + 3 s blindheid; de twee koppen mikken 0,6 uit elkaar.
88. Rustvenster aan de wand: hij hangt stil (hinderlaag), de sprong van de wand volgt vanzelf; op de grond de standaard
   (lopen of cirkelen).
89. Geluiden: vanilla met lage toonhoogte (ravager-brul bij de sprong, warden-graven bij het ingraven, lamaspuug bij het
   spuwen); eigen geluiden in de backlog.

## Leven-ronde — De Herrijzende (L3)
90. Vuurtegels: particles, in brand zetten en `fireDamage` per 0,5 s; niet opgeslagen (uitgeladen = uit).
91. Duikvlucht elke 30–45 s, hoogte 28, windup 1,5 s, snelheid 2,0, 16 schade + 3 s in brand, scheren over 14 blokken.
92. Sintelregen elke 22–32 s: straal 7, windup 1,5 s, 5 s lang, 2 sintels per 4 ticks, val 1 s, 4 schade.
93. Na een grote aanval wachten duik en asregen ≥ 4 s; een aanval die alleen in de tick leeft (duik, landen, duikvlucht,
   sintelregen) begint na het laden opnieuw bij cirkelen.
94. Rustvenster: afstand nemen = een 1,5× bredere en 8 hogere cirkel, anders gewoon cirkelen. Zoeken: laag en krap boven
   de laatst bekende plek, daar rondkijken (animatie + roep).
95. Grondgevecht: landen met kans 0,4 na een reeks aanvallen in de lucht, uiterlijk na 40–60 s vlucht; 15 s op de grond,
   loopsnelheid 0,8 × `movementSpeed`; stamp 8 schade in 5 blokken + 0,5 opgooien, pik 9 (windup 14 ticks). Een
   grondaanval die afbreekt (verstijven) komt later niet alsnog.
