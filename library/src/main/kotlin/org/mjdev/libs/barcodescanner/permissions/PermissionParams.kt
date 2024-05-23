package org.mjdev.libs.barcodescanner.permissions

import java.io.Serializable

class PermissionParams(
    vararg val permissions: String,
    val onGranted: PermissionConsumer<String, Unit>,
    val onDenied: PermissionConsumer<String, Unit>
) : Serializable