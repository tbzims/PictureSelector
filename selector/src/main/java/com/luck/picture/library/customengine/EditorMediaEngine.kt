package com.luck.picture.library.customengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.luck.picture.library.entity.LocalMedia
import com.luck.picture.library.helper.ActivityCompatHelper
import com.luck.picture.library.interfaces.OnEditorMediaListener
import com.luck.picture.library.utils.MediaUtils
import com.luck.picture.library.utils.ToastUtils
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropImageEngine
import java.io.File

class EditorMediaEngine :OnEditorMediaListener {
    override fun onEditorMedia(
        fragment: Fragment,
        media: LocalMedia,
        requestCode: Int
    ) {
        if (MediaUtils.hasMimeTypeOfVideo(media.mimeType)) {
            ToastUtils.showMsg(fragment.requireContext(), "视频编辑功能请自行实现")
            return
        }
        val path = media.getAvailablePath() ?: return
        val sourceUri =
            if (MediaUtils.isContent(path)) Uri.parse(path) else Uri.fromFile(File(path))
        val destinationUri = Uri.fromFile(
            File(fragment.requireContext().cacheDir, "${System.currentTimeMillis()}.jpg")
        )
            val uCrop = UCrop.of<UCrop>(sourceUri, destinationUri)
            uCrop.setImageEngine(object : UCropImageEngine {
                override fun loadImage(
                    context: Context,
                    url: String,
                    imageView: ImageView
                ) {
                    if (!ActivityCompatHelper.assertValidRequest(context)) {
                        return
                    }
                    Glide.with(context).load(url).override(180, 180)
                        .into(imageView)
                }

                override fun loadImage(
                    context: Context,
                    url: Uri,
                    maxWidth: Int,
                    maxHeight: Int,
                    call: UCropImageEngine.OnCallbackListener<Bitmap>?
                ) {
                    if (!ActivityCompatHelper.assertValidRequest(context)) {
                        return
                    }
                    Glide.with(context).asBitmap().load(url)
                        .override(maxWidth, maxHeight)
                        .into(object : CustomTarget<Bitmap?>() {

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                call?.onCall(null)
                            }

                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap?>?
                            ) {
                                call?.onCall(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                call?.onCall(null)
                            }
                        })
                }
            })
            uCrop.startEdit(fragment.requireContext(), fragment, requestCode)
    }
}