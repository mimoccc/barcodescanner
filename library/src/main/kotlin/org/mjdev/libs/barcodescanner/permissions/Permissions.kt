@file:Suppress("unused")

package org.mjdev.libs.barcodescanner.permissions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.*

internal val onGrantedListeners: MutableMap<String, (String) -> Unit> = mutableMapOf()
internal val onDeniedListeners: MutableMap<String, (String) -> Unit> = mutableMapOf()

fun Context.iNeed(
    onGranted: (String) -> Unit = {},
    onDenied: (String) -> Unit = {},
    vararg permissions: String
) {
    val id = UUID.randomUUID().toString()
    onGrantedListeners[id] = onGranted
    onDeniedListeners[id] = onDenied
    getGranted(*permissions)?.forEach {
        onGranted(it)
    }
    val notGrantedPermissions = getNotGranted(*permissions)
    if (notGrantedPermissions != null) {
        val params = PermissionParams(
            *notGrantedPermissions,
            onGranted = object : PermissionConsumer<String, Unit> {
                override fun consume(t: String) {
                    granted(id, t)
                }
            },
            onDenied = object : PermissionConsumer<String, Unit> {
                override fun consume(t: String) {
                    denied(id, t)
                }
            }
        )
        val intent = Intent(this, PermissionActivity::class.java).apply {
            putExtra(PERMISSION_PARAMS_KEY, params)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}

fun Context.doIHave(
    vararg permissions: String,
    onGranted: (String) -> Unit,
    onDenied: (String) -> Unit
) {
    val id = UUID.randomUUID().toString()
    onGrantedListeners[id] = onGranted
    onDeniedListeners[id] = onDenied
    getGranted(*permissions)?.forEach {
        onGranted(it)
    }
    getNotGranted(*permissions)?.forEach {
        onDenied(it)
    }
}

fun granted(id: String, permission: String) = onGrantedListeners[id]?.invoke(permission)

fun denied(id: String, permission: String) = onDeniedListeners[id]?.invoke(permission)

fun Context.getGranted(vararg permissions: String): Array<String>? =
    permissions.mapNotNull { permission ->
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
            permission
        else
            null
    }.toTypedArray().nullIfEmpty()

fun Context.getNotGranted(vararg permissions: String): Array<String>? =
    permissions.mapNotNull { permission ->
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        )
            permission
        else
            null
    }.toTypedArray().nullIfEmpty()

fun Context.getDenied(vararg permissions: String): Array<String>? =
    permissions.mapNotNull { permission ->
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED)
            permission
        else
            null
    }.toTypedArray().nullIfEmpty()

fun <T> Array<T>.nullIfEmpty(): Array<T>? = if (isEmpty()) null else this