package com.luck.picture.library.customengine

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.luck.picture.library.engine.MediaConverterEngine
import com.luck.picture.library.entity.LocalMedia
import com.luck.picture.library.utils.FileUtils
import com.luck.picture.library.utils.MediaUtils
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author：luck
 * @date：2022-5-24 22:30
 * @describe：资源转换，可用于处理Android 10沙盒机制、图片水印、视频缩略图等操作
 */
class MediaConverter : MediaConverterEngine {

    override suspend fun converter(context: Context, media: LocalMedia): LocalMedia {
        withContext(Dispatchers.IO) {
            val path = media.getAvailablePath()
            val mimeType = media.mimeType
            if (path == null || TextUtils.isEmpty(path)) {
                return@withContext
            }
            if (mimeType == null || TextUtils.isEmpty(mimeType)) {
                return@withContext
            }
            when {
                MediaUtils.hasMimeTypeOfImage(mimeType) -> {
                    if (MediaUtils.isContent(path)) {
                        val realPath = copyToSandbox(
                            context,
                            path,
                            mimeType,
                            MediaUtils.getPostfix(context, path, "jpg")
                        )
                        media.sandboxPath = realPath
                        media.compressPath = realPath?.let { compress(context, realPath) }
                    } else {
                        media.compressPath = compress(context, path)
                    }
                }
                MediaUtils.hasMimeTypeOfVideo(mimeType) -> {
                    if (MediaUtils.isContent(path)) {
                        media.sandboxPath = copyToSandbox(
                            context,
                            path,
                            mimeType,
                            MediaUtils.getPostfix(context, path, "mp4")
                        )
                        media.videoThumbnailPath = generateVideoThumbnail(context, path, media.id)
                    }
                }
                MediaUtils.hasMimeTypeOfAudio(mimeType) -> {
                    if (MediaUtils.isContent(path)) {
                        media.sandboxPath = copyToSandbox(
                            context,
                            path,
                            mimeType,
                            MediaUtils.getPostfix(context, path, "amr")
                        )
                    }
                }
            }
        }
        return media
    }

    /**
     * Copy files into the application sandbox
     */
    private fun copyToSandbox(
        context: Context,
        path: String,
        mimeType: String,
        postfix: String
    ): String? {
        val target = "${getFileDir(context, mimeType)}/${System.currentTimeMillis()}.$postfix"
        return FileUtils.copyFile(context, path, target)
    }

    /**
     * Compress files
     */
    private suspend fun compress(context: Context, path: String): String? {
        if (MediaUtils.isUrlHasGif(path)) {
            return path
        }
        val compressFile = Compressor.compress(context, File(path)) {
            default(4096, 4096)
            destination(getCompressFileDir(context))
            size(1024 * 1024 * 50)
        }
        return compressFile.absolutePath
    }


    private fun getCompressFileDir(context: Context): File {
        return File(
            context.getExternalFilesDir(""), "/compressor/${System.currentTimeMillis()}.jpg"
        )
    }

    private fun generateVideoThumbnail(
        context: Context,
        videoPath: String,
        videoId: Long
    ): String? {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)

            val bitmap = retriever.frameAtTime

            if (bitmap != null) {
                val thumbnailFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "thumbnail_${videoId}_${System.currentTimeMillis()}.jpg"
                )
                val outputStream = thumbnailFile.outputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
                outputStream.close()
                return thumbnailFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("MediaConverter", "生成视频缩略图失败: ${e.message}", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.e("MediaConverter", "释放MediaMetadataRetriever失败: ${e.message}", e)
            }
        }
        return null
    }

    private fun getFileDir(context: Context, mimeType: String): File? {
        return when {
            MediaUtils.hasMimeTypeOfImage(mimeType) -> {
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            }
            MediaUtils.hasMimeTypeOfVideo(mimeType) -> {
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            }
            MediaUtils.hasMimeTypeOfAudio(mimeType) -> {
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            }
            else -> null
        }
    }

    companion object {
        fun create() = InstanceHelper.engine
    }

    object InstanceHelper {
        val engine = MediaConverter()
    }
}