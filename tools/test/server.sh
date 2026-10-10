#!/bin/bash
# Dedicated test server of this stream (Gradle runServer, port 25570+n, RCON 25580+n on 127.0.0.1).
#
#   server.sh start <level> [--fresh] [--perf] [--xmx 5G] [--timeout 900] [--wait 1800] [--no-slot] [--worktree DIR]
#       prepares run/ (eula, server.properties: level, seed 20260930, corrupted world preset, ports, RCON on with a new
#       password), takes a JVM slot (unless --no-slot: the caller holds one, e.g. perf.sh), starts runServer in the
#       background, waits for "Done" (time-out --timeout s), records the server JVM PID in build/<stream>.pids and the
#       state in build/test/server.state (rcon.py reads it). --fresh deletes run/<level> first; --perf installs the
#       vanilla datapack dimensions (perfpack/). --worktree runs another checkout (e.g. a reference on main) with these
#       tools; its run/ is used, state and logs stay here. Log: build/test/server-<level>.log.
#   server.sh stop [--timeout 180]
#       RCON `stop`, waits until the recorded PID is gone (only then, after the time-out, stops that PID - after
#       checking it is still this checkout's server JVM), turns RCON off, frees the slot.
#   server.sh status
# Exit 3 when the user is playing (C:/Temp/ca-user-test.lock): no JVM starts.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
STATE="$CA_OUT/server.state"

running_pid() {
    local pid wt
    pid="$(ca_state_get server PID)"; wt="$(ca_state_get server WT)"
    [ -n "$pid" ] && ca_is_own_java "$pid" "$wt" server && echo "$pid"
}

start() {
    local level="$1"; shift
    [[ "$level" =~ ^[A-Za-z0-9._-]+$ ]] || ca_die "level name must be [A-Za-z0-9._-]+"
    local fresh=0 perf=0 xmx=5G timeout=900 wait=1800 slot=1 wt="$CA_WT"
    while [ $# -gt 0 ]; do
        case "$1" in
            --fresh) fresh=1; shift ;;
            --perf) perf=1; shift ;;
            --xmx) xmx="$2"; shift 2 ;;
            --timeout) timeout="$2"; shift 2 ;;
            --wait) wait="$2"; shift 2 ;;
            --no-slot) slot=0; shift ;;
            --worktree) wt="$(cd "$2" && pwd)" || ca_die "no worktree $2"; shift 2 ;;
            *) ca_die "unknown argument $1" ;;
        esac
    done
    ca_user_check
    ca_need_port
    [ -z "$(running_pid)" ] || ca_die "a server of this stream is still running (pid $(running_pid)); server.sh stop first"
    [ -x "$wt/gradlew" ] || ca_die "$wt has no gradlew"
    if [ $slot = 1 ]; then
        bash "$CA_TOOLS/slots.sh" take -n 1 --task "server" --wait "$wait" || exit $?
    fi
    ca_prepare_run "$wt" "$level"
    local props="$wt/run/server.properties" password
    password="ca-$CA_STREAM-$RANDOM$RANDOM"
    ca_setprop "$props" enable-rcon true
    ca_setprop "$props" rcon.password "$password"
    if [ $fresh = 1 ]; then rm -rf "${wt:?}/run/$level"; fi
    if [ $perf = 1 ]; then
        mkdir -p "$wt/run/$level/datapacks"
        rm -rf "$wt/run/$level/datapacks/perftest"
        cp -r "$CA_TOOLS/perfpack" "$wt/run/$level/datapacks/perftest"
    fi
    local log="$CA_OUT/server-$level.log" before pid t0
    before=" $(ca_java_pids "$wt" server | tr '\n' ' ') "
    rm -f "$log"
    ca_log "starting runServer in $wt on level $level (port $CA_SERVER_PORT, RCON $CA_RCON_PORT)"
    # All of the background job's descriptors go to the log first, so it never holds the caller's stdout (a pipe).
    (exec > "$log" 2>&1 < /dev/null; cd "$wt" && JAVA_TOOL_OPTIONS="-Xmx$xmx" exec nohup ./gradlew runServer --console=plain) &
    t0=$(date +%s); pid=""
    while true; do
        if [ -z "$pid" ]; then
            for p in $(ca_java_pids "$wt" server); do
                case "$before" in *" $p "*) ;; *) pid="$p"; ca_record_pid "$pid" "server $level port=$CA_SERVER_PORT wt=$(ca_winpath "$wt")";; esac
            done
        fi
        if grep -qE "Done \([0-9.]+s\)!" "$log" 2>/dev/null; then break; fi
        if grep -qE "Exception in server tick loop|BUILD FAILED|Failed to start|FAILURE: Build failed" "$log" 2>/dev/null \
            || [ $(( $(date +%s) - t0 )) -gt "$timeout" ]; then
            echo "server did not start (see $log):" >&2
            grep -E "Exception|BUILD FAILED|Failed to start|FAILURE" "$log" | head -5 >&2
            [ -n "$pid" ] && ca_is_own_java "$pid" "$wt" server && ca_stop_pid "$pid"
            ca_setprop "$props" enable-rcon false
            [ $slot = 1 ] && bash "$CA_TOOLS/slots.sh" release --task server >/dev/null
            exit 1
        fi
        sleep 3
    done
    if [ -z "$pid" ]; then
        # Without a PID we cannot prove it is ours later: stop it now via RCON and give up.
        echo "server is up but its JVM PID was not found; stopping it via RCON" >&2
        RCON_PORT=$CA_RCON_PORT RCON_PASSWORD="$password" $CA_PY "$CA_TOOLS/py/rcon.py" stop >&2
        ca_setprop "$props" enable-rcon false
        [ $slot = 1 ] && bash "$CA_TOOLS/slots.sh" release --task server >/dev/null
        exit 1
    fi
    local java
    java="$(cygpath -m "$(powershell -NoProfile -Command "(Get-CimInstance Win32_Process -Filter 'ProcessId=$pid').ExecutablePath" | tr -d '\r')")"
    cat > "$STATE" <<EOF
PID=$pid
LEVEL=$level
WT=$(cygpath -m "$wt")
WORLD=$(cygpath -m "$wt/run/$level")
LOG=$(cygpath -m "$log")
PORT=$CA_SERVER_PORT
RCON_PORT=$CA_RCON_PORT
RCON_PASSWORD=$password
JAVA=$java
SLOT=$slot
EOF
    [ $slot = 1 ] && bash "$CA_TOOLS/slots.sh" adopt "$pid" --task server >/dev/null
    grep -E "Done \(" "$log" | tail -1
    ca_log "server up: pid $pid, world $(cygpath -m "$wt/run/$level")"
}

stop() {
    local timeout=180
    [ "$1" = --timeout ] && timeout="$2"
    [ -f "$STATE" ] || { echo "no server state (nothing to stop)"; return 0; }
    local pid wt slot t0
    pid="$(ca_state_get server PID)"; wt="$(ca_state_get server WT)"; slot="$(ca_state_get server SLOT)"
    if ca_is_own_java "$pid" "$wt" server; then
        $CA_PY "$CA_TOOLS/py/rcon.py" stop || echo "RCON stop failed; waiting for the time-out" >&2
        t0=$(date +%s)
        while ca_pid_alive "$pid"; do
            if [ $(( $(date +%s) - t0 )) -gt "$timeout" ]; then
                ca_is_own_java "$pid" "$wt" server && { echo "pid $pid still running after ${timeout}s: stopping it" >&2; ca_stop_pid "$pid"; }
                sleep 3
                break
            fi
            sleep 3
        done
    else
        echo "pid $pid is not running (or not this checkout's server)"
    fi
    ca_setprop "$wt/run/server.properties" enable-rcon false
    [ "$slot" = 1 ] && bash "$CA_TOOLS/slots.sh" release --task server >/dev/null
    rm -f "$STATE"
    ca_log "stopped server $pid; RCON off"
}

cmd="$1"; shift
case "$cmd" in
    start) [ -n "$1" ] || ca_die "start needs a level name"; start "$@" ;;
    stop) stop "$@" ;;
    status)
        if [ -n "$(running_pid)" ]; then cat "$STATE" | grep -v PASSWORD; else echo "no server running"; fi ;;
    *) sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
