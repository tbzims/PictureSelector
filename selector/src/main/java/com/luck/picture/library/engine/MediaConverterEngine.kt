package com.luck.picture.library.engine

import android.content.Context
import com.luck.picture.library.entity.LocalMedia

/**
 * @author：luck
 * @date：2020-01-14 17:08
 * @describe：Media Data Converter
 */
interface MediaConverterEngine {
    /**
     * 单条媒体转换时需要遵循的准备选项。
     *
     * [isOriginalSelected] 只表示用户是否勾选原图；
     * 其它三个参数分别控制压缩、沙盒路径和可发送本地路径，彼此独立。
     */
    data class ConvertOptions(
        val isOriginalSelected: Boolean,
        val needCompressPath: Boolean,
        val needSandboxPath: Boolean,
        val needOriginalAbsolutePath: Boolean,
    )

    /**
     * Media Data Converter
     * 1、Android 10 platform, sandbox processing of external directory files [LocalMedia.sandboxPath]
     * 2、Android 10 platform, original image processing [LocalMedia.originalPath]
     * 3、Video thumbnail [LocalMedia.videoThumbnailPath]
     * 4、Image watermark [LocalMedia.watermarkPath]
     * 5、Image or video compression [LocalMedia.compressPath]
     * ...
     * Customize Other Actions [LocalMedia.customizeExtra]
     */
    suspend fun converter(
        context: Context,
        media: LocalMedia,
        isOriginalPath: Boolean,
        isCompress: Boolean
    ): LocalMedia

    /**
     * 使用完整选项执行媒体转换。
     *
     * 默认实现兼容已有自定义转换引擎：
     * 未适配新选项的引擎仍会收到旧的“原图、压缩”两个参数。
     */
    suspend fun converter(
        context: Context,
        media: LocalMedia,
        options: ConvertOptions,
    ): LocalMedia {
        return converter(
            context = context,
            media = media,
            isOriginalPath = options.isOriginalSelected,
            isCompress = options.needCompressPath,
        )
    }

    /**
     * 判断当前媒体是否需要在回调前执行异步转换。
     *
     * 自定义引擎默认返回 true，保证旧实现不会因为选择器跳过转换而改变行为。
     */
    fun requiresConversion(
        media: LocalMedia,
        options: ConvertOptions,
    ): Boolean {
        return true
    }

    /**
     * 判断当前媒体转换是否需要展示阻塞式 loading。
     *
     * 默认跟随 [requiresConversion]，保证自定义转换引擎的旧行为不变；
     * 内置引擎可以对仅执行轻量本地处理的场景返回 false。
     */
    fun requiresLoading(
        media: LocalMedia,
        options: ConvertOptions,
    ): Boolean {
        return requiresConversion(media, options)
    }
}
