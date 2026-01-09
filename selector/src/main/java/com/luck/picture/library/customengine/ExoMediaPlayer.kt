package com.luck.picture.library.customengine

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.luck.picture.library.player.IMediaPlayer
import com.luck.picture.library.utils.MediaUtils
import java.io.File

/**
 * @author：luck
 * @date：2023/1/4 4:55 下午
 * @describe：Exo Video MediaPlayer
 */
class ExoMediaPlayer(context: Context) : PlayerView(context), IMediaPlayer {
    private var mediaPlayer: ExoPlayer? = null
    private var mErrorListener: IMediaPlayer.OnErrorListener? = null
    private var mCompletionListener: IMediaPlayer.OnCompletionListener? = null
    private var mPreparedListener: IMediaPlayer.OnPreparedListener? = null
    private val exoPlayerListener: Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            mErrorListener?.onError(this@ExoMediaPlayer, error.errorCode, -1)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    mPreparedListener?.onPrepared(this@ExoMediaPlayer)
                }
                Player.STATE_ENDED -> {
                    mCompletionListener?.onCompletion(this@ExoMediaPlayer)
                }
                else -> {
                }
            }
        }
    }

    override fun initMediaPlayer() {
        useController = false
        mediaPlayer = ExoPlayer.Builder(context).build()
        mediaPlayer?.addListener(exoPlayerListener)
        player = mediaPlayer
    }

    override fun setDataSource(context: Context, path: String, isLoopAutoPlay: Boolean) {
        val mediaItem = when {
            MediaUtils.isContent(path) -> {
                MediaItem.fromUri(Uri.parse(path))
            }
            MediaUtils.isHasHttp(path) -> {
                MediaItem.fromUri(path)
            }
            else -> {
                MediaItem.fromUri(Uri.fromFile(File(path)))
            }
        }
        mediaPlayer?.repeatMode =
            if (isLoopAutoPlay) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        mediaPlayer?.setMediaItem(mediaItem)
        mediaPlayer?.prepare()
        mediaPlayer?.play()
    }

    override fun getCurrentPosition(): Long {
        return mediaPlayer?.currentPosition ?: 0L
    }

    override fun getDuration(): Long {
        return mediaPlayer?.duration ?: 0L
    }

    override fun seekTo(speed: Int) {
        mediaPlayer?.seekTo(speed.toLong())
    }

    override fun start() {
        mediaPlayer?.play()
    }

    override fun resume() {
        mediaPlayer?.play()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    override fun stop() {
        mediaPlayer?.stop()
    }

    override fun reset() {
        // ExoPlayer There is no such method to ignore
    }

    override fun release() {
        mediaPlayer?.removeListener(exoPlayerListener)
        mediaPlayer?.release()
        player = null
    }

    override fun setOnInfoListener(listener: IMediaPlayer.OnInfoListener?) {
        // ExoPlayer There is no such method to ignore
    }

    override fun setOnErrorListener(listener: IMediaPlayer.OnErrorListener?) {
        this.mErrorListener = listener
    }

    override fun setOnPreparedListener(listener: IMediaPlayer.OnPreparedListener?) {
        this.mPreparedListener = listener
    }

    override fun setOnCompletionListener(listener: IMediaPlayer.OnCompletionListener?) {
        this.mCompletionListener = listener
    }

    override fun setOnVideoSizeChangedListener(listener: IMediaPlayer.OnVideoSizeChangedListener?) {
        // ExoPlayer There is no such method to ignore
    }
}