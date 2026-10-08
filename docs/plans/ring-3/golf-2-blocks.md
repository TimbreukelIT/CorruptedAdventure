# Golf 2 — stroom B (blokken): 46-rest (geluiden en particles aan fumarole, geiser, modder; afstemming)

Scope: `blocks` — werkpakket 46 (rest), branch `ring3/blocks-g2`, poort 0 (server 25570, RCON 25580). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

**Doel:**
- de blokken van 46 krijgen hun eigen geluiden en de `steam_puff`-particle van A, in plaats van de vanilla-tijdelijke;
- het gedrag wordt afgestemd na 4a (geiser-ritme, eventueel een waarschuwing).

Klein pakket. Daarna is B beschikbaar voor nieuwe blokken die de golf 2-features via main vragen.

**Bron:**
- `golf-1-blocks.md` (46, overdracht, valkuilen);
- `contract.md` §3 (46) en §6 (sound ids `block.fumarole.puff`, `block.geyser.burst`, `block.boiling_mud.bubble`; particle `steam_puff`);
- backlog r. 177, 179.

## 1. Werk (één commit)
1. **Geluiden** (events van A, contract §6):
   - `FumaroleBlock.animateTick`: af en toe `block.fumarole.puff` (lokaal, client-level, zoals de particles); niet als de opening bedekt is (`GeyserBlock.ventOpen`).
   - `GeyserBlock`: de uitbarsting speelt `block.geyser.burst` server-side (`level.playSound` met `null`-speler, dus voor iedereen hoorbaar) in plaats van `GENERIC_EXTINGUISH_FIRE`.
   - `BoilingMudBlock.animateTick`: `block.boiling_mud.bubble` bij een deel van de bubbels.
   - Volume en kans zo dat een veld van tientallen fumarolen of modderblokken niet gaat dreunen. Een cap per tick per client is een implementatiekeuze, maar meet het in `steam_fens` (48) en de zwavelvelden (47).
2. **Particles:** `steam_puff` (A) vervangt `CAMPFIRE_COSY_SMOKE`/`CLOUD` in fumarole en geiser. Tint of eigen kleur: afspraak A ↔ T (backlog r. 182).
3. **Afstemming geiser** (backlog r. 177): ritme en duw volgens G2-B1. Waarschuwing: server-side, met een extra geplande tick (bijv. 20 ticks vóór de uitbarsting: borrelen en een kleine stoompuf via `sendParticles`), zodat alle clients hetzelfde zien.
4. **Kokende modder:** gedrag volgens G2-B2.
5. **Nieuwe blokken uit golf 2:** geen gepland. Vraagt W-biomes of W-nether er een via main (bijv. een eigen schedelblok of stoomvent), dan komt het als apart pakket (`46b`/`51b`) met een eigen commit.

## 2. Bestanden
- **Eigen:** `block/FumaroleBlock`, `block/GeyserBlock`, `block/BoilingMudBlock` (+ tests), en zo nodig `ModBlocks`/blok-datagen/`Ring3BlockNames`.
- **Leest van A (niet wijzigen):** `ModSoundEvents`, `ModParticleTypes`.

## 3. Afhankelijkheden
- **← A** (hard): de sound events en `steam_puff` moeten bestaan.
  - Voorstel: A registreert ze in voorpakket 50-0 op main, met vanilla-verwijzingen in `sounds.json` en de bestaande sprites van T (zie `golf-2-atmosphere.md`). Dan start B vanaf die commit.
  - Zonder voorpakket wacht B op de merge van A's 50. B is klein en kan ook als laatste.
- **→ W-biomes:** 47 (fumarolen) en 48 (`steam_fens`: geisers, modder) gebruiken deze blokken; het geluid zit in het blok, niet in de feature. Geen wachtrelatie.
- **→ C:** geen (de hittetags veranderen niet).

## 4. Open vragen
Aanbevolen optie eerst. **C** = complexiteit, **MP** = multiplayer, **S** = speelgevoel, **P** = prestatie.

**G2-B1 — Geiser: welk ritme en welke duw?** (nu elke 100 ticks vast, ~4,9 blokken)
- **A. Vast ritme per geiser maar verschillend per positie (80–200 ticks, deterministisch uit de positie), 1 s waarschuwing (borrelen + kleine puf), duw ~5 blokken (aanbevolen).** C laag (extra geplande tick). MP: alles server-side, iedereen ziet hetzelfde. S: een veld geisers "leeft" (niet in de maat) en je kunt timen.
- **B. Hoge geisers in `steam_fens` (duw ~10–12 blokken) als lanceerplatform om plateaus en randen te bereiken, met waarschuwing.** C laag. S: wow en mobiliteit; risico op valschade naast de geiser.
- **C. Zoals nu (100 ticks, ~5 blokken, geen waarschuwing).** C geen. S: voorspelbaar, wat mechanisch.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-B2 — Kokende modder: alleen traag, of ook gevaarlijk?** (nu: honing-traagheid + hittebron)
- **A. Zoals nu (traag + hittebron) (aanbevolen).** C geen. S: hinderlijk, niet dodelijk; past bij de schaarste zonder straf.
- **B. Ook kleine brandschade (1 per 2 s, zoals magma; sluipend lopen beschermt).** C laag. S: modderpoelen worden echte obstakels in `steam_fens`.
- **C. Gevallen items zinken weg (worden vernietigd).** C laag/middel. S: frustrerend; weinig winst.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
- **Geluid en particles:** fumarole en modder via `animateTick` (client-level, geen client-class); geiser-geluid en -particles server-side (`playSound`, `sendParticles`), dus voor iedereen hetzelfde.
- Geen nieuwe payloads.

## 6. Tests
- **B:** `./gradlew build` groen; `GeyserBlockTest` + interval/waarschuwing (puur), `ModBlocksTest` ongewijzigd.
- **D:** `runData` groen.
- **S:** server (25570) + client.
  - Geiser: uitbarsting met geluid; waarschuwing vóór de duw; bedekt = stil.
  - Fumarole puft met `steam_puff` + geluid.
  - Modder bubbelt hoorbaar.
  - Twee clients horen/zien de geiser op hetzelfde moment.
- **Review:** "klaar voor review-doorsturen: 46-rest" aan main; main draait `/code-review medium`; daarna committen.

## 7. W-checklist (in-game; na 48 ook in `steam_fens`, `/ca tpboundary 3`)
- [ ] Fumarole: stoompuf (`steam_puff`) met een zacht sissen; stil als er een blok op ligt.
- [ ] Geiser: waarschuwing (borrelen), dan de uitbarsting met geluid; een veld geisers loopt niet in de maat; landen geeft geen valschade.
- [ ] Kokende modder: bubbelt hoorbaar; traagheid en hitte zoals in 46 (of G2-B2).
- [ ] Een veld van tientallen fumarolen of modderblokken klinkt levendig, niet dreunend (Particles: Minimal → minder puffen).

## Voortgang

## CODEBASE-delta

## Beslissingen

## Backlog
