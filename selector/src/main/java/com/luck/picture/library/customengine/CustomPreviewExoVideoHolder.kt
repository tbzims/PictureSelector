package com.luck.picture.library.customengine

import android.view.View
import com.luck.picture.library.adapter.PreviewVideoHolder
import com.luck.picture.library.player.IMediaPlayer

/**
 * @author：luck
 * @date：2023/1/4 4:55 下午
 * @describe：CustomPreviewExoVideoHolder
 */
class CustomPreviewExoVideoHolder(itemView: View) : PreviewVideoHolder(itemView) {
    override fun onCreateVideoComponent(): IMediaPlayer {
        return ExoMediaPlayer(itemView.context)
    }
}