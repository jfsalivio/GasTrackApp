# GasTrack Android

The installed application now packages the **same React frontend as the web app** in an Android WebView. `MainActivity` opens `WebAppActivity`; the previous Compose implementation remains as reference code. Modules, forms, validation, histories, reports, held carts, receipts and management settings come from `src`, rather than separate simplified native screens. Mobile layouts use `src/Mobile.css` and a collapsible navigation drawer. Desktop layouts retain the web sidebar.

Gradle runs `npm run build:android` before building the APK and bundles the resulting HTML, JavaScript, CSS, logos and public product images under `app/src/main/assets/web`. Install root Node dependencies (`npm.cmd ci`) before building. Generated frontend assets are ignored by Git. No frontend server is needed at runtime; Express, MySQL and ML still run separately.

The local frontend origin is `https://appassets.androidplatform.net`; Express explicitly allows that origin. The API address comes from the Android build configuration. Debug defaults to `http://10.0.2.2:4000/`; release requires HTTPS. Existing native sessions transfer once into the shared frontend and are then removed from native session storage. Imports and logo selection use Android's system file picker. Downloads use the system Save dialog; receipts use Android printing, including Save as PDF. No broad storage permission is required.

Debug UI verification: forward the running WebView devtools socket to local TCP 9222 and run `node android/scripts/check-web-parity.mjs` from the repository root. This checks module rendering, image failures and page overflow and saves screenshots under ignored `android/review`. It does not submit business transactions or replace role-based acceptance testing.

Open **this `android` directory** as a project in Android Studio. The React, Express and Python applications remain in the repository root, `Backend`, and `ML`.

## Build

Requires JDK 17 or 21, Android SDK platform 35 and Build Tools 35. Java/Kotlin bytecode targets 17; minimum Android API is 26; compile/target API is 35. Set Android Studio's Gradle JDK to 17/21 rather than its bundled JDK 25.

```powershell
cd android
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:ANDROID_HOME = 'C:\Users\user\AppData\Local\Android\Sdk'
$env:ANDROID_USER_HOME = Join-Path $PWD '.android'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The verification run used a project-local JDK 21 under `.tools/jdk21` and `GRADLE_USER_HOME` under `.gradle-user`; both directories are ignored. SDK/build tools were installed through Gradle using the existing accepted SDK licenses. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Backend environments

Debug defaults to `http://10.0.2.2:4000/`, the Android emulator's address for the host machine. Start/configure the existing backend separately according to the root README. For a physical phone, supply an address reachable from that phone:

```powershell
.\gradlew.bat :app:assembleDebug -PGASTRACK_DEV_URL=http://192.168.1.10:4000/
```

Release builds require an explicit HTTPS endpoint:

```powershell
.\gradlew.bat :app:assembleRelease -PGASTRACK_PRODUCTION_URL=https://your-api.example.com/
```

Release signing credentials and deployment are not included. Configure a private signing key in Android Studio before distribution. The API has no `/api` prefix. Never put JWT secrets, MySQL credentials, SMTP passwords or ML-service credentials into Android build properties.

Cleartext is enabled only for debug. Production disables cleartext and network logging. Debug logs omit authentication requests and never log bodies or authorization headers. DataStore sessions are excluded from backups and device transfers. Account permission refresh runs on foreground entry and every 30 seconds while foregrounded. A late 401 cannot clear a newer token.

Password reset accepts `gastrack://reset-password?token=...`. Existing emailed web hash links remain web links; connecting a verified HTTPS app link requires deployment-specific domain ownership and `assetlinks.json` hosting.

## Retained native implementation

```text
app/src/main/java/com/gastrack/app/
  core/common       Resource, bounded file input
  core/network      Auth interceptor, error parsing, connectivity, token interface
  core/di           Hilt network and repository providers
  data/models       Serialized DTOs, decimal/identifier serializers, domain mappers
  data/api          Explicit Retrofit route declarations
  data/repository   Auth, operations, management, fulfillment and DataStore
  domain/entities   Framework-independent business models and commands
  domain/repository Repository contracts
  domain/usecase    Registration validation
  presentation      Compose screens, components, StateFlow ViewModels, typed navigation
```

Money remains `BigDecimal` in the domain. Wire `Decimal` accepts MySQL string/number responses and emits validated unquoted numeric JSON without converting through `Double`. SQL date/time strings remain unchanged in DTOs; pickup input is interpreted in `Asia/Manila`.

Some detail/history/admin endpoints intentionally retain `JsonObject` at the transport boundary. They are mapped in repositories rather than exposed in Compose. These are not a claim that every legacy response has been converted to a dedicated Kotlin DTO.

## ML and files

Android calls authenticated Express `/restocking`, `/restocking/generate`, `/restocking/rescore` and quick-recommendation routes. Express dispatches to Flask/XGBoost, persists results and retains its existing four-second inference timeout and heuristic fallback. Android never contacts Flask directly or fabricates fallback confidence. Confidence is a score in percent, not a confidence interval; the server contract provides no intervals.

Only `INTERNET` and `ACCESS_NETWORK_STATE` are requested. Numeric inference requires no camera/media permission. Android Activity Result document/content contracts handle CSV files, downloaded files, and logo images without broad storage access. Logo input is limited to 3 MB and uses the existing JSON data-URL contract. PDF responses are declared as `ResponseBody`; base64 exports and CSV reports are decoded in repositories.

## Verification

```powershell
python scripts/check_endpoint_coverage.py
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The static checker matches 130 Express route patterns against 135 Retrofit declarations, including generated brand/category, supplier-type, purchase-order action and user-settings routes. It checks routing coverage, not runtime schema completeness or authorization correctness.

The 14 JVM tests exercise exact query names, numeric request serialization, login/current-user field casing, ML mapping, registration validation, bearer headers, missing/expired sessions, stale 401 responses, timeout behavior, and cancellation. See [MIGRATION_STATUS.md](MIGRATION_STATUS.md) for scope and verification limits.
