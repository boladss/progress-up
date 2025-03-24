package org.tensorflow.lite.examples.poseestimation.data

enum class ProgressionType(val index: Int) {
    WALL(0),
    INCLINE(1),
    KNEE(2),
    STANDARD(3),
    DECLINE(4),
    PSEUDOPLANCHE(5);

    companion object {
        private val map = entries.associateBy(ProgressionType::index)
        fun fromIndex(index: Int): ProgressionType? = map[index]
    }
}