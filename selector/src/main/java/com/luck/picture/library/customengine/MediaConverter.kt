package com.luck.picture.library.customengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.luck.picture.library.engine.MediaConverterEngine
import com.luck.picture.library.entity.LocalMedia
import com.luck.picture.library.utils.FileUtils
import com.luck.picture.library.utils.MediaUtils
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

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
                    Log.d("MediaConverter", "图片原路径: $path")
                    if (MediaUtils.isContent(path)) {
                        val realPath = copyToSandbox(
                            context,
                            path,
                            mimeType,
                            MediaUtils.getPostfix(context, path, "jpg")
                        )
                        Log.d("MediaConverter", "图片沙盒路径: $realPath")
                        media.sandboxPath = realPath
                        media.compressPath = realPath?.let { compress(context, realPath) }
                    } else {
                        media.compressPath = compress(context, path)
                    }
//                    media.compressPath =
//                        processImageWithSampling(context, media.sandboxPath ?: path, mimeType)
                    Log.d("MediaConverter", "图片压缩路径: ${media.compressPath}")
                    try {
                        val exif = ExifInterface(media.sandboxPath ?: path)
                        media.orientation = when (exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                        if (media.orientation == 90 || media.orientation == 270) {
                            val width = media.width
                            media.width = media.height
                            media.height = width
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
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
                    }
                    media.videoThumbnailPath =
                        generateVideoThumbnail(context, media)
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
        val pathHash = md5Hash(path)
//        val target = "${getFileDir(context, mimeType)}/${System.currentTimeMillis()}.$postfix"
        val target = "${getFileDir(context, mimeType)}/sandbox_${pathHash}.${postfix}"
        val targetFile = File(target)
        if (targetFile.exists()) {
            return targetFile.absolutePath
        }
        return FileUtils.copyFile(context, path, target)
    }

    private suspend fun processImageWithSampling(
        context: Context,
        path: String,
        mimeType: String
    ): String? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, 300, 300)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val scaledBitmap = BitmapFactory.decodeFile(path, options)

            if (scaledBitmap != null) {
                val outputFile = File(
                    context.filesDir,
                    "converted_${System.currentTimeMillis()}.jpg"
                )

                val outputStream = FileOutputStream(outputFile)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
                outputStream.close()

                if (!scaledBitmap.isRecycled) {
                    scaledBitmap.recycle()
                }

                outputFile.absolutePath
            } else {
                if (MediaUtils.isContent(path)) {
                    val realPath = copyToSandbox(
                        context,
                        path,
                        mimeType,
                        MediaUtils.getPostfix(context, path, "jpg")
                    )
                    realPath?.let { compress(context, realPath) }
                } else {
                    compress(context, path)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaConverter", "使用采样率处理图片时发生异常: ${e.message}", e)
            if (MediaUtils.isContent(path)) {
                val realPath = copyToSandbox(
                    context,
                    path,
                    mimeType,
                    MediaUtils.getPostfix(context, path, "jpg")
                )
                realPath?.let { compress(context, realPath) }
            } else {
                compress(context, path)
            }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Compress files
     */
    private suspend fun compress(context: Context, path: String): String? {
        if (MediaUtils.isUrlHasGif(path)) {
            return path
        }
        val targetPath = context.filesDir.resolve("compressor").resolve("${md5Hash(path)}.jpg")
        if (targetPath.exists()) {
            return targetPath.absolutePath
        }
        val compressFile = Compressor.compress(context, File(path)) {
            resolution(2000, 2000)
            quality(80)
            format(Bitmap.CompressFormat.JPEG)
            destination(targetPath)
            size(1024 * 1024 * 10)
        }
//        val results = luban(context) {
//            outputDir = targetPath
//            compress(File(path))
//        }
//        var compressFileUrl: String? = null
//        results.forEach { result ->
//            result.getOrNull()?.let { file ->
//                compressFileUrl = file.absolutePath
//                Log.d("Luban", "压缩成功: ${file.absolutePath}")
//            } ?: run {
//                val error = result.exceptionOrNull()
//                Log.e("Luban", "压缩失败: ${error?.message}")
//            }
//        }
        return compressFile.absolutePath
    }


    private fun getCompressFileDir(context: Context): File {
        return File(
            context.getExternalFilesDir(""), "/compressor/${System.currentTimeMillis()}.jpg"
        )
    }

    private fun generateVideoThumbnail(
        context: Context, media: LocalMedia
    ): String? {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(media.sandboxPath ?: media.path)

            val bitmap = retriever.frameAtTime

            if (bitmap != null) {
                val rotationStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                media.orientation = rotationStr?.toIntOrNull() ?: 0
                if (media.orientation == 90 || media.orientation == 270) {
                    val width = media.width
                    media.width = media.height
                    media.height = width
                }
                val processedBitmap = if (media.orientation != 0) {
                    val matrix = android.graphics.Matrix()
                    matrix.postRotate(media.orientation.toFloat())
                    val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )

                    if (bitmap != rotatedBitmap && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }

                    rotatedBitmap
                } else {
                    bitmap
                }
                val thumbnailFile = File(
                    context.filesDir,
                    "thumbnail_${media.id}_${System.currentTimeMillis()}.jpg"
                )
                val outputStream = thumbnailFile.outputStream()
                processedBitmap.compress(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    80,
                    outputStream
                )
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

    private fun md5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun create() = InstanceHelper.engine
    }

    object InstanceHelper {
        val engine = MediaConverter()
    }
}