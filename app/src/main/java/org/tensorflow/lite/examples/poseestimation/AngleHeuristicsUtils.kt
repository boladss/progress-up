package org.tensorflow.lite.examples.poseestimation

import android.graphics.PointF
import org.tensorflow.lite.examples.poseestimation.data.Person
import kotlin.math.sqrt
import kotlin.math.acos

object AngleHeuristicsUtils {

  private const val STANDARD_UPPER_TORSO_ANGLE = 180;
  private const val STANDARD_UPPER_TORSO_DOF = 10;
  private const val STANDARD_LOWER_TORSO_ANGLE = 180;
  private const val STANDARD_LOWER_TORSO_DOF = 15;
  private const val STANDARD_ELBOW_ANGLE = 180;
  private const val STANDARD_ELBOW_DOF = 20; // degrees of freedom --- INCREASED TO 15
  private const val STANDARD_KNEE_ANGLE = 180;
  private const val STANDARD_KNEE_DOF = 25;

  // ARMS / ELBOWS
  // Checks if within valid range of motion
  fun isElbowValid(angle: Double): Boolean {
    if (angle > STANDARD_ELBOW_ANGLE + STANDARD_ELBOW_DOF) return false;
    if (angle < STANDARD_ELBOW_ANGLE - STANDARD_ELBOW_DOF) return false;
    return true;
  }
  // LEFT ELBOW
  fun checkLeftElbowAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, listOf(5, 7, 9), ::isElbowValid)   // indices for LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST
  }
  // RIGHT ELBOW
  fun checkRightElbowAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, listOf(6, 8, 10), ::isElbowValid)  // indices for RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST
  }

  // LEGS / KNEES
  fun isKneeValid(angle: Double): Boolean {
    if (angle > STANDARD_KNEE_ANGLE + STANDARD_KNEE_DOF) return false;
    if (angle < STANDARD_KNEE_ANGLE - STANDARD_KNEE_DOF) return false;
    return true;
  }
  // LEFT KNEE
  fun checkLeftKneeAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, listOf(11, 13, 15), ::isKneeValid)  // indices for LEFT_HIP, LEFT_KNEE, LEFT_ANKLE
  }
  // RIGHT KNEE
  fun checkRightKneeAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, listOf(12, 14, 16), ::isKneeValid)  // indices for RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE
  }

  // UPPER TORSO (head-shoulder-hip alignment)
  fun isUpperTorsoValid(angle: Double): Boolean {
    if (angle > STANDARD_UPPER_TORSO_ANGLE + STANDARD_UPPER_TORSO_DOF) return false;
    if (angle < STANDARD_UPPER_TORSO_ANGLE - STANDARD_UPPER_TORSO_DOF) return false;
    return true;
  }
  // LEFT UPPER TORSO
  fun checkLeftUpperTorsoAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, listOf(3, 5, 11), ::isUpperTorsoValid)  // indices for LEFT_EAR, LEFT_SHOULDER, LEFT_HIP
  }
  // RIGHT UPPER TORSO
  fun checkRightUpperTorsoAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, listOf(4, 6, 12), ::isUpperTorsoValid)  // indices for RIGHT_EAR, RIGHT_SHOULDER, RIGHT_HIP
  }

  // LOWER TORSO (shoulder-hip-knee alignment)
  fun isLowerTorsoValid(angle: Double): Boolean {
    if (angle > STANDARD_LOWER_TORSO_ANGLE + STANDARD_LOWER_TORSO_DOF) return false;
    if (angle < STANDARD_LOWER_TORSO_ANGLE - STANDARD_LOWER_TORSO_DOF) return false;
    return true;
  }
  // LEFT LOWER TORSO
  fun checkLeftLowerTorsoAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, listOf(5, 11, 13), ::isLowerTorsoValid)  // indices for LEFT_SHOULDER, LEFT_HIP, LEFT_KNEE
  }
  // RIGHT LOWER TORSO
  fun checkRightLowerTorsoAngle(person: Person): Pair<Double, Boolean> {
    return checkJointAngle(person, listOf(6, 12, 14), ::isLowerTorsoValid)  // indices for RIGHT_SHOULDER, RIGHT_HIP, RIGHT_KNEE
  }

  // Generalized function to handle fetching of keypoints
  fun checkJointAngle(person: Person, jointIndices: List<Int>, checkFunction: (Double) -> Boolean): Pair<Double, Boolean> {
    val angle = calculateAngle(
        person.keyPoints[jointIndices[0]].coordinate,
        person.keyPoints[jointIndices[1]].coordinate,
        person.keyPoints[jointIndices[2]].coordinate
    )
    return Pair(angle, checkFunction(angle))
  }

  // Function to calculate angle between three points
    fun calculateAngle(pointA: PointF, pointB: PointF, pointC: PointF): Double {
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
}