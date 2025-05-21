package org.tensorflow.lite.examples.poseestimation.progressions

import android.media.MediaPlayer
import android.provider.ContactsContract.Data
import org.tensorflow.lite.examples.poseestimation.calculateAngle
import org.tensorflow.lite.examples.poseestimation.data.Angles
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import org.tensorflow.lite.examples.poseestimation.data.LeftParts
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.data.RightParts
import org.tensorflow.lite.examples.poseestimation.data.Sides
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private val progression = ProgressionTypes.KNEE
private val Standards = mapOf(
    Pair("STANDARD_LOWER_TORSO_ANGLE", 180),
    Pair("STANDARD_UPPER_TORSO_ANGLE", 180),
    Pair("STANDARD_ELBOW_ANGLE", 180),
    Pair("STANDARD_LOWER_TORSO_DOF", 15),
    Pair("STANDARD_UPPER_TORSO_DOF", 10),
    Pair("STANDARD_ELBOW_DOF", 20), // degrees of freedom --- INCREASED TO 10
    Pair("STANDARD_KNEE_ANGLE", 90),
    Pair("STANDARD_KNEE_DOF", 45)
)
private val SubStandards = mapOf(
    Pair("STANDARD_LOWER_TORSO_ANGLE", 180),
    Pair("STANDARD_UPPER_TORSO_ANGLE", 180),
    Pair("STANDARD_ELBOW_ANGLE", 180),
    Pair("STANDARD_LOWER_TORSO_DOF", 25),
    Pair("STANDARD_UPPER_TORSO_DOF", 20),
    Pair("STANDARD_ELBOW_DOF", 20),
    Pair("STANDARD_KNEE_ANGLE", 90),
    Pair("STANDARD_KNEE_DOF", 45)
)

fun checkValidityKnee(person: Person) : Person {
    return genericValidityCheck(person, Standards, SubStandards)
}

fun getFeedbackKnee(currentState: ProgressionState, person:Person, dbHandler: DatabaseHandler, mediaPlayer: MediaPlayer) : ProgressionState {
    val angles = person.angles
    val keypoints = person.keyPoints

    var feedback = currentState.feedback.toMutableList()
    var errors = currentState.errors.toMutableSet()
    var (totalReps, badReps, goodReps) = currentState.reps

    val mainSide = person.mainSide
    val subSide = person.subSide

    //figure out where head is relative to hands
//    var facingLeft : Boolean = if (keypoints[mainSide.shoulder].coordinate.x > keypoints[mainSide.wrist].coordinate.x) true else false

    when (currentState.state) {
        ProgressionStates.INITIALIZE -> {
            //if the head is facing towards the top of the phone
            if (keypoints[BodyPart.NOSE.position].coordinate.y < keypoints[BodyPart.LEFT_SHOULDER.position].coordinate.y ||
                keypoints[BodyPart.NOSE.position].coordinate.y < keypoints[BodyPart.RIGHT_SHOULDER.position].coordinate.y)
                currentState.headPointingUp = true
            else
                currentState.headPointingUp = false

            //wait until the body is in the correct state
            val pointB = Pair(keypoints[mainSide.wrist].coordinate.x, keypoints[mainSide.wrist].coordinate.y)
            val pointC = Pair(keypoints[mainSide.knee].coordinate.x, keypoints[mainSide.knee].coordinate.y)
            val pointA = Pair(keypoints[mainSide.wrist].coordinate.x, keypoints[mainSide.knee].coordinate.y)
            if (listOf("LElbow", "LLTorso", "LKnee", "RElbow", "RLTorso", "RKnee").any {!angles[it]!!.valid} ||
                (calculateAngle(pointA, pointB, pointC) > 10)) { //knees shouldn't be too far from parallel
                currentState.feedback =
                    listOf("Angle: ${calculateAngle(pointA, pointB, pointC)}")
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
            if (listOf("LElbow", "LLTorso", "LKnee", "RElbow", "RLTorso", "RKnee").all {angles[it]!!.valid}) {
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
                errors.add("Knees should be around 90 degrees.")
                currentState.errorCounter.reset()
            }

            //hands shouldn't go above shoulders (pike)
            if ((currentState.headPointingUp && keypoints[mainSide.shoulder].coordinate.y > keypoints[mainSide.wrist].coordinate.y + 30) ||
                (!currentState.headPointingUp && keypoints[mainSide.shoulder].coordinate.y < keypoints[mainSide.wrist].coordinate.y - 30)) {
                currentState.goodForm = false
                errors.add("Hands are not under shoulders.")
            }
            if ((currentState.headPointingUp && keypoints[subSide.shoulder].coordinate.y > keypoints[subSide.wrist].coordinate.y + 30) ||
                (!currentState.headPointingUp && keypoints[subSide.shoulder].coordinate.y < keypoints[subSide.wrist].coordinate.y - 30)) {
                currentState.goodForm = false
                errors.add("Hands are not under shoulders.")
            }
            //hands shouldn't be too close to hips (pseudo-planche)
            val midX = if (currentState.headPointingUp)
                (2 * keypoints[mainSide.shoulder].coordinate.y + keypoints[mainSide.hip].coordinate.y) / 3 else
                (keypoints[mainSide.shoulder].coordinate.y + 2 * keypoints[mainSide.hip].coordinate.y) / 3
            if (!((midX - keypoints[mainSide.shoulder].coordinate.y < 0) xor (midX - keypoints[mainSide.hip].coordinate.y < 0))) {
                //checks if wrist is in the middle of the shoulder and 1/4 of the hip-shoulder line
                //basically, subtract the middle point to the outer points. one result should be negative, the other positive.
                currentState.goodForm = false
                errors.add("Hands too close to hips.")
            }

            //check if feet are level with knees
            var pointB = Pair(keypoints[mainSide.wrist].coordinate.x, keypoints[mainSide.wrist].coordinate.y)
            var pointC = Pair(keypoints[mainSide.knee].coordinate.x, keypoints[mainSide.knee].coordinate.y)
            var pointA = Pair(keypoints[mainSide.wrist].coordinate.x, keypoints[mainSide.knee].coordinate.y)
            if (calculateAngle(pointA, pointB, pointC) > 10) {
                currentState.goodForm = false
                errors.add(if (mainSide.side == Sides.LEFT) "Right hand not level with knees." else "Left hand not level with knees.")
            }

            currentState.errors = errors

            if (currentState.down && angles[mainSide.elbowAngle]!!.valid && currentState.lowestArmDist < 0.8 * currentState.startingArmDist)
                currentState.state = ProgressionStates.GOINGUP
            return currentState
        }
        ProgressionStates.GOINGUP -> {
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