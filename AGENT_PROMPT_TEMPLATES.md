# Agent Prompt Template: How to Use UI Specifications

When you need to assign UI development work to an AI agent, use this template as your starting point.

---

## 🎯 Example: Building the Note Editor Screen

### Your Instructions to the Agent

```
I need you to build the Note Editor Screen for the EchoWithin mobile app.

REFERENCE MATERIALS:
1. Architecture: FDROID_NOTE_APP_SPECIFICATION.md (Parts 3, 7, 8)
2. Screen specs: MOBILE_UI_UX_AGENT_GUIDE.md (SCREEN 2: Note Editor)
3. Implementation steps: AGENT_UI_IMPLEMENTATION.md (SCREEN 2)
4. Quick checklist: SCREEN_IMPLEMENTATION_QUICK_REF.md (Screen 2)
5. Mobile UX: WEB_VS_MOBILE_UI_COMPARISON.md (Section 2)

REQUIREMENTS:
- Build a FULL-SCREEN page (not a modal)
- File: presentation/screens/NoteEditorScreen.kt
- Route: "editor?noteId={noteId}" in NavGraph
- Use Jetpack Compose with Material Design 3
- Implement these required components:
  ✓ TopAppBar (back button + title + save button)
  ✓ Title TextField (auto-focused)
  ✓ Tags ChipInput (autocomplete)
  ✓ MarkdownEditor (with toolbar: B, I, U, -, *, #)
  ✓ Attachments section
  ✓ Privacy toggle + Share button

CRITICAL NOTES:
- This is a FULL-SCREEN page, NOT a modal popup
- Back button dismisses (not X icon)
- Save button only shows in AppBar (not bottom)
- Auto-save to local DB every 5 seconds
- Share button opens ModalBottomSheet (not navigate)

FOLLOW THESE PATTERNS:
- Use @HiltViewModel for dependency injection
- State management via MutableStateFlow
- Use Kotlin coroutines (viewModelScope.launch)
- Apply colors from design tokens (EchoWithinColors)
- Typography from EchoWithinTypography

INTEGRATION:
- Connect to NoteEditorViewModel (provided)
- Hook save button to viewModel.saveNote()
- Hook back button to onBack() lambda
- Hook share button to show ShareBottomSheet
- Validate with navController.navigate()

TESTING:
Before marking done:
✓ Back button works and pops back stack
✓ Save button enabled only when changes made
✓ Auto-save works (check local DB)
✓ Share button shows bottom sheet
✓ All text readable (14sp+ font)
✓ Works in landscape mode
✓ No crashes on rotate

REFERENCE CODE EXAMPLES:
- See AGENT_UI_IMPLEMENTATION.md for AppBar code
- See MOBILE_UI_UX_AGENT_GUIDE.md Part 3.2 for component code
- See SCREEN_IMPLEMENTATION_QUICK_REF.md for design tokens
```

---

## 📋 Document Usage Matrix

Use this table to know which document to reference for different tasks:

| Task | Primary Reference | Secondary | Quick Ref |
|---|---|---|---|
| **Overall architecture** | FDROID_NOTE_APP_SPECIFICATION.md (Part 3) | AGENT_UI_IMPLEMENTATION.md | — |
| **Building a screen** | MOBILE_UI_UX_AGENT_GUIDE.md (Part 2) | AGENT_UI_IMPLEMENTATION.md | SCREEN_IMPLEMENTATION_QUICK_REF.md |
| **Understanding navigation** | AGENT_UI_IMPLEMENTATION.md (Part 6) | MOBILE_UI_UX_AGENT_GUIDE.md (Part 4) | SCREEN_IMPLEMENTATION_QUICK_REF.md |
| **Design tokens** | MOBILE_UI_UX_AGENT_GUIDE.md (Part 5) | — | SCREEN_IMPLEMENTATION_QUICK_REF.md |
| **Web vs mobile changes** | WEB_VS_MOBILE_UI_COMPARISON.md | MOBILE_UI_UX_AGENT_GUIDE.md | — |
| **Component code** | MOBILE_UI_UX_AGENT_GUIDE.md (Part 3) | AGENT_UI_IMPLEMENTATION.md | — |
| **Common pitfalls** | AGENT_UI_IMPLEMENTATION.md (Part 8) | — | — |
| **Implementation checklist** | SCREEN_IMPLEMENTATION_QUICK_REF.md | AGENT_UI_IMPLEMENTATION.md | — |

---

## 🔄 Screen-by-Screen Agent Workflow

### For EACH screen you build, give the agent this workflow:

```
STEP 1: UNDERSTAND THE DESIGN
   → Read MOBILE_UI_UX_AGENT_GUIDE.md (your screen section)
   → Review WEB_VS_MOBILE_UI_COMPARISON.md (see web→mobile transform)
   → Look at the ASCII art diagram

STEP 2: UNDERSTAND NAVIGATION
   → Read AGENT_UI_IMPLEMENTATION.md (your screen section)
   → Review how screen is accessed (button click, nav route)
   → Understand back/dismiss behavior

STEP 3: CODE THE SCREEN
   → Copy component examples from MOBILE_UI_UX_AGENT_GUIDE.md Part 3
   → Follow implementation steps from AGENT_UI_IMPLEMENTATION.md
   → Use design tokens from SCREEN_IMPLEMENTATION_QUICK_REF.md

STEP 4: INTEGRATE NAVIGATION
   → Add route to NavGraph (AGENT_UI_IMPLEMENTATION.md Part 6)
   → Connect buttons to navigation
   → Test back button behavior

STEP 5: CONNECT STATE
   → Wire up ViewModel (use Hilt)
   → Connect UI inputs to viewModel methods
   → Test state updates (auto-save, sync, etc.)

STEP 6: TEST
   → Run checklist from SCREEN_IMPLEMENTATION_QUICK_REF.md
   → Test on phone emulator (API 26+)
   → Test landscape mode
   → Check accessibility (min 48dp buttons)

STEP 7: DOCUMENT
   → Add notes to code comments
   → Reference docs in commit message
```

---

## 💡 Common Agent Questions & Answers

### "Should the editor be a modal or full-screen?"
→ **FULL-SCREEN PAGE**  
→ See WEB_VS_MOBILE_UI_COMPARISON.md Section 2  
→ Implementation in MOBILE_UI_UX_AGENT_GUIDE.md SCREEN 2

### "Where do I put the Save button?"
→ **In the AppBar (top-right), not at bottom**  
→ Code example: AGENT_UI_IMPLEMENTATION.md (under SCREEN 2)

### "Should Share open a modal or bottom sheet?"
→ **BOTTOM SHEET** (not full-screen modal)  
→ See MOBILE_UI_UX_AGENT_GUIDE.md (Part 3.3 component code)

### "How do I handle back navigation?"
→ Use `navController.popBackStack()`  
→ Pattern examples: AGENT_UI_IMPLEMENTATION.md (Part 6)

### "What colors should I use?"
→ Design tokens: SCREEN_IMPLEMENTATION_QUICK_REF.md  
→ Full palette: MOBILE_UI_UX_AGENT_GUIDE.md (Part 5)

### "How many items in bottom navigation?"
→ **MAX 5 items** (Home, Search, Saved, Premium, Profile)  
→ Setup code: SCREEN_IMPLEMENTATION_QUICK_REF.md (Bottom Navigation Setup)

### "Should settings be a modal?"
→ **NO - full-screen with tabs**  
→ Example: SCREEN_IMPLEMENTATION_QUICK_REF.md (Screen 7)

---

## 🚀 Assigning Multiple Screens to Agent

```
I need you to build 3 screens for the EchoWithin mobile app:

SCREEN 1: Home (presentation/screens/HomeScreen.kt)
- Route: "home"
- Reference: MOBILE_UI_UX_AGENT_GUIDE.md SCREEN 1
- Implementation: AGENT_UI_IMPLEMENTATION.md (SCREEN 1)
- Check: SCREEN_IMPLEMENTATION_QUICK_REF.md (Screen 1)

SCREEN 2: Note Editor (presentation/screens/NoteEditorScreen.kt)
- Route: "editor/{noteId}?"
- Reference: MOBILE_UI_UX_AGENT_GUIDE.md SCREEN 2
- Implementation: AGENT_UI_IMPLEMENTATION.md (SCREEN 2)
- Check: SCREEN_IMPLEMENTATION_QUICK_REF.md (Screen 2)
- IMPORTANT: Full-screen page, NOT modal

SCREEN 3: Note Detail (presentation/screens/NoteDetailScreen.kt)
- Route: "detail/{noteId}"
- Reference: MOBILE_UI_UX_AGENT_GUIDE.md SCREEN 3
- Implementation: AGENT_UI_IMPLEMENTATION.md (SCREEN 3)
- Check: SCREEN_IMPLEMENTATION_QUICK_REF.md (Screen 3)
- NOTE: Collapsing AppBar on scroll

COMMON FILES TO UPDATE:
- presentation/navigation/NavGraph.kt (add routes)
- presentation/screens/MainActivity.kt (bottom nav)

FOLLOW THIS ORDER:
1. Build HomeScreen (depends on nothing)
2. Build NoteEditorScreen (depends on home)
3. Build NoteDetailScreen (depends on editor)
4. Update NavGraph (add all routes)
5. Test navigation between screens

For design/layout questions:
→ Check MOBILE_UI_UX_AGENT_GUIDE.md
→ Check WEB_VS_MOBILE_UI_COMPARISON.md
```

---

## 📞 When Agent Says "I need clarification..."

### If agent asks: "Should this be full-width?"
→ Send: WEB_VS_MOBILE_UI_COMPARISON.md (Rule 5: Inline Content → Full-Height Scrollable)

### If agent asks: "How do I make AppBar collapse?"
→ Send: AGENT_UI_IMPLEMENTATION.md (SCREEN 3: Note Detail)

### If agent asks: "What's the difference between this and the web?"
→ Send: WEB_VS_MOBILE_UI_COMPARISON.md + SCREEN_IMPLEMENTATION_QUICK_REF.md

### If agent asks: "What components should I use?"
→ Send: MOBILE_UI_UX_AGENT_GUIDE.md (Part 3: Component Library)

### If agent asks: "How do I test this?"
→ Send: SCREEN_IMPLEMENTATION_QUICK_REF.md (Testing Checklist)

---

## ✅ Sign-Off Checklist for Agent-Built Screens

When agent says "Screen is done", verify with:

```
SCREEN CHECKLIST (each screen):
□ File created in correct location
□ Route added to NavGraph
□ Back button/navigation works
□ All required components present
□ Design tokens applied (colors, typography)
□ Responsive on multiple screen sizes
□ Tested on API 26+ emulator
□ No crashes
□ Follows MVVM pattern
□ ViewModel injected with Hilt

DOCUMENTATION:
□ Code comments present
□ Commit message references docs
□ No TODO comments (all done)

FUNCTIONALITY:
□ All interactions work
□ State updates visible
□ Loading states shown
□ Error handling present
□ Empty states shown (if applicable)
```

---

## 📊 Document Hierarchy

```
FDROID_NOTE_APP_SPECIFICATION.md
├── Overall architecture & features
├── Technology stack choices
└── 8-week roadmap

MOBILE_UI_UX_AGENT_GUIDE.md
├── Part 1: Screen mapping (web → mobile)
├── Part 2: Detailed screen specs (7 screens)
├── Part 3: Reusable components (Kotlin code)
├── Part 4: Navigation graph
├── Part 5: Design tokens
├── Part 6: Agent instructions
└── Part 7: Quick reference

AGENT_UI_IMPLEMENTATION.md
├── Quick start principles
├── Mandatory screen implementations (with code)
├── Navigation configuration
├── Bottom navigation setup
├── Common pitfalls to avoid
└── Testing checklist

WEB_VS_MOBILE_UI_COMPARISON.md
├── Side-by-side comparisons (ASCII art)
├── Web version vs Mobile version
└── Transformation rules

SCREEN_IMPLEMENTATION_QUICK_REF.md
├── Screen-by-screen checklist
├── Design tokens
├── Common code patterns
└── Deploy checklist
```

---

## 🎓 Training an Agent from Scratch

If you're training a new agent on this project:

**1. Send these in order:**
1. FDROID_NOTE_APP_SPECIFICATION.md (Intro + architecture)
2. WEB_VS_MOBILE_UI_COMPARISON.md (Understand web→mobile)
3. MOBILE_UI_UX_AGENT_GUIDE.md (Learn design system & screens)
4. AGENT_UI_IMPLEMENTATION.md (Learn patterns & common pitfalls)
5. SCREEN_IMPLEMENTATION_QUICK_REF.md (Use as reference)

**2. Then assign a single screen:**
   - Start with HomeScreen (simplest)
   - Have agent read its section in each document
   - Review their code before merging

**3. Then assign next screen:**
   - NoteEditorScreen (more complex)
   - Should be faster with experience
   - They can now reference previous screen

**4. Then assign parallel work:**
   - Multiple agents build different screens
   - All use same design tokens & patterns
   - Consistency guaranteed

---

## 🔗 Document Links for Copy-Paste

Use these in your agent prompts:

```
Architecture: See FDROID_NOTE_APP_SPECIFICATION.md Part 3-8
Design specs: See MOBILE_UI_UX_AGENT_GUIDE.md
Implementation: See AGENT_UI_IMPLEMENTATION.md
Web comparison: See WEB_VS_MOBILE_UI_COMPARISON.md
Quick ref: See SCREEN_IMPLEMENTATION_QUICK_REF.md
```

---

## 💬 Example: Full Agent Prompt for Complete Feature

```
Build the Note Editor feature for EchoWithin mobile.

TASK:
Create a dedicated full-screen note editor screen (not modal popup).

SCREENS TO BUILD:
1. NoteEditorScreen.kt (main editor)
2. Update NavGraph.kt (add route)
3. Connect HomeScreen FAB (new note)

DETAILED SPECS:
- Layout: MOBILE_UI_UX_AGENT_GUIDE.md → SCREEN 2
- Web comparison: WEB_VS_MOBILE_UI_COMPARISON.md → Section 2
- Implementation: AGENT_UI_IMPLEMENTATION.md → SCREEN 2
- Quick checks: SCREEN_IMPLEMENTATION_QUICK_REF.md → Screen 2

REQUIRED COMPONENTS:
✓ TopAppBar with back + title + save (see code examples)
✓ Title TextField (auto-focused, 20sp)
✓ Tags ChipInput (with autocomplete)
✓ MarkdownEditor with toolbar (use component from guide)
✓ Attachments section (add button + preview)
✓ Privacy toggle (Private/Public)
✓ Share button (opens bottom sheet)

CRITICAL:
- Full-screen page (NOT modal)
- Back button dismisses
- Save button in AppBar only
- Auto-save to local DB every 5 seconds

STATE MANAGEMENT:
- Use @HiltViewModel
- Use MutableStateFlow for state
- Use viewModelScope.launch for async

DESIGN:
- Colors: EchoWithinColors (from SCREEN_IMPLEMENTATION_QUICK_REF.md)
- Typography: EchoWithinTypography
- Spacing: 16dp padding, 8dp radius

TEST BEFORE SUBMITTING:
✓ Back button works
✓ Save button shows progress
✓ Auto-save works (check DB)
✓ Share opens bottom sheet
✓ No crashes on rotate
✓ Landscape mode works

When you're done:
1. Share code file path
2. Confirm NavGraph updated
3. List any blockers/questions
```

---

## 💬 Example: Agent Prompt for Direct Messaging (Chat Screen)

```
Build the Direct Messaging Chat screen for EchoWithin mobile.

TASK:
Create a chat bubble view screen with real-time text exchange and media attachments.

SCREENS TO BUILD:
1. DirectMessageChatScreen.kt (main conversation view)
2. Update NavGraph.kt (add route: "chat/{userId}")

DETAILED SPECS:
- Layout: MOBILE_UI_UX_AGENT_GUIDE.md → SCREEN 9
- Web comparison: WEB_VS_MOBILE_UI_COMPARISON.md → Section 7
- API integration: FDROID_NOTE_APP_SPECIFICATION.md → Part 5 (Direct Messaging API endpoints)

REQUIRED COMPONENTS:
✓ ChatAppBar with back button, user name, and online/offline presence indicator
✓ Message bubble list (incoming left-aligned in grey, outgoing right-aligned in primary color)
✓ Sticky bottom message input bar containing:
  - Attachment clip icon (📎) to share files/images
  - Text entry field ("Type message...")
  - Voice note mic icon (🎙️) to hold-to-record
  - Emoji selector button (😊)

CRITICAL RULES:
- Sticky input box should automatically adjust height when screen keyboard rises
- Message bubbles must have 8dp corner radius with nice padding
- Chat scrolling must automatically focus on the newest message upon receipt
- Long-pressing a bubble shows reaction toolbar and options (Copy, Reply, Delete)

STATE MANAGEMENT:
- Fetch messages from `/api/messages/history/{userId}`
- Sync messages via Socket.IO events ("new_message", "message_deleted")
- Store chat states with state flows inside ChatViewModel

TEST BEFORE SUBMITTING:
✓ Sending a text automatically updates list and scrolls bottom
✓ Back button successfully navigates back to chats list screen
✓ Landscape mode handles the input toolbar and virtual keyboard properly
✓ Quick tap on voice recorder shows placeholder instructions
```

---

**These templates should be in your prompts when assigning work to agents. The documents provide everything needed for high-quality implementations with zero ambiguity.**
