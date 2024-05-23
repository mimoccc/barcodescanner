@file:Suppress("unused")

package org.mjdev.libs.barcodescanner.base

import android.graphics.Matrix

import java.lang.Integer.max
import kotlin.math.min

/**
 * Custom rect implementation with helper functions
 * class hold information about 2 dimensional objects
 */
class Rect(
    var left: Int,
    var top: Int,
    var right: Int,
    var bottom: Int
) {
    val width: Int get() = right - left
    val height: Int = bottom - top

    /**
     * Check if point is inside this object
     * @param x x of point
     * @param y y of point
     * @return true if point is from this 2 dimensional object
     */
    fun isPointInside(x: Int, y: Int): Boolean {
        return (left < x) && (top < y) && (right > x) && (bottom > y)
    }

    /**
     * Used to manage object size
     */
    fun sort(): Rect {
        if (left <= right && top <= bottom) {
            return this
        }
        if (left > right) {
            val temp = left
            left = right
            right = temp
        }
        if (top > bottom) {
            val temp = top
            top = bottom
            bottom = temp
        }
        return Rect(left, top, right, bottom)
    }

    /**
     * Bounding to another dimension
     */
    fun bound(bLeft: Int, bTop: Int, bRight: Int, bBottom: Int): Rect {
        if ((left >= bLeft) && (top >= bTop) && (right <= bRight) && (bottom <= bBottom)) {
            return this
        }
        return Rect(
            max(left, bLeft),
            max(top, bTop),
            min(right, bRight),
            min(bottom, bBottom)
        )
    }

    /**
     * Rotate
     */
    fun rotate(angle: Float, x: Float, y: Float): Rect {
        val matrix = Matrix()
        val rect: FloatArray = arrayOf(
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat()
        ).toFloatArray()
        matrix.postRotate(angle, x, y)
        matrix.mapPoints(rect)
        var left = rect[0]
        var top = rect[1]
        var right = rect[2]
        var bottom = rect[3]
        if (left > right) {
            val temp = left
            left = right
            right = temp
        }
        if (top > bottom) {
            val temp = top
            top = bottom
            bottom = temp
        }
        return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    /**
     * Fit rect in rect
     * Manages fitting one area to another
     * @param area rect of an area to fit
     * @return rect that needs to be used
     */
    fun fitIn(area: Rect): Rect {
        var rLeft = left
        var rTop = top
        var rRight = right
        var rBottom = bottom
        val rWidth = width
        val rHeight = height
        val areaLeft = area.left
        val areaTop = area.top
        val areaRight = area.right
        val areaBottom = area.bottom
        val areaWidth = area.width
        val areaHeight = area.height
        if (rLeft >= areaLeft && rTop >= areaTop && rRight <= areaRight && rBottom <= areaBottom) {
            return this
        }
        val fitWidth = min(rWidth, areaWidth)
        val fitHeight = min(rHeight, areaHeight)
        if (rLeft < areaLeft) {
            rLeft = areaLeft
            rRight = rLeft + fitWidth
        } else if (rRight > areaRight) {
            rRight = areaRight
            rLeft = rRight - fitWidth
        }
        if (rTop < areaTop) {
            rTop = areaTop
            rBottom = rTop + fitHeight
        } else if (rBottom > areaBottom) {
            rBottom = areaBottom
            rTop = rBottom - fitHeight
        }
        return Rect(rLeft, rTop, rRight, rBottom)
    }

    override fun hashCode(): Int {
        return 31 * (31 * (31 * left + top) + right) + bottom
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            this -> true
            is Rect -> left == other.left && top == other.top && right == other.right && bottom == other.bottom
            else -> false
        }
    }

    override fun toString(): String {
        return "[($left; $top) - ($right; $bottom)]"
    }
}
