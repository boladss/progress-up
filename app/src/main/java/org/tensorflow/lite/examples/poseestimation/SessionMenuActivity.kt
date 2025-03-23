package org.tensorflow.lite.examples.poseestimation

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import org.tensorflow.lite.examples.poseestimation.data.ProgressionType
import java.time.Duration
import java.time.Instant

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
        val columns = arrayOf(
            DatabaseHandler.SESSIONS_COL_ID,
            DatabaseHandler.SESSIONS_COL_START_TIME,
            DatabaseHandler.SESSIONS_COL_END_TIME,
            DatabaseHandler.SESSIONS_COL_PROG_TYPE
        )

        val toViews = intArrayOf(
            R.id.textId, R.id.textStartTime,
            R.id.textEndTime, R.id.textProgressionType
        )

        val adapter = SimpleCursorAdapter(
            this, R.layout.session_list_item, cursor, columns, toViews, 0
        )

        val sessionListView = findViewById<ListView>(R.id.sessionListView)
        sessionListView.adapter = adapter

        // Listener for each session
        sessionListView.setOnItemClickListener { _, _, position, _ ->
            cursor.moveToPosition(position)
            val sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_ID))

            // Open new activity to retrieve session repetitions
//            val intent = Intent(this, )
        }
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