package org.tensorflow.lite.examples.poseestimation.components

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.tensorflow.lite.examples.poseestimation.R

// Reference used: https://medium.com/@hossam.dev92/building-custom-ui-components-in-android-with-kotlin-a-step-by-step-guide-871efab2ebe1
class SelectPushUpView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val titleTextView: TextView
    private val descriptionTextView: TextView
    private val progFormGuideButton: Button
    private val progSessionLogsButton: Button

    init {
        inflate(context, R.layout.select_push_up_view, this)
        imageView = findViewById(R.id.imageView)
        titleTextView = findViewById(R.id.titleTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        progFormGuideButton = findViewById(R.id.progFormGuideButton)
        progSessionLogsButton = findViewById(R.id.progSessionLogsButton)

        isClickable = true
    }

    fun setImageSrc(drawableResId: Int) {
        imageView.setImageResource(drawableResId)
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setDescription(description: String) {
        descriptionTextView.text = description
    }

    // Passes the button for the per-progression form guides
    fun getFormGuideButton(): Button {
        return progFormGuideButton
    }

    // Passes the button for the per-progression session logs
    fun getSessionLogsButton(): Button {
        return progSessionLogsButton
    }
}