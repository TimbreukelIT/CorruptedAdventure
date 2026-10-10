#!/bin/bash
# Machine lock + JVM slots (waves.md §4, agent-rules-golf3). At most 3 Gradle/Minecraft JVMs on the machine: every
# build, runData, runServer and runClient holds a slot `<lockdir>/ca-jvm-slot-{1,2,3}`; machine-wide things (runClient
# with quick play, JFR, ffmpeg) also hold the lock `<lockdir>/ca-test.lock`, taken FIRST. Both are taken with an atomic
# mkdir and carry an `owner` file (stream, Windows PID, time, task, checkout).
#
#   slots.sh take [-n N] [--lock] [--task TEXT] [--wait SEC]
#       N slots (default 1), the lock first with --lock, all or nothing. If not everything is free, frees what it got and
#       retries every 20 s for up to SEC seconds (default 0: one try; -1: no limit) - never waits holding anything.
#       Exit 0 held, 2 busy, 3 the user is playing.
#   slots.sh release [--task TEXT]   free what this stream holds (only entries of that task with --task)
#   slots.sh run [-n N] [--lock] [--task TEXT] [--wait SEC] -- <command...>
#       take, run the command, and always release (trap on EXIT/INT/TERM); exit code of the command
#   slots.sh adopt <winpid> [--task TEXT]   make <winpid> the owner PID (a long-running server/client JVM)
#   slots.sh status                  lock, slots and the play lock with owner and age
#   slots.sh clean                   remove stale entries: owner older than 60 min AND its PID dead
#
# Refuses everything while `<lockdir>/ca-user-test.lock` exists (main puts it there while the user plays).
# Test the protocol in a scratch directory with CA_LOCK_DIR=<dir> (never touch the real play lock).
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

STALE_MIN=60
mkdir -p "$CA_LOCK_DIR" 2>/dev/null

ENTRIES=("$CA_MACHINE_LOCK" "$CA_LOCK_DIR/ca-jvm-slot-1" "$CA_LOCK_DIR/ca-jvm-slot-2" "$CA_LOCK_DIR/ca-jvm-slot-3")

owner_line() { echo "stream=$CA_STREAM pid=${OWNER_PID:-$(ca_winpid)} time=$(ca_now) task=${TASK:-manual} root=$(ca_winpath "$CA_ROOT")"; }

# Ours = same stream and same checkout (and the same task with --task).
is_ours() {
    local f="$1/owner"
    [ -f "$f" ] || return 1
    grep -q "stream=$CA_STREAM " "$f" && grep -qF "root=$(ca_winpath "$CA_ROOT")" "$f" || return 1
    [ -z "$TASK_FILTER" ] || grep -qF "task=$TASK_FILTER root=" "$f"
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
        pid="$(sed -n 's/.*pid=\([0-9]*\).*/\1/p' "$e/owner" 2>/dev/null)"
        if [ -n "$pid" ] && ca_pid_alive "$pid"; then continue; fi
        echo "stale (> $STALE_MIN min, pid ${pid:-?} dead): $e — $(cat "$e/owner" 2>/dev/null)"
        rm -rf "$e"
    done
}

release() {
    local e
    for e in "${ENTRIES[@]}"; do
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
    echo "held: slots ${got[*]}$([ "$USE_LOCK" = 1 ] && echo ' + lock') (stream $CA_STREAM, task ${TASK:-manual}) at $(date +%T)"
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
N=1; USE_LOCK=0; TASK=""; WAIT=0; TASK_FILTER=""; OWNER_PID=""
while [ $# -gt 0 ]; do
    case "$1" in
        -n) N="$2"; shift 2 ;;
        --lock) USE_LOCK=1; shift ;;
        --task) TASK="$2"; TASK_FILTER="$2"; shift 2 ;;
        --wait) WAIT="$2"; shift 2 ;;
        --) shift; break ;;
        *) if [ "$cmd" = adopt ] && [ -z "$OWNER_PID" ]; then OWNER_PID="$1"; shift; else ca_die "unknown argument $1"; fi ;;
    esac
done
[ "$N" -ge 1 ] && [ "$N" -le 3 ] || ca_die "-n must be 1..3"

case "$cmd" in
    take) take ;;
    release) release ;;
    clean) clean ;;
    status) status ;;
    adopt)
        [ -n "$OWNER_PID" ] || ca_die "adopt needs a Windows PID"
        for e in "${ENTRIES[@]}"; do
            if is_ours "$e"; then
                t="$(sed -n 's/.* task=\(.*\) root=.*/\1/p' "$e/owner")"
                TASK="$t" owner_line > "$e/owner"; echo "adopted $(basename "$e") by pid $OWNER_PID"
            fi
        done ;;
    run)
        [ $# -gt 0 ] || ca_die "run needs -- <command>"
        TASK="${TASK:-$1}"; TASK_FILTER="$TASK"
        take || exit $?
        trap 'release >/dev/null' EXIT
        trap 'exit 130' INT TERM
        "$@"
        exit $? ;;
    *) sed -n '2,24p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
