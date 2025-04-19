package org.tensorflow.lite.examples.poseestimation.progressions

import org.tensorflow.lite.examples.poseestimation.data.Angles
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private val progression = ProgressionTypes.PSEUDOPLANCHE
private val Standards = mapOf(
    Pair("STANDARD_LOWER_TORSO_ANGLE", 180),
    Pair("STANDARD_UPPER_TORSO_ANGLE", 180),
    Pair("STANDARD_ELBOW_ANGLE", 180),
    Pair("STANDARD_LOWER_TORSO_DOF", 25),
    Pair("STANDARD_UPPER_TORSO_DOF", 20),
    Pair("STANDARD_ELBOW_DOF", 20), // degrees of freedom --- INCREASED TO 10
    Pair("STANDARD_KNEE_ANGLE", 180),
    Pair("STANDARD_KNEE_DOF", 25)
)

fun checkValidityPseudoPlanche(person: Person) : Person {
    Angles.entries.forEach {
        person.angles[it.name]!!.valid = it.check(person.angles[it.name]!!.value, Standards)
    }
    return person
}

fun getFeedbackPseudoPlanche(currentState: ProgressionState, person:Person, dbHandler: DatabaseHandler) : ProgressionState {
    val angles = person.angles
    val keypoints = person.keyPoints
    val startingArmDist = currentState.startingArmDist
    val lowestArmDist = currentState.lowestArmDist

    var feedback = currentState.feedback.toMutableList()
    val errors = currentState.errors.toMutableSet()
    var (totalReps, badReps, goodReps) = currentState.reps

    when (currentState.state) {
        ProgressionStates.INITIALIZE -> {
            //wait until the body is in the correct state
            if (!angles[Angles.LElbow.name]!!.valid || //|| angleValidity["LElbow"]!!) &&
                !angles[Angles.LLTorso.name]!!.valid || //|| angleValidity["LLTorso"]!!) &&
                !angles[Angles.LKnee.name]!!.valid) {
                currentState.feedback =
                    listOf("${angles[Angles.LElbow.name]!!.valid} | ${angles[Angles.LLTorso.name]!!.valid} | ${angles[Angles.LKnee.name]!!.valid}")
                return currentState
            }
            else {
                currentState.sessionId = dbHandler.insertSessionData(Instant.now(), Instant.now(), progression)
                currentState.feedback = listOf("Good: $goodReps | Bad: $badReps | Total: $totalReps")
                currentState.state = ProgressionStates.START
                //change starting arm dist from actual distance to only x distance
                currentState.startingArmDist = abs(keypoints[BodyPart.LEFT_SHOULDER.ordinal].coordinate.x - keypoints[BodyPart.LEFT_WRIST.ordinal].coordinate.x)
                return currentState
            }
        }
        ProgressionStates.START -> {
            //wait until valid again
            if (angles[Angles.LElbow.name]!!.valid && //|| angleValidity["LElbow"]!!) &&
                angles[Angles.LLTorso.name]!!.valid && //|| angleValidity["LLTorso"]!!) &&
                angles[Angles.LKnee.name]!!.valid) { //|| angleValidity["LKnee"]!!)))
                if (feedback.last() != "| Next Rep")
                    feedback.add("| Next Rep")
                currentState.startingArmDist = abs(keypoints[BodyPart.LEFT_SHOULDER.ordinal].coordinate.x - keypoints[BodyPart.LEFT_WRIST.ordinal].coordinate.x)
                currentState.feedback = feedback
                currentState.state = ProgressionStates.GOINGDOWN
                currentState.goodForm = true
                currentState.errors = setOf()
                currentState.lowestArmDist = 9999999f
                return currentState
            } else return currentState
        }
        ProgressionStates.GOINGDOWN -> {
            //track hand and shoulder distance

            if (feedback.last() != " | GoingDown")
                feedback.add(" | GoingDown")
            val currentArmDist = abs(keypoints[BodyPart.LEFT_SHOULDER.ordinal].coordinate.x - keypoints[BodyPart.LEFT_WRIST.ordinal].coordinate.x)

            if (currentArmDist < lowestArmDist) {
                currentState.lowestArmDist = currentArmDist
            }

            if (!angles[Angles.LElbow.name]!!.valid && currentArmDist < 0.8 * startingArmDist) //assume push up has started once elbow bends
                currentState.down = true

            if (!angles[Angles.LLTorso.name]!!.valid || //!angleValidity["LLTorso"]!! || //check if the torso buckles
                !angles[Angles.LKnee.name]!!.valid){// || !angleValidity["LKnee"]!!){ // or the knees buckle
                currentState.goodForm = false
                errors.add("Body buckling")
            }

            //check if hands are in the bottom 60% of hips to shoulders
            if (keypoints[BodyPart.LEFT_WRIST.ordinal].coordinate.y >
                keypoints[BodyPart.LEFT_HIP.ordinal].coordinate.y +
                   0.6 * abs(keypoints[BodyPart.LEFT_SHOULDER.ordinal].coordinate.y - keypoints[BodyPart.LEFT_HIP.ordinal].coordinate.y)
                )
            {// && abs(pixels[6].y - pixels[10].y) > 100){
                currentState.goodForm = false
                errors.add("Hands should be closer to hips")
            }

            //check if feet are level with hands
            //figure out where head is relative to hands
            //todo: change to nose
            val isLowYDown : Boolean = if (keypoints[BodyPart.LEFT_SHOULDER.ordinal].coordinate.x > keypoints[BodyPart.LEFT_WRIST.ordinal].coordinate.x) true else false

//            if ((isLowYDown && pixels[15].x < pixels[9].x) ||
//                (!isLowYDown && pixels[15].x > pixels[9].x)){
//                goodForm = false
//                errors.add("Hands not aligned with feet")
//            }

            currentState.errors = errors
            currentState.feedback = feedback

            if (currentState.down && angles[Angles.LElbow.name]!!.valid && currentState.lowestArmDist < 0.8 * startingArmDist)
                currentState.state = ProgressionStates.GOINGUP
            return currentState
        }
        ProgressionStates.GOINGUP -> {
            //check range of motion here, make rep bad if not enough
            if (currentState.goodForm && startingArmDist - lowestArmDist < 0.5 * startingArmDist) {
                errors.add("ROM not enough")
                currentState.goodForm = false
            }

            totalReps++
            if (currentState.goodForm) {
                dbHandler.insertRepetitionData(currentState.sessionId, totalReps, true)
                goodReps++
            } else {
                dbHandler.insertRepetitionData(currentState.sessionId, totalReps, false)
                badReps++
            }
            currentState.reps = Triple(totalReps, badReps, goodReps)

            if (currentState.goodForm)  {
                currentState.feedback = listOf("Good: $goodReps | Bad: $badReps | Total: $totalReps | Rep good")
            }
            else {
                feedback = mutableListOf("Good: $goodReps | Bad: $badReps | Total: $totalReps | Errors:\n")
                errors.forEach{
                    feedback.add(it + "\n")
                }
                currentState.feedback = feedback
            }

            if (computeDistOfTwoParts(keypoints, BodyPart.LEFT_SHOULDER, BodyPart.LEFT_WRIST) < startingArmDist - 10 &&
                person.angles[Angles.LElbow.name]!!.valid) {
                //wait until close to start
                return currentState
            }
            else {
                currentState.startingArmDist = computeDistOfTwoParts(keypoints, BodyPart.LEFT_SHOULDER, BodyPart.LEFT_WRIST)
                currentState.state = ProgressionStates.START
                return currentState
            }
        }
        else -> return currentState
    }
}