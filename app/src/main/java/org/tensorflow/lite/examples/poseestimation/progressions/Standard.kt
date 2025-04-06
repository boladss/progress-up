package org.tensorflow.lite.examples.poseestimation.progressions

import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.ml.Angles

fun getFeedbackStandard(person:Person) : Pair<Boolean, List<String>> {
    val angles = person.angles

    while(!angles[Angles.LElbow.name]!!.valid || //|| angleValidity["LElbow"]!!) &&
            !angles[Angles.LLTorso.name]!!.valid || //|| angleValidity["LLTorso"]!!) &&
            !angles[Angles.LKnee.name]!!.valid) { //angleValidity.containsValue(false)) {
        //wait until the body is in a correct state
    }

//    repCount.text = "Good: ${goodReps} | Bad: ${badReps} | Total: ${currReps} | Reading start"
//    var startingArmDist = sqrt(abs(pixels[5].x - pixels[9].x).pow(2)+abs(pixels[5].y - pixels[9].y).pow(2)) //distance between shoulder and hand
//
//    while(true) {
//       // rep start
//        while (true) {
//            //do nothing
//            if ((angleValidity["LElbow"]!!) &&//|| angleValidity["LElbow"]!!) &&
//                (angleValidity["LLTorso"]!!) &&//|| angleValidity["LLTorso"]!!) &&
//                (angleValidity["LKnee"]!!)) //|| angleValidity["LKnee"]!!))
//                break
//
//        }
//        runOnUiThread {
//            repCount.append(" | Next rep")
//        }
//
//        var goodForm = true
//        var lowestArmDist = 9999999f
//        var down = false
//        val errors = mutableSetOf<String>()
//
//        //track rep
//        while (true) {
//            //track hand and shoulder distance
//            val currArmDist = sqrt(abs(pixels[5].x - pixels[9].x).pow(2)+abs(pixels[5].y - pixels[9].y).pow(2))
//            if (currArmDist < lowestArmDist)
//                lowestArmDist = currArmDist
//
//            if (!angleValidity["LElbow"]!!) //assume push up has started once elbow bends
//                down = true
//
//            if (!angleValidity["LLTorso"]!! || //!angleValidity["LLTorso"]!! || //check if the torso buckles
//                !angleValidity["LKnee"]!!){// || !angleValidity["LKnee"]!!){ // or the knees buckle
//                goodForm = false
//                errors.add("Body buckling")
//            }
//
//            //check if hands are under shoulders
//            if (abs(pixels[5].y - pixels[9].y) > 60){// && abs(pixels[6].y - pixels[10].y) > 100){
//                goodForm = false
//                errors.add("Hands not under shoulders")
//            }
//
//            //check if feet are level with hands
//            //figure out where head is relative to hands
//            //todo: change to nose
//            val isLowYDown : Boolean = if (pixels[5].x > pixels[9].x) true else false
//
//            if ((isLowYDown && pixels[15].x < pixels[9].x) ||
//                (!isLowYDown && pixels[15].x > pixels[9].x)){
//                goodForm = false
//                errors.add("Hands not aligned with feet")
//            }
//
//            //if has gone down, and arm straightens again, assume that they're done
//            if (down && angleValidity["LElbow"]!! && lowestArmDist < 0.8 * startingArmDist)
//                break
//        }
//
//        //check range of motion here, make rep bad if not enough
//        if (goodForm && startingArmDist - lowestArmDist < 0.5 * startingArmDist) {
//            errors.add("ROM not enough")
//            goodForm = false
//        }
//
//        if (goodForm) runOnUiThread {
//            repCount.text =
//                "Good: ${goodReps} | Bad: ${badReps} | Total: ${currReps} | Rep good"
//        }
//        else {
//            runOnUiThread {
//                repCount.text = "Good: ${goodReps} | Bad: ${badReps} | Total: ${currReps} | Errors:\n"
//                errors.forEach{
//                    repCount.append(it)
//                    repCount.append("\n")
//                }
//            }
//        }
//
//        while (sqrt(abs(pixels[5].x - pixels[9].x).pow(2)+abs(pixels[5].y - pixels[9].y).pow(2)) < startingArmDist - 10) {
//            //wait until
//        }
//        startingArmDist = sqrt(abs(pixels[5].x - pixels[9].x).pow(2)+abs(pixels[5].y - pixels[9].y).pow(2))
//        //increment rep when back to start
//        currReps++
//        if (goodForm) goodReps++ else badReps++
//    }
    return Pair(true, listOf("null"))
}