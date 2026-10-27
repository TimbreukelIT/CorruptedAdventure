# Teststop fase 2 — golf 1 t/m 4

Eén speelsessie na golf 4 (besluit gebruiker, 2026-10-27). Dit bestand groeit per golf; nu staan de delen
**Golf 1 + 2** (basis, ring 2 + Nether-1) en **Golf 3 + 4** (ring 3, Nether-3, ring 4, pijlen) erin.

# Golf 1 + 2

Twee golven in één deel. Bron: `docs/design/fase-2-items.md` §4 (golf 1) en §5 (golf 2); wat Claude al
automatisch testte staat in `docs/plans/fase-2/golf-2-INT.md`. Vink af, noteer bij elk punt kort wat je vindt.

**Start:** nieuwe wereld met de preset *Corrupted World*, cheats aan. Handig:
`/gamemode survival` · `/gamemode creative` · `/ca tpring 2` · `/ca tpring 3` · `/ca tpring 1 nether` ·
`/ca condition get` · `/ca condition set @s dehydration 60` · `/time set noon` · `/weather clear`.

## A. Golf 1 — basis
1. [ ] **Tabs:** vijf creative-tabs "Corrupted Adventure: Ring 1–5"; per tab eerst de blokken, dan de items; Nether-ring N
   staat in tab N (zielkristal in tab 1).
2. [ ] **Houtsets:** dood, palm, druipzwam, verkoold, rijpzwam, moonpetal, verdant en corrupted: planken, trap, plaat,
   hek, poort, deur, valluik, knop en drukplaat in de werkbank (receptenboek). Corrupted ook bord en hangbord.
3. [ ] **Steensets:** puimsteen, koude lava, tidestone, fossielsteen, witte steen en zout: vormen aan de werkbank en
   alles aan de steenzaag. Kloofkeien smelten naar oranje kloofsteen.
4. [ ] **Smelten:** ruwe zonnesteen en een zonnesteenerts in oven én hoogoven → `sunstone_ingot`; ruw turkoois →
   `turquoise`. (`/give @s corruptedadventure:raw_sunstone 8`)
5. [ ] **Opslagblokken:** 9 staven → blok → 9 staven (zonnesteen, turkoois, ruw); 4 zout ↔ zoutblok.
6. [ ] **Coating:** `/give @s corruptedadventure:dehydration_coating_smithing_template` + `turquoise` + een harnasstuk in de
   smeedtafel: coating zichtbaar op het lichaam en in de tooltip. Met 4 stukken in de zon van ring 2 stijgt uitdroging
   ~4× zo traag (`/ca condition get`: 0,10 i.p.v. 0,40/s).

## B. Golf 2 — zonnesteen (ring 2 + Nether-1)
7. [ ] **Template craften:** 6 zielkristal + 2 diamant + 1 scorched sandstone (`S D S / S C S / S D S`) → 1
   zonnesteen-template. Er is geen kopie-recept.
8. [ ] **Upgrade:** diamanten zwaard (met een enchantment) + template + `sunstone_ingot` in de smeedtafel → zonnesteen-
   zwaard; enchantment blijft. Idem voor een harnasstuk met een coating: de coating blijft. Netherite-gear werkt niet.
9. [ ] **Look:** zonnesteen-tools in hand/inventaris en het harnas (layer 1 + 2) op het lichaam naast goud en netherite
   (barnsteen-goud, geen emissive). Het template naast de drie andere in de smeedtafel.
10. [ ] **Mijnen (survival):** zonnesteen/turkoois-erts: ijzeren pickaxe dropt niets, diamant wel. Vulkaniet/zwavel/
    vuurrobijn (`/ca tpring 3`): diamant dropt niets, de zonnesteen-pickaxe wel; getijdezilver/jade nog niet.
11. [ ] **Brandvrij:** een zonnesteen-item in lava blijft liggen (zoals netherite).

## C. Golf 2 — uitdroging, turkoois, zout, zielkristal
12. [ ] **Veldfles craften** (` S ` / `LBL` / ` L `: string, leer, glazen fles) → leeg. Vullen: rechtsklik op water
    (bron blijft), een water-cauldron (zakt één niveau) en een dauwbron in Nether-ring 2 (`/ca tpring 2 nether`).
13. [ ] **Drinken (survival, ring 2, zon):** per slok −25 uitdroging, icoon "Verkoeld" 0:45 rechtsboven; uitdroging
    stijgt dan half zo snel (0,20 i.p.v. 0,40/s). Tooltip "Slokken: 3/4", blauw balkje in 4 stappen, model leeg/vol.
14. [ ] **Cactussap** (barrel cactus + glazen fles): −15, fles terug, verkort een lopende veldfles-tijd niet.
15. [ ] **Gezouten gedroogd vlees** (zout + gebakken vlees of vis → 2): snel gegeten, 6 honger, +8 uitdroging.
16. [ ] **Verkoeld:** relog binnen 45 s → loopt door; melk en de dood halen het weg.
17. [ ] **Uitdrogings-template uit een recept:** `T S T / S C S / T S T` (turkoois, scorched sandstone, barrel cactus)
    → 1; het kopie-recept (template + 7 turkoois + scorched sandstone → 2) werkt nog.
18. [ ] **Ingelegd zandsteen:** 8 scorched sandstone rond 1 turkoois → 8; trap/plaat/muur aan werkbank en steenzaag;
    naden tussen blok, trap, plaat en muur.
19. [ ] **Zoutlamp:** fakkel op een zoutblok → 1; licht 12; zij-, boven- en onderkant bekijken.
20. [ ] **Zielkristalblok:** 2×2 zielkristal → 1; tinkelt bij een treffer; mijnbaar met een houten pickaxe.
21. [ ] **Multiplayer (als je met twee speelt):** de slokken en het "Verkoeld" van de een veranderen niets bij de ander.

## D. Tuningvragen golf 1 + 2 (graag per punt: ok / anders, en hoe)
**Golf 1**
- Coatingsterkte 0,1875 per stuk (0,81× / 0,63× / 0,44× / 0,25×): te sterk, te zwak?
- Kopie-recept coating-template (template + 7 materiaal + ringsteen → 2): prijs ok?
- Placeholder-textures golf 1 (ingots behalve zonnesteen, turkoois, opslagblokken, coating-overlays): wat eerst?
- Tab-iconen en de volgorde in de tabs.
**Golf 2 — stats en kosten**
- Zonnesteen = netherite + duurzaamheid 2200 (netherite 2031) en harnasfactor 40 (37); brandvrij zoals netherite. Goed?
- Template-recept `S D S / S C S / S D S` (6 zielkristal + 2 diamant + 1 scorched sandstone): te duur / goedkoop
  (~3 chunks Nether-1 mijnen)? Zielkristal-ertstarieven laten zoals ze zijn?
- Upgrade neemt het item `sunstone_ingot` (niet de tag `c:ingots/sunstone`).
- NL-namen "Zonnestenen zwaard / … / kuras / beenstukken / laarzen".
- Veldfles −25 + 45 s ×0,5; cactussap −15 + 20 s; jerky +8 uitdroging, 6 / 1,2 voeding: balans?
- Veldfles-recept (string als touw), gecraft leeg, in creative geen slokverlies; jerky doet niets in creative.
- Cactussap: drinkduur 32 ticks, ook met volle honger; stack 16.
- Jerky: alle gebakken vlees én vis; NL-naam "Gezouten gedroogd vlees".
- Ingelegd zandsteen 8 → 8 (+ in `c:sandstone`), zoutlamp-recept (fakkel op zoutblok), zielkristalblok 2×2 zonder weg
  terug, zonder licht en zonder tier-eis, in tab 1.
- Uitdrogings-template-recept `T S T / S C S / T S T`.
**Golf 2 — textures**
- Zonnesteen-set (palet barnsteen-goud), template, harnas-layers.
- Veldfles leeg (bleek, kurk aan een touwtje) / vol (kurk erin); cactussap; jerky; icoon "Verkoeld" (druppel met rijp).
- Zielkristalblok, ingelegd zandsteen, zoutlamp (warm zout met gloed, houten voetrand).

# Golf 3 + 4

Ring 3 (vulkanisch), Nether-ring 3 (koud), ring 4 en de pijlen. Bron: `docs/design/fase-2-items.md` §6 en
`docs/plans/fase-2/golf-34-{G,C,A,R3,R4,T}.md`; wat Claude al automatisch testte staat in het INT-rapport golf 3 + 4.

## Zo start je
Nieuwe wereld *Corrupted World*, cheats aan, creative. Handig: `/ca tpring 3` · `/ca tpring 4` · `/ca tpring 5` ·
`/ca tpring 3 nether` · `/ca condition get` · `/time set noon` · `/gamemode survival`. Items: `/give @s
corruptedadventure:<id> <n>`. Gear-start: `sunstone_sword`, `volcanite_upgrade_smithing_template`, `volcanite_ingot 9`,
`tidesilver_upgrade_smithing_template`, `tidesilver_ingot 9`.

## E. Gear: vulkaniet en getijdezilver
22. [ ] **Templates craften:** 6 `dew_crystal` + 2 `sunstone_ingot` + 1 `pumice` (`S I S / S C S / S I S`) → vulkaniet-
    template; 6 `primordial_ice` + 2 `volcanite_ingot` + 1 `tidestone` → getijdezilver-template. Geen kopie-recept.
23. [ ] **Smithing:** zonnesteen-stuk (met enchantment én coating) + vulkaniet-template + `volcanite_ingot` → vulkaniet;
    daarna + getijdezilver-template + `tidesilver_ingot` → getijdezilver. Enchantment en coating blijven.
24. [ ] **Look:** vulkaniet (gloeiende scheuren, verkoolde greep) en getijdezilver (jade-binding/pommel) in hand en
    inventaris naast zonnesteen/netherite; harnas layer 1 + 2, ook alleen beenstukken; smeedtafel met alle templates.
    Ring 3-tab eindigt met de vulkaniet-set, ring 4-tab met de getijdezilver-set. Brandvrij in lava.
25. [ ] **Mijnen (survival):** vulkaniet/zwavel/vuurrobijn (`/ca tpring 3`): diamant niets, zonnesteen-pickaxe wel.
    Getijdezilver/jade (`/ca tpring 4`): zonnesteen niets, vulkaniet-pickaxe wel. Nulliet/witte kern (`/ca tpring 5`):
    vulkaniet niets, getijdezilver-pickaxe wel.
26. [ ] **Vulkaniet-blokken:** ruw blok en vulkanietblok 3 × 3 geplaatst (naden); erts op steen, basalt en deepslate.

## F. Condities: dauwdrank, zuiver water, handwarmer
27. [ ] **Dauwdrank** (`dew_crystal` + waterfles, vormloos): ring 3 in de zon −20 hitte, "Verkoeld" ~45 s (hitte stijgt
    half zo snel, `/ca condition get`), fles terug. De veldfles geeft ook "Verkoeld" maar haalt geen hitte af.
28. [ ] **Zuiver water** (1 `primordial_ice` + 4 glazen flessen → 4): ring 4 bij een gifpoel −20 gif, "Gezuiverd" 45 s
    (gif stijgt half zo snel), fles terug.
29. [ ] **Handwarmer** (`L F L / _ L _`: leer, `fire_crystal`) → gecraft **leeg** (asgrijs). Bijvullen: vuurkristal erop
    (vuursteengeluid) → 4 ladingen. In `/ca tpring 3 nether`: per gebruik −20 kou + "Verwarmd" 45 s (kou half zo snel),
    geknetter; tooltip, oranje balkje en model (leeg bij 0) lopen mee.
30. [ ] **Effecten:** "Gezuiverd"/"Verwarmd" met timer rechtsboven en in de inventaris; melk haalt ze weg; relog: ze
    lopen door. Met twee spelers: de ladingen van de een veranderen niets bij de ander.

## G. Pijlen
31. [ ] **Craften:** ` A ` / `AMA` / ` A ` (4 pijlen rond `sulfur`) → 4 `fire_arrow`; rond `toxic_crystal_shard` → 4
    `poison_arrow`.
32. [ ] **Schieten:** boog én kruisboog; Infinity geeft geen gratis pijlen. Brandpijl: mob brandt ~5 s; TNT, kampvuur
    (uit) en kaarsen gaan níet aan, geen vuurblok. Gifpijl: Gif I ~5 s. Spoor in de vlucht (vlammetje / groene swirl).
33. [ ] **Dispenser** schiet beide; opgeraapte pijl geeft het eigen item terug. Pijl-entity van dichtbij (punt, veren).

## H. Ring 3-blokken en -items
34. [ ] **Zwavel:** zwavel + houtskool → 2 buskruit (steenkool werkt niet); zwavel en `sulfur_block` branden in de oven
    zo lang als kolen/kolenblok; zwavelblok ↔ 9 zwavel; een zwavelblok vat geen vuur in de wereld.
35. [ ] **Gloeibessen:** rijpe struik plukken → `ember_berries`; ze planten de struik op as/mos; eten → 5 s
    vuurbestendigheid. Druipzwamsoep: kom terug.
36. [ ] **Puimsteen-vlotter** (`pumice_raft`, 4 puimsteen `# # / # #` → 2): alleen op lava te plaatsen (niet op water), je
    kunt erop staan, breekt als de lava weg is, direct kapot te slaan.
37. [ ] **Zwavellamp** (4 zwavel rond glas): licht 13, geelgroene gloed. **Dauwkristalblok** (2 × 2): tinkelt; staat aan
    het eind van tab Nether-2.

## I. Ring 4: ingelegde sets, coating-templates
38. [ ] **Ingelegde sets** (8 steen rond 1 edelsteen → 8): `fire_ruby_inlaid_pumice`, `jade_inlaid_tidestone`,
    `aquamarine_inlaid_lagoon_sandstone`; trap/plaat/muur aan werkbank en steenzaag; naden en bovenkant; mijnbaar met
    een houten pickaxe, droppen zichzelf (dubbele plaat = 2). Tab 4: achteraan na `jade_block`.
39. [ ] **Coating-templates uit het ringrecept:** hitte `R P R / P D P / R P R` (vuurrobijn, puimsteen, dauwkristal), gif
    `J S J / S C S / J S J` (jade, tidestone, oerijs), kou `F S F / S C S / F S F` (vuurkristal, koude lava, oerijs);
    de kopie-recepten werken nog. Gecoat harnas: in de juiste ring stijgt die conditie trager.
40. [ ] **Veen** in de oven: 6 items per blok.

## J. Tuningvragen golf 3 + 4 (graag per punt: ok / anders, en hoe)
**Gear (G)**
- Template-recepten `S I S / S C S / S I S` (Nether-materiaal, staaf van de vorige tier, ringsteen); geen kopie-recept.
- NL-namen "Vulkanieten …" / "Getijdezilveren …"; schoffel slaat bij elke tier voor 1; brandvrij.
**Consumables (C)**
- Dauwdrank vormloos dauwkristal + waterfles → 1, stack 16, 32 ticks; zuiver water 1 oerijs + 4 flessen → 4 (oerijs
  valt alleen met Silk Touch), stack 16.
- Handwarmer-recept `LFL / _L_`, geluiden (geknetter / vuursteen), volle handwarmer + kristal = vanilla-wissel, balkkleur
  `FF8A2A`; in creative gratis.
- Effectkleuren cleansed `B8F0D0`, warmed `FF9A3C`.
**Pijlen (A)**
- Receptvorm ` A ` / `AMA` / ` A ` → 4; schade en vlucht als een gewone pijl; spoor per tick; in `#minecraft:arrows`
  (breken dus ook puntig druipsteen, chorus en versierde potten); NL "Brandpijl" / "Gifpijl".
**Ring 3 (R3)**
- Hitte-template `RPR / PDP / RPR`; buskruit alleen met houtskool (→ 2); zwavelblok brandt niet in de wereld.
- Ingelegde puimsteen 8 → 8 (trap 6 → 4, plaat 3 → 6, muur 6 → 6; steenzaag 1/2/1); zwavellamp in redstonelamp-vorm met
  zeelantaarn-eigenschappen; vlotter 4 → 2, volle 16 × 16, direct kapot; dauwkristalblok 2 × 2 zonder weg terug.
- Gloeibesstruik zonder eigen item; gloeibessen in `c:foods/berry`, composteerbaar 0,3; druipzwamsoep in `c:foods/soup`.
**Ring 4 (R4)**
- Ingelegde sets droppen zichzelf; basisstenen tidestone en lagoon sandstone.
- Gif-template `JSJ / SCS / JSJ`; kou-template `FSF / SCS / FSF` met `cold_lava` als ringsteen (voorlopig).
**Textures (T)**
- Gear-silhouetten als zonnesteen; vulkaniet gloeiende scheuren + verkoolde greep; getijdezilver jade-binding en -pommel.
- Templates: vulkaniet vlam uit basalttablet, getijdezilver twee golven in getijdesteen, nulliet void-ring.
- Vulkaniet ruw/blokken herkleurd uit de ring 3-vormen; erts alleen palet.
- Dauwdrank/zuiver water in de cactussapfles; handwarmer = leren buidel met venster; iconen cleansed (mintgroene
  druppel) en warmed (vlam).
- Pijlen: zwavelpunt + rode veren / gifkristalpunt + groene veren.
- Zwavelblok, dauwkristalblok, zwavellamp; inleg (ruit / cabochon / langwerpig kristal); vlotter als vastgesjorde platen.
