# Fase 2 — voorlopige keuzes (afsluitronde met de gebruiker)

Ontdubbeld uit `golf-2-*.md`, `golf-34-*.md`, `golf-5-*.md` (ook de nog niet gemergde L en T), `fase-2-items.md` §5.8 en
de tuningvragen van `eindtest-fase-2.md` (was `teststop-golf-1-4.md`; X1, 2026-10-28). Per punt: **ok** of **anders, en hoe**.

## Stats en kosten
1. Tier-curve §6.1 (zonnesteen 2200/9/4 … nulliet 3800/12/7,5; harnas tot 5/8/10/5, toughness 5): vulkaniet bewust steil. Goed?
2. Coatingsterkte 0,1875 per stuk (0,81× / 0,63× / 0,44× / 0,25×); met leer tegen kou samen 0,1×.
3. Doses: veldfles 4 slokken à −25 + 45 s; cactussap −15 + 20 s; dauwdrank, zuiver water, hemellicht, handwarmer (4 ladingen) en galmschelp (duurzaamheid 16, cooldown 45 s) −20 + 45 s; jerky +8 uitdroging bij voeding 6 / 1,2.
4. Upgrade-templates: 6 Nether-materiaal + 2 staaf (zonnesteen: diamant) + 1 ringsteen, geen kopie (zonnesteen ≈ 3 chunks Nether-1 mijnen).
5. Coating-templates: ringrecept 4 + 4 + 1 → 1; kopie 7 materiaal + ringsteen + template → 2.
6. Zuiver water: 1 oerijs + 4 flessen → 4 (oerijs valt alleen met Silk Touch).
7. Brandstof: veen 6 items, zwavel 8, zwavelblok 80 (kolenblok); zwavelblok brandt niet in de wereld.

## Recepten
8. Upgrade-templates `SIS / SCS / SIS` (alle 4); de smithing neemt het staaf-item, niet `c:ingots/<tier>`.
9. Coating-templates: uitdroging `TST/SCS/TST`, hitte `RPR/PDP/RPR`, gif `JSJ/SCS/JSJ`, kou `FSF/SCS/FSF` (ringsteen koude lava), void `GVG/VHV/GVG`, stilte `ZWZ/WKW/ZWZ`.
10. Veldfles ` S /LBL/ L ` (string = touw) en handwarmer `LFL/ L `, beide gecraft leeg; galmschelp ` F /FWF/ F `; lens verrekijkervorm (hemelglas boven 2 getijdezilverstaven).
11. Drankjes vormloos (cactussap, dauwdrank, hemellicht; alleen met een waterfles); jerky zout + elk gebakken vlees of vis → 2; buskruit alleen met houtskool → 2.
12. Pijlen (brand, gif, glitch) ` A /AMA/ A ` → 4.
13. Ingelegde sets (4): 8 steen rond 1 edelsteen → 8, trap/plaat/muur zoals vanilla, steenzaag 1/2/1, droppen zichzelf; turkoois-zandsteen in `c:sandstone`.
14. Zoutlamp (fakkel op zoutblok), zwavellamp (4 zwavel rond glas, zeelantaarn-eigenschappen, geen gereedschap), vlotter `# #/# #` → 2, kristalblokken 2×2 zonder weg terug, glitchscherfblok 9 ↔ 1 (elke pickaxe).
15. Hemelglas alleen in de oven (0,1 XP); hemelzand niet in de zand-tags, hemelglas niet in `c:glass_blocks/colorless|cheap`.

## Namen
16. NL bijvoeglijk: "Zonnestenen / Vulkanieten / Getijdezilveren / Nullieten zwaard … kuras, beenstukken, laarzen".
17. "Gezouten gedroogd vlees", "Brandpijl / Gifpijl / Glitchpijl", "Hemellicht", "Galmschelp" (effect "Galm"), "Glitchscherfblok"; upgrade-templates heten "Smeedsjabloon" (zoals vanilla), coatings "<X>coatingsjabloon".

## Textures en look
18. Gear: zonnesteen barnsteen-goud; vulkaniet basalt met gloeiende scheuren en verkoolde greep; getijdezilver zilver met jade-binding en -pommel; nulliet antraciet met cyaan/paarse chroma-split, een gat-pixel en geen kruisgreep-steen.
19. Templates: vulkaniet vlam uit een basalttablet, getijdezilver twee golven, nulliet void-ring; coatings een stenen tablet met het embleem in de materiaalkleur.
20. Verbruiksitems: veldfles leeg (kurk aan touwtje) / vol; ronde fles voor dauwdrank, zuiver water en hemellicht; handwarmer = leren buidel met venster; galmschelp = fossielschelp met gloeiende kern.
21. Effecticonen en -kleuren: cooled (druppel), cleansed `B8F0D0`, warmed `FF9A3C`, anchored `F4E6A6` (anker), resounding `B9A8F0` (schelp met bogen); balkjes veldfles blauw, handwarmer `FF8A2A`.
22. Blokken: hemelglas (cutout, wit-blauwe rand, fonkelkruis), hemelzand, glitchscherfblok (flikkerframes), zielkristal-, dauwkristal- en zwavelblok, zwavellamp, inleg (ruit / cabochon / kristal), vlotter als vastgesjorde platen.
23. Coating op het item-icoon leent een vanilla-kleurindex (hemelglas en dauwkristal allebei "diamant"); op het lichaam de eigen kleur.
24. Welke golf 1-placeholders (ingots, opslagblokken, coating-overlays) eerst vernieuwen?

## Gedrag
25. Creative: geen slok-, lading- of duurzaamheidsverlies; meterverhogende doses (jerky) doen niets in creative/spectator.
26. Veldfles vult ook aan waterlogged blokken, de cauldron zakt 1 niveau, vol = drinken; volle handwarmer + kristal = vanilla-wissel.
27. Drankjes: 32 ticks drinken, ook met volle honger; stack 16.
28. Pijlen: schade en vlucht als een gewone pijl; in `#minecraft:arrows` (breken dus ook druipsteen, chorus, potten).
29. Glitchpijl: spelers, bazen en bereden mobs springen niet; in ring 5 nooit naar buiten; geen veilige plek = niets; een waterdier kan op het droge belanden.
30. Galmschelp: geluid op "spelers" (niet "muziekblokken"), ~64 blokken, eigen audio; geen betoveringen of reparatie.
31. Lens (L): omlijning violet / cyaan / bleekwit door de grond heen; dempt glitch-fog en vervorming naar 25 %, de glitchstorm niet; plafonds 512 blokken / 256 gaten / 16 chunks; plat item-model.
32. Schoffel slaat bij elke tier voor 1; alle tiers brandvrij.
33. Zielkristalblok zonder tier-eis en licht, in tab 1; gloeibesstruik zonder eigen item (gloeibessen in `c:foods/berry`, compost 0,3).
34. Tabs: iconen en volgorde (in tab 5 staat de glitchpijl vóór de nulliet-set; in tab 3 en 4 staan de pijlen erna).
