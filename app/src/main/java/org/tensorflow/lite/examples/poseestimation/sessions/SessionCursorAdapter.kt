package org.tensorflow.lite.examples.poseestimation.sessions

import android.content.Context
import android.database.Cursor
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
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

class SessionCursorAdapter(
    var context: Context,
    private var header: MutableList<SessionHeader>,
    private var childItem: MutableList<MutableList<RepetitionItem>>
): BaseExpandableListAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    // Necessary methods for ExpandableAdapter
    override fun getGroupCount(): Int {
        return header.size
    }

    override fun getChildrenCount(p0: Int): Int {
        return childItem[p0].size
    }

    override fun getGroup(p0: Int): Any {
        return header[p0]
    }

    override fun getChild(p0: Int, p1: Int): Any {
        return childItem[p0][p1]
    }

    override fun getGroupId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getChildId(p0: Int, p1: Int): Long {
        return p1.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(p0: Int, p1: Boolean, p2: View?, p3: ViewGroup?): View {
        var convertView = p2

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.session_list_item, null)
        }

        val sessionData = getGroup(p0) as? SessionHeader

        if (sessionData != null) {
            // Views
            val textId = convertView!!.findViewById<TextView>(R.id.textId)
            val textStartTime = convertView.findViewById<TextView>(R.id.textStartTime)
            val textEndTime = convertView.findViewById<TextView>(R.id.textEndTime)
            val textProgressionType = convertView.findViewById<TextView>(R.id.textProgressionType)

            // Get values from entry
            val sessionId = sessionData.id
            val startTimeISO = sessionData.startTime
            val endTimeISO = sessionData.endTime
            val progType = sessionData.progressionType

            // Format timestamps and text
//            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startTime = "Started: ${iso8601ToFormat(startTimeISO)}"
            val endTime = "Ended: ${iso8601ToFormat(endTimeISO)}"
            val idNumber = "ID: $sessionId"

            textProgressionType.text = progType
            textId.text = idNumber
            textStartTime.text = startTime
            textEndTime.text = endTime

        } else {
            val textId = convertView!!.findViewById<TextView>(R.id.textId)
            val errorText = "Error: Session data is null"
            textId.text = errorText
        }

        return convertView
    }

    override fun getChildView(p0: Int, p1: Int, p2: Boolean, p3: View?, p4: ViewGroup?): View {
        var convertView = p3

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.repetition_list_item, null)
        }

        val repetitionData = getChild(p0, p1) as? RepetitionItem

        if (repetitionData != null) {
            // Views
            val textRepId = convertView!!.findViewById<TextView>(R.id.textRepId)
            val textRepCount = convertView.findViewById<TextView>(R.id.textRepCount)
            val textRepQuality = convertView.findViewById<TextView>(R.id.textRepQuality)

            // Get values from entry
            val repId = repetitionData.id
            val repCount = repetitionData.repCount
            val repQuality = repetitionData.goodQuality

            // Format text
            val idNumber = "(ID: $repId)"
            val repCountText = "Rep #$repCount"
            val repQualityText = if (repQuality) "Good" else "Bad"

            textRepId.text = idNumber
            textRepCount.text = repCountText
            textRepQuality.text = repQualityText

        } else {
            val textRepId = convertView!!.findViewById<TextView>(R.id.textRepId)
            val errorText = "Error: Repetition data is null"
            textRepId.text = errorText
        }

        return convertView
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return true
    }

    // Helper function to translate timestamps
    private fun iso8601ToFormat(iso8601String: String, zoneId: ZoneId = ZoneId.systemDefault()): String? {
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