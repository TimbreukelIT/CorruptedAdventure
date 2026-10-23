#!/bin/bash
# Photo studio for structures (S0, dev only; docs/plans/structures/s0-studio.md). Uses this stream's running server and
# client (tools/test/): places one template or structure with /ca studio, photographs every camera pose over RCON +
# client.sh shot and glues the shots into one labelled contact sheet: build/studio/<name>/sheet.png (+ the single shots).
#
#   studio.sh prepare [1|2] [--render 16]
#       once, before client.sh start: the client's options.txt gets the studio's fixed view (FOV 70, no FOV effects,
#       no clouds, render distance --render). Changes that checkout's run/options.txt (or run/client2/), nothing else.
#   studio.sh template <id> [--rot none|clockwise_90|180|counterclockwise_90] [--mirror none|left_right|front_back]
#                           [--here X Y Z [--dim DIM]] [common options]
#   studio.sh structure <id> [--seed S] [--here X Y Z [--dim DIM]] [common options]
#       without --here: in the studio dimensions (an empty void by day and by night; the server needs
#       `server.sh start <level> --studio`); with --here: in situ at X Y Z of DIM (default minecraft:overworld; Y
#       `surface` = the first free block above the ground; for a structure only X Z pick the chunk, its own rules pick Y).
#   studio.sh shots [common options]      photograph the current placement again
#   studio.sh sheet <dir>                 rebuild the contact sheet of a shot folder
#   common options: --name N (folder under build/studio; default from the id), --floors Y,Y (walking surface of every
#       floor for the cut-aways; default: detected), --client 1|2, --clear (clear the studio afterwards),
#       --keep (in situ: leave the structure; by default the terrain is restored after the shots, because the snapshot
#       that restores it lives only in the running server), --max-wait S (per shot, default 25)
#
# Start first:  bash tools/test/server.sh start studio --studio --client && bash tools/test/client.sh start
source "$(dirname "${BASH_SOURCE[0]}")/../test/lib.sh"

if [ "$1" = prepare ]; then
    shift
    k=1 render=16
    while [ $# -gt 0 ]; do
        case "$1" in
            1|2) k="$1"; shift ;;
            --render) render="$2"; shift 2 ;;
            *) ca_die "unknown argument $1" ;;
        esac
    done
    ca_prepare_options "$CA_ROOT"
    opts="$CA_ROOT/run/options.txt"; [ "$k" = 2 ] && opts="$CA_ROOT/run/client2/options.txt"
    ca_setprop "$opts" fov 0.0 ":"
    ca_setprop "$opts" fovEffectScale 0.0 ":"
    ca_setprop "$opts" renderClouds '"false"' ":"
    ca_setprop "$opts" renderDistance "$render" ":"
    ca_setprop "$opts" pauseOnLostFocus false ":"
    echo "prepared $opts (FOV 70, no FOV effects, no clouds, render distance $render); start the client after this"
    exit 0
fi

[ "$CA_OS" = windows ] || ca_die "studio.sh photographs with client.sh, which needs Windows"
STUDIO_BASH="$(cygpath -w "$BASH")" STUDIO_ROOT="$(cygpath -w "$CA_ROOT")" exec $CA_PY "$(cygpath -w "$CA_ROOT/tools/structures/studio.py")" "$@"
