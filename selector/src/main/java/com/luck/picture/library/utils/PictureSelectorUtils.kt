package com.luck.picture.library.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.activity.result.ActivityResultLauncher
import com.luck.picture.library.R
import com.luck.picture.library.config.MediaType
import com.luck.picture.library.config.SelectionMode
import com.luck.picture.library.constant.SelectorConstant
import com.luck.picture.library.customengine.CustomPreviewExoVideoHolder
import com.luck.picture.library.customengine.MediaConverter
import com.luck.picture.library.customengine.UCropEngine
import com.luck.picture.library.entity.LocalMedia
import com.luck.picture.library.interfaces.OnExternalPreviewListener
import com.luck.picture.library.interfaces.OnResultCallbackListener
import com.luck.picture.library.interfaces.SelectorExpandViewInjector
import com.luck.picture.library.model.PictureSelector
import com.luck.picture.library.style.StatusBarStyle
import com.luck.picture.library.style.WindowAnimStyle
import com.tmmtmm.im.style.utils.getColorByAttr

const val ALBUM_MAX_SIZE = 1 * 1024 * 1024 * 1024L

class PictureSelectorUtils(
    val context: Context,
    val mediaType: MediaType = MediaType.ALL,
    val isMultiple: Boolean = true,
    val maxSelectTotalNum: Int = 9,
    val maxSelectVideoNum: Int = 1,
    val isGif: Boolean = false,
    val isWebp: Boolean = false,
    val isUCrop: Boolean = false,
    val maxFileSize: Long = ALBUM_MAX_SIZE,
    val injectorClasses: List<Class<out SelectorExpandViewInjector>> = emptyList(),
    val showCamera: Boolean = false,
    val isAllWithImageVideo: Boolean = false
) {


    fun createOnlyCamera(listener: OnResultCallbackListener) {
        return PictureSelector.create(context)
            .openCamera(mediaType)
            .setAllOfCameraMode(mediaType)
            .isCameraForegroundService(true)
            .setMediaConverterEngine(MediaConverter.Companion.create())
            .setCropEngine(UCropEngine())
            .forResult(listener)
    }

    fun createPictureSelector(listener: (List<LocalMedia>) -> Unit) {
        PictureSelector.create(context)
            .openGallery(mediaType)
            .setStatusBarStyle(StatusBarStyle().apply {
                of(
                    false,
                    context.getColorByAttr(com.tmmtmm.im.style.R.attr.bg_3),
                    context.getColorByAttr(com.tmmtmm.im.style.R.attr.bg_3)
                )
            })
            .setWindowAnimStyle(WindowAnimStyle().apply {
                of(
                    R.anim.ps_anim_up_in,
                    R.anim.ps_anim_down_out
                )
            })
            .setSelectionMode(if (isMultiple) SelectionMode.MULTIPLE else SelectionMode.ONLY_SINGLE)
            .setImageSpanCount(3)
            .setMaxSelectNum(
                maxSelectTotalNum,
                maxSelectVideoNum,
                true
            )
            .isPreviewZoomEffect(
                true,
                true
            )
            .isDisplayCamera(showCamera)
            .isMaxSelectEnabledMask(true)
            .registry(CustomPreviewExoVideoHolder::class.java)
            .isGif(isGif)
            .isWebp(isWebp)
            .isAllWithImageVideo(isAllWithImageVideo)
            .setMediaConverterEngine(MediaConverter.create())
            .setCropEngine(if (isUCrop) UCropEngine() else null)
            .setFilterMaxFileSize(maxFileSize)
            .setFilterMinFileSize(0)
            .setInjectorClasses(injectorClasses)
            .forResult(object : OnResultCallbackListener {
                override fun onResult(result: List<LocalMedia>) {
                    listener.invoke(result)
                }

                override fun onCancel() {
                }
            })
    }

    fun createPictureUrlPreview(position: Int = 0, strings: MutableList<String>) {
        val preview = PictureSelector.create(context).openPreview()
        preview.setStatusBarStyle(StatusBarStyle().apply {
            of(
                false,
                context.getColorByAttr(com.tmmtmm.im.style.R.attr.bg_3),
                context.getColorByAttr(com.tmmtmm.im.style.R.attr.bg_3)
            )
        })
        preview.forPreviewUrl(position, strings, true)
    }

    fun createPicturePreview(position: Int = 0, strings: MutableList<LocalMedia>) {
        val preview = PictureSelector.create(context).openPreview()
        preview.setStatusBarStyle(StatusBarStyle().apply {
            of(
                false,
                context.getColorByAttr(com.tmmtmm.im.style.R.attr.bg_3),
                context.getColorByAttr(com.tmmtmm.im.style.R.attr.bg_3)
            )
        })
        preview.forPreview(position, strings, true)
    }
}