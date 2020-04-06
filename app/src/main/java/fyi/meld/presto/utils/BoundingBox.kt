package fyi.meld.presto.utils

import android.graphics.RectF

class BoundingBox (val classIndex: Int, val classIdentifier: String?, val confidence: Float?, private var location: RectF?) {

    fun getLocation(): RectF {
        return RectF(location)
    }

    fun setLocation(location: RectF?) {
        this.location = location
    }

    override fun toString(): String {
        var resultString = String.format("[%d] ", classIndex)
        if (classIdentifier != null) {
            resultString += "$classIdentifier "
        }
        if (confidence != null) {
            resultString += String.format("(%.1f%%) ", confidence * 100.0f)
        }
        if (location != null) {
            resultString += location.toString() + " "
        }
        return resultString.trim { it <= ' ' }
    }

}