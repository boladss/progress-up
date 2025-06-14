package org.tensorflow.lite.examples.poseestimation

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.examples.poseestimation.progressions.ProgressionTypes
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import org.tensorflow.lite.examples.poseestimation.sessions.RepetitionItem
import org.tensorflow.lite.examples.poseestimation.sessions.SessionCursorAdapter
import org.tensorflow.lite.examples.poseestimation.sessions.SessionHeader
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter



class SessionMenuActivity : AppCompatActivity() {

    private lateinit var dbHandler: DatabaseHandler
//    private lateinit var addSessionButton: Button
    private lateinit var deleteSessionButton: Button
    private lateinit var deleteIdText: EditText

    private var progression: ProgressionTypes? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_session_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHandler = DatabaseHandler(this)

        // Defaults to null when no extras are inputted
        progression = intent.extras?.getInt("progressionType")?.let {
            ProgressionTypes.fromInt(it)
        }

        displaySessionData()
    }

    fun displaySessionData() {
        val cursor = dbHandler.readSessionData(progression)
        val sessionListView = findViewById<ExpandableListView>(R.id.sessionListView)
        val noSessionsTextView = findViewById<TextView>(R.id.noSessionsTextView)

        val header: MutableList<SessionHeader> = ArrayList() // Sessions
        val childItem: MutableList<MutableList<RepetitionItem>> = ArrayList() // Repetitions

        if (cursor.count > 0) {
            // Go through each session
            if (cursor.moveToFirst()) {
                do {
                    // Get values from entry
                    val sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_ID))
                    val startTimeISO = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_START_TIME))
                    val endTimeISO =  cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_END_TIME))
                    val progType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_PROG_TYPE))

                    val sessionHeader = SessionHeader(sessionId, startTimeISO, endTimeISO, progType)
                    header.add(sessionHeader)

                    // Obtain repetitions per session
                    val repetitions: MutableList<RepetitionItem> = ArrayList()
                    val repetitionCursor = dbHandler.readRepetitionData(sessionId)

                    if (repetitionCursor.moveToFirst()) {
                        do {
                            val repId = repetitionCursor.getLong(repetitionCursor.getColumnIndexOrThrow(DatabaseHandler.REPS_COL_ID))
                            val repCount = repetitionCursor.getInt(repetitionCursor.getColumnIndexOrThrow(DatabaseHandler.REPS_COL_REP_NUM))
                            val goodQual = repetitionCursor.getInt(repetitionCursor.getColumnIndexOrThrow(DatabaseHandler.REPS_COL_GOOD_QUAL)) == 1 // Acts as boolean

                            val repetitionItem = RepetitionItem(repId, sessionId, repCount, goodQual)
                            repetitions.add(repetitionItem)

                        } while (repetitionCursor.moveToNext())
                    }
                    repetitionCursor.close()
                    childItem.add(repetitions)

                } while (cursor.moveToNext())
            }
            cursor.close()
            sessionListView.visibility = View.VISIBLE
            noSessionsTextView.visibility = View.GONE
            sessionListView.setAdapter(SessionCursorAdapter(this, header, childItem, this, dbHandler))

        } else { // Display message denoting lack of sessions
            cursor.close()
            sessionListView.visibility = View.GONE
            noSessionsTextView.visibility = View.VISIBLE
        }


     }

    private fun createNewSession() {
        // Temporary values
        val startTime = Instant.now()
        val endTime = Instant.now().plus(Duration.ofMinutes(30))
        val progressionType = ProgressionTypes.INCLINE

        if (startTime != null && endTime != null) {
            dbHandler.insertSessionData(startTime, endTime, progressionType)
        }

        displaySessionData()
    }
}