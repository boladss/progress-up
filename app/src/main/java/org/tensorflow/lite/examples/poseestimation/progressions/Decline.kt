package org.tensorflow.lite.examples.poseestimation.progressions

import android.media.MediaPlayer
import android.provider.ContactsContract.Data
import org.tensorflow.lite.examples.poseestimation.calculateAngle
import org.tensorflow.lite.examples.poseestimation.data.Angles
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.BodySide
import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import org.tensorflow.lite.examples.poseestimation.data.LeftParts
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.data.RightParts
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private val progression = ProgressionTypes.DECLINE
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
private val SubStandards = mapOf(
    Pair("STANDARD_LOWER_TORSO_ANGLE", 180),
    Pair("STANDARD_UPPER_TORSO_ANGLE", 180),
    Pair("STANDARD_ELBOW_ANGLE", 180),
    Pair("STANDARD_LOWER_TORSO_DOF", 25),
    Pair("STANDARD_UPPER_TORSO_DOF", 20),
    Pair("STANDARD_ELBOW_DOF", 45), // degrees of freedom --- INCREASED TO 10
    Pair("STANDARD_KNEE_ANGLE", 180),
    Pair("STANDARD_KNEE_DOF", 25)
)

private fun isFeetHigherThanHands (keypoints: List<KeyPoint>, mainSide: BodySide, facingLeft: Boolean) : Boolean {
    val anklesHigherThanShoulders : Boolean = (
            (facingLeft && keypoints[mainSide.ankle].coordinate.x > keypoints[mainSide.shoulder].coordinate.x) ||
                    (!facingLeft && keypoints[mainSide.ankle].coordinate.x < keypoints[mainSide.shoulder].coordinate.x)
            )

    val pointB = Pair(keypoints[mainSide.shoulder].coordinate.x, keypoints[mainSide.shoulder].coordinate.y)
    val pointC = Pair(keypoints[mainSide.ankle].coordinate.x, keypoints[mainSide.ankle].coordinate.y)
    val pointA = Pair(keypoints[mainSide.shoulder].coordinate.x, keypoints[mainSide.ankle].coordinate.y)
    val torsoCloseToParallel : Boolean = (calculateAngle(pointA, pointB, pointC) < 10)

    return (anklesHigherThanShoulders || torsoCloseToParallel)
}

private fun areHandsUnderShoulders (keypoints: List<KeyPoint>, mainSide: BodySide, subSide: BodySide) : Boolean {
    var pointB = Pair(keypoints[mainSide.shoulder].coordinate.x, keypoints[mainSide.shoulder].coordinate.y)
    var pointC = Pair(keypoints[mainSide.wrist].coordinate.x, keypoints[mainSide.wrist].coordinate.y)
    var pointA = Pair(keypoints[mainSide.wrist].coordinate.x, keypoints[mainSide.shoulder].coordinate.y)
    val main : Boolean = (calculateAngle(pointA, pointB, pointC) < 35)

    pointB = Pair(keypoints[subSide.shoulder].coordinate.x, keypoints[subSide.shoulder].coordinate.y)
    pointC = Pair(keypoints[subSide.wrist].coordinate.x, keypoints[subSide.wrist].coordinate.y)
    pointA = Pair(keypoints[subSide.wrist].coordinate.x, keypoints[subSide.shoulder].coordinate.y)
    val sub : Boolean = (calculateAngle(pointA, pointB, pointC) < 45)

    return main && sub
}

fun checkValidityDecline(person: Person) : Person {
    return genericValidityCheck(person, Standards, SubStandards)
}

fun getFeedbackDecline(currentState: ProgressionState, person:Person, dbHandler: DatabaseHandler, mediaPlayer: MediaPlayer) : ProgressionState {
    val angles = person.angles
    val keypoints = person.keyPoints

    var feedback = currentState.feedback.toMutableList()
    var errors = currentState.errors.toMutableSet()
    var (totalReps, badReps, goodReps) = currentState.reps

    val mainSide = person.mainSide
    val subSide = person.subSide

    //figure out where head is relative to hands
    var facingLeft : Boolean = if (keypoints[mainSide.shoulder].coordinate.x > keypoints[mainSide.wrist].coordinate.x) true else false

    when (currentState.state) {
        ProgressionStates.INITIALIZE -> {
            //if the head is facing towards the top of the phone
            if (keypoints[BodyPart.NOSE.position].coordinate.y < keypoints[BodyPart.LEFT_SHOULDER.position].coordinate.y ||
                keypoints[BodyPart.NOSE.position].coordinate.y < keypoints[BodyPart.RIGHT_SHOULDER.position].coordinate.y)
                currentState.headPointingUp = true
            else
                currentState.headPointingUp = false

            //wait until the body is in the correct state
            val handsUnderShoulders = areHandsUnderShoulders(keypoints, mainSide, subSide)
            if (listOf("LElbow", "LLTorso", "LKnee", "RElbow", "RLTorso", "RKnee").any {!angles[it]!!.valid} ||
                !areArmsOnSameSide(keypoints, mainSide, subSide) ||
                !handsUnderShoulders ||
                !isFeetHigherThanHands(keypoints, mainSide, facingLeft)) {
                currentState.feedback =
                    listOf("Initial Form Check:\n" +
                            "Arms: ${angles[mainSide.elbowAngle]!!.valid && angles[subSide.elbowAngle]!!.valid}\n" +
                            "Torso: ${angles[mainSide.lTorsoAngle]!!.valid && angles[subSide.lTorsoAngle]!!.valid}\n" +
                            "Legs: ${angles[mainSide.kneeAngle]!!.valid && angles[subSide.kneeAngle]!!.valid}\n" +
                            "Wrists under shoulders: $handsUnderShoulders")
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
                areArmsOnSameSide(keypoints, mainSide, subSide)) {
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
            val currentArmDist = computeDistOfTwoParts(keypoints, BodyPart.fromInt(mainSide.shoulder), BodyPart.fromInt(mainSide.wrist))
            if (currentArmDist < currentState.lowestArmDist)
                currentState.lowestArmDist = currentArmDist

            if (!angles[mainSide.elbowAngle]!!.valid && !angles[subSide.elbowAngle]!!.valid && currentArmDist < 0.8 * currentState.startingArmDist) //assume push up has started once elbows bend
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

            //ensure ankles are always higher than wrists
            if ((facingLeft && keypoints[mainSide.ankle].coordinate.x < keypoints[mainSide.wrist].coordinate.x) ||
                (!facingLeft && keypoints[mainSide.ankle].coordinate.x > keypoints[mainSide.wrist].coordinate.x)) {
                currentState.goodForm = false
                errors.add("Keep your feet higher than your hands.")
            }

            currentState.errors = errors

            if (currentState.down && angles[mainSide.elbowAngle]!!.valid && currentState.lowestArmDist < 0.8 * currentState.startingArmDist)
                currentState.state = ProgressionStates.GOINGUP
            return currentState
        }
        ProgressionStates.GOINGUP -> {
            //check if hands are under shoulders
            if (!areHandsUnderShoulders(keypoints, mainSide, subSide)) {
                currentState.goodForm = false
                errors.add("Hands not under shoulders.")
            }

            //check height of feet
            if (!isFeetHigherThanHands(keypoints, mainSide, facingLeft)) {
                currentState.goodForm = false
                errors.add("Feet should be higher than hands.")
            }

            //check range of motion here, make rep bad if not enough
            if (currentState.goodForm && currentState.startingArmDist - currentState.lowestArmDist < 0.3 * currentState.startingArmDist) {
                errors.add("Not enough range of motion.")
                currentState.goodForm = false
            }

            //inform the user that the rep is counted
            currentState.feedback = listOf("Rep done! \n Return to start position for feedback.")

            if (computeDistOfTwoParts(keypoints, BodyPart.fromInt(mainSide.shoulder), BodyPart.fromInt(mainSide.wrist)) < currentState.startingArmDist - 10 &&
                !person.angles[mainSide.elbowAngle]!!.valid) {
                //wait until close to start
                return currentState
            }
            else {
                currentState.startingArmDist = computeDistOfTwoParts(keypoints, BodyPart.fromInt(mainSide.shoulder), BodyPart.fromInt(mainSide.wrist))
                currentState.state = ProgressionStates.START
                return currentState
            }
        }
        else -> return currentState
    }
}