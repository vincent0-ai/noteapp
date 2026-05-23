# Web vs Mobile UI Comparison Guide

## Overview: How EchoWithin Web Translates to Mobile

This guide shows side-by-side comparisons of web screens and their mobile equivalents, highlighting the key UI/UX differences.

---

## 1. DASHBOARD / HOME

### WEB VERSION
```
┌─────────────────────────────────────────────────────────┐
│ EchoWithin        [Search...]        [Menu]             │ ← Top bar
├──────────────────────┬──────────────────────────────────┤
│ Navigation           │  Your Feed                       │
│ ─────────────────    │ ┌────────────────────────────────┤
│ 🏠 Home              │ │ Post Title                    │
│ 🔍 Search            │ │ Posted by @user 2 hours ago  │
│ 📝 Create            │ │                                │
│ 🔖 Saved             │ │ Preview of content...        │
│ ⭐ Premium           │ │                                │
│ ⚙️ Settings          │ │ ❤️ 42  💬 8  🔗 Share        │
│ 👤 Profile           │ ├────────────────────────────────┤
│                      │ │ Another Post                   │
│                      │ │ ...                            │
│ ────────────         │ │                                │
│ Community            │ │ ❤️ 15  💬 3  🔗 Share        │
│ Trending             │ └────────────────────────────────┘
│ Announcements        │
└──────────────────────┴──────────────────────────────────┘

LAYOUT: Sidebar (fixed) + Main content (scrollable)
INTERACTION: Click sidebar → navigate within same page
```

### MOBILE VERSION
```
┌─────────────────────────────────────────────────────────┐
│ 📝 EchoWithin          🔍 Search       ⋮ Menu           │ ← AppBar
├─────────────────────────────────────────────────────────┤
│                                                           │
│ ⭐ Your Notes                                            │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Post Title                                          │ │
│ │ Brief preview text. Posted 2 hours ago.            │ │
│ │ #tag1 #tag2                            📌 ⋯        │ │ ← Full-width card
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Another Post                                        │ │
│ │ ...                                                 │ │
│ │ #tag                                     📌 ⋯      │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│                                                     ➕    │ ← FAB (new note)
│                                                           │
├─────────────────────────────────────────────────────────┤
│ 🏠 Notes   🔍 Search  📌 Saved  ⭐ Premium  👤 Profile  │ ← Bottom nav
└─────────────────────────────────────────────────────────┘

LAYOUT: Full-screen list, bottom navigation
INTERACTION: Tap nav item → navigate to that screen
KEY DIFFERENCE: Sidebar → Bottom Navigation (thumb-friendly)
```

---

## 2. CREATE/EDIT POST

### WEB VERSION
```
┌─────────────────────────────────────────────────────────┐
│ Your Feed                                               │
│ ┌──────────────────────────────────────────────────────┐│
│ │ ╔═══════════════════════════════════════════════════╗ ││
│ │ ║ ✕ Create Post                                  💾 ║ ││ ← Modal overlay
│ │ ╠═══════════════════════════════════════════════════╣ ││
│ │ ║ Title: [________________]                       ║ ││
│ │ ║                                                   ║ ││
│ │ ║ Content:                                          ║ ││
│ │ ║ ┌─────────────────────────────────────────────┐ ║ ││
│ │ ║ │ Start typing...                             │ ║ ││
│ │ ║ │                                               │ ║ ││
│ │ ║ │                                               │ ║ ││
│ │ ║ │                                               │ ║ ││
│ │ ║ └─────────────────────────────────────────────┘ ║ ││
│ │ ║                                                   ║ ││
│ │ ║ [Share] [Save as draft]    [Post]               ║ ││
│ │ ╚═══════════════════════════════════════════════════╝ ││
│ └──────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘

PRESENTATION: Modal overlay, X button to close
SIZE: Constrained width, stays centered
BUTTONS: Share, Save Draft, Post (inline)
```

### MOBILE VERSION
```
┌─────────────────────────────────────────────────────────┐
│ ◀ Back                    Edit Note                💾    │ ← AppBar with save
├─────────────────────────────────────────────────────────┤
│                                                           │
│ Title                                                     │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ My Note Title                                       │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ Tags                                                      │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🏷️ #productivity #personal  + Add tag             │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ Content                                                   │
│ ┌─ B  I  U  ──────────────────────────────────────────┐ │ ← Toolbar
│ │                                                       │ │
│ │ Start typing your note...                           │ │
│ │                                                       │ │
│ │ # Markdown support                                  │ │
│ │                                                       │ │
│ │                                                       │ │
│ │                                                       │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Attachments (optional)                              │ │
│ │ 📎 Add files        [Camera]                        │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ [Private 🔒]                        [Share 🔗]          │
│                                                           │
├─────────────────────────────────────────────────────────┤
└─────────────────────────────────────────────────────────┘

PRESENTATION: Full-screen page (own navigation destination)
SIZE: Full width, uses entire screen
BUTTONS: Save (AppBar), Share (bottom)
INTERACTION: Back button dismisses, not X
```

### Key Differences:
| Web | Mobile |
|---|---|
| Modal overlay (semi-transparent bg) | Full-screen page |
| X button to close | Back button to close |
| Centered, constrained width | Full width |
| Buttons at bottom of modal | Save button in AppBar, Share in sheet |
| Share button in modal | Share opens bottom sheet |

---

## 3. POST/NOTE DETAIL VIEW

### WEB VERSION
```
┌─────────────────────────────────────────────────────────┐
│ Navigation   │ Single Post                              │
│              │ ┌─────────────────────────────────────┐  │
│              │ │ Post Title                          │  │
│              │ │ By @author • May 19, 2026 • Read   │  │
│              │ │                                     │  │
│              │ │ Full content of the post rendered   │  │
│              │ │ with markdown. **Bold** and         │  │
│              │ │ *italic* work.                      │  │
│              │ │                                     │  │
│              │ │ Comments Section                    │  │
│              │ │ ─────────────────────────────────   │  │
│              │ │ 💬 Comments (3)                    │  │
│              │ │                                     │  │
│              │ │ John: Great post!                   │  │
│              │ │ Posted 2 hours ago                  │  │
│              │ │ [Reply] [More]                      │  │
│              │ │                                     │  │
│              │ │ Jane: Thanks for sharing!           │  │
│              │ │ Posted 1 hour ago                   │  │
│              │ │ [Reply] [More]                      │  │
│              │ │                                     │  │
│              │ │ [Add comment: ____________]         │  │
│              │ │                                     │  │
│              │ │ ─────────────────────────────────   │  │
│              │ │ ❤️ 42  💬 3  🔗 Share  ⋮           │  │
│              │ └─────────────────────────────────────┘  │
└─────────────┴─────────────────────────────────────────┘

LAYOUT: Sidebar + single column
COMMENTS: Inline with post
ACTIONS: At bottom of content
```

### MOBILE VERSION
```
┌─────────────────────────────────────────────────────────┐
│ ◀ Back                                  ⋮ Menu          │ ← Collapsed AppBar
├─────────────────────────────────────────────────────────┤
│                                                           │
│ Post Title                                               │ ← Expands on scroll up
│ By @author • May 19, 2026                               │
│ Read time: 2 min                                        │
│                                                           │
│ ─────────────────────────────────────────────────────────│
│                                                           │
│ Full content rendered with markdown.                    │
│ **Bold** and *italic* work as expected.                 │
│                                                           │
│ - Bullet points look good                              │
│ - List formatting works                                │
│                                                           │
│ ─────────────────────────────────────────────────────────│
│                                                           │
│ 👤 By You  •  📌 Pinned  •  🔒 Locked                   │
│                                                           │
│ 💬 Comments (3)                                          │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ John: Great post!                  [More]          │ │
│ │ Posted 2 hours ago                                  │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Jane: Thanks for sharing!         [More]           │ │
│ │ Posted 1 hour ago                                   │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ [Add a comment... 💬]                                   │
│                                                           │
├─────────────────────────────────────────────────────────┤
│ ❤️ 42    💬 3    🔗 Share    ⋮ More                   │ ← Sticky action bar
└─────────────────────────────────────────────────────────┘

LAYOUT: Full-screen, scrollable
COMMENTS: Inline threads (expandable)
ACTIONS: Sticky footer bar
APPBAR: Collapses on scroll (title hides, AppBar shrinks)
```

### Key Differences:
| Web | Mobile |
|---|---|
| Sidebar visible | Full-width content |
| Static AppBar | Collapsing AppBar (title animates) |
| Comments inline | Comments inline (same, but stickier) |
| Action buttons at bottom of content | Sticky action bar (always visible) |
| Fixed layout | Responsive, scrollable |

---

## 4. SEARCH

### WEB VERSION
```
┌─────────────────────────────────────────────────────────┐
│ EchoWithin        [Search...]        [Menu]             │
├──────────────────────┬──────────────────────────────────┤
│ Navigation           │                                  │
│                      │ ╔═══════════════════════════════╗ │
│                      │ ║ 🔍 [Search posts...]       ✕  ║ │ ← Modal overlay
│                      │ ╠═══════════════════════════════╣ │
│                      │ ║ Recent searches:              ║ │
│                      │ ║ #productivity  #ideas        ║ │
│                      │ ║                                ║ │
│                      │ ║ Results:                       ║ │
│                      │ ║ [Post 1] ...                  ║ │
│                      │ ║ [Post 2] ...                  ║ │
│                      │ ║                                ║ │
│                      │ ╚═══════════════════════════════╝ │
│                      │                                  │
└──────────────────────┴──────────────────────────────────┘

PRESENTATION: Modal popup (small-ish)
INTERACTION: Type in modal, X to close
FOCUS: Modal has focus, background dims
```

### MOBILE VERSION
```
┌─────────────────────────────────────────────────────────┐
│ ◀ Search                                          X      │ ← AppBar with clear
├─────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🔍 Search notes...                                  │ │ ← Always-focused
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ Recent searches                                           │
│ #productivity  #ideas  #personal  ×  ×                  │
│                                                           │
│ Filters                                                   │
│ [All]  [Locked]  [Public]  [Drafts]  [⋯]               │
│                                                           │
│ ─────────────────────────────────────────────────────────│
│                                                           │
│ Results: 12 notes                                        │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Note Title                                          │ │
│ │ ...matched **search term** snippet...              │ │
│ │ #tag1  #tag2                                        │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Another Note                                        │ │
│ │ ...containing **search term**...                   │ │
│ │ #tag                                                │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│                                                           │
├─────────────────────────────────────────────────────────┤
│ 🏠 Notes   🔍 Search  📌 Saved  ⭐ Premium  👤 Profile  │
└─────────────────────────────────────────────────────────┘

PRESENTATION: Full-screen page (bottom nav destination)
INTERACTION: Keyboard auto-shows, input always-focused
RESULTS: Full-width cards with highlighted matches
```

### Key Differences:
| Web | Mobile |
|---|---|
| Modal overlay | Full-screen page |
| X button to close | Back button + X to clear |
| Modal stays small | Full-width utilization |
| Type in modal | Keyboard opens automatically |

---

## 5. SHARING

### WEB VERSION
```
┌─────────────────────────────────────────────────────────┐
│ Post View                                               │
│ ┌──────────────────────────────────────────────────────┐│
│ │                                                      ││
│ │ ╔═══════════════════════════════════════════════╗  ││
│ │ ║ 🔗 Share Post                             ✕   ║  ││ ← Modal
│ │ ╠═══════════════════════════════════════════════╣  ││
│ │ ║ Link: echowithin.xyz/share/abc123xyz        ║  ││
│ │ ║ [Copy]                                       ║  ││
│ │ ║                                              ║  ││
│ │ ║ Access: [View Only ▼]                       ║  ││
│ │ ║ Expires: [Never ▼]                          ║  ││
│ │ ║ Password: [Toggle]                          ║  ││
│ │ ║                                              ║  ││
│ │ ║ Shared with:                                 ║  ││
│ │ ║ john@... [Edit] [Remove]                     ║  ││
│ │ ║ jane@... [Edit] [Remove]                     ║  ││
│ │ ║ [+ Add People]                               ║  ││
│ │ ║                                              ║  ││
│ │ ║ [Cancel] [Save]                              ║  ││
│ │ ╚═══════════════════════════════════════════════╝  ││
│ │                                                      ││
│ └──────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘

PRESENTATION: Modal dialog
BUTTONS: Cancel, Save (at bottom of modal)
INTERACTION: X or Cancel to close
```

### MOBILE VERSION
```
┌─────────────────────────────────────────────────────────┐
│ Post View                                               │
│                                                           │
│ [Action: Share button]                                   │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ ═════════════════════════════════════════════════   │ │
│ │ Share "Post Title"                                  │ │ ← Bottom Sheet
│ │ ─────────────────────────────────────────────────── │ │
│ │                                                       │ │
│ │ 🔗 Share Link                                       │ │
│ │ ┌─────────────────────────────────────────────────┐│ │
│ │ │ echowithin.xyz/share/abc123              [Copy]││ │
│ │ └─────────────────────────────────────────────────┘│ │
│ │                                                       │ │
│ │ 🔐 Access Level                                     │ │
│ │ ◯ View Only  ◉ Can Edit  ◯ Admin                   │ │
│ │                                                       │ │
│ │ ⏰ Expires                                           │ │
│ │ ◉ Never  ◯ 1 week  ◯ Custom                         │ │
│ │                                                       │ │
│ │ 🔒 Password: [Toggle] ••••••                        │ │
│ │                                                       │ │
│ │ 👥 Shared With                                      │ │
│ │ + Add people                                        │ │
│ │ john@... [Edit] [✕]                                │ │
│ │ jane@... [Edit] [✕]                                │ │
│ │                                                       │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘

PRESENTATION: Bottom sheet (draggable, dismissible)
BUTTONS: Only close/dismiss (drag down or X)
INTERACTION: Changes auto-save, swipe-down to close
DRAG HANDLE: Visible at top
```

### Key Differences:
| Web | Mobile |
|---|---|
| Modal dialog | Bottom sheet |
| X button to close | Drag handle or swipe-down |
| Cancel/Save buttons | Auto-save on change |
| Fixed size, centered | Full-width, bottom-aligned |
| Modal stacked on page | Sheet overlays content |

---

## 6. PREMIUM / SUBSCRIPTION

### WEB VERSION
```
┌─────────────────────────────────────────────────────────┐
│ Dashboard                                               │
│                                                           │
│ ╔═══════════════════════════════════════════════════╗   │
│ ║ 🎁 Upgrade to Premium                         ✕  ║   │ ← Modal
│ ╠═══════════════════════════════════════════════════╣   │
│ ║                                                   ║   │
│ ║ Current: Free Plan                             ║   │
│ ║                                                   ║   │
│ ║ Features:                                        ║   │
│ ║ ✓ 100 notes      ✗ 1GB storage                 ║   │
│ ║ ✓ Basic editing  ✗ No encryption               ║   │
│ ║                                                   ║   │
│ ║ Premium: KSH 50/month                           ║   │
│ ║ ✓ Unlimited notes    ✓ 100GB storage           ║   │
│ ║ ✓ Advanced editing   ✓ Encryption               ║   │
│ ║ ✓ AI features        ✓ Custom themes            ║   │
│ ║                                                   ║   │
│ ║ [Start Free Trial] [Restore Purchase]           ║   │
│ ║                                                   ║   │
│ ╚═══════════════════════════════════════════════════╝   │
│                                                           │
└─────────────────────────────────────────────────────────┘

PRESENTATION: Modal overlay
SIZE: Constrained width
INTERACTION: X to close
CONTENT: Feature comparison, buttons at bottom
```

### MOBILE VERSION
```
┌─────────────────────────────────────────────────────────┐
│ ◀ Back              Premium Features  ⋮                  │ ← AppBar
├─────────────────────────────────────────────────────────┤
│                                                           │
│ 🎁 Unlock Premium                                        │
│ Get unlimited notes, AI features & more                 │
│                                                           │
│ Current Plan: Free                                       │
│                                                           │
│ ─────────────────────────────────────────────────────────│
│                                                           │
│ Feature Comparison  ← Horizontal Scroll →               │
│                                                           │
│ ┌──────────────────┬──────────────────┐                 │
│ │ FREE             │ PREMIUM          │                 │
│ │ ─────────────────┼──────────────────│                 │
│ │ ✓ 100 notes      │ ✓ Unlimited      │                 │
│ │ ✗ 1GB storage    │ ✓ 100GB storage  │                 │
│ │ ✓ Basic edit     │ ✓ Advanced edit  │                 │
│ │ ✗ Encryption     │ ✓ Encryption     │                 │
│ │ ✗ AI features    │ ✓ AI features    │                 │
│ │ ✗ Custom theme   │ ✓ Custom theme   │                 │
│ └──────────────────┴──────────────────┘                 │
│                                                           │
│ ─────────────────────────────────────────────────────────│
│                                                           │
│ 💰 Pricing                                               │
│ KSH 50 / month                                           │
│ or KSH 450 / year (save 25%)                            │
│                                                           │
│ [Start Free Trial (1 Day)]                              │
│                                                           │
│ Already have access? [Restore Purchase]                │
│                                                           │
│ [FAQ]  [Contact Support]                               │
│                                                           │
├─────────────────────────────────────────────────────────┤
└─────────────────────────────────────────────────────────┘

PRESENTATION: Full-screen page
SIZE: Full width, scrollable
INTERACTION: Back button to dismiss
CONTENT: Feature comparison (horizontal scroll), pricing, CTA
```

### Key Differences:
| Web | Mobile |
|---|---|
| Modal overlay | Full-screen page |
| X button | Back button |
| Inline comparison | Horizontal-scrollable table |
| Compact layout | Full-width, spacious |
| Buttons at bottom | Button sticky or scrollable |

---

## 6. COMMUNITIES

### WEB VERSION
```
┌─────────────────────────────────────────────────────────┐
│ EchoWithin        Communities Feed                      │
├─────────────────────────────────────────────────────────┤
│ 🚀 Tech [Join] • 240 members • 12 new posts today       │
│ Description of tech community...                        │
├─────────────────────────────────────────────────────────┤
│ 🎨 Writers [Join] • 150 members • Public                │
│ Description of writers community...                     │
└─────────────────────────────────────────────────────────┘
```
LAYOUT: Sidebar directory listing and dense layout.

### MOBILE VERSION
```
┌─────────────────────────────────────────────────────────┐
│ ◀ Back                Communities                 ➕    │ ← AppBar
├─────────────────────────────────────────────────────────┤
│  My Joined        Browse All                            │ ← Tabs
├─────────────────────────────────────────────────────────┤
│ Pinned Communities                                      │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🚀 Tech Enthusiasts                                 │ │ ← Card style
│ │ 240 members • 12 new posts today                    │ │
│ └─────────────────────────────────────────────────────┘ │
│ Active Communities                                      │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🎨 Creative Writers                                 │ │
│ │ [Join] 150 members • Public                         │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```
LAYOUT: Card-based lists partitioned under "My Joined" and "Browse All" tabs.
KEY DIFFERENCE: Dense HTML table/list directory → Dedicated touch-optimized feed tabs and Mitglied buttons inside cards.

---

## 7. DIRECT MESSAGING (CHAT)

### WEB VERSION
```
┌─────────────────────────────────────────────────────────┐
│ Messages                                                │
├──────────────────────┬──────────────────────────────────┤
│ Active Chats         │ Chat History with Jane Doe       │
│ ───                  │                                  │
│ @jane_doe (online)   │ [Jane] Hey! Did you check note?  │
│ @bob_smith           │ [Me] Yes, added comments!        │
│                      │ [Send input box here           ] │
└──────────────────────┴──────────────────────────────────┘
```
LAYOUT: Two-column split-pane interface (chats list on left, chat history on right).

### MOBILE VERSION
```
┌─────────────────────────────────────────────────────────┐
│ ◀ Back                Jane Doe                          │ ← Single active user
├─────────────────────────────────────────────────────────┤
│             Today, 10:15 AM                             │
│                                                         │
│ ┌─────────────────────────┐                             │
│ │ Hey! Did you review the │                             │ ← Left-aligned bubble
│ │ shared note yet?        │                             │
│ └─────────────────────────┘                             │
│                                                         │
│             ┌────────────────────────┐                  │
│             │ Yes, I just added some │                  │ ← Right-aligned bubble
│             │ comments to it!        │                  │
│             └────────────────────────┘                  │
├─────────────────────────────────────────────────────────┤
│ 📎  [Write a message...]                           🎙️  │ ← Sticky bottom bar
└─────────────────────────────────────────────────────────┘
```
LAYOUT: Fullscreen conversation page with sticky bottom bar. Back arrow navigates back to chats list screen.
KEY DIFFERENCE: Split-pane layout → Single active chat fullscreen view, optimized keyboard and media attachment access.

---

## Summary: Web → Mobile Transformation Rules

### Rule 1: Modals → Full-Screen Pages
**Web**: Edit post, Premium signup, Settings  
**Mobile**: Each gets own navigation destination  
**Why**: Full screen better for typing, form-filling on mobile

### Rule 2: Sidebar → Bottom Navigation
**Web**: Left sidebar with 6+ items  
**Mobile**: Bottom navigation with 5 items max  
**Why**: Thumb-friendly, Material Design standard

### Rule 3: Popovers → Bottom Sheets
**Web**: Share dialog (modal)  
**Mobile**: Share bottom sheet (draggable)  
**Why**: Native mobile feel, finger-friendly

### Rule 4: Static AppBar → Collapsing AppBar
**Web**: Title static at top  
**Mobile**: Title collapses/expands on scroll  
**Why**: Maximize content space on smaller screens

### Rule 5: Inline Content → Full-Height Scrollable
**Web**: Multi-column layouts  
**Mobile**: Single-column, full-width scrolling  
**Why**: Responsive design, mobile-first

---

**Use this guide when comparing your implementation to the original web design. It shows exactly how to adapt each component for mobile while maintaining the same visual identity.**
