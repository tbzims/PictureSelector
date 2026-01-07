package com.luck.pictureselector.custom

import android.view.View
import com.luck.picture.library.adapter.PreviewVideoHolder
import com.luck.picture.library.player.IMediaPlayer

/**
 * @author：luck
 * @date：2023/1/4 4:55 下午
 * @describe：CustomPreviewIjkVideoHolder
 */
class CustomPreviewIjkVideoHolder(itemView: View) : PreviewVideoHolder(itemView) {
    override fun onCreateVideoComponent(): IMediaPlayer {
        return IjkMediaPlayer(itemView.context)
    }
}