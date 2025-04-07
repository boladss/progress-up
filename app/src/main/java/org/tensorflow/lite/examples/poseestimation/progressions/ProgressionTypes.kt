package org.tensorflow.lite.examples.poseestimation.progressions

import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler

enum class ProgressionTypes(val processHeuristics: (ProgressionState, Person, DatabaseHandler) -> ProgressionState, val getValidity: (Person) -> Person) {
    WALL(::getFeedbackStandard, ::checkValidityStandard),
    INCLINE(::getFeedbackStandard, ::checkValidityStandard),
    KNEE(::getFeedbackStandard, ::checkValidityStandard),
    STANDARD(::getFeedbackStandard, ::checkValidityStandard),
    DECLINE(::getFeedbackStandard, ::checkValidityStandard),
    PSEUDOPLANCHE(::getFeedbackStandard, ::checkValidityStandard);
    companion object{
        private val map = ProgressionTypes.entries.associateBy{it.ordinal}
        fun fromInt(position: Int): ProgressionTypes = map.getValue(position)
    }
}

enum class ProgressionStates {
    INITIALIZE,
    START,
    GOINGDOWN,
    GOINGUP,
}

data class ProgressionState(
    var sessionId: Long,
    var reps : Triple<Int, Int, Int>,
    var state: ProgressionStates,
    var startingArmDist: Float,
    var feedback: List<String>,
    var goodForm: Boolean,
    var lowestArmDist: Float,
    var errors: Set<String>,
    var down: Boolean
)