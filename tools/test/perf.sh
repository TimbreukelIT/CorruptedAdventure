#!/bin/bash
# Worldgen performance (waves.md §6): JFR minecraft.ChunkGeneration, the mod against vanilla datapack dimensions in one
# server, steps noise/surface/features, median per area; by default 3 warm rounds + 1 cold round, a fresh world per
# round, and a HARD STOP.
#
#   perf.sh [--warm 3] [--cold 1] [--areas FILE] [--only "a b"] [--cold-only "a b"] [--max-min 45] [--name NAME]
#           [--ref DIR] [--wait 1800]
#
# Holds the machine lock + all 3 JVM slots for the whole run (never two measurements at once), released on any exit.
# After --max-min minutes no new round or area starts; a watchdog stops the server 2 minutes later if a round hangs.
# Output in build/test/<NAME>/ (default perf-<date>-<time>): p<round>.json, p<round>.txt and table.md
# (py/perfsum.py; --ref adds the noise of an earlier run directory). The JFR recordings and the round worlds are
# deleted after each round (everything stays on P:, never C:).
# Cold round default areas: ring2 great caldera titan ember tube abyss (the full list does not fit in the hard stop).
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
warm=3; cold=1; areas="$CA_TOOLS/areas/perf.txt"; only=""; cold_only="ring2 great caldera titan ember tube abyss"
max_min=45; name="perf-$(date +%Y%m%d-%H%M)"; ref=(); wait=1800
while [ $# -gt 0 ]; do
    case "$1" in
        --warm) warm="$2"; shift 2 ;;
        --cold) cold="$2"; shift 2 ;;
        --areas) areas="$2"; shift 2 ;;
        --only) only="$2"; shift 2 ;;
        --cold-only) cold_only="$2"; shift 2 ;;
        --max-min) max_min="$2"; shift 2 ;;
        --name) name="$2"; shift 2 ;;
        --ref) ref=(--ref "$(cygpath -m "$2")"); shift 2 ;;
        --wait) wait="$2"; shift 2 ;;
        *) ca_die "unknown argument $1" ;;
    esac
done
ca_user_check
out="$CA_OUT/$name"; mkdir -p "$out"
start=$(date +%s); deadline=$((start + max_min * 60))
bash "$CA_TOOLS/slots.sh" take -n 3 --lock --task perf --wait "$wait" || exit $?
watchdog=""
cleanup() {
    [ -n "$watchdog" ] && kill "$watchdog" 2>/dev/null
    [ -n "$(bash "$CA_TOOLS/server.sh" status | grep '^PID=')" ] && bash "$CA_TOOLS/server.sh" stop --timeout 60
    rm -f "$CA_WT/run/debug/"*.jfr "$CA_WT/run/debug/"jfr-report-*.json
    bash "$CA_TOOLS/slots.sh" release --task perf
}
trap cleanup EXIT
trap 'exit 130' INT TERM
(
    sleep $(( deadline - $(date +%s) + 120 ))
    echo "hard stop at $(date +%T)" > "$out/HARDSTOP"
    bash "$CA_TOOLS/server.sh" stop --timeout 60
) > "$out/watchdog.log" 2>&1 &
watchdog=$!
ca_log "perf $name: $warm warm + $cold cold rounds, hard stop at $(date -d "@$deadline" +%T)"

for ((i = 1; i <= warm + cold; i++)); do
    if [ "$(date +%s)" -ge "$deadline" ]; then ca_log "hard stop: round $i not started"; break; fi
    args=(--run "$(cygpath -m "$CA_WT/run")" --areas "$(cygpath -m "$areas")" --deadline "$deadline")
    if [ "$i" -gt "$warm" ]; then
        args+=(--nowarm); [ -n "$cold_only" ] && { read -r -a sel <<< "$cold_only"; args+=(--only "${sel[@]}"); }
    elif [ -n "$only" ]; then
        read -r -a sel <<< "$only"; args+=(--only "${sel[@]}")
    fi
    level="$name-p$i"
    bash "$CA_TOOLS/server.sh" start "$level" --fresh --perf --no-slot || exit 1
    java="$(ca_state_get server JAVA)"
    args+=(--jfr "$(dirname "$java")/jfr.exe")
    $CA_PY "$CA_TOOLS/py/perf.py" "$i" "$(cygpath -m "$out/p$i.json")" "${args[@]}" > "$out/p$i.txt" 2>&1
    grep "^R$i" "$out/p$i.txt" | sed 's/^/  /'
    bash "$CA_TOOLS/server.sh" stop --timeout 120
    rm -f "$CA_WT/run/debug/"*.jfr "$CA_WT/run/debug/"jfr-report-*.json
    rm -rf "${CA_WT:?}/run/$level"
done
$CA_PY "$CA_TOOLS/py/perfsum.py" "$(cygpath -m "$out")" "${ref[@]}" --out "$(cygpath -m "$out/table.md")"
ca_log "perf done in $(( ($(date +%s) - start) / 60 )) min: $out/table.md"
