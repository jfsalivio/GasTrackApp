# GasTrack

GasTrack includes a web application, an Android application, an Express backend,
a MySQL database, and a Python restocking service. The Android APK bundles the
same React frontend used by the website, with layouts for phones and Android
file-selection, download, and printing dialogs.

All terminal commands below use **Windows PowerShell**. Run them from the
repository root unless a section explicitly changes directories. Use `npm.cmd`
instead of `npm` if PowerShell blocks `npm.ps1`.

## Contents

- [Components and addresses](#components-and-addresses)
- [Requirements](#requirements)
- [First-time setup](#first-time-setup)
- [Restore or connect the database](#restore-or-connect-the-database)
- [Prepare the ML model](#prepare-the-ml-model)
- [Daily startup: backend, ML, and web frontend](#daily-startup-backend-ml-and-web-frontend)
- [Run the Android application](#run-the-android-application)
- [Verify the installation](#verify-the-installation)
- [Build and test](#build-and-test)
- [Stop and restart](#stop-and-restart)
- [Database backups and upgrades](#database-backups-and-upgrades)
- [Troubleshooting](#troubleshooting)

## Components and addresses

| Component | Location | Local address / purpose |
| --- | --- | --- |
| Web frontend | `src`, root `package.json` | `http://localhost:5173` |
| Android application | `android` | Installed APK; frontend files are bundled inside it |
| Express API | `Backend` | `http://localhost:4000` |
| MySQL | Configured in environment files | Usually `127.0.0.1:3306` for a local installation |
| Flask / XGBoost ML service | `ML` | `http://localhost:5001` |

The web frontend and Android application call Express. Express reads and writes
MySQL and calls Flask for restocking predictions. Android does not connect to
MySQL or Flask directly. API routes have **no `/api` prefix**.

For the **web app**, run the backend, ML service, and Vite frontend. For the
**Android app**, run the backend and ML service, then launch the APK; a running
Vite server is not required. MySQL must be available in both cases.

## Requirements

Install these before setting up a new machine:

| Requirement | Used for |
| --- | --- |
| Git | Clone and update the repository |
| Node.js and npm | Frontend, backend, and Android frontend bundling; Vite requires Node `^20.19.0` or `>=22.12.0` |
| Python with `venv` and `pip` | Install packages from `ML/requirements.txt` and run Flask / XGBoost |
| MySQL 8 with TLS | Application data; use your configured remote server or a local server |
| MySQL 8 client tools: `mysql`, `mysqldump` | Restore and capture the private database snapshot |
| Android Studio, JDK 17 or 21 | Build and run Android; these are not needed for web-only use |
| Android SDK Platform 35 and Build Tools 35 | Compile the Android project |
| Android Emulator and Platform Tools | Run a virtual device and install APKs using ADB |

Check the command-line tools:

```powershell
git --version
node --version
npm.cmd --version
python --version
```

For Android Studio, select a **Gradle JDK of 17 or 21**. Do not use a bundled
JDK 25 for this project. Android's minimum supported API is 26; compile and
target API are 35. Install SDK components using Android Studio's SDK Manager.

## First-time setup

### 1. Open or clone the repository

If you already have the project, open PowerShell and change to its root:

```powershell
Set-Location 'C:\Users\user\AndroidStudioProjects\GasTrackApp'
```

For a new clone:

```powershell
git clone https://github.com/jfsalivio/GasTrackApp.git
Set-Location GasTrackApp
```

The root contains `package.json`, `src`, `Backend`, `ML`, and `android`.

### 2. Install frontend and backend dependencies

From the repository root:

```powershell
npm.cmd ci
npm.cmd --prefix Backend ci
```

Install the root dependencies even when running Android only: Gradle uses Vite
to bundle the frontend into the APK.

### 3. Create the environment files

These commands create missing files and preserve existing configuration:

```powershell
if (!(Test-Path Backend/.env)) { Copy-Item Backend/.env.example Backend/.env }
if (!(Test-Path ML/.env)) { Copy-Item ML/.env.example ML/.env }
if (!(Test-Path .env)) {
    'VITE_API_BASE_URL=http://localhost:4000' | Set-Content -Encoding UTF8 .env
}
```

Edit each file using your own connection settings. Environment files are
ignored by Git; cloning the repository does not supply working credentials.
If an existing installation is already working, keep its database name,
passwords, and JWT secret.

#### Root `.env`: web frontend

```dotenv
VITE_API_BASE_URL=http://localhost:4000
```

This value is the API address reachable by the browser. Restart Vite after
changing it, or rebuild if serving compiled frontend files. Do not add database
passwords or server secrets to `VITE_` variables: these are frontend values.

#### `Backend/.env`: API and database

Example for a new local installation; replace the placeholder values:

```dotenv
PORT=4000
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=your_mysql_user
DB_PASSWORD=your_mysql_password
DB_NAME=gastrack
JWT_SECRET=replace_with_a_long_random_secret
JWT_EXPIRES_IN=8h
CORS_ORIGIN=http://localhost:5173,http://127.0.0.1:5173
FRONTEND_URL=http://localhost:5173
FRONTEND_BASE_PATH=
ML_SERVICE_URL=http://127.0.0.1:5001
```

For a new installation, generate a JWT secret with:

```powershell
node -p "require('node:crypto').randomBytes(32).toString('hex')"
```

Paste the result into `JWT_SECRET`. Changing the secret on an existing
installation invalidates its current sign-in tokens.

For local Vite, leave `FRONTEND_BASE_PATH` empty. The example file also supports
deployment under a subdirectory; keeping an old `/GasTrack-CAPSTONE` value for
local Vite would point password-reset emails at the wrong path.

Configure SMTP if using password-reset or other email workflows:

```dotenv
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_SECURE=false
SMTP_USER=your.email@gmail.com
SMTP_PASS=your_email_app_password
SMTP_FROM="GasTrack <your.email@gmail.com>"
```

Use the host, port, credentials, and TLS setting required by your email provider.
The API and ordinary data screens can run without working email delivery, but
email-dependent workflows need valid SMTP configuration.

#### `ML/.env`: training database and Flask port

```dotenv
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=your_mysql_user
DB_PASSWORD=your_mysql_password
DB_NAME=gastrack
ML_SERVICE_PORT=5001
```

Use the **same database** as `Backend/.env`, including an existing review database
name if that is the installation you are using. ML loads this file relative to
its scripts; UTF-8 files with or without a byte-order mark are supported.

## Restore or connect the database

### Existing installation

Start the MySQL server if it is local, or make sure the configured remote server
is reachable. Keep the existing `Backend/.env` and `ML/.env` values. Normal
backend startup checks the database and skips seeding when tables already exist;
restarting preserves saved accounts, passwords, and business records.

Check the configured connection from the repository root:

```powershell
Set-Location Backend
node test-db.js
```

Expected output includes `Connection successful!` and `Query result: 2`.

### New installation using the private snapshot

The database backup belongs at:

```text
Backend/db/snapshots/current.sql
```

**This file is private and is not included in GitHub.** Obtain a copy from the
project owner. It contains the saved schema and business records, including
account password hashes. There is no public demo seed or default login password.

1. Start MySQL and create a **new empty database** using MySQL Workbench or the
   MySQL command-line client. For example:

   ```sql
   CREATE DATABASE gastrack CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```

2. Set `DB_NAME` in both environment files to that database and configure the
   correct host, port, username, and password.
3. Copy the private snapshot to `Backend/db/snapshots/current.sql`.
4. Stop other instances that might write to this database, then restore from
   the repository root:

   ```powershell
   Set-Location Backend
   npm.cmd run seed
   node test-db.js
   ```

The seed restores only into a database with **no tables or views**. It does not
overwrite or merge into an existing installation. A missing snapshot on an empty
database stops startup. Do not import `Backend/db/schema.sql` before this restore:
the snapshot already includes the schema. If a restore fails partway, resolve
the error and retry with a new empty database.

The restore uses MySQL client tools with TLS required. The scripts detect the
standard `C:\Program Files\MySQL\MySQL Server 8.0\bin` installation. For another
location, add the actual executable paths to `Backend/.env`:

```dotenv
MYSQL_CLIENT_PATH=C:/path/to/mysql.exe
MYSQLDUMP_PATH=C:/path/to/mysqldump.exe
```

Copy any additional files in `Backend/uploads` separately when moving an
installation. The SQL snapshot does not contain uploaded files or the ML model.

## Prepare the ML model

### 1. Create the Python environment and install packages

From the repository root:

```powershell
Set-Location ML
python -m venv venv
.\venv\Scripts\python.exe -m pip install -r requirements.txt
.\venv\Scripts\python.exe -m pip check
```

Using the virtual environment's Python executable directly avoids having to
activate it in every terminal.

### 2. Reuse or train the model

Flask expects this file:

```text
ML/restock_model.pkl
```

If a compatible saved model was supplied with your installation, keep and reuse
it. The model is ignored by Git and is not downloaded by `git clone`.

If the file is missing, configure `ML/.env`, restore/connect the database, then
run from the `ML` directory:

```powershell
.\venv\Scripts\python.exe train_model.py
```

Training saves `restock_model.pkl`. It uses available MySQL history and synthetic
bootstrap samples; it does not insert synthetic sales or stock into MySQL.
Check the training output to confirm the database connection if you want real
history included. Training can fall back to bootstrap samples when database
history cannot be loaded.

Train once for setup, reuse the saved model for daily startup, and retrain when
you intentionally want to incorporate newer history. Restart Flask after
replacing the model. Only load model files from a trusted source.

## Daily startup: backend, ML, and web frontend

After first-time setup, use separate PowerShell terminals. Each block below
starts from the repository root. Leave the service terminals open.

### Terminal 1: backend API

```powershell
Set-Location Backend
npm.cmd run dev
```

Expected output:

```text
GasTrack API listening on port 4000
```

`npm.cmd run dev` uses nodemon and restarts the API when backend files change.
For a process without automatic reload, use `npm.cmd start` instead. Both
commands check/seed the database before starting.

### Terminal 2: ML service

```powershell
Set-Location ML
.\venv\Scripts\python.exe serve.py
```

Expected output includes Flask listening on port `5001`. Keep this running while
using ML-backed restocking. The backend uses a heuristic fallback if Flask is
unreachable; fallback operation does not mean the trained model is running.

### Terminal 3: web frontend

```powershell
npm.cmd run dev
```

Open **http://localhost:5173** in a browser. Sign in with an existing account
from the configured database, or use company registration when creating a new
company. No password can be recovered directly from the snapshot's hashes.

The frontend uses port `5173` with `strictPort` enabled; it does not silently
switch to another port if that port is occupied.

## Run the Android application

### Option A: Android Studio and an emulator

1. Complete root Node dependency installation and backend/database/ML setup.
2. Start the backend and ML service using Terminals 1 and 2 above. Vite is
   optional when using Android.
3. In Android Studio, choose **Open** and select the repository's **`android`**
   directory, not the repository root.
4. In SDK Manager, install Platform 35, Build Tools 35, Platform Tools, Android
   Emulator, and a system image for your virtual device.
5. In Gradle settings, choose JDK 17 or 21 and let Gradle sync finish. The
   project includes its Gradle wrapper; a separate Gradle installation is not
   required. The first sync/build requires dependency downloads.
6. In Device Manager, create or start an Android virtual device with API 26 or
   newer. Wait for the home screen to finish loading.
7. Select the **`app`** run configuration and the emulator, then click **Run**.
8. Sign in to GasTrack. Verify Dashboard, Products, Inventory, and POS images.

Gradle automatically runs `npm.cmd run build:android` from the repository root
and packages the frontend under `android/app/src/main/assets/web`. Those
generated files are ignored by Git. After editing React or CSS, rebuild and
reinstall the APK to see the change on Android; Vite hot reload does not update
an already installed APK.

### Option B: build and install from PowerShell

Start an emulator from Android Studio's Device Manager first. From the repository
root, set paths to your **actual installed JDK and SDK**:

```powershell
Set-Location android
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_USER_HOME = Join-Path $PWD '.android'
$env:ANDROID_PREFS_ROOT = "$PWD"
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user'

.\gradlew.bat :app:assembleDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n com.gastrack.app.debug/com.gastrack.app.MainActivity
```

`adb devices` should show an emulator with status `device`. If several devices
are connected, add `-s DEVICE_SERIAL` immediately after `adb.exe` for installation
and launch commands.

The APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

### API addresses: computer, emulator, and physical phone

| Client | API address | Configuration |
| --- | --- | --- |
| Browser on the development computer | `http://localhost:4000` | Root `.env`: `VITE_API_BASE_URL` |
| Standard Android Studio emulator | `http://10.0.2.2:4000/` | Android debug default |
| Physical phone on the same network | For example `http://192.168.1.10:4000/` | Android `GASTRACK_DEV_URL` Gradle property |

Inside an emulator or phone, `localhost` refers to that device, not your
development computer. `10.0.2.2` is the standard emulator's route to the host.
Android's configured API address overrides the root frontend `.env` inside the APK.

For a physical phone, enable USB debugging, connect it, confirm it appears in
`adb devices`, and find the computer's LAN IPv4 address with `ipconfig`. Keep
both devices on a network where the phone can reach the computer. From `android`:

```powershell
.\gradlew.bat :app:assembleDebug -PGASTRACK_DEV_URL=http://192.168.1.10:4000/
```

Replace the example IP, install the resulting APK, and allow inbound API traffic
on port `4000` if Windows Firewall blocks it. Keep MySQL and ML configured on the
backend machine; the phone does not need access to their ports.

### Release build

Release requires an explicit HTTPS API address. From `android`:

```powershell
.\gradlew.bat :app:assembleRelease -PGASTRACK_PRODUCTION_URL=https://your-api.example.com/
```

Configure private release signing before distributing the application. Debug
allows local HTTP; release disables cleartext traffic. Build properties should
contain the API address only, not database passwords, JWT secrets, or SMTP keys.
See [Android documentation](android/README.md) for further implementation details.

## Verify the installation

### API and ML health

Run in another PowerShell terminal while the services are running:

```powershell
Invoke-RestMethod http://localhost:4000/health
Invoke-RestMethod http://localhost:5001/health
```

Expected API response: `status: ok`. Expected ML response: `status: ok` and
`model_loaded: True`. API health alone does not check MySQL: also run
`node test-db.js` from `Backend` and sign in to verify saved records.

### ML prediction smoke test

This computes a prediction without creating business records:

```powershell
$prediction = @{
    items = @(@{
        productId = 'SMOKE-TEST'
        current_stock = 5
        reorder_level = 10
        sales_7d = 14
        sales_30d = 60
        sales_90d = 180
        lead_time_days = 3
        days_since_last_restock = 12
    })
} | ConvertTo-Json -Depth 4

Invoke-RestMethod http://localhost:5001/predict -Method Post -ContentType 'application/json' -Body $prediction
```

Expect a `results` array containing `productId`, `confidence`, and
`recommendedQuantity`. Confidence is a percentage score, not a confidence interval.

### Frontend and Android checks

Sign in, open each module, and confirm data loads from the expected database.
In Android, use the menu button to switch modules. Check product images in POS,
opening a product form, holding/restoring a cart, downloading a CSV template,
and opening the import picker. Verify business-changing actions in a separate
review/test database before using them with live records.

## Build and test

### Web frontend

From the repository root:

```powershell
npm.cmd run build
npm.cmd run lint
node --test scripts/mobilePlatform.test.mjs
```

Compiled web files go to `dist`. To preview them locally on the configured
frontend port, stop Vite first, then run:

```powershell
npm.cmd run preview -- --host 127.0.0.1 --port 5173
```

Open `http://127.0.0.1:5173`. The backend and ML service still need to run.

To generate just the frontend assets used by Android:

```powershell
npm.cmd run build:android
```

### Backend

From the repository root, these focused regression tests do not require a live
database:

```powershell
Set-Location Backend
node --test tests/reportScheduling.test.js tests/usersAudit.test.js tests/userDeletion.test.js
```

For the broader suite, use a separate test database:

```powershell
node --test tests/*.test.js
```

The broader suite includes a financial-rules test that connects to MySQL and
runs a migration; other database integration tests are opt-in. Do not treat the
whole suite as a read-only health check against business data.

### Android

From `android`, with the JDK/SDK environment configured as shown above:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

For the generated screen-check procedure, see [Android verification](android/README.md).
Local implementation status and acceptance limits are recorded in
[migration status](android/MIGRATION_STATUS.md).

### ML

From `ML`:

```powershell
.\venv\Scripts\python.exe -m pip check
.\venv\Scripts\python.exe -m py_compile serve.py features.py train_model.py
```

## Stop and restart

- Press **Ctrl+C** in each service terminal to stop Vite, Express, or Flask.
- Closing the Android app does not stop the services running on the computer.
- To restart daily, run the same three startup commands. Do not recreate the
  database, restore the snapshot, reinstall all dependencies, or retrain the model
  on every launch.
- Restart Express after changing `Backend/.env`, Flask after changing `ML/.env`
  or its model, and Vite after changing the root `.env`.
- Rebuild/reinstall Android after frontend changes or an Android API-address change.

## Database backups and upgrades

### Capture a newer snapshot

Stop application writers, then from `Backend`:

```powershell
npm.cmd run seed:capture
```

This exports the configured database and replaces the private
`db/snapshots/current.sql` only after a successful export. Normal startup never
refreshes the backup. Store a secure copy separately from Git, along with
additional uploads and the model if needed for transferring the installation.

### Upgrade an older schema

Apply migrations only when the restored schema needs them. Back up before
upgrading; do not run every migration as a daily startup step. From `Backend`:

| Older feature/schema being upgraded | Command |
| --- | --- |
| Company registration fields | `npm.cmd run migrate:company-registration` |
| Business Profile company name | `npm.cmd run migrate:company-profile` |
| Vehicle rate matrix | `npm.cmd run migrate:vehicle-rates` |
| Historical delivery vehicle details | `npm.cmd run migrate:delivery-vehicle-snapshot` |
| `V-001` vehicle identifiers | `npm.cmd run migrate:vehicle-ids` |

The company-registration migration removes legacy DTI/SEC, DOE license, and
city/municipality columns and their values. The company-profile migration
preserves the company name while renaming `FullName` to `CompanyName`, and removes
unused CompanySettings `Branch`, `Warehouse`, and `Brand` columns.

Vehicle-rate edits are saved using **Save the matrix**; Cancel discards the draft.
Deleted rate configurations are physically removed, while delivery transactions
retain their independently saved vehicle rates. Older transactions without a
snapshot display `Not recorded`. Additional migration scripts are listed in
`Backend/package.json`; use them for the corresponding schema upgrade.

## Troubleshooting

| Problem | What to check |
| --- | --- |
| `npm.ps1` cannot be loaded | Use `npm.cmd` in PowerShell. |
| `vite` or `oxlint` is not recognized | Run `npm.cmd ci` from the repository root. |
| Backend package/module is missing | Run `npm.cmd --prefix Backend ci` from the root. |
| API fails before listening | Check `Backend/.env`, MySQL access, and snapshot/seed output. |
| MySQL connection fails | Verify host, port, username, password, database name, TLS, and whether the server is running/reachable. |
| `mysql` / `mysqldump` is not found | Install MySQL client tools or set their executable paths in `Backend/.env`. |
| Current database snapshot is missing | Obtain the private `current.sql` separately; GitHub does not contain it. |
| Restored records are missing | Confirm both `.env` files target the intended database; avoid replacing an existing review database name with a sample name. |
| `No trained model found` | Copy a compatible trusted `ML/restock_model.pkl`, or train once using the ML virtual environment. |
| `ModuleNotFoundError` in ML | Install `ML/requirements.txt` using `ML/venv/Scripts/python.exe -m pip`; run the same Python environment for Flask. |
| ML health works but model is not loaded | Start with `python serve.py` using the virtual environment; confirm the model file and restart Flask. |
| Restocking uses heuristic fallback | Check Flask health, `ML_SERVICE_URL`, the model, and the backend's ML warning output. |
| Port `4000`, `5001`, or `5173` is occupied | Use the already-running correct service, or stop the conflicting process before restarting. |
| Browser CORS error | Match `CORS_ORIGIN` to the browser's origin and restart Express. Android's bundled origin is explicitly allowed by the server. |
| Android loads UI but cannot load data | Start Express, verify its health, and use `10.0.2.2` for the emulator or the computer's LAN address for a phone. |
| Android photo placeholders | Check that `Backend/uploads` contains the referenced images, that Express serves them, and that the installed APK includes the current image-routing fix. |
| Frontend changes are absent in Android | Rebuild and reinstall the APK; restarting Vite alone does not update bundled files. |
| Gradle cannot find Java or the SDK | Set an installed JDK 17/21, install SDK Platform/Build Tools 35, and configure the SDK location in Android Studio or local properties. |
| Android settings directory is not writable | Set `ANDROID_USER_HOME`, `ANDROID_PREFS_ROOT`, and `GRADLE_USER_HOME` to the project-local directories shown in the build commands. |
| Emulator will not boot or is very slow | Enable hardware virtualization and a supported emulator accelerator; run `emulator.exe -accel-check` from the SDK emulator directory. |
| `adb` reports `offline` or the device is booting | Wait for Android to reach the home screen, then retry `adb devices` and installation. |
| APK update signatures do not match | Rebuild with the original debug keystore. Uninstalling the old debug app also permits a fresh install, but removes its local session and held carts. |
| A module is hidden | Check the signed-in account's role and module permissions; drivers are restricted to their delivery queue. |
| Email or password reset fails | Verify SMTP configuration and `FRONTEND_URL` / `FRONTEND_BASE_PATH`; restart Express. |

For deeper service details, see [Android setup](android/README.md) and
[ML setup](ML/README.md).
