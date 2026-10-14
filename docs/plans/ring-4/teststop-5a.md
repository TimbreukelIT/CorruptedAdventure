# Teststop 5a — terrein-wow (ring 4, verse wereld `ring4-wave1`)

Checklist voor de gebruiker. Basis: `ring4/waterscan-tool` (9ed1999), wereld seed 20260930, preset `corrupted_world`.
Zie `waves.md` (teststop 5a) voor wat er beoordeeld wordt; dit document is alleen de uitvoering.

## 1. Server starten

De speel-server staat stil (`server.sh stop` is gedaan). Het speel-lock `C:/Temp/ca-user-test.lock` staat aan: de
`tools/test`-scripts (`server.sh start`, `client.sh`) weigeren daarom te starten. Start de server dus zelf, rechtstreeks
met Gradle, vanuit de worktree (PowerShell):

```powershell
cd "P:\Projecten\Java\The Corrupted Adventure\.claude\worktrees\agent-a24ec1abc63e0a05e"
.\gradlew.bat runServer --no-daemon
```

- Wacht op `Done (…s)!` in de console (ca. 10–20 s na de compilatie).
- `run/server.properties`: `level-name=ring4-wave1`, `server-port=25577`, `online-mode=false`, RCON uit.
- Verbinden: Multiplayer → Direct verbinden → `127.0.0.1:25577`.
- Eerste keer in de server-console (niet in de client): `op <jouw naam>` (de `/ca`-commando's vereisen op-niveau 2).
- Stoppen: `stop` in de server-console.

## 2. Teleports (`/ca` in de chat)

`tpring` en `tpboundary` teleporteren langs de **richting van jouw positie vanaf (0,0)** op het moment van het commando.
Sta dus eerst ergens in de gewenste richting, bv. met `/tp @s 100 80 0` (oost), `/tp @s 0 80 100` (zuid), en zo voort.

| Commando | Waar je landt |
|---|---|
| `/ca tpring 4` | midden ring 4, overworld (r ≈ 7000) |
| `/ca tpring 4 nether` | midden Nether-ring 4 |
| `/ca tpboundary 3` | grens 3|4, overworld (r ≈ 6000) |
| `/ca tpboundary 3 nether` | grens 3|4 in de Nether |
| `/ca tpboundary 4` | grens 4|5, overworld (r ≈ 8000). Het getal is de buitenste ring: `4` = 4|5 |
| `/ca tpboundary 4 nether` | grens 4|5 in de Nether |

Na elk commando meldt de chat de bestemming en de ring die daar geldt.

## 3. Waterscan: wat de scan vond

Bron: `build/test/waterscan-ring4-ws.txt` in de worktree `agent-a17e01dd18da280b9` (zelfde basis 9ed1999, zelfde seed).
Formaat van de voorbeelden: `x y z (aantal blokken)`. "Over" = water tegen lucht buiten de vastgelegde vallen.

| Gebied | Over (blokken) | Voorbeeldcoördinaten (x y z) |
|---|---|---|
| r4e (interieur ring 4, oost, r 7000) | 160 | 6939 58 -6 (21), 6954 58 20 (20), 7020 -34 2 (14) |
| r4w (interieur ring 4, west, r 7000) | 351 | -6972 6 46 (62), -7029 -31 -1 (23), -7026 -32 4 (19) |
| r4n (noord) | 0 | — |
| r4s (zuid) | 0 | — (70 water, alles verklaard) |
| b45e (grens 4|5, oost, r 8000) | 25 164 | 7965 1 -39 (1109), 7945 1 17 (614), 7944 13 39 (583) |
| b45n (grens 4|5, noord) | 49 032 | -34 1 7983 (935), -64 14 7984 (805), -3 1 7990 (731) |
| b45w (grens 4|5, west) | 28 431 | -7925 1 14 (1004), -7954 1 5 (949), -7923 1 13 (813) |
| b45s (grens 4|5, zuid) | 55 636 | 3 1 -7995 (1431), -35 1 -8004 (1367), 33 1 -7998 (1317) |

Ter info (niet gevraagd): b34w (grens 3|4, west) heeft 38 over-blokken, o.a. bij -5998 101 53 (12).
Totaal over alle gebieden: 158 825 blokken. De 4|5-grens is dus het grootste open punt: het water daar valt grotendeels
buiten de geplande vallen. Kijk bij de teleport naar `/ca tpboundary 4`.

Let op: deze voorbeelden komen uit de scan-wereld. Als jouw wereld afwijkt, gaat het om de posities die je ter plaatse
ziet; de scan kan offline herhaald worden met `tools/test/py/waterscan.py`.

## 4. Nether-cel om te bekijken: spore_caves

Nether-ring 2 (r 375–875 in Nether-blokken, tussen de grenzen 2|3 op r 875 en 1500 in de Nether-scan), biome `spore_caves`.
Cel: chunk **37, −4**, Y **0–15**, dus blokken x 592–607, z −64 tot −49 in de Nether.
Route: `/ca tpring 2 nether` vanuit een positie in oost-noordoost-richting (x positief, z negatief), of `/tp` naar die coördinaten in de Nether-dimensie.

## 5. Terugmelden

Noteer per punt: wat je ziet, de coördinaat (F3), en of het klopt met §3. Het speel-lock blijft staan tot de
gebruiker de teststop afrondt.
