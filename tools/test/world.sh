#!/bin/bash
# A fresh world with the chunks of an area file, for the offline scans.
#
#   world.sh <level> <areas> [--reverse] [--dim ow|nether] [--only "name ..."] [--settle 0] [--worktree DIR] [--keep]
#
# Starts the stream's server on a fresh run/<level> (server.sh start --fresh, takes and frees its slot), loads the areas
# with gen.py (forward, or everything reversed: --reverse; --settle keeps every tile forceloaded that many seconds
# longer, so fluids tick - use the same value for worlds you compare), `save-all flush`, and stops the server (also on an error or Ctrl-C). --keep leaves it running (stop it yourself with
# server.sh stop). --worktree makes the world in another checkout (e.g. a reference on main for the feature index).
# Prints the world directory on the last line.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
[ $# -ge 2 ] || { sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }
level="$1"; areas="$2"; shift 2
gen=(); srv=(); keep=0
while [ $# -gt 0 ]; do
    case "$1" in
        --reverse) gen+=(--reverse); shift ;;
        --dim) gen+=(--dim "$2"); shift 2 ;;
        --only) read -r -a names <<< "$2"; gen+=(--only "${names[@]}"); shift 2 ;;
        --settle) gen+=(--settle "$2"); shift 2 ;;
        --worktree) srv+=(--worktree "$2"); shift 2 ;;
        --keep) keep=1; shift ;;
        *) ca_die "unknown argument $1" ;;
    esac
done
[ -f "$areas" ] || ca_die "no area file $areas"
areas="$(cd "$(dirname "$areas")" && pwd)/$(basename "$areas")"

bash "$CA_TOOLS/server.sh" start "$level" --fresh "${srv[@]}" || exit $?
[ $keep = 1 ] || trap 'bash "$CA_TOOLS/server.sh" stop' EXIT
trap 'exit 130' INT TERM
world="$(ca_state_get server WORLD)"
$CA_PY "$CA_TOOLS/py/gen.py" "$(cygpath -m "$areas")" "${gen[@]}" || exit 1
$CA_PY "$CA_TOOLS/py/rcon.py" "save-all flush"
echo "$world"
