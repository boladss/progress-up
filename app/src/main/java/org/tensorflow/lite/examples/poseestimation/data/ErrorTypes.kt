package org.tensorflow.lite.examples.poseestimation.data

data class ErrorTypes(
    var buckling: Int = 0,
    var startPosition: Int = 0
) {
    fun reset() {
        buckling = 0
    }
}