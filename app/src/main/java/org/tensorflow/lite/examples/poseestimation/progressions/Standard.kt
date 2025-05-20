package org.tensorflow.lite.examples.poseestimation.progressions

import android.content.res.Resources
import android.media.MediaPlayer
import org.tensorflow.lite.examples.poseestimation.calculateAngle
import org.tensorflow.lite.examples.poseestimation.data.Angles
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.LeftParts
import org.tensorflow.lite.examples.poseestimation.data.RightParts
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.data.Sides
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
private val SubStandards = mapOf(
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
    val subAngles = person.subSide.getAngles()
    Angles.entries.forEach {
        if (it.name in subAngles)
            person.angles[it.name]!!.valid = it.check(person.angles[it.name]!!.value, SubStandards)
        else
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

    val mainSide = person.mainSide
    val subSide = person.subSide

    //figure out where head is relative to hands
    var facingLeft : Boolean = if (keypoints[mainSide.shoulder].coordinate.x > keypoints[mainSide.wrist].coordinate.x) true else false

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
                        person.mainSide = LeftParts
                        person.subSide = RightParts
                    }
                //otherwise the primary side is the right side
                else {
                    person.mainSide = RightParts
                    person.subSide = LeftParts
                }
            }
            else if (keypoints[BodyPart.LEFT_WRIST.position].coordinate.x < keypoints[BodyPart.LEFT_SHOULDER.position].coordinate.x ||
                keypoints[BodyPart.RIGHT_WRIST.position].coordinate.x < keypoints[BodyPart.RIGHT_SHOULDER.position].coordinate.x) {
                //head down, arms left; then the primary side is the right side
                currentState.headPointingUp = false
                person.mainSide = RightParts
                person.subSide = LeftParts
            }
            else {
                //head down, arms right; primary side is the left side
                currentState.headPointingUp = false
                person.mainSide = LeftParts
                person.subSide = RightParts
            }
            facingLeft = if (keypoints[mainSide.shoulder].coordinate.x > keypoints[mainSide.wrist].coordinate.x) true else false

            //wait until the body is in the correct state
            if (listOf("LElbow", "LLTorso", "LKnee", "RElbow", "RLTorso", "RKnee").any {!angles[it]!!.valid} ||
                //make sure feet aren't too high
                (facingLeft && keypoints[mainSide.ankle].coordinate.x > keypoints[mainSide.shoulder].coordinate.x) ||
                (!facingLeft && keypoints[mainSide.ankle].coordinate.x < keypoints[mainSide.shoulder].coordinate.x)) {
                currentState.feedback =
                    listOf("${angles[Angles.LElbow.name]!!.valid} | ${angles[Angles.LLTorso.name]!!.valid} | ${angles[Angles.LKnee.name]!!.valid}")
                return currentState
            }
            else {
                currentState.errorCounter.startPosition++ //using this as a counter for start position
                if (currentState.errorCounter.startPosition >= 3) {
                    currentState.errorCounter.startPosition = 0
                    currentState.sessionId =
                        dbHandler.insertSessionData(Instant.now(), Instant.now(), progression)
                    currentState.feedback =
                        listOf("Good: $goodReps | Bad: $badReps | Total: $totalReps")
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
                if (currentState.errorCounter.startPosition >= 2) {
                    currentState.errorCounter.startPosition = 0
                    feedback.add(" | Next rep")
                    currentState.feedback = feedback
                    currentState.state = ProgressionStates.GOINGDOWN
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

            //hands shouldn't go above shoulders (pike)
            if ((currentState.headPointingUp && keypoints[mainSide.shoulder].coordinate.y > keypoints[mainSide.wrist].coordinate.y + 20) ||
                (!currentState.headPointingUp && keypoints[mainSide.shoulder].coordinate.y < keypoints[mainSide.wrist].coordinate.y - 20) ||
                (currentState.headPointingUp && keypoints[subSide.shoulder].coordinate.y > keypoints[subSide.wrist].coordinate.y + 20) ||
                (!currentState.headPointingUp && keypoints[subSide.shoulder].coordinate.y < keypoints[subSide.wrist].coordinate.y - 20)) {
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

            //check if feet are level with hands
            if ((facingLeft && keypoints[mainSide.wrist].coordinate.x > keypoints[mainSide.ankle].coordinate.x) ||
                (!facingLeft && keypoints[mainSide.wrist].coordinate.x < keypoints[mainSide.ankle].coordinate.x) ) {
                currentState.goodForm = false
                errors.add(if (mainSide.side == Sides.LEFT) "Right hand not level with feet." else "Left hand not level with feet.")
            }
            if ((facingLeft && keypoints[subSide.wrist].coordinate.x > keypoints[subSide.ankle].coordinate.x) ||
                (!facingLeft && keypoints[subSide.wrist].coordinate.x < keypoints[subSide.ankle].coordinate.x) ) {
                currentState.goodForm = false
                errors.add(if (subSide.side == Sides.LEFT) "Right hand not level with feet." else "Left hand not level with feet.")
            }

            currentState.errors = errors

            if (currentState.down && angles[mainSide.elbowAngle]!!.valid && currentState.lowestArmDist < 0.8 * currentState.startingArmDist)
                currentState.state = ProgressionStates.GOINGUP
            return currentState
        }
        ProgressionStates.GOINGUP -> {
            //make sure feet aren't too high
            val pointB = Pair(keypoints[mainSide.shoulder].coordinate.x, keypoints[mainSide.shoulder].coordinate.y)
            val pointC = Pair(keypoints[mainSide.ankle].coordinate.x, keypoints[mainSide.ankle].coordinate.y)
            val pointA = Pair(keypoints[mainSide.shoulder].coordinate.x, keypoints[mainSide.ankle].coordinate.y)
            if ((calculateAngle(pointA, pointB, pointC) < 10) || //torso too close to parallel)
                (facingLeft && keypoints[mainSide.ankle].coordinate.x > keypoints[mainSide.shoulder].coordinate.x) || //decline
                (!facingLeft && keypoints[mainSide.ankle].coordinate.x < keypoints[mainSide.shoulder].coordinate.x)) {
                currentState.goodForm = false
                errors.add("Feet too high up.")
            }

            //check range of motion here, make rep bad if not enough
            if (currentState.goodForm && currentState.startingArmDist - currentState.lowestArmDist < 0.3 * currentState.startingArmDist) {
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
            currentState.errorCounter.reset()

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