package org.tensorflow.lite.examples.poseestimation.sessions

import android.content.Context
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.Instant
import java.time.format.DateTimeFormatter
import android.widget.Toast
import org.tensorflow.lite.examples.poseestimation.progressions.ProgressionTypes

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
        const val MISTAKES_COL_REP_NUM = "repetitionNumber"
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
                $MISTAKES_COL_REP_NUM INTEGER,
                $MISTAKES_COL_TYPE TEXT,
                FOREIGN KEY($MISTAKES_COL_SESSION_ID) REFERENCES $SESSIONS_TABLE_NAME($SESSIONS_COL_ID),
                FOREIGN KEY($MISTAKES_COL_REP_NUM) REFERENCES $REPS_TABLE_NAME($REPS_COL_REP_NUM)
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
    fun insertSessionData(startTime: Instant, endTime: Instant, progressionType: ProgressionTypes): Long {
        val db = writableDatabase
        val values = ContentValues()
        values.put(SESSIONS_COL_START_TIME, instantToISO8601(startTime))
        values.put(SESSIONS_COL_END_TIME, instantToISO8601(endTime))
        values.put(SESSIONS_COL_PROG_TYPE, progressionType.name) // Input progressionType as string
        val id = db.insert(SESSIONS_TABLE_NAME, null, values)

        // Notify user of updates
//        if (id > 0) {
//            Toast.makeText(context, "Session saved successfully (ID $id)", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(context, "Session failed to save", Toast.LENGTH_SHORT).show()
//        }

        // Upon creating the session entry and obtaining ID, need to store the individual repetitions associated and their mistakes

        return id
    }

    fun readSessionData(): Cursor {
        val db = readableDatabase
        val readDataQuery = "SELECT * FROM $SESSIONS_TABLE_NAME"
        return db.rawQuery(readDataQuery, null)
    }

    fun updateSessionData(id: Long, progressionType: ProgressionTypes): Int {
        val db = writableDatabase
        val values = ContentValues()
        values.put(SESSIONS_COL_PROG_TYPE, progressionType.name) // Input progressionType as string
        return db.update(SESSIONS_TABLE_NAME, values, "$SESSIONS_COL_ID=?", arrayOf(id.toString()))
    }

    fun deleteSessionData(sessionId: Long): Int {
        val db = writableDatabase

        // Before deleting a session, must delete all corresponding repetitions (and mistakes) with the same sessionId
        val deletedMistakes = db.delete(MISTAKES_TABLE_NAME, "$MISTAKES_COL_SESSION_ID=?", arrayOf(sessionId.toString()))
        val deletedRepetitions = db.delete(REPS_TABLE_NAME, "$REPS_COL_SESSION_ID=?", arrayOf(sessionId.toString()))

        // Delete session
        val deletedSession = db.delete(SESSIONS_TABLE_NAME, "$SESSIONS_COL_ID=?", arrayOf(sessionId.toString()))

        // Notify user of updates
        if (deletedSession > 0) {
            Toast.makeText(context, "Session deleted successfully (ID $sessionId)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Session failed to delete", Toast.LENGTH_SHORT).show()
        }

        return deletedSession
    }

    // Count total repetitions per session
    fun countTotalReps(sessionId: Long): Int {
        val db = readableDatabase
        val countTotalRepsQuery = "SELECT * FROM $REPS_TABLE_NAME WHERE $REPS_COL_SESSION_ID = $sessionId"
        var count = 0

        db.rawQuery(countTotalRepsQuery, null).use { cursor ->
            count = cursor.count
        }

        return count
    }

    // REPETITION TABLE FUNCTIONS
    fun insertRepetitionData(sessionId: Long, repNumber: Int, goodQuality: Boolean): Long {
        val db = writableDatabase
        val values = ContentValues()
        values.put(REPS_COL_SESSION_ID, sessionId)
        values.put(REPS_COL_REP_NUM, repNumber)
        values.put(REPS_COL_GOOD_QUAL, goodQuality)
        val id = db.insert(REPS_TABLE_NAME, null, values)

//        // Notify user of updates in table again
//        if (id > 0) {
//            Toast.makeText(context, "Repetition saved successfully (ID $id)", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(context, "Repetition failed to save", Toast.LENGTH_SHORT).show()
//        }

        return id
    }

    fun readRepetitionData(sessionId: Long): Cursor {
        val db = readableDatabase
        val readDataQuery = "SELECT * FROM $REPS_TABLE_NAME WHERE $REPS_COL_SESSION_ID = $sessionId"
        return db.rawQuery(readDataQuery, null)
    }

    // Add entry for a list of mistakes done in a repetition
    fun insertMistakeData(sessionId: Long, repNumber: Int, mistakes: Set<String>): List<Long> {
        val db = writableDatabase
        val insertedIds = mutableListOf<Long>()

        for (mistake in mistakes) {
            val values = ContentValues()
            values.put(MISTAKES_COL_SESSION_ID, sessionId)
            values.put(MISTAKES_COL_REP_NUM, repNumber)
            values.put(MISTAKES_COL_TYPE, mistake)
            val id = db.insert(MISTAKES_TABLE_NAME, null, values)
            insertedIds.add(id)
        }

        return insertedIds
    }

    // Fetch list of mistakes done for a repetition
    fun readMistakeData(sessionId: Long, repNumber: Int): List<String> {
        val db = readableDatabase
        val mistakesQuery =
            "SELECT * FROM $MISTAKES_TABLE_NAME WHERE $MISTAKES_COL_SESSION_ID = $sessionId AND $MISTAKES_COL_REP_NUM = $repNumber"

        val mistakes = mutableListOf<String>()
        val cursor = db.rawQuery(mistakesQuery, null)

        // Append all mistakes to a list
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val mistake = it.getString(it.getColumnIndexOrThrow(MISTAKES_COL_TYPE))
                    mistakes.add(mistake)
                } while (it.moveToNext())
            }
        }

        return mistakes
    }

    // Fetch summarized list of mistakes
    fun summarizeMistakeData(sessionId: Long): List<String> {
        val db = readableDatabase
        val mistakesQuery = """
            SELECT 
                $MISTAKES_COL_TYPE, 
                COUNT(*) AS mistakeCount, 
                GROUP_CONCAT('#' || $MISTAKES_COL_REP_NUM, ', ') AS mistakeRepNumbers
            FROM
                $MISTAKES_TABLE_NAME
            WHERE 
                $MISTAKES_COL_SESSION_ID = $sessionId
            GROUP BY
                $MISTAKES_COL_TYPE
            ORDER BY
                mistakeCount DESC;
        """.trimIndent()

        val mistakesSummary = mutableListOf<String>()
        val cursor = db.rawQuery(mistakesQuery, null)

        // Format output
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val mistake = it.getString(it.getColumnIndexOrThrow(MISTAKES_COL_TYPE))
                    val mistakeCount = it.getInt(it.getColumnIndexOrThrow("mistakeCount"))
                    val mistakeRepNumbers = it.getString(it.getColumnIndexOrThrow("mistakeRepNumbers"))

                    val formattedString = if (mistakeCount == 1) {
                        "$mistake — $mistakeCount rep ($mistakeRepNumbers)"
                    } else {
                        "$mistake — $mistakeCount reps ($mistakeRepNumbers)"
                    }
                    mistakesSummary.add(formattedString)

                } while (it.moveToNext())
            }
        }

        return mistakesSummary
    }

    // HELPER FUNCTIONS
    fun instantToISO8601(instant: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }
}