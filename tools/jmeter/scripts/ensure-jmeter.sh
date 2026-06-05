#!/usr/bin/env bash
#
# Idempotently provision a pinned Apache JMeter into tools/jmeter/.runtime/.
# Prints the absolute path to the jmeter binary on stdout (logs go to stderr),
# so callers can do: JMETER_BIN="$(scripts/ensure-jmeter.sh)".
#
set -euo pipefail

if [ -z "${BASH_VERSION:-}" ]; then
  echo "ERROR: this script requires bash. On Windows, invoke via Git Bash: bash ensure-jmeter.sh" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JMETER_TOOL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"   # tools/jmeter
RUNTIME_DIR="$JMETER_TOOL_DIR/.runtime"
VERSION_FILE="$JMETER_TOOL_DIR/lib/jmeter-version.properties"

# Load JMETER_VERSION / JMETER_URL / JMETER_SHA512
JMETER_VERSION="$(grep '^JMETER_VERSION=' "$VERSION_FILE" | cut -d= -f2)"
JMETER_URL="$(grep '^JMETER_URL='     "$VERSION_FILE" | cut -d= -f2)"
JMETER_SHA512="$(grep '^JMETER_SHA512=' "$VERSION_FILE" | cut -d= -f2)"

JMETER_DIR="$RUNTIME_DIR/apache-jmeter-${JMETER_VERSION}"
JMETER_BIN="$JMETER_DIR/bin/jmeter"

# 1. Already provisioned -> return cached binary.
if [ -x "$JMETER_BIN" ]; then
  echo "$JMETER_BIN"
  exit 0
fi

echo "Provisioning Apache JMeter ${JMETER_VERSION} (first run only)..." >&2
mkdir -p "$RUNTIME_DIR"

TMP_ZIP="$(mktemp "${TMPDIR:-/tmp}/jmeter-XXXXXX.zip")"
trap 'rm -f "$TMP_ZIP"' EXIT

# 2. Download.
echo "Downloading $JMETER_URL" >&2
curl -fSL --max-redirs 2 --max-time 120 --retry 3 --retry-delay 5 -o "$TMP_ZIP" "$JMETER_URL"

# 3. Verify SHA-512 (abort on mismatch).
echo "Verifying SHA-512..." >&2
if command -v sha512sum >/dev/null 2>&1; then
  actual_sha="$(sha512sum "$TMP_ZIP" | awk '{print $1}')"
elif command -v shasum >/dev/null 2>&1; then
  actual_sha="$(shasum -a 512 "$TMP_ZIP" | awk '{print $1}')"
else
  echo "ERROR: neither sha512sum nor shasum found; cannot verify download integrity" >&2
  exit 1
fi
if [ "$actual_sha" != "$JMETER_SHA512" ]; then
  echo "ERROR: SHA-512 mismatch for $JMETER_URL" >&2
  echo "  expected: $JMETER_SHA512" >&2
  echo "  actual:   $actual_sha" >&2
  exit 1
fi

# 4. Unzip (prefer unzip; fall back to the JDK's jar since Java 25 is required anyway).
echo "Extracting into $RUNTIME_DIR ..." >&2
if command -v unzip >/dev/null 2>&1; then
  unzip -q "$TMP_ZIP" -d "$RUNTIME_DIR"
else
  TMP_ZIP_NATIVE="$(cygpath -w "$TMP_ZIP" 2>/dev/null || echo "$TMP_ZIP")"
  ( cd "$RUNTIME_DIR" && jar xf "$TMP_ZIP_NATIVE" )
fi

# jar/unzip may drop the executable bit on the launcher scripts.
chmod +x "$JMETER_DIR/bin/jmeter" 2>/dev/null || true  # on FAT/NTFS the bit may not apply; line below validates

if [ ! -x "$JMETER_BIN" ]; then
  echo "ERROR: expected binary not found/executable: $JMETER_BIN" >&2
  exit 1
fi

echo "$JMETER_BIN"
