package org.tensorflow.lite.examples.poseestimation

import org.tensorflow.lite.examples.poseestimation.progressions.ProgressionTypes
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.progressions.getFeedbackStandard

class AngleHeuristicsUtils {
  //not good btw, should be setters/getters
  var preferRightSide : Boolean = true

  private var totalReps = 0
  private var badReps = 0
  private var goodReps = 0

  //one call of process heuristics is one repetition
  fun getReps() : Map<String, Int> {
    return mapOf(Pair("Total", totalReps), Pair("Bad", badReps), Pair("Good", goodReps))
  }

//  fun processHeuristics(person:Person, progression: ProgressionTypes, debug: (String) -> Unit) : List<String> {
//    //val (isGoodForm, feedback) = progression.getFeedback(person)
//
//    if (isGoodForm)
//      goodReps++
//    else
//      badReps++
//    totalReps++
//
//    return feedback
//  }
}