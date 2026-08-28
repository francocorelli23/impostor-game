# Building Impostor

## Requirements

- **Android Studio** Ladybug (2024.2) or newer, or a standalone JDK 17 + Android SDK
- **Android SDK Platform 36** and **Build-Tools 36** (Android Studio offers to install
  them the first time you open the project)
- Internet access **for the build only** — Gradle downloads AGP, Kotlin and the
  AndroidX libraries on the first run. The finished app itself never uses the network.

The Gradle wrapper is committed, so you do not need Gradle installed. On first run it
downloads Gradle 8.13 automatically.

---

## 1. Open and run

```bash
# from the project root
./gradlew assembleDebug          # macOS / Linux
gradlew.bat assembleDebug        # Windows
```

Or just open the folder in Android Studio and press Run. The debug variant installs
alongside a release build (its application id is `com.impostor.party.debug`).

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

---

## 2. Create a release keystore (once, ever)

Keep this file safe and backed up. If you lose it you can never update the app on
Google Play under the same listing.

```bash
keytool -genkey -v \
  -keystore impostor-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias impostor
```

Put `impostor-release.jks` in the project root (or anywhere you like — the path in
the next step can be absolute).

## 3. Point the build at it

```bash
cp keystore.properties.example keystore.properties
```

Then edit `keystore.properties`:

```properties
storeFile=impostor-release.jks
storePassword=your-store-password
keyAlias=impostor
keyPassword=your-key-password
```

`keystore.properties` and `*.jks` are already in `.gitignore`. Never commit either.

If the file is missing the project still configures and builds — the release variant
is simply left unsigned — so a fresh clone works out of the box.

## 4. Build the App Bundle for Google Play

```bash
./gradlew bundleRelease
```

Output:

```
app/build/outputs/bundle/release/app-release.aab
```

That is the file you upload in Play Console → Production → Create new release.

To build a signed APK instead (for sideloading or testing):

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

From Android Studio you can also use **Build → Generate Signed App Bundle / APK**,
which will pick up the same keystore.

### Verifying the release before you upload

```bash
# confirm it is signed
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk

# confirm the manifest requests no permissions
$ANDROID_HOME/build-tools/36.0.0/aapt2 dump permissions \
  app/build/outputs/apk/release/app-release.apk
```

The permission dump should list the package name and nothing else.

---

## 5. Versioning

Both values live in `app/build.gradle.kts`:

```kotlin
versionCode = 1        // must increase on every Play upload
versionName = "1.0.0"  // what users see
```

Play rejects an upload whose `versionCode` is not higher than the last one.

---

## Release build configuration (already set up)

| Setting | Value |
|---|---|
| `isMinifyEnabled` | `true` — R8 shrinks and obfuscates |
| `isShrinkResources` | `true` — unused resources stripped |
| `isDebuggable` | `false` |
| ProGuard rules | `app/proguard-rules.pro` (keeps line numbers for Play crash reports, strips `android.util.Log`) |
| Debug-only deps | `ui-tooling` is `debugImplementation`, so it never ships |
| Permissions | none declared |
| `targetSdk` | 36 — Google Play requires API 36 for new apps and updates from 31 Aug 2026 |
| `minSdk` | 26 — runs on Android 8.0 and everything newer |
| Language splits | disabled, so English and Croatian both stay in the base module |

---

## Play Console assets

`store/` contains:

- `play_icon_512.png` — the 512×512 listing icon
- `feature_graphic_1024x500.png` — the feature graphic

You will also need screenshots (at least two phone screenshots, 16:9 or 9:16,
minimum 320px on the short edge). Take them from a device or emulator once the app is
running — Home, the impostor reveal card and the results screen make the best three.

For the Data Safety form: **no data collected, no data shared**. The app has no
`INTERNET` permission, so nothing can leave the device. `PRIVACY.md` is written to be
published as-is at a URL you control, which Play requires you to supply.

---

## Troubleshooting

**"SDK location not found"** — open the project once in Android Studio, or create
`local.properties` with `sdk.dir=/path/to/Android/sdk`.

**"Unsupported Java version"** — the build needs JDK 17. Android Studio ships one;
from the command line set `JAVA_HOME` to a JDK 17 install.

**Gradle sync fails behind a proxy** — the first build needs `dl.google.com` and
`repo.maven.apache.org`. After the dependencies are cached you can build offline with
`./gradlew --offline bundleRelease`.

**Adding another language** — copy `res/values/strings.xml` to
`res/values-<code>/strings.xml` and translate it, add the code to
`res/xml/locales_config.xml` and to `AppLanguage`, and drop a matching word list at
`assets/words/<code>.json` with the same category ids. `tools/verify_project.py`
checks that all three stay in sync.

**Android Studio offers to upgrade AGP** — safe to accept; the project is pinned to
AGP 8.13.0 / Kotlin 2.0.21 in `gradle/libs.versions.toml`.
