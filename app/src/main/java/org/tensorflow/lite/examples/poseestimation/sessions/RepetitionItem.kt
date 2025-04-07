package org.tensorflow.lite.examples.poseestimation.sessions

data class RepetitionItem(
    val id: Long,
    val sessionId: Long,
    val repCount: Int,
    val goodQuality: Boolean
)
