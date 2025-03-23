package org.tensorflow.lite.examples.poseestimation.sessions

import android.content.Context
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.tensorflow.lite.examples.poseestimation.data.ProgressionType
import java.time.Instant
import java.time.format.DateTimeFormatter
import android.widget.Toast

// Reference hehe: https://www.youtube.com/watch?v=3u8vb6Sw8hk
class DatabaseHandler(private val context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    companion object {
        const val DATABASE_NAME = "ProgressUpDB"
        const val SESSIONS_TABLE_NAME = "Sessions"
        const val SESSIONS_COL_ID = "_id"
        const val SESSIONS_COL_START_TIME = "startTime"
        const val SESSIONS_COL_END_TIME = "endTime"
        const val SESSIONS_COL_PROG_TYPE = "progressionType"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Timestamps stored as text in ISO-8601 format
        val createSessionTable = """
            CREATE TABLE $SESSIONS_TABLE_NAME ( 
                $SESSIONS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $SESSIONS_COL_START_TIME TEXT,
                $SESSIONS_COL_END_TIME TEXT,
                $SESSIONS_COL_PROG_TYPE TEXT
            );
            """.trimIndent()

        db?.execSQL(createSessionTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $SESSIONS_TABLE_NAME")
        onCreate(db)
    }

    fun insertSessionData(startTime: Instant, endTime: Instant, progressionType: ProgressionType): Long {
        val db = writableDatabase
        val values = ContentValues()
        values.put(SESSIONS_COL_START_TIME, instantToISO8601(startTime))
        values.put(SESSIONS_COL_END_TIME, instantToISO8601(endTime))
        values.put(SESSIONS_COL_PROG_TYPE, progressionType.name) // Input progressionType as string
        val id = db.insert(SESSIONS_TABLE_NAME, null, values)

        if (id > 0) {
            Toast.makeText(context, "Session saved successfully (ID $id)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save session", Toast.LENGTH_SHORT).show()
        }

        // Upon creating the session entry and obtaining ID, need to store the individual repetitions associated and their mistakes

        return id
    }

    fun readSessionData(): Cursor {
        val db = readableDatabase
        val readDataQuery = "SELECT * FROM $SESSIONS_TABLE_NAME"
        return db.rawQuery(readDataQuery, null)
    }

    fun updateSessionData(id: Int, startTime: Instant, endTime: Instant, progressionType: ProgressionType): Int {
        val db = writableDatabase
        val values = ContentValues()
        values.put(SESSIONS_COL_START_TIME, instantToISO8601(startTime))
        values.put(SESSIONS_COL_END_TIME, instantToISO8601(endTime))
        values.put(SESSIONS_COL_PROG_TYPE, progressionType.name) // Input progressionType as string
        return db.update(SESSIONS_TABLE_NAME, values, "$SESSIONS_COL_ID=?", arrayOf(id.toString()))
    }

    fun deleteSessionData(id: Int): Int {
        val db = writableDatabase
        return db.delete(SESSIONS_TABLE_NAME, "$SESSIONS_COL_ID=?", arrayOf(id.toString()))
    }

    fun instantToISO8601(instant: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }
}