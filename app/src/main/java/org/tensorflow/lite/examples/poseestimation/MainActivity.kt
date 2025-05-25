/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package org.tensorflow.lite.examples.poseestimation

import org.tensorflow.lite.examples.poseestimation.components.SelectPushUpView
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import org.tensorflow.lite.examples.poseestimation.progressions.ProgressionTypes


class MainActivity : AppCompatActivity() {
    private lateinit var wallPushUpButton: SelectPushUpView
    private lateinit var inclinePushUpButton: SelectPushUpView
    private lateinit var kneePushUpButton: SelectPushUpView
    private lateinit var standardPushUpButton: SelectPushUpView
    private lateinit var declinePushUpButton: SelectPushUpView
    private lateinit var pseudoPlanchePushUpButton: SelectPushUpView
    private lateinit var sessionMenuButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Button to open session menu
        sessionMenuButton = findViewById<Button>(R.id.sessionMenuButton)
        sessionMenuButton.setOnClickListener {
            val intent = Intent(this, SessionMenuActivity::class.java)
            startActivity(intent)
        }

        /** PROGRESSION INDEX GUIDE --- For sending data through Intent (progressionType)
         *  See ProgressionTypes.kt, using .ordinal
         **/

        // 0 - WALL PUSH-UPS
        wallPushUpButton = findViewById<SelectPushUpView>(R.id.wallPushUpButton)
        wallPushUpButton.setImageSrc(R.drawable.wall_push_up)
        wallPushUpButton.setTitle("WALL PUSH-UP")
        wallPushUpButton.setDescription("If the other progressions feel hard, start here!")
        wallPushUpButton.setOnClickListener {
            startTracker(ProgressionTypes.WALL.ordinal)
        }
        wallPushUpButton.getSessionLogsButton().setOnClickListener() {
            startSessionMenu(ProgressionTypes.WALL.ordinal)
        }
        wallPushUpButton.getFormGuideButton().setOnClickListener() {
            createProgressionAlertDialog(ProgressionTypes.WALL.toString(), getString(R.string.pu_form_guide_wall))
        }

        // 1 - INCLINE PUSH-UPS
        inclinePushUpButton = findViewById<SelectPushUpView>(R.id.inclinePushUpButton)
        inclinePushUpButton.setImageSrc(R.drawable.incline_push_up)
        inclinePushUpButton.setTitle("INCLINE PUSH-UP")
        inclinePushUpButton.setDescription("Slowly lower your hands to transition from wall push-ups to standard push-ups.")
        inclinePushUpButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.INCLINE.ordinal)
            startActivity(intent)
        }
        inclinePushUpButton.getSessionLogsButton().setOnClickListener() {
            startSessionMenu(ProgressionTypes.INCLINE.ordinal)
        }
        inclinePushUpButton.getFormGuideButton().setOnClickListener() {
            createProgressionAlertDialog(ProgressionTypes.INCLINE.toString(),getString(R.string.pu_form_guide_incline))
        }

        // 2 - KNEE PUSH-UPS
        kneePushUpButton = findViewById<SelectPushUpView>(R.id.kneePushUpButton)
        kneePushUpButton.setImageSrc(R.drawable.knee_push_up)
        kneePushUpButton.setTitle("KNEE PUSH-UP")
        kneePushUpButton.setDescription("Go here when incline push-ups get easy.")
        kneePushUpButton.setOnClickListener {
            startTracker(ProgressionTypes.KNEE.ordinal)
        }
        kneePushUpButton.getSessionLogsButton().setOnClickListener() {
            startSessionMenu(ProgressionTypes.KNEE.ordinal)
        }
        kneePushUpButton.getFormGuideButton().setOnClickListener() {
            createProgressionAlertDialog(ProgressionTypes.KNEE.toString(),getString(R.string.pu_form_guide_knee))
        }

        // 3 - STANDARD PUSH-UPS
        standardPushUpButton = findViewById<SelectPushUpView>(R.id.standardPushUpButton)
        standardPushUpButton.setImageSrc(R.drawable.standard_push_up)
        standardPushUpButton.setTitle("STANDARD PUSH-UP")
        standardPushUpButton.setDescription("A regular push-up.")
        standardPushUpButton.setOnClickListener {
            startTracker(ProgressionTypes.STANDARD.ordinal)
        }
        standardPushUpButton.getSessionLogsButton().setOnClickListener() {
            startSessionMenu(ProgressionTypes.STANDARD.ordinal)
        }
        standardPushUpButton.getFormGuideButton().setOnClickListener() {
            createProgressionAlertDialog(ProgressionTypes.STANDARD.toString(),getString(R.string.pu_form_guide_standard))
        }

        // 4 - DECLINE PUSH-UPS
        declinePushUpButton = findViewById<SelectPushUpView>(R.id.declinePushUpButton)
        declinePushUpButton.setImageSrc(R.drawable.decline_push_up)
        declinePushUpButton.setTitle("DECLINE PUSH-UP")
        declinePushUpButton.setDescription("Raising the legs puts more weight on the hands.")
        declinePushUpButton.setOnClickListener {
            startTracker(ProgressionTypes.DECLINE.ordinal)
        }
        declinePushUpButton.getSessionLogsButton().setOnClickListener() {
            startSessionMenu(ProgressionTypes.DECLINE.ordinal)
        }
        declinePushUpButton.getFormGuideButton().setOnClickListener() {
            createProgressionAlertDialog(ProgressionTypes.DECLINE.toString(),getString(R.string.pu_form_guide_decline))
        }

        // 5 - PSEUDO-PLANCHE PUSH-UPS
        pseudoPlanchePushUpButton = findViewById<SelectPushUpView>(R.id.pseudoPlanchePushUpButton)
        pseudoPlanchePushUpButton.setImageSrc(R.drawable.pseudo_planche_push_up)
        pseudoPlanchePushUpButton.setTitle("PSEUDO-PLANCHE PUSH-UP")
        pseudoPlanchePushUpButton.setDescription("Moving the hands closer to the hips reduces leverage significantly.")
        pseudoPlanchePushUpButton.setOnClickListener {
            startTracker(ProgressionTypes.PSEUDOPLANCHE.ordinal)
        }
        pseudoPlanchePushUpButton.getSessionLogsButton().setOnClickListener() {
            startSessionMenu(ProgressionTypes.PSEUDOPLANCHE.ordinal)
        }
        pseudoPlanchePushUpButton.getFormGuideButton().setOnClickListener() {
            createProgressionAlertDialog(ProgressionTypes.PSEUDOPLANCHE.toString(),getString(R.string.pu_form_guide_pseudoplanche))
        }
    }

    // Display alert dialog containing form guide for the selected progression
    private fun createProgressionAlertDialog(progType: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)

        // Temporary link
        val videoUrl = "https://www.youtube.com/"

        // Create dialog with form guide
        builder
            .setMessage(Html.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setTitle("FORM GUIDE: $progType PUSH-UP")

        // Include button to video guide per progression
        builder.setPositiveButton("Video Guide") { dialog, _ ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No application found to open link.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
        return
    }

    // Start new activity for tracker
    private fun startTracker(progTypeNumber: Int) {
        val intent = Intent(this, TrackerActivity::class.java)
        intent.putExtra("progressionType", progTypeNumber)
        startActivity(intent)
    }

    // Start new activity for session menu
    private fun startSessionMenu(progTypeNumber: Int) {
        val intent = Intent(this, SessionMenuActivity::class.java)
        intent.putExtra("progressionType", progTypeNumber)
        startActivity(intent)
    }
}

