#!/usr/bin/env bash
#
# Sourced by run-stress.sh to point JMeter at a Groovy-compatible JVM.
#
# JMeter 5.6.3 bundles Groovy 3.0.20, whose bytecode reader cannot parse Java 25
# (class-file major version 69): a plan with JSR223/Groovy elements dies with
# "Unsupported class file major version 69". JMeter 5.6.3 is officially supported
# on Java 17, so the stress plan must run on Java 17 (or 21), independently of the
# project's Java 25. (The regression plan has no Groovy and runs on any Java, so
# run-regression.sh does NOT source this.)
#
# Resolution order: JMETER_JAVA_HOME (dedicated override) then JAVA_HOME. This lets
# you keep JAVA_HOME on Java 25 for the app and point only JMeter at a 17/21 JDK:
#   export JMETER_JAVA_HOME='C:\Program Files\Java\jdk-17.0.12'
#
# Exits non-zero with guidance if the resolved JVM is newer than Java 21, instead
# of letting JMeter crash with the cryptic Groovy stack trace. Exports JAVA_HOME
# (unix-style) so the bash-invoked JMeter launcher uses the resolved JVM.

_raw_home="${JMETER_JAVA_HOME:-${JAVA_HOME:-}}"
if [ -z "$_raw_home" ]; then
  echo "ERROR: set JMETER_JAVA_HOME (or JAVA_HOME) to a Java 17 (recommended) or 21 JDK." >&2
  echo "       JMeter 5.6.3 (Groovy 3.0.20) cannot run the stress plan on Java > 21." >&2
  echo "       e.g.: export JMETER_JAVA_HOME='C:\\Program Files\\Java\\jdk-17.0.12'" >&2
  exit 1
fi

# Normalize to a unix-style path for the bash-invoked launcher (Git Bash on Windows).
if command -v cygpath >/dev/null 2>&1; then
  _java_home="$(cygpath -u "$_raw_home" 2>/dev/null || printf '%s' "$_raw_home")"
else
  _java_home="$_raw_home"
fi

_java_bin="$_java_home/bin/java"
if [ ! -x "$_java_bin" ] && [ ! -x "$_java_bin.exe" ]; then
  echo "ERROR: no java executable under $_raw_home (expected $_java_bin)." >&2
  exit 1
fi

_ver_out="$("$_java_bin" -version 2>&1)" || { echo "ERROR: '$_java_bin -version' failed." >&2; exit 1; }
_first_line="${_ver_out%%$'\n'*}"
_ver_raw="$(printf '%s' "$_first_line" | sed -E 's/.*"([^"]+)".*/\1/')"
case "$_ver_raw" in
  1.*) _ver_major="$(printf '%s' "$_ver_raw" | cut -d. -f2)" ;;   # 1.8.0_201 -> 8
  *)   _ver_major="$(printf '%s' "$_ver_raw" | cut -d. -f1)" ;;   # 17.0.12   -> 17
esac

case "$_ver_major" in
  ''|*[!0-9]*)
    echo "ERROR: could not determine Java major version from '$_ver_raw'." >&2
    exit 1 ;;
esac

if [ "$_ver_major" -gt 21 ]; then
  echo "ERROR: JMeter would run under Java $_ver_major ($_raw_home)." >&2
  echo "       JMeter 5.6.3 bundles Groovy 3.0.20, which cannot run on Java > 21 -" >&2
  echo "       the stress plan's JSR223/Groovy scripts fail with" >&2
  echo "       'Unsupported class file major version'. Point JMeter at a Java 17" >&2
  echo "       (recommended) or 21 JDK without changing your project JAVA_HOME:" >&2
  echo "         export JMETER_JAVA_HOME='C:\\Program Files\\Java\\jdk-17.0.12'" >&2
  exit 1
fi

export JAVA_HOME="$_java_home"
echo "Using Java $_ver_major for JMeter: $_java_home" >&2