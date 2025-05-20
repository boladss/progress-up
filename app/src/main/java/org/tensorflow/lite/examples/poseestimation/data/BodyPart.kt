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

package org.tensorflow.lite.examples.poseestimation.data

enum class BodyPart(val position: Int) {
    NOSE(0),
    LEFT_EYE(1),
    RIGHT_EYE(2),
    LEFT_EAR(3),
    RIGHT_EAR(4),
    LEFT_SHOULDER(5),
    RIGHT_SHOULDER(6),
    LEFT_ELBOW(7),
    RIGHT_ELBOW(8),
    LEFT_WRIST(9),
    RIGHT_WRIST(10),
    LEFT_HIP(11),
    RIGHT_HIP(12),
    LEFT_KNEE(13),
    RIGHT_KNEE(14),
    LEFT_ANKLE(15),
    RIGHT_ANKLE(16);
    companion object {
        private val map = entries.associateBy(BodyPart::position)
        fun fromInt(position: Int): BodyPart = map.getValue(position)
    }
}

enum class Sides () {
    LEFT,
    RIGHT
}

data class BodySide (
    val side : Sides,
    val eye : Int,
    val ear : Int,
    val shoulder : Int,
    val elbow : Int,
    val wrist : Int,
    val hip : Int,
    val knee : Int,
    val ankle : Int,
    val elbowAngle : String,
    val kneeAngle : String,
    val lTorsoAngle : String,
    val uTorsoAngle : String,
) {
    fun getSideInts() : List<Int> {
        return listOf(eye, ear, shoulder, elbow, wrist, hip, knee, ankle)
    }
    fun getAngles() : List<String> {
        return listOf(elbowAngle, kneeAngle, lTorsoAngle, uTorsoAngle)
    }
}

val LeftParts = BodySide(
    side = Sides.LEFT,
    eye = BodyPart.LEFT_EYE.position,
    ear = BodyPart.LEFT_EAR.position,
    shoulder = BodyPart.LEFT_SHOULDER.position,
    elbow = BodyPart.LEFT_ELBOW.position,
    wrist = BodyPart.LEFT_WRIST.position,
    hip = BodyPart.LEFT_HIP.position,
    knee = BodyPart.LEFT_KNEE.position,
    ankle = BodyPart.LEFT_ANKLE.position,
    elbowAngle = "LElbow",
    kneeAngle = "LKnee",
    lTorsoAngle = "LLTorso",
    uTorsoAngle = "LUTorso",
)

val RightParts = BodySide(
    side = Sides.RIGHT,
    eye = BodyPart.RIGHT_EYE.position,
    ear = BodyPart.RIGHT_EAR.position,
    shoulder = BodyPart.RIGHT_SHOULDER.position,
    elbow = BodyPart.RIGHT_ELBOW.position,
    wrist = BodyPart.RIGHT_WRIST.position,
    hip = BodyPart.RIGHT_HIP.position,
    knee = BodyPart.RIGHT_KNEE.position,
    ankle = BodyPart.RIGHT_ANKLE.position,
    elbowAngle = "RElbow",
    kneeAngle = "RKnee",
    lTorsoAngle = "RLTorso",
    uTorsoAngle = "RUTorso",
)