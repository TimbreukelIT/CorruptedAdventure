#!/bin/bash
# Water against air outside the planned falls (contract §4, ring 4): waterscan.sh <level> <areas> [--settle S]
#
#   waterscan.sh ring4-ws tools/test/areas/ring4.txt
#
# Makes a fresh world of the areas with world.sh (--keep, so the server stays up), asks the server for the planned
# falls of every overworld area box (`/ca falls`, the same IslandField.waterfallsWithin as the terrain; written to
# <world>/ca_falls), stops the server, then runs py/waterscan.py on the saved regions and the fall lists.
# Output: build/test/waterscan-<level>.txt (+ .json). Exit code of waterscan.py: 1 when an area has an overtreding.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
[ $# -ge 2 ] || { sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }
level="$1"; areas="$2"; shift 2
settle=0
while [ $# -gt 0 ]; do
    case "$1" in
        --settle) settle="$2"; shift 2 ;;
        *) ca_die "unknown argument $1" ;;
    esac
done
[ -f "$areas" ] || ca_die "no area file $areas"
areas="$(cd "$(dirname "$areas")" && pwd)/$(basename "$areas")"

trap 'bash "$CA_TOOLS/server.sh" stop' EXIT
trap 'exit 130' INT TERM
out="$(bash "$CA_TOOLS/world.sh" "$level" "$areas" --keep --settle "$settle")" || exit $?
world="$(echo "$out" | tail -1)"

# One /ca falls per overworld area box, in the block coordinates the scan uses (chunks x 16, far corner + 15).
cmds=()
while read -r name cx0 cz0 cx1 cz1 dim; do
    case "$name" in ''|\#*) continue ;; esac
    [ -z "$dim" ] || [ "$dim" = ow ] || [ "$dim" = overworld ] || continue
    x0=$((cx0 * 16)); z0=$((cz0 * 16)); x1=$((cx1 * 16 + 15)); z1=$((cz1 * 16 + 15))
    cmds+=("ca falls $x0 $z0 $x1 $z1")
done < "$areas"
[ ${#cmds[@]} -gt 0 ] || ca_die "no overworld areas in $areas"
$CA_PY "$CA_TOOLS/py/rcon.py" "${cmds[@]}" || exit 1
bash "$CA_TOOLS/server.sh" stop || exit $?
trap 'exit 130' INT TERM

$CA_PY "$CA_TOOLS/py/waterscan.py" "$(cygpath -m "$world")" "$(cygpath -m "$areas")" "$(cygpath -m "$world/ca_falls")" \
    --json "$(cygpath -m "$CA_OUT/waterscan-$level.json")" | tee "$CA_OUT/waterscan-$level.txt"
exit "${PIPESTATUS[0]}"
