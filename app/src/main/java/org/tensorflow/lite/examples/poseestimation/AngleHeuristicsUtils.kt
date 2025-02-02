package org.tensorflow.lite.examples.poseestimation

import android.graphics.PointF
import org.tensorflow.lite.examples.poseestimation.data.Person
import kotlin.math.sqrt
import kotlin.math.acos

object AngleHeuristicsUtils {

  private const val STANDARD_ELBOW_ANGLE = 180;
  private const val STANDARD_ELBOW_DOF = 10; // degrees of freedom --- INCREASED TO 10
  private const val STANDARD_KNEE_ANGLE = 180;
  private const val STANDARD_KNEE_DOF = 10;

  // ARMS / ELBOWS
  // Checks if within valid range of motion
  fun isElbowValid(angle: Double): Boolean {
    if (angle > STANDARD_ELBOW_ANGLE + STANDARD_ELBOW_DOF) return false;
    if (angle < STANDARD_ELBOW_ANGLE - STANDARD_ELBOW_DOF) return false;
    return true;
  }

  // LEFT ELBOW
  fun checkLeftElbowAngle(person: Person): Pair<Double, Boolean> {
    val angle = calculateAngle(
      person.keyPoints.get(5).coordinate,     // LEFT_SHOULDER
      person.keyPoints.get(7).coordinate,     // LEFT_ELBOW
      person.keyPoints.get(9).coordinate      // LEFT_WRIST
    )
    return Pair(angle, isElbowValid(angle));
  }

  // RIGHT ELBOW
  fun checkRightElbowAngle(person: Person): Pair<Double, Boolean> {
    val angle = calculateAngle(
      person.keyPoints.get(6).coordinate,     // RIGHT_SHOULDER
      person.keyPoints.get(8).coordinate,     // RIGHT_ELBOW
      person.keyPoints.get(10).coordinate     // RIGHT_WRIST
    )
    return Pair(angle, isElbowValid(angle));
  }

  // LEGS / KNEES
  fun isKneeValid(angle: Double): Boolean {
    if (angle > STANDARD_KNEE_ANGLE + STANDARD_KNEE_DOF) return false;
    if (angle < STANDARD_KNEE_ANGLE - STANDARD_KNEE_DOF) return false;
    return true;
  }

  // LEFT KNEE
  fun checkLeftKneeAngle(person: Person): Pair<Double, Boolean> {
    val angle = calculateAngle(
      person.keyPoints.get(11).coordinate,
      person.keyPoints.get(13).coordinate,
      person.keyPoints.get(15).coordinate,
    )
    return Pair(angle, isKneeValid(angle));
  }

  // RIGHT KNEE
  fun checkRightKneeAngle(person: Person): Pair<Double, Boolean> {
    val angle = calculateAngle(
      person.keyPoints.get(12).coordinate,
      person.keyPoints.get(14).coordinate,
      person.keyPoints.get(16).coordinate,
    )
    return Pair(angle, isKneeValid(angle));
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