package org.tensorflow.lite.examples.poseestimation.progressions

import org.tensorflow.lite.examples.poseestimation.data.Person

fun getFeedbackWall(person:Person) : Pair<Boolean, List<String>> {
    return Pair(true, listOf("null"))
}