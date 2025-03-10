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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.poseestimation.ml.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var standardPushUpButton: SelectPushUpView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        standardPushUpButton = findViewById<SelectPushUpView>(R.id.standardPushUpButton)
        standardPushUpButton.setImageSrc(R.drawable.standard_push_up)
        standardPushUpButton.setTitle("STANDARD PUSH-UP")
        standardPushUpButton.setDescription("A regular push-up.")

        standardPushUpButton.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            startActivity(intent)
        }
    }
}

