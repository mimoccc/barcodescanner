package org.mjdev.libs.barcodescanner.permissions

import java.io.Serializable

interface PermissionConsumer<T, U> : Serializable {
    fun consume(t: T): U
}