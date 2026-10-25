#!/bin/bash
# Shared settings and helpers for tools/test/*.sh (sourced, not run). Everything is relative to the repo root of the
# checkout these tools live in, so the same scripts work from the main checkout and from every worktree.
#
# Settings (environment overrides build/test.conf, written by `setup.sh`):
#   CA_STREAM    stream name for owner files and build/<stream>.pids (default: branch name after the last '/')
#   CA_PORT_N    port number n: server 25570+n, RCON 25580+n (waves.md §4)
#   CA_LOCK_DIR  directory of the machine lock, the JVM slots and the user's play lock (default C:/Temp on Windows,
#                ${TMPDIR:-/tmp}/ca-test elsewhere)
#   CA_WT        worktree to run Gradle/servers in (default: this checkout; `--worktree` on server.sh/world.sh)
#
# CA_OS: `windows` under Git Bash/MSYS/Cygwin (PowerShell for processes, cygpath for paths, Windows PIDs), else `posix`
# (Linux, e.g. the cloud container: kill/proc for processes, paths as they are, the shell's own PIDs).

case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) CA_OS=windows ;;
    *) CA_OS=posix ;;
esac
CA_TOOLS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CA_ROOT="$(cd "$CA_TOOLS/../.." && pwd)"
if [ -f "$CA_ROOT/build/test.conf" ]; then
    while IFS='=' read -r key value; do
        case "$key" in
            CA_STREAM|CA_PORT_N|CA_LOCK_DIR) [ -z "${!key}" ] && export "$key=$value" ;;
        esac
    done < "$CA_ROOT/build/test.conf"
fi
CA_STREAM="${CA_STREAM:-$(git -C "$CA_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null | sed 's#.*/##')}"
CA_STREAM="${CA_STREAM:-main}"
if [ "$CA_OS" = windows ]; then
    CA_LOCK_DIR="${CA_LOCK_DIR:-C:/Temp}"
else
    CA_LOCK_DIR="${CA_LOCK_DIR:-${TMPDIR:-/tmp}/ca-test}"
fi
CA_WT="${CA_WT:-$CA_ROOT}"
CA_OUT="$CA_ROOT/build/test"            # logs, state and scan output of this checkout (on P:, never C:)
CA_PIDS="$CA_ROOT/build/$CA_STREAM.pids"
CA_SEED=20260930
CA_USER_LOCK="$CA_LOCK_DIR/ca-user-test.lock"
CA_MACHINE_LOCK="$CA_LOCK_DIR/ca-test.lock"
CA_SLOTS="${CA_SLOTS:-5}"                # JVM slots on the machine (2026-10-25: 3 -> 5; ~12 GB RAM and CPU were idle)
CA_PY="python"
CA_EXE=".exe"                           # suffix of JDK tools (jfr.exe)
CA_GRADLEW=(./gradlew)
if [ "$CA_OS" = posix ]; then
    command -v python >/dev/null 2>&1 || CA_PY="python3"
    CA_EXE=""
    # A checkout made on Windows may have gradlew without the executable bit.
    CA_GRADLEW=(bash ./gradlew)
    # No cygpath outside Windows: paths are already what the JVM and Python see (`cygpath -m|-w <path>` -> <path>).
    command -v cygpath >/dev/null 2>&1 || cygpath() { printf '%s\n' "${@: -1}"; }
fi
mkdir -p "$CA_OUT"

ca_die() { echo "error: $*" >&2; exit "${CA_EXIT:-1}"; }
ca_now() { date '+%Y-%m-%dT%H:%M:%S%z'; }
ca_log() { echo "[$(date +%T)] $*"; }

# Exit 3 when the user is playing: no JVM may start (agent-rules-golf3).
ca_user_check() {
    if [ -e "$CA_USER_LOCK" ]; then
        echo "refused: $CA_USER_LOCK exists (the user is testing); start no JVM, tell main and wait idle" >&2
        exit 3
    fi
}

ca_need_port() {
    [ -n "$CA_PORT_N" ] || ca_die "CA_PORT_N is not set (run tools/test/setup.sh <stream> <n> once, or export CA_PORT_N)"
    CA_SERVER_PORT=$((25570 + CA_PORT_N))
    CA_RCON_PORT=$((25580 + CA_PORT_N))
}

# Windows path of a directory (for matching JVM command lines).
ca_winpath() { cygpath -w "$1" 2>/dev/null || echo "$1"; }

# Windows PID of this shell (the owner PID in slot files; Git Bash PIDs are not visible to Windows tools).
ca_winpid() { cat /proc/$$/winpid 2>/dev/null || echo $$; }

ca_pid_alive() {
    local pid="$1"
    [ -n "$pid" ] || return 1
    if [ "$CA_OS" = posix ]; then
        [[ "$pid" =~ ^[0-9]+$ ]] && { kill -0 "$pid" 2>/dev/null || [ -d "/proc/$pid" ]; }
        return
    fi
    powershell -NoProfile -Command "if (Get-Process -Id $pid -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >/dev/null 2>&1
}

# Java PIDs whose command line points at <worktree>\build\moddev\<kind>RunVmArgs (kind: server, client, client2).
# Exact worktree prefix, so the main checkout never matches a worktree's JVM (or the other way round).
ca_java_pids() {
    local wt kind f p
    if [ "$CA_OS" = posix ]; then
        # The JVM gets `@<canonical project dir>/build/moddev/<kind>RunVmArgs.txt`; the comm check drops grep itself.
        wt="$(cd "$1" 2>/dev/null && pwd -P)" || return 0
        kind="$2"
        for f in $(grep -lsaF -- "$wt/build/moddev/${kind}RunVmArgs" /proc/[0-9]*/cmdline); do
            p="${f#/proc/}"; p="${p%/cmdline}"
            [ "$(cat "/proc/$p/comm" 2>/dev/null)" = java ] && echo "$p"
        done
        return 0
    fi
    wt="$(ca_winpath "$1")"; kind="$2"
    powershell -NoProfile -Command "\$w = '$wt'.TrimEnd('\\') + '\\build\\moddev\\${kind}RunVmArgs'; Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -and \$_.CommandLine.Replace('\\\\', '\\').Replace('/', '\\').ToLower().Contains(\$w.ToLower()) } | ForEach-Object { \$_.ProcessId }" | tr -d '\r'
}

# Whether <pid> is still the <kind> JVM of <worktree> (guards against PID reuse before stopping or screenshotting).
ca_is_own_java() {
    local pid="$1" wt="$2" kind="$3"
    [ -n "$pid" ] && ca_java_pids "$wt" "$kind" | grep -qx "$pid"
}

# PID of this stream's server JVM while it runs (from build/test/server.state), else nothing.
ca_server_pid() {
    local pid wt
    pid="$(ca_state_get server PID)"; wt="$(ca_state_get server WT)"
    [ -n "$pid" ] && ca_is_own_java "$pid" "$wt" server && echo "$pid"
}

ca_stop_pid() {
    if [ "$CA_OS" = posix ]; then
        kill -9 "$1" 2>/dev/null
        return 0
    fi
    powershell -NoProfile -Command "Stop-Process -Id $1 -Force -ErrorAction SilentlyContinue" >/dev/null 2>&1
}

# Stop a background Gradle job we started ourselves (Git Bash job PID): the Windows process behind it (the gradlew
# java wrapper and, with --no-daemon, the build JVM under it) as a tree. Only for our own `$!`.
ca_stop_job() {
    local job="$1" wp tree
    [ -n "$job" ] || return 0
    if [ "$CA_OS" = posix ]; then
        tree="$(ca_descendants "$job")"
        kill "$job" 2>/dev/null
        [ -n "$tree" ] && kill -9 $tree 2>/dev/null
        return 0
    fi
    wp="$(cat "/proc/$job/winpid" 2>/dev/null)"
    kill "$job" 2>/dev/null
    [ -n "$wp" ] && taskkill //PID "$wp" //T //F >/dev/null 2>&1
    return 0
}

# Every descendant PID of <pid> (posix; for ca_stop_job), children before grandchildren.
ca_descendants() {
    local c
    for c in $(pgrep -P "$1" 2>/dev/null); do
        echo "$c"
        ca_descendants "$c"
    done
}

# Executable of a running process (the server's java, for jfr next to it), as a mixed path on Windows.
ca_pid_exe() {
    if [ "$CA_OS" = posix ]; then
        readlink "/proc/$1/exe"
        return
    fi
    cygpath -m "$(powershell -NoProfile -Command "(Get-CimInstance Win32_Process -Filter 'ProcessId=$1').ExecutablePath" | tr -d '\r')"
}

ca_record_pid() { echo "$1 $2 $(ca_now)" >> "$CA_PIDS"; }

# key=value in a .properties / options file: replace the line or append it.
ca_setprop() {
    local file="$1" key="$2" value="$3" sep="${4:-=}"
    touch "$file"
    if grep -q "^${key}${sep}" "$file"; then
        local esc
        esc="$(printf '%s' "$value" | sed 's/[&|\\]/\\&/g')"
        sed -i "s|^${key}${sep}.*|${key}${sep}${esc}|" "$file"
    else
        printf '%s%s%s\n' "$key" "$sep" "$value" >> "$file"
    fi
}

# State files: KEY=VALUE lines in build/test/<name>.state
ca_state_get() { [ -f "$CA_OUT/$1.state" ] && sed -n "s/^$2=//p" "$CA_OUT/$1.state" | tail -1; }

# Prepare <worktree>/run for a server on <level> (eula, server.properties) and the dev clients (options.txt).
# Keys are set one by one; the rest of an existing file stays. RCON stays off here; server.sh turns it on.
ca_prepare_run() {
    local wt="$1" level="$2" props opts
    ca_need_port
    mkdir -p "$wt/run/client2"
    echo "eula=true" > "$wt/run/eula.txt"
    props="$wt/run/server.properties"
    ca_setprop "$props" level-name "$level"
    ca_setprop "$props" level-seed "$CA_SEED"
    ca_setprop "$props" level-type 'corruptedadventure\:corrupted_world'
    ca_setprop "$props" server-port "$CA_SERVER_PORT"
    ca_setprop "$props" server-ip 127.0.0.1
    ca_setprop "$props" rcon.port "$CA_RCON_PORT"
    ca_setprop "$props" enable-rcon false
    ca_setprop "$props" online-mode false
    ca_setprop "$props" max-tick-time -1
    ca_setprop "$props" spawn-protection 0
    ca_prepare_options "$wt"
}

ca_prepare_options() {
    local wt="$1" opts
    mkdir -p "$wt/run/client2"
    for opts in "$wt/run/options.txt" "$wt/run/client2/options.txt"; do
        # A new options file also gets pauseOnLostFocus:false (a covered window keeps rendering without the pause
        # menu); an existing one (maybe the user's own) only gets the two keys the protocol needs.
        [ -f "$opts" ] || printf 'pauseOnLostFocus:false\n' > "$opts"
        ca_setprop "$opts" onboardAccessibility false ":"
        ca_setprop "$opts" skipMultiplayerWarning true ":"
    done
}
