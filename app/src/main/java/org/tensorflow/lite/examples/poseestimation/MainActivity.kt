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
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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
        wallPushUpButton.setDescription("Leaning against the wall.")
        wallPushUpButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.WALL.ordinal)
            startActivity(intent)
        }
        wallPushUpButton.getSessionLogsButton().setOnClickListener() {
            val intent = Intent(this, SessionMenuActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.WALL.ordinal)
            startActivity(intent)
        }

        // 1 - INCLINE PUSH-UPS
        inclinePushUpButton = findViewById<SelectPushUpView>(R.id.inclinePushUpButton)
        inclinePushUpButton.setImageSrc(R.drawable.incline_push_up)
        inclinePushUpButton.setTitle("INCLINE PUSH-UP")
        inclinePushUpButton.setDescription("With hands elevated.")
        inclinePushUpButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.INCLINE.ordinal)
            startActivity(intent)
        }
        inclinePushUpButton.getSessionLogsButton().setOnClickListener() {
            val intent = Intent(this, SessionMenuActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.INCLINE.ordinal)
            startActivity(intent)
        }


        // 2 - KNEE PUSH-UPS
        kneePushUpButton = findViewById<SelectPushUpView>(R.id.kneePushUpButton)
        kneePushUpButton.setImageSrc(R.drawable.knee_push_up)
        kneePushUpButton.setTitle("KNEE PUSH-UP")
        kneePushUpButton.setDescription("With knees as the fulcrum.")
        kneePushUpButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.KNEE.ordinal)
            startActivity(intent)
        }
        kneePushUpButton.getSessionLogsButton().setOnClickListener() {
            val intent = Intent(this, SessionMenuActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.KNEE.ordinal)
            startActivity(intent)
        }

        // 3 - STANDARD PUSH-UPS
        standardPushUpButton = findViewById<SelectPushUpView>(R.id.standardPushUpButton)
        standardPushUpButton.setImageSrc(R.drawable.standard_push_up)
        standardPushUpButton.setTitle("STANDARD PUSH-UP")
        standardPushUpButton.setDescription("A regular push-up.")
        standardPushUpButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.STANDARD.ordinal)
            startActivity(intent)
        }
        standardPushUpButton.getSessionLogsButton().setOnClickListener() {
            val intent = Intent(this, SessionMenuActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.STANDARD.ordinal)
            startActivity(intent)
        }

        // 4 - DECLINE PUSH-UPS
        declinePushUpButton = findViewById<SelectPushUpView>(R.id.declinePushUpButton)
        declinePushUpButton.setImageSrc(R.drawable.decline_push_up)
        declinePushUpButton.setTitle("DECLINE PUSH-UP")
        declinePushUpButton.setDescription("With feet elevated.")
        declinePushUpButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.DECLINE.ordinal)
            startActivity(intent)
        }
        declinePushUpButton.getSessionLogsButton().setOnClickListener() {
            val intent = Intent(this, SessionMenuActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.DECLINE.ordinal)
            startActivity(intent)
        }

        // 5 - PSEUDO-PLANCHE PUSH-UPS
        pseudoPlanchePushUpButton = findViewById<SelectPushUpView>(R.id.pseudoPlanchePushUpButton)
        pseudoPlanchePushUpButton.setImageSrc(R.drawable.pseudo_planche_push_up)
        pseudoPlanchePushUpButton.setTitle("PSEUDO-PLANCHE PUSH-UP")
        pseudoPlanchePushUpButton.setDescription("Mimicking the planche hold.")
        pseudoPlanchePushUpButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.PSEUDOPLANCHE.ordinal)
            startActivity(intent)
        }
        pseudoPlanchePushUpButton.getSessionLogsButton().setOnClickListener() {
            val intent = Intent(this, SessionMenuActivity::class.java)
            intent.putExtra("progressionType", ProgressionTypes.PSEUDOPLANCHE.ordinal)
            startActivity(intent)
        }
    }
}

