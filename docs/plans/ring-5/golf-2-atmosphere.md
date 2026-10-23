# Ring 5 golf 2 — stroom A (atmosphere): 98 sfeer ring 5 + Nether-5

Scope: `atmosphere` — branch `ring5/98`, worktree `P:/Projecten/Java/wt/r5-98`, `setup.sh A 6` (server 25576, RCON
25586), basis `main` `1a31dbd6`. Commit-prefix `atmosphere:`.

**Status (2026-10-23, later): beantwoord — V98-1 … V98-11 = A** (gebruiker, zie Beslissingen); 98 (incl. 98-0) wordt
in één keer gebouwd. Oorspronkelijke status: geblokkeerd op de open punten van §2 (drie rondes van max. 4). Het ontwerp zegt
*wat* er komt (lucht per band, sterren overdag, dikke kleurloze fog, glitch-fog, echo-voetstappen, geluid, stilte-client),
maar niet *hoe sterk*, *welke bron* of *wanneer*; volgens CLAUDE.md §3 kiest A dat niet zelf. Hieronder is per onderdeel
de **aanbevolen optie uitgewerkt** (zoals `golf-1-atmosphere.md` met OV1); een ander antwoord verandert alleen dat
onderdeel. Wat al vastligt (§1) kan als voorpakket **98-0** meteen gebouwd worden als main dat wil (§1, laatste alinea).

Bronnen: `fase-1-wereld.md` → Ring 5 "Sparring ring 5" (Sfeer, Weer), "Terrein en sfeer", "Horror-elementen"
(echo-voetstappen), "Nether-tegenpool — Nether-ring 5" (Sfeer, Conditie: stilte), "Client/server-risico's", §1.6
(weermodel), §Geluid (productiemethode); `contract.md` §5 (stilte-meter, drempels; "visuals/volume van A, HUD-balk van
C"), §6 (sound events, particle types), §11 (stilte 30/60/90); `waves.md` §1 (bezit A), §2 golf 2; `golf-1-atmosphere.md`
(92: weer, `Ring5SoundDamping`, `SoundEngineVolumeMixin`, `GlitchStormOverlay`); `golf-1-conditions.md` §2 (drempel 30
van de void-meter = client, A 98; teleport zonder geluid, A 98); backlog r. 474–480 (98). Patroon: ring 4 golf 2
(`docs/plans/ring-4/golf-2-atmosphere.md`: `Ring4Atmosphere`, `NetherRing4Atmosphere`, `Ring4Ambience`, `NetherRing4Fog`,
`Ring4Sounds.java`).

**Bezit** (`waves.md` §1/§5): `client/weather/*` (ring 5 + Nether-5), `client/particle/*` (nieuwe providers),
`client/mixin/*` (de bestaande lucht-mixin uitbreiden; een nieuwe alleen als V98-2 = C), `ModSoundEvents` (blok ring 5),
`ModParticleTypes` (blok ring 5), `BiomeAmbience` (ring 5-haken) + nieuw `Ring5Atmosphere`/`NetherRing5Atmosphere`,
`ModSoundDefinitionsProvider`/`ModParticleDescriptionProvider` (ring 5-regels), `tools/sounds/Ring5Sounds.java` (nieuw),
`Ring5WeatherText` (ondertitels; ook ambient). **Niet:** de stilte-meter zelf, de darkness en de hartslag-schade (C, 97),
de HUD-balk (C, `ConditionHudLayer`), `VoidstoneBlock` (B), de biome-bootstraps buiten de haakregel (W-biomes/W-nether).

---

## 1. Wat al vastligt (geen vraag; voorpakket 98-0)

1. **Particle types** (contract §6; sprites van 99a liggen klaar): `void_mote` (`void_mote_0..3`), `glitch_pixel`
   (`glitch_pixel_0..3`), `reverse_raindrop` (`reverse_raindrop_0..1`) in `ModParticleTypes` (blok `// ring 5 — A`),
   `particles/<naam>.json` via `ModParticleDescriptionProvider`, providers in `client/particle/`:
   - `VoidMoteParticle`: zwart-violet stofje, stijgt traag (negatieve zwaartekracht), krimpt, 30–60 ticks, niet full
     bright (het zuigt licht weg);
   - `glitch_pixel` als extra stijl van `GlitchSpeckParticle` (springt per 2–4 ticks naar een andere sprite en positie
     ±0,3, flikkert aan/uit, full bright, magenta/zwart/wit);
   - `ReverseRaindropParticle`: zwart-violette druppel (`#1A1022`) die omhoog versnelt, 20–30 ticks, weg bij een plafond.
2. **Weer-renderers** (backlog r. 480): `GlitchStormRenderer` → `glitch_pixel` (was `glitch_speck`);
   `ReverseRainRenderer` → `reverse_raindrop` rond de camera (×sterkte × `SkyExposure`, boven `MOTION_BLOCKING`, met
   `WeatherParticles`), naast de `RainSheet`.
3. **Eigen audio voor de drie ring 5-weerloops** (backlog r. 480; §Geluid: eigen synthesescript, geen vanilla-audio):
   `tools/sounds/Ring5Sounds.java` (patroon `Ring4Sounds.java`: alleen JDK + ffmpeg, vaste seed per bestand,
   `StrictMath`, naadloze loops, `--wav-only`) → `sounds/weather/ring_5/glitch_storm_loop_1..2` (bitcrush-ruis,
   haperend), `reverse_rain_loop_1..2` (omgekeerde regen: aanzwellende tikken, achterstevoren), `standstill_loop_1`
   (vrijwel stil, lage toon + zachte tinnitus). `sounds.json` verwijst er dan naar (de vanilla-placeholders van 92 weg).
4. **Sound events** (contract §6): `ambient.ring_5.void_drone`, `ambient.ring_5.glitch`, `ambient.ring_5.echo_step`,
   `ambient.nether_ring_5.silence` in `ModSoundEvents`; eigen `.ogg`'s uit hetzelfde script (`sounds/ambient/ring_5/`,
   `sounds/ambient/nether_ring_5/`). Welke biome welke krijgt: V98-5/7.
5. **Sfeerhaken** (backlog r. 474): `BiomeAmbience.ring5(key)` → nieuw `worldgen/biome/Ring5Atmosphere` (de 6 looks uit
   `Ring5Biomes.LOOKS`, **kleuren ongewijzigd**, plus geluid/deeltjes/mood van V98-5) en `BiomeAmbience.netherRing5(key)`
   → nieuw `NetherRing5Atmosphere` (de fog-kleuren `E8E8EC`/`F4F4F6` en zwarte lucht van 87 ongewijzigd). In
   `Ring5Biomes`/`NetherRing5Biomes` alleen de haakregel (`effects(key)` → haak); `Look` blijft daar als `Climate` of
   verdwijnt (zoals 80 bij ring 4). **Let op:** 94/95 (W-biomes) en 96 (W-nether) werken parallel in dezelfde
   bootstrap-klassen (`generationSettings`); de wijziging van A is één methode, apart van de features → merge-volgorde bij
   INT2: eerst W-biomes/W-nether, dan A (of A rebased).
6. **"Geen muziek" in de stille biomes** (backlog r. 475): biome-`background_music` = een stille muziek (sound event
   `ambient.nether_ring_5.silence` als `Music` met `replaceCurrentMusic` true, vertraging max.) voor de tag `silent`
   (`false_meadow`, `white_halls`, `hollow_cathedral`); anders speelt vanilla `Musics.GAME`. Of heel ring 5 stil is: V98-5.

Client/server 98-0: registraties (beide kanten), biome-JSON (data, beide), client-assets en providers alleen via
`CorruptedAdventureClient` (`Dist.CLIENT`). Geen payload, geen gameplay. **98-0 kan nu**, onafhankelijk van §2: het
maakt de ids beschikbaar (o.a. `void_mote` voor `VoidstoneBlock` van B, nu `reverse_portal`; `glitch_pixel` voor de
features van 94/95). Punt 5 alleen als main de merge-volgorde met 94/95/96 zo wil, anders schuift het naar 98.

## 2. Open punten (vragen aan de gebruiker; aanbevolen optie eerst)

### Ronde 1 — lucht en fog

**V98-1 — Lucht in de buitenband: hoe wordt hij zwart, en blijven zon en maan?** (ontwerp: "buiten steeds zwarter
met sterren, ook overdag"; de binnenband houdt de biome-lucht `5E5868` grijs-violet)
- **A *(aanbevolen)*:** geleidelijk: 0 % zwart op de bandlijn (`Ring5Band` = 0), ~85 % aan de ringrand (9.700), 100 %
  vanaf ~9.900 boven de void; smoothstep op de afstand tot de bandlijn (op de gesyncte layout, dus zonder biome-
  sprongen). Zon en maan blijven, gedimd tot ~40 %; het lichtniveau verandert niet (alleen de weergave). *Complexiteit:*
  laag (bestaande `ClientLevelSkyColorMixin`). *Multiplayer:* geen (client). *Gevoel:* de wereld "gaat uit" naar buiten;
  dag/nacht blijft leesbaar.
- **B:** meteen zwart over de hele buitenband (volgt `Ring5Band` over de ~200 brede overgang), zon/maan zichtbaar.
  *Gevoel:* harde, duidelijke grens; minder "steeds".
- **C:** als A, maar zon en maan verdwijnen in het zwarte deel. *Gevoel:* sterkste void; de tijd zie je alleen nog aan
  het licht (desoriënterend). *Complexiteit:* laag-middel (een tweede mixin-inject in de lucht-render).

**V98-2 — Sterren overdag: waar en hoe?**
- **A *(aanbevolen)*:** sterhelderheid = max(vanilla, zwartheid uit V98-1): in de buitenband overdag zodra de lucht
  donker wordt, vol aan de rand; in de binnenband alleen 's nachts (vanilla). Kijk je naar beneden in de void, dan zie je
  ze ook onder je (vanilla tekent een volle sterrenbol; dat komt vanzelf mee). *Complexiteit:* laag (mixin-inject op
  `ClientLevel#getStarBrightness`, zelfde mixinbestand).
- **B:** als A, plus heel zwakke sterren (~15 %) overdag óók in de binnenband ("net fout"). *Gevoel:* subtiel fout, ook
  dichtbij huis.
- **C:** als A, maar dichter en helderder dan vanilla in de buitenband (eigen sterrenlaag). *Complexiteit:* middel-hoog
  (eigen sky-pass, risico met Sodium/shaderpacks).

**V98-3 — Fog in de binnenband: hoe dik en welke kleur?** (ontwerp: "dikke kleurloze fog")
- **A *(aanbevolen)*:** far = 0,45 × kijkafstand (min. 48, max. 112 blokken), near = 0,15 × far; kleur = grijswaarde
  (luma) van de biome-fog × daglicht (min. 0,15): kleurloos. Naar buiten dunt hij met de zwartheid uit naar vanilla's
  afstand en een zwarte kleur, zodat je in de buitenband ver over de void kijkt (pinakels en bruggen zichtbaar).
  Onder water/lava, blind of zonder hemel: vanilla (`SkyExposure`, patroon `Ring4Fog`). *Complexiteit:* laag.
- **B:** heel dik (far 32–40, "Silent Hill"). *Gevoel:* sterkste horror; navigeren wordt lastig, ook naar de bandlijn
  en de kastelen.
- **C:** matig (far 0,65 × kijkafstand). *Gevoel:* meer uitzicht, minder "dik".

**V98-4 — Glitch-fog: wat doet hij?** (ontwerp alleen: "afwijkend fog-gedrag (glitch-fog) via client-code")
- **A *(aanbevolen)*:** de fog-rand **hapert**: om de 4–12 s springt de fog-afstand 1–3 ticks hard naar 40–70 % (alsof
  een frame wegvalt), 1 op 4 keer met een violet-zwarte fogkleur; in `glitch_fields` twee keer zo vaak; in beide banden,
  niet tijdens een glitchstorm (die heeft eigen flitsen). × vanilla's "Screen Effect Scale" (0 = uit, voor
  lichtgevoeligheid). *Complexiteit:* laag (`ViewportEvent.RenderFog`).
- **B:** fog die **verkeerd reageert**: komt dichterbij als je stilstaat (tot ~50 % in 10 s) en trekt terug als je
  beweegt. *Gevoel:* geen flikkering, wel unheimisch "iets kijkt mee".
- **C:** **gekwantiseerde** fog: de afstand verspringt in stappen van 16 blokken terwijl je loopt (blokkerig "net fout").
  *Gevoel:* subtiel, technisch.
- (A en B zijn te combineren; zeg het als je beide wilt.)

### Ronde 2 — geluid

**V98-5 — Geluid per biome en muziek in ring 5** (contract §6 kent `void_drone`, `glitch`, `echo_step`)
- **A *(aanbevolen)*:** binnenband bijna stil: `corrupted_wastes` geen loop, zeldzaam een `glitch`-one-shot (additions
  0,002); `glitch_fields` dezelfde one-shots vaker (0,008) + `glitch_pixel` als biome-deeltje; `false_meadow` niets (stil,
  ontwerp). Buitenband: `void_drone` als loop in alle drie, `void_mote` als biome-deeltje (het meest in
  `shattered_expanse`). Grotgeluid (mood): de vervormde grotklank van ring 1 (`STRANGE`) in de binnenband, geen in de
  buitenband. **Muziek: geen in heel ring 5** (stille muziek, §1.6). *Gevoel:* de stilte is de muziek; het geluid zegt
  "binnen dood, buiten leegte".
- **B:** als A, maar vanilla-muziek blijft in ring 5 (alleen `false_meadow` en Nether-5 stil).
- **C:** als A, plus een zachte, lagere drone ook in de binnenband (`void_drone` op lage toon als loop).

**V98-6 — Echo-voetstappen: wanneer en hoe?** (ontwerp: "je eigen voetstappen klinken even later nog eens, alsof iemand
je volgt"; client-only, per speler, beide banden)
- **A *(aanbevolen)*:** **in vlagen**: na 1–3 min lopen in ring 5 volgt voor 4–10 stappen elke stap een echo 0,4–0,8 s
  later (vast per vlaag), 6–12 blokken achter je, met het stapgeluid van het blok waar jij liep, iets zachter (0,5–0,7)
  en 5 % lager. Stop je, dan valt er nog precies één echo ná je stilstand. Ook in grotten en in `false_meadow` (daar het
  enige geluid). *Complexiteit:* laag-middel (client-luisteraar op de eigen stapgeluiden + een wachtrij).
- **B:** elke stap altijd een echo. *Gevoel:* went snel; verliest het effect.
- **C:** alleen als je stopt: 1–3 extra stappen na je stilstand, bij ~1 op 8 keer stoppen. *Gevoel:* per keer het
  engst, maar zeldzaam.
- Sub-keuze klank: **(a) *(aanbevolen)*** het vanilla-stapgeluid van het blok (klinkt als jouw stap); **(b)** een eigen,
  galmende echo (`ambient.ring_5.echo_step`, `.ogg` uit het script) — duidelijker "iemand anders".

**V98-7 — Nether-5: fog en het "stilte"-geluid** (ontwerp: "witte fog, geen ambient-geluid, geen muziek, bijna geen
particles")
- **A *(aanbevolen)*:** **wit en ver**: far = 0,8 × kijkafstand (max. 192), near = 0,4 × far, kleur de biome-fog
  (`E8E8EC` hallen, `F4F4F6` kathedraal), geen pulsering; de zuilrasters lossen op in het wit ("eindeloos en leeg").
  Geen ambient-loop, geen mood, geen muziek (stille muziek `ambient.nether_ring_5.silence`, §1.6); "bijna geen particles"
  = heel zelden een wit stofje (vanilla `white_ash`, 0,0005).
- **B:** **dik wit** (vanilla's Nether-fog, ~0,1–0,5 × far): je ziet maar een paar zuilen. *Gevoel:* desoriënterend,
  kleiner.
- **C:** als A, maar de fog wordt dikker naarmate de stilte-meter stijgt (de wereld verdwijnt mee).

**V98-8 — Void-teleport (meter ≥ 60): geluid en deeltjes?** (91 teleporteert stil; backlog r. 478)
- **A *(aanbevolen)*:** de server speelt bij elke teleport `ambient.ring_5.glitch` op de oude én de nieuwe plek
  (iedereen in de buurt hoort het) en een plukje `glitch_pixel` op beide plekken. Eén regel in C's `VoidTeleport` naar
  een haak van A (`Ring5Glitches.teleported(player, from, to)`, common, server-side), af te stemmen met stroom C.
  *Multiplayer:* medespelers zien je verspringen.
- **B:** alleen de speler zelf hoort het (server stuurt het geluid alleen naar hem), geen deeltjes.
- **C:** geen geluid of deeltjes (stil verspringen is enger).

### Ronde 3 — meters op de client

**V98-9 — Stilte (Nether-5): hoe verdwijnt het geluid?** (drempels 30/60/90: gedempt → HUD weg → alle geluid weg +
darkness → schade/hartslag)
- **A *(aanbevolen)*:** vanaf 30 zakt al het wereldgeluid en de muziek (niet de UI/`MASTER`) geleidelijk van 100 % naar
  35 % bij 90; bij 90 binnen ~1 s naar 0. Alleen een **hartslag** blijft hoorbaar zolang de meter ≥ 90 (eigen `.ogg`,
  nieuw event `ambient.nether_ring_5.heartbeat`, sneller naarmate de meter stijgt; de schade zelf is van C). Bel, goat
  horn enz. worden ook gedempt (hun werking op de meter is server-side). *Complexiteit:* laag (uitbreiding van
  `Ring5SoundDamping`/`SoundEngineVolumeMixin`).
- **B:** in stappen: 30 → 50 %, 60 → 25 %, 90 → 0. *Gevoel:* elke drempel duidelijk merkbaar, minder sluipend.
- **C:** als A, maar zonder hartslag: bij 90 is echt alles stil.
- Niet voorgesteld: een echte "dofheid" (low-pass); dat kan in 1.21.1 alleen via OpenAL-EFX op de geluidskanalen
  (eigen mixin, hoog risico). Dempen = volume.

**V98-10 — Stilte bij 60: welke HUD verdwijnt?** (ontwerp: "HUD weg (behalve hotbar)")
- **A *(aanbevolen)*:** weg: hartjes, honger, pantser, lucht, XP-balk en -level, effect-iconen, bossbalken, scorebord;
  blijven: hotbar (+ itemnaam), crosshair, de conditiebalken (je ziet hoe ver de stilte is) en chat. Fade ~1 s.
- **B:** strikt: alles weg behalve de hotbar (ook crosshair, conditiebalk en chat). *Gevoel:* sterkst; bouwen/mikken en
  multiplayer-chat worden lastig.
- **C:** als A, maar ook de conditiebalken weg (je voelt het, je ziet het niet).

**V98-11 — Void-meter drempel 1 (30): welke glitch-visuals?** (ontwerp alleen "glitch-visuals, client, gestuurd door
de gesyncte meter"; 91 doet server-side niets)
- **A *(aanbevolen)*:** een lichte vorm van de glitchstorm-overlay die meegroeit met de meter (30 → 100: 0 → ~60 % van
  de storm): spaarzame ruisblokjes, af en toe een scanline-band, `glitch_pixel` rond de speler; geen hotbar-hapering (die
  blijft van de storm); × "Screen Effect Scale". *Complexiteit:* laag (hergebruik `GlitchStormOverlay`).
- **B:** kleurscheiding (chromatische aberratie) als post-shader, sterker met de meter. *Complexiteit:* middel (eigen
  `PostChain` zoals `HeatHazeEffect`), kan botsen met shaderpacks.
- **C:** blokken in beeld verspringen heel even naar de `glitch_block`-texture (alleen rendering). *Complexiteit:* hoog
  (chunk-herrender, duur); niet aanbevolen.

## 3. Uitwerking (bij de aanbevolen opties; per vraag aan te passen)

### 3.1 Lucht, sterren, fog (overworld ring 5) — V98-1/2/3/4
- Nieuwe pure klasse **`client/weather/Ring5Sky`**: per client-tick de camerastand (patroon `Ring4SkyState`): ring 5-
  gewicht (`RingWeatherMix.weight`, ook de void-rand), `Ring5Band.of(ClientRingLayout).value`, `blackness` (V98-1:
  smoothstep van de bandlijn naar de rand/void), `innerFog` (1 − blackness); niets gesynct. Pure kern
  `Ring5Sky.blackness(bandValue, progressTowardsEdge)` (getest).
- `ClientLevelSkyColorMixin`: lucht ná het weer en vóór `SunderedFade`: `Ring5Sky.skyColor` (naar zwart × blackness);
  nieuwe inject `getStarBrightness` RETURN → `max(vanilla, blackness)` (× (1 − regen), zoals vanilla). Zon/maan gedimd
  via een inject op `LevelRenderer#renderSky` (alleen de kleur-alpha van zon/maan; V98-1 A/C) — in hetzelfde
  mixinbestand als dat kan, anders een nieuw (`LevelRendererSkyMixin`).
- **`client/weather/Ring5Fog`** (patroon `Ring4Fog`, `RenderFog` op `LOWEST`, alleen dichterbij dan wat er al is, zodat
  het weer wint waar het dichter is): binnenband far 0,45 × zicht (48–112), near 0,15 × far, kleur luma × daglicht;
  naar buiten uit met `innerFog`; fog-kleur in de buitenband naar zwart (`ComputeFogColor` op `HIGH`). Niet onder
  water/blind/zonder hemel.
- **`client/weather/GlitchFog`**: de hapering van V98-4 A (eigen timer per client, `RandomSource.create()` is hier
  prima: client-only, geen worldgen), na `Ring5Fog`, uit tijdens glitchstorm-aandeel > 0,5.

### 3.2 Ambience ring 5 — V98-5/6
- `Ring5Atmosphere` (datagen, §1.5): `Look(colours, grass, foliage, particle, loop, additions, mood)` per biome volgens
  V98-5; `BiomeAmbience.ring5(key)`; muziek: stil (§1.6) in alle 6 (V98-5 A) of alleen `false_meadow` (B).
- **`client/weather/Ring5Ambience`** (patroon `Ring4Ambience`, per speler, niet gesynct): echo-voetstappen (V98-6 A):
  `PlaySoundEvent`-luisteraar die de stapgeluiden van de lokale speler herkent (`SoundType#getStepSound` van het blok
  onder hem, positie ≈ speler), een wachtrij met tijdstempels, de echo als `SimpleSoundInstance` (`PLAYERS`, positioneel)
  op 6–12 blokken achter de kijkrichting; de vlaagtimer telt alleen gelopen afstand in ring 5 (`ClientRingLayout`). De
  echo's zelf gaan niet door de luisteraar terug (eigen instance-markering), dus geen kettingreactie. Pure kern
  `EchoSteps` (vlaag, vertraging, plek; getest).

### 3.3 Nether-5 — V98-7
- `NetherRing5Atmosphere` (datagen): fog-kleuren van 87, zwarte lucht, `white_ash` 0,0005, geen loop/mood/additions,
  stille muziek. `BiomeAmbience.netherRing5(key)`.
- **`client/weather/NetherRing5Fog`** (patroon `NetherRing4Fog` zonder puls): far 0,8 × zicht (max. 192), near 0,4 × far,
  inloop ~2,5 s op de tag `is_nether_ring_5`; reset bij uitloggen.

### 3.4 Stilte-client (Nether-5) — V98-9/10 (meter van C, 97)
- **Interface (contract §5, 97 bouwt de serverkant):** `Condition.SILENCE` (naam `silence`, achteraan na `VOID`), de
  gesyncte waarde via de bestaande `condition_sync` in de attachment van de lokale speler:
  `player.getData(ModAttachments.CONDITION_METERS).get(SILENCE)`; drempels `ConditionDefinitions.thresholdLevels(SILENCE)`
  (= `ServerConfig.SILENCE.thresholds`, gesyncte SERVER-config, standaard 30/60/90). Geen nieuwe payload. Staat
  `SILENCE` nog niet op main als A bouwt, dan zoekt A de conditie **op naam** (`"silence"`, één helper
  `Ring5Meters.silence()` die `Optional<Condition>` geeft) en vallen de effecten weg tot 97 gemerged is; de drempels
  dan uit één eigen constante (30/60/90) met een TODO naar `thresholdLevels` — bij INT2 omzetten.
- **`client/weather/SilenceEffects`** (pure kern + client-glue): `volume(meter, t1, t3)` (V98-9 A: 1 → 0,35 tussen 30 en
  90, dan in ~1 s naar 0, gesmoothd per tick); `Ring5SoundDamping.volume` vermenigvuldigt de stilte-factor mee (één
  mixin, alle categorieën behalve `MASTER`); de hartslag (`ambient.nether_ring_5.heartbeat`, niet positioneel; de instance
  wordt door `Ring5SoundDamping` herkend en niet gedempt) speelt bij ≥ t3 met een interval 1,0 → 0,5 s.
- **HUD (V98-10 A):** `RenderGuiLayerEvent.Pre` annuleert de lagen `PLAYER_HEALTH`, `FOOD_LEVEL`, `ARMOR_LEVEL`,
  `AIR_LEVEL`, `EXPERIENCE_BAR`, `EXPERIENCE_LEVEL`, `EFFECTS`, `BOSS_OVERLAY`, `SCOREBOARD_SIDEBAR` (+ `VEHICLE_HEALTH`,
  `JUMP_METER`) zodra de meter ≥ t2; fade via een alpha-stap is in 1.21.1 per laag lastig → harde wissel met 1 s
  hysterese (onder t2 − 2 komt hij terug). Hotbar, itemnaam, crosshair, `corruptedadventure:conditions` en chat blijven.

### 3.5 Void-meter drempel 1 — V98-11
- **`client/weather/VoidGlitchOverlay`**: GUI-laag naast `glitch_storm` (hergebruikt de ruistekening van
  `GlitchStormOverlay` via een gedeelde pure helper), sterkte = clamp((meter − t1) / (100 − t1)) × 0,6 ×
  `screenEffectScale`; `glitch_pixel` rond de speler (0 → 0,5/tick). Meter `VOID`, drempels
  `ConditionDefinitions.thresholdLevels(VOID)` (bestaan).

### 3.6 Void-teleport — V98-8
- `condition/…` niet van A: A levert `weather/Ring5Glitches.teleported(ServerPlayer, Vec3 from, Vec3 to)` (common,
  server: `level.playSound(null, …, ambient.ring_5.glitch, AMBIENT)` + `sendParticles(glitch_pixel, …)` op beide
  plekken); C roept hem aan in `VoidTeleport` (één regel; afstemmen met 97 of via main bij INT2).

## 4. Geluid (`tools/sounds/Ring5Sounds.java`, eigen audio, mono Ogg Vorbis)

| Bestand(en) | Event | Klank (eerste versie) |
|---|---|---|
| `weather/ring_5/glitch_storm_loop_1..2` | `weather.ring_5.glitch_storm` | bitcrush-ruis met haperingen en korte stiltes |
| `weather/ring_5/reverse_rain_loop_1..2` | `weather.ring_5.reverse_rain` | omgekeerde regen (aanzwellende tikken, achterstevoren) |
| `weather/ring_5/standstill_loop_1` | `weather.ring_5.standstill` | bijna stil: lage toon + zachte tinnitus |
| `ambient/ring_5/void_drone_loop_1..2` | `ambient.ring_5.void_drone` | laag, zuigend, langzaam zwellend (onder 60 Hz met overtonen) |
| `ambient/ring_5/glitch_1..4` | `ambient.ring_5.glitch` | korte digitale knetter/stotter, 0,2–0,6 s |
| `ambient/ring_5/echo_step_1..3` | `ambient.ring_5.echo_step` | alleen bij V98-6 (b): doffe stap met galm |
| `ambient/nether_ring_5/silence_loop_1` | `ambient.nether_ring_5.silence` | vrijwel stil (ruisvloer −60 dB), als stille muziek |
| `ambient/nether_ring_5/heartbeat_1..2` | `ambient.nether_ring_5.heartbeat` (nieuw) | alleen bij V98-9 A: dubbele doffe slag |

Geen placeholders nodig zolang het script draait (ffmpeg staat lokaal; anders `--wav-only` en de `.ogg`'s later). Valt
het script uit, dan **tijdelijk** vanilla-`event`-verwijzingen met de naam van het eigen event (zoals 92) en een
backlogregel. Ondertitels (en + nl) in `Ring5WeatherText` (one-shots; loops zonder, conventie).

## 5. Client/server-impact
- **Server:** alleen biome-data (JSON), registraties (sound events, particle types) en — bij V98-8 A — de server-haak
  `Ring5Glitches` (geluid en `sendParticles`, geen gameplay). De stilte-meter, darkness en schade blijven van C.
- **Client:** alles in `client/` (`Ring5Sky`, `Ring5Fog`, `GlitchFog`, `EchoFootsteps`, `NetherRing5Fog`,
  `SilenceEffects`, `VoidGlitchEffects`, providers), geregistreerd vanuit `CorruptedAdventureClient` (`Dist.CLIENT`); de
  mixins alleen in de `client`-lijst. Geen `Minecraft.getInstance()` buiten `client/`; `worldgen/biome/*Atmosphere` en
  `Ring5Glitches` noemen geen client-klassen (`ClientIsolationTest`).
- **Netwerk:** geen nieuwe payload en geen protocolwijziging door A (de meters gaan via `condition_sync`; de lucht rekent
  op de gesyncte layout). 97 voegt `SILENCE` toe → INT2 verhoogt `PROTOCOL_VERSION` (niet A).
- **Multiplayer:** lucht, fog, echo, stilte-effecten en glitch-visuals zijn per client (eigen meter, eigen positie);
  alleen het teleport-geluid (V98-8 A) is gedeeld.

## 6. Tests en gates
- Unit (pure kernen; gebouwd als één `client/weather/Ring5AtmosphereTest`): `Ring5SkyTest` (blackness 0 op de bandlijn, monotoon, ~0,85 op de rand, 1 boven de void; sterren
  = max), `Ring5FogTest` (far/near binnen de grenzen, luma-kleur grijs, uitdunnen), `GlitchFogTest` (interval, duur,
  Screen Effect 0 = uit), `EchoStepsTest` (vlaag 4–10, vertraging vast per vlaag, één echo na stilstand, plek achter de
  kijkrichting), `SilenceEffectsTest` (volume 1 onder t1, 0,35 op t3, 0 erboven, HUD-hysterese), `VoidGlitchOverlayTest`
  (sterkte 0 → 0,6), `BiomeAmbienceTest` (ring 5- en Nether-5-haken; kleuren gelijk aan vóór 98), `Ring5BiomesTest`/
  `NetherRing5BiomesTest` (effecten = haak, stille muziek in `silent`).
- **B:** `./gradlew build --offline` groen (incl. `ClientIsolationTest`).
- **D:** `./gradlew runData --offline`; gecommit: `sounds.json`, `particles/{void_mote,glitch_pixel,reverse_raindrop}.json`,
  lang (en + nl), de biome-JSON van de 6 + 2 biomes; tweede run → `git diff --exit-code src/generated`.
- **S:** `bash tools/test/server.sh start r5-98 --fresh` → log zonder ERROR en zonder client-class-fout, RCON `/ca ring`,
  `/ca weather 5 glitch_storm`, `/ca condition …` (stilte, als 97 er is), `server.sh stop`.
- **Client** (`client.sh`) alleen als de machine vrij is; anders de checklist hieronder voor de gebruiker (teststop 6b).
- `/code-review medium` op `main...ring5/98`.

**In-game checks voor de gebruiker (6b), per antwoord:** lucht en sterren van de bandlijn tot de void (dag en nacht);
fog binnen/buiten en de glitch-fog (ook met Screen Effect Scale 0); echo-voetstappen (binnen, buiten, grot,
`false_meadow`); geluid per biome en geen muziek; Nether-5 (fog, stilte, stille muziek); stilte 30/60/90 met twee
spelers (`/ca condition set … silence 35/65/95`); void-meter ≥ 30 (visuals) en ≥ 60 (teleportgeluid voor een tweede
speler); de drie weertypes met de nieuwe audio en deeltjes.

## 7. Koppeling met C (97)
- A leest `SILENCE` alleen via de gesyncte attachment en `thresholdLevels`; geen eigen sync.
- A dempt; C doet darkness en schade. De hartslag-klank (V98-9 A) is van A en hangt aan de gesyncte meter, niet aan het
  schade-event (zelfde drempel).
- V98-8 A: C roept `Ring5Glitches.teleported` aan in `VoidTeleport` (één regel).

## Voortgang
- Voortgang: 2026-10-23 15:00 worktree `P:/Projecten/Java/wt/r5-98` op `ring5/98` vanaf `1a31dbd6`, `setup.sh A 6`.
- Voortgang: 2026-10-23 15:15 bronnen gelezen; plan met 11 open punten geschreven; **wacht op de antwoorden** (§2).
- Voortgang: 2026-10-23 15:25 antwoorden binnen (alle elf A, via main); 98 + 98-0 gebouwd.
- Voortgang: 2026-10-23 16:00 `build --offline` groen (incl. `ClientIsolationTest`, `Ring5AtmosphereTest` 9,
  `BiomeAmbienceTest` 19); `runData --offline` groen (gegenereerd: lang, `sounds.json`, 3 particle-JSON's, 8 biome-JSON's);
  `Ring5Sounds.java` → 17 `.ogg`'s (ffmpeg lokaal).
- Voortgang: 2026-10-23 16:06 S-smoke `r5-98` (poort 6, verse wereld): `Done`, log zonder ERROR en zonder
  client-class-fout; RCON `/ca ring`, `/ca weather 5 glitch_storm`, `/ca weather`, `/ca castles`; gestopt. Geen client
  (slots bezet door andere stromen, o.a. perf van W3): de checklist van §6 is voor de gebruiker (6b).
- Voortgang: 2026-10-23 16:40 `/code-review medium` (main...ring5/98, 9 bevindingen). Opgelost: (1) de eigen stappen
  komen als `PlayLevelSoundEvent.AtEntity` (`LocalPlayer#playSound`), niet `AtPosition` — de echo luisterde naar het
  verkeerde event; (2)+(6) "stilstand" = 12 ticks zonder beweging (niet de staptussenruimte: sluipen/zielenzand), terugval
  van de pas 8 ticks (`DEFAULT_GAP`); (3) `NetherRing5Fog` ook op een door `NetherRing4Fog` geannuleerd event (beide
  faden over elkaar); (5) glitch-fog pas vanaf ring 5-gewicht ½ (zoals de echo), niet al diep in zone 4→5; (8) het
  ongebruikte event `ambient.ring_5.echo_step` + 3 `.ogg`'s + ondertitel weg; (9) geen allocatie per frame voor de
  zonsopgang-gloed. Bewust niet: (4) "dubbel zwart" — vanilla mengt de fog maar voor `1 − (0,25 + 0,75·rd/32)^¼` (≈ 15 %
  bij 12 chunks, max. ~26 %) naar de lucht; het extra verschil is ≤ k·b·(1 − b), enkele procenten, en de
  buitenband-biomes hebben een lichtere fog dan lucht, dus geen donkere band; bij 6b nakijken. (7) categorie: de
  aanroep in `VoidTeleport` en de haakregels in `Ring5Biomes`/`NetherRing5Biomes` zijn op opdracht van main (V98-8 A,
  "minimaal, INT2-mergepunt") en volgen het patroon van ring 3/4 (`BiomeAmbience`-haken); vastgelegd in de backlog.

## CODEBASE-delta (gebouwd; INT schrijft hem in `CODEBASE.md`)
- `registry/ModParticleTypes` (blok `// ring 5 — A`): `void_mote`, `glitch_pixel`, `reverse_raindrop`; `particles/*.json`
  via `ModParticleDescriptionProvider` (sprites van 99a: `void_mote_0..3`, `glitch_pixel_0..3`, `reverse_raindrop_0..1`).
  Providers zonder nieuwe klassen: `DriftingHazeParticle.VOID_MOTE` (stijgt traag, geen tint), `GlitchSpeckParticle`
  met record `Jitter` (`SPECK` 0,15/1 op 6 voor `glitch_speck`, `PIXEL` 0,3/1 op 3 voor `glitch_pixel`),
  `RisingBubbleParticle.REVERSE_RAINDROP` (schiet omhoog, stopt onder een dak).
- `registry/ModSoundEvents` (blok `// ring 5 — A`): `ambient.ring_5.{void_drone,glitch}`,
  `ambient.nether_ring_5.{silence,heartbeat}` (`heartbeat` nieuw t.o.v. het contract; **`echo_step` niet geregistreerd**:
  V98-6 a speelt het stapgeluid van het blok, een ongebruikt event + `.ogg`'s viel in de review weg). `ModSoundDefinitionsProvider`: de drie ring 5-weerloops en de vijf nieuwe events
  naar eigen `.ogg`'s (geen vanilla-placeholders meer voor ring 5). `datagen/lang/Ring5WeatherText` + ondertitels
  `ambient.ring_5.glitch`, `ambient.nether_ring_5.heartbeat` (en + nl).
- **Nieuw `tools/sounds/Ring5Sounds.java`** (patroon `Ring4Sounds.java`; `java tools/sounds/Ring5Sounds.java [ffmpeg]`,
  `--wav-only`) → 14 `.ogg`'s: `sounds/weather/ring_5/{glitch_storm_loop_1..2, reverse_rain_loop_1..2, standstill_loop_1}`,
  `sounds/ambient/ring_5/{void_drone_loop_1..2, glitch_1..4}`,
  `sounds/ambient/nether_ring_5/{silence_loop_1, heartbeat_1..2}`.
- `worldgen/biome/`: nieuw `Ring5Atmosphere` (`Look(particle, particleChance, drone, glitchChance, strangeCaves)` per
  biome) en `NetherRing5Atmosphere` (`ASH_CHANCE` 0,0005 `white_ash`); `BiomeAmbience.ring5(key, colours)`,
  `netherRing5(key, colours)` en `SILENT_MUSIC` (= `Music` op `ambient.nether_ring_5.silence`, 12.000–24.000, vervangt
  lopende muziek). `Ring5Biomes.bootstrap` en `NetherRing5Biomes.effects`: één haakregel; **de kleuren blijven in de
  biome-klassen** (minimale wijziging naast 94/95/96; backlog r. 474 daarmee half: de geluiden zitten in de haak, de
  kleuren verhuizen niet).
- `weather/Ring5Glitches` (common, server): `teleported(player, from, to)` = `ambient.ring_5.glitch` (`PLAYERS`, 0,8)
  + 12 `glitch_pixel` op beide plekken. **`condition/VoidTeleport` (C):** één aanroep vóór `teleportTo` (+ 2 imports,
  doc-regel) — **INT2-mergepunt met 97**.
- `client/weather/` (alles via `Ring5ClientAtmosphere.register(gameBus)` uit `CorruptedAdventureClient`):
  `Ring5Sky` (camerastand per tick: ring 5-gewicht, `blackness` = smoothstep van de bandlijn naar 0,85 op de ringrand,
  1 vanaf 200 voorbij; `innerFog`; `skyColor`, `starBrightness` (≥ 0,5 × zwart), `sunFactor` (→ 0,4),
  `sunriseColor`), `Ring5Fog` (binnenband-fog `innerFar` = 0,45 × zicht in 48–112, near 0,15 ×, lucht-fog × 2, kleur
  naar luma op `HIGH`; zwart en glitch-violet op `LOW`; afstanden op `LOWEST`), `GlitchFog` (pure stotter-staat),
  `EchoFootsteps` (+ pure `EchoSteps`; `PlayLevelSoundEvent.AtEntity` van de `LocalPlayer` (zijn `playSound` vuurt
  dat, niet `AtPosition`), `*.step` in `PLAYERS`; stilstand = 12 ticks zonder beweging), `NetherRing5Fog` (0,8 × zicht ≤ 192, near 0,4 ×, op `is_nether_ring_5`),
  `SilenceEffects` (volume 1 → 0,35 tussen drempel 1 en 3, daarna 0, ≤ 0,05 per tick; hartslag in `MASTER` 20 → 10
  ticks; HUD-lagen weg vanaf drempel 2 met 2 punten hysterese), `Ring5Meters` (meter + drempels van de gesyncte
  config; `SILENCE` op naam), `VoidGlitchEffects` (void-meter ≥ drempel 1: ruis via `GlitchStormOverlay.feedVoid` tot
  0,6 × Screen Effect Scale, `glitch_pixel` rond de speler); `GlitchStormRenderer` → `glitch_pixel`,
  `ReverseRainRenderer` → `reverse_raindrop` (3/tick); `Ring5SoundDamping.feedSilence`; `GlitchStormOverlay.feedVoid`
  (ruis = max van storm en void, hotbar-hapering alleen van de storm).
- Client-mixins: `ClientLevelSkyColorMixin` (+ `Ring5Sky.skyColor` na het weer, nieuwe inject `getStarBrightness`),
  `DimensionSpecialEffectsMixin` (`Ring5Sky.sunriseColor` na `Ring4Dusk`), **nieuw `LevelRendererSkyMixin`**
  (`@ModifyArg` alpha van de derde `RenderSystem.setShaderColor` in `renderSky` = zon/maan, met `javap` gecontroleerd
  op 21.1.252); `corruptedadventure.mixins.json` → `client`.
- Tests: nieuw `client/weather/Ring5AtmosphereTest` (zwartheid, sterren/zon, fog-afstanden binnen en Nether-5,
  glitch-fog, echo-vlagen en extra echo, echo-volume, stilte-volume/hartslag/HUD, void-glitch, gedeelde demping);
  `BiomeAmbienceTest.ring5HookAddsSilenceGlitchesAndTheDroneOnTopOfTheColours`; `Ring5BiomesTest` en
  `NetherRing5BiomesTest` (stille muziek, `white_ash`).
- Gegenereerd: `sounds.json`, `particles/{void_mote,glitch_pixel,reverse_raindrop}.json`, `lang/{en_us,nl_nl}.json`,
  biome-JSON van de 6 ring 5- en de 2 Nether-5-biomes.

## Beslissingen
Gebruiker (2026-10-23): **alle elf = A** (INT2 zet ze in `fase-1-wereld.md` en het beslissingenlog):
- V98-1 A: lucht geleidelijk zwart van de bandlijn (0 %) tot ~85 % op 9.700 en 100 % boven de void; zon en maan gedimd.
- V98-2 A: sterren volgen de zwartheid (overdag in de buitenband, ook onder je in de void); binnenband vanilla.
- V98-3 A: binnenband-fog far 0,45 × zicht (48–112), near 0,15 × far, kleurloos (luma × daglicht), naar buiten uitdunnend.
- V98-4 A: glitch-fog = haperende fog-rand (1–3 ticks naar 40–70 %, om de 4–12 s, 1 op 4 violet-zwart), 2× in
  `glitch_fields`, × Screen Effect Scale.
- V98-5 A: binnenband bijna stil (zeldzame `glitch`-one-shots, vaker in `glitch_fields` + `glitch_pixel`),
  `false_meadow` stil; buitenband `void_drone`-loop + `void_mote`; mood `STRANGE` binnen; geen muziek in ring 5.
- V98-6 A (klank a): echo-voetstappen in vlagen van 4–10 stappen, 0,4–0,8 s later, 6–12 blokken achter je, met het
  stapgeluid van het blok; één echo na stilstand.
- V98-7 A: Nether-5-fog wit en ver (0,8 × zicht, max. 192), geen ambient, stille muziek, zeldzaam `white_ash`.
- V98-8 A: void-teleport: geluid `ambient.ring_5.glitch` + `glitch_pixel` op beide plekken, server-side (één aanroep in
  `VoidTeleport`, INT2-mergepunt met 97).
- V98-9 A: stilte-volume 100 % → 35 % tussen 30 en 90, daarna stil; alleen de hartslag blijft.
- V98-10 A: bij 60 blijven hotbar (+ itemnaam), crosshair, conditiebalken en chat.
- V98-11 A: void-meter ≥ 30: lichte glitchstorm-overlay, meegroeiend tot ~60 %, × Screen Effect Scale.

Implementatiekeuzes van de stroom:
- "Geen muziek" = een stille `Music` (`BiomeAmbience.SILENT_MUSIC`) op het event `ambient.nether_ring_5.silence`
  (contract: "vrijwel stil, voor het dempen"; een bijna lege loop van 20 s), in alle 6 ring 5- en beide Nether-5-biomes.
- Kleuren blijven in `Ring5Biomes`/`NetherRing5Biomes` (opdracht main: haakwijziging minimaal naast 94/95/96); de haak
  krijgt de kleurbouwer en voegt geluid, deeltjes en muziek toe.
- Zwartheid op de gesyncte layout: `Ring5Band.signedDistance` (de bandlijn met haar ruis) en `effectiveDistance` tot de
  ringrand (`outward` = afgelegd deel van de weg lijn → rand); niet de `breakup` van `Ring5Field` (die kent de client
  niet, hij hangt aan de wereldseed).
- Sterren: vanilla's middernacht is 0,5 (`getStarBrightness`); "vol aan de rand" = 0,5 × zwartheid. Zon/maan en de
  zonsopgang-gloed dimmen tot 40 %.
- Binnenband-fog: kleur = de luma van de al door vanilla belichte fog-kleur (dus "× daglicht" vanzelf); lucht-fog × 2
  zodat de grijs-violette lucht boven blijft; × `SkyExposure` (grotten vanilla). De glitch-fog geldt ook in grotten.
- Echo: het volume aan de bron wordt opgehoogd tegen vanilla's lineaire afval over 16 blokken, zodat de echo als
  0,5–0,7 van je eigen stap klinkt (max. 1); de plek wordt bij het afspelen bepaald (achter de huidige kijkrichting).
  Extra echo na stilstand: stilstand = 12 ticks zonder stap, de extra komt één eigen pas (5–12 ticks) na de laatste echo.
- Stilte: hartslag in `MASTER` (niet gedempt, niet positioneel), elke 20 → 10 ticks van drempel 3 naar 100; HUD harde
  wissel (vanilla-lagen kennen geen alpha) met 2 punten hysterese; verborgen: gezondheid, honger, pantser, lucht,
  rijdier, XP-balk/-level, springmeter, effecten, bossbalk, scorebord.
- Void-glitch: ruis × Screen Effect Scale; de deeltjes niet (vanilla's Particles-optie geldt al).
- Teleport-glitch in `PLAYERS` (de speler hapert), toonhoogte 0,9–1,1.

## Backlog
- `[blocks]` (B, via main): `VoidstoneBlock` van `reverse_portal` naar `void_mote` (het type staat er nu).
- `[conditions]`/INT2: mergepunt `VoidTeleport` (één aanroep `Ring5Glitches.teleported`, door A gezet); 97 werkt
  parallel aan de void-code.
- `[infra]` (INT2): `PROTOCOL_VERSION` bij `Condition.SILENCE` (97), niet door A. Na de merge van 97 werkt de
  stilte-client vanzelf (`Ring5Meters` zoekt `silence` op naam; `thresholdLevels(SILENCE)` moet dan bestaan).
- `[worldgen]` (W-biomes/W-nether): de kleuren van `Ring5Biomes.LOOKS` en `NetherRing5Biomes` kunnen later alsnog naar
  `Ring5Atmosphere`/`NetherRing5Atmosphere` (backlog r. 474, rest), als 94/95/96 klaar zijn.
- `[atmosphere]` (6b, in-game): alles van §6 "In-game checks"; in het bijzonder de mixin-doelen (`getStarBrightness`,
  `renderSky` ordinal 2) in een echte client, het volume van de nieuwe `.ogg`'s, de dichtheid van `void_mote`/
  `glitch_pixel`/`reverse_raindrop` en de hoorbaarheid van de echo op 6–12 blokken.
