# Teststop fase 2 — golf 1 t/m 4

Eén speelsessie na golf 4 (besluit gebruiker, 2026-10-27). Dit bestand groeit per golf; nu staat alleen het deel
**Golf 1 + 2** (basis, ring 2 + Nether-1) erin, golf 3 en 4 volgen.

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

# Golf 3
_(volgt)_

# Golf 4
_(volgt)_
