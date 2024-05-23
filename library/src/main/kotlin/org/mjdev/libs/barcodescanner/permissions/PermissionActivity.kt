package org.mjdev.libs.barcodescanner.permissions

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat

const val PERMISSION_REQUEST = 1
const val PERMISSION_PARAMS_KEY = "PERMISSION_PARAMS_KEY"

class PermissionActivity : Activity() {
    private lateinit var permissionParams: PermissionParams

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this).also {
            it.setBackgroundColor(Color.RED)
        })
        @Suppress("DEPRECATION")
        intent.getSerializableExtra(PERMISSION_PARAMS_KEY)?.let {
            permissionParams = it as PermissionParams
        } ?: finish()
        ActivityCompat.requestPermissions(
            this,
            permissionParams.permissions,
            PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    permissionParams.onGranted
                } else {
                    permissionParams.onDenied
                }.consume(permission)
            }
            finish()
        }
    }
}