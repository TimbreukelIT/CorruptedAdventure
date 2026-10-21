#!/bin/bash
# Machine lock + JVM slots (waves.md §4, agent-rules-golf3). At most 3 Gradle/Minecraft JVMs on the machine: every
# build, runData, runServer and runClient holds a slot `<lockdir>/ca-jvm-slot-{1,2,3}`; machine-wide things (the
# quick-play start of a client, JFR, ffmpeg) also hold the lock `<lockdir>/ca-test.lock`, taken FIRST. Both are taken
# with an atomic mkdir and carry an `owner` file:
#     stream=<s> pid=<windows pid of the real holder> time=<iso> task=<label> token=<id> root=<checkout>
#
#   slots.sh run [-n N] [--lock] [--task LABEL] [--wait SEC] -- <command...>
#       take, run the command, and always release (trap on EXIT/INT/TERM); exit code of the command. The holder PID is
#       this script (alive while the command runs); a unique token per run, so parallel runs never free each other.
#   slots.sh take [-n N | --tasks A,B,..] [--lock] [--task LABEL] [--token ID] [--pid WINPID] [--wait SEC]
#       N slots (default 1), the lock first with --lock, all or nothing. If not everything is free, frees what it got and
#       retries every 20 s for up to SEC seconds (default 0: one try; -1: no limit) - never waits holding anything.
#       --tasks: one slot per label, each with its own task (and token) - e.g. `--tasks server,client1` reserves a
#       server and its client at once (server.sh start --client), so nobody holds a server slot waiting for a client.
#       -n 0 --lock: the lock only (client.sh start with a reserved slot).
#       --pid: the process that holds them (default: the caller, i.e. the parent of this script) - it must live as
#       long as the hold, or `adopt` a longer-lived one. Exit 0 held, 2 busy, 3 the user is playing.
#   slots.sh release [--task LABEL] [--token ID] [--holder WINPID] [--lock-only]
#                                    free this stream's entries (of that task/token, held by that PID)
#   slots.sh adopt <winpid> [--task LABEL] [--token ID]          make <winpid> the holder (a server/client JVM)
#   slots.sh held [--task LABEL]     exit 0 when this stream holds a slot (of that task), and print it
#   slots.sh status                  lock, slots and the play lock with owner, age and whether the holder lives
#   slots.sh clean                   remove stale entries: holder PID dead and owner older than CA_STALE_MIN (20) min
#
# Refuses everything while `<lockdir>/ca-user-test.lock` exists (main puts it there while the user plays).
# Test the protocol in a scratch directory with CA_LOCK_DIR=<dir> (never touch the real play lock).
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

# A dead holder frees its entry after this many minutes: longer than a server (900 s) or client (600 s) start, during
# which the starting script holds the slots before its JVM adopts them.
STALE_MIN="${CA_STALE_MIN:-20}"
mkdir -p "$CA_LOCK_DIR" 2>/dev/null

ENTRIES=("$CA_MACHINE_LOCK" "$CA_LOCK_DIR/ca-jvm-slot-1" "$CA_LOCK_DIR/ca-jvm-slot-2" "$CA_LOCK_DIR/ca-jvm-slot-3")

owner_line() {
    echo "stream=$CA_STREAM pid=$OWNER_PID time=$(ca_now) task=${TASK:-manual} token=${TOKEN:-${TASK:-manual}} root=$(ca_winpath "$CA_ROOT")"
}

field() { sed -n "s/.* $2=\([^ ]*\).*/\1/p; s/^$2=\([^ ]*\).*/\1/p" "$1/owner" 2>/dev/null | head -1; }

# Ours = same stream and same checkout (and the same task/token when filtered).
is_ours() {
    [ -f "$1/owner" ] || return 1
    [ "$(field "$1" stream)" = "$CA_STREAM" ] && grep -qF "root=$(ca_winpath "$CA_ROOT")" "$1/owner" || return 1
    [ -z "$TASK_FILTER" ] || [ "$(field "$1" task)" = "$TASK_FILTER" ] || return 1
    [ -z "$TOKEN_FILTER" ] || [ "$(field "$1" token)" = "$TOKEN_FILTER" ] || return 1
    [ -z "$HOLDER_FILTER" ] || [ "$(field "$1" pid)" = "$HOLDER_FILTER" ]
}

age_min() {
    local f="$1/owner"; [ -f "$f" ] || f="$1"
    echo $(( ($(date +%s) - $(stat -c %Y "$f")) / 60 ))
}

# The holder PIDs (of the given entries) that are alive, one per line, then the line `ok`. Without `ok` (PowerShell
# failed) nothing is known and callers treat every holder as alive (posix: kill -0 and /proc, which cannot fail).
# CA_ALIVE_PIDS (self-test) replaces the lookup.
alive_pids() {
    local e pid pids=()
    if [ -n "${CA_ALIVE_PIDS+x}" ]; then printf '%s\n' $CA_ALIVE_PIDS ok; return; fi
    for e in "$@"; do
        pid="$(field "$e" pid)"; [[ "$pid" =~ ^[0-9]+$ ]] && pids+=("$pid")
    done
    [ ${#pids[@]} = 0 ] && { echo ok; return; }
    if [ "$CA_OS" = posix ]; then
        for pid in "${pids[@]}"; do ca_pid_alive "$pid" && echo "$pid"; done
        echo ok
        return
    fi
    powershell -NoProfile -Command "Get-Process -Id $(IFS=,; echo "${pids[*]}") -ErrorAction SilentlyContinue | ForEach-Object { \$_.Id }; 'ok'" 2>/dev/null | tr -d '\r'
}

# holder_alive <entry> <alive list>: 0 alive or unknown, 1 dead (or no PID in the owner file).
holder_alive() {
    local pid
    grep -qx ok <<< "$2" || return 0
    pid="$(field "$1" pid)"
    [ -n "$pid" ] && grep -qx "$pid" <<< "$2"
}

clean() {
    local e old=() alive
    for e in "${ENTRIES[@]}"; do
        [ -d "$e" ] && [ "$(age_min "$e")" -ge "$STALE_MIN" ] && old+=("$e")
    done
    [ ${#old[@]} = 0 ] && return 0
    alive="$(alive_pids "${old[@]}")"
    for e in "${old[@]}"; do
        holder_alive "$e" "$alive" && continue
        echo "stale (>= $STALE_MIN min, holder pid $(field "$e" pid) dead): $e - $(cat "$e/owner" 2>/dev/null)"
        rm -rf "$e"
    done
}

release() {
    local e
    for e in "${ENTRIES[@]}"; do
        [ "$LOCK_ONLY" = 1 ] && [ "$e" != "$CA_MACHINE_LOCK" ] && continue
        if is_ours "$e"; then rm -rf "$e"; echo "released $(basename "$e")"; fi
    done
}

# One attempt: lock first (if asked), then N slots (with --tasks: the i-th slot gets label i); all or nothing.
attempt() {
    local got=() s
    ca_user_check
    if [ "$USE_LOCK" = 1 ]; then
        mkdir "$CA_MACHINE_LOCK" 2>/dev/null || return 2
        slot_owner 0 > "$CA_MACHINE_LOCK/owner"
    fi
    for s in 1 2 3; do
        [ ${#got[@]} -ge "$N" ] && break
        if mkdir "$CA_LOCK_DIR/ca-jvm-slot-$s" 2>/dev/null; then
            slot_owner ${#got[@]} > "$CA_LOCK_DIR/ca-jvm-slot-$s/owner"; got+=("$s")
        fi
    done
    if [ ${#got[@]} -lt "$N" ]; then
        for s in "${got[@]}"; do rm -rf "$CA_LOCK_DIR/ca-jvm-slot-$s"; done
        [ "$USE_LOCK" = 1 ] && rm -rf "$CA_MACHINE_LOCK"
        return 2
    fi
    echo "held: slots ${got[*]:-none}$([ "$USE_LOCK" = 1 ] && echo ' + lock') (stream $CA_STREAM, task ${TASKS:-${TASK:-manual}}, holder pid $OWNER_PID) at $(date +%T)"
}

# Owner line of the i-th slot taken (the lock uses 0): with --tasks its own label as task and token.
slot_owner() {
    if [ -n "$TASKS" ]; then
        local labels; IFS=, read -ra labels <<< "$TASKS"
        TASK="${labels[$1]}" TOKEN="${TOKEN_ARG:-${labels[$1]}}" owner_line
    else
        owner_line
    fi
}

take() {
    local start rc said=""
    start=$(date +%s)
    clean >/dev/null
    while true; do
        attempt; rc=$?
        [ $rc = 0 ] && return 0
        if [ "$WAIT" -ge 0 ] && [ $(( $(date +%s) - start + 20 )) -gt "$WAIT" ]; then
            echo "busy: could not take $N slot(s)$([ "$USE_LOCK" = 1 ] && echo ' + lock') within ${WAIT}s" >&2
            status >&2
            return 2
        fi
        [ -z "$said" ] && { said=1; echo "waiting for $N slot(s)$([ "$USE_LOCK" = 1 ] && echo ' + lock') (holding nothing, up to ${WAIT}s)" >&2; status >&2; }
        sleep 20
        clean >/dev/null
    done
}

status() {
    local e present=() alive
    [ -e "$CA_USER_LOCK" ] && echo "PLAY LOCK $CA_USER_LOCK (age $(age_min "$CA_USER_LOCK") min)"
    for e in "${ENTRIES[@]}"; do [ -d "$e" ] && present+=("$e"); done
    [ ${#present[@]} = 0 ] && return 0
    alive="$(alive_pids "${present[@]}")"
    for e in "${present[@]}"; do
        echo "$(basename "$e") (age $(age_min "$e") min$(holder_alive "$e" "$alive" || echo ', holder dead')): $(cat "$e/owner" 2>/dev/null)"
    done
    return 0
}

held() {
    local e found=1
    for e in "${ENTRIES[@]}"; do
        [ "$e" = "$CA_MACHINE_LOCK" ] && continue
        if is_ours "$e"; then echo "$(basename "$e"): $(cat "$e/owner")"; found=0; fi
    done
    return $found
}

cmd="$1"; shift
N=1; USE_LOCK=0; TASK=""; TOKEN=""; WAIT=0; TASK_FILTER=""; TOKEN_FILTER=""; OWNER_PID=""; LOCK_ONLY=0
TASKS=""; TOKEN_ARG=""; HOLDER_FILTER=""
while [ $# -gt 0 ]; do
    case "$1" in
        -n) N="$2"; shift 2 ;;
        --tasks) TASKS="${2// /_}"; shift 2 ;;
        --holder) HOLDER_FILTER="$2"; shift 2 ;;
        --lock) USE_LOCK=1; shift ;;
        --lock-only) LOCK_ONLY=1; shift ;;
        --task) TASK="${2// /_}"; TASK_FILTER="$TASK"; shift 2 ;;
        --token) TOKEN="${2// /_}"; TOKEN_FILTER="$TOKEN"; TOKEN_ARG="$TOKEN"; shift 2 ;;
        --pid) OWNER_PID="$2"; shift 2 ;;
        --wait) WAIT="$2"; shift 2 ;;
        --) shift; break ;;
        *) if [ "$cmd" = adopt ] && [ -z "$OWNER_PID" ]; then OWNER_PID="$1"; shift; else ca_die "unknown argument $1"; fi ;;
    esac
done
if [ -n "$TASKS" ]; then
    [[ "$TASKS" =~ ^[A-Za-z0-9._-]+(,[A-Za-z0-9._-]+)*$ ]] || ca_die "--tasks must be labels separated by commas"
    N=$(( $(tr -cd , <<< "$TASKS" | wc -c) + 1 )); TASK="${TASKS%%,*}"
fi
[[ "$N" =~ ^[0-3]$ ]] || ca_die "-n must be 0..3"
[ "$N" -ge 1 ] || [ "$USE_LOCK" = 1 ] || ca_die "-n 0 only with --lock"

case "$cmd" in
    take)
        # The caller holds the entries (this script ends right away).
        OWNER_PID="${OWNER_PID:-$(cat "/proc/$PPID/winpid" 2>/dev/null || echo "$PPID")}"
        take ;;
    release) release ;;
    held) held ;;
    clean) clean ;;
    status) status ;;
    adopt)
        [ -n "$OWNER_PID" ] || ca_die "adopt needs a Windows PID"
        for e in "${ENTRIES[@]}"; do
            if is_ours "$e"; then
                TASK="$(field "$e" task)" TOKEN="$(field "$e" token)" owner_line > "$e/owner"
                echo "adopted $(basename "$e") by pid $OWNER_PID"
            fi
        done ;;
    run)
        [ $# -gt 0 ] || ca_die "run needs -- <command>"
        OWNER_PID="$(ca_winpid)"
        TASK="${TASK:-$(basename "$1")}"; TOKEN="run-$OWNER_PID-$RANDOM"; TOKEN_FILTER="$TOKEN"; TOKEN_ARG="$TOKEN"; TASK_FILTER=""
        take || exit $?
        trap 'release >/dev/null' EXIT
        trap 'exit 130' INT TERM
        "$@"
        exit $? ;;
    *) sed -n '2,/^source/p' "$0" | grep '^#' | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
