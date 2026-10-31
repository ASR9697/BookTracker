#!/bin/sh
# Records the app's startup profile on a connected Android 13+ device and saves it
# as app/src/main/baseline-prof.txt. Run it after big UI changes, then commit the file.
#
#   scripts/update-baseline-profile.sh <adb-serial>     (serial from `adb devices`)
#
# It drives the app's "benchmark" build (package <APP_ID>.benchmark, seeded with a
# demo library), so the reader's own install and data on the device are untouched.
set -e
cd "$(dirname "$0")/.."

if [ -z "$1" ]; then
    echo "usage: $0 <adb-serial>   (one of:)" >&2
    adb devices -l >&2
    exit 1
fi
export ANDROID_SERIAL="$1"

./gradlew :baselineprofile:connectedBenchmarkAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile

out=$(ls -t baselineprofile/build/outputs/connected_android_test_additional_output/benchmark/connected/*/BaselineProfileGenerator_generate-startup-prof.txt | head -n 1)
# The demo-library seeder only exists in the benchmark build.
grep -v "booktracker/app/benchmark/" "$out" > app/src/main/baseline-prof.txt
echo "Saved $(wc -l < app/src/main/baseline-prof.txt) rules to app/src/main/baseline-prof.txt"
