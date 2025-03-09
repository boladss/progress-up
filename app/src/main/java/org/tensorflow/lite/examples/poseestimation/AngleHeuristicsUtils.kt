package org.tensorflow.lite.examples.poseestimation

import android.graphics.PointF
import org.tensorflow.lite.examples.poseestimation.data.Person
import kotlin.math.sqrt
import kotlin.math.acos

object AngleHeuristicsUtils {

  //not good btw, should be setters/getters
  public var preferRightSide : Boolean = true
  public var angleValidity = mutableMapOf<String, Boolean>(
    "LElbow" to false,
    "RElbow" to false,
    "LKnee" to false,
    "RKnee" to false,
    "LLTorso" to false,
    "RLTorso" to false,
    "LUTorso" to false,
    "RUTorso" to false,
  ).withDefault { false }
  public var pixels = MutableList<PointF>(17) {
    PointF(0.0f,0.0f)
  }

  val rightSide = listOf("RElbow", "RKnee", "RLTorso", "RUTorso");
  val leftSide = listOf("LElbow", "LKnee", "LLTorso", "LUTorso");

  private const val STANDARD_UPPER_TORSO_ANGLE = 180;
  private const val STANDARD_UPPER_TORSO_DOF = 10;
  private const val STANDARD_LOWER_TORSO_ANGLE = 180;
  private const val STANDARD_LOWER_TORSO_DOF = 15;
  private const val STANDARD_ELBOW_ANGLE = 180;
  private const val STANDARD_ELBOW_DOF = 20; // degrees of freedom --- INCREASED TO 10
  private const val STANDARD_KNEE_ANGLE = 180;
  private const val STANDARD_KNEE_DOF = 25;
  private val indexToPartMapping = mapOf<String, Triple<Int, Int, Int>>(
    "LElbow" to Triple(5,7,9),
    "RElbow" to Triple(6, 8, 10),
    "LKnee" to Triple(11, 13, 15),
    "RKnee" to Triple(12, 14, 16),
    "LLTorso" to Triple(5, 11, 13),
    "RLTorso" to Triple(6, 12, 14),
    "LUTorso" to Triple(3, 5, 11),
    "RUTorso" to Triple(4, 6, 12)
  )

  // ARMS / ELBOWS
  // Checks if within valid range of motion
  fun isElbowValid(angle: Double): Boolean {
    if (angle > STANDARD_ELBOW_ANGLE + STANDARD_ELBOW_DOF) return false;
    if (angle < STANDARD_ELBOW_ANGLE - STANDARD_ELBOW_DOF) return false;
    return true;
  }
  // LEFT ELBOW
  fun checkLeftElbowAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, "LElbow", ::isElbowValid)   // indices for LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST
  }
  // RIGHT ELBOW
  fun checkRightElbowAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, "RElbow", ::isElbowValid)  // indices for RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST
  }

  // LEGS / KNEES
  fun isKneeValid(angle: Double): Boolean {
    if (angle > STANDARD_KNEE_ANGLE + STANDARD_KNEE_DOF) return false;
    if (angle < STANDARD_KNEE_ANGLE - STANDARD_KNEE_DOF) return false;
    return true;
  }
  // LEFT KNEE
  fun checkLeftKneeAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, "LKnee", ::isKneeValid)  // indices for LEFT_HIP, LEFT_KNEE, LEFT_ANKLE
  }
  // RIGHT KNEE
  fun checkRightKneeAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, "RKnee", ::isKneeValid)  // indices for RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE
  }

  // UPPER TORSO (head-shoulder-hip alignment)
  fun isUpperTorsoValid(angle: Double): Boolean {
    if (angle > STANDARD_UPPER_TORSO_ANGLE + STANDARD_UPPER_TORSO_DOF) return false;
    if (angle < STANDARD_UPPER_TORSO_ANGLE - STANDARD_UPPER_TORSO_DOF) return false;
    return true;
  }
  // LEFT UPPER TORSO
  fun checkLeftUpperTorsoAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, "LUTorso", ::isUpperTorsoValid)  // indices for LEFT_EAR, LEFT_SHOULDER, LEFT_HIP
  }
  // RIGHT UPPER TORSO
  fun checkRightUpperTorsoAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, "RUTorso", ::isUpperTorsoValid)  // indices for RIGHT_EAR, RIGHT_SHOULDER, RIGHT_HIP
  }

  // LOWER TORSO (shoulder-hip-knee alignment)
  fun isLowerTorsoValid(angle: Double): Boolean {
    if (angle > STANDARD_LOWER_TORSO_ANGLE + STANDARD_LOWER_TORSO_DOF) return false;
    if (angle < STANDARD_LOWER_TORSO_ANGLE - STANDARD_LOWER_TORSO_DOF) return false;
    return true;
  }
  // LEFT LOWER TORSO
  fun checkLeftLowerTorsoAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, "LLTorso", ::isLowerTorsoValid)  // indices for LEFT_SHOULDER, LEFT_HIP, LEFT_KNEE
  }
  // RIGHT LOWER TORSO
  fun checkRightLowerTorsoAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, "RLTorso", ::isLowerTorsoValid)  // indices for RIGHT_SHOULDER, RIGHT_HIP, RIGHT_KNEE
  }

  // Generalized function to handle fetching of keypoints
  fun checkJointAngle(person: Person, bodyAngle: String, checkFunction: (Double) -> Boolean): Pair<Double, Boolean> {
    val jointIndices = indexToPartMapping[bodyAngle]
    if (jointIndices === null)
      return Pair(0.0, false)

    val angle = calculateAngle(
        person.keyPoints[jointIndices.first].coordinate,
        person.keyPoints[jointIndices.second].coordinate,
        person.keyPoints[jointIndices.third].coordinate
    )
    
    pixels[jointIndices.first] = person.keyPoints[jointIndices.first].coordinate
    pixels[jointIndices.second] = person.keyPoints[jointIndices.second].coordinate
    pixels[jointIndices.third] = person.keyPoints[jointIndices.third].coordinate

    val isValid = checkFunction(angle)
    angleValidity[bodyAngle] = isValid

    return Pair(angle, isValid)
  }

  // Function to calculate angle between three points
  private fun calculateAngle(pointA: PointF, pointB: PointF, pointC: PointF): Double {
    // Formula using dot product (B as central point):
    // cos(theta) = (AB dot BC) / (||AB|| x ||BC||)

    // Lengths of each vector (AB, BC)
    val ABx = pointB.x - pointA.x
    val ABy = pointB.y - pointA.y

    val BCx = pointC.x - pointB.x
    val BCy = pointC.y - pointB.y

    // Dot product and vector magnitudes
    val dotProduct = ABx * BCx + ABy * BCy
    val magnitudeAB: Float = sqrt(ABx * ABx + ABy * ABy)
    val magnitudeBC: Float = sqrt(BCx * BCx + BCy * BCy)

    // Check for division by 0
    if (magnitudeAB == 0.0f || magnitudeBC == 0.0f) return 0.0

    /* Calculate for angle:
        - Obtain cos(theta) using dot product and magnitudes
        - Ensure cos(theta) is within bounds [-1.0, 1.0]
        - Compute for arccos(cos(theta))
    */
    var angle = acos((dotProduct / (magnitudeAB * magnitudeBC)).coerceIn(-1.0f, 1.0f))

    // Return value in degrees
    // Not finalized yet, (Math.PI - angle) is just to force straight lines to show 180 degrees---but possibly needs a bit more computation to distinguish direction of angle
    return (Math.PI - angle) * (180 / Math.PI)
    }

  fun checkSideDrawPriority(person: Person): Boolean {
    // Determine orientation of phone (and thus, which side to draw) based on head, arms, and legs
    val head = person.keyPoints[0].coordinate; // NOSE

    // Use average of wrists for better demonstration purposes
    val xLeftWrist = person.keyPoints[9].coordinate.x;
    val xRightWrist = person.keyPoints[10].coordinate.x;
    val xAvgWrists = (xLeftWrist + xRightWrist) / 2;

    // Average of hips
    val yLeftHip = person.keyPoints[11].coordinate.y;
    val yRightHip = person.keyPoints[12].coordinate.y;
    val yAvgHips = (yLeftHip + yRightHip) / 2;

    // Head towards top of phone
    //  LEFT:   head.y > leg.y & head.x > hand.x -> standard orientation
    // RIGHT:   head.y > leg.y & head.x < hand.x

    // Head towards bottom of phone --- WARNING: MODEL SEEMS LESS ACCURATE
    // RIGHT:   head.y < leg.y & head.x > hand.x
    //  LEFT:   head.y < leg.y & head.x < hand.x

    val isRight = !((head.y > yAvgHips) xor (head.x > xAvgWrists))
    preferRightSide = isRight

    // Keep in mind that upon selecting a preferred side, we are effectively IGNORING the opposite side.
    // So, we need a way to set these as "valid" (angleValidity) --- i.e., disregarding them for checking form.
    if (preferRightSide) {
      leftSide.forEach { joint ->
        angleValidity[joint] = true
      }
    } else {
      rightSide.forEach { joint -> 
        angleValidity[joint] = true
      }
    }

    return isRight;
  }
}