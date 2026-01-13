package com.luck.picture.library.customengine

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.luck.picture.library.R
import com.luck.picture.library.engine.ImageEngine
import com.luck.picture.library.helper.ActivityCompatHelper
import com.luck.picture.library.utils.DensityUtil

/**
 * @author：luck
 * @date：2022-5-24 22:30
 * @describe：Glide图片加载
 */
class GlideEngine : ImageEngine {

    override fun loadImage(context: Context, url: String?, imageView: ImageView) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView)
    }

    override fun loadImage(
        context: Context,
        url: String?,
        width: Int,
        height: Int,
        imageView: ImageView
    ) {
        Glide.with(context).load(url).override(width, height).diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView)
    }

    override fun loadAlbumCover(context: Context, url: String?, imageView: ImageView) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context).load(url)
            .override(180, 180)
            .transform(CenterCrop(), RoundedCorners(DensityUtil.dip2px(context, 4f)))
            .placeholder(R.drawable.ps_image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    override fun loadListImage(context: Context, url: String?, imageView: ImageView) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context).load(url)
            .override(300, 300)
            .centerCrop()
            .placeholder(R.drawable.ps_image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    override fun loadRoundImage(
        context: Context,
        url: String?,
        imageView: ImageView,
        round: Int
    ) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context).load(url)
            .transform(CenterCrop(), RoundedCorners(round))
            .placeholder(R.drawable.ps_image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    override fun pauseRequests(context: Context) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context).pauseRequests()
    }

    override fun resumeRequests(context: Context) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context).resumeRequests()
    }

    companion object {
        fun create() = InstanceHelper.engine
    }

    object InstanceHelper {
        val engine = GlideEngine()
    }
}