#!/bin/bash
# Dev client of this stream, joining the stream's own server with quick play (127.0.0.1:25570+n; not "localhost",
# which resolves to ::1 first while the server listens on 127.0.0.1 only).
#
#   client.sh start [1|2] [--xmx 2560M] [--timeout 600] [--wait 1800]
#       needs a running server (server.sh start); takes the machine lock + a JVM slot (the quick-play start is
#       machine-wide, waves.md §4; the lock is freed once joined, the slot stays with the client), starts runClient (1 = Dev, run/) or runClient2 (2 = Dev2, run/client2), waits until
#       "<name> joined the game" in the server log, records the client JVM PID (build/<stream>.pids, build/test/client<k>.state)
#   client.sh shot <out.png> [1|2]     screenshot of OUR client window only (PrintWindow on the recorded PID, after
#                                      checking that PID is still this checkout's client JVM; never by window title)
#   client.sh key <F1|F2|F3|ESC|T|ENTER> [1|2]
#   client.sh stop [1|2]               close our client (WM_CLOSE, then the recorded PID only), free lock + slot
# Exit 3 when the user is playing (C:/Temp/ca-user-test.lock).
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

k=1
pick() { case "$1" in 1|2) k="$1" ;; "") ;; *) ca_die "client number must be 1 or 2" ;; esac; }
kind() { [ "$k" = 2 ] && echo client2 || echo client; }

own_pid() {
    local pid wt
    pid="$(ca_state_get "client$k" PID)"; wt="$(ca_state_get "client$k" WT)"
    [ -n "$pid" ] && ca_is_own_java "$pid" "$wt" "$(kind)" && echo "$pid"
}

start() {
    local xmx=2560M timeout=600 wait=1800
    while [ $# -gt 0 ]; do
        case "$1" in
            1|2) pick "$1"; shift ;;
            --xmx) xmx="$2"; shift 2 ;;
            --timeout) timeout="$2"; shift 2 ;;
            --wait) wait="$2"; shift 2 ;;
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
    bash "$CA_TOOLS/slots.sh" take -n 1 --lock --task "client$k" --wait "$wait" || exit $?
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
            bash "$CA_TOOLS/slots.sh" release --task "client$k" >/dev/null
            exit 1
        fi
        sleep 3
    done
    if [ -z "$pid" ]; then
        echo "client joined but its JVM PID was not found; kick it and stop" >&2
        $CA_PY "$CA_TOOLS/py/rcon.py" "kick $name" >&2
        ca_stop_job "$job"
        bash "$CA_TOOLS/slots.sh" release --task "client$k" >/dev/null
        exit 1
    fi
    printf 'PID=%s\nWT=%s\nNAME=%s\n' "$pid" "$(cygpath -m "$wt")" "$name" > "$CA_OUT/client$k.state"
    # The lock only covers the quick-play start; the slot stays with the client JVM until client.sh stop.
    bash "$CA_TOOLS/slots.sh" release --task "client$k" --lock-only >/dev/null
    bash "$CA_TOOLS/slots.sh" adopt "$pid" --task "client$k" >/dev/null
    ca_log "client $k ($name) joined: pid $pid"
}

win() { powershell -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$CA_TOOLS/win.ps1")" "$@" | tr -d '\r'; }

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
    bash "$CA_TOOLS/slots.sh" release --task "client$k" >/dev/null
    ca_log "client $k stopped (${pid:-no pid})"
}

cmd="$1"; shift
case "$cmd" in
    start) start "$@" ;;
    stop) pick "$1"; stop ;;
    shot)
        [ -n "$1" ] || ca_die "shot needs an output file"
        out="$1"; pick "$2"
        pid="$(own_pid)"; [ -n "$pid" ] || ca_die "client $k is not running (or its PID is not ours)"
        mkdir -p "$(dirname "$out")"
        win shot "$pid" "$(cygpath -w "$(cd "$(dirname "$out")" && pwd)/$(basename "$out")")" ;;
    key)
        pick "$2"; pid="$(own_pid)"; [ -n "$pid" ] || ca_die "client $k is not running (or its PID is not ours)"
        win key "$pid" "$1" ;;
    list) pick "$1"; pid="$(own_pid)"; [ -n "$pid" ] && win list "$pid" ;;
    *) sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
