# StatusVault

Save and manage WhatsApp statuses on your Android device — privately, offline, and without a Play Store account.

StatusVault reads statuses already on your phone after you grant folder access. It does not scrape chats, bypass privacy settings, or require an internet connection for core use.

## Download

**[Download APK (latest)](https://github.com/deepaksinghbora/statusvault/releases/latest/download/app-release.apk)**

Or open [Releases](https://github.com/deepaksinghbora/statusvault/releases) and pick a version.

### Install

1. Download `app-release.apk` on your Android phone.
2. Open the file. If prompted, allow install from your browser / file manager.
3. Launch **StatusVault** and choose the WhatsApp `.Statuses` folder when asked.

Typical folder paths:

- WhatsApp: `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- WhatsApp Business: `Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses`

## Features

- Browse WhatsApp / WhatsApp Business statuses (images & videos)
- Filter by All / Images / Videos; sort newest or oldest
- Full-screen preview with swipe between items
- Save statuses into an in-app library
- Export to the device gallery
- Share media (or the app download link) via the system share sheet
- Light / dark / system theme
- Works offline — no account, no ads, no tracking for core flows

## Requirements

|             |                                                         |
| ----------- | ------------------------------------------------------- |
| Android     | 7.0 (API 24) or newer                                   |
| Permissions | Folder access to `.Statuses` (Storage Access Framework) |

## Build from source

```bash
git clone https://github.com/deepaksinghbora/statusvault.git
cd statusvault
./gradlew assembleRelease
```

Release APK output:

```text
app/build/outputs/apk/release/app-release.apk
```

Open the project in Android Studio (Ladybug / newer) if you prefer a GUI build.

## Versioning

- `versionName` — SemVer shown to users (`1.0.0`)
- `versionCode` — integer that must increase on every release

Both are set in `app/build.gradle.kts`. See [CHANGELOG.md](CHANGELOG.md) for release notes.

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose, Lifecycle, DataStore
- Coil (images), Media3 (video)
- Min SDK 24 · Target / Compile SDK 36

## Privacy

StatusVault only accesses media you explicitly grant via the system folder picker. Statuses stay on your device. Core browsing and saving do not require network access.

## License

All rights reserved unless a license file is added to this repository.
