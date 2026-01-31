package com.luck.picture.library.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.library.R
import com.luck.picture.library.config.SelectorConfig
import com.luck.picture.library.entity.LocalMedia
import com.luck.picture.library.interfaces.OnItemClickListener
import com.luck.picture.library.interfaces.OnLongClickListener
import com.luck.picture.library.utils.DensityUtil
import com.luck.picture.library.utils.MediaUtils

class PreviewAdapter(
    var config: SelectorConfig
) :
    RecyclerView.Adapter<PreviewAdapter.GalleryViewHolder>() {
    var currentMedia: LocalMedia? = null

    //    var selectResult: MutableList<LocalMedia>? = null
    private var data: MutableList<LocalMedia> = mutableListOf()

    fun setNewData(data: MutableList<LocalMedia>) {
        this.data = data.toMutableList()
        notifyDataSetChanged()
    }

    fun addData(data: LocalMedia) {
        this.data.add(data)
        notifyItemInserted(this.data.size - 1)
    }

    fun removeData(data: LocalMedia) {
        val index = this.data.indexOf(data)
        if (index >= 0) {
            notifyItemChanged(index)
            this.data.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        return GalleryViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.ps_preview_gallery_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val media = data[position]

        holder.viewBorder.visibility =
            if (isSelected(currentMedia, media)) View.VISIBLE else View.INVISIBLE
        holder.ivEditor.visibility = if (media.isEditor()) View.VISIBLE else View.GONE
        holder.ivVideoFlag.visibility =
            if (MediaUtils.hasMimeTypeOfVideo(media.mimeType) || MediaUtils.hasMimeTypeOfAudio(
                    media.mimeType
                )
            ) View.VISIBLE else View.GONE
        if (MediaUtils.hasMimeTypeOfAudio(media.mimeType)) {
            holder.ivCover.setImageResource(R.drawable.ps_audio_placeholder)
        } else {
            config.imageEngine?.loadRoundImage(
                holder.ivCover.context,
                media.getAvailablePath(),
                holder.ivCover,
                DensityUtil.dip2px(holder.itemView.context, 3f)
            )
        }
        holder.itemView.setOnClickListener {
            mItemClickListener?.onItemClick(position, media)
        }
        holder.itemView.setOnLongClickListener {
            mLongClickListener?.onLongClick(holder, position, media)
            true
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.iv_image)
        val viewBorder: View = itemView.findViewById(R.id.view_border)
        val ivEditor: ImageView = itemView.findViewById(R.id.iv_editor)
        val ivVideoFlag: ImageView = itemView.findViewById(R.id.iv_video_flag)
    }

    fun isSelected(currentMedia: LocalMedia?, media: LocalMedia): Boolean {
        return TextUtils.equals(currentMedia?.path, media.path) || currentMedia?.id == media.id
    }

    private var mItemClickListener: OnItemClickListener<LocalMedia>? = null

    fun setOnItemClickListener(l: OnItemClickListener<LocalMedia>) {
        this.mItemClickListener = l
    }

    private var mLongClickListener: OnLongClickListener<LocalMedia>? = null
    fun setOnLongItemClickListener(l: OnLongClickListener<LocalMedia>?) {
        this.mLongClickListener = l
    }
}