# AGENTS.md

## Project Identity & Release Status
- **App Name**: EchoWithin Mobile (Android)
- **Target**: Production-ready, F-Droid compliant note application.
- **Production Backend**: `https://echowithin.xyz/`
- **API Prefix**: All blueprint routes in `api.py` are under `/api/v1/`.
- **License**: AGPL-3.0 (F-Droid compatible).

## Technical Architecture (Clean Architecture + MVVM)
- **UI Layer**: Jetpack Compose + Material 3.
- **State Management**: `ViewModel` + UI state classes.
- **Dependency Injection**: Factory-based ViewModel injection.
- **Data Layer**:
    - **Local**: Room DB (SQLite) for offline-first capability.
    - **Remote**: Retrofit + OkHttp for REST API.
    - **Sync**: WorkManager for background synchronization.
- **Authentication**: Native app token authentication via `X-App-Token` header and httpOnly cookies. The app uses an auto-reauth flow via the `/api/v1/app_reauth` endpoint to revive sessions using the stored token.

## Backend Integration Details
### Core Endpoints (`/api/v1/`)
- **Auth**: `/login`, `/register`, `/confirm/<email>`, `/logout`, `/app_reauth` (Session revival).
- **Notes**: `/notes` (GET), `/notes/create` (POST), `/notes/edit/<id>`, `/notes/delete/<id>`.
- **Search**: `/personal_post/search?q=<query>` (Meilisearch integration).
- **Community**: `/communities`, `/community/create`, `/community/join`, `/community/<id>/notes`.
- **App Lock / Security**: `/app_lock/check_status` (GET), `/app_lock/setup` (POST), `/app_lock/verify` (POST), `/app_lock/remove` (POST).

### Data Models
- **Auth**: `LoginRequest`, `RegisterRequest`, `ConfirmRequest`.
- **Notes**: `NoteDto`, `CreateNoteRequest`, `UpdateNoteRequest`.
- **Search**: `SearchResultsDto`, `SearchHitDto`.

## Mobile UI/UX Principles (Web → Mobile Transform)
1. **Modals → Full-Screen Pages**: Note editor, Premium upgrade, and Settings are dedicated screens, not popups.
2. **Sidebar → Bottom Navigation**: Main areas (Home, Search, Premium, Settings) belong in the `NavigationBar` (styled using custom Material Icons).
3. **Popovers → Bottom Sheets**: Use `ModalBottomSheet` for secondary actions like sharing or comment options.
4. **Design Integration**: High-contrast modern dark mode matching the web platform colors.

## Design Tokens (Material 3 - Redesigned)
- **Primary**: Brand Orange (`#FF7A00`)
- **Secondary**: Brand Amber (`#FFB000`)
- **Background**: Dark Background (`#0F1419`)
- **Surface**: Dark Surface (`#1A1F2E`)
- **Text Color**: Light Gray (`#F3F4F6` / `#E5E7EB`)
- **Typography**: SansSerif with custom weights (Medium/Bold/Black) and proper typography roles.
- **Spacing**: 16dp standard padding, 12dp/16dp card rounded corner radius.

## Development & Tooling Conventions
- **Version Catalog**: All dependencies in `gradle/libs.versions.toml`.
- **Build Config**: `API_BASE_URL` defined in `app/build.gradle.kts`.
- **Target SDK**: Android 15+ (API 35/36), Min SDK 24.
- **Navigation**: `AppNavGraph.kt` handles all routes.

## Development Workflow for Agents
1. **Understand Specs**: Read `MOBILE_UI_UX_AGENT_GUIDE.md` and `WEB_VS_MOBILE_UI_COMPARISON.md`.
2. **Implementation**: Build UI in `presentation/screens/`, wire `ViewModel`, and add to `NavGraph`.
3. **Verification**: Verify layout targeting, Material 3 compatibility, dark mode styling, and server sync.

## Reference Documentation
- **Architecture**: `FDROID_NOTE_APP_SPECIFICATION.md`.
- **UI Guide**: `MOBILE_UI_UX_AGENT_GUIDE.md`.
- **Transform Rules**: `WEB_VS_MOBILE_UI_COMPARISON.md`.
- **Checklist**: `SCREEN_IMPLEMENTATION_QUICK_REF.md`.
- **Index**: `INDEX_ALL_DOCUMENTATION.md` for a full list of docs.

## Recent Production Bug Fixes & Refactoring (May 2026)
1. **Backend App Lock PIN integration (`api.py`)**:
   - Fixed `/api/v1/app_lock/check_status` (in Flask `api.py`) to query MongoDB for `app_lock_pin_hash`.
   - Returns the dynamic `has_pin: true/false` flag across all JSON branches. This resolves `HTTP 401` by letting the app verify PIN lock status correctly and prompt for unlock instead of setup.
2. **Note Editor Duplication Fix (`NoteEditorScreen.kt` & `AppNavGraph.kt`)**:
   - Refactored the Editor screen to split the title (the first line) from the body text on load.
   - Concatenates the title and body on save (`"$title\n$content"`) to ensure in-place updates. This resolves the duplication issue where saving created a new note.
   - Handled navigation parameter parsing so that literal `"null"` strings are properly parsed as Kotlin `null` in the NavGraph.
3. **Advanced Compose Markdown Renderer (`NoteDetailScreen.kt`)**:
   - Implemented a premium, custom inline markdown formatter (`renderMarkdown`) to support Headings (large Brand Orange), bold (`**`), italics (`*`), blockquotes (`>`), and inline code (`` ` ``).
   - Stripped the first line of content dynamically on the detail screen to prevent redundant title display since it is already showcased as a beautiful header.
4. **Meilisearch Highlight Snippet Fix (`SearchScreen.kt`)**:
   - Refactored the global `stripMarkdown` helper to robustly clear all formatting characters globally (`*`, `_`, `~`, `` ` ``), completely avoiding balanced-pair issues caused by Meilisearch cropping mid-sentence.
   - Highlighted Meilisearch hits with `<mark>` tags seamlessly as premium bold Brand Orange.
5. **Clean Production client (`ApiClient.kt`)**:
   - Completely deleted references and imports to the deprecated `FakeEchoWithinApiService` to ensure it is targeted strictly at production.

### Model: opencode/minimax-m3-free
**Date:** 2026-06-05
**Changes:**
- **Note preview takes full height (scrollable)** — Removed the `heightIn(min = 100.dp, max = 500.dp)` cap on the content card in `NoteDetailScreen.kt`. The outer `verticalScroll` on the screen Column now carries the user through long notes instead of clipping them at 500.dp. Added `min = 160.dp` on the body so a one-liner still looks balanced, and added bottom padding so the last line never sits under the sticky action bar.
- **Sticky bottom action bar** — Restructured `NoteDetailScreen` so the layout is `Box { Column(verticalScroll) { ... }; Surface(align = Alignment.BottomCenter) { ... } }`. The Surface hosts the primary reading actions (Edit / Copy / Share / History) that are always reachable, while secondary actions (Sync / Lock / Delete) remain in a slim inline row inside the scrollable content. The Surface must be a *direct* child of the outer Box so `BoxScope.align` resolves — earlier attempts placed it inside the inner Column and triggered `'fun Modifier.align(...)' cannot be called in this context with an implicit receiver` at compile time.
- **KaTeX WebView is non-scrolling** — `WebView` settings now use `LayoutAlgorithm.TEXT_AUTOSIZING`, disabled vertical/horizontal scrollbars, and `OVER_SCROLL_NEVER` so the rendered math grows with its content rather than nesting an extra scroll surface on top of the screen's `verticalScroll`.
- **Readability pass** — Body `Text` bumped to `17.sp / 26.sp line height` with horizontal and vertical padding. Combined with the removed height cap, a single screenful is much more readable on small phones.
**Files touched:** `app/src/main/java/com/example/echowithin/presentation/screens/NoteDetailScreen.kt`, this `AGENTS.md`.
**Verification:** `./gradlew.bat compileDebugKotlin` and `./gradlew.bat assembleDebug` both pass. Only a pre-existing `LocalClipboardManager` deprecation warning remains.
**Privacy Note:** No data exposure changes — all actions delegate to existing handlers; Lock still requires the user's stored PIN.

### Model: opencode/minimax-m3-free
**Date:** 2026-06-06
**Changes:**
- **Wrong PIN no longer logs the user out** — Root cause was a mismatch between the server's `/api/v1/app_lock/verify` endpoint (which returned **HTTP 401** for an incorrect PIN) and the Android client's global 401 interceptor (`ApiClient.kt` line 189-201) which treats *any* 401 on a non-auth endpoint as "session expired" and silently signs the user out. The fix is on the server: wrong-PIN now returns `200 OK` with `{"success": false, "error": "Incorrect PIN."}`. The client already handles `success=false` correctly (sets `lockError`, doesn't clear the session). The error is no longer an authentication failure; it's a validation failure.
- **"Mark all as read" actually works now** — The Activity tab's "Mark all as read" button was calling two endpoints that did not exist on the backend (`/api/posts/mark-all-read` and `/api/activity/mark_read`), so the requests 404'd and were silently caught in the `catch (e: Exception)` block. Added both endpoints to `api.py`, backed by a new `activity_read_conf` Mongo collection. `api_my_commented_activity` now upserts per-(user, comment) read-flag rows on every load so the unread set is well-defined; the two mark-* endpoints flip `read_at` from `null` to now. On the client, the button now shows an in-line spinner while the request is in flight (new `markingAllRead` UI state), the badge clears optimistically before the request completes, and a "Cleared N items" toast confirms the result.
- **Shared Links tab is back** — The previous implementation called `/api/v1/notes/shares/<noteId>` in a loop over every local note. That failed silently for `local_*` IDs (which aren't valid ObjectIds), and the tab went empty any time the local cache didn't include a note that owned a share. Added a new server endpoint `/api/v1/notes/shares` (no path arg) that returns every active share the current user owns in one round-trip, with a decrypted `note_title` so the cards render without a second fetch. The Android `loadActiveShares()` now calls the new endpoint first and only falls back to the per-note loop if the server is too old. If the server returns shares for notes the local cache doesn't have, the screen synthesises a placeholder note so the user can still see and revoke their links.
- **Note detail action bar reorganised** — Promoted **Lock** from the secondary inline row (where it was easy to miss) into the always-visible sticky bottom bar alongside Edit / Copy / Share. The label flips to "Locked" when the note is already protected, and a small toast ("Note locked" / "Note unlocked") confirms the change. The inline row now hosts Sync (only on saved copies), History, and Delete. Premium-only lock actions still show the upsell toast on tap, but the button is now visible by default.
- **Offline-first overhaul**:
  - New `NetworkMonitor` (`data/network/NetworkMonitor.kt`) wraps `ConnectivityManager` and exposes a Compose-friendly `StateFlow<Boolean>`. Registered once in `EchoWithinApp` via `ensureRegistered(context)` so the first frame is correct.
  - New `OfflineBanner` component (`presentation/components/OfflineBanner.kt`) slides down from the top of the home screen when the device is offline. The copy dynamically says "N changes will sync when you reconnect" when the local DB has pending writes, and "You're offline. Local notes are still available." otherwise. Has a "Retry" CTA.
  - New "Synced Xm ago" label in the top app bar (hidden when the timestamp is 0 or the device is offline).
  - `NotesViewModel` now exposes a `syncTrigger: StateFlow<Long>` and a `pendingSyncCount` derived from `NoteDatabaseHelper.getPendingNotes()`. `onConnectivityChanged(isOnline)` debounces the reconnect logic and pings the trigger so a single sync is launched when connectivity returns, even if the home screen is not the foreground route. The first `loadAllData` after a cold launch now seeds the pending counter too.
  - Optimistic UI for `markAllNotificationsAsRead` — the badge clears instantly, the server call happens in the background, and the final state is reconciled.
- **Ephemeral toasts via `ephemeralMessage`** — `NotesViewModel` now exposes a `ephemeralMessage: String?` that the UI consumes in a `LaunchedEffect` and immediately clears, so success / failure toasts ("Synced with the server", "Cleared N items", etc.) survive recomposition but are shown exactly once.
- **App Lock screen shake-on-error** — `AppLockScreen.kt` now runs a 7-step horizontal-shake animation (4 oscillations, 70dp peak, FastOutSlowInEasing) every time a new `lockError` appears. Deduped by error text so repeated recompositions don't re-fire it. The PIN card border turns `ErrorRed` while the error is on screen, and the `OutlinedTextField` flips to `isError = true` so the standard Material 3 red treatment is consistent with the rest of the form.
- **Lucrative feature roadmap** — `docs/FEATURE_ROADMAP.md` added with 8 quick wins, 7 mid-size bets, 6 moonshots, and a 5th "anti-churn" section, all written against the existing backend primitives (`note_attachments_conf`, `note_shares_conf`, `note_versions_conf`, Fernet encryption) so every idea is implementable without a database migration.
**Files touched:** `app/src/main/java/com/example/echowithin/data/network/NetworkMonitor.kt` (new), `app/src/main/java/com/example/echowithin/data/network/ApiClient.kt` (unchanged, the server fix makes the 401 interceptor safe again), `app/src/main/java/com/example/echowithin/data/model/ApiModels.kt` (added `ActiveShareDto`, `ActiveSharesResponseDto`), `app/src/main/java/com/example/echowithin/data/network/EchoWithinApiService.kt` (added `getActiveShares`), `app/src/main/java/com/example/echowithin/data/network/FakeEchoWithinApiService.kt` (implement `getActiveShares`), `app/src/main/java/com/example/echowithin/presentation/components/OfflineBanner.kt` (new), `app/src/main/java/com/example/echowithin/presentation/components/Branding.kt` (unchanged), `app/src/main/java/com/example/echowithin/presentation/EchoWithinApp.kt` (register NetworkMonitor, hoisted to early in the composition), `app/src/main/java/com/example/echowithin/presentation/navigation/AppNavGraph.kt` (collect `NetworkMonitor.isOnline`, observe `syncTrigger`, surface `ephemeralMessage` as a toast, plumb `isOnline` / `pendingSyncCount` / `lastSyncedAt` / `markingAllRead` to `HomeScreen`), `app/src/main/java/com/example/echowithin/presentation/screens/HomeScreen.kt` (offline banner, "Synced 5m ago" pill, mark-all-read spinner, new `markingAllRead` param, `formatLastSynced` helper), `app/src/main/java/com/example/echowithin/presentation/screens/NoteDetailScreen.kt` (lock promoted to sticky bar, inline row now hosts Sync/History/Delete), `app/src/main/java/com/example/echowithin/presentation/screens/AppLockScreen.kt` (shake-on-error, red card border while error present), `app/src/main/java/com/example/echowithin/presentation/viewmodel/NotesViewModel.kt` (new `pendingSyncCount`, `lastSyncedAt`, `markingAllRead`, `lastMarkedReadCount`, `ephemeralMessage`, `syncTrigger`, `onConnectivityChanged`, optimistic mark-all-read, loadActiveShares via new endpoint), `app/build.gradle.kts` (versionCode 7→8, versionName 1.6→1.7), `docs/FEATURE_ROADMAP.md` (new), this `AGENTS.md`.
**Verification:** `./gradlew.bat compileDebugKotlin` and `./gradlew.bat assembleDebug` both pass. Only pre-existing deprecation warnings remain (ScrollableTabRow → PrimaryScrollableTabRow, LocalClipboardManager → LocalClipboard, Icons.Filled.Launch → AutoMirrored.Filled.Launch, TabRow → PrimaryTabRow).
**Privacy Note:** No new data exposure. The new backend endpoints all live under existing `@login_required` decorators; the new list-all-shares endpoint only returns shares whose `owner_id` is the current user; the new activity-read endpoints only flip rows belonging to the current user. NetworkMonitor does not transmit any data off-device.

### Model: opencode/minimax-m3-free
**Date:** 2026-06-06
**Changes:**
- **Sticky bar layout fix in `NoteDetailScreen.kt`** — The four primary actions (Edit / Copy / Share / Lock) at the bottom of the note detail screen used to use `Arrangement.SpaceEvenly` with no width weighting, so the last button (Lock) could be squeezed to near-zero width on narrow phones. That caused the label to wrap letter-by-letter ("L / o / c / k" stacked vertically) and the `RoundedCornerShape(20.dp)` border to collapse into a near-circle. Each button now has `Modifier.weight(1f)`, the arrangement is `spacedBy(6.dp)`, content padding is `PaddingValues(horizontal = 8.dp, vertical = 6.dp)`, the shape is a tighter `RoundedCornerShape(10.dp)`, icons are 16dp, text is 12sp with `maxLines = 1, softWrap = false`. All four buttons get equal width and a clean one-line label on every device size.
- **Duplicate-notes sync fix (the "I have double my notes" bug)** — Root cause: `clearSyncFlags()` (called on logout / 401 / splash token check) was setting `is_synced=0, pending_op="none"` on every already-synced note, and the sync loop's dispatcher treated `pending_op == "none"` as a CREATE, re-pushing every synced note to the server on the next login and producing a fresh duplicate on the server every time. The fix has three parts, in order of how much they actually move the needle:
  1. `NoteDatabaseHelper.getPendingNotes()` now filters out the stale rows up front: `is_synced = 0 AND (pending_op != 'none' OR id LIKE 'local_%')`. A row with a real server UUID and `pending_op = "none"` is treated as "already on the server, just refresh me" and never enters the push loop.
  2. `NotesRepository.syncNotesInternal()` dispatcher was rewritten to key off the **ID prefix**, not `pending_op`. `local_*` ids always push as `create`; real server UUIDs only push when the user explicitly set `edit` / `delete`; the residual (server id + `pending_op = "none"`) is `SKIPPED` and just falls through to the pull step. The old `if (note.pendingOp == "none") "create"` fallback is gone.
  3. `NotesRepository.clearLocalData()` now does a full `clearAll()` instead of `clearSyncFlags()`. On logout / 401 / splash token check, the local cache is wiped completely; the next sync just pulls fresh from the server. The old behaviour was the source of the duplicates. `clearSyncFlags()` is left in place as a debug helper but the Javadoc now warns against calling it from the logout path.
- **One-shot dedup on the server** — `POST /api/v1/notes/dedup` (added in the backend repo) groups the user's notes by their decrypted, lowercased, whitespace-collapsed content and, for each group of 2+, keeps the OLDEST and deletes the rest along with their shares / versions / unlock-notifications / Typesense entries. The endpoint supports a `?confirm=true` query param; without it the call is a dry-run that just reports the groups that *would* be removed. The Android client calls it (with `confirm=true`) after every successful sync. If the server removed anything, the app shows a "Cleaned up N duplicate notes from a previous sync" toast and reloads the list; if nothing was duplicated, the call is a no-op and the user sees no toast.
- **Version bump** — `versionCode 8 → 9`, `versionName 1.7 → 1.7.1`. New APK pushed to `static/downloads/app-debug.apk` and the `static/update-manifest.json` updated to match.
**Files touched:** `app/src/main/java/com/example/echowithin/presentation/screens/NoteDetailScreen.kt` (sticky bar layout), `app/src/main/java/com/example/echowithin/data/local/NoteDatabaseHelper.kt` (`getPendingNotes()` filter, `clearSyncFlags()` Javadoc warning), `app/src/main/java/com/example/echowithin/data/repository/NotesRepository.kt` (rewrote sync dispatcher, `clearLocalData()` → `clearAll()`, new `dedupNotesOnServer()`), `app/src/main/java/com/example/echowithin/data/model/ApiModels.kt` (added `DedupResponseDto`, `DedupGroupDto`), `app/src/main/java/com/example/echowithin/data/network/EchoWithinApiService.kt` (added `dedupNotes(confirm)`), `app/src/main/java/com/example/echowithin/data/network/FakeEchoWithinApiService.kt` (fake `dedupNotes` no-op), `app/src/main/java/com/example/echowithin/presentation/viewmodel/NotesViewModel.kt` (calls `dedupNotesOnServer()` after sync success, surfaces the result as an `ephemeralMessage`), `app/build.gradle.kts` (version bump).
**Verification:** `./gradlew.bat compileDebugKotlin` and `./gradlew.bat assembleDebug` both pass. Only pre-existing deprecation warnings remain.
**Privacy Note:** No new data exposure. The duplicate-detection logic runs server-side, the decrypted plaintext is held in memory only and never returned to the client, and the deletions are scoped to `ObjectId(current_user.id)` (the user can only dedupe their own notes). The client just reads back the `removed_count` and shows a toast.

