package org.tensorflow.lite.examples.poseestimation.data

data class Angle(val value: Double, var valid: Boolean, val indices: Triple<Int, Int, Int>)

/**
 * This defines checks for certain types of angles.
 */
private enum class AngleCheckFunction(val check: (Double, Map<String, Int>) -> Boolean) {
    //todo: redundancy removal
    Elbow(fun(angle, map): Boolean{
        if (angle > map["STANDARD_ELBOW_ANGLE"]!! + map["STANDARD_ELBOW_DOF"]!!)
            return false
        if (angle < map["STANDARD_ELBOW_ANGLE"]!! - map["STANDARD_ELBOW_DOF"]!!)
            return false
        return true
    }),
    Knee(fun(angle, map): Boolean{
        if (angle > map["STANDARD_KNEE_ANGLE"]!! + map["STANDARD_KNEE_DOF"]!!) return false
        if (angle < map["STANDARD_KNEE_ANGLE"]!! - map["STANDARD_KNEE_DOF"]!!) return false
        return true
    }),
    UpperTorso(fun(angle, map): Boolean {
        if (angle > map["STANDARD_UPPER_TORSO_ANGLE"]!! + map["STANDARD_UPPER_TORSO_DOF"]!!) return false
        if (angle < map["STANDARD_UPPER_TORSO_ANGLE"]!! - map["STANDARD_UPPER_TORSO_DOF"]!!) return false
        return true
    }),
    LowerTorso(fun(angle, map) : Boolean{
        if (angle > map["STANDARD_LOWER_TORSO_ANGLE"]!! + map["STANDARD_LOWER_TORSO_DOF"]!!) return false
        if (angle < map["STANDARD_LOWER_TORSO_ANGLE"]!! - map["STANDARD_LOWER_TORSO_DOF"]!!) return false
        return true
    })
}

/**
 * This defines all angles to be checked.
 */
enum class Angles(val indices: Triple<Int, Int, Int>, val check: (Double, Map<String, Int>) -> Boolean) {
    LElbow(
        Triple(
            BodyPart.LEFT_SHOULDER.position,
            BodyPart.LEFT_ELBOW.position,
            BodyPart.LEFT_WRIST.position
        ),
        AngleCheckFunction.Elbow.check
    ),
    RElbow(
        Triple(
            BodyPart.RIGHT_SHOULDER.position,
            BodyPart.RIGHT_ELBOW.position,
            BodyPart.RIGHT_WRIST.position
        ),
        AngleCheckFunction.Elbow.check
    ),
    LKnee(
        Triple(
            BodyPart.LEFT_HIP.position,
            BodyPart.LEFT_KNEE.position,
            BodyPart.LEFT_ANKLE.position
        ),
        AngleCheckFunction.Knee.check
    ),
    RKnee(
        Triple(
            BodyPart.RIGHT_HIP.position,
            BodyPart.RIGHT_KNEE.position,
            BodyPart.RIGHT_ANKLE.position
        ),
        AngleCheckFunction.Knee.check
    ),
    LLTorso(
        Triple(
            BodyPart.LEFT_SHOULDER.position,
            BodyPart.LEFT_HIP.position,
            BodyPart.LEFT_KNEE.position
        ),
        AngleCheckFunction.LowerTorso.check
    ),
    RLTorso(
        Triple(
            BodyPart.RIGHT_SHOULDER.position,
            BodyPart.RIGHT_HIP.position,
            BodyPart.RIGHT_KNEE.position
        ),
        AngleCheckFunction.LowerTorso.check
    ),
    LUTorso(
        Triple(
            BodyPart.LEFT_EAR.position,
            BodyPart.LEFT_SHOULDER.position,
            BodyPart.LEFT_HIP.position
        ),
        AngleCheckFunction.UpperTorso.check
    ),
    RUTorso(
        Triple(
            BodyPart.RIGHT_EAR.position,
            BodyPart.RIGHT_SHOULDER.position,
            BodyPart.RIGHT_HIP.position
        ),
        AngleCheckFunction.UpperTorso.check
    )
}
