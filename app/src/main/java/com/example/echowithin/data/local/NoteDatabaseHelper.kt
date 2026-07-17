package com.example.echowithin.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.echowithin.data.model.AppNote
import java.time.Instant

interface NoteDbHelper {
    fun getAllNotes(): List<AppNote>
    fun getNoteById(id: String): AppNote?
    fun getPendingNotes(): List<AppNote>
    fun saveNote(note: AppNote, isSynced: Boolean = note.isSynced, pendingOp: String = note.pendingOp)
    fun markDeleted(id: String)
    fun deletePhysically(id: String)
    fun clearSyncFlags()
    fun clearAll()
    fun clearSyncedNotes()
    fun getTrashedNotes(): List<AppNote>
    fun trashNote(id: String)
    fun restoreNote(id: String)
    fun emptyTrash()
}

class NoteDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), NoteDbHelper {

    companion object {
        private const val DATABASE_NAME = "echowithin.db"
        private const val DATABASE_VERSION = 4

        const val TABLE_NOTES = "notes"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_REFERENCE = "reference"
        const val COLUMN_TAGS = "tags"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val COLUMN_IS_LOCKED = "is_locked"
        const val COLUMN_IS_PINNED = "is_pinned"
        
        // Sync control flags
        const val COLUMN_IS_SYNCED = "is_synced"
        const val COLUMN_PENDING_OP = "pending_op" // "none", "create", "edit", "delete"
        const val COLUMN_UPDATE_AVAILABLE = "update_available"
        const val COLUMN_SOURCE_NOTE_ID = "source_note_id"
        const val COLUMN_SOURCE_SHARE_ID = "source_share_id"
        const val COLUMN_IS_TRASHED = "is_trashed"
        const val COLUMN_TRASHED_AT = "trashed_at"
        const val COLUMN_FOLDER = "folder"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_TITLE TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_REFERENCE TEXT,
                $COLUMN_TAGS TEXT,
                $COLUMN_UPDATED_AT TEXT,
                $COLUMN_IS_LOCKED INTEGER DEFAULT 0,
                $COLUMN_IS_PINNED INTEGER DEFAULT 0,
                $COLUMN_IS_SYNCED INTEGER DEFAULT 1,
                $COLUMN_PENDING_OP TEXT DEFAULT 'none',
                $COLUMN_UPDATE_AVAILABLE INTEGER DEFAULT 0,
                $COLUMN_SOURCE_NOTE_ID TEXT,
                $COLUMN_SOURCE_SHARE_ID TEXT,
                $COLUMN_IS_TRASHED INTEGER DEFAULT 0,
                $COLUMN_TRASHED_AT TEXT,
                $COLUMN_FOLDER TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var currentVersion = oldVersion
        try {
            if (currentVersion < 2) {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_UPDATE_AVAILABLE INTEGER DEFAULT 0")
                currentVersion = 2
            }
            if (currentVersion < 3) {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_SOURCE_NOTE_ID TEXT")
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_SOURCE_SHARE_ID TEXT")
                currentVersion = 3
            }
            if (currentVersion < 4) {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_IS_TRASHED INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_TRASHED_AT TEXT")
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_FOLDER TEXT")
                currentVersion = 4
            }
        } catch (e: Exception) {
            e.printStackTrace()
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
            onCreate(db)
        }
    }

    override fun getAllNotes(): List<AppNote> {
        val notes = mutableListOf<AppNote>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COLUMN_PENDING_OP != ? AND $COLUMN_IS_TRASHED = 0",
            arrayOf("delete"),
            null,
            null,
            "$COLUMN_IS_PINNED DESC, $COLUMN_UPDATED_AT DESC"
        )
        
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COLUMN_ID)
            val titleIdx = c.getColumnIndexOrThrow(COLUMN_TITLE)
            val contentIdx = c.getColumnIndexOrThrow(COLUMN_CONTENT)
            val refIdx = c.getColumnIndexOrThrow(COLUMN_REFERENCE)
            val tagsIdx = c.getColumnIndexOrThrow(COLUMN_TAGS)
            val updatedIdx = c.getColumnIndexOrThrow(COLUMN_UPDATED_AT)
            val lockedIdx = c.getColumnIndexOrThrow(COLUMN_IS_LOCKED)
            val pinnedIdx = c.getColumnIndexOrThrow(COLUMN_IS_PINNED)
            val syncedIdx = c.getColumnIndexOrThrow(COLUMN_IS_SYNCED)
            val opIdx = c.getColumnIndexOrThrow(COLUMN_PENDING_OP)
            val updateAvailableIdx = c.getColumnIndexOrThrow(COLUMN_UPDATE_AVAILABLE)
            val sourceNoteIdIdx = c.getColumnIndexOrThrow(COLUMN_SOURCE_NOTE_ID)
            val sourceShareIdIdx = c.getColumnIndexOrThrow(COLUMN_SOURCE_SHARE_ID)

            while (c.moveToNext()) {
                val tagsStr = c.getString(tagsIdx).orEmpty()
                val tagsList = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                notes.add(
                    cursorToAppNote(c, idIdx, titleIdx, contentIdx, refIdx, tagsIdx, updatedIdx, lockedIdx, pinnedIdx, syncedIdx, opIdx, updateAvailableIdx, sourceNoteIdIdx, sourceShareIdIdx)
                )
            }
        }
        return notes
    }

    override fun getNoteById(id: String): AppNote? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COLUMN_ID = ? AND $COLUMN_PENDING_OP != ?",
            arrayOf(id, "delete"),
            null, null, null
        )
        
        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToAppNote(
                    c,
                    c.getColumnIndexOrThrow(COLUMN_ID),
                    c.getColumnIndexOrThrow(COLUMN_TITLE),
                    c.getColumnIndexOrThrow(COLUMN_CONTENT),
                    c.getColumnIndexOrThrow(COLUMN_REFERENCE),
                    c.getColumnIndexOrThrow(COLUMN_TAGS),
                    c.getColumnIndexOrThrow(COLUMN_UPDATED_AT),
                    c.getColumnIndexOrThrow(COLUMN_IS_LOCKED),
                    c.getColumnIndexOrThrow(COLUMN_IS_PINNED),
                    c.getColumnIndexOrThrow(COLUMN_IS_SYNCED),
                    c.getColumnIndexOrThrow(COLUMN_PENDING_OP),
                    c.getColumnIndexOrThrow(COLUMN_UPDATE_AVAILABLE),
                    c.getColumnIndexOrThrow(COLUMN_SOURCE_NOTE_ID),
                    c.getColumnIndexOrThrow(COLUMN_SOURCE_SHARE_ID)
                )
            }
        }
        return null
    }

    override fun getPendingNotes(): List<AppNote> {
        val notes = mutableListOf<AppNote>()
        val db = readableDatabase
        // A note is "pending" only if it either has an explicit pending op
        // (create/edit/delete) or was created locally and never reached the
        // server (id starts with "local_"). A stale row with is_synced=0 and
        // pending_op="none" is the result of a sync-flag reset (e.g. logout)
        // and must NOT be re-pushed as a CREATE — that was causing the
        // duplicate-notes bug.
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COLUMN_IS_SYNCED = 0 AND ($COLUMN_PENDING_OP != 'none' OR $COLUMN_ID LIKE 'local_%')",
            null, null, null, null
        )
        
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COLUMN_ID)
            val titleIdx = c.getColumnIndexOrThrow(COLUMN_TITLE)
            val contentIdx = c.getColumnIndexOrThrow(COLUMN_CONTENT)
            val refIdx = c.getColumnIndexOrThrow(COLUMN_REFERENCE)
            val tagsIdx = c.getColumnIndexOrThrow(COLUMN_TAGS)
            val updatedIdx = c.getColumnIndexOrThrow(COLUMN_UPDATED_AT)
            val lockedIdx = c.getColumnIndexOrThrow(COLUMN_IS_LOCKED)
            val pinnedIdx = c.getColumnIndexOrThrow(COLUMN_IS_PINNED)
            val syncedIdx = c.getColumnIndexOrThrow(COLUMN_IS_SYNCED)
            val opIdx = c.getColumnIndexOrThrow(COLUMN_PENDING_OP)
            val updateAvailableIdx = c.getColumnIndexOrThrow(COLUMN_UPDATE_AVAILABLE)
            val sourceNoteIdIdx = c.getColumnIndexOrThrow(COLUMN_SOURCE_NOTE_ID)
            val sourceShareIdIdx = c.getColumnIndexOrThrow(COLUMN_SOURCE_SHARE_ID)

            while (c.moveToNext()) {
                notes.add(
                    cursorToAppNote(c, idIdx, titleIdx, contentIdx, refIdx, tagsIdx, updatedIdx, lockedIdx, pinnedIdx, syncedIdx, opIdx, updateAvailableIdx, sourceNoteIdIdx, sourceShareIdIdx)
                )
            }
        }
        return notes
    }

    override fun saveNote(note: AppNote, isSynced: Boolean, pendingOp: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, note.id)
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_REFERENCE, note.reference)
            put(COLUMN_TAGS, note.tags.joinToString(","))
            put(COLUMN_UPDATED_AT, note.updatedAt)
            put(COLUMN_IS_LOCKED, if (note.isLocked) 1 else 0)
            put(COLUMN_IS_PINNED, if (note.isPinned) 1 else 0)
            put(COLUMN_IS_SYNCED, if (isSynced) 1 else 0)
            put(COLUMN_PENDING_OP, pendingOp)
            put(COLUMN_UPDATE_AVAILABLE, if (note.updateAvailable) 1 else 0)
            put(COLUMN_SOURCE_NOTE_ID, note.sourceNoteId)
            put(COLUMN_SOURCE_SHARE_ID, note.sourceShareId)
            put(COLUMN_IS_TRASHED, if (note.isTrashed) 1 else 0)
            put(COLUMN_TRASHED_AT, note.trashedAt)
            put(COLUMN_FOLDER, note.folder)
        }
        db.replace(TABLE_NOTES, null, values)
    }

    override fun markDeleted(id: String) {
        val db = writableDatabase
        val note = getNoteById(id)
        if (note == null) {
            // Delete physically if not found
            db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id))
            return
        }
        
        if (note.pendingOp == "create" || note.id.startsWith("local_")) {
            // If created offline and deleted offline, just remove it entirely!
            db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id))
        } else {
            // Mark as pending delete
            val values = ContentValues().apply {
                put(COLUMN_IS_SYNCED, 0)
                put(COLUMN_PENDING_OP, "delete")
            }
            db.update(TABLE_NOTES, values, "$COLUMN_ID = ?", arrayOf(id))
        }
    }

    override fun deletePhysically(id: String) {
        val db = writableDatabase
        db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id))
    }

    /**
     * Resets sync flags on all notes but keeps content intact.
     * NOTE: Do NOT call this from the logout/401 path — it sets
     * pending_op="none" with a real server id, and the sync loop used to
     * misread that combination as "create" and re-push the note to the
     * server (the original duplicate-notes bug). The logout path now
     * uses [clearAll] instead.
     */
    override fun clearSyncFlags() {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_IS_SYNCED, 0)
            put(COLUMN_PENDING_OP, "none")
        }
        db.update(TABLE_NOTES, values, null, null)
    }

    /**
     * Deletes ALL synced notes. Only use for explicit "Delete Account" or full data wipe.
     */
    override fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_NOTES, null, null)
    }

    /**
     * Deletes only server-synced notes and pending changes, preserving
     * offline-only notes (local_* IDs or never-synced notes with no pending op).
     * Used on session expiry/401 so the user retains their private local notes.
     */
    override fun clearSyncedNotes() {
        val db = writableDatabase
        // Delete all notes that are associated with the server account (id does not start with local_)
        db.delete(
            TABLE_NOTES,
            "$COLUMN_ID NOT LIKE 'local_%'",
            null
        )
    }

    override fun getTrashedNotes(): List<AppNote> {
        val notes = mutableListOf<AppNote>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES, null,
            "$COLUMN_IS_TRASHED = 1",
            null, null, null,
            "$COLUMN_TRASHED_AT DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COLUMN_ID)
            val titleIdx = c.getColumnIndexOrThrow(COLUMN_TITLE)
            val contentIdx = c.getColumnIndexOrThrow(COLUMN_CONTENT)
            val refIdx = c.getColumnIndexOrThrow(COLUMN_REFERENCE)
            val tagsIdx = c.getColumnIndexOrThrow(COLUMN_TAGS)
            val updatedIdx = c.getColumnIndexOrThrow(COLUMN_UPDATED_AT)
            val lockedIdx = c.getColumnIndexOrThrow(COLUMN_IS_LOCKED)
            val pinnedIdx = c.getColumnIndexOrThrow(COLUMN_IS_PINNED)
            val syncedIdx = c.getColumnIndexOrThrow(COLUMN_IS_SYNCED)
            val opIdx = c.getColumnIndexOrThrow(COLUMN_PENDING_OP)
            val updateAvailableIdx = c.getColumnIndexOrThrow(COLUMN_UPDATE_AVAILABLE)
            val sourceNoteIdIdx = c.getColumnIndexOrThrow(COLUMN_SOURCE_NOTE_ID)
            val sourceShareIdIdx = c.getColumnIndexOrThrow(COLUMN_SOURCE_SHARE_ID)
            while (c.moveToNext()) {
                notes.add(cursorToAppNote(c, idIdx, titleIdx, contentIdx, refIdx, tagsIdx, updatedIdx, lockedIdx, pinnedIdx, syncedIdx, opIdx, updateAvailableIdx, sourceNoteIdIdx, sourceShareIdIdx))
            }
        }
        return notes
    }

    override fun trashNote(id: String) {
        val db = writableDatabase
        val note = getNoteById(id)
        if (note == null) {
            // Already hidden or doesn't exist — silently ignore
            return
        }
        if (note.pendingOp == "create" || note.id.startsWith("local_")) {
            // If created offline and trashed offline, just remove it entirely
            db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id))
        } else {
            val values = ContentValues().apply {
                put(COLUMN_IS_TRASHED, 1)
                put(COLUMN_TRASHED_AT, Instant.now().toString())
                put(COLUMN_IS_SYNCED, 0)
                put(COLUMN_PENDING_OP, "delete")
            }
            db.update(TABLE_NOTES, values, "$COLUMN_ID = ?", arrayOf(id))
        }
    }

    override fun restoreNote(id: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_TRASHED, 0)
            putNull(COLUMN_TRASHED_AT)
            put(COLUMN_IS_SYNCED, 0)
            put(COLUMN_PENDING_OP, "edit")
        }
        db.update(TABLE_NOTES, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    override fun emptyTrash() {
        val db = writableDatabase
        db.delete(TABLE_NOTES, "$COLUMN_IS_TRASHED = 1", null)
    }

    /**
     * Shared helper to read an AppNote from a cursor row. The column indices
     * must have been obtained from the SAME cursor via getColumnIndexOrThrow.
     */
    private fun cursorToAppNote(
        c: android.database.Cursor,
        idIdx: Int, titleIdx: Int, contentIdx: Int, refIdx: Int,
        tagsIdx: Int, updatedIdx: Int, lockedIdx: Int, pinnedIdx: Int,
        syncedIdx: Int, opIdx: Int, updateAvailableIdx: Int,
        sourceNoteIdIdx: Int, sourceShareIdIdx: Int
    ): AppNote {
        val tagsStr = c.getString(tagsIdx).orEmpty()
        val tagsList = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val trashedIdx = c.getColumnIndex(COLUMN_IS_TRASHED)
        val trashedAtIdx = c.getColumnIndex(COLUMN_TRASHED_AT)
        val folderIdx = c.getColumnIndex(COLUMN_FOLDER)
        return AppNote(
            id = c.getString(idIdx),
            title = c.getString(titleIdx).orEmpty(),
            content = c.getString(contentIdx).orEmpty(),
            reference = c.getString(refIdx).orEmpty(),
            tags = tagsList,
            updatedAt = c.getString(updatedIdx).orEmpty(),
            isLocked = c.getInt(lockedIdx) == 1,
            isPinned = c.getInt(pinnedIdx) == 1,
            isSynced = c.getInt(syncedIdx) == 1,
            pendingOp = c.getString(opIdx).orEmpty(),
            updateAvailable = c.getInt(updateAvailableIdx) == 1,
            sourceNoteId = c.getString(sourceNoteIdIdx),
            sourceShareId = c.getString(sourceShareIdIdx),
            isTrashed = if (trashedIdx >= 0) c.getInt(trashedIdx) == 1 else false,
            trashedAt = if (trashedAtIdx >= 0) c.getString(trashedAtIdx) else null,
            folder = if (folderIdx >= 0) c.getString(folderIdx) else null
        )
    }
}
