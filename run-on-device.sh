#!/usr/bin/env zsh
# Build, install, and launch Campus Desk (debug) on a connected Android device.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="${ANDROID_HOME:-$ROOT/.android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT/.gradle-home}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

APP_ID="com.derived.campusdesk"
MAIN_ACTIVITY="com.derived.campusdesk.MainActivity"
LOG="$ROOT/install-debug.log"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

command -v java >/dev/null || die "JDK 17 not found. Install openjdk@17 or set JAVA_HOME."
command -v adb >/dev/null || die "adb not found under $ANDROID_HOME/platform-tools"
[[ -x "$ROOT/gradlew" ]] || die "gradlew missing or not executable"

echo "==> Checking connected devices..."
adb start-server >/dev/null
DEVICES=("${(@f)$(adb devices | awk 'NR>1 && $2=="device" {print $1}')}")
(( ${#DEVICES[@]} > 0 )) || die "No authorized device found. Enable USB debugging and accept the prompt on the phone."

if (( ${#DEVICES[@]} > 1 )); then
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    DEVICE="$ANDROID_SERIAL"
  else
    echo "Multiple devices connected:"
    printf '  %s\n' "${DEVICES[@]}"
    die "Set ANDROID_SERIAL=<serial> and re-run."
  fi
else
  DEVICE="${DEVICES[1]}"
fi

export ANDROID_SERIAL="$DEVICE"
echo "Using device: $DEVICE"
adb devices -l

echo "==> Building and installing debug APK..."
{
  echo "=== $(date) ==="
  echo "device=$DEVICE"
  ./gradlew :app:installDebug --no-daemon
} 2>&1 | tee "$LOG"

echo "==> Launching $APP_ID..."
adb shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
adb shell am start -n "$APP_ID/$MAIN_ACTIVITY" \
  -a android.intent.action.MAIN \
  -c android.intent.category.LAUNCHER

echo "Done. App launched on $DEVICE."
echo "Log: $LOG"
