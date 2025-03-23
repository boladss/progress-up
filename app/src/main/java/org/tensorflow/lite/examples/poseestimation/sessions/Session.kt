package org.tensorflow.lite.examples.poseestimation.sessions

import java.time.Instant
import org.tensorflow.lite.examples.poseestimation.data.ProgressionType

// Denotes a workout session, with a startTime, endTime, and corresponding progressionType
class Session {
    var id: Int = 0
    var startTime: Instant = Instant.now()
    var endTime: Instant = Instant.now()
    var progressionType: ProgressionType = ProgressionType.STANDARD

    constructor(startTime: Instant, endTime: Instant, progressionType: ProgressionType) {
        this.startTime = startTime
        this.endTime = endTime
        this.progressionType = progressionType
    }
}