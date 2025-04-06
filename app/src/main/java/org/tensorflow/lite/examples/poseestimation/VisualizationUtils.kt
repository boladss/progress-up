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
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person
import kotlin.math.max

object VisualizationUtils {
    /** Radius of circle used to draw keypoints.  */
    private const val CIRCLE_RADIUS = 6f

    /** Width of line used to connected two keypoints.  */
    private const val LINE_WIDTH = 4f

    /** The text size of the person id that will be displayed when the tracker is available.  */
    private const val PERSON_ID_TEXT_SIZE = 24f

    /** Distance from person id to the nose keypoint.  */
    private const val PERSON_ID_MARGIN = 6f

    private const val ANGLE_TEXT_MARGIN = 6f

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
            color = Color.CYAN
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

            //todo: create a replacement for processbodyangles

            person.angles.values.forEach{
                val (first, second, third) = it.indices
                //draw body joints
                drawBodyJoint(originalSizeCanvas, person, Pair(BodyPart.fromInt(first), BodyPart.fromInt(second)), it.valid)
                drawBodyJoint(originalSizeCanvas, person, Pair(BodyPart.fromInt(second), BodyPart.fromInt(third)), it.valid)
                //draw angle text
                drawAngleText(originalSizeCanvas, person, BodyPart.fromInt(first), it.value, it.valid)
            }

            //additional QoL lines
            drawBodyJoint(originalSizeCanvas, person, Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER), true)
            drawBodyJoint(originalSizeCanvas, person, Pair(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP), true)
        }
        return output
    }
/*
    // Handles going through all relevant keypoints and joints to draw
    fun processBodyAngles(canvas: Canvas, person: Person) {
        // Check which side to render first
        val drawRightSide = checkSideDrawPriority(person)
        
        val allJoints = listOf(
            // Left arm joints and corresponding check function
            Triple(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW, AngleHeuristicsUtils::checkLeftElbowAngle),
            Triple(BodyPart.LEFT_ELBOW, BodyPart.LEFT_WRIST, AngleHeuristicsUtils::checkLeftElbowAngle),
            
            // Right arm joints
            Triple(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW, AngleHeuristicsUtils::checkRightElbowAngle),
            Triple(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST, AngleHeuristicsUtils::checkRightElbowAngle),
            
            // Left knee joints
            Triple(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE, AngleHeuristicsUtils::checkLeftKneeAngle),
            Triple(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE, AngleHeuristicsUtils::checkLeftKneeAngle),
            
            // Right knee joints
            Triple(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE, AngleHeuristicsUtils::checkRightKneeAngle),
            Triple(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE, AngleHeuristicsUtils::checkRightKneeAngle),

            // DISABLED FOR NOW, NOT SURE IF CORRECT WAY OF TRACKING
            // // Left upper torso
            Triple(BodyPart.LEFT_EAR, BodyPart.LEFT_SHOULDER, AngleHeuristicsUtils::checkLeftUpperTorsoAngle),
            // Triple(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP, AngleHeuristicsUtils::checkLeftUpperTorsoAngle),
            
            // // Right upper torso
            Triple(BodyPart.RIGHT_EAR, BodyPart.RIGHT_SHOULDER, AngleHeuristicsUtils::checkRightUpperTorsoAngle),
            // Triple(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_HIP, AngleHeuristicsUtils::checkRightUpperTorsoAngle),

            // Left lower torso
            Triple(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP, AngleHeuristicsUtils::checkLeftLowerTorsoAngle),
            Triple(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE, AngleHeuristicsUtils::checkLeftLowerTorsoAngle),
            
            // Right lower torso
            Triple(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_HIP, AngleHeuristicsUtils::checkRightLowerTorsoAngle),
            Triple(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE, AngleHeuristicsUtils::checkRightLowerTorsoAngle),
        )

        // Filter which side to draw
        val jointsToCheck = allJoints.filter { (start, _, _) -> 
            if (!drawRightSide) {
                start in setOf(
                    BodyPart.LEFT_EYE, BodyPart.LEFT_EAR,
                    BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW, BodyPart.LEFT_WRIST,
                    BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE
                )
            } else {
                start in setOf(
                    BodyPart.RIGHT_EYE, BodyPart.RIGHT_EAR,
                    BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST,
                    BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE
                )
            }
        }

        // Process each joint and draw
        jointsToCheck.forEach { (start, end, checkAngleFunction) ->
            val (angle, isValid) = checkAngleFunction(person)
            
            drawBodyJoint(canvas, person, Pair(start, end), isValid)
            drawAngleText(canvas, person, start, angle, isValid)
        }
    }
*/
    fun drawBodyJoint(
        canvas: Canvas,
        person: Person,
        bodyJoint: Pair<BodyPart, BodyPart>,
        isValid: Boolean,
        ) {
        val pointA = person.keyPoints[bodyJoint.first.position].coordinate
        val pointB = person.keyPoints[bodyJoint.second.position].coordinate

        // Paint circles on keypoints
        val paintCircle = Paint().apply {
            strokeWidth = CIRCLE_RADIUS
            style = Paint.Style.FILL
            color = if (isValid) Color.CYAN else Color.RED
        }
        canvas.drawCircle(pointA.x, pointA.y, CIRCLE_RADIUS, paintCircle)
        canvas.drawCircle(pointB.x, pointB.y, CIRCLE_RADIUS, paintCircle)

        // Paint lines for joints
        val paintLine = Paint().apply {
            strokeWidth = LINE_WIDTH
            style = Paint.Style.STROKE 
            color = if (isValid) Color.CYAN else Color.RED
        }
        canvas.drawLine(pointA.x, pointA.y, pointB.x, pointB.y, paintLine)
    }

    fun drawAngleText(
        canvas: Canvas,
        person: Person,
        bodyPart: BodyPart,
        angle: Double,
        isValid: Boolean,
        ) {

        val paintText = Paint().apply {
            textSize = PERSON_ID_TEXT_SIZE
            color = if (isValid) Color.CYAN else Color.RED
            textAlign = Paint.Align.LEFT
        }

        val keypoint = person.keyPoints[bodyPart.position].coordinate

        canvas.save()
        canvas.rotate(90f, keypoint.x + ANGLE_TEXT_MARGIN, keypoint.y - ANGLE_TEXT_MARGIN)
        canvas.drawText(
            "${angle.toInt()}°",
            keypoint.x + ANGLE_TEXT_MARGIN,
            keypoint.y - ANGLE_TEXT_MARGIN,
            paintText
        )
        canvas.restore()

        return
    }
}
