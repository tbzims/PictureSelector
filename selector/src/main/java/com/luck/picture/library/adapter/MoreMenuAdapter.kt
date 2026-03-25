package com.luck.picture.library.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.library.databinding.PsItemMoreMenuBinding
import com.luck.picture.library.model.MoreMenuItem
import com.tmmtmm.im.style.utils.getColorByAttr

class MoreMenuAdapter : RecyclerView.Adapter<MoreMenuAdapter.MoreMenuViewHolder>() {

    private var mData: List<MoreMenuItem> = emptyList()
    private var onItemClickListener: ((MoreMenuItem) -> Unit)? = null

    // 更新数据的方法
    fun submitList(list: List<MoreMenuItem>) {
        this.mData = list
        notifyDataSetChanged()
    }

    // 设置点击回调
    fun setOnItemClickListener(listener: (MoreMenuItem) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoreMenuViewHolder {
        val binding = PsItemMoreMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MoreMenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoreMenuViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = mData.size

    inner class MoreMenuViewHolder(private val binding: PsItemMoreMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MoreMenuItem) {
            binding.ivIcon.setImageResource(item.iconRes)
            binding.tvTitle.text = item.title
            binding.ivCheck.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE
            binding.tvTitle.setTextColor(
                itemView.context.getColorByAttr(com.tmmtmm.im.style.R.attr.special_3)
            )
            itemView.setOnClickListener {
                onItemClickListener?.invoke(item)
            }
        }
    }
}