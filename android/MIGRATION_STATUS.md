# Migration status

## Current Android frontend (October 19, 2026)

The installed Android app now runs the bundled React application from `src` in `WebAppActivity`. This replaces the separate simplified Compose screens as the active UI. Web and Android use the same module components, images, forms, validation and API workflows. The Compose files below are retained as historical reference.

Phone layouts add a collapsible navigation drawer, two-column POS catalog, scrollable tables, single-column settings and registration forms, and constrained dialogs. Android provides document selection, CSV/Excel/PDF saving and receipt printing. Public backend product images use a local asset-origin proxy, preventing debug HTTP images from being blocked or upgraded to HTTPS. No Vite server is required by the installed APK.

Verified: all 12 module screens rendered at 411 CSS pixels without page overflow or JavaScript exceptions; all 18 POS photos decoded with no placeholders; product forms and brand/category managers opened; adding and holding a cart worked; API reads inside WebView returned HTTP 200. Shared frontend build/lint and Android build/lint passed; the 14 existing Android JVM tests passed. Backend user/audit tests and report-scheduling regression tests passed. A separate regression test verifies exact Android download bytes despite immediate object-URL revocation.

The local database remains the separate snapshot review database. No sale, stock movement, supplier purchase, payment or delivery completion was submitted during these UI checks. Matching shared source does not replace end-to-end acceptance of every business mutation, role, SMTP delivery or physical-device behavior. Release signing and a production HTTPS endpoint remain deployment work. See `README.md` for build and runtime details and `review/parity-check.json` for generated local screen-check results.

Final platform checks: the inventory CSV template saved to Android Downloads and its 86 bytes matched the original template; import opened the Android document picker; a diagnostic receipt opened Android print preview. Held-cart testing confirmed add → hold → restore with the item restored, without checkout. Backend report scheduling now allocates and returns the required `REP-…` identifier. Report validation and identifier regression tests pass.

## Historical native baseline

This is a functional native migration baseline. It is **not a production parity sign-off**.

## Implemented

- SDK 35 Android Studio project, Gradle wrapper, JDK 17 bytecode/JDK 17–21 build compatibility, Compose Material 3, typed Navigation Compose, Hilt, Retrofit/OkHttp, Kotlin serialization, DataStore and StateFlow.
- Authentication, registration with inline validation and original legal documents, forgot/reset password, session verification/expiry, permission-filtered drawer and driver delivery queue.
- Dashboard metrics, sales trend drawing and activity.
- Catalog listing, product images and product create/edit forms.
- Inventory listing, stock adjustment, stock-in/out and transfer forms, transaction and transfer lists.
- POS cart and walk-in/pickup/delivery checkout contracts, payment-method selection, references, discount, warehouse, customer, pickup scheduling, delivery vehicle/driver selection and cylinder-return quantities.
- Customer listing/creation; order listing; pickup readiness and claims; delivery dispatch/failure/completion, COD collection and cylinder returns.
- Restocking generation/rescoring, confidence visualization and quantity customization.
- Purchase-order listing, creation, send/confirmation/dispatch actions, payment form and full/partial receiving form.
- Supplier and user create/edit/delete forms, administrator module switches; sales listing/import/delete; report scheduling/generation; sales CSV export/import and data logs.
- Profile/logo, operations, financial, receipt and system settings.
- Connectivity banner, cancellation-aware errors, loading panels, safe timeout messaging, disabled automatic network retries for transactions.

## Remaining parity work

- Dedicated DTOs for the remaining raw JSON detail/history/admin responses; the current route checker does not validate every JSON field.
- Screenshot comparison against a running web application, tablet grid refinements, advanced filters/pagination and all detail/history dialogs.
- Held-cart flows, complete receipt/PDF preview and print integration, inventory-import UI, full data-export options and annual report controls.
- Financial settlement/refund screens and delivery assignment editor (the API declarations and some request DTOs are present).
- Complete role/branch/warehouse, brand/category/supplier-type and vehicle-rate management screens.
- Exact validation parity for every administrative field, saved-state restoration for every in-progress form/cart, and deployment-specific HTTPS password-reset app links.
- Live integration against the configured private database, SMTP service and ML service; release signing and release/device acceptance testing.

## Local verification and service limitations

The project builds a debug APK; the JVM suite has 14 tests. Android lint passes with dependency-update notices for the pinned SDK-35-compatible library set. The final task result is recorded in the build output and generated reports under `app/build`.

Live business transactions were not exercised. For the subsequent local review, the user supplied `C:\Users\user\LPG_Capstone_jfsalivio\Backend\db\snapshots\current.sql`. It was restored into a new database named `gastrack_android_review_20261019060038` (44 tables); the source database was not changed. Private `Backend/.env` and a private snapshot copy are now configured and ignored by Git.

The API is running on port 4000. The ML service uses the original project's Python environment on port 5001; health reports that the existing model is loaded, and a sample prediction succeeded. Protected read checks for `/auth/me`, `/dashboard/summary`, `/products`, `/inventory`, `/orders`, `/deliveries` and `/restocking` returned HTTP 200 against the restored database. The model was not retrained.

On October 19, 2026, WHPX acceleration became available. `Medium_Phone_API_37.0` booted, the updated debug APK was installed, and the native sign-in screen was verified through UI Automator. The emulator can reach the host at `10.0.2.2`. A Pixel Launcher ANR dialog was dismissed; the app returned to the foreground. Screenshot: `emulator-gastrack.png`. Password login and business transactions remain unverified pending existing account credentials.

The undefined `target`/`changes` references in status-filtered `/users` and `/users/activity-log` handlers were removed. Both routes and seven core authenticated reads returned HTTP 200 against the review database using `node scripts/checkAndroidReview.cjs` from `Backend`. This script creates a short-lived local session and does not verify password login or write business records. Eight backend user/audit tests passed. MySQL connectivity, API health and ML health with the loaded model passed. Web production build and lint passed with existing warnings. The Android JVM suite passed all 14 tests. Report creation still needs review because it omits an explicit ID while the checked-in compliance schema uses a VARCHAR primary key.

## Reference documentation

Final October 19 verification: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` completed successfully. The installed APK includes the guard that skips permission/session refresh before a token exists, avoiding a false session-expiry message on first launch.

Module navigation follow-up: disabled destination-ID state restoration for the shared `ModuleRoute` destination, which could restore Dashboard arguments after selecting another module. Rebuilt and installed the debug APK while preserving the session. Emulator checks opened POS, Products, Inventory, Sales and Order and Delivery; Inventory and Sales displayed backend records.

- [Compose BOM guidance](https://developer.android.com/develop/ui/compose/bom)
- [Hilt Gradle setup](https://dagger.dev/hilt/gradle-setup.html)
- [Gradle 8.11.1](https://docs.gradle.org/8.11.1/release-notes.html)
- [Coil 2.7.0 release](https://github.com/coil-kt/coil/releases/tag/2.7.0)
