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
#   slots.sh take [-n N] [--lock] [--task LABEL] [--token ID] [--pid WINPID] [--wait SEC]
#       N slots (default 1), the lock first with --lock, all or nothing. If not everything is free, frees what it got and
#       retries every 20 s for up to SEC seconds (default 0: one try; -1: no limit) - never waits holding anything.
#       --pid: the process that holds them (default: the caller, i.e. the parent of this script) - it must live as
#       long as the hold, or `adopt` a longer-lived one. Exit 0 held, 2 busy, 3 the user is playing.
#   slots.sh release [--task LABEL] [--token ID] [--lock-only]   free this stream's entries (of that task/token)
#   slots.sh adopt <winpid> [--task LABEL] [--token ID]          make <winpid> the holder (a server/client JVM)
#   slots.sh status                  lock, slots and the play lock with owner and age
#   slots.sh clean                   remove stale entries: owner older than 60 min AND its holder PID dead
#
# Refuses everything while `<lockdir>/ca-user-test.lock` exists (main puts it there while the user plays).
# Test the protocol in a scratch directory with CA_LOCK_DIR=<dir> (never touch the real play lock).
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

STALE_MIN=60
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
    [ -z "$TOKEN_FILTER" ] || [ "$(field "$1" token)" = "$TOKEN_FILTER" ]
}

age_min() {
    local f="$1/owner"; [ -f "$f" ] || f="$1"
    echo $(( ($(date +%s) - $(stat -c %Y "$f")) / 60 ))
}

clean() {
    local e pid
    for e in "${ENTRIES[@]}"; do
        [ -d "$e" ] || continue
        [ "$(age_min "$e")" -gt "$STALE_MIN" ] || continue
        pid="$(field "$e" pid)"
        if [ -n "$pid" ] && ca_pid_alive "$pid"; then continue; fi
        echo "stale (> $STALE_MIN min, pid ${pid:-?} dead): $e — $(cat "$e/owner" 2>/dev/null)"
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

# One attempt: lock first (if asked), then N slots; all or nothing.
attempt() {
    local got=() s
    ca_user_check
    if [ "$USE_LOCK" = 1 ]; then
        mkdir "$CA_MACHINE_LOCK" 2>/dev/null || return 2
        owner_line > "$CA_MACHINE_LOCK/owner"
    fi
    for s in 1 2 3; do
        [ ${#got[@]} -ge "$N" ] && break
        if mkdir "$CA_LOCK_DIR/ca-jvm-slot-$s" 2>/dev/null; then
            owner_line > "$CA_LOCK_DIR/ca-jvm-slot-$s/owner"; got+=("$s")
        fi
    done
    if [ ${#got[@]} -lt "$N" ]; then
        for s in "${got[@]}"; do rm -rf "$CA_LOCK_DIR/ca-jvm-slot-$s"; done
        [ "$USE_LOCK" = 1 ] && rm -rf "$CA_MACHINE_LOCK"
        return 2
    fi
    echo "held: slots ${got[*]}$([ "$USE_LOCK" = 1 ] && echo ' + lock') (stream $CA_STREAM, task ${TASK:-manual}, holder pid $OWNER_PID) at $(date +%T)"
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
    local e
    [ -e "$CA_USER_LOCK" ] && echo "PLAY LOCK $CA_USER_LOCK (age $(age_min "$CA_USER_LOCK") min)"
    for e in "${ENTRIES[@]}"; do
        [ -d "$e" ] && echo "$(basename "$e") (age $(age_min "$e") min): $(cat "$e/owner" 2>/dev/null)"
    done
    return 0
}

cmd="$1"; shift
N=1; USE_LOCK=0; TASK=""; TOKEN=""; WAIT=0; TASK_FILTER=""; TOKEN_FILTER=""; OWNER_PID=""; LOCK_ONLY=0
while [ $# -gt 0 ]; do
    case "$1" in
        -n) N="$2"; shift 2 ;;
        --lock) USE_LOCK=1; shift ;;
        --lock-only) LOCK_ONLY=1; shift ;;
        --task) TASK="${2// /_}"; TASK_FILTER="$TASK"; shift 2 ;;
        --token) TOKEN="${2// /_}"; TOKEN_FILTER="$TOKEN"; shift 2 ;;
        --pid) OWNER_PID="$2"; shift 2 ;;
        --wait) WAIT="$2"; shift 2 ;;
        --) shift; break ;;
        *) if [ "$cmd" = adopt ] && [ -z "$OWNER_PID" ]; then OWNER_PID="$1"; shift; else ca_die "unknown argument $1"; fi ;;
    esac
done
[ "$N" -ge 1 ] && [ "$N" -le 3 ] || ca_die "-n must be 1..3"

case "$cmd" in
    take)
        # The caller holds the entries (this script ends right away).
        OWNER_PID="${OWNER_PID:-$(cat "/proc/$PPID/winpid" 2>/dev/null || echo "$PPID")}"
        take ;;
    release) release ;;
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
        TASK="${TASK:-$(basename "$1")}"; TOKEN="run-$OWNER_PID-$RANDOM"; TOKEN_FILTER="$TOKEN"; TASK_FILTER=""
        take || exit $?
        trap 'release >/dev/null' EXIT
        trap 'exit 130' INT TERM
        "$@"
        exit $? ;;
    *) sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
