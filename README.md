# Fenrir AI 1.2.0

Android application for offline family contract tracking. Package: `com.fenrir.ai`.

## Features in this source snapshot
- Local-only storage using Android SharedPreferences (JSON); no account, server, or cloud database.
- Family member list.
- Contract execution with selected participants and a **minimum-only** participant threshold. There is no maximum participant limit.
- Contract cooldown display and disabled execution while cooling down.
- Execution journal, removal of journal entries, day/week/month earnings summaries, and participant activity ranking.
- Default sample contracts: Темні води І, Чисте місто І, Логістика І.

## Build APK on GitHub
1. Create a new GitHub repository and upload the contents of this folder (not the ZIP itself).
2. Open **Actions** and enable workflows if GitHub asks.
3. Run **Build Fenrir AI APK** (or push a commit to `main`).
4. Open the completed workflow run, find **Artifacts**, and download `FenrirAI-debug-apk`.
5. Extract the artifact ZIP; install `app-debug.apk` on the Android device.

The workflow uses JDK 17 and Gradle 8.9. See the official [Gradle GitHub Actions guide](https://docs.gradle.org/current/userguide/github-actions.html).

## Important status
This is a GitHub-buildable source project, not a locally verified APK. The current environment has no Android SDK and no installed Gradle, so the Android compilation must be confirmed by the GitHub Actions run. This initial implementation is a functional foundation; contract editing, full backup/restore, and editing an existing journal record are not yet implemented.
