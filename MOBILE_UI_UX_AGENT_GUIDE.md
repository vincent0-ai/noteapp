# EchoWithin Mobile UI/UX Implementation Guide for Agents

## Purpose
This document provides **exact, actionable UI specifications** for replicating EchoWithin's web design on mobile while adapting for touch interaction, screen size, and navigation patterns.

---

## Part 1: Screen Mapping (Web → Mobile)

### Web Components to Mobile Screens

| Web Component | Mobile Screen | Changes |
|---|---|---|
| Dashboard (sidebar + content) | **Home Screen** (bottom nav) | Simplified, fullscreen layout |
| Sidebar navigation | **Bottom Navigation + Drawer** | Material Design bottom bar (5 items max) |
| Posts feed | **Notes List Screen** | Card-based, paginated, full-width |
| Single post view | **Note Detail/View Screen** | Fullscreen, slide-up comments |
| Post editor (modal) | **Note Editor Screen** (dedicated page) | Own top-level screen with Appbar |
| Comments section | **Slide-up Bottom Sheet** | Pull-down to dismiss, swipeable |
| User profile sidebar | **Profile Screen** (swipe from edge) | Fullscreen with tabs |
| Premium modal | **Premium Screen** (page navigation) | Dedicated screen, not modal |
| Search bar (top) | **Search Screen** (dedicated) | Full-screen search interface |
| Sharing widget | **Share Screen** (bottom sheet) | Interactive sheet with QR code |

---

## Part 2: Detailed Screen Specifications

### SCREEN 1: Home/Dashboard (Tab or Page)

**Web Reference**: Dashboard with feed  
**Mobile Approach**: Bottom navigation destination

```
┌─────────────────────────────────────┐
│ 📝 EchoWithin          Search  Menu  │  ← AppBar
├─────────────────────────────────────┤
│ ⭐ Your Notes                        │  ← Section header
├─────────────────────────────────────┤
│ ┌───────────────────────────────────┐│  ← Note Card (full width)
│ │ Note Title                        ││
│ │ Brief preview text...             ││
│ │ #tag1 #tag2          📌 ⋯         ││  ← Actions
│ └───────────────────────────────────┘│
│                                       │
│ ┌───────────────────────────────────┐│
│ │ Another Note                      ││
│ │ ...                               ││
│ └───────────────────────────────────┘│
│                                       │
│                      🔵 Add New Note  │  ← FAB (bottom right)
├─────────────────────────────────────┤
│  🏠 Notes    🔍   📌   ⚙️   👤      │  ← Bottom Navigation
└─────────────────────────────────────┘
```

**Design Details**:
- AppBar: Title + Search icon + 3-dot menu
- Note cards: 8dp padding, 8dp corner radius
- Card content: Title (16sp bold) + Preview (14sp, 2 lines max)
- Tags row: Inline, max 3 tags visible
- Actions: Pin, Lock, More (swipe to reveal on mobile)
- FAB: 56dp diameter, at bottom-right, 16dp margin
- Bottom nav: 5 destinations max, labels + icons

**Interaction**:
- Tap card → **Note Detail Screen**
- Tap FAB → **Note Editor Screen** (new page, not modal)
- Long-press card → Multi-select mode
- Swipe card → Reveal actions (delete, share)

---

### SCREEN 2: Note Editor (Dedicated Page)

**Web Reference**: Modal popup editor  
**Mobile Approach**: **Full-screen page** (own navigation destination)

```
┌─────────────────────────────────────┐
│ ◀ Back          Edit Note          💾 │  ← AppBar (save on right)
├─────────────────────────────────────┤
│                                       │
│ Title                                 │  ← Text field
│ ┌───────────────────────────────────┐│
│ │ My Note Title...                  ││
│ └───────────────────────────────────┘│
│                                       │
│ Tags                                  │
│ ┌───────────────────────────────────┐│
│ │ 🏷️ #productivity #personal        ││
│ │ + Add tag                         ││  ← Chip-based tag input
│ └───────────────────────────────────┘│
│                                       │
│ Content                               │
│ ┌───────────────────────────────────┐│
│ │ ┌─ B I ─────────────────────────┐ ││  ← Toolbar (bold/italic/etc)
│ │ │                               │ ││
│ │ │ Start typing your note...     │ ││
│ │ │                               │ ││
│ │ │ # Markdown support           │ ││
│ │ │ - Lists work too             │ ││
│ │ │                               │ ││
│ │ │                               │ ││
│ │ └─────────────────────────────┘ ││
│ └───────────────────────────────────┘│
│                                       │
│ Attachments                           │
│ ┌───────────────────────────────────┐│
│ │ 📎 Add files          [Camera]   ││
│ └───────────────────────────────────┘│
│                                       │
│ [Privacy: 🔒 Private]  [🔗 Share]   │  ← Bottom actions
└─────────────────────────────────────┘
```

**Design Details**:
- AppBar: Back button (navigation) + Title + Save button
- Save button: Only enabled when changes made (visual feedback)
- Title field: Single-line input, 20sp, auto-focus on open
- Tags: Chip input with autocomplete (local + server)
- Content editor: Full-height, scrollable, Markdown preview
- Toolbar: Material button group (B I U ~ - * etc.)
- Attachments: Horizontal scrollable preview
- Bottom bar: Privacy toggle + Share button

**Interaction**:
- Type title → Title updates in real-time
- Type content → Auto-save to local DB (every 5 seconds)
- Tap Back → Show "Save changes?" if modified
- Tap Save → Sync to server (show progress)
- Tap Share → **Share Screen** (bottom sheet)
- Tap Attachments → File picker or camera

**Key Difference from Web**:
- NOT a modal/overlay
- Full-page with dedicated top AppBar
- Back button dismisses (not X icon)
- Occupies entire screen (better for typing on mobile)

---

### SCREEN 3: Note Detail/View

**Web Reference**: Single post view page  
**Mobile Approach**: Fullscreen with collapsing AppBar

```
┌─────────────────────────────────────┐
│ ◀ Back                       ⋯       │  ← Collapsed AppBar
├─────────────────────────────────────┤
│                                       │
│ My Note Title                        │  ← Title (collapsing)
│ May 19, 2026 • 2 min read           │  ← Metadata
│                                       │
│ ─────────────────────────────────────│
│                                       │
│ # Markdown Preview                   │
│                                       │
│ This is the full content rendered    │
│ as rich text. **Bold** and *italic*  │
│ work as expected.                    │
│                                       │
│ - Bullet points                      │
│ - Look good                          │
│                                       │
│                                       │
│ ─────────────────────────────────────│
│                                       │
│ 👤 By You  •  📌 Pinned  •  🔒 Locked│  ← Status chips
│                                       │
│ 💬 Comments (3)                      │  ← Section header
│ ┌───────────────────────────────────┐│
│ │ John: Great note!                 ││
│ │ May 19 at 2:30 PM                 ││
│ └───────────────────────────────────┘│
│                                       │
│ ┌───────────────────────────────────┐│
│ │ Jane: Thanks for sharing this    ││
│ │ ...                               ││
│ └───────────────────────────────────┘│
│                                       │
│ [Add a comment...]                   │
│                                       │
│                                       │
├─────────────────────────────────────┤
│ ❤️ Like  💬 Comment  🔗 Share  ⋯   │  ← Action bar (sticky)
└─────────────────────────────────────┘
```

**Design Details**:
- AppBar: Back + Title (collapses on scroll) + Menu
- Content: Full-width, readable typography (18sp base)
- Comments: Nested, expandable threads
- Action bar: Sticky at bottom, icon + count format
- Status chips: Pinned, Locked, Public/Private badges

**Interaction**:
- Scroll up → AppBar expands to show full title + metadata
- Scroll down → AppBar collapses to title-only
- Tap comment → Expand thread (or open **Comment Detail**)
- Tap Like → Toggle (show animation + count update)
- Tap Share → **Share Sheet** (bottom sheet)
- Tap Menu → Options (Edit, Delete, Archive, Report)

---

### SCREEN 4: Note List with Search

**Web Reference**: Dashboard feed + search modal  
**Mobile Approach**: **Dedicated Search Screen** (swipe/tap from home)

```
┌─────────────────────────────────────┐
│ ◀ Search                      X      │  ← AppBar with clear
├─────────────────────────────────────┤
│ ┌───────────────────────────────────┐│
│ │ 🔍 Search notes...                ││  ← Always-focused input
│ └───────────────────────────────────┘│
│                                       │
│ Recent searches                       │  ← When input empty
│ #productivity  #personal  #ideas      │
│                                       │
│ Filters                               │
│ [All]  [Locked]  [Public]  [⋯]      │  ← Chips to filter
│                                       │
│ ─────────────────────────────────────│
│                                       │
│ Results: 12 notes                     │
│                                       │
│ ┌───────────────────────────────────┐│
│ │ Note Title                        ││
│ │ ...matched text snippet...        ││
│ │ #tag                              ││
│ └───────────────────────────────────┘│
│                                       │
│ ┌───────────────────────────────────┐│
│ │ Another Match                     ││
│ │ ...highlighted search term...     ││
│ └───────────────────────────────────┘│
│                                       │
│                                       │
└─────────────────────────────────────┘
```

**Design Details**:
- Input field: Full-width, always-focused (keyboard shows)
- Recent searches: Horizontal scrollable chips
- Filters: Chip group (single-select or multi-select)
- Results: Card layout (same as home), text snippets highlighted
- Search: Real-time (debounced 300ms) or "Search" button

**Interaction**:
- Type → Results update in real-time
- Tap chip → Filter applied, results update
- Tap result → Go to **Note Detail Screen**
- Tap X → Clear search, go back to **Home Screen**

**Note**: On web, search is modal overlay. On mobile, it's a full screen for better UX.

---

### SCREEN 5: Sharing & Access Control

**Web Reference**: Share modal popup  
**Mobile Approach**: **Bottom Sheet** (draggable, dismissible)

```
┌─────────────────────────────────────┐
│ ═══════════════════════════════════  │  ← Drag handle
│ Share "My Note Title"                │  ← Header
├─────────────────────────────────────┤
│                                       │
│ 🔗 Share Link                         │
│ ┌───────────────────────────────────┐│
│ │ echowithin.xyz/share/abc123xyz   ││  ← Copyable link
│ │ [Copy] [View as...]              ││
│ └───────────────────────────────────┘│
│                                       │
│ 🔐 Access Level                       │
│ ◯ View Only   ◉ Can Edit   ◯ Admin   │  ← Radio buttons
│                                       │
│ ⏰ Expires                             │
│ ◉ Never   ◯ In 1 week   ◯ Custom     │  ← Date picker
│                                       │
│ 🔒 Require Password                   │
│ [Toggle] Set password...              │
│                                       │
│ 👥 Shared With                        │
│ + Add people                          │
│ ┌──────────┬─────────┐               │
│ │ john@... │ Edit  ✕ │               │
│ │ jane@... │ View  ✕ │               │
│ └──────────┴─────────┘               │
│                                       │
│ 📊 Stats                              │
│ Views: 24  • Comments: 3              │
│                                       │
└─────────────────────────────────────┘
```

**Design Details**:
- Header: Drag handle + Title
- Link section: Copy button, QR code toggle
- Access level: Radio button group
- Expiration: Date picker with presets
- Password: Toggle + secure input
- People list: Email + access level + remove
- Stats: Read-only metrics

**Interaction**:
- Drag handle → Collapse/expand sheet
- Swipe down → Dismiss
- Tap Copy → Copy to clipboard (show toast)
- Tap access level → Update (real-time sync)
- Tap "Add people" → People picker dialog
- Tap password toggle → Show secure input

---

### SCREEN 6: Premium/Subscription

**Web Reference**: Premium modal or dedicated page  
**Mobile Approach**: **Full-screen page** (not modal)

```
┌─────────────────────────────────────┐
│ ◀ Back              Premium Features  │  ← AppBar
├─────────────────────────────────────┤
│                                       │
│ 🎁 Unlock Premium                    │
│ Unlimited notes, AI features, more   │
│                                       │
│ Current Plan: Free                   │  ← Status badge
│                                       │
│ ─────────────────────────────────────│
│                                       │
│ FREE PLAN          PREMIUM PLAN       │  ← Comparison table (scroll H)
│ ─────────────────────────────────────│
│ 100 notes          Unlimited          │
│ 5 shares           Unlimited          │
│ No encryption      Encryption         │
│ Limited storage    100GB              │
│ No AI features     AI summary         │
│ -                  Custom themes      │
│                                       │
│ ─────────────────────────────────────│
│                                       │
│ 💰 Pricing                            │
│ KSH 50/month                          │
│ or KSH 450/year (25% off)             │
│                                       │
│ [Free Trial - 1 Day]                 │  ← CTA button
│                                       │
│ Already have access? [Restore]       │  ← Secondary action
│                                       │
│ Questions? [FAQ]  [Contact]          │
│                                       │
└─────────────────────────────────────┘
```

**Design Details**:
- Header: Tagline + current plan badge
- Comparison: Horizontal scrollable table (or expandable list)
- Pricing: Large text, clear currency
- CTA: Full-width button, prominent color
- Restore: Subtitle text link
- FAQ: In-app help links

**Interaction**:
- Tap trial button → Start free trial (1 day)
- Tap Restore → Verify existing purchase
- Tap FAQ → Open FAQ screen
- Tap Contact → Send email
- Trial expires → Show banner on home

**Note**: This is a full page, not a modal, because mobile users need scrollable content.

---

### SCREEN 7: Settings & Account

**Web Reference**: Profile settings page  
**Mobile Approach**: **Full-screen with tabs** or nested pages

```
┌─────────────────────────────────────┐
│ ◀ Settings                        ⋯  │  ← AppBar
├─────────────────────────────────────┤
│ 👤 Account  🔔 Notifications  ⚙️ App │  ← Tabs (or section list)
│ ┴─────────────────────────────────┴─ │
│                                       │
│ [Profile Avatar]   You                │  ← Account tab
│ john.doe@example.com                  │
│                                       │
│ Profile Settings                      │  ← Section
│ ┌─ Username ────────────────────────┐│
│ │ john_doe                          ││
│ └──────────────────────────────────┘│
│                                       │
│ ┌─ Email ────────────────────────────┐│
│ │ john.doe@example.com              ││
│ │ ✓ Verified                        ││
│ └──────────────────────────────────┘│
│                                       │
│ ┌─ Password ──────────────────────────│
│ │ ••••••••••••  [Change]            ││
│ └──────────────────────────────────┘│
│                                       │
│ Premium Status                        │
│ ┌─ Your Plan ─────────────────────────│
│ │ Free  [Upgrade]                   ││
│ └──────────────────────────────────┘│
│                                       │
│ Data & Privacy                        │  ← Section
│ ┌─ Export My Data ───────────────────│
│ │ Download all notes as JSON        ││
│ └──────────────────────────────────┘│
│                                       │
│ ┌─ Delete Account ──────────────────┐│
│ │ ⚠️ Permanent & irreversible       ││
│ │ [Delete]                          ││
│ └──────────────────────────────────┘│
│                                       │
│ [Logout]                              │  ← Action button
│                                       │
└─────────────────────────────────────┘
```

**Design Details**:
- Tabs: Account / Notifications / App
- Sections: Grouping with headers
- Input fields: Single-line text, editable
- Destructive actions: Red/warning color
- Logout: Full-width secondary button

**Interaction**:
- Tap field → Edit (modal or inline)
- Tap Change password → Go to **Change Password Screen**
- Tap Export → Download JSON
- Tap Delete → Confirmation dialog
- Tap Logout → Clear tokens, go to **Login Screen**

---

## Part 3: Component Library (Reusable)

### 1. Note Card Component
```kotlin
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Text(
                text = note.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Preview
            Text(
                text = note.content.take(100),
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tags & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(modifier = Modifier.weight(1f)) {
                    items(note.tags.take(3)) { tag ->
                        Chip(
                            onClick = {},
                            label = { Text(tag) },
                            modifier = Modifier.padding(end = 4.dp),
                            colors = ChipDefaults.chipColors(
                                containerColor = Color(0xFFE0E7FF)
                            )
                        )
                    }
                }
                
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        }
    }
}
```

### 2. Markdown Editor Component
```kotlin
@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("B", "I", "U", "-", "*", "#") .forEach { button ->
                Button(
                    onClick = { /* Insert markdown */ },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(button, fontSize = 12.sp)
                }
            }
        }
        
        // Editor
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                .padding(12.dp),
            textStyle = TextStyle(fontSize = 16.sp),
            maxLines = Int.MAX_VALUE
        )
    }
}
```

### 3. Bottom Sheet for Sharing
```kotlin
@Composable
fun ShareBottomSheet(
    onDismiss: () -> Unit,
    note: Note,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Share \"${note.title}\"",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Share link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("echowithin.xyz/share/abc123", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Button(onClick = { /* Copy */ }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Access level
            Text("Access Level", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            RadioButtonRow(
                options = listOf("View Only", "Can Edit", "Admin"),
                selected = "Can Edit",
                onSelect = { /* Update */ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add people
            Button(onClick = { /* Show picker */ }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Add People")
            }
        }
    }
}
```

---

## Part 4: Navigation & Flow

### Navigation Graph Structure
```kotlin
// Main app navigation
sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Search : AppDestination("search")
    data object Editor : AppDestination("editor/{noteId}?") // Optional noteId for new notes
    data object Detail : AppDestination("detail/{noteId}")
    data object Premium : AppDestination("premium")
    data object Settings : AppDestination("settings")
    data object Login : AppDestination("login")
    data object Profile : AppDestination("profile")
}

// Navigation flow
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppDestination.Home.route) {
        composable(AppDestination.Home.route) {
            HomeScreen(
                onNoteClick = { noteId ->
                    navController.navigate(AppDestination.Detail.route.replace("{noteId}", noteId))
                },
                onNewNoteClick = {
                    navController.navigate(AppDestination.Editor.route) // No noteId = new
                },
                onPremiumClick = {
                    navController.navigate(AppDestination.Premium.route)
                }
            )
        }
        
        composable(AppDestination.Editor.route) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            NoteEditorScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }
        
        composable(AppDestination.Detail.route) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            NoteDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onEdit = {
                    navController.navigate(AppDestination.Editor.route.replace("{noteId}", noteId))
                }
            )
        }
        
        // ... more routes
    }
}
```

---

## Part 5: Design Tokens

### Colors (Material Design 3)
```kotlin
object EchoWithinColors {
    val Primary = Color(0xFF6366F1)
    val PrimaryContainer = Color(0xFFE0E7FF)
    val OnPrimary = Color.White
    
    val Secondary = Color(0xFF8B5CF6)
    val SecondaryContainer = Color(0xFFF3E8FF)
    
    val Tertiary = Color(0xFF10B981)
    val Error = Color(0xFFDC2626)
    
    val Background = Color(0xFFFAFAFA)
    val Surface = Color.White
    val SurfaceVariant = Color(0xFFF3F4F6)
}
```

### Typography
```kotlin
object EchoWithinTypography {
    val HeadlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp
    )
    
    val HeadlineMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    )
    
    val TitleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    
    val BodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    
    val BodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    
    val LabelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}
```

---

## Part 6: Instructions for AI Agents

### How to Use This Document

**When implementing UI screens:**

1. **Find the screen** in Part 2 (Screen Specifications)
2. **Reference the layout diagram** (ASCII art shows structure)
3. **Copy the Kotlin component example** from Part 3
4. **Adapt the component** to match your data model
5. **Use design tokens** from Part 5 (colors, typography)
6. **Hook up navigation** using the pattern from Part 4
7. **Follow the interaction** flow described

### Example: Implementing the Note Editor Screen

```
STEP 1: Reference Section SCREEN 2
STEP 2: Build AppBar with back + title + save
        → Use topAppBar = { TopAppBar(...) }
STEP 3: Add TextField for title
        → Use BasicTextField or OutlinedTextField
STEP 4: Add MarkdownEditor component
        → Use the component from Part 3.2
STEP 5: Add Tags input
        → Use FlowRow with Chips
STEP 6: Add Attachments section
        → Use LazyRow for preview, Button for add
STEP 7: Add bottom action bar
        → Use Row with Privacy toggle + Share button
STEP 8: Hook SaveButton.onClick to viewModel.saveNote()
STEP 9: Hook BackButton.onClick to navController.popBackStack()
STEP 10: Hook ShareButton to show ShareBottomSheet
```

### Critical Mobile UI Principles

When implementing any screen:

1. **Full-screen pages instead of modals**
   - Note editor: Dedicated screen, not modal
   - Premium upgrade: Full page, not popup
   - Settings: Full page with tabs
   - ✅ Reason: Mobile users need more tap area and visibility

2. **Bottom navigation for main destinations**
   - Max 5 items (Home, Search, Saved, Premium, Profile)
   - Icons + labels (not icons-only)
   - ✅ Reason: Easier thumb reach on mobile

3. **Bottom sheets for secondary actions**
   - Share: Bottom sheet (draggable)
   - Comments: Slide-up or new page
   - Options menu: Context menu or bottom sheet
   - ✅ Reason: Feels native, dismissible with swipe

4. **Fullscreen list views**
   - Notes list: Fullscreen cards
   - Search results: Full-width scrollable
   - Comments: Full-height thread
   - ✅ Reason: Maximum content visibility

5. **AppBar patterns**
   - List screens: Simple (title + search + menu)
   - Detail screens: Collapsing (title animates)
   - Editor screens: Sticky save button
   - ✅ Reason: Context awareness, consistency

6. **FAB for primary action**
   - Home screen: FAB = New Note
   - Other screens: No FAB (use buttons instead)
   - ✅ Reason: Clear primary action

### Translation Examples

**From Web Modal → Mobile Full-screen:**
```
Web: Modal popup overlay
Mobile: composable(Editor.route) { NoteEditorScreen(...) }

Web: Click X to close
Mobile: Back button → popBackStack()

Web: Modal stacked on feed
Mobile: Full navigation stack, back arrow
```

**From Web Sidebar → Mobile Bottom Nav:**
```
Web: Vertical menu on left (always visible)
Mobile: Bottom navigation bar (5 items)

Web: Click menu → Navigate
Mobile: Tap nav item → Navigate (screen replaces)

Web: Sub-menus
Mobile: Nested navigation or tabs within screen
```

---

## Part 7: Quick Reference for Agents

### "I need to replicate the web UI but..."

| Requirement | Solution |
|---|---|
| The editor should be its own page | ✅ Create `NoteEditorScreen()` as a top-level destination in `NavGraph` |
| Share button opens a popup | ✅ Use `ModalBottomSheet` for sharing (draggable, not overlay) |
| Comments should be expandable | ✅ Create `CommentThread()` composable with expand/collapse state |
| Settings is a modal on web | ✅ Make it a full page with tabs: Account, Notifications, App |
| Premium is a popup | ✅ Create `PremiumScreen()` as a full navigation destination |
| Search is embedded in top bar | ✅ Create `SearchScreen()` dedicated page, tap search icon to navigate |
| Notes list is sidebar | ✅ Make it full-width cards on `HomeScreen` with paging |
| User profile is a dropdown | ✅ Create `ProfileScreen()` accessible from bottom nav |

---

## Summary: Key UI/UX Adaptations

| Component | Web | Mobile | Reasoning |
|---|---|---|---|
| Note editor | Modal popup | Full-screen page | Typing requires full space |
| Sidebar navigation | Always visible | Bottom navigation | Thumb-friendly on phone |
| Comments | Inline on page | Bottom sheet or new page | Scrollable thread view |
| Share dialog | Modal overlay | Bottom sheet | Draggable, feels native |
| Premium signup | Modal popup | Full-screen page | Comparison table needs scroll |
| Search | Top bar input | Dedicated full screen | Mobile-optimized input |
| Profile menu | Dropdown | Full-screen tab | More content on mobile |
| Settings | Page | Full-screen with tabs | Organized grouping |

---

**This guide is designed to be fed directly to an AI agent building the mobile app. It provides exact specifications for translating web UX to mobile while optimizing for touch interaction.**

---

## Part 8: Communities & Direct Messaging Specifications

### SCREEN 8: Communities (Full-Screen Page)

**Web Reference**: /communities and /community/<id> pages
**Mobile Approach**: Tab-based layout (Browse Communities vs Joined Communities) with full-screen community views.

```
┌─────────────────────────────────────┐
│ ◀ Back          Communities         ➕ │  ← AppBar (Create community on right)
├─────────────────────────────────────┤
│ 👤 My Joined       🌐 Browse All     │  ← Tabs
│ ┴─────────────────────────────────┴─ │
│                                       │
│ Pinned Communities                    │  ← Section
│ ┌───────────────────────────────────┐│
│ │ 🚀 Tech Enthusiasts               ││  ← Community card
│ │ 240 members • 12 new posts today  ││
│ └───────────────────────────────────┘│
│                                       │
│ Active Communities                    │
│ ┌───────────────────────────────────┐│
│ │ 🎨 Creative Writers               ││
│ │ [Join] 150 members • Public       ││  ← Action inside card
│ └───────────────────────────────────┘│
│ ┌───────────────────────────────────┐│
│ │ 🔒 Premium Investors              ││
│ │ [Request Access] (Premium Only)   ││
│ └───────────────────────────────────┘│
└─────────────────────────────────────┘
```

**Design Details**:
- **Tabs**: "My Joined" (showing pinned/frequent communities) and "Browse All" (discover feed).
- **Cards**: 8dp padding, 8dp corner radius. Title (16sp bold) + Stats (12sp grey) + Short Description (14sp).
- **CTA**: "Join" or "Request Access" buttons built directly inside cards for non-members.

---

### SCREEN 9: Direct Messaging (Chat View)

**Web Reference**: /messages page
**Mobile Approach**: Dedicated chat view utilizing a sticky bottom message input.

```
┌─────────────────────────────────────┐
│ ◀ Back        Jane Doe        [Call] │  ← Chat AppBar with user presence
├─────────────────────────────────────┤
│             Today, 10:15 AM         │  ← Time separator
│                                       │
│ ┌─────────────────────────┐           │  ← Incoming message bubble
│ │ Hey! Did you review the │           │
│ │ shared note yet?        │           │
│ └─────────────────────────┘           │
│                                       │
│             ┌────────────────────────┐│  ← Outgoing message bubble
│             │ Yes, I just added some ││
│             │ comments to it!        ││
│             └────────────────────────┘│
│                                       │
│ ┌─────────────────────────┐           │
│ │ Awesome! Thanks! 🙏     │           │
│ └─────────────────────────┘           │
│                                       │
│ ┌─ Type message... ─────────────────┐ │  ← Message input box
│ │ 📎   [Write a message...]   🎙️  😊 │ │  ← Voice note + emoji icons
│ └───────────────────────────────────┘ │
└─────────────────────────────────────┘
```

**Design Details**:
- **AppBar**: Back button + avatar + name + status indicator (Online / Away).
- **Bubbles**: Incoming (left-aligned, off-white/grey container) vs Outgoing (right-aligned, primary brand color container). 8dp corner radius.
- **Input**: Persistent bottom bar with file attachment button (`📎`), text input field, voice note recorder (`🎙️`), and emoji selector.

**Interaction**:
- **Long-press message**: Show reaction bar (Heart, Thumbs Up, etc.) and options (Copy, Reply, Delete).
- **Voice note tap**: Tap and hold to record, swipe left to cancel recording.

