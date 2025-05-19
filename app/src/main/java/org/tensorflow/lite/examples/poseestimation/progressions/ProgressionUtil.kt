package org.tensorflow.lite.examples.poseestimation.progressions

import android.media.MediaPlayer
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

fun computeDistOfTwoParts(keypoints : List<KeyPoint>, part1: BodyPart, part2: BodyPart): Float {
    return sqrt(
        (keypoints[part1.ordinal].coordinate.x - keypoints[part2.ordinal].coordinate.x).pow(2) +
            (keypoints[part1.ordinal].coordinate.y - keypoints[part2.ordinal].coordinate.y).pow(2))
}

/**
 * create a triangle with corners part1, part2, and part3
 * then return the height of part1 relative to base part2-part3
 */
fun computeTriangleHeight(keypoints : List<KeyPoint>, part1: BodyPart, part2: BodyPart, part3: BodyPart) : Float {
    //calculate lengths
    val length12 = computeDistOfTwoParts(keypoints, part1, part2)
    val length23 = computeDistOfTwoParts(keypoints, part2, part3)
    val length13 = computeDistOfTwoParts(keypoints, part1, part3)

    //calculate triangle area using heron's formula
    val semiperimeter = (length12 + length23 + length13)/2
    val area = sqrt(semiperimeter * (semiperimeter - length12) * (semiperimeter - length23) * (semiperimeter - length13))

    //use area to compute height, A = bh/2, h = 2A/b
    return 2 * area / length23
}

//fun playAudio(mediaPlayer: MediaPlayer, file: Int) {
//    if (mediaPlayer.isPlaying) {
//        mediaPlayer.pause()
//        mediaPlayer.seekTo(0)
//    }
//    mediaPlayer.setDataSource()
//}