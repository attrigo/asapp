#!/usr/bin/env bash
#
# Sourced by run-stress.sh to point JMeter at a Groovy-compatible JVM.
#
# JMeter 5.6.3 is officially supported on Java 17 and bundles Groovy 3.0.20, whose bytecode reader cannot parse class files newer than Java 21 and,
# so plans must run on Java 17 (or 21).
#
# The JDK is passed in as $1; if empty it falls back to JAVA_HOME.
# This lets you keep JAVA_HOME on the project's JDK for the app and point only JMeter at a 17/21 JDK:
#   ./run-stress.sh --java-home '/path/to/jdk-17'
#
# Exits non-zero with guidance if the resolved JVM is newer than Java 21, instead of letting JMeter crash with the cryptic Groovy stack trace.
# Exports JAVA_HOME (unix-style) so the bash-invoked JMeter launcher uses the resolved JVM.

# All logic lives in a function so its intermediate variables stay `local` and do not leak into the sourcing shell (run-stress.sh).
# Because we are sourced (not subshelled), `exit` below still aborts the whole run, and `export JAVA_HOME` still reaches the caller.
_resolve_jmeter_java() {
  local cli_home="${1:-}"
  local raw_home java_home java_bin ver_out first_line ver_raw ver_major

  raw_home="${cli_home:-${JAVA_HOME:-}}"
  if [[ -z "$raw_home" ]]; then
    echo "ERROR: pass --java-home <path> (or set JAVA_HOME) to a Java 17 (recommended) or 21 JDK." >&2
    echo "       JMeter 5.6.3 (Groovy 3.0.20) cannot run the stress plan on Java > 21." >&2
    echo "       e.g.: ./run-stress.sh --java-home '/path/to/jdk-17'" >&2
    exit 1
  fi

  # Normalize to a unix-style path for the bash-invoked launcher (Git Bash on Windows).
  if command -v cygpath >/dev/null 2>&1; then
    java_home="$(cygpath -u "$raw_home" 2>/dev/null || printf '%s' "$raw_home")"
  else
    java_home="$raw_home"
  fi

  java_bin="$java_home/bin/java"
  if [[ ! -x "$java_bin" && ! -x "$java_bin.exe" ]]; then
    echo "ERROR: no java executable under $raw_home (expected $java_bin)." >&2
    exit 1
  fi

  # `java -version` prints to stderr; capture it, keep only the first line, then extract the
  # version from inside the double-quotes (e.g. openjdk version "17.0.12" ... -> 17.0.12).
  ver_out="$("$java_bin" -version 2>&1)" || { echo "ERROR: '$java_bin -version' failed." >&2; exit 1; }
  first_line="${ver_out%%$'\n'*}"
  ver_raw="$(printf '%s' "$first_line" | sed -E 's/.*"([^"]+)".*/\1/')"
  case "$ver_raw" in
    1.*) ver_major="$(printf '%s' "$ver_raw" | cut -d. -f2)" ;;   # 1.8.0_201 -> 8
    *)   ver_major="$(printf '%s' "$ver_raw" | cut -d. -f1)" ;;   # 17.0.12   -> 17
  esac

  case "$ver_major" in
    ''|*[!0-9]*)
      echo "ERROR: could not determine Java major version from '$ver_raw'." >&2
      exit 1 ;;
  esac

  if (( ver_major > 21 )); then
    echo "ERROR: JMeter would run under Java $ver_major ($raw_home)." >&2
    echo "       JMeter 5.6.3 bundles Groovy 3.0.20, which cannot run on Java > 21 -" >&2
    echo "       the stress plan's JSR223/Groovy scripts fail with" >&2
    echo "       'Unsupported class file major version'. Point JMeter at a Java 17" >&2
    echo "       (recommended) or 21 JDK without changing your project JAVA_HOME:" >&2
    echo "         ./run-stress.sh --java-home '/path/to/jdk-17'" >&2
    exit 1
  fi

  export JAVA_HOME="$java_home"
  log_step "Resolved Java $ver_major for JMeter: $java_home"
}

_resolve_jmeter_java "${1:-}"
unset -f _resolve_jmeter_java
