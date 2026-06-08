# EchoWithin Android App
This note app is adapted from echowithin website. Find the repo [here](https://github.com/vincent0-ai/echowithin) and the website [here](https://echowithin.xyz) 
It is just the echowithin personal space but note focused, stripping away blog features etc.

Jetpack Compose Android client for EchoWithin APIs.

## Current Functional Slice
- Cookie/session login (`POST /api/v1/login`)
- Session re-authentication (`POST /api/v1/app_reauth`) with persistent httpOnly cookies
- Notes list (`GET /api/v1/notes`), create, edit, delete, search
- Note detail with full Markdown rendering (custom parser + KaTeX for math)
- Offline-first: local SQLite database, bidirectional sync, smart sync dispatcher
- Note locking (premium), version history, collaboration proposals
- Note sharing with permissions, expiry, surprise themes, auto-approve
- Shared Links tab, Activity tab with notifications and proposals
- App Lock: 4-digit PIN with shake animation on wrong entry
- Offline mode: create notes without an account, backup prompt on first login
- FCM push notifications
- In-app OTA update (checks `update-manifest.json`)
- Premium tier display with upgrade deep link
- Guest/Offline mode: full local note-taking without sign-in
- Sync mode preference (Automatic / Manual)

## Project Layout
- `app/src/main/java/com/example/echowithin/presentation/*` - screens, nav, app shell, viewmodels
- `app/src/main/java/com/example/echowithin/data/*` - API service, DTOs, repositories, client
- `app/src/main/java/com/example/echowithin/domain/*` - models, use cases
- `api.py` - backend route reference used for Android client integration

## Configure Java (Windows PowerShell)
Use Android Studio bundled JBR:

```powershell
$Env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$Env:Path = "$Env:JAVA_HOME\bin;$Env:Path"
java -version
```

Persist for current user:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "User")
$currentUserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentUserPath -notlike "*%JAVA_HOME%\\bin*") {
    [Environment]::SetEnvironmentVariable("Path", "%JAVA_HOME%\\bin;$currentUserPath", "User")
}
```

## Build, Test and Deploy (Windows PowerShell)

Ensure you set the path to Android Studio's bundled JetBrains Runtime (JBR) for the Java Environment:

```powershell
# Set JBR environment for current shell session
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

# Compile and assemble the debug APK
.\gradlew assembleDebug

# Run unit tests
.\gradlew testDebugUnitTest
```

### Install and Launch on Device/Emulator

Once compiled, you can deploy the debug APK directly to a connected Android phone or emulator:

```powershell
# 1. Identify connected device serial
C:\Users\DevTech\AppData\Local\Android\Sdk\platform-tools\adb.exe devices

# 2. Deploy/Install the APK (replace <DEVICE_SERIAL> with actual serial, e.g., 3Z01Z3479Y3C1900330)
C:\Users\DevTech\AppData\Local\Android\Sdk\platform-tools\adb.exe -s <DEVICE_SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Start/Launch the application automatically
C:\Users\DevTech\AppData\Local\Android\Sdk\platform-tools\adb.exe -s <DEVICE_SERIAL> shell am start -n com.example.echowithin/com.example.echowithin.MainActivity
```

## API Base URL
Configured in `app/build.gradle.kts` as:
- `BuildConfig.API_BASE_URL = "https://echowithin.xyz/api/"`

If your backend runs elsewhere, update that field and rebuild.


