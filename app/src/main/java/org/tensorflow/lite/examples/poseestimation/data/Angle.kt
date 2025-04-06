package org.tensorflow.lite.examples.poseestimation.data

data class Angle(val value: Double, val valid: Boolean, val indices: Triple<Int, Int, Int>)
