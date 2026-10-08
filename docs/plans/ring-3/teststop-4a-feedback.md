# Teststop 4a — feedback van de gebruiker en antwoorden

Bij `teststop-4a.md`. Terreinfeedback uit de eerste in-game sessie (2026-10-07) en de beslissingen die daarop volgden. Uitgevoerd in change 39d (`change-39d.md`); de rest van 4a (B–E) loopt intussen door op main.

## Feedback (2026-10-07)
1. "De hele ring voelt plat" — overal, niet op één plek.
   - Oorzaak (verkenning main): buiten de vulkanen komt de hoogte uit één gladde 2D-vlakte (`VolcanicField.plain()`, Y 89 ± 3–5).
   - Op de vlakte geen 3D-detail (ruwheid 0), dus elke blokhoogte is een rechte traplijn.
   - Vulkanen dekken maar ~3–4 % van as/smeulwoud/zwavel.
2. "Ik mis echt lavarivieren en kan amper een vulkaan of gebergte vinden in ring 3." Biomes zelf zijn ok.
3. "Meer fantasie-element in het landschap; vliegende eilanden zijn gaaf, maar ik mis nog het wow-effect."
4. "Alle biomes zijn plat; ik zie blokken veranderen, maar de vormgeving per biome moet grondig, zodat het niet allemaal hetzelfde saaie landschap is."
5. "`volcanic_peaks` en `basalt_flows`: daar ben ik al enigszins positief over, dat ziet er al decent uit (kan nog wel beter)." Dus behouden en verbeteren, niet vervangen. Vóór/na-screenshots van die twee.

## Beslissingen (2026-10-07)
1. **Reliëf "Ruig":** Y ~70–150 tussen de vulkanen; brede hooglanden en laagtes, heuvels 20–40, kliffen en plateaus 15–40. Vulkanen blijven de hoogste punten.
2. **Vormen: alles**:
   - basaltplateaus met kliffen;
   - slakkenkegelvelden (kegels 10–40 met minikrater, niet bij vulkanen, zeekom of rivieren);
   - riftdalen (20–40 diep, getrapte randen, stoppen bij rivieren en de zeekom);
   - 3D-ruwheid op de vlakte.
3. **Aanpak parallel:** één bouwagent bouwt 39d in een eigen worktree; de gebruiker test intussen de rest van 4a op main.
4. **Vulkanen overal zichtbaar:**
   - vanaf elk punt in ring 3 minstens 1–2 vulkanen aan de horizon;
   - kans in `ashen_wastes`, `ember_woods` en `sulfur_fields` ~3× omhoog (≈ 1 per 500–600 blokken);
   - meer middelgrote kegels;
   - horizonband en pieken blijven het dichtst; zeekom en titanenveld blijven (vrijwel) zonder;
   - de eilandendichtheid per oppervlak blijft ongeveer gelijk.
5. **Lavarivieren vaker en zichtbaarder:**
   - ~2× zoveel, breder (6–16) en ondieper in de geul, zodat je ze van ver ziet gloeien;
   - ze ontspringen bij vulkanen en stromen naar laagtes en riftdalen;
   - korst alleen nog op losse randblokken, geen korsttreden over de rivier;
   - oevers smaller en minder magma bij brede rivieren (review-punt 39c).
6. **Fantasy-vormen, alle drie de groepen:**
   - a. zwevende archipels: grotere eilanden (60–100 breed) in clusters boven grote caldera's, met lavavallen en basaltpilaren naar de grond, plus een zeldzame **zwevende vulkaan** die lava spuwt;
   - b. bogen en reuzenzuilen: basaltbogen over lavarivieren en riftdalen, kliffen van zeshoekige zuilen, obsidiaannaalden van 60–120 hoog;
   - c. afgrond naar de diepte: enkele zeer diepe gloeiende kloven (tot Y ~0) met een lavazee onderin en smalle natuurlijke bruggen.
7. **Frequentie = mix:** kleine elementen (bogen, naalden, zuilen) elke paar honderd blokken; grote landmarks (archipel, zwevende vulkaan, afgrond) enkele per ring, van ver zichtbaar. Coördinaten op seed 20260930 in het rapport.
8. **`ashen_wastes` = duinzee met geulen:** asduinen 10–25 hoog in één windrichting, daartussen uitgesleten geulen met aspilaren (hoodoos 5–15).
9. **`ember_woods` = glooiend laagland:** zachte heuvels; het bos (golf 2) doet de rest.
10. **`sulfur_fields` = zwavelterrassen:** laag bekken met getrapte terrassen (kommen met randjes), fumarole-heuveltjes en zinkgaten.
11. **`basalt_flows` = getrapte lavaplateaus** met zuilenkliffen en ingestorte lavabuizen (sleuven en tunnels).
12. **`titan_fields` = fantasy:** stenen ribben en wervels als bogen uit de grond (20–60 hoog), zeldzaam een hele ribbenkast.
13. **`volcanic_peaks` = echte bergketen:** kegels verbonden door kammen en graten; het hoogste deel van de ring (Y 150–250).
14. **`dried_seabed`:** kustterrassen, zeepilaren (15–40) en oude rifruggen.
15. **Biomegrenzen** eindigen niet in harde kliffen (vormen lopen uit over de regiorand).
16. **Middelgrote kegels:** rand 80–140 boven de vlakte (Y ~180–250); horizonreuzen omhoog naar Y 250–285, zodat ze de hoogste blijven (antwoord op vraag 1 van de bouwagent).
17. **Getraceerde rivieren** (antwoord op vraag 2 van de bouwagent):
    - per actieve vulkaan 1–2 rivieren vanaf de voet bergaf, om vulkanen heen en door riftdalen;
    - het niveau stijgt stroomafwaarts nooit; elke daling van 1 blok is een klein lavavalletje;
    - ze eindigen in een lavapoel in een laagte;
    - deterministisch per seed, per wereld gecachet en thread-safe, onafhankelijk van de laadvolgorde;
    - de tracing-tijd wordt gemeten.

## Hertest na 39d (2026-10-08)
Stand: main `66628d3` (39e + 39d). Oordeel gebruiker: "stukken beter, in principe al aardig goed".

**Teststop 4a: akkoord met fixes.** Golf 2 start nu. De resterende terreinpunten gaan naar change **39f** (`worldgen`, parallel aan golf 2; W-terrein 42 pas ná 39f):
1. Archipel (bv. 2622, −2738): basaltpilaren zijn kaarsrechte vierkante kolommen; lavavallen zijn korte statische platen die niet tot de grond of het kratermeer vallen; de eilanden hangen in de wolkenlaag (Y ~192); de kleine hangende kegel onder het hoofdeiland.
2. Rechthoekige, asgerichte put met rechte wanden en hoeken van 90°, lichtgrijze verticale banden op de wanden en een strakke laag (grove) aarde langs de rand.
3. Vlakte: traplijnen van 1 blok nog duidelijk zichtbaar.
4. Losse vlekken grove aarde/zand en de gestreepte naald ogen "half".
5. Open punten 39d: kruinen van ribben/bogen krijgen de biome-surface; maar 3 bogen over rifts; surface-stap ring 3 1,5–2,8×.
6. Open punten 39e (laag): gedeelde gate-interface, verouderde Javadoc, Fill-cache op (dimensie, ring), `RingBlend.fillArray` per unieke kolom, `iterateNoiseColumn` in `duringFill`, officiële 39e-meting + client-smoke.

Screenshots: `run/screenshots/2026-10-08_13.20.26.png` (archipel) en `2026-10-08_13.21.50.png` (put, vlakte, naald).
