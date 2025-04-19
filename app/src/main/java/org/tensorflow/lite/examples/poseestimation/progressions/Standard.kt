package org.tensorflow.lite.examples.poseestimation.progressions

import android.provider.ContactsContract.Data
import org.tensorflow.lite.examples.poseestimation.data.Angles
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

val progression = ProgressionTypes.STANDARD
val Standards = mapOf(
    Pair("STANDARD_LOWER_TORSO_ANGLE", 180),
    Pair("STANDARD_UPPER_TORSO_ANGLE", 180),
    Pair("STANDARD_ELBOW_ANGLE", 180),
    Pair("STANDARD_LOWER_TORSO_DOF", 25),
    Pair("STANDARD_UPPER_TORSO_DOF", 20),
    Pair("STANDARD_ELBOW_DOF", 20), // degrees of freedom --- INCREASED TO 10
    Pair("STANDARD_KNEE_ANGLE", 180),
    Pair("STANDARD_KNEE_DOF", 25)
)

fun checkValidityStandard(person: Person) : Person {
    Angles.entries.forEach {
        person.angles[it.name]!!.valid = it.check(person.angles[it.name]!!.value, Standards)
    }
    return person
}

fun getFeedbackStandard(currentState: ProgressionState, person:Person, dbHandler: DatabaseHandler) : ProgressionState {
    val angles = person.angles
    val keypoints = person.keyPoints
    val startingArmDist = currentState.startingArmDist
    val lowestArmDist = currentState.lowestArmDist

    var feedback = currentState.feedback.toMutableList()
    var errors = currentState.errors.toMutableSet()
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
                currentState.feedback = listOf("Good: ${goodReps} | Bad: ${badReps} | Total: ${totalReps}")
                currentState.state = ProgressionStates.START
                currentState.startingArmDist = computeDistOfTwoParts(keypoints, BodyPart.LEFT_SHOULDER, BodyPart.LEFT_WRIST)
                return currentState
            }
        }
        ProgressionStates.START -> {
            //wait until valid again
            if (angles[Angles.LElbow.name]!!.valid && //|| angleValidity["LElbow"]!!) &&
                angles[Angles.LLTorso.name]!!.valid && //|| angleValidity["LLTorso"]!!) &&
                angles[Angles.LKnee.name]!!.valid) { //|| angleValidity["LKnee"]!!)))
                feedback.add(" | Next rep")
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
            val currentArmDist = sqrt(abs(keypoints[BodyPart.LEFT_SHOULDER.ordinal].coordinate.x - keypoints[BodyPart.LEFT_WRIST.ordinal].coordinate.x).pow(2) +
                    abs(keypoints[BodyPart.LEFT_SHOULDER.ordinal].coordinate.y - keypoints[BodyPart.LEFT_WRIST.ordinal].coordinate.y).pow(2)
            )
            if (currentArmDist < lowestArmDist)
                currentState.lowestArmDist = currentArmDist

            if (!angles[Angles.LElbow.name]!!.valid) //assume push up has started once elbow bends
                currentState.down = true

            if (!angles[Angles.LLTorso.name]!!.valid || //!angleValidity["LLTorso"]!! || //check if the torso buckles
                !angles[Angles.LKnee.name]!!.valid){// || !angleValidity["LKnee"]!!){ // or the knees buckle
                currentState.goodForm = false
                errors.add("Body is buckling.")
            }

            //check if hands are under shoulders
            if (abs(keypoints[BodyPart.LEFT_SHOULDER.ordinal].coordinate.y - keypoints[BodyPart.LEFT_WRIST.ordinal].coordinate.y) > 60){// && abs(pixels[6].y - pixels[10].y) > 100){
                currentState.goodForm = false
                errors.add("Hands not under shoulders.")
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

            if (currentState.down && angles[Angles.LElbow.name]!!.valid && lowestArmDist < 0.8 * startingArmDist)
                currentState.state = ProgressionStates.GOINGUP
            return currentState
        }
        ProgressionStates.GOINGUP -> {
            //check range of motion here, make rep bad if not enough
            if (currentState.goodForm && startingArmDist - lowestArmDist < 0.5 * startingArmDist) {
                errors.add("Not enough range of motion.")
                currentState.goodForm = false
            }

            totalReps++
            if (currentState.goodForm) {
                dbHandler.insertRepetitionData(currentState.sessionId, totalReps, true)
                goodReps++
            } else {
                dbHandler.insertRepetitionData(currentState.sessionId, totalReps, false)
                dbHandler.insertMistakeData(currentState.sessionId, totalReps, errors)
                badReps++
            }
            currentState.reps = Triple(totalReps, badReps, goodReps)

            if (currentState.goodForm)  {
                currentState.feedback = listOf("Good: ${goodReps} | Bad: ${badReps} | Total: ${totalReps} | Rep good")
            }
            else {
                feedback = mutableListOf("Good: ${goodReps} | Bad: ${badReps} | Total: ${totalReps} | Errors:\n")
                errors.forEach{
                    feedback.add(it + "\n")
                }
                currentState.feedback = feedback
            }

            if (computeDistOfTwoParts(keypoints, BodyPart.LEFT_SHOULDER, BodyPart.LEFT_WRIST) < startingArmDist - 10) {
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