# Fase 4 — Nightmares (concept)

**Status:** concept uit een vooruitblik-sparring (2026-10-20). Er wordt niets gebouwd vóór fase 4; de fase 4-sparring maakt dit definitief. Scope: `mobs`.

## Idee
Een groep **Nightmares**: één per ring (5 totaal), groter dan gewone mobs, bedoeld om kippenvel te geven. Rondzwervende overworld-ringbosses, ingezet vanuit Herobrines legioen.

- **Rolverdeling:** bewakers zijn *poortwachters op een vaste plek* (jij gaat naar hen toe); Nightmares zijn *dreiging die jou vindt*. Herobrine blijft de enige die "persoonlijk" is.
- **Rode draad:** Herobrines witte ogen als handtekening van het legioen. Elke Nightmare geeft betekenis aan iets wat fase 1 al in de wereld heeft gelegd.

## Beslissingen (sparring 2026-10-20)
1. **Los en optioneel** t.o.v. de bewakers: verslaan levert iets waardevols op, maar is geen progressie-eis.
2. **Eén unieke Nightmare per ring per server.** De server houdt een virtuele positie bij (`SavedData`) en laat hem bij spelers in zijn ring verschijnen; ook als zijn chunks niet geladen zijn, "bestaat" hij.
3. **Komt sterker terug** na verslaan (timer en/of gekoppeld aan wereldcorruptie, fase 5).
4. **Ring 1: volwaardig bossgevecht.** De Holle valt ook bovengronds actief aan; de stalk-opbouw blijft als aanloop.

## Rooster (werktitels, ter discussie)
| Ring | Nightmare | Haak in de bestaande wereld |
|---|---|---|
| 1 | **De Holle** (The Hollow) — ontzielde titaan, dimension shifter | corruptie van ring 5 in de vanilla-ring; zielkristallen van Nether-1 |
| 2 | **De Gelaagde** (The Layered; was "De Luchtspiegeling") — veelkoppige kloofkruiper, eter van de reuzen | kloven, half begraven reuzenskeletten in de duinenzee |
| 3 | **Het Askoor** — reus van samengesmolten as-afgietsels die zingen, beweegt door lava | as-afgietsels, voetsporen in de as, lavarivieren |
| 4 | **Wat onder de bellen leeft** | backlog: "iets groots onder water (bij de bellen)", het silhouet is de voorbode |
| 5 | **De Onvoltooide** — half geladen, glitchende wachter | void, glitches, kastelen (niet overlappen met "beweegt als je niet kijkt") |

## Ring 1 — De Holle, de ontzielde titaan (werktitel)
*Herontwerp 2026-10-20: vervangt de eerste schets (uitgerekte nabootser). "Hol" betekent nu: zonder ziel.*

![De Holle](img/nightmare-ring1-de-holle.png)

**Wie hij was:** een oude wachter van de overworld, een stenen titaan. Herobrine rukte zijn ziel uit en maakte hem tot dimension shifter van zijn legioen. Daarom loopt er iets corrupts in de vanilla-ring: hij hoort er niet, hij komt via een scheur uit Herobrines domein.

**Gedaante (~6 blokken, altijd krom; rechtop ~8):**
- Lijf van gebarsten steen- en deepslate-platen met mos (knieën, schouders, bult). Over de rug loopt een gebroken stenen kam.
- **Asymmetrisch:** zijn rechterarm is gezwollen en reusachtig, met knokkels die over de grond slepen. Die arm is het meest corrupt, omdat hij als eerste door de scheur komt. De linkerarm is verschrompeld en heeft een gewricht te veel.
- **Gebroken nek:** het hoofd (stenen schedel met gebroken kroon) hangt op zijn linkerschouder. De oogkassen zijn leeg, op twee witte pixels na: dat zijn Herobrines ogen, niet de zijne. Uit één oogkas loopt een corruptietraan.
- **Borst:** een opengebroken ribbenkooi met daarin pure void. Licht valt erin en niet eruit. Op de rand zitten zielblauwe resten in het palet van de zielkristallen.
- **Corruptie:** de corruptie van ring 5 (grijsviolet, void-zwart, glitchpixels) glijdt als een vloeistof over zijn lijf. Ze stroomt uit de borst omlaag en ligt het dikst op de reuzenarm. Af en toe verspringt een ledemaat een paar pixels, alsof hij niet helemaal in deze dimensie zit.

**Zijn ziel:** die is versplinterd tot de **zielkristallen van Nether-ring 1** (beslissing 2026-10-20). Later (fase 2) wordt dat ook een zwakte: een zielkristal-item laat hem verstijven of houdt hem "in fase".

**De komst (dimensiescheur):**
1. *Voorbode (~30 s):* er verschijnt een haarscheur in de lucht. Dieren vluchten, fakkels flikkeren en geluiden spelen achteruit.
2. *De scheur:* een rafelige verticale wond (~3 × 7 blokken) met een void vol sterren (render zoals het End-portaal) en corrupte, gloeiende randen.
3. *Eerst de hand:* de reuzenhand grijpt de rand en trekt hem erdoor. Diep in de scheur kijken twee witte ogen.
4. *Litteken:* waar de scheur stond blijft een **klein permanent litteken** van ring 5-corruptieblokken achter (beslissing 2026-10-20). Het genees-/vervalgedrag volgt in fase 5.

**Gevecht (volwaardig, ook bovengronds):**
- *Verspringen:* hij stapt een scheur in en komt achter je weer naar buiten. Een krakend geluid waarschuwt je vooraf.
- *Uit fase:* delen van zijn lijf zijn soms doorzichtig en dan onkwetsbaar. Je moet slaan als hij volledig "hier" is.
- *Zuigkracht:* een open scheur trekt spelers en items naar zich toe.
- *Wisselen:* hij verwisselt twee spelers van plek (multiplayer).
- *Aftocht:* bij een schadedrempel (of bij zonsopgang, nog open) valt hij achterover zijn scheur in. Daarna komt hij sterker terug.

**Bouw (1 blok = 16 px):**
- **Hitbox:** ~2,5 × 5,5 blok. Het model heeft een eigen culling-box nodig, omdat het groter is dan de hitbox.
- **Romp:** hoofd 12×12×12 + kroon · romp 26×24 (borstholte) + buik 16×12 · bult 32×12 + stenen kam.
- **Reuzenarm:** schouderplaat 14×12, bovenarm 11×26, onderarm 15×38, hand 20×14 + 4 knokkels.
- **Dunne arm:** 3 delen + 3 vingers.
- **Lagen:** steen/mos (basis) · geanimeerde corruptie-overlay die omlaag schuift · emissive laag (ogen, zielresten, corruptieranden).
- **Paletten:** steen `2E2D33 3E3D44 4F4E55 63626A 7A7980` · mos `3B4A26 4F6330 66803C` · corruptie `140E1A 2B1F38 45305C 6A4A8C 9C7AC4` · ziel `17758C 2DB8C8 5FE3E6 A8F7F4` (= zielkristal) · void `07050A` · glitch `F800F8`.
- **Beweging en techniek:** hij beweegt via scheuren, wat het pathfinding-probleem van een reus oplost. De scheur is een eigen entity: de server bepaalt positie en levensduur, de client tekent hem.

## Ring 2 — De Gelaagde (The Layered), de kloofkruiper (werktitel)
*Sparring 2026-10-27, op basis van twee referentiebeelden van de gebruiker (een spinachtig kloofbeest en een driekoppige hellehond). Het zijn inspiratiebronnen, geen kopie.*

![De Gelaagde](img/nightmare-ring2-de-gelaagde.png)

- **Lijf:** de basis is een kloofkruiper: veel te lange poten en een zwaar lijf. Hij heeft **2–3 schedelkoppen**; alleen de middelste heeft Herobrines witte ogen. Ketenen om de voorpoten zijn een optie.
- **Materiaal (camouflage):** zijn rug bestaat uit platen in de drie kloofsteen-tinten (rood, oranje, crème, in banden zoals de wand), zodat hij tegen de wand bijna onzichtbaar is. De poten zijn van reuzenbot, de buik is ruige, stoffige vacht.
- **Lijfvorm (uitgemergeld en getrapt):** een smal borststuk met de koppen en de zes poten, een dunne taille met zichtbare ruggengraat en een lang, plat achterlijf (~2,5 breed × ~6 lang). De platen op het achterlijf liggen getrapt, zoals de treden van de kloof: zijn rug is een kleine kloofwand. Omdat hij plat is, ligt hij strak tegen de wand, zonder schaduw die hem verraadt.
- **Prooi in het lijf:** uit de vachtbuik en tussen de rugplaten steken gebroken reuzenbotten (ribben, een wervel) van de reuzen die hij at.
- **Dode koppen:** ze zijn dood, maar bewegen toch. Ze draaien los van elkaar naar de speler en happen af en toe. Waar ze vandaan komen is nog open.
- **Komst (de wand komt los):** als voorbode vallen er steentjes en stof van de wand, ratelen er ergens in de kloof botten en vluchten de dieren. Daarna ontvouwt zich een stuk kloofwand: dat is hij.
- **Aanvallen:**
  - *Grijpen en meeklimmen:* hij grijpt een speler en klimt met hem de wand op; laat hij los, dan volgt valschade. Andere spelers bevrijden de speler door de kop te raken.
  - *Steenslag en los zand:* vanaf de wand maakt hij stenen en `loose_sand` boven de speler los. Dit zijn echte blokken en de schade is **blijvend**. Bewust geaccepteerd: het kan bases en terrein beschadigen.
- **Formaat:** ~4 blokken hoog, poten ~10 breed. De hitbox is kleiner (~2,5 × 2,5) en het model krijgt een eigen culling-box.
- **Zwakte (twee lagen):**
  - *Pantserrug, zachte buik:* de stenen rug dempt schade, de vachtbuik krijgt de volle klap. Kruipt hij boven je over een wand of overhang, dan ligt de buik open. Vuur in zijn vacht laat hem van de wand vallen.
  - *Zonnesteen-flits:* fel zonnesteenlicht verblindt de levende kop, waardoor hij zijn grip verliest en valt. Hier is een item of effect voor nodig in `items` (backlog).

**Open (ring 2):** waar de dode koppen vandaan komen; gedrag buiten het gevecht en aftocht; terugkeer; beloning; balans met ring 2-gear (zonnesteen); hoe de blijvende steenslag begrensd wordt (aantal blokken, niet bij spelerbouwsels?).
- **Verhaal:** hij is de **eter van de reuzen**. De half begraven reuzenskeletten in de duinenzee (blikvanger `dune_sea`) zijn zijn prooi.
- **Beweging:** over de kloofbodem én langs wanden en treden, en over de rand het plateau op. Bij de kloof ben je nergens veilig. Het model draait mee met de wand: de client rendert het, de server bepaalt de staat.
- **Zwakte:** vrij te kiezen (geen vast patroon per Nether-ring); nog open.

## Ring 3 — Het Askoor (werktitel)
*Sparring 2026-10-27.*

![Het Askoor](img/nightmare-ring3-het-askoor.png)

- **Wie hij is:** een reus die bestaat uit honderden samengesmolten as-afgietsels: zijn slachtoffers. Gezichten, handen, ruggen en knieën steken uit zijn lijf, met gloeiende naden ertussen. Ze zingen laag: dat is het koor. Herobrines witte ogen zitten in één kop in het midden.
- **Haak in de wereld:** elk as-afgietsel in ring 3 (die allemaal naar de dichtstbijzijnde vulkaan kijken) is iemand die hij verbrandde. De voetsporen in de as die abrupt stoppen of bij een kraterrand eindigen, zijn mensen die hij meenam.
- **Beweging:** door lava. Hij zwemt door lavarivieren en kratermeren en komt eruit omhoog; kort kan hij over land. Lava is zijn "scheur", en dat lost ook de pathfinding van een reus op.
- **Overal in ring 3 (hij smelt zich omhoog):** lava ligt maar op ~1,6 % van het oppervlak, dus hij **maakt het zelf**. Hij reist onder de grond (virtuele positie; de lavabuizen geven het verhaal) en kan overal bovenkomen. De grond gloeit en scheurt, en smelt dan tot een tijdelijke lavapoel (~5–7 breed) waar hij uit oprijst. Echte lavarivieren en kratermeren blijven zijn favoriet: daar is hij sterker en daar trekt hij zich terug.
- **Litteken:** na afloop stolt de poel tot een blijvende plek van basalt, magma en obsidiaan, net als het litteken van De Holle. Hij raakt alleen natuurlijke blokken, nooit spelersblokken. Zo blijft de fase 1-regel "geen lava tegen open lucht" heel.
- **Formaat:** de grootste Nightmare tot nu toe, ~8–10 blokken, zodat je hem van ver boven de asvlakte ziet.
- **Gedaante (vormeloze massa):** een berg samengesmolten figuren zonder vaste vorm, die armen en koppen vormt waar hij ze nodig heeft. Dit is het engst, maar ook het zwaarste model- en animatiewerk van alle Nightmares (veel losse delen).
- **Komst (het koor zwelt aan):** eerst hoor je een laag koor, steeds luider en uit de richting van de lava (of onder je voeten). Uit de afgietsels in de buurt stijgen rook en vonken op (particles). Dan borrelt de lava, of gloeit en smelt de grond, en rijst hij op. Behalve de gesmolten poel en het litteken verandert het terrein niet.
- **Aanvallen:**
  - *Tot afgietsel maken:* hij bedekt je met as, waardoor je steeds trager wordt tot je vaststaat. Medespelers kunnen de as eraf slaan. Sterf je zo, dan blijft er op die plek een blijvend afgietsel in jouw houding achter.
  - *Koorschreeuw:* alle koppen schreeuwen tegelijk: misselijkheid of zwakte in een straal, en dieren en mobs vluchten.
  - *Slachtoffers loslaten:* hij scheurt figuren uit zijn lijf die als aswandelaars gaan lopen (de gewone ring 3-mobs uit de backlog; afstemmen in fase 4).
- **Zwakte (ring 3-hoorn):** een eigen, versterkte ring 3-hoorn overstemt het koor en laat hem kort verstijven; de vanilla-geitenhoorn doet niets. De basis is een geitenhoorn, en geiten leven in ring 3 alleen op de pieken. Het recept volgt in `items` (backlog).
- **Techniek van de vormeloze massa:** één massa-model met ~20–30 figuren als geanimeerde delen. "Armen vormen" is een vaste set van 4–6 vormen (arm links, arm rechts, kop omhoog, ineengezakt, uit de lava) die in elkaar overvloeien. Alleen losgescheurde figuren worden echte entities (aswandelaars). GeckoLib ligt hier voor de hand (animatietechniek nog open).
- **Afgietsel bij je dood:** blijvend, in jouw houding (as- en smeulende-asblokken), met je naam zichtbaar als iemand ernaar kijkt (klein block entity). Alleen op vrije ruimte, nooit over spelersblokken.

**Open (ring 3):** hoe ver en hoe lang hij over land kan; gedrag buiten het gevecht; aftocht en terugkeer; beloning; balans met ring 3-gear (vulkaniet); de precieze werking van het verstijven.

## Haalbaarheid in 1.21.1 / NeoForge
- **Goed te doen:** grote modellen (4–6 blokken), bossbar (`ServerBossEvent`, multiplayer-proof), eigen AI-goals, positionele geluiden server-side (vanilla-events met pitch, zoals change 32), particles en client-effecten via payload.
- **Lastig maar haalbaar:** pathfinding van grote mobs (vanilla-navigatie loopt vast in bossen en grotten → hitbox kleiner dan het model, of eigen beweging: kruipen, graven, zwemmen); de unieke, persistente Nightmare (virtuele positie in `SavedData`, pas materialiseren bij spelers); houdingwissel met hitbox (kruiphouding, zoals een zwemmende speler) plus eigen navigatie door 1×2.
- **Af te raden:** per-speler hallucinaties met één gedeelde entity (vereist client-only nep-entities); zware render-glitches/shaders (ring 5) → houd het bij model-swaps, stotter-teleports en geluid.
- **Client/server:** alle logica (positie, AI, aanvallen, fakkels doven, terugkeertimer) server-side; de client toont alleen model, animatie en effecten.

## Open voor de fase 4-sparring
- Balans van het ring 1-gevecht met ring 1-gear (ijzer/diamant): HP, schade, schaal met het aantal spelers.
- Terugkeer: interval, hoeveel sterker, wat er verandert (nieuwe aanval of fase).
- Beloning/drop per Nightmare (waardevol, niet verplicht).
- Zonsopgang: trekt hij zich terug of vecht hij door?
- Wat doet hij buiten het gevecht (stalken via haarscheuren? alleen komen bij nacht?).
- Blijft er een link met de 2×1-tunnels en kale plekken van ring 1, of horen die bij iets anders?
- Animatietechniek: GeckoLib (dependency, makkelijke complexe animaties) of vanilla keyframe-animaties (geen dependency, meer werk).
- Concepten ring 2–5 uitwerken; Nightmares in de Nether-ringen (ja/nee).
