# Teststop 4b — feedback van de gebruiker en antwoorden

Bij `teststop-4b.md` (branch `ring3/int2`). Feedback uit de eigen test van de gebruiker op de integratiestand van golf 2 (2026-10-09, client vanuit `.claude/worktrees/int2`, seed `20260930`).

## Feedback (2026-10-09)
1. **`dormant_caldera`:** de krater mag meer aankleding krijgen. Nu een vrij lege, kale vlakte met een klein beetje aankleding; het voegt weinig toe aan het landschap.
2. **`sulfur_fields`:** ook hier kan meer worden toegevoegd.

## Al bekend vóór de test
- Lava tegen lucht zonder korst in ring 3 (randen van lavameertjes, luchtgeul door een lavameer, diepe grotlava, diagonale buiswand); al aanwezig op main `6ef5a85`. **Besluit 2026-10-09: backlog + fix-pakket direct na 4b.**

## Beslissingen (2026-10-09, sparring na de test)
De gebruiker: "dat was het eigenlijk voornamelijk"; de huidige ring 3-backlog wordt meegenomen om golf 2 af te ronden (golf 2-afronding), daarna volgt golf 3.

1. **`dormant_caldera` → overwoekerde krater:** het leven keert terug: mostapijten en -heuvels, bloeiende azalea's en kleine azaleabosjes, ranken en hangende wortels langs de wand, dripleaf/varens in vochtige kommen, af en toe een omgevallen stam. Geen water (contract). Groot contrast met de rest van ring 3.
2. **`sulfur_fields` → zwavelkristallen + gekleurde kookpoelen:** gele kristalclusters/-naalden rond vents en op korsten (bestaande blokken hergebruiken waar het kan); terrasvormige poelranden (travertijn-achtig) met borrelende modder of kokend water in geel/groen/oranje, met `sulfur_steam` erboven. Niet gekozen: gebleekte resten, meer stoomvents.
3. **Lava tegen lucht** (INT2-vondst): fix-pakket in de golf 2-afronding (eerder: "na 4b").
4. **Mee in de golf 2-afronding (alle vier groepen gekozen):**
   - terreinrestjes: wanden van diepe sneden in ring 3-gesteente i.p.v. vanilla `stone`; ijsblokjes bij de warme scheuren in Nether-3 weg; de 9 determinismeblokken (aslaag op erts, vallende askorst); `ring_chance` voor sunstone/turquoise in de ring 3-helft;
   - sfeer en geluid: eigen geiser-waarschuwingsgeluid; lavabommen niet stapelen bij meerdere spelers en oude bommen opruimen;
   - blokken en condities: `glowing_fissure` met willekeurige rotaties; gedoofd kampvuur telt niet als hittebron; ijs smelt door ring 3-hitte; natte spons droogt in ring 3;
   - verre rookpluimen zonder terreinmist (eigen render-pass, client-only).
5. **4b-beoordelingspunten:** zielvuur rond de ribbenkasten en de rode lucht bij een uitbarsting versterken. De rest blijft zoals het is (akkoord): 21 riftbogen, lavavallen van 1 blok breed, askorst-daken boven buizen, alleen de startmond in een 39d-sleuf.
6. **Niet genoemd = akkoord:** de overige punten van `teststop-4b.md` (o.a. geluiden, mist Nether-3 zo laten, bevriezing, pluimen, striders) gelden als goedgekeurd.
