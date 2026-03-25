package com.luck.picture.library.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.library.R
import com.luck.picture.library.adapter.MoreMenuAdapter
import com.luck.picture.library.model.MoreMenuItem
import com.luck.picture.library.utils.DensityUtil

class MoreMenuPopWindow(context: Context) : PopupWindow(context) {
    private val recyclerView: RecyclerView
    private val adapter: MoreMenuAdapter

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.ps_popup_more_menu, null)
        contentView = view
        width = DensityUtil.dip2px(context, 220f)
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        recyclerView = view.findViewById(R.id.rv_more_menu)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MoreMenuAdapter()
        recyclerView.adapter = adapter
    }

    fun setData(data: List<MoreMenuItem>, listener: (MoreMenuItem) -> Unit) {
        adapter.submitList(data)
        adapter.setOnItemClickListener {
            listener.invoke(it)
            dismiss()
        }
    }
}