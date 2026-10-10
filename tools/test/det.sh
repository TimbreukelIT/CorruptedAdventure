#!/bin/bash
# Determinism (waves.md §6): two fresh worlds of the same seed, the second loaded in the reverse order (the Nether
# first), compared block by block on the mod's own blocks.
#
#   det.sh [--areas FILE] [--name det] [--all] [--settle 0] [--compare-only]
#
# Makes run/<name>-a (areas forward) and run/<name>-b (everything reversed) with world.sh, then
# py/detcmp.py -> build/test/<name>.txt (+ .json). --compare-only skips the worlds (they exist already).
# Default areas: tools/test/areas/int.txt. Exit code of detcmp: 1 own block differs or chunks missing, 2 only `other`
# differences (explain them), 0 clean.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
areas="$CA_TOOLS/areas/int.txt"; name=det; all=(); settle=0; make=1
while [ $# -gt 0 ]; do
    case "$1" in
        --areas) areas="$2"; shift 2 ;;
        --name) name="$2"; shift 2 ;;
        --all) all=(--all); shift ;;
        --settle) settle="$2"; shift 2 ;;
        --compare-only) make=0; shift ;;
        *) ca_die "unknown argument $1" ;;
    esac
done
if [ $make = 1 ]; then
    bash "$CA_TOOLS/world.sh" "$name-a" "$areas" --settle "$settle" || exit $?
    bash "$CA_TOOLS/world.sh" "$name-b" "$areas" --reverse --settle "$settle" || exit $?
fi
run="$(cygpath -m "$CA_WT/run")"
$CA_PY "$CA_TOOLS/py/detcmp.py" "$run/$name-a" "$run/$name-b" "$(cygpath -m "$areas")" "${all[@]}" \
    --json "$(cygpath -m "$CA_OUT/$name.json")" | tee "$CA_OUT/$name.txt"
exit "${PIPESTATUS[0]}"
