# Mobile UI/UX Implementation Instructions for Agents

> **Purpose**: These instructions guide you (the AI agent) on how to implement the EchoWithin mobile app's UI to match the web design while optimizing for mobile. Use this alongside `MOBILE_UI_UX_AGENT_GUIDE.md`.

---

## Quick Start: Core UI Principles

When you're building any screen, remember these **three rules**:

1. **Full-page navigation, not modals**
   - Note editor → Its own screen (not a popup)
   - Premium upgrade → Full page (not modal)
   - Settings → Full screen with tabs
   
2. **Bottom navigation for main areas**
   - Home (notes), Search, Saved, Premium, Profile
   - Only 5 items max
   - Each item = separate destination in `NavGraph`

3. **Bottom sheets for secondary actions**
   - Share button → Shows `ModalBottomSheet`
   - Comments options → Bottom sheet menu
   - File picker → Native bottom sheet

---

## Mandatory Screen Implementations

### 1️⃣ Home Screen (First Implementation)
**Location**: `presentation/screens/HomeScreen.kt`

```kotlin
@Composable
fun HomeScreen(
    viewModel: NotesListViewModel,
    onNoteClick: (String) -> Unit,
    onNewNoteClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    // MUST HAVE:
    // ✅ AppBar with title + search icon + menu
    // ✅ Notes list as full-width cards
    // ✅ FAB (floating action button) for new note
    // ✅ Bottom navigation visible
    // ✅ Sync status indicator
    // ✅ Empty state if no notes
}
```

**Do**:
- ✅ Use LazyColumn for note cards
- ✅ Each card is clickable → onNoteClick(id)
- ✅ FAB onClick → onNewNoteClick()
- ✅ Search icon → onSearchClick()

**Don't**:
- ❌ Show editor as modal
- ❌ Hide navigation while loading
- ❌ Use nested ListViews (performance)

---

### 2️⃣ Note Editor Screen (Dedicated Page)
**Location**: `presentation/screens/NoteEditorScreen.kt`

**This is KEY**: The editor is a **full-screen destination**, not a modal overlay.

```kotlin
@Composable
fun NoteEditorScreen(
    noteId: String?,  // null = new note, otherwise = edit
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
    onNavigateToShare: (String) -> Unit
) {
    // MUST HAVE:
    // ✅ AppBar with back button + title + save button
    // ✅ Title text field (auto-focused)
    // ✅ Tags chip input
    // ✅ Markdown editor with toolbar
    // ✅ Attachments section
    // ✅ Bottom action bar (privacy + share)
}
```

**AppBar Implementation**:
```kotlin
TopAppBar(
    title = { Text("Edit Note") },
    navigationIcon = {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
    },
    actions = {
        IconButton(
            onClick = { viewModel.saveNote() },
            enabled = viewModel.hasChanges.value
        ) {
            Icon(Icons.Default.Check, "Save")
        }
    }
)
```

**Navigation Connection**:
```kotlin
// In NavGraph
composable(
    route = "editor?noteId={noteId}",
    arguments = listOf(
        navArgument("noteId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )
) { backStackEntry ->
    val noteId = backStackEntry.arguments?.getString("noteId")
    NoteEditorScreen(
        noteId = noteId,
        viewModel = hiltViewModel(),
        onBack = { navController.popBackStack() },
        onNavigateToShare = { noteId ->
            navController.navigate("share_sheet/$noteId")
        }
    )
}
```

**Critical**: 
- Back button dismisses (not modal close)
- Save button only enabled if changed
- Auto-save to local DB every 5 seconds

---

### 3️⃣ Note Detail Screen
**Location**: `presentation/screens/NoteDetailScreen.kt`

```kotlin
@Composable
fun NoteDetailScreen(
    noteId: String,
    viewModel: NoteDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    // MUST HAVE:
    // ✅ Collapsing AppBar (title collapses on scroll)
    // ✅ Full note content rendered as markdown
    // ✅ Comments section (bottom sheet or inline)
    // ✅ Sticky action bar (like, comment, share, menu)
    // ✅ Status badges (pinned, locked, public)
}
```

**Collapsing AppBar**:
```kotlin
var expandedState by remember { mutableStateOf(true) }

LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = scrollState
) {
    item {
        // Show full title + metadata when expanded
        if (expandedState) {
            Text(note.title, fontSize = 32.sp)
            Text(note.updatedAt, fontSize = 12.sp)
        }
    }
    // ... content
}

// Detect scroll to collapse
LaunchedEffect(scrollState.firstVisibleItemIndex) {
    expandedState = scrollState.firstVisibleItemIndex == 0
}
```

---

### 4️⃣ Search Screen (Full-Page, Not Embedded)
**Location**: `presentation/screens/SearchScreen.kt`

**Why full-page?** Mobile users need bigger tap targets for search input.

```kotlin
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onResultClick: (String) -> Unit,  // Navigate to detail
    onBack: () -> Unit
) {
    // MUST HAVE:
    // ✅ Full-width search input (always focused, keyboard shows)
    // ✅ Recent searches as chips
    // ✅ Filter chips (All, Locked, Public)
    // ✅ Results as cards (same as home)
    // ✅ Real-time search (debounced 300ms)
}
```

**Input Configuration**:
```kotlin
LaunchedEffect(Unit) {
    focusRequester.requestFocus()
}

BasicTextField(
    value = searchQuery,
    onValueChange = { query ->
        searchQuery = query
        viewModel.searchNotes(query)  // Debounced
    },
    modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester),
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search
    )
)
```

---

### 5️⃣ Share Bottom Sheet (Not Full Screen)
**Location**: `presentation/screens/ShareBottomSheet.kt`

**Key**: Use `ModalBottomSheet`, NOT a full-screen page.

```kotlin
@Composable
fun ShareBottomSheet(
    noteId: String,
    onDismiss: () -> Unit,
    viewModel: ShareViewModel
) {
    // MUST HAVE:
    // ✅ Drag handle at top
    // ✅ Copyable link
    // ✅ Access level selector (radio buttons)
    // ✅ Expiration date picker
    // ✅ Password toggle
    // ✅ Add people button
}
```

**Invocation** (from Note Detail):
```kotlin
var showShareSheet by remember { mutableStateOf(false) }

if (showShareSheet) {
    ShareBottomSheet(
        noteId = noteId,
        onDismiss = { showShareSheet = false },
        viewModel = hiltViewModel()
    )
}

// Action bar
Button(onClick = { showShareSheet = true }) {
    Text("Share")
}
```

---

### 6️⃣ Premium Screen (Full-Page, Not Modal)
**Location**: `presentation/screens/PremiumScreen.kt`

```kotlin
@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel,
    onBack: () -> Unit,
    onTrialClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    // MUST HAVE:
    // ✅ Feature comparison table (horizontally scrollable)
    // ✅ Pricing info
    // ✅ Free trial button
    // ✅ Current plan badge
    // ✅ Restore purchases link
    // ✅ FAQ & help links
}
```

**Comparison Table**:
```kotlin
LazyRow(modifier = Modifier.fillMaxWidth()) {
    items(plans) { plan ->
        Column(modifier = Modifier.width(150.dp)) {
            Text(plan.name, fontWeight = FontWeight.Bold)
            plan.features.forEach { feature ->
                Row {
                    Icon(
                        if (feature.included) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null
                    )
                    Text(feature.name)
                }
            }
        }
    }
}
```

---

### 7️⃣ Settings Screen (Full-Page with Tabs)
**Location**: `presentation/screens/SettingsScreen.kt`

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column {
        TopAppBar(title = { Text("Settings") }, /* ... */)
        
        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Account")
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Notifications")
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("App")
            }
        }
        
        // Content
        when (selectedTab) {
            0 -> AccountSettingsTab(viewModel)
            1 -> NotificationSettingsTab(viewModel)
            2 -> AppSettingsTab(viewModel)
        }
    }
}
```

---

## Navigation Configuration

**File**: `presentation/navigation/NavGraph.kt`

```kotlin
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = "home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home screen
        composable("home") {
            HomeScreen(
                onNoteClick = { noteId ->
                    navController.navigate("detail/$noteId")
                },
                onNewNoteClick = {
                    navController.navigate("editor")  // No ID = new note
                },
                onSearchClick = {
                    navController.navigate("search")
                }
            )
        }
        
        // Note editor (full-screen page)
        composable(
            route = "editor?noteId={noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            NoteEditorScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Note detail (full-screen page)
        composable("detail/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")!!
            NoteDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onEdit = {
                    navController.navigate("editor?noteId=$noteId")
                }
            )
        }
        
        // Search (full-screen page)
        composable("search") {
            SearchScreen(
                onResultClick = { noteId ->
                    navController.navigate("detail/$noteId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Premium (full-screen page)
        composable("premium") {
            PremiumScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Settings (full-screen page)
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Profile (full-screen page)
        composable("profile/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username")!!
            ProfileScreen(
                username = username,
                onBack = { navController.popBackStack() }
            )
        }

        // Communities (full-screen tab/page)
        composable("communities") {
            CommunitiesScreen(
                onCommunityClick = { id -> navController.navigate("community/$id") },
                onBack = { navController.popBackStack() }
            )
        }

        // Community Detail
        composable("community/{communityId}") { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId")!!
            CommunityDetailScreen(
                communityId = communityId,
                onBack = { navController.popBackStack() }
            )
        }

        // Messages list
        composable("messages") {
            MessagesScreen(
                onChatClick = { userId -> navController.navigate("chat/$userId") },
                onBack = { navController.popBackStack() }
            )
        }

        // Active Chat View
        composable("chat/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            ChatScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

---

## Bottom Navigation Configuration

**File**: `presentation/screens/MainActivity.kt`

```kotlin
@Composable
fun MainApp(navController: NavHostController) {
    var selectedNav by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Notes") },
                    selected = selectedNav == 0,
                    onClick = {
                        selectedNav = 0
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Search") },
                    selected = selectedNav == 1,
                    onClick = {
                        selectedNav = 1
                        navController.navigate("search")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Bookmark, null) },
                    label = { Text("Saved") },
                    selected = selectedNav == 2,
                    onClick = {
                        selectedNav = 2
                        navController.navigate("saved")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, null) },
                    label = { Text("Premium") },
                    selected = selectedNav == 3,
                    onClick = {
                        selectedNav = 3
                        navController.navigate("premium")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Profile") },
                    selected = selectedNav == 4,
                    onClick = {
                        selectedNav = 4
                        navController.navigate("profile/me")
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavGraph(navController)
        }
    }
}
```

---

## Common Pitfalls to Avoid

### ❌ Mistake 1: Note Editor as Modal
```kotlin
// WRONG ❌
Dialog(onDismissRequest = {}) {
    NoteEditorScreen()  // Don't do this!
}

// RIGHT ✅
composable("editor/{noteId}?") {
    NoteEditorScreen()  // Full-screen destination
}
```

### ❌ Mistake 2: Hiding Navigation While Editing
```kotlin
// WRONG ❌
if (isEditing) {
    HideBottomNavigation()
}

// RIGHT ✅
// Bottom nav is handled by Scaffold
// Editor screen is just another destination
```

### ❌ Mistake 3: Share as Full Screen
```kotlin
// WRONG ❌
composable("share") {
    ShareScreen()  // Full-page waste
}

// RIGHT ✅
if (showShare) {
    ModalBottomSheet { ShareBottomSheet() }  // Bottom sheet
}
```

### ❌ Mistake 4: Mixing Web UX with Mobile
```kotlin
// WRONG ❌
// Sidebar drawer that's always visible
PermanentDrawer() {
    NavigationMenu()
}

// RIGHT ✅
// Bottom navigation (thumb-friendly)
NavigationBar {
    NavigationBarItem(...)
}
```

### ❌ Mistake 5: Inline Search Input
```kotlin
// WRONG ❌
// Search input in AppBar (tiny on mobile)
TopAppBar {
    SearchTextField()  // Too cramped
}

// RIGHT ✅
// Full-page search screen
composable("search") {
    SearchScreen()  // Full-width input
}
```

---

## Testing Checklist for Each Screen

Before marking a screen as complete:

```kotlin
✅ Does it have proper AppBar or TopBar?
✅ Is text readable (minimum 14sp)?
✅ Are buttons tappable (minimum 48dp)?
✅ Does it work in landscape mode?
✅ Are empty states shown?
✅ Does loading state show progress?
✅ Does error state show retry button?
✅ Is navigation working (back button)?
✅ Are colors from design tokens (Part 5)?
✅ Does keyboard dismiss on action?
```

---

## Summary: Quick Implementation Checklist

When implementing a new screen:

- [ ] Create `Composable` function in `presentation/screens/`
- [ ] Add route to `NavGraph.kt`
- [ ] Implement AppBar with navigation
- [ ] Use design tokens (colors, typography)
- [ ] Add ViewModel integration
- [ ] Test on phone emulator (API 26+)
- [ ] Check landscape orientation
- [ ] Verify back navigation works
- [ ] Add to bottom nav if main destination
- [ ] Update this checklist ✓

---

**This guide is your reference while building the mobile app. When in doubt, check `MOBILE_UI_UX_AGENT_GUIDE.md` for detailed screen layouts and design specifications.**
