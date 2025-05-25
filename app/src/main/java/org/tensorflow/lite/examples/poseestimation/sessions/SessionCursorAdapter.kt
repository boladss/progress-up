package org.tensorflow.lite.examples.poseestimation.sessions

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import org.tensorflow.lite.examples.poseestimation.R
import org.tensorflow.lite.examples.poseestimation.SessionMenuActivity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SessionCursorAdapter(
    var context: Context,
    private var header: MutableList<SessionHeader>,
    private var childItem: MutableList<MutableList<RepetitionItem>>,
    private val activity: SessionMenuActivity,  // Used to update entries
    private var dbHandler: DatabaseHandler
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
            val textRepCount = convertView!!.findViewById<TextView>(R.id.textRepCount)
            val textStartTime = convertView.findViewById<TextView>(R.id.textStartTime)
//            val textEndTime = convertView.findViewById<TextView>(R.id.textEndTime)
            val textProgressionType = convertView.findViewById<TextView>(R.id.textProgressionType)
            val textMistakesSummary = convertView.findViewById<TextView>(R.id.textMistakesSummary)
            val textMistakesSummaryTitle = convertView.findViewById<TextView>(R.id.textMistakesSummaryTitle)

            // Get values from entry
            val sessionId = sessionData.id
            val startTimeISO = sessionData.startTime
//            val endTimeISO = sessionData.endTime
            val progType = "${sessionData.progressionType} PUSH-UPS"
            val repCount = dbHandler.countTotalReps(sessionId)

            // Format timestamps and text
//            val startTime = "${iso8601ToFormat(startTimeISO)} (ID: $sessionId)"
            val startTime = "${iso8601ToFormat(startTimeISO)}"
//            val endTime = "Ended: ${iso8601ToFormat(endTimeISO)}"
            val repCountNum = "$repCount"
            val repCountText = "$repCount REPS"
            var mistakesSummaryText = ""

            // Handle deleting of session
            val deleteSessionButton = convertView.findViewById<Button>(R.id.deleteSessionButton)
            deleteSessionButton.setOnClickListener {
                val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogTheme)
                builder
                    .setMessage("Are you sure you want to delete this session ($startTime) with $repCountNum logged reps?")
                    .setTitle("Delete Session")
                    .setPositiveButton("Delete") { dialog, which ->
                        dbHandler.deleteSessionData(sessionId)
                        activity.displaySessionData()
                    }
                    .setNegativeButton("Cancel") {_, _ -> }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }

            // Summarize mistake data
            val mistakesSummary = dbHandler.summarizeMistakeData(sessionId)
            if (mistakesSummary.isNotEmpty()) {
                mistakesSummaryText = mistakesSummary.joinToString("\n")
                textMistakesSummary.visibility = View.VISIBLE
                textMistakesSummaryTitle.visibility = View.VISIBLE
            } else {
                textMistakesSummary.visibility = View.GONE
                textMistakesSummaryTitle.visibility = View.GONE
            }

            textProgressionType.text = progType
            textRepCount.text = repCountText
            textStartTime.text = startTime
//            textEndTime.text = endTime
            textMistakesSummary.text = mistakesSummaryText

        } else {
            val textRepCount = convertView!!.findViewById<TextView>(R.id.textRepCount)
            val errorText = "Error: Session data is null"
            textRepCount.text = errorText
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
            val textRepMistakes = convertView.findViewById<TextView>(R.id.textRepMistakes)

            // Get values from entry
            val repId = repetitionData.id
            val sessionId = repetitionData.sessionId
            val repCount = repetitionData.repCount
            val repQuality = repetitionData.goodQuality
            
            // Format text
//            val idNumber = "(ID: $repId)"
            val idNumber = ""
            val repCountText = "Rep #$repCount"
            val repQualityText = if (repQuality) "Good" else "Poor"
            var repMistakesText = ""

            // List mistakes if bad quality
            if (!repQuality) {
                val mistakesArray = dbHandler.readMistakeData(sessionId, repCount)
                if (mistakesArray.isNotEmpty()) {
                    repMistakesText = mistakesArray.joinToString("\n")
                    textRepMistakes.visibility = View.VISIBLE
                } else {
                    // This shouldn't happen (but in case it does, makes it look better)
                    textRepMistakes.visibility = View.GONE
                }
            } else {
                // Remove the TextView when good
                textRepMistakes.visibility = View.GONE
            }

            textRepId.text = idNumber
            textRepCount.text = repCountText
            textRepQuality.text = repQualityText
            textRepMistakes.text = repMistakesText

            val repQualityColorId = if (repQuality) R.color.green else R.color.red
            textRepQuality.setTextColor(ContextCompat.getColor(textRepQuality.context, repQualityColorId))

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
//            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")
            localDateTime.format(formatter)
        } catch (e: Exception) {
            null
        }
    }
}