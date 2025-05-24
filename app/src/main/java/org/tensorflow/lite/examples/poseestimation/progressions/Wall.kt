package org.tensorflow.lite.examples.poseestimation.progressions

import android.media.MediaPlayer
import android.provider.ContactsContract.Data
import org.tensorflow.lite.examples.poseestimation.R
import org.tensorflow.lite.examples.poseestimation.calculateAngle
import org.tensorflow.lite.examples.poseestimation.data.Angles
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.BodySide
import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import org.tensorflow.lite.examples.poseestimation.data.LeftParts
import org.tensorflow.lite.examples.poseestimation.data.RightParts
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.data.Sides
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private val progression = ProgressionTypes.WALL
private val Standards = mapOf(
    Pair("STANDARD_LOWER_TORSO_ANGLE", 180),
    Pair("STANDARD_UPPER_TORSO_ANGLE", 180),
    Pair("STANDARD_ELBOW_ANGLE", 180),
    Pair("STANDARD_LOWER_TORSO_DOF", 20),
    Pair("STANDARD_UPPER_TORSO_DOF", 20),
    Pair("STANDARD_ELBOW_DOF", 25), // degrees of freedom --- INCREASED TO 10
    Pair("STANDARD_KNEE_ANGLE", 180),
    Pair("STANDARD_KNEE_DOF", 25)
)
private val SubStandards = mapOf(
    Pair("STANDARD_LOWER_TORSO_ANGLE", 180),
    Pair("STANDARD_UPPER_TORSO_ANGLE", 180),
    Pair("STANDARD_ELBOW_ANGLE", 180),
    Pair("STANDARD_LOWER_TORSO_DOF", 20),
    Pair("STANDARD_UPPER_TORSO_DOF", 20),
    Pair("STANDARD_ELBOW_DOF", 90), // degrees of freedom --- INCREASED TO 10
    Pair("STANDARD_KNEE_ANGLE", 180),
    Pair("STANDARD_KNEE_DOF", 25)
)

fun checkValidityWall(person: Person) : Person {
    return genericValidityCheck(person, Standards, SubStandards)
}

private fun hasValidArmAngle(keypoints: List<KeyPoint>, mainSide: BodySide, subSide: BodySide) : Boolean {
    val mainAngle = calculateAngle(keypoints[mainSide.elbow].coordinate, keypoints[mainSide.shoulder].coordinate, keypoints[mainSide.hip].coordinate)
    val subAngle = calculateAngle(keypoints[subSide.elbow].coordinate, keypoints[subSide.shoulder].coordinate, keypoints[subSide.hip].coordinate)

    return (mainAngle > 50 && subAngle > 45)
}

fun getFeedbackWall(currentState: ProgressionState, person:Person, dbHandler: DatabaseHandler, mediaPlayer: MediaPlayer) : ProgressionState {
    val angles = person.angles
    val keypoints = person.keyPoints

    var feedback = currentState.feedback.toMutableList()
    val errors = currentState.errors.toMutableSet()
    var (totalReps, badReps, goodReps) = currentState.reps

    val mainSide = person.mainSide
    val subSide = person.subSide

    //figure out where head is relative to hands
    var facingLeft : Boolean = if (keypoints[mainSide.shoulder].coordinate.x > keypoints[mainSide.wrist].coordinate.x) true else false

    when (currentState.state) {

        ProgressionStates.INITIALIZE -> {
            //assume head is always facing up
            currentState.headPointingUp = true

            //wait until the body is in the correct state
            if (listOf("LElbow", "LLTorso", "LKnee", "RElbow", "RLTorso", "RKnee").any {!angles[it]!!.valid} ||
                (!areArmsOnSameSide(keypoints, mainSide, subSide)) ||
                !hasValidArmAngle(keypoints, mainSide, subSide)) {
                currentState.feedback =
                    listOf("Initial Form Check:\n" +
                            // "Arms: ${angles[mainSide.elbowAngle]!!.valid && angles[subSide.elbowAngle]!!.valid}\n" +
                            "Arms: ${hasValidArmAngle(keypoints, mainSide, subSide)}\n" +
                            "Torso: ${angles[mainSide.lTorsoAngle]!!.valid && angles[subSide.lTorsoAngle]!!.valid}\n" +
                            "Legs: ${angles[mainSide.kneeAngle]!!.valid && angles[subSide.kneeAngle]!!.valid}\n")
                return currentState
            }
            else {
                currentState.errorCounter.startPosition++ //using this as a counter for start position
                if (currentState.errorCounter.startPosition >= 3) {
                    currentState.errorCounter.startPosition = 0
                    currentState.sessionId =
                        dbHandler.insertSessionData(Instant.now(), Instant.now(), progression)
                    currentState.feedback = listOf("Starting workout...\nStay in starting position!")
                    currentState.state = ProgressionStates.START
                    currentState.startingArmDist = computeDistOfTwoParts(
                        keypoints,
                        BodyPart.fromInt(mainSide.shoulder),
                        BodyPart.fromInt(mainSide.wrist)
                    )
                }
                return currentState
            }
        }
        ProgressionStates.START -> {
            //wait until valid again
            if (listOf("LElbow", "LLTorso", "LKnee", "RElbow", "RLTorso", "RKnee").all {angles[it]!!.valid} &&
                areArmsOnSameSide(keypoints, mainSide, subSide) &&
                hasValidArmAngle(keypoints, mainSide, subSide)){
                currentState.errorCounter.startPosition++
                if (currentState.errorCounter.startPosition >= END_WAIT_FRAMES) {
                    //process new rep
                    currentState.reps = addRep(currentState, dbHandler)
                    currentState.feedback = processFeedback(currentState)
                    currentState.errorCounter.reset()
                    currentState.state = ProgressionStates.GOINGDOWN
                    currentState.prevRepGood = currentState.goodForm
                    currentState.goodForm = true
                    currentState.errors = setOf()
                    currentState.lowestArmDist = 9999999f
                }
                return currentState
            } else return currentState
        }
        ProgressionStates.GOINGDOWN -> {
            //track hand and shoulder distance
            val currentArmDist = computeTriangleHeight(keypoints, BodyPart.fromInt(mainSide.wrist), BodyPart.fromInt(mainSide.hip), BodyPart.fromInt(mainSide.shoulder))
            if (currentArmDist < currentState.lowestArmDist)
                currentState.lowestArmDist = currentArmDist

            if (!angles[mainSide.elbowAngle]!!.valid && !angles[subSide.elbowAngle]!!.valid && currentArmDist < 0.9 * currentState.startingArmDist) //assume push up has started once elbows bend
                currentState.down = true

            if (listOf(mainSide.lTorsoAngle, subSide.lTorsoAngle).any {!angles[it]!!.valid}) //check if the torso buckles
                currentState.errorCounter.torsoBuckling++
            else if (currentState.errorCounter.torsoBuckling > 0) currentState.errorCounter.torsoBuckling--
            if (currentState.errorCounter.torsoBuckling >= 3) {
                currentState.goodForm = false
                errors.add("Torso is buckling.")
                currentState.errorCounter.reset()
            }

            if (listOf(mainSide.kneeAngle, subSide.kneeAngle).any {!angles[it]!!.valid}) // check if the knees buckle
                currentState.errorCounter.kneesBuckling++
            else if (currentState.errorCounter.kneesBuckling > 0) currentState.errorCounter.kneesBuckling--
            if (currentState.errorCounter.kneesBuckling >= 3) {
                currentState.goodForm = false
                errors.add("Knees are buckling.")
                currentState.errorCounter.reset()
            }

            //check if hands are lower than shoulders
            if ((keypoints[mainSide.wrist].coordinate.y < keypoints[mainSide.shoulder].coordinate.y) ||
                (keypoints[subSide.wrist].coordinate.y < keypoints[subSide.shoulder].coordinate.y)) {
                currentState.goodForm = false
                errors.add("Hands are not under shoulders.")
            }

            currentState.errors = errors

            if (currentState.down && angles[mainSide.elbowAngle]!!.valid && currentState.lowestArmDist < 0.9 * currentState.startingArmDist)
                currentState.state = ProgressionStates.GOINGUP
            return currentState
        }
        ProgressionStates.GOINGUP -> {
            //check range of motion here, make rep bad if not enough
            if (currentState.goodForm && currentState.startingArmDist - currentState.lowestArmDist < 0.1 * currentState.startingArmDist) {
                errors.add("Not enough range of motion.")
                currentState.goodForm = false
            }

            //inform the user that the rep is counted
            currentState.feedback = listOf("Rep done! \n Return to start position for feedback.")

            if (computeTriangleHeight(keypoints, BodyPart.fromInt(mainSide.wrist), BodyPart.fromInt(mainSide.hip), BodyPart.fromInt(mainSide.shoulder)) < currentState.startingArmDist - 15 ||
                !person.angles[mainSide.elbowAngle]!!.valid) {
                //wait until close to start
                return currentState
            }
            else {
                currentState.startingArmDist = computeTriangleHeight(keypoints, BodyPart.fromInt(mainSide.wrist), BodyPart.fromInt(mainSide.hip), BodyPart.fromInt(mainSide.shoulder))
                currentState.state = ProgressionStates.START
                return currentState
            }
        }
        else -> return currentState
    }
}