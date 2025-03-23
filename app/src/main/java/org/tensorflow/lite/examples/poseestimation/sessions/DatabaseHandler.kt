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

        // Sessions Table
        const val SESSIONS_TABLE_NAME = "Sessions"
        const val SESSIONS_COL_ID = "_id" // Seems to be necessary when using
        const val SESSIONS_COL_START_TIME = "startTime"
        const val SESSIONS_COL_END_TIME = "endTime"
        const val SESSIONS_COL_PROG_TYPE = "progressionType" // WALL, INCLINE, KNEE, etc.

        // Repetitions Table
        const val REPS_TABLE_NAME = "Repetitions"
        const val REPS_COL_ID = "_id"
        const val REPS_COL_SESSION_ID = "sessionId"
        const val REPS_COL_REP_NUM = "repetitionNumber" // 1st rep in session, 2nd rep, etc.
        const val REPS_COL_GOOD_QUAL = "goodQuality"

        // Mistakes Table
        const val MISTAKES_TABLE_NAME = "Mistakes"
        const val MISTAKES_COL_ID = "_id"
        const val MISTAKES_COL_SESSION_ID = "sessionId"
        const val MISTAKES_COL_REPS_ID = "repId"
        const val MISTAKES_COL_TYPE = "mistakeType" // The specific error in form cue
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Workout Sessions Table
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

        // Repetitions Table
        val createRepetitionsTable = """
            CREATE TABLE $REPS_TABLE_NAME (
                $REPS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $REPS_COL_SESSION_ID INTEGER,
                $REPS_COL_REP_NUM INTEGER,
                $REPS_COL_GOOD_QUAL INTEGER,
                FOREIGN KEY($REPS_COL_SESSION_ID) REFERENCES $SESSIONS_TABLE_NAME($SESSIONS_COL_ID)
            );
            """.trimIndent()
        db?.execSQL(createRepetitionsTable)

        // Mistakes Table
        val createMistakesTable = """
            CREATE TABLE $MISTAKES_TABLE_NAME (
                $MISTAKES_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $MISTAKES_COL_SESSION_ID INTEGER,
                $MISTAKES_COL_REPS_ID INTEGER,
                $MISTAKES_COL_TYPE TEXT,
                FOREIGN KEY($MISTAKES_COL_SESSION_ID) REFERENCES $SESSIONS_TABLE_NAME($SESSIONS_COL_ID),
                FOREIGN KEY($MISTAKES_COL_REPS_ID) REFERENCES $REPS_TABLE_NAME($REPS_COL_ID)
            );
        """.trimIndent()
        db?.execSQL(createMistakesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $SESSIONS_TABLE_NAME")
        db?.execSQL("DROP TABLE IF EXISTS $REPS_TABLE_NAME")
        onCreate(db)
    }

    // SESSION TABLE FUNCTIONS
    fun insertSessionData(startTime: Instant, endTime: Instant, progressionType: ProgressionType): Long {
        val db = writableDatabase
        val values = ContentValues()
        values.put(SESSIONS_COL_START_TIME, instantToISO8601(startTime))
        values.put(SESSIONS_COL_END_TIME, instantToISO8601(endTime))
        values.put(SESSIONS_COL_PROG_TYPE, progressionType.name) // Input progressionType as string
        val id = db.insert(SESSIONS_TABLE_NAME, null, values)

        // Notify user of updates
        if (id > 0) {
            Toast.makeText(context, "Session saved successfully (ID $id)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Session failed to save", Toast.LENGTH_SHORT).show()
        }

        // Upon creating the session entry and obtaining ID, need to store the individual repetitions associated and their mistakes

        return id
    }

    fun readSessionData(): Cursor {
        val db = readableDatabase
        val readDataQuery = "SELECT * FROM $SESSIONS_TABLE_NAME"
        return db.rawQuery(readDataQuery, null)
    }

    fun updateSessionData(id: Int, progressionType: ProgressionType): Int {
        val db = writableDatabase
        val values = ContentValues()
        values.put(SESSIONS_COL_PROG_TYPE, progressionType.name) // Input progressionType as string
        return db.update(SESSIONS_TABLE_NAME, values, "$SESSIONS_COL_ID=?", arrayOf(id.toString()))
    }

    fun deleteSessionData(id: Int): Int {
        val db = writableDatabase
        return db.delete(SESSIONS_TABLE_NAME, "$SESSIONS_COL_ID=?", arrayOf(id.toString()))
    }

    // REPETITION TABLE FUNCTIONS
    fun insertRepetitionData(sessionId: Int, repNumber: Int, goodQuality: Boolean): Long {
        val db = writableDatabase
        val values = ContentValues()
        values.put(REPS_COL_SESSION_ID, sessionId)
        values.put(REPS_COL_REP_NUM, repNumber)
        values.put(REPS_COL_GOOD_QUAL, goodQuality)
        val id = db.insert(REPS_TABLE_NAME, null, values)

        // Notify user of updates in table again
        if (id > 0) {
            Toast.makeText(context, "Repetition saved successfully (ID $id)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Repetition failed to save", Toast.LENGTH_SHORT).show()
        }

        return id
    }

    fun readRepetitionData(sessionId: Int): Cursor {
        val db = readableDatabase
        val readDataQuery = "SELECT * FROM $REPS_TABLE_NAME WHERE $REPS_COL_SESSION_ID = $sessionId"
        return db.rawQuery(readDataQuery, null)
    }

    // HELPER FUNCTIONS
    fun instantToISO8601(instant: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }
}