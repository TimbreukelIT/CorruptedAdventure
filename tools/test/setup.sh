#!/bin/bash
# One-time setup of a checkout for the test tools: setup.sh <stream> <port-n>
# Writes build/test.conf (CA_STREAM, CA_PORT_N; not in git) and prepares run/ (waves.md §4): eula.txt, a
# server.properties with the corrupted world preset, seed 20260930, the stream's ports (server 25570+n, RCON 25580+n,
# RCON off until server.sh starts a server), and options.txt for the dev clients (no onboarding, no multiplayer warning).
# Existing files are updated key by key, never replaced. No JVM is started.
[ $# -eq 2 ] || { sed -n '2,6p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }
export CA_STREAM="$1" CA_PORT_N="$2"
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
ca_need_port

printf 'CA_STREAM=%s\nCA_PORT_N=%s\n' "$CA_STREAM" "$CA_PORT_N" > "$CA_ROOT/build/test.conf"
ca_prepare_run "$CA_ROOT" setup
echo "configured $CA_ROOT: stream $CA_STREAM, server port $CA_SERVER_PORT, RCON $CA_RCON_PORT (build/test.conf)"
