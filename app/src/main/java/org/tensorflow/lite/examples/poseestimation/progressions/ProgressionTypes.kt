package org.tensorflow.lite.examples.poseestimation.progressions

import org.tensorflow.lite.examples.poseestimation.data.Person

enum class ProgressionTypes(val getFeedback: (Person) -> Pair<Boolean, List<String>>, val getValidity: (Person) -> Person) {
    WALL(::getFeedbackWall, ::checkValidityStandard),
    INCLINE(::getFeedbackWall, ::checkValidityStandard),
    KNEE(::getFeedbackWall, ::checkValidityStandard),
    STANDARD(::getFeedbackStandard, ::checkValidityStandard),
    DECLINE(::getFeedbackWall, ::checkValidityStandard),
    PPPU(::getFeedbackWall, ::checkValidityStandard);
    companion object{
        private val map = ProgressionTypes.entries.associateBy{it.ordinal}
        fun fromInt(position: Int): ProgressionTypes = map.getValue(position)
    }
}