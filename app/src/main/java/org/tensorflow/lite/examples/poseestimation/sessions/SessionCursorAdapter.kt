package org.tensorflow.lite.examples.poseestimation.sessions

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import org.tensorflow.lite.examples.poseestimation.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class SessionCursorAdapter(context: Context, cursor: Cursor?): CursorAdapter(context, cursor, 0) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return inflater.inflate(R.layout.session_list_item, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        // Views
        val textId = view.findViewById<TextView>(R.id.textId)
        val textStartTime = view.findViewById<TextView>(R.id.textStartTime)
        val textEndTime = view.findViewById<TextView>(R.id.textEndTime)
        val textProgressionType = view.findViewById<TextView>(R.id.textProgressionType)

        // Get values from entry
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_ID))
        val startTimeISO = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_START_TIME))
        val endTimeISO = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_END_TIME))
        val progType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.SESSIONS_COL_PROG_TYPE))

        // Format timestamps
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = "Started: ${iso8601ToFormat(startTimeISO)}"
        val endTime = "Ended: ${iso8601ToFormat(endTimeISO)}"
        val idNumber = "ID: $id"

        textProgressionType.text = progType
        textId.text = idNumber
        textStartTime.text = startTime
        textEndTime.text = endTime

    }

    fun iso8601ToFormat(iso8601String: String, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        return try {
            val instant = Instant.parse(iso8601String)
            val localDateTime = LocalDateTime.ofInstant(instant, zoneId)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            localDateTime.format(formatter)
        } catch (e: Exception) {
            null
        }
    }
}