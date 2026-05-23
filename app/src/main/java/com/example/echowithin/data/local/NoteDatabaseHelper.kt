package com.example.echowithin.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.echowithin.data.model.AppNote

class NoteDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "echowithin.db"
        private const val DATABASE_VERSION = 1

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
                $COLUMN_PENDING_OP TEXT DEFAULT 'none'
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    fun getAllNotes(): List<AppNote> {
        val notes = mutableListOf<AppNote>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COLUMN_PENDING_OP != ?",
            arrayOf("delete"),
            null,
            null,
            "$COLUMN_UPDATED_AT DESC"
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

            while (c.moveToNext()) {
                val tagsStr = c.getString(tagsIdx).orEmpty()
                val tagsList = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                notes.add(
                    AppNote(
                        id = c.getString(idIdx),
                        title = c.getString(titleIdx).orEmpty(),
                        content = c.getString(contentIdx).orEmpty(),
                        reference = c.getString(refIdx).orEmpty(),
                        tags = tagsList,
                        updatedAt = c.getString(updatedIdx).orEmpty(),
                        isLocked = c.getInt(lockedIdx) == 1,
                        isPinned = c.getInt(pinnedIdx) == 1,
                        isSynced = c.getInt(syncedIdx) == 1,
                        pendingOp = c.getString(opIdx).orEmpty()
                    )
                )
            }
        }
        return notes
    }

    fun getNoteById(id: String): AppNote? {
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
                val tagsStr = c.getString(c.getColumnIndexOrThrow(COLUMN_TAGS)).orEmpty()
                val tagsList = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                return AppNote(
                    id = c.getString(c.getColumnIndexOrThrow(COLUMN_ID)),
                    title = c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE)).orEmpty(),
                    content = c.getString(c.getColumnIndexOrThrow(COLUMN_CONTENT)).orEmpty(),
                    reference = c.getString(c.getColumnIndexOrThrow(COLUMN_REFERENCE)).orEmpty(),
                    tags = tagsList,
                    updatedAt = c.getString(c.getColumnIndexOrThrow(COLUMN_UPDATED_AT)).orEmpty(),
                    isLocked = c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_LOCKED)) == 1,
                    isPinned = c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_PINNED)) == 1,
                    isSynced = c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_SYNCED)) == 1,
                    pendingOp = c.getString(c.getColumnIndexOrThrow(COLUMN_PENDING_OP)).orEmpty()
                )
            }
        }
        return null
    }

    fun getPendingNotes(): List<AppNote> {
        val notes = mutableListOf<AppNote>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COLUMN_IS_SYNCED = 0",
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

            while (c.moveToNext()) {
                val tagsStr = c.getString(tagsIdx).orEmpty()
                val tagsList = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                notes.add(
                    AppNote(
                        id = c.getString(idIdx),
                        title = c.getString(titleIdx).orEmpty(),
                        content = c.getString(contentIdx).orEmpty(),
                        reference = c.getString(refIdx).orEmpty(),
                        tags = tagsList,
                        updatedAt = c.getString(updatedIdx).orEmpty(),
                        isLocked = c.getInt(lockedIdx) == 1,
                        isPinned = c.getInt(pinnedIdx) == 1,
                        isSynced = c.getInt(syncedIdx) == 1,
                        pendingOp = c.getString(opIdx).orEmpty()
                    )
                )
            }
        }
        return notes
    }

    fun saveNote(note: AppNote, isSynced: Boolean = true, pendingOp: String = "none") {
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
        }
        db.replace(TABLE_NOTES, null, values)
    }

    fun markDeleted(id: String) {
        val db = writableDatabase
        val note = getNoteById(id)
        if (note == null) {
            // Delete physically if not found
            db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id))
            return
        }
        
        if (note.pendingOp == "create") {
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

    fun deletePhysically(id: String) {
        val db = writableDatabase
        db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_NOTES, "$COLUMN_IS_SYNCED = ?", arrayOf("1"))
    }
}
