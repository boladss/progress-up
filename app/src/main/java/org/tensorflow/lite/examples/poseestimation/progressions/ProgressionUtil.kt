package org.tensorflow.lite.examples.poseestimation.progressions

import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

fun computeDistOfTwoParts(keypoints : List<KeyPoint>, part1: BodyPart, part2: BodyPart): Float {
    return sqrt(
        abs(keypoints[part1.ordinal].coordinate.x - keypoints[part2.ordinal].coordinate.x).pow(2) +
            abs(keypoints[part1.ordinal].coordinate.y - keypoints[part2.ordinal].coordinate.y).pow(2))
}