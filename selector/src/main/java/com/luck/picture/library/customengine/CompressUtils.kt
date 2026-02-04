package com.luck.picture.library.customengine

import android.graphics.BitmapFactory
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.im.common.utils.IMFileUtils
import com.im.common.utils.RustImageUtil
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File


class CompressUtils private constructor() {

    companion object {
        @JvmStatic
        val INSTANCE: CompressUtils by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { CompressUtils() }
    }

    private val rustImageUtil = RustImageUtil()

    private val mutex = Mutex()

    suspend fun compress(
        filePath: String,
        targetFilePath: String,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int,
        maxSize: Long
    ): String {
        mutex.withLock {
            val sourceFile = File(filePath)
            if (!sourceFile.exists()) {
                return ""
            }
            if (IMFileUtils.isFileExists(targetFilePath)) {
                return targetFilePath
            }
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(filePath, options)
                val originalWidth = options.outWidth
                val originalHeight = options.outHeight
                if (originalWidth <= 0 || originalHeight <= 0) {
                    return ""
                }
                val (targetWidth, targetHeight) = calculateScaledSize(
                    originalWidth, originalHeight, maxWidth, maxHeight
                )
                val rotateDegree = ImageUtils.getRotateDegree(filePath)
                val isSuccess = rustImageUtil.cropAndCompressImage(
                    inputPath = filePath,
                    outputPath = targetFilePath,
                    width = targetWidth,
                    height = targetHeight,
                    rotate = rotateDegree,
                    quality = quality,
                    maxSize = maxSize
                )
                if (isSuccess) return targetFilePath
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                if (IMFileUtils.isFileExists(targetFilePath)) {
                    FileUtils.delete(targetFilePath)
                }
            } catch (e: Exception) {
//                IMLogUtils.e("Test") { "compressThumbTest: $e",  }
                if (IMFileUtils.isFileExists(targetFilePath)) {
                    FileUtils.delete(targetFilePath)
                }
            }

            return ""
        }
    }

    /**
     * Calculate scaled dimensions with proportional scaling
     * @param originalWidth Original width
     * @param originalHeight Original height
     * @param maxWidth Maximum width limit
     * @param maxHeight Maximum height limit
     * @return Pair<Int, Int> Scaled width and height
     */
    private fun calculateScaledSize(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        // Return original size if already within limits
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return Pair(originalWidth, originalHeight)
        }

        val maxTextureSize = 16383 // OpenGL ES maximum texture size limit
        // First scale by short edge limit
        val (widthAfterShortLimit, heightAfterShortLimit) = scaleByShortEdge(
            originalWidth, originalHeight, maxWidth, maxHeight
        )
        // Check if long edge exceeds 16383, scale further if needed
        val (finalWidth, finalHeight) = if (maxOf(
                widthAfterShortLimit,
                heightAfterShortLimit
            ) > maxTextureSize
        ) {
            scaleToMaxTextureSize(widthAfterShortLimit, heightAfterShortLimit, maxTextureSize)
        } else {
            Pair(widthAfterShortLimit, heightAfterShortLimit)
        }

        return Pair(finalWidth, finalHeight)
    }

    /**
     * Scale by short edge limitation
     */
    private fun scaleByShortEdge(
        width: Int,
        height: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        val originalShort = minOf(width, height)
        val originalLong = maxOf(width, height)
        val maxShort = minOf(maxWidth, maxHeight)
        val maxLong = maxOf(maxWidth, maxHeight)
        // Return original if short edge already meets requirements
        if (originalShort <= maxShort) {
            return Pair(width, height)
        }
        // Scale by short edge ratio
        val scaleRatio = maxShort.toDouble() / originalShort.toDouble()
        val newWidth = (width * scaleRatio).toInt()
        val newHeight = (height * scaleRatio).toInt()

        return Pair(newWidth, newHeight)
    }

    /**
     * Scale dimensions to not exceed maximum texture size
     */
    private fun scaleToMaxTextureSize(
        width: Int,
        height: Int,
        maxTextureSize: Int
    ): Pair<Int, Int> {
        val maxDimension = maxOf(width, height)
        // Return original if already within limit
        if (maxDimension <= maxTextureSize) {
            return Pair(width, height)
        }
        // Scale by maximum dimension limit
        val scaleRatio = maxTextureSize.toDouble() / maxDimension.toDouble()
        val newWidth = (width * scaleRatio).toInt()
        val newHeight = (height * scaleRatio).toInt()

        return Pair(newWidth, newHeight)
    }
}