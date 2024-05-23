package org.mjdev.libs.barcodescanner.base

/**
 * Point
 * class represents one point of an element or picture
 */
class Point(
    var x: Int,
    var y: Int
) {
    override fun hashCode(): Int {
        return x xor (y shl Integer.SIZE / 2 or (y ushr Integer.SIZE / 2))
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            this -> true
            is Point -> ((x == other.x) && (y == other.y))
            else -> false
        }
    }

    override fun toString(): String {
        return "($x; $y)"
    }
}
