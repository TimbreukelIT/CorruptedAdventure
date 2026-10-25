#!/bin/bash
# Dev client of this stream, joining the stream's own server with quick play (127.0.0.1:25570+n; not "localhost",
# which resolves to ::1 first while the server listens on 127.0.0.1 only).
#
#   client.sh start [1|2] [--xmx 2560M] [--timeout 600] [--wait 1800]
#       needs a running server (server.sh start); takes the machine lock + a JVM slot (the quick-play start is
#       machine-wide, waves.md §4; the lock is freed once joined, the slot stays with the client), starts runClient
#       (1 = Dev, run/) or runClient2 (2 = Dev2, run/client2), waits until "<name> joined the game" in the server log,
#       records the client JVM PID (build/<stream>.pids, build/test/client<k>.state). When the server reserved this
#       client's slot (server.sh start --client), only the lock is taken and the reserved slot is used: no slot wait.
#       Without a reservation while this stream's server holds a slot it tries once (no --wait given) or waits --wait.
#   client.sh shot <out.png> [1|2]     screenshot of OUR client window only (PrintWindow on the recorded PID, after
#                                      checking that PID is still this checkout's client JVM; never by window title).
#                                      A blank (flat white or black) image falls back to F2 (Minecraft's own
#                                      screenshot, run/screenshots) with a warning; exit 4 when no F2 file comes.
#                                      CA_SHOT_F2=1: always F2 (walk 1: with the display off PrintWindow can also
#                                      return the last composed frame again and again, not blank: a stale series).
#   client.sh key <F1|F2|F3|ESC|T|ENTER> [1|2]
#   client.sh stop [1|2]               close our client (WM_CLOSE, then the recorded PID only), free lock + slot
#                                      (a reserved slot goes back to the running server instead)
# Exit 3 when the user is playing (C:/Temp/ca-user-test.lock).
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
# Window handling and screenshots go through win.ps1 (PowerShell, user32): Windows only.
[ "$CA_OS" = windows ] || ca_die "client.sh needs Windows (Git Bash + PowerShell for the client window); on $(uname -s) only the headless server tools work"

k=1
pick() { case "$1" in 1|2) k="$1" ;; "") ;; *) ca_die "client number must be 1 or 2" ;; esac; }
kind() { [ "$k" = 2 ] && echo client2 || echo client; }

# free_slot <holder>: free this client's lock and the slot held by <holder> (only that entry: a restarted server may
# hold a new reservation with the same label). A slot the running server reserved for this client (server.sh start
# --client) goes back to the server instead, so a next client.sh start needs no slot again. Without a holder (no
# recorded PID) every slot of this client is freed, as before.
free_slot() {
    local holder="$1" spid
    bash "$CA_TOOLS/slots.sh" release --task "client$k" --lock-only >/dev/null
    if [ -z "$holder" ]; then
        bash "$CA_TOOLS/slots.sh" release --task "client$k" >/dev/null
    elif [[ " $(ca_state_get server CLIENTS) " == *" $k "* ]] && spid="$(ca_server_pid)" && [ -n "$spid" ]; then
        [ "$holder" = "$spid" ] || bash "$CA_TOOLS/slots.sh" adopt "$spid" --task "client$k" --holder "$holder" >/dev/null
    else
        bash "$CA_TOOLS/slots.sh" release --task "client$k" --holder "$holder" >/dev/null
    fi
}

# Holder PID of this stream's slot for client k (a reservation, or a leftover of a crashed client), else nothing.
slot_holder() {
    bash "$CA_TOOLS/slots.sh" held --task "client$k" 2>/dev/null | sed -n 's/.* pid=\([0-9]*\) .*/\1/p' | head -1
}

# Take the lock and this client's slot: the reserved one when there is one (lock only), else a free one. Sets
# `holder` to the PID that holds the slot (the server for a reservation, this script otherwise).
take_slot() {
    local wait="$1" wait_set="$2" rc
    holder="$(slot_holder)"
    if [ -n "$holder" ]; then
        bash "$CA_TOOLS/slots.sh" take -n 0 --lock --task "client$k" --wait "$wait" || exit $?
        # `take` cleans stale entries first: a reservation whose holder died may be gone now.
        bash "$CA_TOOLS/slots.sh" held --task "client$k" --holder "$holder" >/dev/null && return 0
        bash "$CA_TOOLS/slots.sh" release --task "client$k" --lock-only >/dev/null
    fi
    if [ "$wait_set" = 0 ] && bash "$CA_TOOLS/slots.sh" held --task server >/dev/null; then
        # Waiting for a slot while our own server holds one is how three streams deadlocked on 2026-10-19.
        wait=0
        echo "note: this stream's server holds a slot and none was reserved for client $k: one try only" \
            "(server.sh start --client reserves both at once; --wait SEC waits anyway)" >&2
    fi
    bash "$CA_TOOLS/slots.sh" take -n 1 --lock --task "client$k" --wait "$wait"; rc=$?
    [ $rc = 0 ] || exit $rc
    holder="$(ca_winpid)"
}

own_pid() {
    local pid wt
    pid="$(ca_state_get "client$k" PID)"; wt="$(ca_state_get "client$k" WT)"
    [ -n "$pid" ] && ca_is_own_java "$pid" "$wt" "$(kind)" && echo "$pid"
}

start() {
    local xmx=2560M timeout=600 wait=1800 wait_set=0 holder
    while [ $# -gt 0 ]; do
        case "$1" in
            1|2) pick "$1"; shift ;;
            --xmx) xmx="$2"; shift 2 ;;
            --timeout) timeout="$2"; shift 2 ;;
            --wait) wait="$2"; wait_set=1; shift 2 ;;
            *) ca_die "unknown argument $1" ;;
        esac
    done
    ca_user_check
    ca_need_port
    local wt slog name task before log pid t0
    wt="$(ca_state_get server WT)"; slog="$(ca_state_get server LOG)"
    [ -n "$wt" ] && [ -f "$slog" ] || ca_die "no running server of this stream (server.sh start first)"
    [ -z "$(own_pid)" ] || ca_die "client $k is already running (pid $(own_pid))"
    name=Dev; task=runClient; [ "$k" = 2 ] && { name=Dev2; task=runClient2; }
    take_slot "$wait" "$wait_set"
    ca_prepare_options "$wt"
    log="$CA_OUT/client$k.log"; rm -f "$log"
    before=" $(ca_java_pids "$wt" "$(kind)" | tr '\n' ' ') "
    local joined_before
    joined_before=$(grep -c "$name joined the game" "$slog")
    ca_log "starting $task -> 127.0.0.1:$CA_SERVER_PORT"
    (exec > "$log" 2>&1 < /dev/null; cd "$wt" && JAVA_TOOL_OPTIONS="-Xmx$xmx" exec nohup ./gradlew "$task" "-PquickPlay=127.0.0.1:$CA_SERVER_PORT" --no-daemon --console=plain) &
    local job=$!
    t0=$(date +%s); pid=""
    while true; do
        if [ -z "$pid" ]; then
            for p in $(ca_java_pids "$wt" "$(kind)"); do
                case "$before" in *" $p "*) ;; *) pid="$p"; ca_record_pid "$pid" "client$k wt=$(ca_winpath "$wt")";; esac
            done
        fi
        [ "$(grep -c "$name joined the game" "$slog")" -gt "$joined_before" ] && break
        if grep -qE "BUILD FAILED|FAILURE: Build failed" "$log" 2>/dev/null || [ $(( $(date +%s) - t0 )) -gt "$timeout" ]; then
            echo "client $k did not join (see $log)" >&2
            [ -n "$pid" ] && ca_is_own_java "$pid" "$wt" "$(kind)" && ca_stop_pid "$pid"
            ca_stop_job "$job"
            free_slot "$holder"
            exit 1
        fi
        sleep 3
    done
    if [ -z "$pid" ]; then
        echo "client joined but its JVM PID was not found; kick it and stop" >&2
        $CA_PY "$CA_TOOLS/py/rcon.py" "kick $name" >&2
        ca_stop_job "$job"
        free_slot "$holder"
        exit 1
    fi
    printf 'PID=%s\nWT=%s\nNAME=%s\n' "$pid" "$(cygpath -m "$wt")" "$name" > "$CA_OUT/client$k.state"
    # The lock only covers the quick-play start; the slot stays with the client JVM until client.sh stop.
    bash "$CA_TOOLS/slots.sh" release --task "client$k" --lock-only >/dev/null
    bash "$CA_TOOLS/slots.sh" adopt "$pid" --task "client$k" --holder "$holder" >/dev/null
    ca_log "client $k ($name) joined: pid $pid"
}

win() {
    local out rc
    out="$(powershell -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$CA_TOOLS/win.ps1")" "$@")"; rc=$?
    printf '%s\n' "$out" | tr -d '\r'
    return $rc
}

# PrintWindow first; a blank image (display off after the power plan's idle timeout, window minimized: DWM composes
# nothing, see win.ps1) falls back to F2, which reads the game's own framebuffer. F2 adds a "Saved screenshot" chat
# line to the next frames (hidden with F1).
shot() {
    local out="$1" winout res rc dir before after size stable t0
    pid="$(own_pid)"; [ -n "$pid" ] || ca_die "client $k is not running (or its PID is not ours)"
    mkdir -p "$(dirname "$out")"
    winout="$(cygpath -w "$(cd "$(dirname "$out")" && pwd)/$(basename "$out")")"
    if [ "${CA_SHOT_F2:-0}" != 1 ]; then
        res="$(win shot "$pid" "$winout")"; rc=$?
        [ $rc = 0 ] && { echo "$res"; return 0; }
        [ $rc = 4 ] || { echo "$res" >&2; return $rc; }
        echo "warning: PrintWindow image is blank ($res) - the display is off (no user input for the power plan's" \
            "15 min) or the window is minimized; falling back to F2" >&2
    fi
    dir="$(ca_state_get "client$k" WT)/run$([ "$k" = 2 ] && echo /client2)/screenshots"
    before="$(ls -t "$dir"/*.png 2>/dev/null | head -1)"
    win key "$pid" F2 >/dev/null || { echo "warning: F2 could not be sent; $out stays blank" >&2; return 4; }
    t0=$(date +%s); after=""; size=-1; stable=0
    while [ $(( $(date +%s) - t0 )) -lt 20 ]; do
        sleep 1
        after="$(ls -t "$dir"/*.png 2>/dev/null | head -1)"
        [ -n "$after" ] && [ "$after" != "$before" ] || continue
        # Minecraft writes the file on an IO thread: only a size seen twice in a row counts as written.
        [ "$(stat -c %s "$after")" = "$size" ] && [ "$size" -gt 0 ] && { stable=1; break; }
        size="$(stat -c %s "$after")"
    done
    if [ $stable = 0 ]; then
        echo "warning: no complete F2 screenshot appeared in $dir within 20 s; $out stays blank" >&2
        return 4
    fi
    cp "$after" "$out"
    res="$(win blank 0 "$winout")"; rc=$?
    # F2 is the game's own frame: a flat one is what the game shows (a black cave with F1), so it is kept.
    [ $rc = 4 ] && echo "warning: the F2 screenshot is flat as well ($res); kept, check the scene" >&2
    [ $rc = 0 ] || [ $rc = 4 ] || { echo "warning: could not check the F2 screenshot ($res)" >&2; return 4; }
    echo "saved $winout (F2 fallback: $(basename "$after"))"
}

stop() {
    local pid wt t0
    pid="$(ca_state_get "client$k" PID)"; wt="$(ca_state_get "client$k" WT)"
    if [ -n "$pid" ] && ca_is_own_java "$pid" "$wt" "$(kind)"; then
        win close "$pid" >/dev/null 2>&1
        t0=$(date +%s)
        while ca_pid_alive "$pid" && [ $(( $(date +%s) - t0 )) -lt 30 ]; do sleep 2; done
        ca_is_own_java "$pid" "$wt" "$(kind)" && ca_stop_pid "$pid"
    fi
    rm -f "$CA_OUT/client$k.state"
    free_slot "$pid"
    ca_log "client $k stopped (${pid:-no pid})"
}

cmd="$1"; shift
case "$cmd" in
    start) start "$@" ;;
    stop) pick "$1"; stop ;;
    shot)
        [ -n "$1" ] || ca_die "shot needs an output file"
        pick "$2"; shot "$1" ;;
    key)
        pick "$2"; pid="$(own_pid)"; [ -n "$pid" ] || ca_die "client $k is not running (or its PID is not ours)"
        win key "$pid" "$1" ;;
    list) pick "$1"; pid="$(own_pid)"; [ -n "$pid" ] && win list "$pid" ;;
    *) sed -n '2,/^source/p' "$0" | grep '^#' | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
