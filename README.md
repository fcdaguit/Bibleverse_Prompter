# Bible Verse Prompter

A 100% offline Android teleprompter for preachers. It listens continuously,
recognises spoken scripture references in English, Tagalog and Cebuano, and puts
the verse on screen in large, high-legibility type — advancing verse by verse as
you read.

No internet is needed for anything except the optional Bible downloader.

---

## Build status

This repository is being delivered in five steps. **Steps 1 and 2 are in place**,
and the project compiles and produces an installable APK today: launching it
shows the Bible import running, which is the fastest way to confirm your asset
JSON is wired up correctly on a real phone.

| Step | Scope | Status |
| --- | --- | --- |
| 1 | Project tree, Gradle config, GitHub Actions workflow | done |
| 2 | Room database, DAOs, asset seeder, JSON schemas | done |
| 3 | Offline speech engine + multilingual reference parser | next |
| 4 | Themes, typography, Compose UI (prompter, manager, settings) | pending |
| 5 | Manifest, runtime permission flow | partly in place (manifest written) |

---

## Building the APK on GitHub (no Android Studio needed)

1. Create a repository and push this project to it.
2. Open the **Actions** tab. The `Build APK` workflow runs on every push, and can
   also be started by hand with **Run workflow**.
3. When it finishes, open the run and download the
   `bible-verse-prompter-debug-<n>` artifact from the Artifacts section.
4. Unzip it and install the `.apk` on your phone (you will need to allow
   installing from unknown sources).

The workflow sets up JDK 17, installs the Android SDK, runs
`./gradlew :app:assembleDebug`, and uploads the result. If a build fails it also
uploads the Gradle reports so you can see why without a local setup.

The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`)
is committed, so CI does not have to bootstrap it. **Do not add `*.jar` to
`.gitignore`** — that is the single most common reason an Android repo fails to
build on a fresh clone. The workflow regenerates the wrapper if it goes missing
anyway, as a safety net.

---

## Project tree

```
BibleVersePrompter/
├── .github/
│   └── workflows/
│       └── build-apk.yml                GitHub Actions: JDK 17 → assembleDebug → artifact
├── gradle/
│   ├── libs.versions.toml               single source of truth for every version
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties    Gradle 8.11.1
├── gradlew  /  gradlew.bat
├── settings.gradle.kts
├── build.gradle.kts                     root, plugins declared with `apply false`
├── gradle.properties
├── .gitignore
├── README.md
└── app/
    ├── build.gradle.kts                 SDK levels, Compose, Room/KSP, Vosk, packaging
    ├── proguard-rules.pro               keeps JNA/Vosk reflection working in release
    └── src/main/
        ├── AndroidManifest.xml          RECORD_AUDIO, INTERNET, ACCESS_NETWORK_STATE
        ├── assets/
        │   └── bibles/
        │       ├── manifest.json        which translations ship in the APK
        │       ├── kjv.json             sample — swap for the full public-domain text
        │       ├── tagalog_adb.json     sample
        │       ├── cebuano_bugna.json   sample
        │       └── README.md            schema + how to drop in full Bibles
        ├── java/com/dlbc/verseprompter/
        │   ├── VersePrompterApp.kt      manual DI; starts the one-time import
        │   ├── MainActivity.kt          placeholder host (replaced in Step 4)
        │   └── data/
        │       ├── model/
        │       │   ├── BibleBook.kt     canonical 66 books + EN/TL/CEB names
        │       │   ├── Verse.kt         VerseRef, Verse, VerseRange
        │       │   └── BibleVersion.kt
        │       ├── db/
        │       │   ├── BibleDatabase.kt Room database + creation callback
        │       │   ├── entity/
        │       │   │   ├── VerseEntity.kt
        │       │   │   └── BibleVersionEntity.kt
        │       │   ├── dao/
        │       │   │   ├── VerseDao.kt
        │       │   │   └── BibleVersionDao.kt
        │       │   └── seed/
        │       │       ├── BundledVersion.kt     reads assets/bibles/manifest.json
        │       │       ├── BibleJsonImporter.kt  streaming JSON → verse rows
        │       │       ├── BibleSeedManager.kt   one-time import + progress
        │       │       └── SeedState.kt
        │       └── repository/
        │           └── BibleRepository.kt        the only scripture API the UI sees
        └── res/
            ├── drawable/ic_launcher_foreground.xml
            ├── mipmap-anydpi-v26/ic_launcher.xml
            └── values/{colors,strings,themes}.xml
```

Still to come: `speech/`, `parser/`, `ui/` (theme, prompter, manager, settings)
and `settings/` (DataStore).

---

## Stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM — ViewModel + StateFlow, manual DI (no Hilt) |
| Database | Room 2.6.1 (SQLite), KSP |
| Preferences | Jetpack DataStore |
| Speech | Vosk-Android 0.3.47, with the platform `SpeechRecognizer` as fallback |
| Min / Target SDK | 26 (Android 8.0) / 34 (Android 14) |
| Build | AGP 8.7.3, Gradle 8.11.1, JDK 17 |

Versions are pinned to a combination known to work together rather than to the
newest release of each. Newer Android Gradle Plugin lines exist; AGP 9.x is a
breaking major release, so bump it deliberately, not casually.

---

## How the offline database works

**Lookup speed.** `verses` uses a composite primary key of
`(versionCode, bookNumber, chapter, verse)`. A single-verse lookup resolves
through that index with no table scan, which keeps it comfortably under the 10 ms
budget even with several translations installed. A second index on
`(versionCode, bookNumber, chapter)` covers whole-chapter and range reads.

**Language independence.** Book identity is a number, never a name. Genesis is
book 1 whether you are in KJV, Ang Dating Biblia or Bugna, so switching
translations mid-sermon keeps you on the same verse. Display names live in
`BibleBook.kt` and are chosen by the version's language tag.

**Seeding.** The Room creation callback deliberately does *not* parse Bibles
inline — that would block the first query behind several seconds of work on app
start. It raises a flag, and `BibleSeedManager` does the import in the
background, reporting progress through a `StateFlow` the UI can show.

**Interrupted imports recover.** A version row is written with `isComplete = 0`
before its verses load and flipped to `1` only when the file finishes. If the app
is killed halfway, the next launch sees the incomplete row, wipes that version
and re-imports it, so you never get a half-loaded Bible that silently misses
Revelation.

**Memory.** Parsing uses `android.util.JsonReader` streaming with 2,000-row insert
batches. A full Bible is ~31,000 verses; loading it as a JSON tree on a low-end
phone is a realistic OutOfMemoryError, and streaming avoids it entirely.

---

## Before you preach from this

- The bundled Bible files are **samples**. See
  `app/src/main/assets/bibles/README.md` for how to drop in the full
  public-domain text.
- The Tagalog and Cebuano sample verses, and the translated book names in
  `BibleBook.kt`, were written from memory and have not been checked against a
  verified source. Correct them against the edition your church uses — both are
  in one place each, by design.
