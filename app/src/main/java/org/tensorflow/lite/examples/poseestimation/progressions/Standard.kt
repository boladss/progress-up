package org.tensorflow.lite.examples.poseestimation.progressions

import android.media.MediaPlayer
import android.provider.ContactsContract.Data
import org.tensorflow.lite.examples.poseestimation.data.Angles
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.BodySide
import org.tensorflow.lite.examples.poseestimation.data.LeftParts
import org.tensorflow.lite.examples.poseestimation.data.RightParts
import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import java.time.Instant
import kotlin.math.round

private val progression = ProgressionTypes.STANDARD
private val Standards = mapOf(
    Pair("STANDARD_LOWER_TORSO_ANGLE", 180),
    Pair("STANDARD_UPPER_TORSO_ANGLE", 180),
    Pair("STANDARD_ELBOW_ANGLE", 180),
    Pair("STANDARD_LOWER_TORSO_DOF", 25),
    Pair("STANDARD_UPPER_TORSO_DOF", 20),
    Pair("STANDARD_ELBOW_DOF", 20),
    Pair("STANDARD_KNEE_ANGLE", 180),
    Pair("STANDARD_KNEE_DOF", 25)
)

fun checkValidityStandard(person: Person) : Person {
    Angles.entries.forEach {
        person.angles[it.name]!!.valid = it.check(person.angles[it.name]!!.value, Standards)
    }
    return person
}

fun getFeedbackStandard(currentState: ProgressionState, person:Person, dbHandler: DatabaseHandler, mediaPlayer: MediaPlayer) : ProgressionState {
    val angles = person.angles
    val keypoints = person.keyPoints

    var feedback = currentState.feedback.toMutableList()
    val errors = currentState.errors.toMutableSet()
    var (totalReps, badReps, goodReps) = currentState.reps

    val mainSide = currentState.mainSide
    val subSide = currentState.subSide

    when (currentState.state) {
        ProgressionStates.INITIALIZE -> {
            //constantly set the mainSide and subSide
            if (keypoints[BodyPart.NOSE.position].coordinate.y < keypoints[BodyPart.LEFT_SHOULDER.position].coordinate.y ||
                keypoints[BodyPart.NOSE.position].coordinate.y < keypoints[BodyPart.RIGHT_SHOULDER.position].coordinate.y) {
                //if the head is facing towards the top of the phone
                currentState.headPointingUp = true
                if (keypoints[BodyPart.LEFT_WRIST.position].coordinate.x < keypoints[BodyPart.LEFT_SHOULDER.position].coordinate.x ||
                    keypoints[BodyPart.RIGHT_WRIST.position].coordinate.x < keypoints[BodyPart.RIGHT_SHOULDER.position].coordinate.x) {
                    //and the arms are facing left
                    //then the primary side is the left side
                        currentState.mainSide = LeftParts
                        currentState.subSide = RightParts
                    }
                //otherwise the primary side is the right side
                else {
                    currentState.mainSide = RightParts
                    currentState.subSide = LeftParts
                }
            }
            else if (keypoints[BodyPart.LEFT_WRIST.position].coordinate.x < keypoints[BodyPart.LEFT_SHOULDER.position].coordinate.x ||
                keypoints[BodyPart.RIGHT_WRIST.position].coordinate.x < keypoints[BodyPart.RIGHT_SHOULDER.position].coordinate.x) {
                //head down, arms left; then the primary side is the right side
                currentState.headPointingUp = false
                currentState.mainSide = RightParts
                currentState.subSide = LeftParts
            }
            else {
                //head down, arms right; primary side is the left side
                currentState.headPointingUp = false
                currentState.mainSide = LeftParts
                currentState.subSide = RightParts
            }

            //wait until the body is in the correct state
            if (listOf("LElbow", "LLTorso", "LKnee", "RElbow", "RLTorso", "RKnee").any {!angles[it]!!.valid}) {
                currentState.feedback =
                    listOf("${angles[Angles.LElbow.name]!!.valid} | ${angles[Angles.LLTorso.name]!!.valid} | ${angles[Angles.LKnee.name]!!.valid}")
                return currentState
            }
            else {
                currentState.sessionId = dbHandler.insertSessionData(Instant.now(), Instant.now(), progression)
                currentState.feedback = listOf("Good: ${goodReps} | Bad: ${badReps} | Total: ${totalReps}")
                currentState.state = ProgressionStates.START
                currentState.startingArmDist = computeDistOfTwoParts(keypoints, BodyPart.fromInt(mainSide.shoulder), BodyPart.fromInt(mainSide.wrist))
                return currentState
            }
        }
        ProgressionStates.START -> {
            //wait until valid again
            if (listOf("LElbow", "LLTorso", "LKnee", "RElbow", "RLTorso", "RKnee").all {angles[it]!!.valid}) {
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
            val currentArmDist = computeDistOfTwoParts(keypoints, BodyPart.fromInt(mainSide.shoulder), BodyPart.fromInt(mainSide.wrist))
            if (currentArmDist < currentState.lowestArmDist)
                currentState.lowestArmDist = currentArmDist

            if (!angles[mainSide.elbowAngle]!!.valid && !angles[subSide.elbowAngle]!!.valid && currentArmDist < 0.8 * currentState.startingArmDist) //assume push up has started once elbows bend
                currentState.down = true

            if (!angles[mainSide.lTorsoAngle]!!.valid || //!angleValidity["LLTorso"]!! || //check if the torso buckles
                !angles[mainSide.kneeAngle]!!.valid){// || !angleValidity["LKnee"]!!){ // or the knees buckle
                currentState.errorCounter.buckling++
            }
            else currentState.errorCounter.buckling--
            if (currentState.errorCounter.buckling >= 3) {
                currentState.goodForm = false
                errors.add("Body buckling")
            }

            //check if hands are under shoulders
            if ((currentState.headPointingUp && keypoints[mainSide.shoulder].coordinate.y > keypoints[mainSide.wrist].coordinate.y + 20) ||
                (!currentState.headPointingUp && keypoints[mainSide.shoulder].coordinate.y < keypoints[mainSide.wrist].coordinate.y - 20)) {// && abs(pixels[6].y - pixels[10].y) > 100){
                currentState.goodForm = false
                errors.add("head is up: ${currentState.headPointingUp}")
                errors.add("Hands are not under shoulders.")
            }

            //check if feet are level with hands
            //figure out where head is relative to hands
            if ((keypoints[mainSide.shoulder].coordinate.x > keypoints[mainSide.wrist].coordinate.x && keypoints[mainSide.wrist].coordinate.x > keypoints[mainSide.ankle].coordinate.x) || //facing left
                (keypoints[mainSide.shoulder].coordinate.x < keypoints[mainSide.wrist].coordinate.x && keypoints[mainSide.wrist].coordinate.x < keypoints[mainSide.ankle].coordinate.x) ) { //facing right
                currentState.goodForm = false
                errors.add("Hands not aligned with feet")
            }

            currentState.errors = errors

            if (currentState.down && angles[mainSide.elbowAngle]!!.valid && currentState.lowestArmDist < 0.8 * currentState.startingArmDist)
                currentState.state = ProgressionStates.GOINGUP
            return currentState
        }
        ProgressionStates.GOINGUP -> {
            //check range of motion here, make rep bad if not enough
            if (currentState.goodForm && currentState.startingArmDist - currentState.lowestArmDist < 0.4 * currentState.startingArmDist) {
                errors.add("Starting: ${round(currentState.startingArmDist)} | Lowest: ${round(currentState.lowestArmDist)}")
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