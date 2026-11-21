# ASOC Exam Prep

An Android app to prepare for India's **Amateur Station Operator's Certificate (ASOC)** exam,
for both the General and Restricted grades.

**[Download the latest APK from Releases →](https://github.com/ClaudeShaji/asoc-exam-prep/releases/latest)**

## Features

- **Study notes** for every syllabus topic, taken from the NIAR Study Manual and the
  Telecommunications (Amateur Services) Rules, 2024, with the source credited on each page.
- **Morse trainer**: learn one character at a time, then groups, words and sentences.
  Includes copy practice, send practice on an on-screen key, and a Morse code test.
- **Exams**: full mock papers, Radio Theory (Section A) and Rules & Regulations (Section B)
  papers, topic practice, and a review queue for mistakes. Questions come from published
  question banks with their published answer keys.
- **Live results** toggle, and hints quoted from the study notes.
- **Reference**: phonetic alphabet with audio, Q codes, abbreviations and band plan.
- **Progress tracking** with streaks, levels and badges.

## Install

1. Download `ASOC-Exam-Prep-<version>.apk` from [Releases](https://github.com/ClaudeShaji/asoc-exam-prep/releases).
2. Open it on your phone and allow installing from your browser or file manager.

Needs Android 8.0 or newer. Each release also has a `.sha256` file for checking the download.

## Build

Requires JDK 21 and the Android SDK.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Release builds are signed with the key named in an untracked `keystore.properties`
(`STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) or the matching `ASOC_*`
environment variables. Without one, they fall back to the debug key.

## Releasing

Set `versionCode` and `versionName` in `app/build.gradle.kts`, then push a matching tag:

```bash
git tag v0.8.0 && git push origin v0.8.0
```

The [Release workflow](.github/workflows/release.yml) builds, signs and publishes the APK.

## Licence

The source code is licensed under the [Apache License 2.0](LICENSE).
The bundled study content and voice clips are not: see [NOTICE](NOTICE) for their owners and
licences. In the app, every source is listed under **Settings → About**.

Not affiliated with or endorsed by the Department of Telecommunications, WPC or NIAR.
