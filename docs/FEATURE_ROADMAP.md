# EchoWithin Android — Lucrative Feature Roadmap

A practical list of features that would meaningfully increase retention,
premium conversion, or word-of-mouth for EchoWithin Mobile. The
priorities are chosen so each idea is small enough to ship in 1-2
sprints and pairs with a natural upsell moment (the most reliable
premium conversion lever in a notes app is *making the friction go
away on the device the user is already paying to be productive on*).

The roadmap is split into **Quick wins** (≤ 1 sprint, high signal),
**Mid-size bets** (1-2 sprints, real revenue), and **Moonshots**
(multi-quarter, brand-defining).

---

## 1. Quick wins — ship in the next 2 sprints

### 1.1. "Quick capture" home-screen widget (Android App Widget)
- One-tap `+` on the launcher that opens straight into a new note.
- Premium tier: voice-to-text via on-device Whisper / Android
  SpeechRecognizer; free tier: opens the editor.
- Conversion lever: people who add a widget use the app 3-4x more
  per day; the voice capture is the upsell.

### 1.2. Share-to-EchoWithin intent receiver
- Accept `text/plain` and `text/*` shares from any app (browser,
  Twitter, Kindle, Photos).
- "Save to locked notes" vs "Save to a new note" picker.
- Conversion lever: gets EchoWithin in front of users at the moment
  they're already trying to save something — very high intent.

### 1.3. Daily prompt / journaling reminder (opt-in)
- 1 push notification at the user's chosen time, with a rotating
  prompt ("What made you smile today?").
- Streak counter on the home tab; breaks visible to the user.
- Conversion lever: the streak UI is in the free tier; exporting
  the streak history + a "year in review" PDF is premium.

### 1.4. Note templates
- Pre-made markdown skeletons: meeting notes, gratitude journal,
  weekly review, book notes, dream journal, prayer log.
- Free: 3 templates. Premium: unlimited + user-created templates.
- Conversion lever: templates are highly personal, so once someone
  invests in 2-3 of their own templates they stay subscribed.

### 1.5. Pin-to-top + note color tags
- Already partially built (`isPinned` exists on the model).
- Add a color picker (6 brand-aligned tints) → faster visual scan
  of the home list.
- Free: pin up to 3 notes. Premium: unlimited + colors.
- Conversion lever: the limit is visible in the UI ("2 of 3
  pinned notes used"), so users hit a wall, not a paywall.

### 1.6. In-note attachments (premium)
- Already have `note_attachments_conf` on the backend.
- Photos, PDFs, audio. 20/file soft cap on the free tier.
- Conversion lever: people who attach a photo to a note rarely
  churn.

### 1.7. Quick filter chips on the home tab
- 1-tap filters: Today, This Week, Locked, Has Tags, Has Updates.
- Reuses existing `getAllNotes()` — no backend work.

### 1.8. Recurring reminder on a note
- "Remind me about this note every Monday 9am" — fires a push with
  the note title + deep link.
- Free: 1 active reminder. Premium: unlimited.
- Conversion lever: people who rely on reminders to come back to
  notes become daily users.

---

## 2. Mid-size bets — 1-2 sprints, real revenue

### 2.1. End-to-end encrypted shared notebooks
- Today, shared notes are server-encrypted-at-rest but the server
  can read the plaintext. A premium-only "Encrypted Notebook" mode
  could derive a per-notebook key from a shared secret, send the
  key via QR or out-of-band, and never let the server see plaintext.
- Conversion lever: "your most private journal" is the killer app
  for end-to-end encryption. Charge a premium because the server
  literally cannot recover lost keys.

### 2.2. Cross-device sync without an account
- A peer-to-peer sync over Wi-Fi Direct / local network, with a
  QR-code pairing flow. Both devices scan a QR, derive a shared key
  from a 6-word passphrase, and exchange notes directly.
- The trick: keeps notes off any server. Powerful for journalists,
  lawyers, doctors, and people in countries with hostile networks.
- Free for the first paired device; premium for unlimited devices.
- Conversion lever: privacy-conscious users will pay for the
  ability to keep their notes off "the cloud".

### 2.3. Web clipper (Chrome extension)
- Companion to the existing app, not a separate app.
- One-click save from the browser; renders to a clean note with the
  page title, URL, and a selectable quote.
- Free: 10 clips/day. Premium: unlimited + auto-tagging.

### 2.4. In-note voice memos (premium)
- One-tap record. Stored as Opus, inline-playable in the note.
- Synced as part of the existing share flow; surprise-theme
  audio could re-use this.
- Conversion lever: voice + text is a unique combination very few
  notes apps do well.

### 2.5. Markdown export + scheduled email digests
- Free: manual export of any single note as .md or .pdf.
- Premium: weekly digest email with the week's notes as a PDF book.
- Conversion lever: people who print or email their notes are
  *far* more likely to keep using the app.

### 2.6. AI-powered "Echo Insight" (premium)
- On-device summarization: 1-tap "summarize this note" or "what
  did I write about X this month?".
- Use a small, on-device model (Phi-3-mini, Gemma 2B) — privacy
  preserved end-to-end.
- Free: 5 queries/day. Premium: unlimited + multi-note Q&A.
- Conversion lever: this is the obvious "AI tax" play and is the
  single most asked-for feature in note apps in 2026.

### 2.7. Habit & mood tracker built on notes
- A new note type: "Daily Check-in" with a mood slider, a habit
  checklist, and a free-form text.
- Trends view (Sparkline of last 30 days, 12 months).
- Free: 7 days of history. Premium: unlimited + multi-habit +
  export.

---

## 3. Moonshots — multi-quarter, brand-defining

### 3.1. "Echo Sphere" — social layer
- Today the app is private-first. A social layer where users
  opt-in to a small feed of *anonymised* one-liners from people
  who wrote a note today ("Today I realised…").
- Heavily moderated. Aggressive block / hide / report. The whole
  product stays private-by-default.
- The pitch: "a journaling app that isn't lonely."
- Revenue: sponsored surprise themes (Valentine / Birthday
  cards from brands, optionally).

### 3.2. Shared "Echo Room" for couples / families
- A small, invite-only shared space where 2-6 people can leave
  short notes for each other (text, voice, photo).
- The first social surface inside EchoWithin, and the only one
  that ever has more than 1 person.
- Revenue: family plan (5 seats for KSH 200/month instead of
  KSH 50 each).

### 3.3. Wear OS + quick-capture complication
- One-tap voice note from the watch.
- Complication on the watch face shows today's prompt + a tiny
  streak indicator.
- Brand-defining because the watch face is the most intimate
  surface a user has.

### 3.4. Desktop app (Compose Multiplatform)
- Reuse the existing UI layer (already 80% compatible).
- Tray-resident quick capture.
- This is the single biggest retention lever for any note app —
  users who have it on phone *and* desktop write 5-10x more.

### 3.5. End-to-end encrypted backup
- Premium-only "Encrypted Vault" — a single encrypted bundle
  (PBKDF2 + Argon2id + Fernet) that the user can drop on their
  own cloud storage (Google Drive, Dropbox, S3).
- The server never sees the key. Lost key = lost data, by design.
- Conversion lever: the existing `note_attachments_conf` and
  Fernet code is 90% of the way there.

### 3.6. Mindful Echoes (already shipped!) → "Mindful Months"
- Surface random past memories not just from notes but from
  shared notes, blog posts, and now: voice memos.
- A monthly email digest of the most resurfaced memories.
- A "year in review" view that is shareable as a beautiful
  static page (with everything blurred by default until the
  user explicitly publishes).

---

## 4. Quick-win anti-churn plays (do these *first*)

These don't generate direct revenue but they cut churn, which
indirectly makes every other feature more profitable.

- **Onboarding rewind**: detect users who installed but never
  wrote a note, and offer a 1-tap "first note" experience with
  a sample prompt.
- **Note recovery**: a "Trash" tab with 30-day retention.
  Easy to implement, dramatically reduces regret-driven churn.
- **Stable scroll position** in the home list (already partly
  done — keep tab state in `rememberSaveable` and `LazyListState`
  to survive configuration changes).
- **Biometric unlock** for the App Lock (free, low effort,
  almost everyone uses it).

---

## 5. Pricing & positioning experiments to run

- **7-day premium trial** on the first install (already in the
  web app). Consider extending to *every* new device the user
  signs in on, not just first install.
- **Annual plan at KSH 500/year** (~10x monthly). Note-app
  churn drops ~40% on annual plans, so net revenue goes up
  even with the discount.
- **Student discount** (KSH 25/month with `.edu` email).
  Students who learn the app in college convert to full price
  in <2 years.
- **"Lifetime" tier** at KSH 2000 (one-time). Caps revenue per
  user but kills churn and is a strong viral hook.

---

## 6. North-star metric

We should measure:

- **Weekly Active Notes** (notes created or edited in the last 7
  days) — not DAU, not installs.
- **Premium conversion rate within 30 days of install**.
- **Day-7 retention** (fraction of users who are still active 7
  days after install).

Every feature in this doc should move at least one of these. If
a feature doesn't move any of them, it's not worth the sprint.
