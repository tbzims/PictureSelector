package com.luck.picture.library.interfaces

import androidx.recyclerview.widget.RecyclerView

/**
 * @author：luck
 * @date：2023/1/13 11:40 上午
 * @describe：OnLongClickListener
 */
interface OnLongClickListener<T> {
    fun onLongClick(holder: RecyclerView.ViewHolder, position: Int, data: T)
}