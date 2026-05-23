# 🚀 Quick Reference: Screen-by-Screen Implementation Checklist

Use this as your **quick lookup** while building each screen. For detailed specs, refer to the full guides.

---

## Screen 1: HOME SCREEN 🏠

**File**: `presentation/screens/HomeScreen.kt`  
**Route**: `"home"` (bottom nav destination)

### Structure
```
AppBar (title + search icon + menu)
  ↓
LazyColumn (note cards)
  ├─ NoteCard(...)
  ├─ NoteCard(...)
  └─ NoteCard(...)
  ↓
FAB (new note button)
```

### Required Components
- [ ] TopAppBar with title, search icon, menu icon
- [ ] LazyColumn with note cards
- [ ] NoteCard component (reusable)
- [ ] FAB at bottom-right (56dp diameter)
- [ ] Empty state (when no notes)
- [ ] Loading indicator
- [ ] Sync status badge

### Interactions
| Action | Destination |
|---|---|
| Tap note card | `detail/{noteId}` |
| Tap search icon | `search` |
| Tap FAB | `editor` (no noteId = new note) |
| Tap menu icon | Show options (Edit, Delete, etc.) |

### Design Tokens
- Colors: Primary, Background, Surface
- Typography: TitleLarge (card title), BodyMedium (preview)
- Spacing: 16dp padding, 8dp corner radius

---

## Screen 2: NOTE EDITOR 📝

**File**: `presentation/screens/NoteEditorScreen.kt`  
**Route**: `"editor?noteId={noteId}"` (full-screen page, not modal!)

### Structure
```
TopAppBar (back + title + save)
  ↓
Column (scrollable)
  ├─ Title TextField
  ├─ Tags ChipInput
  ├─ MarkdownEditor (with toolbar)
  ├─ Attachments section
  └─ Bottom actions (privacy + share)
```

### Required Components
- [ ] TopAppBar with back button + title + save button
- [ ] Title text field (auto-focused on open)
- [ ] Tags chip input with autocomplete
- [ ] MarkdownEditor component with toolbar (B, I, U, -, *, #)
- [ ] Attachments section (add button + preview)
- [ ] Privacy toggle (Private/Public)
- [ ] Share button (opens bottom sheet)

### Interactions
| Action | Result |
|---|---|
| Tap back | Show "Save changes?" if modified, then popBackStack() |
| Tap save | Call viewModel.saveNote(), show progress |
| Type title | Auto-focus, updates in real-time |
| Type content | Auto-save to local DB every 5 sec |
| Tap share | Show ShareBottomSheet |
| Tap attachment | Open file picker or camera |

### Important
- ✅ **This is a FULL-SCREEN page, NOT a modal**
- ✅ Back button dismisses (not X icon)
- ✅ Save button in AppBar (not bottom)
- ✅ Auto-save to local DB while typing

---

## Screen 3: NOTE DETAIL 👀

**File**: `presentation/screens/NoteDetailScreen.kt`  
**Route**: `"detail/{noteId}"`

### Structure
```
LazyColumn (scrollable)
  ├─ CollapsibleAppBar (title collapses on scroll)
  ├─ Content (markdown rendered)
  ├─ Status badges (Pinned, Locked, Public)
  ├─ Comments section
  └─ Comment input
  ↓
Sticky Action Bar (bottom)
```

### Required Components
- [ ] TopAppBar with back + collapse behavior
- [ ] Full note content rendered as markdown
- [ ] Status badges (pinned, locked, public/private)
- [ ] Comments section (expandable threads)
- [ ] Comment input field
- [ ] Sticky action bar (Like, Comment, Share, Menu)

### Interactions
| Action | Result |
|---|---|
| Scroll up | AppBar expands, shows full title |
| Scroll down | AppBar collapses, title hides |
| Tap like | Toggle like state (show animation) |
| Tap comment | Focus input or expand thread |
| Tap share | Show ShareBottomSheet |
| Tap menu | Show options (Edit, Delete, Report) |
| Tap edit | Navigate to `editor/{noteId}` |

### Design Notes
- AppBar collapses smoothly (Compose LazyColumn + scroll state)
- Comments: Nested threads with expand/collapse
- Action bar: Sticky at bottom (always visible)

---

## Screen 4: SEARCH 🔍

**File**: `presentation/screens/SearchScreen.kt`  
**Route**: `"search"` (full-screen page, bottom nav destination)

### Structure
```
AppBar (back + X/clear)
  ↓
SearchInput (always-focused, keyboard shows)
  ↓
RecentSearches (chips)
  ↓
FilterChips (All, Locked, Public)
  ↓
LazyColumn (results)
  └─ Result cards
```

### Required Components
- [ ] AppBar with back button + X (clear)
- [ ] Full-width search input (always-focused)
- [ ] Recent searches as chips (horizontal)
- [ ] Filter chips (All, Locked, Public, etc.)
- [ ] Search results as cards (same style as home)
- [ ] Results counter ("12 results found")
- [ ] Empty state ("No results")
- [ ] Real-time search (debounced 300ms)

### Interactions
| Action | Result |
|---|---|
| Type in input | Search results update (debounced) |
| Tap recent chip | Search for that query |
| Tap filter chip | Apply filter, update results |
| Tap result | Navigate to `detail/{noteId}` |
| Tap X | Clear search, go to `home` |

### Design Notes
- Input field: Full-width, 14sp font, light gray background
- Recent searches: Horizontal scrollable, closeable chips
- Results: Same card style as home screen
- Text snippets: Highlight search term (bold)

---

## Screen 5: SHARE BOTTOM SHEET 🔗

**File**: `presentation/screens/ShareBottomSheet.kt`  
**Component**: `ModalBottomSheet` (not full-screen!)

### Structure
```
ModalBottomSheet
  ├─ Drag handle
  ├─ Header ("Share Note Title")
  ├─ Link section (copyable)
  ├─ Access level selector (radio)
  ├─ Expiration date picker
  ├─ Password toggle
  ├─ Shared with list
  └─ Stats
```

### Required Components
- [ ] ModalBottomSheet container
- [ ] Drag handle at top
- [ ] Copyable link (text + copy button)
- [ ] Access level radio buttons (View, Edit, Admin)
- [ ] Expiration date picker (Never, 1 week, Custom)
- [ ] Password toggle + secure input
- [ ] Add people button + list of shared users
- [ ] Stats (views, comments)

### Interactions
| Action | Result |
|---|---|
| Drag down | Dismiss sheet |
| Tap copy | Copy link to clipboard (show toast) |
| Change access level | Update in real-time (sync to server) |
| Tap password toggle | Show/hide password input |
| Tap add people | Show people picker dialog |
| Remove user | Delete from share access |

### Important
- ✅ **This is a BOTTOM SHEET, NOT a full-screen page**
- ✅ Draggable, can dismiss with swipe-down
- ✅ Changes auto-save (no buttons needed)
- ✅ Invoked from Note Detail or Editor screen

---

## Screen 6: PREMIUM 💎

**File**: `presentation/screens/PremiumScreen.kt`  
**Route**: `"premium"` (full-screen page, bottom nav destination)

### Structure
```
AppBar (back + title)
  ↓
ScrollColumn
  ├─ Header (tagline)
  ├─ Current plan badge
  ├─ Feature comparison table (horizontal scroll)
  ├─ Pricing info
  ├─ CTA button (Start free trial)
  ├─ Restore purchase link
  └─ FAQ/Help links
```

### Required Components
- [ ] TopAppBar with back button
- [ ] Current plan badge (Free/Premium)
- [ ] Feature comparison table (Free vs Premium columns)
- [ ] Pricing info (KSH 50/month, KSH 450/year)
- [ ] Free trial button (full-width, prominent)
- [ ] Restore purchase button (secondary)
- [ ] FAQ link
- [ ] Contact/Help link

### Interactions
| Action | Result |
|---|---|
| Tap free trial | Start 1-day trial (show confirmation) |
| Trial expires | Show banner on home screen |
| Tap restore | Verify existing purchase |
| Tap FAQ | Open FAQ screen (in-app WebView) |
| Tap contact | Send email (intent to email app) |

### Design Notes
- Comparison table: Horizontal scrollable, full-width
- Pricing: Large, bold text (KSH currency)
- CTA button: Full-width, primary color
- Restoration: For handling app reinstalls

---

## Screen 7: SETTINGS ⚙️

**File**: `presentation/screens/SettingsScreen.kt`  
**Route**: `"settings"` (full-screen page)

### Structure
```
AppBar (back + title)
  ↓
TabRow (Account, Notifications, App)
  ↓
Column (scrollable)
  ├─ Account tab
  │  ├─ Profile settings
  │  ├─ Password change
  │  ├─ Premium status
  │  ├─ Export data
  │  └─ Delete account
  ├─ Notifications tab
  │  ├─ Push notifications toggle
  │  ├─ Email notifications toggle
  │  ├─ Sound/Vibration
  │  └─ Notification types
  └─ App tab
     ├─ Theme (Light/Dark/System)
     ├─ Language
     ├─ Storage usage
     ├─ Version info
     └─ About/Help
```

### Required Components
- [ ] TopAppBar with back button
- [ ] TabRow with 3 tabs
- [ ] Profile input fields (username, email)
- [ ] Change password button
- [ ] Premium status display
- [ ] Export data button
- [ ] Delete account button (red/warning color)
- [ ] Toggle switches (notifications, dark mode)
- [ ] Dropdown selectors (language, theme)
- [ ] Logout button (full-width, secondary)

### Interactions
| Action | Result |
|---|---|
| Edit field | Show inline edit or modal |
| Change password | Go to password change screen |
| Export data | Download JSON file |
| Delete account | Show confirmation dialog |
| Toggle notification | Save preference, sync to server |
| Change theme | Apply immediately |
| Logout | Clear tokens, navigate to login |

---

## Screen 8: LOGIN 🔐

**File**: `presentation/screens/LoginScreen.kt`  
**Route**: `"login"`

### Structure
```
Column (centered)
  ├─ Logo/Title
  ├─ Email TextField
  ├─ Password TextField
  ├─ Login button
  ├─ Google OAuth button (optional)
  ├─ Forgot password link
  └─ Sign up link
```

### Required Components
- [ ] Email input field
- [ ] Password input field
- [ ] Login button (full-width)
- [ ] Google OAuth button (optional, F-Droid consideration)
- [ ] Forgot password link
- [ ] Sign up link
- [ ] Error messages
- [ ] Loading indicator

### Interactions
| Action | Result |
|---|---|
| Enter credentials | Validate locally |
| Tap login | Call API, show progress |
| Login success | Clear tokens, navigate to home |
| Tap forgot password | Go to password reset screen |
| Tap sign up | Go to registration screen |

---

## Bottom Navigation Setup 🧭

**File**: `presentation/screens/MainActivity.kt`

```kotlin
NavigationBar {
    NavigationBarItem(
        icon = { Icon(Icons.Default.Home, null) },
        label = { Text("Notes") },
        selected = currentDest == "home",
        onClick = { navController.navigate("home") }
    )
    NavigationBarItem(
        icon = { Icon(Icons.Default.Search, null) },
        label = { Text("Search") },
        selected = currentDest == "search",
        onClick = { navController.navigate("search") }
    )
    NavigationBarItem(
        icon = { Icon(Icons.Default.Bookmark, null) },
        label = { Text("Saved") },
        selected = currentDest == "saved",
        onClick = { navController.navigate("saved") }
    )
    NavigationBarItem(
        icon = { Icon(Icons.Default.Star, null) },
        label = { Text("Premium") },
        selected = currentDest == "premium",
        onClick = { navController.navigate("premium") }
    )
    NavigationBarItem(
        icon = { Icon(Icons.Default.Person, null) },
        label = { Text("Profile") },
        selected = currentDest == "profile",
        onClick = { navController.navigate("profile") }
    )
}
```

**Destinations**:
1. 🏠 Home (`home`)
2. 🔍 Search (`search`)
3. 📌 Saved (`saved`)
4. ⭐ Premium (`premium`)
5. 👤 Profile (`profile`)

---

## Design System Quick Ref 🎨

### Colors
```kotlin
Primary = Color(0xFF6366F1)          // Indigo
Secondary = Color(0xFF8B5CF6)        // Violet
Tertiary = Color(0xFF10B981)         // Emerald
Error = Color(0xFFDC2626)            // Red
Background = Color(0xFFFAFAFA)       // Off-white
Surface = Color.White
```

### Typography
```kotlin
HeadlineLarge = 32sp bold            // Screen titles
TitleLarge = 22sp bold               // Section headers
BodyLarge = 16sp                     // Main content
BodyMedium = 14sp                    // Secondary
LabelSmall = 12sp semi-bold          // Tags, labels
```

### Spacing
```kotlin
Padding: 16dp (standard), 8dp (compact)
Corner radius: 8dp (cards), 4dp (inputs)
Icon size: 24dp (standard)
Button height: 48dp (minimum)
FAB size: 56dp diameter
```

---

## Testing Checklist ✅

Before marking a screen complete:

```
□ AppBar/navigation present
□ Text readable (min 14sp)
□ Buttons tappable (min 48dp)
□ Works in landscape
□ Empty state shown
□ Loading state works
□ Error state shown
□ Back button works
□ Uses design tokens
□ Colors correct
□ Keyboard dismisses
□ No crashes on rotate
□ Performance good (no lag)
```

---

## Common Code Patterns

### Launching Navigation
```kotlin
// Push new screen (back button returns)
navController.navigate("detail/$noteId")

// Replace current screen (back goes to previous)
navController.navigate("home") {
    popUpTo("home") { inclusive = true }
}
```

### Showing Bottom Sheet
```kotlin
var showShare by remember { mutableStateOf(false) }

if (showShare) {
    ShareBottomSheet(
        onDismiss = { showShare = false }
    )
}

Button(onClick = { showShare = true }) {
    Text("Share")
}
```

### ViewModel + State
```kotlin
@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val getNoteUseCase: GetNoteUseCase
) : ViewModel() {
    
    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()
    
    fun loadNote(id: String) {
        viewModelScope.launch {
            _note.value = getNoteUseCase(id)
        }
    }
}
```

---

## Screen 9: COMMUNITIES 🌐

**File**: `presentation/screens/CommunitiesScreen.kt`  
**Route**: `"communities"` (tab or full-screen page)

### Structure
```
TopAppBar (back + title + create button)
  ↓
TabRow (My Joined, Browse All)
  ↓
LazyColumn (community lists)
  ├─ Pinned/Joined community cards
  └─ Discover community cards (with Join CTA)
```

### Required Components
- [ ] TopAppBar with back arrow and "+" create community button
- [ ] TabRow with "My Joined" and "Browse All" destinations
- [ ] CommunityCard component with badge status (Public/Premium Only)
- [ ] Search input at top of Browse All tab
- [ ] Join/Request Access button inside cards for non-members

### Interactions
| Action | Result |
|---|---|
| Tap tab | Toggle list view |
| Tap community card | Navigate to `community/{communityId}` |
| Tap Join CTA | Dispatch Join API call, update state to Joined |
| Tap "+" | Open Create Community Screen |

---

## Screen 10: DIRECT MESSAGING (CHAT) 💬

**File**: `presentation/screens/ChatScreen.kt`  
**Route**: `"chat/{userId}"`

### Structure
```
ChatAppBar (back + avatar + user name + presence status)
  ↓
LazyColumn (chat messages list)
  ├─ Incoming message bubbles (left-aligned)
  └─ Outgoing message bubbles (right-aligned)
  ↓
Sticky Bottom message input bar (attachments + text input + mic + emoji)
```

### Required Components
- [ ] ChatAppBar with user avatar, name, and green online circle
- [ ] LazyColumn that auto-scrolls to bottom on load/new message
- [ ] Chat bubble layout with 8dp corner radius and readable text
- [ ] Time and date section dividers
- [ ] Sticky message input field with attachment clip, voice mic, emoji icons

### Interactions
| Action | Result |
|---|---|
| Tap back | popBackStack() |
| Enter text & tap send | Call Send API, clear input, append bubble |
| Long-press bubble | Show reaction toolbar and options (Copy/Reply/Delete) |
| Hold mic button | Record voice note audio |
| Tap attach clip | Open file/photo picker |

---

## Deploy Checklist 🚀

Before submitting to F-Droid:

```
□ All screens implemented
□ All features tested
□ No crashes on common actions
□ Privacy policy in-app
□ No tracking/analytics
□ Open-source dependencies only
□ Target API 35
□ Min API 26
□ License: AGPL-3.0
□ README.md complete
□ CONTRIBUTING.md present
□ Source code on GitHub (public)
□ Gradle wrapper locked
□ R8 minification enabled
□ ProGuard rules set
```

---

**This quick reference is your go-to guide while building. Use it alongside the detailed implementation guide. Good luck! 🚀**
