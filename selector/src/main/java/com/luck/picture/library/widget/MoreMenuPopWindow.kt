package com.luck.picture.library.widget

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.library.R
import com.luck.picture.library.adapter.MoreMenuAdapter
import com.luck.picture.library.model.MoreMenuItem

class MoreMenuPopWindow(val context: Context) : PopupWindow(context) {
    private val recyclerView: RecyclerView
    private val adapter: MoreMenuAdapter

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.ps_popup_more_menu, null)
        contentView = view
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        recyclerView = view.findViewById(R.id.rv_more_menu)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        adapter = MoreMenuAdapter()
        recyclerView.adapter = adapter
    }

    fun setData(data: List<MoreMenuItem>, listener: (MoreMenuItem, Int) -> Unit) {
        adapter.submitList(data)
        adapter.setOnItemClickListener { item, position ->
            listener.invoke(item, position)
            dismiss()
        }
    }

    fun setChangeSelect(position: Int, isSelected: Boolean) {
        val newList = adapter.getList().mapIndexed { index, item ->
            if (index == position) {
                item.copy(isSelected = isSelected)
            } else {
                item
            }
        }
        adapter.submitList(newList)
    }

    fun getItem(position: Int): MoreMenuItem {
        return adapter.getCurrentItem(position)
    }

    fun showAtBottom(anchor: View?) {
//        val rightMargin = context.resources.getDimensionPixelSize(com.tmmtmm.im.style.R.dimen.dp_16)
        // 获取锚点视图的位置
//        val location = IntArray(2)
//        anchor?.getLocationInWindow(location)
//
//        // 计算 PopupWindow 应该显示的 x 坐标
//        val x = (location[0] + anchor!!.width - width).coerceAtLeast(rightMargin)
//        val y = location[1] + anchor.height
//
//        // 使用 showAtLocation 精确控制位置
//        showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, x, y)

        if (anchor == null) return
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT

        // 2. 测量逻辑：给一个不受限的测量环境
        // 注意：如果是从 Adapter 获取数据后立刻显示，建议先调用 adapter.notifyDataSetChanged()
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // 3. 获取测量后的最大宽度
        val measuredWidth = contentView.measuredWidth

        // 4. 重点：显式给 PopupWindow 的 width 赋具体数值
        // 这样 RecyclerView 会被拉伸到这个最大宽度，所有 Item 也就一样宽了
        this.width = measuredWidth

        // 5. 计算坐标 (靠右对齐)
        val location = IntArray(2)
        anchor.getLocationInWindow(location)

        val x = location[0] + anchor.width - this.width
        val y = location[1] + anchor.height

        showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, x, y)
    }
}