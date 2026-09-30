#!/bin/bash
# Smoke test for `S`: starts runServer, waits until it is up (or has failed), then kills the server JVM.
# Gradle does not forward stdin to the server, so `stop` cannot be piped in. Log: build/runServer.log
# Gradle then reports "BUILD FAILED" because the JVM was killed; judge the run by the log above that line.
LOG=build/runServer.log
: > "$LOG"
./gradlew runServer --console=plain > "$LOG" 2>&1 &
for i in $(seq 1 120); do
  sleep 3
  grep -qE 'Done \(|BUILD FAILED|Encountered an unexpected exception|Exception stopping the server' "$LOG" && break
done
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*fml.modFolders=corruptedadventure*' -and \$_.CommandLine -like '*serverRun*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }"
wait
