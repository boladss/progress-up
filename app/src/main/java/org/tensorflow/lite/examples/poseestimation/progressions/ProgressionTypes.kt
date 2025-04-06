package org.tensorflow.lite.examples.poseestimation.progressions

import org.tensorflow.lite.examples.poseestimation.data.Person

enum class ProgressionTypes(val getFeedback: (Person) -> Pair<Boolean, List<String>>) {
    WALL(::getFeedbackWall),
    INCLINE(::getFeedbackWall),
    KNEE(::getFeedbackWall),
    STANDARD(::getFeedbackStandard),
    DECLINE(::getFeedbackWall),
    PPPU(::getFeedbackWall);
    companion object{
        private val map = ProgressionTypes.entries.associateBy{it.ordinal}
        fun fromInt(position: Int): ProgressionTypes = map.getValue(position)
    }
}