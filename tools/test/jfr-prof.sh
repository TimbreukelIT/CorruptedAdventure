#!/bin/bash
# CPU profile of chunk generation per area (hot methods), on a fresh world of this checkout.
#
#   jfr-prof.sh [--areas FILE] [--only "a b"] [--name NAME] [--nowarm] [--top 20] [--wait 1800]
#
# Holds the machine lock + all JVM slots like perf.sh (a quiet machine), starts the stream's server on a fresh
# world, and runs py/jfrprof.py: warm-up, then per area RCON `jfr start`, forceload, `jfr stop` (the server's own
# profiler, no jcmd attach). Output in build/test/<NAME>/ (default prof-<date>-<time>): <area>.jfr, <area>.txt (top self
# frames and inclusive mod methods, py/jfrhot.py). The world is deleted afterwards; the recordings stay (on P:).
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
areas="$CA_TOOLS/areas/perf.txt"; only=(); name="prof-$(date +%Y%m%d-%H%M)"; extra=(); wait=1800
while [ $# -gt 0 ]; do
    case "$1" in
        --areas) areas="$2"; shift 2 ;;
        --only) read -r -a sel <<< "$2"; only=(--only "${sel[@]}"); shift 2 ;;
        --name) name="$2"; shift 2 ;;
        --nowarm) extra+=(--nowarm); shift ;;
        --top) extra+=(--top "$2"); shift 2 ;;
        --wait) wait="$2"; shift 2 ;;
        *) ca_die "unknown argument $1" ;;
    esac
done
ca_user_check
out="$CA_OUT/$name"; mkdir -p "$out"
level="$name-w"
bash "$CA_TOOLS/slots.sh" take -n "$CA_SLOTS" --lock --task jfrprof --wait "$wait" || exit $?
cleanup() {
    [ -n "$(bash "$CA_TOOLS/server.sh" status | grep '^PID=')" ] && bash "$CA_TOOLS/server.sh" stop --timeout 120
    rm -rf "${CA_WT:?}/run/$level"
    bash "$CA_TOOLS/slots.sh" release --task jfrprof
}
trap cleanup EXIT
trap 'exit 130' INT TERM
# Density functions nest deeply: JFR's default of 64 frames per sample cuts off the chunk step at the root. Without
# DebugNonSafepoints the samples land on the nearest safepoint's frame, so self time sticks to the wrong (inlined) method.
# JFR runs from the JVM's start (CA_JFR_AT_START, lib.sh): the first `jfr start` must not deoptimize all compiled code.
CA_SERVER_JVM="$CA_JFR_AT_START -XX:FlightRecorderOptions:stackdepth=1024 -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints${CA_JVM_EXTRA:+ $CA_JVM_EXTRA}" bash "$CA_TOOLS/server.sh" start "$level" --fresh --no-slot || exit 1
java="$(ca_state_get server JAVA)"
$CA_PY "$CA_TOOLS/py/jfrprof.py" "$(cygpath -m "$out")" --run "$(cygpath -m "$CA_WT/run")" \
    --jfr "$(dirname "$java")/jfr$CA_EXE" --areas "$(cygpath -m "$areas")" "${only[@]}" "${extra[@]}" | tee "$out/prof.txt"
exit "${PIPESTATUS[0]}"
