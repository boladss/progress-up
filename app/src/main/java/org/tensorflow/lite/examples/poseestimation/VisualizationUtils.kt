/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package org.tensorflow.lite.examples.poseestimation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.acos

object VisualizationUtils {
    /** Radius of circle used to draw keypoints.  */
    private const val CIRCLE_RADIUS = 6f

    /** Width of line used to connected two keypoints.  */
    private const val LINE_WIDTH = 4f

    /** The text size of the person id that will be displayed when the tracker is available.  */
    private const val PERSON_ID_TEXT_SIZE = 30f

    /** Distance from person id to the nose keypoint.  */
    private const val PERSON_ID_MARGIN = 6f

    /** Keypoints to draw circles on (for neatness in testing) */
    val keypointsToDraw = setOf(
        BodyPart.LEFT_SHOULDER,
        BodyPart.LEFT_ELBOW,
        BodyPart.LEFT_WRIST,
        BodyPart.RIGHT_SHOULDER,
        BodyPart.RIGHT_ELBOW,
        BodyPart.RIGHT_WRIST
    )

    /** Pair of keypoints to draw lines between.  */
    private val bodyJoints = listOf(
//        Pair(BodyPart.NOSE, BodyPart.LEFT_EYE),
//        Pair(BodyPart.NOSE, BodyPart.RIGHT_EYE),
//        Pair(BodyPart.LEFT_EYE, BodyPart.LEFT_EAR),
//        Pair(BodyPart.RIGHT_EYE, BodyPart.RIGHT_EAR),
//        Pair(BodyPart.NOSE, BodyPart.LEFT_SHOULDER),
//        Pair(BodyPart.NOSE, BodyPart.RIGHT_SHOULDER),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW),
        Pair(BodyPart.LEFT_ELBOW, BodyPart.LEFT_WRIST),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
        Pair(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST),
//        Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
//        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP),
//        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_HIP),
//        Pair(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP),
//        Pair(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE),
//        Pair(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE),
//        Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE),
//        Pair(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE)
    )

    // Draw line and point indicate body pose
    fun drawBodyKeypoints(
        input: Bitmap,
        persons: List<Person>,
        isTrackerEnabled: Boolean = false
    ): Bitmap {
        val paintCircle = Paint().apply {
            strokeWidth = CIRCLE_RADIUS
            color = Color.CYAN
            style = Paint.Style.FILL
        }
        val paintLine = Paint().apply {
            strokeWidth = LINE_WIDTH
            color = Color.CYAN
            style = Paint.Style.STROKE
        }

        val paintText = Paint().apply {
            textSize = PERSON_ID_TEXT_SIZE
            color = Color.BLUE
            textAlign = Paint.Align.LEFT
        }

        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val originalSizeCanvas = Canvas(output)
        persons.forEach { person ->
            // draw person id if tracker is enable
            if (isTrackerEnabled) {
                person.boundingBox?.let {
                    val personIdX = max(0f, it.left)
                    val personIdY = max(0f, it.top)

                    originalSizeCanvas.drawText(
                        person.id.toString(),
                        personIdX,
                        personIdY - PERSON_ID_MARGIN,
                        paintText
                    )
                    originalSizeCanvas.drawRect(it, paintLine)
                }
            }
            bodyJoints.forEach {
                val pointA = person.keyPoints[it.first.position].coordinate
                val pointB = person.keyPoints[it.second.position].coordinate
                originalSizeCanvas.drawLine(pointA.x, pointA.y, pointB.x, pointB.y, paintLine)
            }

            person.keyPoints.forEach { point ->
                // Only draw relevant keypoints for neatness
                if (point.bodyPart in keypointsToDraw) {
                    originalSizeCanvas.drawCircle(
                        point.coordinate.x,
                        point.coordinate.y,
                        CIRCLE_RADIUS,
                        paintCircle
                    )
                }
            }

            // Calculate angle of both elbows and print to logs
            // Check BodyPart.kt for indices of keypoints
            val left_elbow_angle = calculateAngle(
                person.keyPoints.get(5).coordinate,     // LEFT_SHOULDER
                person.keyPoints.get(7).coordinate,     // LEFT_ELBOW
                person.keyPoints.get(9).coordinate      // LEFT_WRIST
            )

            val right_elbow_angle = calculateAngle(
                person.keyPoints.get(6).coordinate,     // RIGHT_SHOULDER
                person.keyPoints.get(8).coordinate,     // RIGHT_ELBOW
                person.keyPoints.get(10).coordinate     // RIGHT_WRIST
            )
            println("LEFT ELBOW: ${left_elbow_angle}°, RIGHT ELBOW: ${right_elbow_angle}°")

        }
        return output
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

        // Calculate for angle:
        // - Obtain cos(theta) using dot product and magnitudes
        // - Ensure cos(theta) is within bounds [-1.0, 1.0]
        // - Compute for arccos(cos(theta))
        var angle = acos((dotProduct / (magnitudeAB * magnitudeBC)).coerceIn(-1.0f, 1.0f))

        // Return value in degrees
        // Not finalized yet, (Math.PI - angle) is just to force straight lines to show 180 degrees---but possibly needs a bit more computation to distinguish direction of angle
        return (Math.PI - angle) * (180 / Math.PI)
    }
}
