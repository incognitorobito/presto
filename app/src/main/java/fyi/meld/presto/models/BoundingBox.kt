package fyi.meld.presto.models

import android.graphics.RectF

data class BoundingBox (val classIndex: Int, val classIdentifier: String?, private val confidence: Float?, var location: RectF?) {

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

    fun isWithin(otherBox: BoundingBox): Boolean {

        return( otherBox.location?.left!! <= this.location?.left!! &&
                otherBox.location?.right!! >= this.location?.right!! &&
                otherBox.location?.bottom!! >= this.location?.bottom!! &&
                otherBox.location?.top!! <= this.location?.top!! )
    }

}