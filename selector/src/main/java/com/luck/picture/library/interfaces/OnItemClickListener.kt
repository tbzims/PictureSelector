package com.luck.picture.library.interfaces

/**
 * @author：luck
 * @date：2023/1/13 11:40 上午
 * @describe：OnItemClickListener
 */
interface OnItemClickListener<T> {
    fun onItemClick(position: Int, data: T)
}