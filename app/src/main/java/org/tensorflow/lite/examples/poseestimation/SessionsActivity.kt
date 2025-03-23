package org.tensorflow.lite.examples.poseestimation

import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import org.tensorflow.lite.examples.poseestimation.data.ProgressionType
import java.time.Duration
import java.time.Instant

class SessionsActivity : AppCompatActivity() {

    private lateinit var dbHandler: DatabaseHandler
    private lateinit var addSessionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sessions)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHandler = DatabaseHandler(this)
        addSessionButton = findViewById(R.id.addSessionButton)
        addSessionButton.setOnClickListener { createNewSession() }

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
}