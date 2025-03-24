package org.tensorflow.lite.examples.poseestimation

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import org.tensorflow.lite.examples.poseestimation.data.ProgressionType
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
    private lateinit var addSessionButton: Button
    private lateinit var deleteSessionButton: Button
    private lateinit var deleteIdText: EditText

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

        addSessionButton = findViewById(R.id.addSessionButton)
        addSessionButton.setOnClickListener { createNewSession() }
        deleteSessionButton = findViewById(R.id.deleteSessionButton)
        deleteSessionButton.setOnClickListener { deleteSession() }

        deleteIdText = findViewById(R.id.deleteIdText)
        deleteIdText.inputType = InputType.TYPE_CLASS_NUMBER // Restrict to numeric inputs

        displaySessionData()
    }

    private fun displaySessionData() {
        val cursor = dbHandler.readSessionData()
//        dbHandler.insertRepetitionData(2, 1, true)
//        dbHandler.insertRepetitionData(2, 2, true)
//        dbHandler.insertRepetitionData(2, 3, false)
//        val adapter = SessionCursorAdapter(this, cursor)
        val sessionListView = findViewById<ExpandableListView>(R.id.sessionListView)
//        sessionListView.adapter = adapter

        val header: MutableList<SessionHeader> = ArrayList() // Sessions
        val childItem: MutableList<MutableList<RepetitionItem>> = ArrayList() // Repetitions

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

                        val repetitionItem = RepetitionItem(repId, repCount, goodQual)
                        repetitions.add(repetitionItem)

                    } while (repetitionCursor.moveToNext())
                }
                repetitionCursor.close()
                childItem.add(repetitions)

            } while (cursor.moveToNext())
        }
        cursor.close()

        sessionListView.setAdapter(SessionCursorAdapter(this, header, childItem))
     }

    private fun createNewSession() {
        // Temporary values
        val startTime = Instant.now()
        val endTime = Instant.now().plus(Duration.ofMinutes(30))
        val progressionType = ProgressionType.INCLINE

        if (startTime != null && endTime != null) {
            dbHandler.insertSessionData(startTime, endTime, progressionType)
        }

        displaySessionData()
    }

    private fun deleteSession() {
        val id = deleteIdText.text.toString()

        // Check if id was inputted
        if (id.isNotBlank()) {
            val deleteRow = dbHandler.deleteSessionData(id.toInt())

            // Reset edit text value and update display
            if (deleteRow > 0) {
                deleteIdText.text.clear()
                displaySessionData()
            }
        }
    }
}