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

