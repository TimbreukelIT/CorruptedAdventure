# Eindtest fase 2 — live multiplayer- en visuele test (LIVE)

2026-10-28, 17:56–18:20. Branch `f2g5/live` op main `5fe1048a` (alles van fase 2). n = 64 (server 25634, RCON 25644).
Dedicated server via `tools/test/server.sh start f2live --fresh --client both` (seed 20260930, preset
`corruptedadventure:corrupted_world`) + 2 clients (`Dev`, `Dev2`) via `client.sh`. Alles via RCON, screenshots via
`client.sh shot`; de scripts staan in `build/test/f2live/` (`cond.py`, `echo.py`, `inv.py`, `armor.py`; niet
gecommit). Screenshots: `build/test/shots/f2live/` (niet gecommit).

**Testtooling (commit `infra:`):** `client.sh mouse <RDOWN|RUP|LCLICK|CORNER>` en `client.sh key E|1..9` (via
`win.ps1`, PostMessage naar het eigen client-venster). `RDOWN` … `RUP` houdt "gebruiken" ingedrukt: zo kijkt een client
door de lens, drinkt hij een drank en blaast hij op de galmschelp. `CORNER` zet de cursor linksboven (geen tooltip op
een inventaris-shot).

## 1. Multiplayer/server — ✅
- Server start zonder client-class-fouten (`build/test/server-f2live.log`: geen `ERROR`/`Exception` buiten de
  bekende log4j-/jar-meldingen, geen "Attempted to load class … invalid dist").
- `Dev` en `Dev2` joinen allebei (`list`: 2 spelers). `ModNetwork.PROTOCOL_VERSION` = `"11"`; NeoForge weigert een join
  bij een andere versie, dus beide kanten hebben 11.
- Geen ontbrekende modellen of textures in de clientlogs.

## 2. Condities met coatings en doses — ✅
Survival, per conditie: meter op 30, rate via `/ca condition get` kaal, met 4 gecoate nulliet-stukken
(`/item replace … [minecraft:trim={pattern,material}]`), gecoat + effect, alleen het effect.

| Conditie (plek) | Kaal | 4 coatings | Coating + effect | Alleen effect |
|---|---|---|---|---|
| uitdroging (r2, zon) | +0,40/s | +0,10 (0,25×) | +0,05 | +0,20 (`cooled`) |
| hitte (r3) | +0,55/s | +0,14 (0,25×) | +0,07 | +0,28 (`cooled`) |
| gif (r4, naast `toxic_pool`, `pool`) | +3,00/s* | +0,75 (0,25×) | +0,38 | +1,50 (`cleansed`) |
| kou (Nether-3) | +0,42/s | +0,10 (0,25×) | +0,05 | +0,21 (`warmed`) |
| void (r5, `outer`) | +0,55/s | +0,14 (0,25×) | +0,07 | +0,28 (`anchored`) |
| stilte (Nether-5, `zone`) | +0,33/s | +0,08 (0,25×) | +0,04 | +0,17 (`resounding`) |

\* De kale gif-meting kwam vóór de eerste tick naast de poel (`clean`, −1,00); +3,00 volgt uit 0,75 / 0,25 en 1,50 / 0,5.
Coatings en effecten vermenigvuldigen (0,25 × 0,5 = 0,125×).

**Doses echt gebruikt (client 1, rechtermuis via `client.sh mouse`):**
- **Hemellicht:** void 50 → −20, effect `anchored` 45 s, glazen fles terug.
- **Galmschelp** (Nether-5, beide 50 stilte): `Dev` blaast → −20 + `resounding` 45 s, duurzaamheid −1; `Dev` en `Dev2` op
  **12 blokken** krijgen allebei ~10 s `sound` (−2,00/s), `Dev2` zonder effect. Met `Dev2` op **24 blokken**: alleen
  `Dev`, `Dev2` blijft `zone` (+0,33/s). Een eerdere poging ~40 s na de vorige blaas deed niets: vermoedelijk de cooldown van 45 s (niet apart nagemeten).

## 3. Lens — ✅ (met één opmerking)
Ring 5 (−6258 99 6257), 4 `glitch_block`s achter een stenen muur, `Dev` met de lens, `Dev2` 3 blokken ernaast.
- **Omlijning:** zichtbaar door de muur heen, violet, met de scope-overlay van de verrekijker (`lens-b-c1-lens.png`);
  zonder lens niets (`lens-a-c1-nolens.png`).
- **Client 2 ziet niets** terwijl client 1 kijkt (`lens-c-c2-during.png`, `lens-i-storm1-c2.png`).
- **Glitch-fog/vervorming gedempt:** glitchstorm intensiteit 1,0 + void 85: zonder lens duidelijke violette ruis- en
  scanline-strepen (`lens-g-storm1-nolens.png`), met lens alleen nog zwakke donkere strepen (`lens-h-storm1-lens.png`).
- **Opmerking (cosmetisch, geen fix):** de restruis van de storm-overlay valt óver de zwarte rand van de scope heen
  (zwakke strepen in het zwart). Of de overlay onder de scope hoort, is een keuze voor main/T.
- **MSPT** (`/tick query`, 100 ticks, 2 spelers, ring 5): niemand kijkt 4,5 ms (P95 6,3); `Dev` kijkt 4,2 ms (P95 5,7).
  Geen meetbaar verschil (ruis).

## 4. Visueel
- ✅ **Gear, alle tiers** (`vis-inv-gear.png`): zonnesteen (hotbar, barnsteen), vulkaniet, getijdezilver, nulliet: 36
  stuks, allemaal getextureerd, geen paars-zwart, tiers goed van elkaar te onderscheiden.
- ✅ **Nieuwe items** (`vis-inv-new.png`): hemelzand, hemelglas, ruit, glitchscherfblok, glitchscherf, glitchpijl, lens,
  hemellicht, galmschelp, ruw nulliet, nullietstaaf, de blokken, alle templates, witte kern, zuiverkristalscherf,
  voidsteen en de verbruiksitems van golf 2–4: allemaal getextureerd. Klein: het item van **hemelzand en hemelglas**
  lijkt in de hotbar sterk op elkaar (allebei lichtgrijs-wit).
- ✅ **Nulliet-harnas op het lichaam** (`vis-armor.png`): kaal, met void-, gif- en stiltecoating, en void op
  getijdezilver: layers 1 + 2 goed, coatings zichtbaar.
- ❌ **Void- naast gifcoating: de kleuren zijn bijna gelijk** (`vis-armor-void-vs-poison.png`). De paletten liggen
  binnen ~5–10 RGB-stappen van elkaar: `sky_glass` `#cee5f6 … #374753`, `primordial_ice` `#d3e6f5 … #3a4752`
  (`textures/trims/color_palettes/`). Alleen het patroon (gebroken lijn vs druppels) verschilt. Ook `dew_crystal`
  (hitte) ligt in dezelfde lichtblauwe hoek. Gemeld aan main met opties.
- ✅ **Hemelglas geplaatst** (`vis-blocks-1.png`): blok 3 × 3 en ruit 3 × 3 voor eikenhout, transparant, de ruiten
  verbinden met elkaar en met de blokken ernaast; hemelzand, nullietblok en ruw nullietblok ernaast.
- ✅ **Glitchscherfblok** (`vis-blocks-1/2.png`): magenta-zwart, de animatie bestaat (4 frames, korte flikkers van 2–3
  ticks in ~100 ticks). Op twee shots met 0,7 s ertussen zag ik geen flikker; dat klopt met de frametijden.
- ✅ **Glitchpijl** (`vis-glitch-arrow.png`; zwevend gesummond, `NoGravity`, dus "in de vlucht"): model met
  magenta/cyane veren, een spoor van `glitch_pixel` in magenta, cyaan en zwart.

## Bijvangst (testopzet, geen bug in de mod)
- Een speler in survival die in de lucht hangt (ook met slow falling) wordt na ~4 s gekickt: "Flying is not enabled"
  (`allow-flight=false` in de testserver). Bij inloggen gebeurt dat direct opnieuw. Opgelost met water op de opgeslagen
  positie. Advies voor scripts: wissel pas naar survival als de speler op de grond staat.

## Opruimen
Clients 1 en 2 en de server zijn gestopt (`client.sh stop`, `server.sh stop`). `slots.sh status`: geen slots of locks
van LIVE meer.

**Opgelost (2026-10-28):** de void-coating heeft een eigen violet-blauw palet (`3970debe`, besluit gebruiker); gif blijft ijsblauw.
