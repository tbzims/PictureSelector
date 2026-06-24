package com.luck.picture.library.customengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.luck.picture.library.engine.MediaConverterEngine
import com.luck.picture.library.engine.MediaConverterEngine.ConvertOptions
import com.luck.picture.library.entity.LocalMedia
import com.luck.picture.library.utils.FileUtils
import com.luck.picture.library.utils.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * @author：luck
 * @date：2022-5-24 22:30
 * @describe：资源转换，可用于处理Android 10沙盒机制、图片水印、视频缩略图等操作
 */
class MediaConverter : MediaConverterEngine {

    override suspend fun converter(
        context: Context,
        media: LocalMedia,
        isOriginalPath: Boolean,
        isCompress: Boolean
    ): LocalMedia {
        /**
         * 旧转换接口对应的兼容配置。
         *
         * 旧调用默认仍然准备沙盒路径，避免升级后改变自定义调用方原有行为。
         */
        val legacyOptions = ConvertOptions(
            isOriginalSelected = isOriginalPath,
            needCompressPath = isCompress,
            needSandboxPath = true,
            needOriginalAbsolutePath = false,
        )
        return converter(
            context = context,
            media = media,
            options = legacyOptions,
        )
    }

    override suspend fun converter(
        context: Context,
        media: LocalMedia,
        options: ConvertOptions,
    ): LocalMedia {
        withContext(Dispatchers.IO) {
            /**
             * 当前媒体最初由选择器返回的可访问地址。
             *
             * 它可能是文件路径，也可能是 `content://`，不能直接假设可以构造 `File`。
             */
            val sourcePath = media.getAvailablePath()

            /**
             * 当前媒体 MIME 类型，用来确定沙盒目录、后缀和具体转换分支。
             */
            val mimeType = media.mimeType
            if (sourcePath == null || TextUtils.isEmpty(sourcePath)) {
                return@withContext
            }
            if (mimeType == null || TextUtils.isEmpty(mimeType)) {
                return@withContext
            }
            when {
                MediaUtils.hasMimeTypeOfImage(mimeType) -> {
                    /**
                     * 图片后续能力共同使用的本地文件路径。
                     *
                     * `needSandboxPath` 只在调用方明确要求时强制使用沙盒；
                     * 其它场景优先复用相册原文件，最后才复制。
                     */
                    val localPath = resolveLocalPath(
                        context = context,
                        media = media,
                        mimeType = mimeType,
                        sourcePath = sourcePath,
                        allowFinalCopy = options.needSandboxPath ||
                                options.needOriginalAbsolutePath ||
                                options.needCompressPath ||
                                options.isOriginalSelected,
                        forceSandboxForContent = options.needSandboxPath,
                    )
                    if (options.needOriginalAbsolutePath) {
                        media.preparedLocalPath = localPath
                    }
                    if (options.isOriginalSelected) {
                        media.originalPath = localPath
                    }
                    if (options.needCompressPath) {
                        localPath?.let { readableLocalPath ->
                            media.compressPath = compress(context, readableLocalPath)
                        }
                    }
//                    media.compressPath =
//                        processImageWithSampling(context, media.sandboxPath ?: path, mimeType)
//                    Log.d("MediaConverter", "图片压缩路径: ${media.compressPath}")
                    localPath?.let { readableLocalPath ->
                        try {
                            val exif = ExifInterface(readableLocalPath)
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
                }

                MediaUtils.hasMimeTypeOfVideo(mimeType) -> {
                    /**
                     * 视频缩略图生成和聊天发送共同使用的可读本地视频路径。
                     */
                    val localPath = resolveLocalPath(
                        context = context,
                        media = media,
                        mimeType = mimeType,
                        sourcePath = sourcePath,
                        allowFinalCopy = options.needSandboxPath ||
                                options.needOriginalAbsolutePath ||
                                media.videoThumbnailPath.isNullOrEmpty(),
                        forceSandboxForContent = options.needSandboxPath,
                    )
                    if (options.needOriginalAbsolutePath) {
                        media.preparedLocalPath = localPath
                    }
                    if (media.videoThumbnailPath.isNullOrEmpty()) {
                        media.videoThumbnailPath = localPath?.let {
                            generateVideoThumbnail(context, media, it)
                        }
                    }
                }

                MediaUtils.hasMimeTypeOfAudio(mimeType) -> {
                    /**
                     * 音频调用方需要时准备的本地文件路径。
                     */
                    val localPath = resolveLocalPath(
                        context = context,
                        media = media,
                        mimeType = mimeType,
                        sourcePath = sourcePath,
                        allowFinalCopy = options.needSandboxPath || options.needOriginalAbsolutePath,
                        forceSandboxForContent = options.needSandboxPath,
                    )
                    if (options.needOriginalAbsolutePath) {
                        media.preparedLocalPath = localPath
                    }
                }
            }
        }
        return media
    }

    /**
     * 判断当前媒体是否需要在选择结果回调前执行转换工作。
     *
     * 图片只有启用了原图标记、压缩、沙盒或可发送路径能力时才需要转换；
     * 视频仍需确保缩略图存在，音频只在请求本地路径能力时处理。
     */
    override fun requiresConversion(
        media: LocalMedia,
        options: ConvertOptions,
    ): Boolean {
        /**
         * 当前媒体类型，用来选择图片、视频或音频的转换策略。
         */
        val mimeType = media.mimeType
        return when {
            MediaUtils.hasMimeTypeOfImage(mimeType) -> {
                options.isOriginalSelected ||
                        options.needCompressPath ||
                        options.needSandboxPath ||
                        options.needOriginalAbsolutePath
            }

            MediaUtils.hasMimeTypeOfVideo(mimeType) -> {
                media.videoThumbnailPath.isNullOrEmpty() ||
                        options.needSandboxPath ||
                        options.needOriginalAbsolutePath
            }

            MediaUtils.hasMimeTypeOfAudio(mimeType) -> {
                options.needSandboxPath || options.needOriginalAbsolutePath
            }

            else -> false
        }
    }

    /**
     * 判断内置转换工作是否需要展示选择器 loading。
     *
     * 已有可读本地图片时，只执行路径赋值和 EXIF 读取，不展示 loading；
     * 需要 Provider 解析、沙盒复制、压缩或视频缩略图时继续展示。
     */
    override fun requiresLoading(
        media: LocalMedia,
        options: ConvertOptions,
    ): Boolean {
        /**
         * 当前媒体类型，用来区分哪些转换属于需要 loading 的重任务。
         */
        val mimeType = media.mimeType
        return when {
            MediaUtils.hasMimeTypeOfImage(mimeType) -> {
                when {
                    options.needCompressPath || options.needSandboxPath -> true
                    options.isOriginalSelected || options.needOriginalAbsolutePath -> {
                        findImmediateLocalPath(media) == null
                    }

                    else -> false
                }
            }

            MediaUtils.hasMimeTypeOfVideo(mimeType) -> {
                when {
                    media.videoThumbnailPath.isNullOrEmpty() -> true
                    options.needSandboxPath -> true
                    options.needOriginalAbsolutePath -> findImmediateLocalPath(media) == null
                    else -> false
                }
            }

            MediaUtils.hasMimeTypeOfAudio(mimeType) -> {
                if (!options.needSandboxPath && !options.needOriginalAbsolutePath) {
                    false
                } else {
                    findImmediateLocalPath(media) == null
                }
            }

            else -> false
        }
    }

    /**
     * 按固定优先级解析当前媒体可供路径型业务使用的本地文件。
     *
     * 顺序为：
     * 1. `LocalMedia` 已有相册原文件；
     * 2. 从 `content://` 再尝试解析真实文件；
     * 3. 已有 `preparedLocalPath`、`sandboxPath` 或确定性缓存；
     * 4. 确实需要时才复制到沙盒。
     */
    private suspend fun resolveLocalPath(
        context: Context,
        media: LocalMedia,
        mimeType: String,
        sourcePath: String,
        allowFinalCopy: Boolean,
        forceSandboxForContent: Boolean,
    ): String? {
        if (!forceSandboxForContent || !MediaUtils.isContent(sourcePath)) {
            if (media.isCrop() || media.isEditor()) {
                findReadableLocalPath(sourcePath, 0L)?.let { editedPath ->
                    return editedPath
                }
            }
            findReadableLocalPath(media.absolutePath, media.size)?.let { absolutePath ->
                return absolutePath
            }
            if (!media.isCrop() && !media.isEditor()) {
                findReadableLocalPath(sourcePath, media.size)?.let { filePath ->
                    return filePath
                }
            }
            resolveProviderLocalPath(context, sourcePath, media.size)?.let { resolvedPath ->
                return resolvedPath
            }
        }

        findReadableLocalPath(media.originalPath, media.size)?.let { originalPath ->
            return originalPath
        }
        findReadableLocalPath(
            candidatePath = media.preparedLocalPath,
            expectedSourceSize = media.size,
        )?.let { preparedPath ->
            return preparedPath
        }
        findReadableLocalPath(
            candidatePath = media.sandboxPath,
            expectedSourceSize = media.size,
        )?.let { sandboxPath ->
            media.preparedLocalPath = media.preparedLocalPath ?: sandboxPath
            return sandboxPath
        }

        /**
         * 当前媒体按 URI 和后缀计算出的确定性沙盒目标。
         *
         * 即使 `LocalMedia.sandboxPath` 没有保留下来，也可以复用之前已经复制成功的文件。
         */
        val sandboxTarget = buildSandboxTarget(
            context = context,
            sourcePath = sourcePath,
            mimeType = mimeType,
            postfix = MediaUtils.getPostfix(context, sourcePath, defaultPostfix(mimeType)),
        )
        findReadableLocalPath(
            candidatePath = sandboxTarget.absolutePath,
            expectedSourceSize = media.size,
        )?.let { cachedPath ->
            media.sandboxPath = cachedPath
            media.preparedLocalPath = media.preparedLocalPath ?: cachedPath
            return cachedPath
        }

        if (!allowFinalCopy) {
            return null
        }
        /**
         * 最终兜底复制完成后得到的候选沙盒路径。
         */
        val copiedPath = copyToSandbox(
            context = context,
            path = sourcePath,
            mimeType = mimeType,
            postfix = MediaUtils.getPostfix(context, sourcePath, defaultPostfix(mimeType)),
            expectedSourceSize = media.size,
        )
        if (copiedPath == null) {
            return null
        }
        media.sandboxPath = copiedPath
        return copiedPath
    }

    /**
     * 查找无需 Provider 查询或沙盒复制即可立即使用的本地文件。
     *
     * 裁剪、编辑结果优先于原相册文件；
     * 普通媒体则先检查 `absolutePath`，再检查本身已经是文件路径的 `path`。
     */
    private fun findImmediateLocalPath(media: LocalMedia): String? {
        /**
         * 当前媒体按照裁剪、编辑等派生结果优先级得到的地址。
         */
        val availablePath = media.getAvailablePath()
        if (media.isCrop() || media.isEditor()) {
            findReadableLocalPath(availablePath, 0L)?.let { editedPath ->
                return editedPath
            }
        }
        findReadableLocalPath(media.absolutePath, media.size)?.let { absolutePath ->
            return absolutePath
        }
        if (!media.isCrop() && !media.isEditor()) {
            findReadableLocalPath(availablePath, media.size)?.let { filePath ->
                return filePath
            }
        }
        return null
    }

    /**
     * 尝试从 `content://` 或 `file://` 地址解析真实本地文件。
     *
     * 解析结果必须再次通过文件系统校验，避免把云端标识、相对路径或失效 DATA 值误当成文件。
     */
    private fun resolveProviderLocalPath(
        context: Context,
        sourcePath: String,
        expectedSourceSize: Long,
    ): String? {
        if (!MediaUtils.isContent(sourcePath) && !sourcePath.startsWith("file://")) {
            return null
        }
        return try {
            val resolvedPath = MediaUtils.getPath(context, Uri.parse(sourcePath))
            findReadableLocalPath(resolvedPath, expectedSourceSize)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 规范化并验证候选文件路径。
     *
     * 只有绝对、存在、普通、可读的文件才允许进入发送或压缩链路；
     * 已知原资源非空时，同时拒绝零字节残留文件。
     */
    private fun findReadableLocalPath(
        candidatePath: String?,
        expectedSourceSize: Long,
    ): String? {
        if (candidatePath.isNullOrEmpty() || MediaUtils.isContent(candidatePath)) {
            return null
        }
        /**
         * 去除 `file://` scheme 后用于文件系统校验的真实路径。
         */
        val normalizedPath = if (candidatePath.startsWith("file://")) {
            Uri.parse(candidatePath).path
        } else {
            candidatePath
        }
        if (normalizedPath.isNullOrEmpty()) {
            return null
        }
        /**
         * 规范化路径对应的文件系统对象。
         */
        val candidateFile = File(normalizedPath)
        if (!candidateFile.isAbsolute ||
            !candidateFile.exists() ||
            !candidateFile.isFile ||
            !candidateFile.canRead()
        ) {
            return null
        }
        if (expectedSourceSize > 0L && candidateFile.length() <= 0L) {
            return null
        }
        return candidateFile.absolutePath
    }

    /**
     * Copy files into the application sandbox.
     *
     * 同一个确定性目标通过互斥锁串行写入，
     * 防止批量选择或重复资源同时复制时读取到未完成文件。
     */
    private suspend fun copyToSandbox(
        context: Context,
        path: String,
        mimeType: String,
        postfix: String,
        expectedSourceSize: Long,
    ): String? {
        /**
         * 当前资源对应的确定性沙盒目标文件。
         */
        val targetFile = buildSandboxTarget(context, path, mimeType, postfix)

        /**
         * 当前目标文件对应的进程内复制锁。
         */
        val copyMutex = synchronized(sandboxCopyMutexes) {
            sandboxCopyMutexes[targetFile.absolutePath] ?: Mutex().also { createdMutex ->
                sandboxCopyMutexes[targetFile.absolutePath] = createdMutex
            }
        }
        return copyMutex.withLock {
            findReadableLocalPath(
                candidatePath = targetFile.absolutePath,
                expectedSourceSize = expectedSourceSize,
            )?.let {
                return@withLock it
            }
            if (targetFile.exists()) {
                targetFile.delete()
            }
            /**
             * 当前正式缓存对应的临时写入文件。
             *
             * 先完整写入临时文件，再替换正式缓存，
             * 避免进程中断后留下一个会被误判为缓存命中的半截文件。
             */
            val temporaryFile = File("${targetFile.absolutePath}.tmp")
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
            /**
             * 文件复制工具返回的临时结果路径。
             *
             * 该结果仍需经过统一文件校验，不能只根据非空判断复制成功。
             */
            val temporaryCopiedPath = FileUtils.copyFile(
                context = context,
                from = path,
                target = temporaryFile.absolutePath,
            )

            /**
             * 已经通过完整文件校验的临时路径。
             */
            val readableTemporaryPath = findReadableLocalPath(
                candidatePath = temporaryCopiedPath,
                expectedSourceSize = expectedSourceSize,
            )
            if (readableTemporaryPath == null) {
                temporaryFile.delete()
                return@withLock null
            }
            if (!File(readableTemporaryPath).renameTo(targetFile)) {
                FileUtils.copyFile(
                    context = context,
                    from = readableTemporaryPath,
                    target = targetFile.absolutePath,
                )
                File(readableTemporaryPath).delete()
            }
            findReadableLocalPath(
                candidatePath = targetFile.absolutePath,
                expectedSourceSize = expectedSourceSize,
            )
        }
    }

    /**
     * 构建资源对应的确定性沙盒缓存文件。
     */
    private fun buildSandboxTarget(
        context: Context,
        sourcePath: String,
        mimeType: String,
        postfix: String,
    ): File {
        /**
         * 当前媒体类型对应的沙盒目录。
         *
         * 外部应用目录不可用时退回内部 filesDir，避免生成 `null/...` 路径。
         */
        val targetDirectory = getFileDir(context, mimeType) ?: context.filesDir
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        return File(targetDirectory, "sandbox_${md5Hash(sourcePath)}.$postfix")
    }

    /**
     * 返回当前媒体类型无法从 URI 取得后缀时使用的默认后缀。
     */
    private fun defaultPostfix(mimeType: String): String {
        return when {
            MediaUtils.hasMimeTypeOfVideo(mimeType) -> "mp4"
            MediaUtils.hasMimeTypeOfAudio(mimeType) -> "amr"
            else -> "jpg"
        }
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
                        context = context,
                        path = path,
                        mimeType = mimeType,
                        postfix = MediaUtils.getPostfix(context, path, "jpg"),
                        expectedSourceSize = 0L,
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
                    context = context,
                    path = path,
                    mimeType = mimeType,
                    postfix = MediaUtils.getPostfix(context, path, "jpg"),
                    expectedSourceSize = 0L,
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
//        val compressFile = Compressor.compress(context, File(path)) {
//            resolution(2000, 2000)
//            quality(80)
//            format(Bitmap.CompressFormat.JPEG)
//            destination(targetPath)
//            size(1024 * 1024 * 10)
//        }
        val compressFilePath = CompressUtils.INSTANCE.compress(
            path,
            targetPath.absolutePath,
            2000,
            4000,
            80,
            1024 * 1024 * 10
        )
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
        return compressFilePath
    }


    private fun getCompressFileDir(context: Context): File {
        return File(
            context.getExternalFilesDir(""), "/compressor/${System.currentTimeMillis()}.jpg"
        )
    }

    private fun generateVideoThumbnail(
        context: Context,
        media: LocalMedia,
        localVideoPath: String,
    ): String? {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(localVideoPath)

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
        /**
         * 沙盒目标路径和复制互斥锁的进程内映射。
         *
         * 同一资源在批量任务中重复出现时，确保只有一个协程写入目标文件。
         */
        private val sandboxCopyMutexes = ConcurrentHashMap<String, Mutex>()

        fun create() = InstanceHelper.engine
    }

    object InstanceHelper {
        val engine = MediaConverter()
    }
}
