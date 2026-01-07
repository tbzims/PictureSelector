package com.luck.picture.library.interfaces

/**
 * @author：luck
 * @date：2020/4/24 11:48 AM
 * @describe：OnCallbackListener
 */
interface OnCallbackListener<T> {
    /**
     * @param data
     */
    fun onCall(data: T)
}