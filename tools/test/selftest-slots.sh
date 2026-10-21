#!/bin/bash
# Self-test of slots.sh in a scratch lock directory (never C:/Temp): fake owner files, fake holder PIDs, no JVM.
#   bash tools/test/selftest-slots.sh [--slow]     --slow adds the 20 s retry of `take --wait`
# Exit 0 when every check passes.
T="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRATCH="$(mktemp -d)"
trap 'rm -rf "$SCRATCH"' EXIT
export CA_LOCK_DIR="$SCRATCH/lock" CA_STREAM=selftest
export CA_ALIVE_PIDS="1111 2222"            # the "living" holders; every other PID counts as dead
ME="$(cat /proc/$$/winpid 2>/dev/null || echo $$)"
fails=0
slots() { bash "$T/slots.sh" "$@"; }
ok() { echo "ok   $1"; }
bad() { echo "FAIL $1"; fails=$((fails + 1)); }
check() { local name="$1"; shift; if "$@" >/dev/null 2>&1; then ok "$name"; else bad "$name"; fi; }
nope() { ! "$@"; }
slot() { echo "$CA_LOCK_DIR/ca-jvm-slot-$1"; }
owner() { cat "$(slot "$1")/owner" 2>/dev/null; }
count() { local n=0 s; for s in 1 2 3; do [ -d "$(slot "$s")" ] && n=$((n + 1)); done; echo $n; }
# foreign <slot> <stream> <pid> [<minutes old>]: an entry of another stream
foreign() {
    mkdir -p "$(slot "$1")"
    echo "stream=$2 pid=$3 time=x task=server token=server root=P:\\other" > "$(slot "$1")/owner"
    [ -n "$4" ] && touch -d "-$4 minutes" "$(slot "$1")/owner" "$(slot "$1")"
    return 0
}
reset() { rm -rf "$CA_LOCK_DIR"; mkdir -p "$CA_LOCK_DIR"; }

# 1. A server and its client in one take: two slots, each with its own task, held by --pid.
reset
slots take --tasks server,client1 --pid 1111 >/dev/null
check "pair: two slots" [ "$(count)" = 2 ]
check "pair: slot 1 task server" grep -q "task=server token=server root=" <<< "$(owner 1)"
check "pair: slot 2 task client1" grep -q "task=client1 token=client1 root=" <<< "$(owner 2)"
check "pair: holder pid" grep -q "pid=1111 " <<< "$(owner 2)"
check "pair: owner format" grep -qE "^stream=selftest pid=1111 time=[^ ]+ task=client1 token=client1 root=" <<< "$(owner 2)"

# 2. Two slots of other streams busy: the pair is refused and nothing stays held (no partial hold).
reset; foreign 1 a 1111; foreign 2 b 2222
slots take --tasks server,client1 --pid 1111 >/dev/null 2>&1; rc=$?
check "busy pair: exit 2" [ "$rc" = 2 ]
check "busy pair: holds nothing" [ "$(count)" = 2 ]
check "busy pair: slot 3 free" [ ! -d "$(slot 3)" ]
slots take -n 1 --task server --pid 1111 >/dev/null
check "single take still fits" grep -q "stream=selftest" <<< "$(owner 3)"

# 3. The 2026-10-19 deadlock: three streams with a server each could never start a client. With pairs the first
#    stream gets both, the second gets nothing and waits empty-handed.
reset
CA_STREAM=s1 slots take --tasks server,client1 --pid 1111 >/dev/null
CA_STREAM=s2 slots take --tasks server,client1 --pid 2222 >/dev/null 2>&1; rc=$?
check "deadlock: s2 refused" [ "$rc" = 2 ]
check "deadlock: s2 holds nothing" nope grep -q "stream=s2" <<< "$(owner 1)$(owner 2)$(owner 3)"

# 4. held, lock-only take, adopt, release by holder.
reset
slots take --tasks server,client1 --pid 1111 >/dev/null
check "held client1" slots held --task client1
check "not held client2" nope slots held --task client2
slots take -n 0 --lock --task client1 --pid 1111 >/dev/null
check "lock only: lock held" [ -d "$CA_LOCK_DIR/ca-test.lock" ]
check "lock only: no extra slot" [ "$(count)" = 2 ]
slots release --task client1 --lock-only >/dev/null
check "lock released" [ ! -d "$CA_LOCK_DIR/ca-test.lock" ]
check "client slot kept" slots held --task client1
slots adopt 4242 --task client1 >/dev/null
check "adopt: client slot pid" grep -q "pid=4242 .*task=client1" <<< "$(owner 2)"
check "adopt: server slot untouched" grep -q "pid=1111 .*task=server" <<< "$(owner 1)"
slots release --task client1 --holder 9999 >/dev/null
check "release --holder other pid: kept" slots held --task client1
slots release --task client1 --holder 4242 >/dev/null
check "release --holder its pid: freed" nope slots held --task client1
check "server slot still held" slots held --task server
slots release --task server >/dev/null
check "all free" [ "$(count)" = 0 ]

# 4b. Two entries with one label (a restarted server reserved client1 while the old client 1 still runs): adopt and
#     release with --holder touch only the entry of that holder (client.sh stop hands back only its own).
reset
slots take --tasks server,client1 --pid 1111 >/dev/null      # new server 1111 + its reservation
slots take --task client1 --pid 4242 >/dev/null              # the old client's slot
slots adopt 1111 --task client1 --holder 4242 >/dev/null
check "adopt --holder: both client1 now 1111" [ "$(slots held --task client1 | grep -c "pid=1111 ")" = 2 ]
check "adopt --holder: no 4242 left" nope grep -q "pid=4242" <<< "$(slots held)"
slots release --task client1 --holder 1111 >/dev/null
check "release --holder: server slot kept" slots held --task server
reset
slots take --tasks server,client1 --pid 1111 >/dev/null
slots take --task client1 --pid 4242 >/dev/null
slots release --task client1 --holder 4242 >/dev/null
check "release --holder: only the old client's entry" [ "$(slots held --task client1 | wc -l)" = 1 ]
check "release --holder: the reservation stays" grep -q "pid=1111" <<< "$(slots held --task client1)"

# 5. Other streams' entries are never released or adopted by us.
reset; foreign 1 other 1111
slots release >/dev/null; slots release --task server >/dev/null; slots adopt 5555 --task server >/dev/null
check "foreign kept" grep -q "stream=other pid=1111" <<< "$(owner 1)"

# 6. clean: dead holder AND older than CA_STALE_MIN (20) min; a living or young holder stays.
reset
foreign 1 a 7777 21          # dead, old      -> removed
foreign 2 b 1111 120         # alive, old     -> kept
foreign 3 c 8888 2           # dead, young    -> kept (grace for a take about to adopt)
mkdir -p "$CA_LOCK_DIR/ca-test.lock"; touch -d "-30 minutes" "$CA_LOCK_DIR/ca-test.lock"   # no owner file, old -> removed
slots clean >/dev/null
check "clean: dead old removed" [ ! -d "$(slot 1)" ]
check "clean: alive old kept" [ -d "$(slot 2)" ]
check "clean: dead young kept" [ -d "$(slot 3)" ]
check "clean: ownerless old lock removed" [ ! -d "$CA_LOCK_DIR/ca-test.lock" ]
CA_STALE_MIN=1 slots clean >/dev/null
check "clean: CA_STALE_MIN" [ ! -d "$(slot 3)" ]

# 7. take cleans first: a dead holder no longer blocks the pair.
reset; foreign 1 a 7777 30; foreign 2 b 7778 30; foreign 3 c 1111
slots take --tasks server,client1 --pid 1111 >/dev/null
check "take after clean: pair held" [ "$(slots held | wc -l)" = 2 ]

# 8. Unknown liveness (PowerShell fails) keeps everything. Windows only: the posix lookup (kill -0, /proc) cannot fail.
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) windows=1 ;; *) windows=0 ;; esac
if [ $windows = 1 ]; then
    reset; foreign 1 a 7777 30
    mkdir -p "$SCRATCH/bin"; printf '#!/bin/sh\nexit 1\n' > "$SCRATCH/bin/powershell"; chmod +x "$SCRATCH/bin/powershell"
    ( unset CA_ALIVE_PIDS; PATH="$SCRATCH/bin:$PATH" bash "$T/slots.sh" clean >/dev/null )
    check "clean: unknown liveness keeps" [ -d "$(slot 1)" ]
else
    echo "skip clean: unknown liveness keeps (posix)"
fi

# 9. The real liveness lookup (PowerShell on Windows, kill -0 elsewhere): this shell is alive, PID 999999 is not.
reset; foreign 1 a 999999 30; foreign 2 b "$ME" 30
( unset CA_ALIVE_PIDS; bash "$T/slots.sh" clean >/dev/null )
check "real lookup: dead removed" [ ! -d "$(slot 1)" ]
check "real lookup: alive kept" [ -d "$(slot 2)" ]
check "status marks dead holder" grep -q "holder dead" <<< "$(foreign 3 c 999999; unset CA_ALIVE_PIDS; bash "$T/slots.sh" status)"

# 10. run frees its slot; argument checks; the play lock.
reset
slots run --task t -- bash -c "[ -d '$(slot 1)' ]" >/dev/null; rc=$?
check "run: slot held during the command" [ "$rc" = 0 ]
check "run: freed after" [ "$(count)" = 0 ]
check "-n 0 needs --lock" nope slots take -n 0
check "--tasks max 3" nope slots take --tasks a,b,c,d
touch "$CA_LOCK_DIR/ca-user-test.lock"
slots take --tasks server,client1 --pid 1111 >/dev/null 2>&1; rc=$?
check "play lock: exit 3" [ "$rc" = 3 ]
check "play lock: nothing held" [ "$(count)" = 0 ]
rm -f "$CA_LOCK_DIR/ca-user-test.lock"

# 11. --wait retries without holding anything and gets the pair once a slot frees (20 s retry).
if [ "$1" = --slow ]; then
    reset; foreign 1 a 1111; foreign 2 b 2222
    ( sleep 5; rm -rf "$(slot 2)" ) &
    slots take --tasks server,client1 --pid 1111 --wait 60 >/dev/null 2>&1; rc=$?
    check "wait: pair held after a slot freed" [ "$rc" = 0 ]
    check "wait: two of ours" [ "$(slots held | wc -l)" = 2 ]
    wait
fi

[ "$fails" = 0 ] && echo "all checks passed" || echo "$fails check(s) failed"
[ "$fails" = 0 ]
