package com.luck.picture.library.adapter

import android.view.View
import android.widget.TextView
import com.luck.picture.library.R
import com.luck.picture.library.adapter.base.BaseListViewHolder
import com.luck.picture.library.config.MediaType

/**
 * @author：luck
 * @date：2022/11/30 3:29 下午
 * @describe：CameraViewHolder
 */
open class CameraViewHolder(itemView: View) : BaseListViewHolder(itemView) {
    private var tvCamera: TextView = itemView.findViewById(R.id.tv_camera)
    fun bindData(position: Int) {
        itemView.setOnClickListener {
            if (mItemClickListener != null) {
                mItemClickListener?.openCamera()
            }
        }
//        if (config.mediaType == MediaType.AUDIO) {
//            tvCamera.text = itemView.context.getString(R.string.ps_tape)
//        } else {
//            tvCamera.text = itemView.context.getString(R.string.ps_take_picture)
//        }
    }
}