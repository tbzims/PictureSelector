package com.luck.picture.library.permissions

/**
 * @author：luck
 * @date：2021/11/18 10:15 上午
 * @describe：OnPermissionResultListener
 */
interface OnPermissionResultListener {
    fun onGranted()
    fun onDenied()
}