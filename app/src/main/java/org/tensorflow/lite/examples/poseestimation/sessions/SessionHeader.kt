package org.tensorflow.lite.examples.poseestimation.sessions

data class SessionHeader(
    val id: Long,
    val startTime: String,
    val endTime: String,
    val progressionType: String
)