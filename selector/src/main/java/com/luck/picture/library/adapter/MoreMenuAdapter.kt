package com.luck.picture.library.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.library.R
import com.luck.picture.library.model.MoreMenuItem
import com.tmmtmm.im.style.utils.getColorByAttr

class MoreMenuAdapter :
    ListAdapter<MoreMenuItem, MoreMenuAdapter.MoreMenuViewHolder>(MoreMenuItemDiffCallback()) {
    private var onItemClickListener: ((MoreMenuItem, Int) -> Unit)? = null

    // 更新数据的方法
//    fun submitList(list: List<MoreMenuItem>) {
//        this.mData = list
//        notifyDataSetChanged()
//    }

    fun getList(): List<MoreMenuItem> {
        return currentList
    }

    fun getCurrentItem(position: Int): MoreMenuItem {
        return getItem(position)
    }

    // 设置点击回调
    fun setOnItemClickListener(listener: (MoreMenuItem, Int) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoreMenuViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.ps_item_more_menu, parent, false)
        return MoreMenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoreMenuViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position)
    }

    override fun onBindViewHolder(holder: MoreMenuViewHolder, position: Int, payloads: List<Any?>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach {
                when (it) {
                    "isSelected" -> {
                        holder.ivCheck.visibility =
                            if (getItem(position).isSelected) View.VISIBLE else View.INVISIBLE
                    }
                }
            }
        }
    }

    inner class MoreMenuViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)

        fun bind(item: MoreMenuItem, position: Int) {
            ivIcon.setImageResource(item.iconRes)
            tvTitle.text = item.title
            ivCheck.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE
            tvTitle.setTextColor(
                itemView.context.getColorByAttr(com.tmmtmm.im.style.R.attr.special_3)
            )
            itemView.setOnClickListener {
                onItemClickListener?.invoke(item, position)
            }
        }
    }

    class MoreMenuItemDiffCallback : DiffUtil.ItemCallback<MoreMenuItem>() {
        override fun areItemsTheSame(oldItem: MoreMenuItem, newItem: MoreMenuItem): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: MoreMenuItem, newItem: MoreMenuItem): Boolean {
            return oldItem.isSelected == newItem.isSelected
        }

        override fun getChangePayload(oldItem: MoreMenuItem, newItem: MoreMenuItem): Any? {
            if (oldItem.isSelected != newItem.isSelected) {
                return "isSelected"
            }
            return null
        }
    }
}