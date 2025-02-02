package org.tensorflow.lite.examples.poseestimation

import android.graphics.PointF
import org.tensorflow.lite.examples.poseestimation.data.Person
import kotlin.math.sqrt
import kotlin.math.acos

object AngleHeuristicsUtils {

  private const val STANDARD_ELBOW_ANGLE = 180;
  private const val STANDARD_ELBOW_DOF = 10; // degrees of freedom


  fun checkLeftElbowAngle(person: Person): Pair<Double, Boolean> {
    // Calculate for angle given keypoints
    val angle = calculateAngle(
      person.keyPoints.get(5).coordinate,     // LEFT_SHOULDER
      person.keyPoints.get(7).coordinate,     // LEFT_ELBOW
      person.keyPoints.get(9).coordinate      // LEFT_WRIST
    )

    // Check if angle is valid
    var isValid = true;
    if (angle > STANDARD_ELBOW_ANGLE + STANDARD_ELBOW_DOF) isValid = false;
    if (angle < STANDARD_ELBOW_ANGLE - STANDARD_ELBOW_DOF) isValid = false;
    
    return Pair(angle, isValid);
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