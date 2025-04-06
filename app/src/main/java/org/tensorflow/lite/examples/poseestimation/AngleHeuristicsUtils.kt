package org.tensorflow.lite.examples.poseestimation

import android.graphics.PointF
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person
import kotlin.math.sqrt
import kotlin.math.acos

object AngleHeuristicsUtils {

  //not good btw, should be setters/getters
  var preferRightSide : Boolean = true

//  fun checkSideDrawPriority(person: Person): Boolean {
//    // Determine orientation of phone (and thus, which side to draw) based on head, arms, and legs
//    val head = person.keyPoints[0].coordinate; // NOSE
//
//    // Use average of wrists for better demonstration purposes
//    val xLeftWrist = person.keyPoints[9].coordinate.x;
//    val xRightWrist = person.keyPoints[10].coordinate.x;
//    val xAvgWrists = (xLeftWrist + xRightWrist) / 2;
//
//    // Average of hips
//    val yLeftHip = person.keyPoints[11].coordinate.y;
//    val yRightHip = person.keyPoints[12].coordinate.y;
//    val yAvgHips = (yLeftHip + yRightHip) / 2;
//
//    // Head towards top of phone
//    //  LEFT:   head.y > leg.y & head.x > hand.x -> standard orientation
//    // RIGHT:   head.y > leg.y & head.x < hand.x
//
//    // Head towards bottom of phone --- WARNING: MODEL SEEMS LESS ACCURATE
//    // RIGHT:   head.y < leg.y & head.x > hand.x
//    //  LEFT:   head.y < leg.y & head.x < hand.x
//
//    val isRight = !((head.y > yAvgHips) xor (head.x > xAvgWrists))
//    preferRightSide = isRight
//
//    // Keep in mind that upon selecting a preferred side, we are effectively IGNORING the opposite side.
//    // So, we need a way to set these as "valid" (angleValidity) --- i.e., disregarding them for checking form.
//    if (preferRightSide) {
//      leftSide.forEach { joint ->
//        angleValidity[joint] = true
//      }
//    } else {
//      rightSide.forEach { joint ->
//        angleValidity[joint] = true
//      }
//    }
//
//    return isRight;
//  }

  //fun
}