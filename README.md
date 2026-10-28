# Minimal Player — Android video player starter

A Kotlin + Jetpack Compose + Media3 starter for Android 13+ (API 33+), with a restrained macOS-inspired visual direction: generous spacing, rounded surfaces, neutral palette, light/dark theme, and simple controls.

## Open and run
1. Install Android Studio (current stable) and Android SDK 35.
2. Open this folder in Android Studio and allow Gradle sync.
3. Run the `app` configuration on an Android 13+ device/emulator.

## Included in this starter
- Local video picker using Android Storage Access Framework (no broad storage permission needed).
- Network video URL entry.
- Media3 ExoPlayer playback and basic player screen.
- Light/dark theme toggle persisted in preferences.
- Playback position memory per media URI and resume prompt.
- Playback speed selector.
- External subtitle picker (SRT/VTT/ASS/SSA/TTML support depends on Media3 parser/build).
- Basic tap/seek gestures.
- Picture-in-picture manifest support and entry hook.
- Searchable recent-media list and sorting.
- Folder URI picker entry point (SAF tree selection).

## Important implementation notes
This is a **starter project**, not a production-ready release. Validate Gradle sync/build against your installed Android Studio and SDK. Media3 PiP and background playback require device testing; a robust always-on background playback experience should move playback ownership into a `MediaSessionService`. The folder picker currently records the selected folder URI; recursive media indexing and persistent tree access should be implemented before release. Subtitle parsing for formats beyond SRT/VTT may need Media3 extension dependencies. Network playback requires a direct media URL supported by Media3; arbitrary web pages and DRM services are not direct video URLs.

## Suggested next milestones
1. Add `MediaSessionService` for true background playback and notification/media controls.
2. Add MediaStore indexing + folder browser, thumbnail cache, and resume database (Room).
3. Add robust gesture overlay, subtitle styling/sync, playlist, and network error states.
4. Add automated tests, accessibility review, privacy disclosures, and release signing.

## Cloud build workflow
A GitHub Actions workflow is included at `.github/workflows/build-apk.yml`. To use it, upload this project to a GitHub repository, then run **Actions → Build Android APK → Run workflow**. Download `MinimalPlayer-debug-apk` from the completed run's Artifacts section. The current chat environment does not include Android SDK or Gradle, so it cannot itself produce or verify an APK.
