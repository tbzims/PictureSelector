package com.luck.picture.library.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.library.R
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
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.ps_item_more_menu, parent, false)
        return MoreMenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoreMenuViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = mData.size

    inner class MoreMenuViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)

        fun bind(item: MoreMenuItem) {
            ivIcon.setImageResource(item.iconRes)
            tvTitle.text = item.title
            ivCheck.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE
            tvTitle.setTextColor(
                itemView.context.getColorByAttr(com.tmmtmm.im.style.R.attr.special_3)
            )
            itemView.setOnClickListener {
                onItemClickListener?.invoke(item)
            }
        }
    }
}