package org.tensorflow.lite.examples.poseestimation.progressions

import android.media.MediaPlayer
import org.tensorflow.lite.examples.poseestimation.calculateAngle
import org.tensorflow.lite.examples.poseestimation.data.Angles
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.BodySide
import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import org.tensorflow.lite.examples.poseestimation.data.LeftParts
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.data.RightParts
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

fun computeDistOfTwoParts(keypoints : List<KeyPoint>, part1: BodyPart, part2: BodyPart): Float {
    return sqrt(
        (keypoints[part1.ordinal].coordinate.x - keypoints[part2.ordinal].coordinate.x).pow(2) +
            (keypoints[part1.ordinal].coordinate.y - keypoints[part2.ordinal].coordinate.y).pow(2))
}

fun genericValidityCheck(person: Person, standards: Map<String, Int>, subStandards: Map<String, Int>) : Person {
    val subAngles = person.subSide.getAngles()
    Angles.entries.forEach {
        if (it.name in subAngles)
            person.angles[it.name]!!.valid = it.check(person.angles[it.name]!!.value, subStandards)
        else
            person.angles[it.name]!!.valid = it.check(person.angles[it.name]!!.value, standards)
    }

    val keypoints = person.keyPoints
    val mainSide = person.mainSide
    val facingLeft = (keypoints[mainSide.shoulder].coordinate.x > keypoints[mainSide.wrist].coordinate.x)
    val facingUp = (keypoints[BodyPart.NOSE.position].coordinate.y < keypoints[BodyPart.LEFT_SHOULDER.position].coordinate.y ||
            keypoints[BodyPart.NOSE.position].coordinate.y < keypoints[BodyPart.RIGHT_SHOULDER.position].coordinate.y)

    if ((facingUp && facingLeft) || (!facingUp && !facingLeft)) {
        person.mainSide = LeftParts
        person.subSide = RightParts
    } else {
        person.mainSide = RightParts
        person.subSide = LeftParts
    }
    return person
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

fun addRep(currentState: ProgressionState, dbHandler: DatabaseHandler) : Triple<Int, Int, Int> {
    var (totalReps, badReps, goodReps) = currentState.reps
    val errors = currentState.errors.toMutableSet()
    totalReps++
        if (currentState.goodForm) {
            goodReps++
            if (totalReps > 0) dbHandler.insertRepetitionData(currentState.sessionId, totalReps, true)
        } else {
            badReps++
            if (totalReps > 0) {
                dbHandler.insertRepetitionData(currentState.sessionId, totalReps, false)
                dbHandler.insertMistakeData(currentState.sessionId, totalReps, errors)
            }
        }
    return Triple(totalReps, badReps, goodReps)
}

fun processFeedback(currentState: ProgressionState) : List<String> {
    val (totalReps, badReps, goodReps) = currentState.reps
    val errors = currentState.errors.toMutableSet()
    if (currentState.goodForm)  {
        if (totalReps > 0)
            currentState.feedback = listOf("Good rep!")
    }
    else {
        val feedback = mutableListOf("Rep $totalReps errors:\n")
        val errorList = errors.toList()
        feedback.addAll(errorList.dropLast(1).map { it + "\n" })
        errorList.lastOrNull()?.let { feedback.add(it) }
        currentState.feedback = feedback
    }
    return currentState.feedback
}

fun areArmsOnSameSide (keypoints: List<KeyPoint>, mainSide: BodySide, subSide: BodySide) : Boolean {
    val main = (keypoints[mainSide.shoulder].coordinate.x < keypoints[mainSide.wrist].coordinate.x)
    val sub = (keypoints[subSide.shoulder].coordinate.x < keypoints[subSide.wrist].coordinate.x)

    return !(main xor sub)
}

//fun playAudio(mediaPlayer: MediaPlayer, file: Int) {
//    if (mediaPlayer.isPlaying) {
//        mediaPlayer.pause()
//        mediaPlayer.seekTo(0)
//    }
//    mediaPlayer.setDataSource()
//}