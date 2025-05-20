package org.tensorflow.lite.examples.poseestimation.data

data class ErrorTypes(
    var torsoBuckling: Int = 0,
    var kneesBuckling: Int = 0,
    var startPosition: Int = 0
) {
    fun reset() {
        torsoBuckling = 0
        kneesBuckling = 0
        startPosition = 0
    }
}