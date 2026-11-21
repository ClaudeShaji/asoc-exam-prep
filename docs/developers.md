# For developers

## Build

Requirements: JDK 21 and the Android SDK (compile SDK 37).

```bash
git clone https://github.com/ClaudeShaji/asoc-exam-prep.git
cd asoc-exam-prep
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. Its app ID is
`com.asocexamprep.debug`, so it installs next to the release app.

## Project layout

| Path | What's there |
|---|---|
| `app/src/main/java/com/asocexamprep/` | Kotlin and Jetpack Compose source |
| `app/src/main/assets/content/asoc-in/` | Content pack: syllabus, notes (Markdown), questions, hints, reference tables, Morse training lists, all JSON |
| `app/src/main/assets/audio/phonetics/` | Phonetic alphabet recordings and their licence |
| `app/src/test/` | Unit tests |
| `app/src/androidTest/` | Device UI tests, plus the screenshot tests for this guide |
| `docs/`, `mkdocs.yml` | This guide |

## Tests

```bash
./gradlew :app:testDebugUnitTest                # unit tests
./gradlew :app:connectedDebugAndroidTest        # device tests (phone or emulator attached)
```

## Releases

Releases are built by GitHub Actions. Set `versionCode` and `versionName` in
`app/build.gradle.kts`, commit, then push a matching tag:

```bash
git tag v0.9.0
git push origin v0.9.0
```

The [release workflow](https://github.com/ClaudeShaji/asoc-exam-prep/blob/main/.github/workflows/release.yml)
runs the unit tests, builds and signs the APK, checks the signature and publishes the APK and its
SHA-256 checksum as a GitHub Release. Signing uses the repository secrets `ASOC_KEYSTORE_BASE64`,
`ASOC_STORE_PASSWORD`, `ASOC_KEY_ALIAS` and `ASOC_KEY_PASSWORD`.

## Updating the guide's screenshots

Screenshots are taken on a real device by `DocsScreenshots`, which seeds sample progress, walks
through the app and saves each screen with the status bar cropped:

```bash
./gradlew :app:installDebug :app:installDebugAndroidTest
adb shell am instrument -w -e class com.asocexamprep.DocsScreenshots \
  com.asocexamprep.debug.test/androidx.test.runner.AndroidJUnitRunner
for f in $(adb shell run-as com.asocexamprep.debug ls files/docs-shots); do
  adb exec-out run-as com.asocexamprep.debug cat files/docs-shots/$f > docs/images/$f
done
```

## Preview the guide

```bash
pip install -r docs/requirements.txt
mkdocs serve
```

The [docs workflow](https://github.com/ClaudeShaji/asoc-exam-prep/blob/main/.github/workflows/docs.yml)
publishes the guide to GitHub Pages on every push to `main` that changes it.
